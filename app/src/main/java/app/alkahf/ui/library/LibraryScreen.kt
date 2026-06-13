package app.alkahf.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.LoopPreset
import app.alkahf.data.QuranRepository
import app.alkahf.data.ReciterStatus
import app.alkahf.data.StorageInfo
import app.alkahf.ui.components.AlkahfBottomNav
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.theme.AlkahfColors
import kotlinx.coroutines.launch
import app.alkahf.ui.theme.AmiriQuran

@Composable
fun LibraryScreen(
    onOpenPreset: (Long) -> Unit = {},
    onNewPreset: () -> Unit = {},
    onManageReciter: (ReciterStatus) -> Unit = {},
    onSelectTab: (AlkahfTab) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()

    var storage by remember { mutableStateOf<StorageInfo?>(null) }
    var reciters by remember { mutableStateOf<List<ReciterStatus>>(emptyList()) }
    var activeReciterPath by remember { mutableStateOf(repository.activeReciterPath) }
    var presets by remember { mutableStateOf<List<LoopPreset>>(emptyList()) }
    var showNewReciter by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(refreshKey, activeReciterPath) {
        storage = repository.storageInfo()
        reciters = repository.reciterStatuses()
        presets = repository.presets()
    }

    Scaffold(
        containerColor = AlkahfColors.Paper,
        bottomBar = { AlkahfBottomNav(selected = AlkahfTab.LIBRARY, onSelect = onSelectTab) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            LibraryHeader()
            storage?.let { StorageMeter(it) }
            SectionCaption(stringResource(R.string.library_section_reciters))
            reciters.forEach { reciter ->
                ReciterRow(
                    reciter = reciter,
                    onActivate = {
                        if (reciter.isImported) {
                            onManageReciter(reciter)
                        } else {
                            repository.setActiveReciter(reciter.key)
                            activeReciterPath = reciter.key
                        }
                    },
                    onManage = { onManageReciter(reciter) },
                )
            }
            NewReciterButton { showNewReciter = true }
            SectionCaption(stringResource(R.string.library_section_drill_presets))
            presets.forEach { preset ->
                PresetRow(preset = preset, onClick = { onOpenPreset(preset.id) })
            }
            NewPresetButton(onClick = onNewPreset)
            Box(Modifier.height(12.dp))
        }
    }

    if (showNewReciter) {
        NewReciterDialog(
            onDismiss = { showNewReciter = false },
            onCreate = { name ->
                scope.launch {
                    repository.createCustomReciter(name)
                    showNewReciter = false
                    refreshKey++
                }
            },
        )
    }
}

@Composable
private fun NewReciterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 2.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = AlkahfColors.DashedNode,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.library_import_reciter_button),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkMuted,
        )
    }
}

@Composable
private fun NewReciterDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AlkahfColors.Surface,
        title = { Text(stringResource(R.string.library_import_reciter_title), fontWeight = FontWeight.Bold, color = AlkahfColors.Ink) },
        text = {
            Column {
                Text(
                    stringResource(R.string.library_import_reciter_message),
                    fontSize = 13.sp,
                    color = AlkahfColors.InkSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.library_import_reciter_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.common_create), color = AlkahfColors.AccentDeep, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = AlkahfColors.InkMuted)
            }
        },
    )
}

@Composable
private fun LibraryHeader() {
    Column(Modifier.padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.library_title),
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.Ink,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = stringResource(R.string.library_subtitle),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkFaint,
        )
    }
}

@Composable
private fun StorageMeter(storage: StorageInfo) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = stringResource(R.string.library_offline_audio),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkSecondaryDark,
                )
                Text(
                    text = stringResource(
                        R.string.library_storage_used_of_total,
                        formatBytes(storage.usedBytes),
                        formatBytes(storage.totalBytes),
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            val fraction = if (storage.totalBytes > 0) {
                (storage.usedBytes.toFloat() / storage.totalBytes).coerceIn(0.002f, 1f)
            } else {
                0f
            }
            Box(
                Modifier.fillMaxWidth().height(6.dp).padding(top = 8.dp)
                    .background(AlkahfColors.MapEmpty, RoundedCornerShape(3.dp)),
            ) {
                Box(
                    Modifier.fillMaxWidth(fraction).height(6.dp)
                        .background(AlkahfColors.Accent, RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
private fun SectionCaption(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = AlkahfColors.InkFooter,
        modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 10.dp),
    )
}

@Composable
private fun ReciterRow(
    reciter: ReciterStatus,
    onActivate: () -> Unit,
    onManage: () -> Unit,
) {
    Surface(
        onClick = onActivate,
        shape = RoundedCornerShape(18.dp),
        color = if (reciter.isActive) AlkahfColors.SurfaceHero else AlkahfColors.Surface,
        border = BorderStroke(
            1.dp,
            if (reciter.isActive) AlkahfColors.CardBorderHero else AlkahfColors.CardBorder,
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(46.dp).background(
                    if (reciter.isActive) AlkahfColors.Accent else AlkahfColors.NotStarted,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = reciter.arabicInitial,
                    fontFamily = AmiriQuran,
                    fontSize = 19.sp,
                    color = if (reciter.isActive) AlkahfColors.OnAccent else AlkahfColors.InkMuted,
                )
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(
                    text = reciter.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = reciterSubtitle(reciter),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (reciter.isActive) AlkahfColors.InkSecondary else AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            if (reciter.isActive) {
                Surface(shape = CircleShape, color = AlkahfColors.AccentTint) {
                    Text(
                        text = stringResource(R.string.library_badge_active),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.AccentDeep,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
                Box(Modifier.size(8.dp))
            }
            Surface(
                onClick = onManage,
                shape = RoundedCornerShape(11.dp),
                color = AlkahfColors.Surface,
                border = BorderStroke(1.dp, AlkahfColors.Chevron),
            ) {
                Text(
                    text = if (reciter.isImported) stringResource(R.string.library_action_open)
                    else stringResource(R.string.library_action_manage),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkChrome,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun reciterSubtitle(reciter: ReciterStatus): String = when {
    reciter.isImported && reciter.itemCount > 0 -> stringResource(
        R.string.library_reciter_subtitle_imported_count,
        pluralStringResource(R.plurals.library_surah_count, reciter.itemCount, reciter.itemCount),
    )
    reciter.isImported -> stringResource(R.string.library_reciter_subtitle_imported_empty)
    reciter.itemCount > 0 -> stringResource(
        R.string.library_reciter_subtitle_downloaded,
        reciter.itemCount,
        formatBytes(reciter.bytes),
    )
    else -> stringResource(R.string.library_reciter_subtitle_not_downloaded)
}

@Composable
private fun PresetRow(preset: LoopPreset, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).background(AlkahfColors.PlayTile, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AlkahfColors.PlayTileInk,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(
                    text = preset.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = stringResource(
                        R.string.library_preset_subtitle,
                        preset.reciterName,
                        formatFactor(preset.gapMultiplier),
                        formatFactor(preset.speed),
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            if (preset.isDefault) {
                Surface(shape = CircleShape, color = AlkahfColors.AccentTint) {
                    Text(
                        text = stringResource(R.string.library_badge_default),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.AccentDeep,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AlkahfColors.Chevron,
                )
            }
        }
    }
}

@Composable
private fun NewPresetButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = AlkahfColors.DashedNode,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = AlkahfColors.InkMuted,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.library_new_preset),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkMuted,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

private fun formatFactor(value: Float): String =
    if (value == value.toInt().toFloat()) "${value.toInt()}×" else "$value×"

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
