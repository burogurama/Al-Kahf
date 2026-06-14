package app.alkahf.ui.khatam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.KhatamPortion
import app.alkahf.data.KhatamState
import app.alkahf.ui.rememberSurahNamer
import app.alkahf.ui.theme.AlkahfColors

/**
 * The "log it complete" confirmation sheet over the dimmed tracker. Reflects the
 * state *after* logging (juzʼ n now filled), but does not mutate until [onDone].
 * When the just-completed juzʼ is the 30th, a dignified full-khatam celebration
 * replaces the normal tomorrow callout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhatamLogSheet(
    state: KhatamState,
    onDone: () -> Unit,
    onReadAhead: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surahName = rememberSurahNamer()
    val repository = (LocalContext.current.applicationContext as AlkahfApplication).repository

    // The juzʼ being logged now, and the count after it lands.
    val loggingJuz = state.todaysPortionJuz
    val doneAfter = (state.unitsCompleted + 1).coerceAtMost(state.totalUnits)
    val completesKhatam = doneAfter >= state.totalUnits

    // Tomorrow's portion (the next juzʼ) for the callout, when not finishing.
    val nextPortion by produceState<KhatamPortion?>(null, loggingJuz, state.riwayah) {
        value = if (completesKhatam) null else repository.khatamPortion(loggingJuz + 1, state.riwayah)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AlkahfColors.NavSurface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        scrimColor = AlkahfColors.ModalScrim,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompletionSeal()
            Spacer(Modifier.height(16.dp))
            if (completesKhatam) {
                KhatamCompleteBody()
            } else {
                LogBody(
                    state = state,
                    loggingJuz = loggingJuz,
                    doneAfter = doneAfter,
                    nextPortion = nextPortion,
                    surahName = surahName,
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.Accent,
                    contentColor = AlkahfColors.OnAccent,
                ),
            ) {
                Text(
                    text = stringResource(R.string.khatam_done),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!completesKhatam) {
                Text(
                    text = stringResource(R.string.khatam_read_ahead, loggingJuz + 1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.KhatamGoldDeep,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clickable(onClick = onReadAhead),
                )
            }
        }
    }
}

@Composable
private fun CompletionSeal() {
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(84.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AlkahfColors.KhatamToday, AlkahfColors.GoldText),
                    center = Offset(size.width * 0.5f, size.height * 0.35f),
                    radius = size.minDimension * 0.7f,
                ),
                radius = size.minDimension / 2f,
            )
            // Inner cream ring inset.
            drawCircle(
                color = AlkahfColors.OnAccent,
                radius = size.minDimension / 2f - 7.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AlkahfColors.OnAccent,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun LogBody(
    state: KhatamState,
    loggingJuz: Int,
    doneAfter: Int,
    nextPortion: KhatamPortion?,
    surahName: (Int) -> String,
) {
    Text(
        text = stringResource(R.string.khatam_juz_complete, loggingJuz),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = AlkahfColors.Ink,
    )
    Text(
        text = stringResource(R.string.khatam_day_logged, state.currentDay, state.totalUnits),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = AlkahfColors.InkMuted,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(18.dp))
    // Mini 30-juzʼ track showing the just-completed juzʼ filled.
    JuzMap(
        doneCount = doneAfter,
        todayJuz = 0,
        barHeight = 26.dp,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(
            R.string.khatam_track_caption,
            percentFor(doneAfter, state.totalUnits),
            doneAfter,
            state.totalUnits,
            (state.totalUnits - doneAfter).coerceAtLeast(0),
        ),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AlkahfColors.InkFaint,
        modifier = Modifier.padding(top = 10.dp),
    )
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MiniStat(
            value = "${state.streakDays + 1}",
            label = stringResource(R.string.khatam_stat_streak),
            valueColor = AlkahfColors.AccentDeep,
            modifier = Modifier.weight(1f),
        )
        MiniStat(
            value = epochDayLabel(state.finishEpochDay),
            label = stringResource(R.string.khatam_finish_label),
            valueColor = AlkahfColors.KhatamGoldDeep,
            modifier = Modifier.weight(1f),
        )
    }
    if (nextPortion != null) {
        Spacer(Modifier.height(14.dp))
        TomorrowCallout(nextPortion, surahName)
    }
}

private fun percentFor(done: Int, total: Int): Int =
    if (total == 0) 0 else (done * 100) / total

@Composable
private fun MiniStat(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TomorrowCallout(portion: KhatamPortion, surahName: (Int) -> String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.AccentTint,
        border = BorderStroke(1.dp, AlkahfColors.SecondaryButtonBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(34.dp).background(AlkahfColors.AccentTint2, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AlkahfColors.AccentDeep,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.khatam_tomorrow),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = AlkahfColors.AccentDeep,
                )
                Text(
                    text = stringResource(
                        R.string.khatam_tomorrow_portion,
                        portion.juz,
                        surahName(portion.surahFrom),
                        portion.ayahFrom,
                        surahName(portion.surahTo),
                        portion.ayahTo,
                    ),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkSecondaryDark,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun KhatamCompleteBody() {
    Text(
        text = stringResource(R.string.khatam_complete_title),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = AlkahfColors.Ink,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.khatam_complete_subtitle),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = AlkahfColors.InkMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp),
    )
    Spacer(Modifier.height(18.dp))
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.KhatamCardTint,
        border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.khatam_dua),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.sp,
            color = AlkahfColors.KhatamGoldDeep,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
    }
}
