package com.storagepulse.cleaner.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * One-line text that shrinks (down to [minSize]) before it would ever clip.
 * Use for badges, tile titles and other fixed-width labels on narrow (360dp) phones.
 */
@Composable
fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minSize: TextUnit = 8.sp,
) {
    val max = if (style.fontSize != TextUnit.Unspecified) style.fontSize else 14.sp
    BasicText(
        text = text,
        style = style.copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = minSize, maxFontSize = max, stepSize = 0.5.sp),
        modifier = modifier,
    )
}
