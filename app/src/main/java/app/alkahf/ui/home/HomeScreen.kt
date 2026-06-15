package app.alkahf.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.ui.components.AlkahfBottomNav
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AyahTextStyle
import app.alkahf.ui.theme.LocalQuranFont
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    onOpenMushaf: () -> Unit = {},
    onOpenSabaq: () -> Unit = {},
    onMarkSabaq: () -> Unit = {},
    onOpenLoop: () -> Unit = {},
    onOpenReview: () -> Unit = {},
    onOpenProgress: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBeginKhatam: () -> Unit = {},
    onOpenKhatam: () -> Unit = {},
    onStartExercises: () -> Unit = {},
) {
    Scaffold(
        containerColor = AlkahfColors.Paper,
        bottomBar = {
            AlkahfBottomNav(selected = AlkahfTab.TODAY) { tab ->
                when (tab) {
                    AlkahfTab.MUSHAF -> onOpenMushaf()
                    AlkahfTab.REVIEW -> onOpenReview()
                    AlkahfTab.PROGRESS -> onOpenProgress()
                    AlkahfTab.LIBRARY -> onOpenLibrary()
                    else -> {}
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            HomeHeader(state, onOpenSettings)
            KhatamCard(state, onBeginKhatam, onOpenKhatam)
            SabaqCard(state, onOpenSabaq, onMarkSabaq)
            MurajaahCard(state, onOpenReview)
            ExercisesCard(state, onStartExercises)
            if (state.hasDrill) {
                ResumeDrillCard(state, onOpenLoop)
            }
            ThisWeekCard(state)
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.home_greeting),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkMuted,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.home_today),
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                lineHeight = 34.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = todayLabel(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StreakChip(days = state.streakDays)
            Box(
                modifier = Modifier.padding(start = 6.dp).size(40.dp).clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                    tint = AlkahfColors.InkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

@Composable
private fun StreakChip(days: Int) {
    Surface(
        shape = CircleShape,
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 7.dp, end = 12.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(AlkahfColors.Accent, CircleShape),
            )
            Text(
                text = "$days",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(R.string.home_days),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

@Composable
private fun NoSabaqCard(onOpenMushaf: () -> Unit) {
    Surface(
        onClick = onOpenMushaf,
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.home_new_sabaq),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkMuted,
            )
            Text(
                text = stringResource(R.string.home_no_sabaq),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.home_no_sabaq_hint),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SabaqCard(state: HomeUiState, onOpenMushaf: () -> Unit, onMarkSabaq: () -> Unit) {
    if (!state.hasSabaq) {
        NoSabaqCard(onOpenMushaf)
        return
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.SurfaceHero,
        border = BorderStroke(1.dp, AlkahfColors.CardBorderHero),
    ) {
        Box {
            // Accent rail pinned to the right edge (content is RTL).
            Box(
                Modifier
                    .matchParentSize()
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .width(3.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(3.dp))
                        .background(AlkahfColors.Accent.copy(alpha = 0.55f)),
                )
            }
            Column(
                Modifier.padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_new_sabaq),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = AlkahfColors.AccentDeep,
                    )
                    Text(
                        text = state.sabaqReference,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkSecondary,
                        maxLines = 1,
                    )
                }
                AyahBlock(state)
                ProgressRow(state)
                ActionRow(state.sabaqComplete, onOpenMushaf, onMarkSabaq)
            }
        }
    }
}

@Composable
private fun AyahBlock(state: HomeUiState) {
    val ayah = buildAnnotatedString {
        append(state.sabaqAyahText)
        append(' ')
        withStyle(SpanStyle(color = AlkahfColors.Accent, fontSize = 21.sp)) {
            append(state.sabaqAyahMarker)
        }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = ayah,
            // AyahTextStyle's baked-in colour is the light theme's; read the live
            // token here so the text stays legible in dark mode.
            style = AyahTextStyle.copy(
                fontFamily = LocalQuranFont.current,
                color = AlkahfColors.Ink,
            ),
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun ProgressRow(state: HomeUiState) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            state.sabaqAyahStates.forEach { ayahState ->
                val color = when (ayahState) {
                    AyahMemorizationState.MEMORIZED, AyahMemorizationState.STRONG -> AlkahfColors.Accent
                    AyahMemorizationState.LEARNING -> AlkahfColors.Learning
                    AyahMemorizationState.NOT_STARTED -> AlkahfColors.NotStarted
                }
                Box(
                    Modifier
                        .size(9.dp)
                        .background(color, CircleShape),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.home_ayat_memorized,
                state.memorizedInSabaq,
                state.sabaqAyahStates.size,
            ),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkSecondary,
        )
    }
}

@Composable
private fun ActionRow(sabaqComplete: Boolean, onOpenMushaf: () -> Unit, onMarkSabaq: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        // While the sabaq isn't fully memorized, "continue learning" is the
        // primary action and fills the row; once it's done it steps back to make
        // room for the green Done button.
        ContinueLearningButton(
            onClick = onOpenMushaf,
            emphasized = !sabaqComplete,
            modifier = Modifier.weight(1f),
        )
        if (sabaqComplete) {
            DoneButton(onClick = onMarkSabaq, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ContinueLearningButton(onClick: () -> Unit, emphasized: Boolean, modifier: Modifier = Modifier) {
    if (emphasized) {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(48.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                    spotColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                ),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlkahfColors.Accent,
                contentColor = AlkahfColors.OnAccent,
            ),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_continue_learning),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        // De-emphasized: still tappable, but faded so the green Done button leads.
        Surface(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, AlkahfColors.SecondaryButtonBorder),
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = AlkahfColors.Accent.copy(alpha = 0.5f),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_continue_learning),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Accent.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = AlkahfColors.AccentDeep.copy(alpha = 0.34f),
                spotColor = AlkahfColors.AccentDeep.copy(alpha = 0.34f),
            ),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AlkahfColors.AccentDeep,
            contentColor = AlkahfColors.OnAccent,
        ),
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text = stringResource(R.string.home_sabaq_done),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MurajaahCard(state: HomeUiState, onOpenReview: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_review_murajaah),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.InkMuted,
                )
                Surface(shape = CircleShape, color = AlkahfColors.GoldBg) {
                    Text(
                        text = stringResource(R.string.home_due_today),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.GoldText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    text = pluralStringResource(
                        R.plurals.home_portions,
                        state.reviewPortionCount,
                        state.reviewPortionCount,
                    ),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = stringResource(R.string.home_approx_min, state.reviewEstimatedMinutes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            FlowRow(
                modifier = Modifier.padding(top = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                state.reviewPortionNames.forEach { name ->
                    ReviewChip(text = name, textColor = AlkahfColors.InkSecondaryDark)
                }
                if (state.reviewOverflowCount > 0) {
                    ReviewChip(
                        text = "+${state.reviewOverflowCount}",
                        textColor = AlkahfColors.InkMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Button(
                onClick = onOpenReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.AccentTint2,
                    contentColor = AlkahfColors.AccentDeep,
                ),
                elevation = null,
            ) {
                Text(
                    text = stringResource(R.string.home_start_review),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ReviewChip(
    text: String,
    textColor: Color,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = AlkahfColors.ChipBg,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = fontWeight,
            color = textColor,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun ExercisesCard(state: HomeUiState, onStartTest: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorderHero),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = AlkahfColors.AccentDeep,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.ex_today_kicker),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.AccentDeep,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                text = stringResource(R.string.ex_today_headline),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(R.string.ex_today_description),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkSecondary,
                modifier = Modifier.padding(top = 5.dp),
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = AlkahfColors.InkFaint,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = state.exerciseReadinessLine,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkSecondary,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            Button(
                onClick = onStartTest,
                enabled = state.exerciseReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(50.dp)
                    .shadow(
                        elevation = if (state.exerciseReady) 4.dp else 0.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                        spotColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.Accent,
                    contentColor = AlkahfColors.OnAccent,
                    disabledContainerColor = AlkahfColors.NotStarted,
                    disabledContentColor = AlkahfColors.InkFaint,
                ),
            ) {
                Text(
                    text = stringResource(R.string.ex_today_start),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (state.exerciseHasLastResult) {
                LastResultRow(state, onStartTest)
            }
        }
    }
}

@Composable
private fun LastResultRow(state: HomeUiState, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = AlkahfColors.AccentTint2, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = state.exerciseLastScore,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.AccentDeep,
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.ex_today_last_test),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = if (state.exerciseLastToRevisit > 0) {
                        stringResource(
                            R.string.ex_today_last_detail,
                            state.exerciseLastWhen,
                            state.exerciseLastToRevisit,
                        )
                    } else {
                        state.exerciseLastWhen
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            Text(
                text = stringResource(R.string.ex_today_review),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.Accent,
            )
        }
    }
}

@Composable
private fun ResumeDrillCard(state: HomeUiState, onOpenLoop: () -> Unit) {
    Surface(
        onClick = onOpenLoop,
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = AlkahfColors.PlayTile,
                modifier = Modifier.size(50.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = stringResource(R.string.home_resume_drill),
                        tint = AlkahfColors.PlayTileInk,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_resume_drill_caption),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = AlkahfColors.InkFaint,
                )
                Text(
                    text = state.drillPresetTitle,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Ink,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = state.drillPresetDetail,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkMuted,
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
private fun KhatamCard(state: HomeUiState, onBegin: () -> Unit, onOpen: () -> Unit) {
    if (state.hasKhatam) {
        ActiveKhatamCard(state, onOpen)
    } else {
        EmptyKhatamCard(onBegin)
    }
}

@Composable
private fun EmptyKhatamCard(onBegin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = AlkahfColors.GoldText,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.khatam_kicker),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.KhatamGoldDeep,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            // Empty 30-juzʼ dashed track.
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = AlkahfColors.KhatamCardTint.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, AlkahfColors.KhatamBorderSoft),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        repeat(30) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AlkahfColors.KhatamUpcoming),
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.khatam_empty_caption),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFainter,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.khatam_empty_headline),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                lineHeight = 28.sp,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = stringResource(R.string.khatam_empty_sub),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkSecondary,
                modifier = Modifier.padding(top = 6.dp),
            )
            Button(
                onClick = onBegin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(50.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                        spotColor = AlkahfColors.Accent.copy(alpha = 0.26f),
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.Accent,
                    contentColor = AlkahfColors.OnAccent,
                ),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.khatam_begin_cta),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ActiveKhatamCard(state: HomeUiState, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
    ) {
        Row(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(AlkahfColors.KhatamCardTint, AlkahfColors.Surface),
                    ),
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MiniKhatamRing(state.khatamRingFraction, state.khatamPercent)
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.khatam_today_juz, state.khatamTodayJuz),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.KhatamGoldDeep,
                )
                Text(
                    text = state.khatamTodayReference,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    // The compact card (ring + chevron) is too narrow for a long
                    // range on one line; let it wrap so the end āyah isn't clipped.
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Text(
                    text = stringResource(R.string.khatam_resume_start),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Accent,
                    modifier = Modifier.padding(top = 4.dp),
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
private fun MiniKhatamRing(fraction: Float, percent: Int) {
    val track = AlkahfColors.KhatamRingTrack
    val fill = AlkahfColors.KhatamProgressFill
    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(56.dp)) {
            val strokePx = 7.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = androidx.compose.ui.geometry.Size(size.width - strokePx, size.height - strokePx)
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
        Text(
            text = stringResource(R.string.khatam_percent, percent),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.KhatamGoldDeep,
        )
    }
}

@Composable
private fun ThisWeekCard(state: HomeUiState) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_this_week),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Ink,
                )
                Text(
                    text = state.weekSummary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                state.weekDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val squareColor = when (day.activity) {
                            DayActivity.PRACTICED -> AlkahfColors.Accent
                            DayActivity.MISSED -> AlkahfColors.NotStarted
                            DayActivity.TODAY -> AlkahfColors.AccentLight
                        }
                        Box(
                            Modifier
                                .size(18.dp)
                                .background(squareColor, RoundedCornerShape(6.dp)),
                        )
                        Text(
                            text = day.letter,
                            fontSize = 10.sp,
                            fontWeight = if (day.activity == DayActivity.TODAY) {
                                FontWeight.Bold
                            } else {
                                FontWeight.SemiBold
                            },
                            color = if (day.activity == DayActivity.TODAY) {
                                AlkahfColors.Accent
                            } else {
                                AlkahfColors.InkFainter
                            },
                        )
                    }
                }
            }
        }
    }
}

