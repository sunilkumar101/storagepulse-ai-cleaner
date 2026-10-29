package com.storagepulse.cleaner.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.storagepulse.cleaner.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google User Messaging Platform (UMP): GDPR (EEA/UK/CH) and US-state privacy messages.
 * [gather] runs on every launch for free users; ads may be requested only when [canRequestAds].
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _canRequestAds = MutableStateFlow(info.canRequestAds())
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** Settings must offer "Ad privacy choices" when this is true. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /**
     * Refreshes consent and shows Google's form only when it is required. [onReady] runs once the
     * flow ends (form closed, not required, or offline: the previous decision is kept).
     */
    fun gather(activity: Activity, onReady: () -> Unit) {
        if (info.canRequestAds()) publish(onReady) // returning user: start ads without waiting
        info.requestConsentInfoUpdate(
            activity,
            requestParameters(activity),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                    publish(onReady)
                }
            },
            { error ->
                Log.w(TAG, "Consent update failed: ${error.message}")
                publish(onReady)
            },
        )
    }

    fun showPrivacyOptions(activity: Activity, onReady: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { publish(onReady) }
    }

    private fun publish(onReady: () -> Unit) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _canRequestAds.value = info.canRequestAds()
        Log.i(TAG, "Consent: status=${info.consentStatus} canRequestAds=${info.canRequestAds()} privacyOptions=${_privacyOptionsRequired.value}")
        onReady()
    }

    private fun requestParameters(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            // Debug builds: treat this phone as a test device; optionally force the EEA form.
            val debug = ConsentDebugSettings.Builder(activity).addTestDeviceHashedId(hashedDeviceId(activity))
            if (BuildConfig.ADMOB_DEBUG_EEA) debug.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            builder.setConsentDebugSettings(debug.build())
        }
        return builder.build()
    }

    companion object {
        private const val TAG = "AdsConsent"

        /** The id AdMob / UMP expect for test devices: MD5 of ANDROID_ID, upper-case hex. */
        @SuppressLint("HardwareIds")
        fun hashedDeviceId(context: Context): String {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
            return MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
                .joinToString("") { "%02X".format(it) }
        }
    }
}
