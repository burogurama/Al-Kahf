package app.alkahf.ui.review

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.data.PageAyah
import app.alkahf.data.ReviewPortion
import app.alkahf.data.WordStumble
import app.alkahf.data.review.ReviewGrade
import app.alkahf.data.review.ReviewScheduler
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.KfgqpcHafs
import kotlinx.coroutines.launch

private const val MINUTES_PER_PORTION = 1.6f

@Composable
fun ReviewScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as AlkahfApplication).repository }
    val scope = rememberCoroutineScope()

    val portions by produceState<List<ReviewPortion>?>(initialValue = null) {
        value = repository.dueReviewPortions()
    }
    var index by remember { mutableIntStateOf(0) }
    var completed by remember { mutableStateOf(false) }

    val queue = portions
    if (queue == null) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.Paper))
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val remaining = queue.size - index
        ReviewTopBar(
            subtitle = when {
                completed || queue.isEmpty() -> "Murājaʿah · done for today"
                else -> "Murājaʿah · ≈ ${minutesLabel(remaining)} left"
            },
            onBack = onBack,
        )
        if (queue.isEmpty() || completed) {
            CompletedBody(gradedCount = queue.size, onDone = onBack)
            return@Column
        }

        val portion = queue[index]
        QueueStrip(total = queue.size, currentIndex = index, surahLatin = portion.surahNameLatin)

        val revealedCounts = remember(portion.id) { mutableStateMapOf<Int, Int>() }
        val stumbles = remember(portion.id) { mutableStateListOf<WordStumble>() }
        var grade by remember(portion.id) { mutableStateOf<ReviewGrade?>(null) }
        val fullyRevealed = portion.ayahs.all {
            (revealedCounts[it.id] ?: 0) >= it.words.size
        }

        PassageCard(
            portion = portion,
            revealedCounts = revealedCounts,
            stumbles = stumbles,
            fullyRevealed = fullyRevealed,
            modifier = Modifier.weight(1f).padding(horizontal = 18.dp),
        )

        when {
            !fullyRevealed -> RevealHintDock()
            grade == null -> GradingDock(
                portion = portion,
                stumbleCount = stumbles.size,
                onGrade = { grade = it },
            )
            else -> GradedDock(
                portion = portion,
                chosenGrade = grade ?: ReviewGrade.HESITANT,
                stumbleCount = stumbles.size,
                nextPortionName = queue.getOrNull(index + 1)?.surahNameLatin,
                remainingAfter = queue.size - index - 1,
                onChange = { grade = null },
                onNext = {
                    val chosen = grade ?: return@GradedDock
                    val effective = ReviewScheduler.effectiveGrade(chosen, stumbles.size)
                    scope.launch {
                        repository.commitReviewGrade(portion, effective)
                        stumbles.forEach { repository.addStumble(it) }
                    }
                    if (index + 1 < queue.size) index++ else completed = true
                },
            )
        }
    }
}

private fun minutesLabel(remainingPortions: Int): String =
    "${(remainingPortions * MINUTES_PER_PORTION + 0.5f).toInt().coerceAtLeast(1)} min"

