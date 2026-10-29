package com.storagepulse.cleaner.ui.screens.treemap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.storagepulse.cleaner.R
import com.storagepulse.cleaner.data.storage.FileClassifier
import com.storagepulse.cleaner.ui.theme.Emerald
import com.storagepulse.cleaner.ui.theme.EmeraldLight
import com.storagepulse.cleaner.ui.theme.IndigoLight
import com.storagepulse.cleaner.ui.theme.SurfaceCard
import com.storagepulse.cleaner.ui.theme.SurfaceRaised
import com.storagepulse.cleaner.ui.theme.SurfaceStroke
import com.storagepulse.cleaner.ui.theme.Telemetry
import com.storagepulse.cleaner.ui.theme.TextPrimary
import com.storagepulse.cleaner.ui.theme.TextSecondary
import com.storagepulse.cleaner.ui.util.formatBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class PreviewEntry(
    val file: File,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
)

private const val MAX_ENTRIES = 300

/** "View": what is inside a folder (largest first), with thumbnails for photos and videos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderPreviewSheet(
    path: String,
    title: String,
    sizeOf: (String) -> Long?,
    onOpenFolder: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val entries by produceState<List<PreviewEntry>?>(null, path) {
        value = withContext(Dispatchers.IO) {
            val target = File(path)
            val files = if (target.isDirectory) target.listFiles()?.toList().orEmpty() else listOf(target)
            files.map { f ->
                PreviewEntry(
                    file = f,
                    isDirectory = f.isDirectory,
                    sizeBytes = if (f.isDirectory) sizeOf(f.path) ?: 0L else f.length(),
                    lastModified = f.lastModified(),
                )
            }.sortedByDescending { it.sizeBytes }.take(MAX_ENTRIES)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceCard,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                stringResource(R.string.preview_title, title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(path, style = Telemetry.sm, color = TextSecondary, maxLines = 1, overflow = TextOverflow.StartEllipsis)
            Spacer(Modifier.size(12.dp))

            val list = entries
            when {
                list == null -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = Emerald, trackColor = SurfaceStroke)
                list.isEmpty() -> Text(stringResource(R.string.preview_empty), color = TextSecondary, modifier = Modifier.padding(vertical = 24.dp))
                else -> LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(list, key = { it.file.path }) { e ->
                        PreviewRow(
                            entry = e,
                            onClick = if (e.isDirectory) ({ onOpenFolder(e.file.path) }) else null,
                        )
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
private fun PreviewRow(entry: PreviewEntry, onClick: (() -> Unit)?) {
    val ext = FileClassifier.extensionOf(entry.file.name)
    val visual = !entry.isDirectory && (FileClassifier.isPhoto(ext) || FileClassifier.isVideo(ext))
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            if (visual) {
                AsyncImage(
                    model = entry.file,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    if (entry.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Description,
                    contentDescription = if (entry.isDirectory) stringResource(R.string.preview_folder) else null,
                    tint = if (entry.isDirectory) EmeraldLight else IndigoLight,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.file.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
            Text(ageText(entry.lastModified), style = Telemetry.sm, color = TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        Text(formatBytes(entry.sizeBytes), style = Telemetry.sm, color = TextPrimary, maxLines = 1)
        if (onClick != null) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = stringResource(R.string.preview_open_folder, entry.file.name),
                tint = TextSecondary,
            )
        }
    }
}
