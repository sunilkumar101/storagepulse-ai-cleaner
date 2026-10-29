package com.storagepulse.cleaner.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.storagepulse.cleaner.ads.AdMobManager

/**
 * Native Advanced ad as an Obsidian card (#111827 glass fill, #1F2937 stroke, emerald "Ad" badge)
 * for long lists. Renders nothing for Pro users, before consent, or when no ad loads.
 */
@Composable
fun NativeAdCard(ads: AdMobManager, adsEnabled: Boolean, modifier: Modifier = Modifier) {
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    LaunchedEffect(adsEnabled) {
        if (adsEnabled && ad == null) ads.loadNative { ad = it }
        if (!adsEnabled) {
            ad?.destroy()
            ad = null
        }
    }
    DisposableEffect(Unit) { onDispose { ad?.destroy() } }
    val loaded = ad ?: return
    if (!adsEnabled) return
    AndroidView(
        factory = { NativeCardView(it).view },
        update = { (it.tag as NativeCardView).bind(loaded) },
        modifier = modifier.fillMaxWidth(),
    )
}

/** Programmatic NativeAdView: every visible asset is registered so clicks and impressions count. */
private class NativeCardView(context: Context) {
    val view = NativeAdView(context).also { it.tag = this }
    private val res = context.resources
    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), res.displayMetrics).toInt()

    private val badge = TextView(context).apply {
        setText(com.storagepulse.cleaner.R.string.ad_badge)
        setTextColor(Color.parseColor("#4EDEA3"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        typeface = Typeface.MONOSPACE
        setPadding(dp(8), dp(2), dp(8), dp(2))
        background = GradientDrawable().apply {
            cornerRadius = dp(99).toFloat()
            setColor(Color.parseColor("#1F4EDEA3"))
            setStroke(dp(1), Color.parseColor("#594EDEA3"))
        }
    }
    private val icon = ImageView(context)
    private val headline = TextView(context).apply {
        setTextColor(Color.parseColor("#DFE2EF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val advertiser = TextView(context).apply {
        setTextColor(Color.parseColor("#86948A"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        maxLines = 1
    }
    private val body = TextView(context).apply {
        setTextColor(Color.parseColor("#BBCABF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private val media = MediaView(context)
    private val cta = Button(context).apply {
        isAllCaps = false
        setTextColor(Color.parseColor("#003824"))
        typeface = Typeface.DEFAULT_BOLD
        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(Color.parseColor("#4EDEA3"))
        }
        minHeight = dp(48)
    }

    init {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            background = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(Color.parseColor("#E6111827"))
                setStroke(dp(1), Color.parseColor("#1F2937"))
            }
        }
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)).apply { marginEnd = dp(12) })
        val titles = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(headline)
        titles.addView(advertiser)
        header.addView(titles, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(badge)
        root.addView(header)
        root.addView(body, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        root.addView(media, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(150)).apply { topMargin = dp(12) })
        root.addView(cta, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(12) })
        view.addView(root)
        view.headlineView = headline
        view.bodyView = body
        view.iconView = icon
        view.advertiserView = advertiser
        view.mediaView = media
        view.callToActionView = cta
    }

    fun bind(ad: NativeAd) {
        headline.text = ad.headline
        body.text = ad.body
        body.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        advertiser.text = ad.advertiser
        advertiser.visibility = if (ad.advertiser.isNullOrBlank()) View.GONE else View.VISIBLE
        val img = ad.icon?.drawable
        icon.setImageDrawable(img)
        icon.visibility = if (img == null) View.GONE else View.VISIBLE
        cta.text = ad.callToAction
        cta.visibility = if (ad.callToAction.isNullOrBlank()) View.GONE else View.VISIBLE
        media.mediaContent = ad.mediaContent
        media.visibility = if (ad.mediaContent == null) View.GONE else View.VISIBLE
        view.setNativeAd(ad)
    }
}