@Composable
private fun ReviewTopBar(subtitle: String, onBack: () -> Unit) {
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
                text = "Daily Review",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
            )
        }
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun QueueStrip(total: Int, currentIndex: Int, surahLatin: String) {
    Column(Modifier.padding(start = 22.dp, top = 2.dp, end = 22.dp, bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                val color = when {
                    i < currentIndex -> AlkahfColors.Accent
                    i == currentIndex -> AlkahfColors.Learning
                    else -> AlkahfColors.NotStarted
                }
                Box(
                    Modifier.weight(1f).height(6.dp)
                        .background(color, RoundedCornerShape(3.dp)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Portion ${currentIndex + 1} of $total · $surahLatin",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkMuted,
                maxLines = 1,
            )
            Text(
                text = "$currentIndex graded",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PassageCard(
    portion: ReviewPortion,
    revealedCounts: MutableMap<Int, Int>,
    stumbles: MutableList<WordStumble>,
    fullyRevealed: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize().padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SŪRAT ${portion.surahNameLatin.uppercase()} · ${portion.ayahFrom}–${portion.ayahTo}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.InkMuted,
                )
                if (stumbles.isNotEmpty()) {
                    Surface(shape = CircleShape, color = AlkahfColors.StumbleBg) {
                        Text(
                            text = "${stumbles.size} STUMBLE${if (stumbles.size > 1) "S" else ""}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlkahfColors.StumbleInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            PassageText(
                portion = portion,
                revealedCounts = revealedCounts,
                stumbles = stumbles,
                modifier = Modifier.verticalScroll(rememberScrollState(), enabled = false),
            )
            Spacer(Modifier.weight(1f))
            HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = AlkahfColors.InkFaint,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = if (fullyRevealed) {
                        "Revealed after recall · ${stumbleSummary(stumbles.size)}"
                    } else {
                        "Recite from memory, then reveal to check"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                )
            }
        }
    }
}

private fun stumbleSummary(count: Int): String = when (count) {
    0 -> "no stumbles"
    1 -> "1 stumble recorded"
    else -> "$count stumbles recorded"
}

@Composable
private fun PassageText(
    portion: ReviewPortion,
    revealedCounts: MutableMap<Int, Int>,
    stumbles: MutableList<WordStumble>,
    modifier: Modifier = Modifier,
) {
    val revealedSnapshot = portion.ayahs.associate { it.id to (revealedCounts[it.id] ?: 0) }
    val passage = remember(portion.id, revealedSnapshot, stumbles.size) {
        buildPassageText(portion.ayahs, revealedSnapshot)
    }
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    val stumbleAmber = AlkahfColors.StumbleAmber

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = passage.annotated,
            style = TextStyle(
                fontFamily = KfgqpcHafs,
                fontSize = 23.sp,
                lineHeight = 45.sp,
                color = AlkahfColors.Ink,
                textAlign = TextAlign.Justify,
            ),
            onTextLayout = { layoutResult.value = it },
            modifier = modifier
                .fillMaxWidth()
                .pointerInput(portion.id) {
                    detectTapGestures(
                        onTap = { position ->
                            val span = spanAt(layoutResult.value, passage, position) ?: return@detectTapGestures
                            val ayah = portion.ayahs.first { it.id == span.ayahId }
                            val revealed = revealedCounts[ayah.id] ?: 0
                            if (span.wordIndex < revealed) {
                                // Manual marking: tapping a revealed word
                                // toggles its stumble mark.
                                val mark = WordStumble(span.ayahId, span.wordIndex)
                                if (!stumbles.remove(mark)) stumbles.add(mark)
                            } else if (revealed < ayah.words.size) {
                                revealedCounts[ayah.id] = revealed + 1
                            }
                        },
                        onLongPress = { position ->
                            val span = spanAt(layoutResult.value, passage, position) ?: return@detectTapGestures
                            val ayah = portion.ayahs.first { it.id == span.ayahId }
                            revealedCounts[ayah.id] = ayah.words.size
                        },
                    )
                }
                .drawBehind {
                    val layout = layoutResult.value ?: return@drawBehind
                    val visible = stumbles.filter { (revealedSnapshot[it.ayahId] ?: 0) > it.wordIndex }
                    visible.forEach { stumble ->
                        val span = passage.spans.firstOrNull {
                            !it.isMarker && it.ayahId == stumble.ayahId && it.wordIndex == stumble.wordIndex
                        } ?: return@forEach
                        val bounds = layout.getPathForRange(span.start, span.end).getBounds()
                        val y = layout.getLineBaseline(layout.getLineForOffset(span.start)) + 9.dp.toPx()
                        drawLine(
                            color = stumbleAmber,
                            start = Offset(bounds.left, y),
                            end = Offset(bounds.right, y),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                    visible.map { it.ayahId }.distinct().forEach { ayahId ->
                        val marker = passage.spans.firstOrNull { it.isMarker && it.ayahId == ayahId }
                            ?: return@forEach
                        val bounds = layout.getPathForRange(marker.start, marker.end).getBounds()
                        drawCircle(
                            color = stumbleAmber,
                            radius = 3.dp.toPx(),
                            center = Offset(bounds.left, bounds.top + 6.dp.toPx()),
                        )
                    }
                },
        )
    }
}

private data class PassageSpan(
    val ayahId: Int,
    val wordIndex: Int,
    val start: Int,
    val end: Int,
    val isMarker: Boolean,
)

private data class PassageAnnotated(val annotated: AnnotatedString, val spans: List<PassageSpan>)

private fun buildPassageText(
    ayahs: List<PageAyah>,
    revealedByAyah: Map<Int, Int>,
): PassageAnnotated {
    val spans = mutableListOf<PassageSpan>()
    val annotated = buildAnnotatedString {
        ayahs.forEachIndexed { ayahIndex, ayah ->
            val revealed = revealedByAyah[ayah.id] ?: 0
            ayah.words.forEachIndexed { wordIndex, word ->
                val start = length
                append(word)
                spans += PassageSpan(ayah.id, wordIndex, start, length, isMarker = false)
                addStyle(
                    style = if (wordIndex >= revealed) {
                        SpanStyle(
                            color = Color.Transparent,
                            shadow = Shadow(AlkahfColors.ConcealedInk, Offset.Zero, 14f),
                        )
                    } else {
                        SpanStyle(color = AlkahfColors.Ink)
                    },
                    start = start,
                    end = length,
                )
                append(' ')
            }
            val markerStart = length
            append(ayah.marker)
            spans += PassageSpan(ayah.id, wordIndex = -1, start = markerStart, end = length, isMarker = true)
            addStyle(
                style = SpanStyle(
                    color = if (revealed >= ayah.words.size) AlkahfColors.Accent else AlkahfColors.ConcealedMedallion,
                ),
                start = markerStart,
                end = length,
            )
            if (ayahIndex != ayahs.lastIndex) append(' ')
        }
    }
    return PassageAnnotated(annotated, spans)
}

private fun spanAt(
    layout: TextLayoutResult?,
    passage: PassageAnnotated,
    position: Offset,
): PassageSpan? {
    val offset = layout?.getOffsetForPosition(position) ?: return null
    return passage.spans.firstOrNull { !it.isMarker && offset in it.start until it.end }
}

@Composable
private fun RevealHintDock() {
    Text(
        text = "Recite from memory · tap to reveal · long-press reveals the ayah · tap a revealed word to mark a stumble",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AlkahfColors.InkSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 16.dp),
    )
}

@Composable
private fun GradingDock(
    portion: ReviewPortion,
    stumbleCount: Int,
    onGrade: (ReviewGrade) -> Unit,
) {
    Column(Modifier.padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 10.dp)) {
        Text(
            text = "How was your recall?",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GradeButton(
                label = "Forgot",
                hint = hintFor(portion, ReviewGrade.FORGOT, stumbleCount),
                labelColor = AlkahfColors.ForgotInk,
                hintColor = AlkahfColors.ForgotHint,
                background = AlkahfColors.ForgotBg,
                border = AlkahfColors.ForgotBorder,
                modifier = Modifier.weight(1f),
            ) { onGrade(ReviewGrade.FORGOT) }
            GradeButton(
                label = "Hesitant",
                hint = hintFor(portion, ReviewGrade.HESITANT, stumbleCount),
                labelColor = AlkahfColors.StumbleInk,
                hintColor = AlkahfColors.HesitantHint,
                background = AlkahfColors.StumbleBg,
                border = AlkahfColors.StumbleBorder,
                modifier = Modifier.weight(1f),
            ) { onGrade(ReviewGrade.HESITANT) }
            Surface(
                onClick = { onGrade(ReviewGrade.PERFECT) },
                shape = RoundedCornerShape(16.dp),
                color = AlkahfColors.Accent,
                modifier = Modifier.weight(1f).height(64.dp).shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                    spotColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "Perfect",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.OnAccent,
                    )
                    Text(
                        text = hintFor(portion, ReviewGrade.PERFECT, stumbleCount),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.PerfectHint,
                        maxLines = 1,
                    )
                }
            }
        }
        Text(
            text = "Stumbles lower the grade automatically",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkFooter,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        )
    }
}

