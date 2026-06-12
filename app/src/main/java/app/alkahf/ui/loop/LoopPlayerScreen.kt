package app.alkahf.ui.loop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import app.alkahf.AlkahfApplication
import app.alkahf.audio.LoopMode
import app.alkahf.data.SurahOption
import app.alkahf.data.audio.AudioStore
import app.alkahf.data.toArabicIndic
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.KfgqpcHafs

@Composable
fun LoopPlayerScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val controller = remember {
        LoopController(
            repository = repository,
            audioStore = AudioStore(context.applicationContext),
            player = ExoPlayer.Builder(context).build(),
            scope = scope,
            initialPreset = repository.loopPreset,
        )
    }
    DisposableEffect(Unit) {
        controller.start()
        onDispose { controller.release() }
    }
    val state by controller.state.collectAsState()
    var showEditor by remember { mutableStateOf(false) }
    val surahs by produceState(initialValue = emptyList<SurahOption>()) {
        value = repository.surahOptions()
    }

    if (showEditor && surahs.isNotEmpty()) {
        BackHandler { showEditor = false }
        PresetEditor(
            initial = state.toPreset(),
            surahs = surahs,
            onSave = { preset ->
                repository.loopPreset = preset
                controller.applyPreset(preset)
                showEditor = false
            },
            onDismiss = { showEditor = false },
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.SessionBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        LoopTopBar(state, onBack, onOpenEditor = { showEditor = true })
        Column(Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
            ChainCard(state, controller)
            AyahCard(state, Modifier.weight(1f).padding(top = 11.dp))
            ProgressSection(state)
            TransportRow(state, controller)
            PresetStrip(state, onOpenEditor = { showEditor = true })
        }
    }
}

@Composable
private fun LoopTopBar(state: LoopUiState, onBack: () -> Unit, onOpenEditor: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Close player",
                tint = AlkahfColors.InkChrome,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "DRILL SESSION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = AlkahfColors.InkFooter,
            )
            Text(
                text = "Sūrat ${state.surahLatin.ifEmpty { "…" }} · ${state.reciterName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                letterSpacing = (-0.2).sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier.size(40.dp).clickable(onClick = onOpenEditor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Preset settings",
                tint = AlkahfColors.InkChrome,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ChainCard(state: LoopUiState, controller: LoopController) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                Text(
                    text = "LOOP MODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AlkahfColors.InkFooter,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "ĀYĀT TO LOOP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AlkahfColors.InkFooter,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(124.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeSegmentedControl(state.mode, controller::setMode, Modifier.weight(1f))
                RangeStepper(state, controller)
            }
            NodeTrack(state)
            StatusRow(state)
        }
    }
}

