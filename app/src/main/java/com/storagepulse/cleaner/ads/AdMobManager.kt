package com.storagepulse.cleaner.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.storagepulse.cleaner.BuildConfig
import com.storagepulse.cleaner.data.billing.ProRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class AdGateResult {
    /** User watched the ad (or is Pro: no ad needed). */
    EARNED,
    /** User closed the ad early: no reward. */
    DISMISSED,
    /** No ad could be loaded (offline, no fill): the feature still runs, never blocked. */
    UNAVAILABLE,
}

/**
 * All ads in one place. Unit ids come from BuildConfig: Google's TEST ids in debug, production
 * ids (gradle properties) in release. Pro users never see or load an ad.
 * Ads are requested only after Google UMP consent allows it ([gatherConsent]).
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    pro: ProRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val _canRequestAds = MutableStateFlow(consent.canRequestAds())

    /**
     * true only for free users whose consent state allows ad requests (UMP). false until the Pro
     * state is known, so Pro users never see a flash of an ad.
     */
    val adsEnabled: StateFlow<Boolean> = combine(pro.isPro, _canRequestAds) { isPro, can -> !isPro && can }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** EEA/UK/CH users must be able to change their ad consent: Settings shows a button then. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Google UMP consent: refreshes the consent state and shows Google's consent form when it is
     * required (EEA/UK/CH). Ads are only initialised and requested once [canRequestAds] is true.
     * Call from the activity on every launch (free users only).
     */
    fun gatherConsent(activity: Activity) {
        if (consent.canRequestAds()) onConsentReady()
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onConsentReady() }
            },
            { onConsentReady() }, // offline / error: keep the previous decision
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onConsentReady() }
    }

    private fun onConsentReady() {
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        val can = consent.canRequestAds()
        _canRequestAds.value = can
        if (can) initialize()
    }

    val bannerUnitId: String = BuildConfig.ADMOB_BANNER_ID

    private var initialized = false

    /** AdMob init off the main thread (it does disk + network work). Only after consent. */
    private fun initialize() {
        if (initialized) return
        initialized = true
        Thread { MobileAds.initialize(context) }.start()
    }

    // ------------------------------------------------------------------ interstitial

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    // Starts the cap at app launch: no interstitial in the first 3 minutes either.
    private var lastInterstitialAt = SystemClock.elapsedRealtime()

    fun preloadInterstitial() {
        if (!adsEnabled.value || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                }
            },
        )
    }

    /**
     * Shows an interstitial after a finished clean-up, at most once every 3 minutes and only if
     * one is already loaded (never makes the user wait). Returns true if shown.
     */
    fun maybeShowInterstitial(activity: Activity): Boolean {
        if (!adsEnabled.value) return false
        val now = SystemClock.elapsedRealtime()
        if (now - lastInterstitialAt < INTERSTITIAL_COOLDOWN_MS) return false
        val ad = interstitial ?: run {
            preloadInterstitial()
            return false
        }
        interstitial = null
        lastInterstitialAt = now
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = preloadInterstitial()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = preloadInterstitial()
        }
        ad.show(activity)
        return true
    }

    // ------------------------------------------------------------------ rewarded

    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false

    fun preloadRewarded() {
        if (!adsEnabled.value || rewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    rewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                }
            },
        )
    }

    /**
     * Rewarded video in front of a batch action. Pro → EARNED with no ad. Offline / no fill
     * within [waitMs] → UNAVAILABLE and the caller runs the feature anyway.
     */
    suspend fun showRewarded(activity: Activity, waitMs: Long = 5_000): AdGateResult {
        if (!adsEnabled.value) return AdGateResult.EARNED
        preloadRewarded()
        val ready = rewarded ?: withTimeoutOrNull(waitMs) {
            while (rewarded == null) {
                if (!rewardedLoading) preloadRewarded()
                delay(200)
            }
            rewarded
        } ?: return AdGateResult.UNAVAILABLE

        rewarded = null
        return suspendCancellableCoroutine { cont ->
            var earned = false
            ready.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    if (cont.isActive) cont.resume(if (earned) AdGateResult.EARNED else AdGateResult.DISMISSED)
                    preloadRewarded()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    if (cont.isActive) cont.resume(AdGateResult.UNAVAILABLE)
                    preloadRewarded()
                }
            }
            ready.show(activity) { earned = true }
        }
    }

    companion object {
        val INTERSTITIAL_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(3)
    }
}
