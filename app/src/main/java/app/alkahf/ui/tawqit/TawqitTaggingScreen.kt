package app.alkahf.ui.tawqit

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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.PageAyah
import app.alkahf.data.TawqitSourceType
import app.alkahf.data.TawqitTrack
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.quranFont
import kotlinx.coroutines.launch

@Composable
fun TawqitTaggingScreen(
    draft: TawqitTrack,
    onSaved: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()
    val controller = remember { TawqitController(ExoPlayer.Builder(context).build(), scope) }

    LaunchedEffect(draft.id) {
        val ayahs = repository.ayahsForRange(draft.surah, draft.ayahFrom, draft.ayahTo)
        val uris = when (draft.sourceType) {
            TawqitSourceType.IMPORT -> List(ayahs.size) { draft.sourceRef }.take(1)
            TawqitSourceType.RECITER ->
                repository.reciterAyahUris(draft.sourceRef, draft.surah, draft.ayahFrom, draft.ayahTo)
        }
        controller.load(
            ayahs = ayahs,
            uris = uris,
            existingEndTimes = draft.endTimesMs,
            offsetMs = draft.globalOffsetMs,
            speed = 0.75f,
        )
    }
    DisposableEffect(Unit) { onDispose { controller.release() } }
    val state by controller.state.collectAsState()

    fun save() {
        scope.launch {
            repository.saveTawqitTrack(
                draft.copy(
                    endTimesMs = state.endTimesMs,
                    globalOffsetMs = state.globalOffsetMs,
                    complete = state.isComplete,
                ),
            )
            onSaved()
        }
    }

    Column(Modifier.fillMaxSize().background(AlkahfColors.PageSurface).statusBarsPadding()) {
        TaggingTopBar(draft = draft, onClose = onClose, onSave = ::save)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.tawqit_header_progress,
                    (state.currentIndex + 1).coerceAtMost(state.ayahs.size),
                    state.ayahs.size,
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkFooter,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
            state.ayahs.forEachIndexed { index, ayah ->
                when {
                    index < state.endTimesMs.size -> TaggedAyahRow(ayah, state.endTimesMs[index])
                    index == state.currentIndex -> CurrentAyahCard(ayah, index + 1)
                    index == state.currentIndex + 1 -> NextAyahRow(ayah)
                    else -> Spacer(Modifier.height(0.dp))
                }
            }
        }
        TaggingDock(state, controller)
    }
}

@Composable
private fun TaggingTopBar(draft: TawqitTrack, onClose: () -> Unit, onSave: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = AlkahfColors.InkChrome,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.tawqit_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = stringResource(
                        R.string.tawqit_source_subtitle,
                        draft.sourceLabel,
                        draft.surahNameLatin,
                        draft.ayahFrom,
                        draft.ayahTo,
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            Box(
                modifier = Modifier.size(54.dp).clickable(onClick = onSave),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.common_save),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.AccentDeep,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline)
    }
}

@Composable
private fun TaggedAyahRow(ayah: PageAyah, endTimeMs: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(0.7f),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(shape = RoundedCornerShape(7.dp), color = AlkahfColors.AccentTint2) {
            Text(
                text = formatClock(endTimeMs),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.AccentDeep,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
        AyahLine(ayah, fontSize = 18, color = AlkahfColors.TaggedText)
    }
}

@Composable
private fun CurrentAyahCard(ayah: PageAyah, ayahNumber: Int) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.SurfaceHero,
        border = BorderStroke(1.dp, AlkahfColors.CardBorderHero),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(AlkahfColors.Accent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.tawqit_now_playing_ayah, ayahNumber),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = AlkahfColors.AccentDeep,
                )
            }
            Spacer(Modifier.height(8.dp))
            AyahLine(ayah, fontSize = 25, color = AlkahfColors.TawqitCurrentInk)
        }
    }
}

