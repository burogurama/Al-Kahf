package app.alkahf.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.data.ReciterSurahItem
import app.alkahf.ui.theme.AlkahfColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ReciterDownloadsScreen(
    reciterKey: String,
    reciterName: String,
    isImported: Boolean,
    onTimeSurah: (Int) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()

    var surahs by remember { mutableStateOf<List<ReciterSurahItem>>(emptyList()) }
    val progress = remember { mutableStateMapOf<Int, Float>() }
    var bulkJob by remember { mutableStateOf<Job?>(null) }
    var refreshKey by remember { mutableStateOf(0) }
    var importTargetSurah by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) { surahs = repository.reciterSurahItems(reciterKey) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && importTargetSurah > 0) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            val surah = importTargetSurah
            scope.launch {
                repository.importSurah(reciterKey, surah, uri.toString())
                refreshKey++
            }
        }
    }

    fun downloadOne(surah: Int) {
        scope.launch {
            progress[surah] = 0f
            repository.downloadSurah(reciterKey, surah) { p -> progress[surah] = p }
            progress.remove(surah)
            refreshKey++
        }
    }

    fun downloadAll() {
        bulkJob = scope.launch {
            for (item in surahs) {
                if (item.hasAudio) continue
                progress[item.surah] = 0f
                repository.downloadSurah(reciterKey, item.surah) { p -> progress[item.surah] = p }
                progress.remove(item.surah)
                refreshKey++
            }
            bulkJob = null
        }
    }

    val readyCount = surahs.count { it.hasAudio }
    val allDone = readyCount == surahs.size && surahs.isNotEmpty()

    Column(Modifier.fillMaxSize().background(AlkahfColors.Paper).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = AlkahfColors.InkChrome,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = reciterName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.Ink)
                Text(
                    text = if (isImported) "$readyCount of ${surahs.size} sūrahs imported"
                    else "$readyCount of ${surahs.size} sūrahs downloaded",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                )
            }
            Box(Modifier.size(40.dp))
        }

        if (!isImported) {
            Surface(
                onClick = {
                    val running = bulkJob
                    if (running != null) { running.cancel(); bulkJob = null } else if (!allDone) downloadAll()
                },
                shape = RoundedCornerShape(14.dp),
                color = if (allDone) AlkahfColors.Surface else AlkahfColors.Accent,
                border = if (allDone) BorderStroke(1.dp, AlkahfColors.CardBorder) else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp).height(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when {
                            bulkJob != null -> "Cancel download"
                            allDone -> "All sūrahs downloaded"
                            else -> "Download all ${surahs.size} sūrahs"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (allDone) AlkahfColors.InkMuted else AlkahfColors.OnAccent,
                    )
                }
            }
        } else {
            Text(
                text = "Import an audio file for each sūrah, then tap “Time it” to sync it to the page.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(surahs, key = { it.surah }) { surah ->
                SurahRow(
                    surah = surah,
                    progress = progress[surah.surah],
                    onDownload = { downloadOne(surah.surah) },
                    onImport = { importTargetSurah = surah.surah; picker.launch(arrayOf("audio/*")) },
                    onTime = { onTimeSurah(surah.surah) },
                    onDelete = {
                        scope.launch {
                            if (isImported) {
                                repository.removeImportedSurah(reciterKey, surah.surah)
                            } else {
                                repository.deleteSurahAudio(reciterKey, surah.surah)
                            }
                            refreshKey++
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: ReciterSurahItem,
    progress: Float?,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onTime: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(AlkahfColors.ChipBg, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${surah.surah}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkMuted)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(surah.nameLatin, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AlkahfColors.Ink)
                Text(
                    text = subtitleFor(surah),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                )
            }
            when {
                progress != null -> CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(26.dp),
                    color = AlkahfColors.Accent,
                    strokeWidth = 2.5.dp,
                )
                surah.isImported && surah.hasAudio -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onTime,
                        shape = RoundedCornerShape(10.dp),
                        color = if (surah.timed) AlkahfColors.AccentTint2 else AlkahfColors.Accent,
                    ) {
                        Text(
                            text = if (surah.timed) "Re-time" else "Time it",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (surah.timed) AlkahfColors.AccentDeep else AlkahfColors.OnAccent,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        )
                    }
                    DeleteButton(onDelete)
                }
                surah.isImported -> Surface(
                    onClick = onImport,
                    shape = RoundedCornerShape(10.dp),
                    color = AlkahfColors.Surface,
                    border = BorderStroke(1.dp, AlkahfColors.Chevron),
                ) {
                    Text(
                        text = "Import",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkChrome,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
                surah.hasAudio -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).background(AlkahfColors.AccentTint2, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Check, "Downloaded", tint = AlkahfColors.AccentDeep, modifier = Modifier.size(16.dp))
                    }
                    DeleteButton(onDelete)
                }
                else -> Surface(
                    onClick = onDownload,
                    shape = CircleShape,
                    color = AlkahfColors.Surface,
                    border = BorderStroke(1.dp, AlkahfColors.Chevron),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Download, "Download", tint = AlkahfColors.InkChrome, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteButton(onDelete: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).clickable(onClick = onDelete),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Delete, "Delete", tint = AlkahfColors.Chevron, modifier = Modifier.size(18.dp))
    }
}

private fun subtitleFor(surah: ReciterSurahItem): String = when {
    surah.isImported && surah.hasAudio && surah.timed -> "Imported · timed"
    surah.isImported && surah.hasAudio -> "Imported · not yet timed"
    surah.isImported -> "${surah.ayahCount} āyāt · no file"
    surah.hasAudio -> "Downloaded · ${formatBytes2(surah.bytes)}"
    surah.downloadedAyahs > 0 -> "${surah.downloadedAyahs} of ${surah.ayahCount} āyāt"
    else -> "${surah.ayahCount} āyāt"
}

private fun formatBytes2(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
