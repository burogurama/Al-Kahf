package app.alkahf.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.ui.rememberSurahNamer
import app.alkahf.ui.theme.AlkahfColors

/**
 * Frames 02–04 — the shared exercise runner shell. The user answers one question
 * at a time, checks it, and moves freely next/previous; [onFinished] fires once
 * every question is answered and the user advances past the last.
 */
@Composable
fun ExercisesRunnerScreen(
    controller: ExercisesController,
    onClose: () -> Unit,
    onFinished: () -> Unit,
) {
    val state by controller.state.collectAsState()
    val surahName = rememberSurahNamer()

    if (!state.ready || state.total == 0) {
        Box(Modifier.fillMaxSize().background(AlkahfColors.Paper))
        return
    }

    val question = state.current ?: return
    val qState = state.currentState
    val answered = qState.status != AnswerStatus.UNANSWERED

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding(),
        // The bottom inset is handled by whichever bottom element is shown — the
        // full-bleed keyboard or the dock — so each can sit flush to the edge.
    ) {
        RunnerTopBar(
            currentIndex = state.currentIndex,
            total = state.total,
            answeredCount = state.answeredCount,
            onClose = onClose,
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(6.dp))
            TypeChip(question.type)
            Spacer(Modifier.height(14.dp))
            when (question) {
                is ExerciseQuestion.GuessSurah -> GuessSurahBody(
                    question = question,
                    state = qState,
                    choices = state.surahChoices,
                    answered = answered,
                    onPick = controller::pickSurah,
                )
                is ExerciseQuestion.FinishAyah -> FinishAyahBody(
                    question = question,
                    state = qState,
                    answered = answered,
                )
                is ExerciseQuestion.OrderAyat -> OrderAyatBody(
                    question = question,
                    state = qState,
                    answered = answered,
                    surahName = surahName(question.surah),
                    positions = if (answered) controller.orderPositions(question) else emptyList(),
                    onMove = controller::moveOrder,
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // While writing an answer the keyboard IS the bottom bar (full-bleed, like a
        // native keyboard, with its own Check key); once checked it gives way to the
        // dock with the feedback banner and Previous / Next.
        if (question is ExerciseQuestion.FinishAyah && !answered) {
            QuranicKeyboard(
                onKey = controller::appendWritten,
                onBackspace = controller::backspaceWritten,
                onSpace = { controller.appendWritten(" ") },
                onCheck = controller::checkCurrent,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            RunnerDock(
                type = question.type,
                status = qState.status,
                canCheck = canCheck(question, qState),
                correctAnswerLabel = correctAnswerLabel(question, surahName),
                reference = referenceLabel(question, surahName),
                isLast = state.currentIndex == state.total - 1,
                hasPrevious = state.currentIndex > 0,
                onPrevious = controller::previous,
                onCheck = controller::checkCurrent,
                onNext = {
                    if (state.currentIndex == state.total - 1) onFinished() else controller.next()
                },
            )
        }
    }
}

/** Whether the primary "Check answer" button is enabled for the current answer. */
private fun canCheck(question: ExerciseQuestion, state: QuestionState): Boolean = when (question) {
    is ExerciseQuestion.GuessSurah -> state.pickedSurah != null
    is ExerciseQuestion.FinishAyah -> state.written.isNotBlank()
    is ExerciseQuestion.OrderAyat -> true
}

@Composable
private fun RunnerTopBar(currentIndex: Int, total: Int, answeredCount: Int, onClose: () -> Unit) {
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
                modifier = Modifier.size(24.dp),
            )
        }
        // The green progress bar fills with the position in the set.
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .padding(horizontal = 6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AlkahfColors.ProgressTrack),
        ) {
            val fraction = (currentIndex + 1).toFloat() / total
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AlkahfColors.Accent),
            )
        }
        Text(
            text = stringResource(R.string.ex_run_counter, currentIndex + 1, total),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.InkMuted,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun RunnerDock(
    type: ExerciseType,
    status: AnswerStatus,
    canCheck: Boolean,
    correctAnswerLabel: String,
    reference: String,
    isLast: Boolean,
    hasPrevious: Boolean,
    onPrevious: () -> Unit,
    onCheck: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        color = AlkahfColors.NavSurface,
        border = BorderStroke(1.dp, AlkahfColors.DockBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // navigationBarsPadding keeps the buttons above the gesture bar while the
        // dock surface itself fills to the bottom edge.
        Column(Modifier.navigationBarsPadding().padding(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 14.dp)) {
            if (status != AnswerStatus.UNANSWERED) {
                FeedbackBanner(
                    status = status,
                    correctAnswerLabel = correctAnswerLabel,
                    reference = reference,
                )
                Spacer(Modifier.height(11.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Previous — a ghost button, present (disabled) on the first card.
                Surface(
                    onClick = onPrevious,
                    enabled = hasPrevious,
                    shape = RoundedCornerShape(15.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, AlkahfColors.SecondaryButtonBorder),
                    modifier = Modifier.height(52.dp),
                ) {
                    Row(
                        // fillMaxHeight (not fillMaxSize) so this non-weighted button
                        // wraps to its content width and leaves room for the weighted
                        // "Check answer" button beside it.
                        Modifier.fillMaxHeight().padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = if (hasPrevious) AlkahfColors.InkSecondary else AlkahfColors.InkFainter,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.ex_run_previous),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasPrevious) AlkahfColors.InkSecondary else AlkahfColors.InkFainter,
                        )
                    }
                }
                val primaryEnabled = status != AnswerStatus.UNANSWERED || canCheck
                Button(
                    onClick = { if (status == AnswerStatus.UNANSWERED) onCheck() else onNext() },
                    enabled = primaryEnabled,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlkahfColors.Accent,
                        contentColor = AlkahfColors.OnAccent,
                        disabledContainerColor = AlkahfColors.NotStarted,
                        disabledContentColor = AlkahfColors.InkFaint,
                    ),
                ) {
                    Text(
                        text = when {
                            status == AnswerStatus.UNANSWERED -> stringResource(R.string.ex_run_check)
                            isLast -> stringResource(R.string.ex_run_see_results)
                            else -> stringResource(R.string.ex_run_next)
                        },
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackBanner(status: AnswerStatus, correctAnswerLabel: String, reference: String) {
    val correct = status == AnswerStatus.CORRECT
    val bg = if (correct) AlkahfColors.CorrectTint else AlkahfColors.ClayBg
    val ink = if (correct) AlkahfColors.CorrectDeep else AlkahfColors.ClayText
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, if (correct) AlkahfColors.CorrectBorder else AlkahfColors.ClayBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (correct) {
                        stringResource(R.string.ex_run_correct)
                    } else {
                        stringResource(R.string.ex_run_not_quite)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                )
                if (correctAnswerLabel.isNotEmpty()) {
                    Text(
                        text = correctAnswerLabel,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (correct) AlkahfColors.InkSecondary else AlkahfColors.ClayTextSoft,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (reference.isNotEmpty()) {
                Text(
                    text = reference,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (correct) AlkahfColors.CorrectDeep else AlkahfColors.ClayTextSoft,
                )
            }
        }
    }
}

/**
 * The plain-language correct answer shown in the not-quite banner (and as a
 * reassurance when correct): the sūrah name for Guess, nothing extra for Finish
 * (its continuation is revealed in the body), and an "order shown above" note for
 * Order.
 */
@Composable
private fun correctAnswerLabel(question: ExerciseQuestion, surahName: (Int) -> String): String =
    when (question) {
        is ExerciseQuestion.GuessSurah ->
            stringResource(R.string.ex_run_answer_was, surahName(question.correctSurah))
        is ExerciseQuestion.FinishAyah ->
            stringResource(R.string.ex_run_finish_note)
        is ExerciseQuestion.OrderAyat ->
            stringResource(R.string.ex_run_order_note)
    }

/** A short reference shown on the banner's trailing edge ("Al-Kahf · 10"). */
@Composable
private fun referenceLabel(question: ExerciseQuestion, surahName: (Int) -> String): String =
    when (question) {
        is ExerciseQuestion.GuessSurah -> ""
        is ExerciseQuestion.FinishAyah ->
            stringResource(R.string.ex_reference, surahName(question.surah), question.number)
        is ExerciseQuestion.OrderAyat -> surahName(question.surah)
    }