@Composable
private fun NextAyahRow(ayah: PageAyah) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(0.5f)) {
        AyahLine(ayah, fontSize = 18, color = AlkahfColors.ConcealedInk)
    }
}

@Composable
private fun AyahLine(ayah: PageAyah, fontSize: Int, color: Color) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = "${ayah.words.joinToString(" ")} ${ayah.marker}",
            fontFamily = quranFont,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.9f).sp,
            color = color,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TaggingDock(state: TawqitUiState, controller: TawqitController) {
    Column(Modifier.fillMaxWidth().background(AlkahfColors.NavSurface)) {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.DockBorder)
        Column(
            Modifier.navigationBarsPadding()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Waveform(state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatClock(state.positionMs)} / ${formatClock(state.durationMs)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Surface(
                    onClick = controller::cycleSpeed,
                    shape = RoundedCornerShape(8.dp),
                    color = AlkahfColors.ChipBg,
                    border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
                ) {
                    Text(
                        text = stringResource(R.string.tawqit_tagging_at_speed, formatSpeed(state.speed)),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkSecondaryDark,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockSquare(onClick = controller::undo, enabled = state.endTimesMs.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = stringResource(R.string.tawqit_undo),
                        tint = AlkahfColors.InkChrome,
                        modifier = Modifier.size(22.dp),
                    )
                }
                DockSquare(onClick = controller::playPause) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.common_play_pause),
                        tint = AlkahfColors.InkChrome,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Surface(
                    onClick = controller::markAyahEnd,
                    shape = RoundedCornerShape(15.dp),
                    color = if (state.isComplete) AlkahfColors.NotStarted else AlkahfColors.Accent,
                    modifier = Modifier.weight(1f).height(56.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(7.dp).background(AlkahfColors.OnAccent, CircleShape))
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = if (state.isComplete) {
                                stringResource(R.string.tawqit_all_ayat_tagged)
                            } else {
                                stringResource(R.string.tawqit_mark_ayah_end)
                            },
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlkahfColors.OnAccent,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (state.isComplete) {
                        stringResource(R.string.tawqit_hint_nudge_then_save)
                    } else {
                        stringResource(R.string.tawqit_hint_tap_when_ayah_finishes, state.currentIndex + 1)
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFooter,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.tawqit_nudge_all),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = AlkahfColors.InkFooter,
                    )
                    Spacer(Modifier.width(6.dp))
                    NudgeButton("−") { controller.nudgeOffset(-50) }
                    Text(
                        text = formatOffset(state.globalOffsetMs),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.Ink,
                        modifier = Modifier.width(54.dp),
                        textAlign = TextAlign.Center,
                    )
                    NudgeButton("+") { controller.nudgeOffset(50) }
                }
            }
        }
    }
}

@Composable
private fun Waveform(state: TawqitUiState) {
    val barCount = 56
    val fraction = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .background(AlkahfColors.WaveformBg, RoundedCornerShape(11.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(barCount) { i ->
            // Deterministic pseudo-random heights so the bar strip reads as audio.
            val h = (6 + (i * 7 + 13) % 22).dp
            val played = i.toFloat() / barCount <= fraction
            Box(
                Modifier.weight(1f).height(h).background(
                    if (played) AlkahfColors.WaveformPlayed else AlkahfColors.WaveformUnplayed,
                    RoundedCornerShape(1.dp),
                ),
            )
        }
    }
}

@Composable
private fun DockSquare(onClick: () -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Surface(
        onClick = { if (enabled) onClick() },
        shape = RoundedCornerShape(15.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
        modifier = Modifier.size(54.dp).alpha(if (enabled) 1f else 0.4f),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun NudgeButton(glyph: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
        modifier = Modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = glyph, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkChrome)
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${"%02d".format(totalSeconds % 60)}"
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "$speed×"

private fun formatOffset(ms: Long): String {
    val sign = if (ms >= 0) "+" else "−"
    return "$sign${"%.2f".format(kotlin.math.abs(ms) / 1000.0)}s"
}
