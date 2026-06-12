package app.alkahf.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AyahTextStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState = HomeUiState(),
    onOpenMushaf: () -> Unit = {},
    onOpenLoop: () -> Unit = {},
    onOpenReview: () -> Unit = {},
) {
    Scaffold(
        containerColor = AlkahfColors.Paper,
        bottomBar = { AlkahfBottomNav(onOpenMushaf, onOpenReview) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            HomeHeader(state)
            SabaqCard(state, onOpenMushaf)
            MurajaahCard(state, onOpenReview)
            ResumeDrillCard(state, onOpenLoop)
            ThisWeekCard(state)
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun HomeHeader(state: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = state.greeting,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkMuted,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
            Text(
                text = "Today",
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                letterSpacing = (-0.5).sp,
                lineHeight = 34.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = todayLabel(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        StreakChip(days = state.streakDays)
    }
}

private fun todayLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH))

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
                text = "DAYS",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

@Composable
private fun SabaqCard(state: HomeUiState, onOpenMushaf: () -> Unit) {
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
                        text = "NEW · SABAQ",
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
                ActionRow(onOpenMushaf)
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
            style = AyahTextStyle,
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
            text = "${state.memorizedInSabaq} of ${state.sabaqAyahStates.size} ayat memorized",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkSecondary,
        )
    }
}

@Composable
private fun ActionRow(onOpenMushaf: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Button(
            onClick = onOpenMushaf,
            modifier = Modifier
                .weight(1f)
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
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Continue learning",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Surface(
            onClick = onOpenMushaf,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, AlkahfColors.SecondaryButtonBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.VisibilityOff,
                    contentDescription = "Hide & self-test",
                    tint = AlkahfColors.AccentDeep,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
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
                    text = "REVIEW · MURĀJAʿAH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.InkMuted,
                )
                Surface(shape = CircleShape, color = AlkahfColors.GoldBg) {
                    Text(
                        text = "DUE TODAY",
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
                    text = "${state.reviewPortionCount} portions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.Ink,
                    letterSpacing = (-0.4).sp,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "≈ ${state.reviewEstimatedMinutes} min",
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
                    text = "Start review",
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
                color = AlkahfColors.Ink,
                modifier = Modifier.size(50.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Resume drill",
                        tint = AlkahfColors.Paper,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "RESUME DRILL",
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
                    text = "This week",
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

private data class NavDestination(val label: String, val icon: ImageVector)

@Composable
private fun AlkahfBottomNav(onOpenMushaf: () -> Unit, onOpenReview: () -> Unit) {
    val destinations = listOf(
        NavDestination("Today", Icons.Outlined.Home),
        NavDestination("Mushaf", Icons.AutoMirrored.Outlined.MenuBook),
        NavDestination("Review", Icons.Outlined.Autorenew),
        NavDestination("Progress", Icons.Outlined.BarChart),
        NavDestination("Library", Icons.Outlined.Download),
    )
    Column {
        HorizontalDivider(thickness = 1.dp, color = AlkahfColors.CardBorder)
        NavigationBar(containerColor = AlkahfColors.NavSurface) {
            destinations.forEachIndexed { index, destination ->
                val selected = index == 0
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        when (destination.label) {
                            "Mushaf" -> onOpenMushaf()
                            "Review" -> onOpenReview()
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(23.dp),
                        )
                    },
                    label = {
                        Text(
                            text = destination.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AlkahfColors.AccentTint,
                        selectedIconColor = AlkahfColors.AccentDeep,
                        selectedTextColor = AlkahfColors.AccentDeep,
                        unselectedIconColor = AlkahfColors.InkMuted,
                        unselectedTextColor = AlkahfColors.InkMuted,
                    ),
                )
            }
        }
    }
}
