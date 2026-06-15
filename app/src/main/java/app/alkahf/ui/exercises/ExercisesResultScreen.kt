package app.alkahf.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.ui.rememberSurahNamer
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran

/** A per-type score line for the breakdown bars. */
private data class TypeScore(val type: ExerciseType, val correct: Int, val total: Int)

/**
 * Frame 05 — the results summary. Records the finished session on entry, then
 * shows the score ring, the per-type breakdown, and the missed-questions list.
 */
@Composable
fun ExercisesResultScreen(
    controller: ExercisesController,
    onClose: () -> Unit,
    onReviewMissed: (Int) -> Unit,
    onNewTest: () -> Unit,
) {
    val state by controller.state.collectAsState()
    val surahName = rememberSurahNamer()
    LaunchedEffect(Unit) { controller.finish() }

    if (!state.ready || state.total == 0) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.Paper))
        return
    }

    val correct = state.correctCount
    val total = state.total
    val typeScores = ExerciseType.entries.mapNotNull { type ->
        val indices = state.questions.indices.filter { state.questions[it].type == type }
        if (indices.isEmpty()) {
            null
        } else {
            TypeScore(
                type = type,
                correct = indices.count { state.answers[it].status == AnswerStatus.CORRECT },
                total = indices.size,
            )
        }
    }
    val toRevisit = state.toRevisitIndices

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ExercisesTopBar(
            title = stringResource(R.string.ex_result_title),
            subtitle = "",
            onClose = onClose,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            ScoreHero(correct = correct, total = total)
            Spacer(Modifier.height(20.dp))

            SectionKicker(stringResource(R.string.ex_result_by_type))
            Spacer(Modifier.height(10.dp))
            typeScores.forEach { score ->
                TypeScoreBar(score)
                Spacer(Modifier.height(10.dp))
            }

            if (toRevisit.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                SectionKicker(
                    stringResource(R.string.ex_result_to_revisit, toRevisit.size),
                    color = AlkahfColors.ClayText,
                )
                Spacer(Modifier.height(10.dp))
                toRevisit.forEach { index ->
                    RevisitRow(
                        question = state.questions[index],
                        surahName = surahName,
                        onClick = { onReviewMissed(index) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        ResultDock(
            toRevisitCount = toRevisit.size,
            onReviewMissed = { toRevisit.firstOrNull()?.let(onReviewMissed) },
            onNewTest = onNewTest,
        )
    }
}

@Composable
private fun ScoreHero(correct: Int, total: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScoreRing(correct = correct, total = total)
            Column(Modifier.weight(1f)) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = stringResource(R.string.ex_result_dua),
                        fontFamily = AmiriQuran,
                        fontSize = 22.sp,
                        color = AlkahfColors.AccentDeep,
                    )
                }
                Text(
                    text = sessionLabel(correct, total),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = stringResource(R.string.ex_result_encourage),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun sessionLabel(correct: Int, total: Int): String {
    val fraction = if (total == 0) 0f else correct.toFloat() / total
    return stringResource(
        when {
            fraction >= 0.8f -> R.string.ex_result_session_strong
            fraction >= 0.5f -> R.string.ex_result_session_solid
            else -> R.string.ex_result_session_keep_going
        },
    )
}

@Composable
private fun ScoreRing(correct: Int, total: Int) {
    val fraction = if (total == 0) 0f else correct.toFloat() / total
    val track = AlkahfColors.ProgressTrack
    val fill = AlkahfColors.Accent
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val strokePx = 10.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = strokePx))
            val sweep = fraction.coerceIn(0f, 1f) * 360f
            if (sweep > 0f) {
                drawArc(
                    fill, -90f, sweep, false, topLeft, arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$correct/$total",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(R.string.ex_result_correct_label),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
            )
        }
    }
}

@Composable
private fun TypeScoreBar(score: TypeScore) {
    val full = score.correct == score.total
    val figureColor = if (full) AlkahfColors.AccentDeep else AlkahfColors.ClayText
    val fraction = if (score.total == 0) 0f else score.correct.toFloat() / score.total
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = typeLabel(score.type),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkSecondaryDark,
            )
            Text(
                text = "${score.correct} / ${score.total}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = figureColor,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AlkahfColors.ProgressTrack, RoundedCornerShape(4.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(
                        if (full) AlkahfColors.Accent else AlkahfColors.ClayText,
                        RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}

@Composable
private fun RevisitRow(
    question: ExerciseQuestion,
    surahName: (Int) -> String,
    onClick: () -> Unit,
) {
    val colors = typeChipColors(question.type)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(AlkahfColors.ClayBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(8.dp).background(AlkahfColors.ClayText, RoundedCornerShape(50)))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = typeLabel(question.type),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = revisitReference(question, surahName),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 1.dp),
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
private fun revisitReference(question: ExerciseQuestion, surahName: (Int) -> String): String =
    when (question) {
        is ExerciseQuestion.GuessSurah ->
            surahName(question.correctSurah)
        is ExerciseQuestion.FinishAyah ->
            stringResource(R.string.ex_reference, surahName(question.surah), question.number)
        is ExerciseQuestion.OrderAyat -> {
            val first = question.correctOrder.firstOrNull()?.number ?: 0
            val last = question.correctOrder.lastOrNull()?.number ?: 0
            stringResource(R.string.ex_reference_range, surahName(question.surah), first, last)
        }
    }

@Composable
private fun ResultDock(toRevisitCount: Int, onReviewMissed: () -> Unit, onNewTest: () -> Unit) {
    Surface(color = AlkahfColors.NavSurface, border = BorderStroke(1.dp, AlkahfColors.DockBorder)) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (toRevisitCount > 0) {
                Surface(
                    onClick = onReviewMissed,
                    shape = RoundedCornerShape(15.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, AlkahfColors.SecondaryButtonBorder),
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.ex_result_review_missed, toRevisitCount),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AlkahfColors.InkSecondary,
                        )
                    }
                }
            }
            Button(
                onClick = onNewTest,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.Accent,
                    contentColor = AlkahfColors.OnAccent,
                ),
            ) {
                Text(
                    text = stringResource(R.string.ex_result_new_test),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
