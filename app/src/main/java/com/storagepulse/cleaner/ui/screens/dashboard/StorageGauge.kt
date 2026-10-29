package com.storagepulse.cleaner.ui.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.storage.model.StorageCategory
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import kotlin.math.roundToInt

/** Neon ring that sweeps from 0 to [fraction] on first show and whenever the value changes. */
@Composable
fun StorageGauge(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    strokeWidth: Dp = 10.dp,
) {
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        sweep.animateTo(fraction.coerceIn(0f, 1f), tween(durationMillis = 1_200, easing = FastOutSlowInEasing))
    }
    val percent = (fraction * 100).roundToInt()
    val shownPercent = (sweep.value * 100).roundToInt()
    val description = stringResource(R.string.dash_gauge_desc, percent)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2 + 6.dp.toPx() // room for the glow
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = SurfaceStroke,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(stroke),
            )
            if (sweep.value > 0f) {
                rotate(-90f) {
                    val brush = Brush.sweepGradient(listOf(Emerald, EmeraldLight, Emerald))
                    // soft glow
                    drawArc(
                        color = Emerald.copy(alpha = 0.22f),
                        startAngle = 0f, sweepAngle = 360f * sweep.value, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke * 2.2f, cap = StrokeCap.Round),
                    )
                    drawArc(
                        brush = brush,
                        startAngle = 0f, sweepAngle = 360f * sweep.value, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = buildAnnotatedString {
                    append(shownPercent.toString())
                    withStyle(SpanStyle(fontSize = 16.sp, color = EmeraldLight)) { append("%") }
                },
                style = Telemetry.xl.copy(fontSize = 34.sp, lineHeight = 38.sp),
                color = TextPrimary,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.dash_full),
                style = Telemetry.labelCaps,
                color = TextSecondary,
                maxLines = 1,
            )
        }
    }
}

/** Proportional category spectrum bar; segments grow in when the scan result arrives. */
@Composable
fun CategorySpectrumBar(
    bytes: Map<StorageCategory, Long>,
    modifier: Modifier = Modifier,
) {
    val total = bytes.values.sum().coerceAtLeast(1L)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(SurfaceStroke),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        StorageCategory.entries.forEach { category ->
            val target = (bytes[category] ?: 0L).toFloat() / total
            val weight by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(900, easing = FastOutSlowInEasing),
                label = "segment-${category.name}",
            )
            if (weight > 0.002f) {
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(Color(category.colorArgb)),
                )
            }
        }
    }
}
