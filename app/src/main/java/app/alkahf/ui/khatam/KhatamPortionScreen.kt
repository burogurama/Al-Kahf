package app.alkahf.ui.khatam

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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.KhatamState
import app.alkahf.ui.rememberSurahNamer
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran

@Composable
fun KhatamPortionScreen(
    state: KhatamState,
    onBack: () -> Unit,
    onStartReading: () -> Unit,
    onMarkComplete: () -> Unit,
) {
    val surahName = rememberSurahNamer()
    val portion = state.todaysPortion
    val repository = (LocalContext.current.applicationContext as AlkahfApplication).repository

    // The first āyah of the portion, in Arabic, for the "begins at" preview.
    val firstAyahText by produceState("", portion, state.riwayah) {
        value = if (portion == null) {
            ""
        } else {
            repository.ayahsForRange(portion.surahFrom, portion.ayahFrom, portion.ayahFrom, state.riwayah)
                .firstOrNull()?.words?.joinToString(" ").orEmpty()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        PortionTopBar(state, onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (portion != null) {
                PortionHeaderCard(state, surahName)
                BeginsAtCard(portion, surahName, firstAyahText)
                SurahsCard(portion, surahName)
            }
            Spacer(Modifier.height(3.dp))
        }
        PortionDock(
            onStartReading = { portion?.let { onStartReading() } },
            onMarkComplete = onMarkComplete,
        )
    }
}

@Composable
private fun PortionTopBar(state: KhatamState, onBack: () -> Unit) {
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
                contentDescription = stringResource(R.string.common_back),
                tint = AlkahfColors.InkChrome,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.khatam_portion_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(
                    R.string.khatam_day_of_total,
                    state.currentDay,
                    state.totalUnits,
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
            )
        }
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun goldGradient(): Brush = Brush.linearGradient(
    colors = listOf(AlkahfColors.KhatamCardTint, AlkahfColors.Surface),
)

@Composable
private fun PortionHeaderCard(state: KhatamState, surahName: (Int) -> String) {
    val portion = state.todaysPortion ?: return
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
    ) {
        Column(Modifier.background(goldGradient()).padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(R.string.khatam_your_portion),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.KhatamGoldDeep,
                )
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = stringResource(R.string.khatam_juz_arabic, state.todaysPortionJuz.toString()),
                        fontFamily = AmiriQuran,
                        fontSize = 22.sp,
                        color = AlkahfColors.GoldText,
                    )
                }
            }
            Text(
                text = stringResource(R.string.khatam_juz_label, state.todaysPortionJuz),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = portionRangeLabel(portion, surahName),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkSecondaryDark,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                IconStat(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    text = stringResource(R.string.khatam_n_pages, portion.pageCount),
                )
                IconStat(
                    icon = Icons.Outlined.Schedule,
                    text = stringResource(R.string.khatam_approx_min, portionMinutes(portion)),
                )
            }
        }
    }
}

@Composable
private fun IconStat(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AlkahfColors.GoldText,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkSecondaryDark,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun BeginsAtCard(
    portion: app.alkahf.data.KhatamPortion,
    surahName: (Int) -> String,
    firstAyahText: String,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = stringResource(
                    R.string.khatam_begins_at,
                    surahName(portion.surahFrom).uppercase(),
                    portion.ayahFrom,
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.KhatamGoldDeep,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            // Āyah inset: rounded on the left, a gold accent rail on the right
            // (content is RTL).
            Surface(
                shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp, topEnd = 13.dp, bottomEnd = 13.dp),
                color = AlkahfColors.KhatamInsetTint,
            ) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = firstAyahText,
                            fontFamily = AmiriQuran,
                            fontSize = 22.sp,
                            lineHeight = 42.sp,
                            color = AlkahfColors.GoldText,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxSize()
                            .background(AlkahfColors.KhatamGoldBorder),
                    )
                }
            }
        }
    }
}

@Composable
private fun SurahsCard(portion: app.alkahf.data.KhatamPortion, surahName: (Int) -> String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.khatam_surahs_in_portion),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkMuted,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SurahChip(stringResource(R.string.khatam_chip_from, surahName(portion.surahFrom), portion.ayahFrom))
                SurahChip(stringResource(R.string.khatam_chip_to, surahName(portion.surahTo), portion.ayahTo))
            }
            // Today's progress bar — fresh portion starts at 0 of pageCount.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(8.dp)
                    .background(AlkahfColors.KhatamRingTrack, RoundedCornerShape(4.dp)),
            )
            Text(
                text = stringResource(R.string.khatam_pages_of, 0, portion.pageCount),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SurahChip(text: String) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = AlkahfColors.ChipBg,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkSecondaryDark,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PortionDock(onStartReading: () -> Unit, onMarkComplete: () -> Unit) {
    Surface(color = AlkahfColors.Paper) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onStartReading,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(
                        elevation = 5.dp,
                        shape = RoundedCornerShape(15.dp),
                        ambientColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                        spotColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                    ),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.Accent,
                    contentColor = AlkahfColors.OnAccent,
                ),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.khatam_start_reading),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Surface(
                onClick = onMarkComplete,
                shape = RoundedCornerShape(15.dp),
                color = AlkahfColors.AccentTint2,
                border = BorderStroke(1.dp, AlkahfColors.SecondaryButtonBorder),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = stringResource(R.string.khatam_mark_complete),
                        tint = AlkahfColors.AccentDeep,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