/** The honest interval each grade would assign, including the stumble cap. */
private fun hintFor(portion: ReviewPortion, grade: ReviewGrade, stumbleCount: Int): String {
    val effective = ReviewScheduler.effectiveGrade(grade, stumbleCount)
    return ReviewScheduler.intervalLabel(
        ReviewScheduler.nextIntervalDays(portion.intervalDays, effective),
    )
}

@Composable
private fun GradeButton(
    label: String,
    hint: String,
    labelColor: Color,
    hintColor: Color,
    background: Color,
    border: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.5.dp, border),
        modifier = modifier.height(64.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = label, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = labelColor)
            Text(
                text = hint,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = hintColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GradedDock(
    portion: ReviewPortion,
    chosenGrade: ReviewGrade,
    stumbleCount: Int,
    nextPortionName: String?,
    remainingAfter: Int,
    onChange: () -> Unit,
    onNext: () -> Unit,
) {
    val effective = ReviewScheduler.effectiveGrade(chosenGrade, stumbleCount)
    val interval = ReviewScheduler.nextIntervalDays(portion.intervalDays, effective)
    val capped = effective != chosenGrade
    Column(Modifier.padding(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AlkahfColors.Accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = buildString {
                    append("Graded ${ReviewScheduler.gradeLabel(effective)}")
                    if (capped) append(" (stumble cap)")
                    append(" · next review ${ReviewScheduler.intervalLabel(interval)}")
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkSecondaryDark,
            )
            Text(
                text = "Change",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkMuted,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onChange),
            )
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp).shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                spotColor = AlkahfColors.Accent.copy(alpha = 0.28f),
            ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlkahfColors.Accent,
                contentColor = AlkahfColors.OnAccent,
            ),
        ) {
            Text(
                text = if (nextPortionName != null) "Next portion · $nextPortionName →" else "Finish review",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = if (remainingAfter > 0) {
                "$remainingAfter portion${if (remainingAfter > 1) "s" else ""} left · ≈ ${minutesLabel(remainingAfter)}"
            } else {
                "Last portion of today's queue"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkFooter,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
        )
    }
}

@Composable
private fun CompletedBody(gradedCount: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = AlkahfColors.Accent,
            modifier = Modifier.size(44.dp),
        )
        Text(
            text = "Review complete",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.Ink,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = if (gradedCount > 0) {
                "$gradedCount portion${if (gradedCount > 1) "s" else ""} graded — the scheduler has set their next reviews"
            } else {
                "Nothing due today"
            },
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        Button(
            onClick = onDone,
            modifier = Modifier.padding(top = 20.dp).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlkahfColors.Accent,
                contentColor = AlkahfColors.OnAccent,
            ),
        ) {
            Text(text = "Back to Today", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
