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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.khatam.KhatamMath
import app.alkahf.ui.theme.AlkahfColors
import java.time.LocalDate

/** A selectable pace option: juzʼ/day, derived duration, and labels. */
private data class PaceOption(val pace: Int, val days: Int, val popular: Boolean)

// Every option programs exactly what it claims: a whole juzʼ-per-day pace whose
// day count is ceil(30 / pace). (A gentle sub-1-juzʼ/day pace is deferred until
// page-granular tracking exists, same as Custom.)
private val PACE_OPTIONS = listOf(
    PaceOption(pace = 1, days = 30, popular = true),
    PaceOption(pace = 2, days = 15, popular = false),
    PaceOption(pace = 3, days = 10, popular = false),
)

@Composable
fun KhatamProgramScreen(
    onClose: () -> Unit,
    onBegin: (pace: Int, reminderEnabled: Boolean, reminderMinute: Int) -> Unit,
) {
    // Default selection: 1 juzʼ/day (POPULAR), the first row.
    var selected by remember { mutableIntStateOf(0) }
    // Daily reminder, on by default at the after-Fajr default time (05:10 = 310).
    var reminderEnabled by remember { mutableStateOf(true) }
    var reminderMinute by remember { mutableIntStateOf(310) }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ProgramTopBar(onClose)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.khatam_how_much_each_day),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkMuted,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
            )
            PaceRow(
                title = stringResource(R.string.khatam_pace_one_title),
                subtitle = stringResource(R.string.khatam_pace_one_sub),
                days = PACE_OPTIONS[0].days,
                popular = PACE_OPTIONS[0].popular,
                selected = selected == 0,
                onClick = { selected = 0 },
            )
            PaceRow(
                title = stringResource(R.string.khatam_pace_two_title),
                subtitle = stringResource(R.string.khatam_pace_two_sub),
                days = PACE_OPTIONS[1].days,
                popular = PACE_OPTIONS[1].popular,
                selected = selected == 1,
                onClick = { selected = 1 },
            )
            PaceRow(
                title = stringResource(R.string.khatam_pace_three_title),
                subtitle = stringResource(R.string.khatam_pace_three_sub),
                days = PACE_OPTIONS[2].days,
                popular = PACE_OPTIONS[2].popular,
                selected = selected == 2,
                onClick = { selected = 2 },
            )
            Spacer(Modifier.height(4.dp))
            PlanSummaryCard(
                pace = PACE_OPTIONS[selected].pace,
                reminderEnabled = reminderEnabled,
                reminderMinute = reminderMinute,
                onToggleReminder = { reminderEnabled = it },
                onPickTime = {
                    showKhatamTimePicker(context, reminderMinute) { reminderMinute = it }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
        BeginDock(onClick = { onBegin(PACE_OPTIONS[selected].pace, reminderEnabled, reminderMinute) })
    }
}

/** Opens the platform time picker, reporting the chosen minute-of-day. */
private fun showKhatamTimePicker(
    context: android.content.Context,
    minute: Int,
    onPicked: (Int) -> Unit,
) {
    android.app.TimePickerDialog(
        context,
        { _, hour, min -> onPicked(hour * 60 + min) },
        minute / 60,
        minute % 60,
        android.text.format.DateFormat.is24HourFormat(context),
    ).show()
}

/** A minute-of-day as a plain "H:mm" clock label (Western digits, UI text). */
private fun clockLabel(minute: Int): String = "%d:%02d".format(minute / 60, minute % 60)

@Composable
private fun ProgramTopBar(onClose: () -> Unit) {
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
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.khatam_new_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(R.string.khatam_step_pace),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
            )
        }
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun PaceRow(
    title: String,
    subtitle: String,
    days: Int,
    popular: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) AlkahfColors.KhatamCardTint else AlkahfColors.Surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) AlkahfColors.KhatamGoldBorder else AlkahfColors.CardBorder,
        ),
        modifier = if (selected) {
            Modifier.shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = AlkahfColors.KhatamProgressFill.copy(alpha = 0.16f),
                spotColor = AlkahfColors.KhatamProgressFill.copy(alpha = 0.16f),
            )
        } else {
            Modifier
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) AlkahfColors.KhatamGoldDeep else AlkahfColors.Ink,
                    )
                    if (popular) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AlkahfColors.GoldText,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.khatam_popular),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = AlkahfColors.OnAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text(
                text = stringResource(R.string.khatam_days, days),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) AlkahfColors.KhatamGoldDeep else AlkahfColors.InkSecondaryDark,
                modifier = Modifier.padding(end = 12.dp),
            )
            RadioDot(selected)
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    if (selected) {
        Box(
            Modifier.size(22.dp).background(AlkahfColors.GoldText, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = AlkahfColors.OnAccent,
                modifier = Modifier.size(14.dp),
            )
        }
    } else {
        Box(
            Modifier
                .size(22.dp)
                .background(AlkahfColors.Surface, CircleShape),
        ) {
            Surface(
                shape = CircleShape,
                color = AlkahfColors.Surface,
                border = BorderStroke(1.5.dp, AlkahfColors.Chevron),
                modifier = Modifier.fillMaxSize(),
            ) {}
        }
    }
}

@Composable
private fun PlanSummaryCard(
    pace: Int,
    reminderEnabled: Boolean,
    reminderMinute: Int,
    onToggleReminder: (Boolean) -> Unit,
    onPickTime: () -> Unit,
) {
    val today = LocalDate.now().toEpochDay()
    val finish = KhatamMath.derivedFinishDate(startDate = today, pace = pace)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = stringResource(R.string.khatam_your_plan),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkMuted,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            PlanRow(
                label = stringResource(R.string.khatam_plan_starts),
                value = stringResource(R.string.khatam_plan_starts_today, epochDayLabel(today)),
                valueColor = AlkahfColors.InkSecondaryDark,
            )
            HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline)
            PlanRow(
                label = stringResource(R.string.khatam_plan_finishes),
                value = epochDayLabel(finish),
                valueColor = AlkahfColors.KhatamGoldDeep,
                bold = true,
            )
            HorizontalDivider(thickness = 1.dp, color = AlkahfColors.Hairline)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = AlkahfColors.InkMuted,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.khatam_plan_reminder),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AlkahfColors.InkSecondary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The chosen time is tappable to re-pick; disabled when reminder is off.
                    Surface(
                        onClick = onPickTime,
                        enabled = reminderEnabled,
                        shape = CircleShape,
                        color = if (reminderEnabled) AlkahfColors.AccentTint2 else AlkahfColors.ChipBg,
                    ) {
                        Text(
                            text = if (reminderEnabled) {
                                clockLabel(reminderMinute)
                            } else {
                                stringResource(R.string.khatam_reminder_off)
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (reminderEnabled) AlkahfColors.AccentDeep else AlkahfColors.InkFaint,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = onToggleReminder,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = AlkahfColors.InkSecondary,
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor,
        )
    }
}

@Composable
private fun BeginDock(onClick: () -> Unit) {
    Surface(color = AlkahfColors.Paper) {
        Box(Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 14.dp)) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
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
                Text(
                    text = stringResource(R.string.khatam_begin),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
