package app.alkahf.ui.tawqit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.data.SurahOption
import app.alkahf.data.TawqitSourceType
import app.alkahf.data.TawqitTrack
import app.alkahf.ui.theme.AlkahfColors
import kotlinx.coroutines.launch

@Composable
fun TawqitHubScreen(
    onBack: () -> Unit,
    onOpenTrack: (TawqitTrack) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()

    var tracks by remember { mutableStateOf<List<TawqitTrack>>(emptyList()) }
    var surahs by remember { mutableStateOf<List<SurahOption>>(emptyList()) }
    var reciterName by remember { mutableStateOf("Ḥuṣarī") }
    var reciterPath by remember { mutableStateOf("Husary_128kbps") }
    var sheetOpen by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    androidx.compose.runtime.LaunchedEffect(refreshKey) {
        tracks = repository.tawqitTracks()
        surahs = repository.surahOptions()
        reciterPath = repository.activeReciterPath
        reciterName = repository.activeReciter.displayName
    }

    Column(Modifier.fillMaxSize().background(AlkahfColors.Paper).statusBarsPadding()) {
        HubTopBar(onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            ExplainerCard()
            Text(
                text = "YOUR TIMING TRACKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkFooter,
                modifier = Modifier.padding(start = 4.dp, top = 18.dp, bottom = 10.dp),
            )
            if (tracks.isEmpty()) {
                Text(
                    text = "No timing tracks yet",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            tracks.forEach { track -> TrackRow(track) { onOpenTrack(track) } }
            NewTrackButton { sheetOpen = true }
            Spacer(Modifier.height(18.dp))
        }
    }

    if (sheetOpen) {
        NewTrackSheet(
            surahs = surahs,
            reciterName = reciterName,
            reciterPath = reciterPath,
            onDismiss = { sheetOpen = false },
            onStart = { draft -> sheetOpen = false; onOpenTrack(draft) },
        )
    }
}

@Composable
private fun HubTopBar(onBack: () -> Unit) {
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
            Text(text = "Tawqīt", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.Ink)
            Text(
                text = "Sync any recitation to the page",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
            )
        }
        Box(Modifier.size(40.dp))
    }
}

@Composable
private fun ExplainerCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.SurfaceHero,
        border = BorderStroke(1.dp, AlkahfColors.CardBorderHero),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(34.dp).background(AlkahfColors.AccentTint, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                WaveformIcon()
            }
            Text(
                text = "Play any recitation and tap at each āyah end. Tawqīt builds the timing so the page highlights in sync — even for continuous (waṣl) recordings.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
                color = AlkahfColors.TawqitCurrentInk,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun WaveformIcon(color: Color = AlkahfColors.AccentDeep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp),
    ) {
        listOf(7.dp, 13.dp, 16.dp, 10.dp, 6.dp).forEach { h ->
            Box(Modifier.width(2.dp).height(h).background(color, RoundedCornerShape(1.dp)))
        }
    }
}

@Composable
private fun TrackRow(track: TawqitTrack, onClick: () -> Unit) {
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
                Modifier.size(42.dp).background(
                    if (track.complete) AlkahfColors.AccentTint2 else AlkahfColors.SegmentedTrack,
                    RoundedCornerShape(11.dp),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (track.complete) Icons.Outlined.Check else Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = if (track.complete) AlkahfColors.AccentDeep else AlkahfColors.StumbleInk,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Text(
                    text = "${track.surahNameLatin} · ${track.ayahFrom}–${track.ayahTo}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = if (track.complete) {
                        "${track.sourceLabel} · ${track.endTimesMs.size} āyāt synced"
                    } else {
                        "${track.sourceLabel} · ${track.endTimesMs.size} of ${track.ayahCount} · resume"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = AlkahfColors.Chevron,
            )
        }
    }
}

@Composable
private fun NewTrackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 7.dp)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = AlkahfColors.SecondaryButtonBorder,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+  New timing track",
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.AccentDeep,
        )
    }
}

