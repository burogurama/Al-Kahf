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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.KhatamState
import app.alkahf.data.khatam.PaceStatus
import app.alkahf.ui.rememberSurahNamer
import app.alkahf.ui.theme.AlkahfColors

@Composable
fun KhatamTrackerScreen(
    state: KhatamState,
    onBack: () -> Unit,
    onOpenPortion: () -> Unit,
    onStartReading: (page: Int) -> Unit,
    onReminderChange: (enabled: Boolean, minute: Int) -> Unit,
    onCancel: () -> Unit,
) {
    val surahName = rememberSurahNamer()
    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TrackerTopBar(state, onBack, onReminderChange, onCancel)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            HeroCard(state)
            JuzMapCard(state)
            TodayPortionCard(state, surahName, onOpenPortion, onStartReading)
            StatsRow(state)
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun TrackerTopBar(
    state: KhatamState,
    onBack: () -> Unit,
    onReminderChange: (Boolean, Int) -> Unit,
    onCancel: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    if (showReminder) {
        KhatamReminderDialog(
            initialEnabled = state.reminderEnabled,
            initialMinute = state.reminderTime ?: 310,
            onConfirm = { enabled, minute ->
                showReminder = false
                onReminderChange(enabled, minute)
            },
            onDismiss = { showReminder = false },
        )
    }
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
                text = stringResource(R.string.khatam_top_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(R.string.khatam_top_subtitle, state.pace),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
            )
        }
        Box {
            Box(
                modifier = Modifier.size(40.dp).clickable { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.khatam_manage),
                    tint = AlkahfColors.InkChrome,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.khatam_plan_reminder),
                            color = AlkahfColors.Ink,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        showReminder = true
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.khatam_cancel),
                            color = AlkahfColors.ForgotInk,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onCancel()
                    },
                )
            }
        }
    }
}

/** Toggle + time picker for the khatam's daily reminder, shown from the ⋯ menu. */
@Composable
private fun KhatamReminderDialog(
    initialEnabled: Boolean,
    initialMinute: Int,
    onConfirm: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(initialEnabled) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AlkahfColors.Surface,
        title = {
            Text(
                text = stringResource(R.string.khatam_plan_reminder),
                color = AlkahfColors.Ink,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = {
                        android.app.TimePickerDialog(
                            context,
                            { _, hour, min -> minute = hour * 60 + min },
                            minute / 60,
                            minute % 60,
                            android.text.format.DateFormat.is24HourFormat(context),
                        ).show()
                    },
                    enabled = enabled,
                    shape = CircleShape,
                    color = if (enabled) AlkahfColors.AccentTint2 else AlkahfColors.ChipBg,
                ) {
                    Text(
                        text = if (enabled) {
                            "%d:%02d".format(minute / 60, minute % 60)
                        } else {
                            stringResource(R.string.khatam_reminder_off)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (enabled) AlkahfColors.AccentDeep else AlkahfColors.InkFaint,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(enabled, minute) }) {
                Text(stringResource(R.string.common_save), color = AlkahfColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel), color = AlkahfColors.InkMuted)
            }
        },
    )
}

@Composable
private fun goldHeroBrush(): Brush = Brush.linearGradient(
    colors = listOf(AlkahfColors.KhatamCardTint, AlkahfColors.Surface),
)

@Composable
private fun HeroCard(state: KhatamState) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
    ) {
        Row(
            modifier = Modifier
                .background(goldHeroBrush())
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KhatamRing(fraction = state.ringFraction, diameter = 116.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.khatam_percent, state.percent),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.KhatamGoldDeep,
                    )
                    Text(
                        text = stringResource(
                            R.string.khatam_units_of_total,
                            state.unitsCompleted,
                            state.totalUnits,
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkFainter,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.khatam_day_n, state.currentDay),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.InkMuted,
                )
                PaceStatusPill(state.paceStatus)
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = AlkahfColors.InkMuted,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = stringResource(R.string.khatam_finishes, epochDayLabel(state.finishEpochDay)),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkSecondaryDark,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.khatam_to_go,
                        (state.totalUnits - state.unitsCompleted).coerceAtLeast(0),
                    ),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PaceStatusPill(status: PaceStatus) {
    val (bg, dot, ink, label) = when (status) {
        PaceStatus.AHEAD -> StatusStyle(
            AlkahfColors.GoldBg,
            AlkahfColors.GoldText,
            AlkahfColors.GoldText,
            stringResource(R.string.khatam_status_ahead),
        )
        PaceStatus.BEHIND -> StatusStyle(
            AlkahfColors.GoldBg,
            AlkahfColors.GoldText,
            AlkahfColors.GoldText,
            stringResource(R.string.khatam_status_behind),
        )
        PaceStatus.ON_TRACK -> StatusStyle(
            AlkahfColors.AccentTint2,
            AlkahfColors.AccentDeep,
            AlkahfColors.AccentDeep,
            stringResource(R.string.khatam_status_on_track),
        )
    }
    Surface(shape = CircleShape, color = bg, modifier = Modifier.padding(top = 6.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.size(7.dp).background(dot, CircleShape))
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ink)
        }
    }
}

private data class StatusStyle(val bg: Color, val dot: Color, val ink: Color, val label: String)

@Composable
private fun JuzMapCard(state: KhatamState) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.khatam_mushaf_30),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.InkMuted,
                )
                Text(
                    text = stringResource(R.string.khatam_n_done, state.unitsCompleted),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkFaint,
                )
            }
            JuzMap(
                doneCount = state.unitsCompleted,
                todayJuz = state.todaysPortionJuz,
                barHeight = 34.dp,
                showAxis = true,
            )
        }
    }
}

@Composable
private fun TodayPortionCard(
    state: KhatamState,
    surahName: (Int) -> String,
    onOpenPortion: () -> Unit,
    onStartReading: (page: Int) -> Unit,
) {
    val portion = state.todaysPortion
    Surface(
        onClick = onOpenPortion,
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.KhatamCardBorder),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.khatam_today_juz, state.todaysPortionJuz),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.KhatamGoldDeep,
                )
                Surface(shape = CircleShape, color = AlkahfColors.GoldBg) {
                    Text(
                        text = stringResource(R.string.khatam_due_today),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlkahfColors.GoldText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                text = if (portion != null) portionRangeLabel(portion, surahName) else "",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                modifier = Modifier.padding(top = 10.dp),
            )
            if (portion != null) {
                Text(
                    text = stringResource(
                        R.string.khatam_pages_minutes,
                        portion.pageFrom,
                        portion.pageTo,
                        portionMinutes(portion),
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Button(
                onClick = { portion?.let { onStartReading(it.pageFrom) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
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
                    text = stringResource(R.string.khatam_start_reading),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StatsRow(state: KhatamState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            value = "${state.streakDays}",
            label = stringResource(R.string.khatam_stat_streak),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = "${state.pagesRead}",
            label = stringResource(R.string.khatam_stat_pages),
            modifier = Modifier.weight(1f),
        )
        StatTile(
            value = stringResource(R.string.khatam_hours_value, state.timeReadSeconds / 3600f),
            label = stringResource(R.string.khatam_stat_time),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.KhatamGoldDeep,
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
