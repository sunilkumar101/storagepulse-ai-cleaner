package com.storagepulse.cleaner

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storagepulse.cleaner.data.settings.AccentColor
import com.storagepulse.cleaner.data.settings.SettingsRepository
import com.storagepulse.cleaner.ui.navigation.AppNavHost
import com.storagepulse.cleaner.ui.navigation.DeepLinks
import com.storagepulse.cleaner.ui.theme.StoragePulseTheme
import com.storagepulse.cleaner.ui.widgets.WidgetRefreshWorker
import androidx.lifecycle.lifecycleScope
import com.storagepulse.cleaner.ads.AdMobManager
import com.storagepulse.cleaner.data.billing.ProRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** FragmentActivity (a ComponentActivity) so BiometricPrompt can attach for the vault. */
@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var ads: AdMobManager
    @Inject lateinit var pro: ProRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark-only app: light icons on transparent bars, drawn edge-to-edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) DeepLinks.offer(intent?.getStringExtra(EXTRA_ROUTE))
        // Google UMP ad consent (EEA/UK/CH form when required). Pro users get no ads, so no form.
        lifecycleScope.launch { if (!pro.isPro.first()) ads.gatherConsent(this@MainActivity) }
        setContent {
            val accent by settings.accent.collectAsStateWithLifecycle(AccentColor.OBSIDIAN)
            StoragePulseTheme(accent) {
                AppNavHost()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinks.offer(intent.getStringExtra(EXTRA_ROUTE))
    }

    override fun onStop() {
        super.onStop()
        // The user may have cleaned something: refresh widget numbers (no scan, cheap).
        WidgetRefreshWorker.enqueue(this, scan = false)
    }

    companion object {
        const val EXTRA_ROUTE = "open_route"
    }
}