@Composable
private fun NewTrackSheet(
    surahs: List<SurahOption>,
    reciterName: String,
    reciterPath: String,
    onDismiss: () -> Unit,
    onStart: (TawqitTrack) -> Unit,
) {
    val context = LocalContext.current
    var useImport by remember { mutableStateOf(true) }
    var importUri by remember { mutableStateOf<String?>(null) }
    var importName by remember { mutableStateOf<String?>(null) }
    var surahIndex by remember { mutableStateOf(surahs.indexOfFirst { it.number == 18 }.coerceAtLeast(0)) }
    var ayahFrom by remember { mutableStateOf(1) }
    var ayahTo by remember { mutableStateOf(10) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            importUri = uri.toString()
            importName = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported audio"
        }
    }

    val surah = surahs.getOrNull(surahIndex)

    fun buildDraft(): TawqitTrack? {
        val s = surah ?: return null
        return if (useImport) {
            val uri = importUri ?: return null
            TawqitTrack(
                sourceType = TawqitSourceType.IMPORT,
                sourceRef = uri,
                sourceLabel = "${importName ?: "Imported"} (imported)",
                surah = s.number,
                surahNameLatin = s.nameLatin,
                ayahFrom = ayahFrom,
                ayahTo = ayahTo,
                endTimesMs = emptyList(),
                globalOffsetMs = 0,
                complete = false,
            )
        } else {
            TawqitTrack(
                sourceType = TawqitSourceType.RECITER,
                sourceRef = reciterPath,
                sourceLabel = reciterName,
                surah = s.number,
                surahNameLatin = s.nameLatin,
                ayahFrom = ayahFrom,
                ayahTo = ayahTo,
                endTimesMs = emptyList(),
                globalOffsetMs = 0,
                complete = false,
            )
        }
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = AlkahfColors.Paper,
            modifier = Modifier.fillMaxWidth().clickable(enabled = false) {},
        ) {
            Column(
                Modifier.navigationBarsPadding()
                    .padding(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 22.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.width(38.dp).height(4.dp).background(AlkahfColors.DashedNode, RoundedCornerShape(2.dp)))
                }
                Text(text = "New timing track", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.Ink)
                Text(
                    text = "Choose the audio, then the portion to sync.",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
                )
                SheetCaption("AUDIO SOURCE")
                SourceRow(
                    selected = useImport,
                    title = "Import audio file",
                    subtitle = importName?.let { "$it selected" } ?: "MP3 or M4A from your device",
                    onClick = { useImport = true },
                )
                Spacer(Modifier.height(8.dp))
                SourceRow(
                    selected = !useImport,
                    title = "Downloaded reciter",
                    subtitle = "$reciterName · already on device",
                    onClick = { useImport = false },
                )
                Spacer(Modifier.height(14.dp))
                SheetCaption("PORTION")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlkahfColors.Surface,
                        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = surah?.nameLatin ?: "—",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AlkahfColors.Ink,
                                modifier = Modifier.weight(1f),
                            )
                            StepCircle("−") {
                                if (surahIndex > 0) { surahIndex--; ayahFrom = 1; ayahTo = 10 }
                            }
                            Spacer(Modifier.width(6.dp))
                            StepCircle("+") {
                                if (surahIndex < surahs.lastIndex) { surahIndex++; ayahFrom = 1; ayahTo = 10 }
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AlkahfColors.ChipBg,
                        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
                        modifier = Modifier.width(140.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            StepCircle("−") {
                                if (ayahTo > ayahFrom) ayahTo-- else if (ayahFrom > 1) ayahFrom--
                            }
                            Text(
                                text = "$ayahFrom – $ayahTo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlkahfColors.Ink,
                            )
                            StepCircle("+") {
                                val max = surah?.ayahCount ?: 286
                                if (ayahTo < max) ayahTo++
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                val ready = surah != null && (!useImport || importUri != null)
                Surface(
                    onClick = {
                        if (useImport && importUri == null) {
                            picker.launch(arrayOf("audio/*"))
                        } else {
                            buildDraft()?.let(onStart)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (ready || useImport) AlkahfColors.Accent else AlkahfColors.NotStarted,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                useImport && importUri == null -> "Choose file & start tagging"
                                else -> "Start tagging"
                            },
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlkahfColors.OnAccent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetCaption(text: String) {
    Text(
        text = text,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = AlkahfColors.InkFooter,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SourceRow(selected: Boolean, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = if (selected) AlkahfColors.SurfaceHero else AlkahfColors.Surface,
        border = BorderStroke(1.dp, if (selected) AlkahfColors.CardBorderHero else AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AlkahfColors.Ink)
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            Box(
                Modifier.size(22.dp).background(Color.Transparent, CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = if (selected) AlkahfColors.Accent else AlkahfColors.DashedNode,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                        if (selected) drawCircle(color = AlkahfColors.Accent, radius = size.minDimension / 4)
                    },
            )
        }
    }
}

@Composable
private fun StepCircle(glyph: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
        modifier = Modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = glyph, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkChrome)
        }
    }
}
