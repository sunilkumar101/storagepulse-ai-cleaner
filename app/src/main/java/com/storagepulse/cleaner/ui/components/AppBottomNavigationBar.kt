package com.storagepulse.cleaner.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.ui.navigation.Routes
import com.storagepulse.cleaner.ui.theme.ObsidianCanvas
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.TextMuted

enum class BottomTab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    // Icons match the Stitch nav: speed, account_tree, auto_awesome, lock, build_circle.
    Dashboard(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Speed),
    Treemap(Routes.TREEMAP, R.string.nav_treemap, Icons.Outlined.AccountTree),
    AiClean(Routes.AI_CLEAN, R.string.nav_ai_clean, Icons.Outlined.AutoAwesome),
    Gallery(Routes.GALLERY, R.string.nav_gallery, Icons.Outlined.Lock),
    Tools(Routes.TOOLS, R.string.nav_tools, Icons.Outlined.BuildCircle),
}

/** 5-tab bottom bar. Active tab uses the telemetry accent (Emerald by default); pads itself above the system navigation bar. */
@Composable
fun AppBottomNavigationBar(
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianCanvas)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(thickness = 1.dp, color = SurfaceStroke)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomTab.entries.forEach { tab ->
                val selected = tab.route == currentRoute
                val tint = if (selected) MaterialTheme.colorScheme.primaryContainer else TextMuted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onTabSelected(tab) },
                        )
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(tab.label),
                        style = MaterialTheme.typography.labelMedium,
                        color = tint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Active-tab dot, as in the design.
                    Spacer(Modifier.height(3.dp))
                    Spacer(
                        Modifier
                            .size(4.dp)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape),
                    )
                }
            }
        }
    }
}
