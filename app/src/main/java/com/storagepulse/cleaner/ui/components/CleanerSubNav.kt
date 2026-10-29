package com.storagepulse.cleaner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.OnEmeraldLight
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary

enum class CleanerTab { SWIPE_DECK, COMPRESSOR, SOCIAL, SAFE_TRASH }

/** Swipe Deck | Video Compressor | Social Cleaner | Safe Trash — shared by the cleaner screens. */
@Composable
fun CleanerSubNav(
    active: CleanerTab,
    onSelect: (CleanerTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Tab(val tab: CleanerTab, val icon: ImageVector, val label: Int)
    val tabs = listOf(
        Tab(CleanerTab.SWIPE_DECK, Icons.Outlined.ViewCarousel, R.string.sw_tab_deck),
        Tab(CleanerTab.COMPRESSOR, Icons.Outlined.Compress, R.string.sw_tab_compressor),
        Tab(CleanerTab.SOCIAL, Icons.AutoMirrored.Outlined.Chat, R.string.sw_tab_social),
        Tab(CleanerTab.SAFE_TRASH, Icons.Outlined.DeleteSweep, R.string.sw_tab_trash),
    )
    val listState = rememberLazyListState()
    LaunchedEffect(active) { listState.scrollToItem(tabs.indexOfFirst { it.tab == active }.coerceAtLeast(0)) }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs) { t ->
            val isActive = t.tab == active
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) EmeraldLight else SurfaceCard)
                    .border(1.dp, if (isActive) EmeraldLight else SurfaceStroke, RoundedCornerShape(50))
                    .clickable(enabled = !isActive) { onSelect(t.tab) }
                    .semantics { role = Role.Tab; selected = isActive }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.icon, null, tint = if (isActive) OnEmeraldLight else TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(t.label),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (isActive) OnEmeraldLight else TextPrimary,
                    maxLines = 1,
                )
            }
        }
    }
}