@Composable
private fun ModeSegmentedControl(
    selected: LoopMode,
    onSelect: (LoopMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(AlkahfColors.SegmentedTrack, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        listOf(
            LoopMode.SINGLE to "Single",
            LoopMode.RANGE to "Range",
            LoopMode.CHAIN to "Chain",
        ).forEach { (mode, label) ->
            val isSelected = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .let {
                        if (isSelected) {
                            it.shadow(2.dp, RoundedCornerShape(9.dp))
                                .background(AlkahfColors.SegmentedSelected, RoundedCornerShape(9.dp))
                        } else {
                            it
                        }
                    }
                    .clickable { onSelect(mode) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) AlkahfColors.AccentDeep else AlkahfColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun RangeStepper(state: LoopUiState, controller: LoopController) {
    Row(
        modifier = Modifier
            .width(124.dp)
            .height(40.dp)
            .background(AlkahfColors.PageSurface, RoundedCornerShape(12.dp))
            .stepperBorder(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(38.dp).fillMaxHeight()
                .clickable { controller.adjustRangeEnd(-1) },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "−", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.InkMuted)
        }
        Text(
            text = rangeLabel(state),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.Ink,
            letterSpacing = 0.2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier.width(38.dp).fillMaxHeight()
                .clickable { controller.adjustRangeEnd(1) },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.AccentDeep)
        }
    }
}

private fun rangeLabel(state: LoopUiState): String =
    if (state.rangeStart == state.rangeEnd) "${state.rangeStart}"
    else "${state.rangeStart}–${state.rangeEnd}"

private fun Modifier.stepperBorder(): Modifier =
    drawBehind {
        drawRoundRect(
            color = AlkahfColors.ControlBorder,
            style = Stroke(width = 1.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
        )
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeTrack(state: LoopUiState) {
    val inChain: (Int) -> Boolean = { ayah ->
        when (state.mode) {
            LoopMode.SINGLE -> ayah == state.rangeStart
            else -> ayah in state.spanStart..state.spanEnd
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            (state.rangeStart..state.rangeEnd).forEach { ayah ->
                if (ayah != state.rangeStart) {
                    Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                        LinkBar(solid = inChain(ayah) && inChain(ayah - 1))
                    }
                }
                Box(Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                    AyahNode(
                        number = ayah,
                        built = inChain(ayah),
                        sounding = ayah == state.currentAyah &&
                            (state.phase == LoopPhase.PLAYING || state.phase == LoopPhase.GAP),
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkBar(solid: Boolean) {
    if (solid) {
        Box(
            Modifier.width(26.dp).height(3.dp)
                .background(AlkahfColors.Accent, RoundedCornerShape(2.dp)),
        )
    } else {
        Box(
            Modifier.width(26.dp).height(3.dp).drawBehind {
                drawLine(
                    color = AlkahfColors.DashedLink,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            },
        )
    }
}

@Composable
private fun AyahNode(number: Int, built: Boolean, sounding: Boolean) {
    val nodeSize = if (sounding) 40.dp else 34.dp
    val pulse = rememberInfiniteTransition(label = "soundRing")
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ringAlpha",
    )
    val ringScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "ringScale",
    )
    Box(contentAlignment = Alignment.Center) {
        if (sounding) {
            Box(
                Modifier.size(nodeSize).scale(ringScale)
                    .background(AlkahfColors.Accent.copy(alpha = ringAlpha), CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(nodeSize)
                .let {
                    if (built) {
                        it.background(AlkahfColors.Accent, CircleShape)
                    } else {
                        it.drawBehind {
                            drawCircle(
                                color = AlkahfColors.DashedNode,
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f)),
                                ),
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toArabicIndic(),
                fontFamily = KfgqpcHafs,
                fontSize = if (sounding) 18.sp else 16.sp,
                color = if (built) AlkahfColors.OnAccent else AlkahfColors.QueuedNumeral,
            )
        }
    }
}

@Composable
private fun StatusRow(state: LoopUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = AlkahfColors.AccentTint2) {
            Text(
                text = if (state.spanStart == state.spanEnd) {
                    "Repeating āyah ${state.spanStart}"
                } else {
                    "Repeating ${state.spanStart} → ${state.spanEnd}"
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.AccentDeep,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        Text(
            text = buildAnnotatedString {
                append("pass ")
                withStyle(SpanStyle(color = AlkahfColors.Ink, fontWeight = FontWeight.Bold)) {
                    append("${state.pass}")
                }
                append(" of ${state.passCount}")
            },
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkMuted,
        )
        Spacer(Modifier.weight(1f))
        state.nextToAdd?.let { next ->
            Text(
                text = "next · +āyah $next",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFooter,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AyahCard(state: LoopUiState, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(5.dp).background(AlkahfColors.Accent, CircleShape))
                Text(
                    text = ayahCardLabel(state),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.AccentDeep,
                )
                if (state.phase == LoopPhase.PLAYING && !state.isPaused) {
                    Equalizer()
                }
            }
            when (state.phase) {
                LoopPhase.ERROR -> Text(
                    text = state.errorMessage ?: "Something went wrong",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.StumbleInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                LoopPhase.COMPLETE -> Text(
                    text = "Drill complete — tap play to run it again",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                else -> AyahText(state)
            }
        }
    }
}

private fun ayahCardLabel(state: LoopUiState): String = when (state.phase) {
    LoopPhase.PREPARING -> "PREPARING AUDIO…"
    LoopPhase.GAP -> "RECITE BACK · ĀYAH ${state.currentAyah}"
    LoopPhase.COMPLETE -> "SESSION COMPLETE"
    LoopPhase.ERROR -> "PLAYBACK ERROR"
    LoopPhase.PLAYING -> "NOW RECITING · ĀYAH ${state.currentAyah}"
}

@Composable
private fun AyahText(state: LoopUiState) {
    val ayah = state.ayahs[state.currentAyah] ?: return
    val annotated = buildAnnotatedString {
        ayah.words.forEachIndexed { index, word ->
            val style = when {
                state.phase == LoopPhase.GAP -> SpanStyle(color = AlkahfColors.Ink)
                state.highlightIndex < 0 -> SpanStyle(color = AlkahfColors.UpcomingWord)
                index < state.highlightIndex -> SpanStyle(color = AlkahfColors.Ink)
                index == state.highlightIndex -> SpanStyle(
                    color = AlkahfColors.WordHighlightInk,
                    background = AlkahfColors.WordHighlightBg,
                )
                else -> SpanStyle(color = AlkahfColors.UpcomingWord)
            }
            withStyle(style) { append(word) }
            append(' ')
        }
        withStyle(SpanStyle(color = AlkahfColors.ConcealedMedallion)) { append(ayah.marker) }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = annotated,
            fontFamily = KfgqpcHafs,
            fontSize = 28.sp,
            lineHeight = 55.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun Equalizer() {
    val transition = rememberInfiniteTransition(label = "equalizer")
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(0, 200, 400).forEach { delayMs ->
            val scale by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(350, delayMillis = delayMs),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "eqBar$delayMs",
            )
            Box(
                Modifier.width(2.5.dp).height(12.dp * scale)
                    .background(AlkahfColors.AccentLight, RoundedCornerShape(1.dp)),
            )
        }
    }
}

@Composable
private fun ProgressSection(state: LoopUiState) {
    Column(Modifier.padding(top = 14.dp)) {
        Box(
            Modifier.fillMaxWidth().height(5.dp)
                .background(AlkahfColors.ProgressTrack, RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            val fraction = if (state.durationMs > 0) {
                (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                Modifier.fillMaxWidth(fraction).height(5.dp)
                    .background(
                        if (state.phase == LoopPhase.GAP) AlkahfColors.StumbleAmber else AlkahfColors.Accent,
                        RoundedCornerShape(3.dp),
                    ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTime(state.positionMs),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
            )
            Surface(shape = RoundedCornerShape(7.dp), color = AlkahfColors.StumbleBg) {
                Text(
                    text = if (state.phase == LoopPhase.GAP) {
                        "↻ recite back · ${formatMultiplier(state.gapMultiplier)} gap"
                    } else {
                        "↻ then ${formatMultiplier(state.gapMultiplier)} silent gap"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.StumbleAmber,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                )
            }
            Text(
                text = formatTime((state.durationMs - state.positionMs).coerceAtLeast(0)),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkChrome,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${"%02d".format(totalSeconds % 60)}"
}

private fun formatMultiplier(value: Float): String =
    if (value == value.toInt().toFloat()) "${value.toInt()}×" else "$value×"

@Composable
private fun TransportRow(state: LoopUiState, controller: LoopController) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, start = 2.dp, end = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SquareControl(label = formatMultiplier(state.speed), onClick = controller::cycleSpeed)
        Box(
            modifier = Modifier.size(52.dp).clickable(onClick = controller::previousAyah),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous ayah",
                tint = AlkahfColors.TransportGlyph,
                modifier = Modifier.size(26.dp),
            )
        }
        Surface(
            onClick = controller::togglePlayPause,
            shape = CircleShape,
            color = AlkahfColors.Accent,
            modifier = Modifier.size(76.dp).shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = AlkahfColors.Accent.copy(alpha = 0.32f),
                spotColor = AlkahfColors.Accent.copy(alpha = 0.32f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val showPlay = state.isPaused ||
                    state.phase == LoopPhase.COMPLETE || state.phase == LoopPhase.ERROR
                Icon(
                    imageVector = if (showPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (showPlay) "Play" else "Pause",
                    tint = AlkahfColors.OnAccent,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Box(
            modifier = Modifier.size(52.dp).clickable(onClick = controller::nextAyah),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next ayah",
                tint = AlkahfColors.TransportGlyph,
                modifier = Modifier.size(26.dp),
            )
        }
        SquareControl(onClick = controller::cyclePerAyah) {
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = "Per-ayah repeats",
                tint = AlkahfColors.InkChrome,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "${state.perAyah}×",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.InkChrome,
            )
        }
    }
}

@Composable
private fun SquareControl(
    label: String? = null,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
        modifier = Modifier.size(50.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (content != null) {
                content()
            } else if (label != null) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.InkChrome,
                )
            }
        }
    }
}

@Composable
private fun PresetStrip(state: LoopUiState, onOpenEditor: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PresetCard("PER ĀYAH", "${state.perAyah}×", Modifier.weight(1f), onOpenEditor)
        PresetCard("PER CHAIN", "${state.perChain}×", Modifier.weight(1f), onOpenEditor)
        PresetCard("GAP", formatMultiplier(state.gapMultiplier), Modifier.weight(1f), onOpenEditor)
        PresetCard("SPEED", "${state.speed}×", Modifier.weight(1f), onOpenEditor)
    }
}

@Composable
private fun PresetCard(
    caption: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = caption,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = AlkahfColors.InkFooter,
                maxLines = 1,
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
