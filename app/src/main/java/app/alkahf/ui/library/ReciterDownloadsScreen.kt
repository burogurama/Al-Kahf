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
import app.alkahf.data.DownloadedSurah
import app.alkahf.ui.theme.AlkahfColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ReciterDownloadsScreen(
    reciterPath: String,
    reciterName: String,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()

    var surahs by remember { mutableStateOf<List<DownloadedSurah>>(emptyList()) }
    val progress = remember { mutableStateMapOf<Int, Float>() }
    var bulkJob by remember { mutableStateOf<Job?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        surahs = repository.surahDownloadStates(reciterPath)
    }

    fun downloadOne(surah: Int) {
        scope.launch {
            progress[surah] = 0f
            repository.downloadSurah(reciterPath, surah) { p -> progress[surah] = p }
            progress.remove(surah)
            refreshKey++
        }
    }

    fun downloadAll() {
        bulkJob = scope.launch {
            for (item in surahs) {
                if (item.downloadedAyahs >= item.totalAyahs) continue
                progress[item.surah] = 0f
                repository.downloadSurah(reciterPath, item.surah) { p -> progress[item.surah] = p }
                progress.remove(item.surah)
                refreshKey++
            }
            bulkJob = null
        }
    }

    val downloadedCount = surahs.count { it.downloadedAyahs >= it.totalAyahs && it.totalAyahs > 0 }
    val allDone = downloadedCount == surahs.size && surahs.isNotEmpty()

    Column(
        Modifier.fillMaxSize().background(AlkahfColors.Paper).statusBarsPadding(),
    ) {
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
                Text(
                    text = reciterName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = "$downloadedCount of ${surahs.size} sūrahs downloaded",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                )
            }
            Box(Modifier.size(40.dp))
        }

        // Download-all / cancel control.
        Surface(
            onClick = {
                val running = bulkJob
                if (running != null) {
                    running.cancel()
                    bulkJob = null
                } else if (!allDone) {
                    downloadAll()
                }
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 18.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(surahs, key = { it.surah }) { surah ->
                SurahDownloadRow(
                    surah = surah,
                    progress = progress[surah.surah],
                    onDownload = { downloadOne(surah.surah) },
                    onDelete = {
                        scope.launch {
                            repository.deleteSurahAudio(reciterPath, surah.surah)
                            refreshKey++
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SurahDownloadRow(
    surah: DownloadedSurah,
    progress: Float?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    val complete = surah.downloadedAyahs >= surah.totalAyahs && surah.totalAyahs > 0
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
                Text(
                    text = "${surah.surah}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.InkMuted,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = surah.nameLatin,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = when {
                        complete -> "Downloaded · ${formatBytes2(surah.bytes)}"
                        surah.downloadedAyahs > 0 -> "${surah.downloadedAyahs} of ${surah.totalAyahs} āyāt"
                        else -> "${surah.totalAyahs} āyāt"
                    },
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
                complete -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(28.dp).background(AlkahfColors.AccentTint2, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Downloaded",
                            tint = AlkahfColors.AccentDeep,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Box(
                        modifier = Modifier.size(34.dp).clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = AlkahfColors.Chevron,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                else -> Surface(
                    onClick = onDownload,
                    shape = CircleShape,
                    color = AlkahfColors.Surface,
                    border = BorderStroke(1.dp, AlkahfColors.Chevron),
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Download",
                            tint = AlkahfColors.InkChrome,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes2(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
