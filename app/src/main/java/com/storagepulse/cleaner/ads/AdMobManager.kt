package com.storagepulse.cleaner.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.storagepulse.cleaner.BuildConfig
import com.storagepulse.cleaner.data.billing.ProRepository
import com.storagepulse.cleaner.data.storage.StorageScannerRepository
import com.storagepulse.cleaner.data.storage.model.ScanState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    /** No ad could be loaded or shown (offline, no fill, error): the feature still runs, never blocked. */
    UNAVAILABLE,
}

/**
 * All ads in one place. Unit ids come from BuildConfig: Google's TEST ids in debug, production
 * ids (gradle properties) in release. Ads are requested only for free users and only after Google
 * UMP consent allows it ([ConsentManager]). Pro users never load or see an ad.
 *
 * Formats: anchored adaptive banner (`AdMobBannerView`), rewarded (batch compression, bulk
 * duplicate clean-up), frequency-capped interstitial (end of a swipe session, after emptying a
 * large Safe Trash) and one native card in the unused-apps list.
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val consent: ConsentManager,
    private val scanner: StorageScannerRepository,
    pro: ProRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** true only for free users whose consent allows ad requests; false until Pro state is known. */
    val adsEnabled: StateFlow<Boolean> = combine(pro.isPro, consent.canRequestAds) { isPro, can -> !isPro && can }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val privacyOptionsRequired: StateFlow<Boolean> = consent.privacyOptionsRequired

    val bannerUnitId: String = BuildConfig.ADMOB_BANNER_ID

    init {
        // Becoming Pro: drop any preloaded full-screen ads at once.
        scope.launch {
            adsEnabled.drop(1).filter { !it }.collect {
                interstitial = null
                rewarded = null
            }
        }
    }

    /** UMP consent first, then SDK init (background thread). Call from the activity, free users only. */
    fun gatherConsent(activity: Activity) = consent.gather(activity) { if (consent.canRequestAds.value) initialize() }

    fun showPrivacyOptions(activity: Activity) = consent.showPrivacyOptions(activity) { if (consent.canRequestAds.value) initialize() }

    @Volatile private var initialized = false

    private fun initialize() {
        if (initialized) return
        initialized = true
        Thread {
            val testIds = buildList {
                add(AdRequest.DEVICE_ID_EMULATOR)
                addAll(BuildConfig.ADMOB_TEST_DEVICE_IDS.split(',').map { it.trim() }.filter { it.isNotEmpty() })
                // Debug: the phone running this build is always a test device, so developer
                // impressions and clicks can never count as invalid traffic.
                if (BuildConfig.DEBUG) add(ConsentManager.hashedDeviceId(context))
            }
            MobileAds.setRequestConfiguration(RequestConfiguration.Builder().setTestDeviceIds(testIds).build())
            MobileAds.initialize(context) { status ->
                Log.i(TAG, "Mobile Ads SDK initialized: " + status.adapterStatusMap.entries.joinToString { "${it.key}=${it.value.initializationState}" })
            }
        }.start()
    }

    // ------------------------------------------------------------------ interstitial

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    // The cap starts at app launch: never an interstitial on launch or in the first 3 minutes.
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
                    Log.i(TAG, "Interstitial not loaded: ${error.code} ${error.message}")
                }
            },
        )
    }

    /**
     * Shows an interstitial at a natural break, at most once every 3 minutes, only if one is
     * already loaded (never makes the user wait) and never while a storage scan is running.
     * Returns true if shown.
     */
    fun maybeShowInterstitial(activity: Activity): Boolean {
        if (!adsEnabled.value) return false
        if (scanner.state.value is ScanState.Scanning) return false
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
                    Log.i(TAG, "Rewarded not loaded: ${error.code} ${error.message}")
                }
            },
        )
    }

    /**
     * Rewarded video in front of a premium action. Pro → EARNED with no ad. Offline / no fill
     * within [waitMs] / failure to show → UNAVAILABLE, and the caller runs the action anyway.
     * Only an ad the user closes early returns DISMISSED (no reward, per AdMob policy).
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

    // ------------------------------------------------------------------ native

    /** Loads one native ad for an in-list card; [onLoaded] runs on the main thread. Caller destroys it. */
    fun loadNative(onLoaded: (NativeAd) -> Unit) {
        if (!adsEnabled.value) return
        AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                if (adsEnabled.value) onLoaded(ad) else ad.destroy()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.i(TAG, "Native not loaded: ${error.code} ${error.message}")
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT).build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    companion object {
        private const val TAG = "StoragePulseAds"
        val INTERSTITIAL_COOLDOWN_MS = TimeUnit.MINUTES.toMillis(3)
    }
}
