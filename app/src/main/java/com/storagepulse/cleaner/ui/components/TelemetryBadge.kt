package com.storagepulse.cleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry

/** Uppercase mono pill (e.g. "OS REQUIREMENT"). Always one line. */
@Composable
fun TelemetryBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showDot) Box(Modifier.size(6.dp).background(color, CircleShape))
        AutoSizeText(text = text, style = Telemetry.labelCaps, color = color, minSize = 7.sp)
    }
}

/** Level-1 card: #111827 fill, 1px #1F2937 stroke. */
fun Modifier.obsidianCard(
    radius: Int = 16,
    fill: Color = SurfaceCard,
): Modifier = this
    .background(fill, RoundedCornerShape(radius.dp))
    .border(1.dp, SurfaceStroke, RoundedCornerShape(radius.dp))
