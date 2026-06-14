package app.alkahf.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.JuzProgress
import app.alkahf.data.JuzStatus
import app.alkahf.data.MemorizationState
import app.alkahf.data.ProgressSnapshot
import app.alkahf.data.QuranRepository
import app.alkahf.ui.components.AlkahfBottomNav
import app.alkahf.ui.components.AlkahfTab
import app.alkahf.ui.theme.AlkahfColors

@Composable
fun ProgressScreen(
    onOpenPage: (Int) -> Unit = {},
    onSelectTab: (AlkahfTab) -> Unit = {},
) {
    val context = LocalContext.current
    val controller = remember {
        ProgressController((context.applicationContext as AlkahfApplication).repository)
    }
    val snapshot by controller.snapshot.collectAsState()
    LaunchedEffect(Unit) { controller.load() }

    Scaffold(
        containerColor = AlkahfColors.Paper,
        bottomBar = { AlkahfBottomNav(selected = AlkahfTab.PROGRESS, onSelect = onSelectTab) },
    ) { innerPadding ->
        val data = snapshot ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ProgressHeader(data)
            MushafMapCard(data, onOpenPage)
            ByJuzCard(data.juzProgress)
            ActivityRow(data)
        }
    }
}

@Composable
private fun ProgressHeader(data: ProgressSnapshot) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(
                text = stringResource(R.string.progress_title),
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.progress_ayat_in_memory,
                    data.memorizedAyahCount,
                    data.memorizedAyahCount,
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = buildAnnotatedString {
                    append("%.1f".format(data.percentOfQuran))
                    withStyle(SpanStyle(fontSize = 16.sp)) { append("%") }
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Accent,
            )
            Text(
                text = stringResource(R.string.progress_of_quran),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                color = AlkahfColors.InkFaint,
            )
        }
    }
}

@Composable
private fun MushafMapCard(data: ProgressSnapshot, onOpenPage: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.progress_mushaf_header,
                        QuranRepository.PAGE_COUNT,
                    ),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    color = AlkahfColors.InkMuted,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        R.string.progress_pages_memorized,
                        data.memorizedPageCount,
                    ),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkFaint,
                    maxLines = 1,
                )
            }
            PageGrid(data.pageStates, onOpenPage)
            LegendRow()
        }
    }
}

private fun stateColor(state: MemorizationState): Color = when (state) {
    MemorizationState.STRONG -> AlkahfColors.AccentDeep
    MemorizationState.MEMORIZED -> AlkahfColors.AccentLight
    MemorizationState.LEARNING -> AlkahfColors.Learning
    MemorizationState.NOT_STARTED -> AlkahfColors.MapEmpty
}

@Composable
private fun PageGrid(pageStates: List<MemorizationState>, onOpenPage: (Int) -> Unit) {
    val cell = 10.dp
    val gap = 2.dp
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val perRow = ((maxWidth + gap) / (cell + gap)).toInt().coerceAtLeast(1)
        val rows = (QuranRepository.PAGE_COUNT + perRow - 1) / perRow
        val gridHeight = cell * rows + gap * (rows - 1)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .pointerInput(perRow) {
                    detectTapGestures { position ->
                        val cellPx = cell.toPx()
                        val gapPx = gap.toPx()
                        // RTL flow: page 1 sits at the top-right corner.
                        val column = ((size.width - position.x) / (cellPx + gapPx)).toInt()
                        val row = (position.y / (cellPx + gapPx)).toInt()
                        val page = row * perRow + column + 1
                        if (column < perRow && page in 1..QuranRepository.PAGE_COUNT) {
                            onOpenPage(page)
                        }
                    }
                },
        ) {
            val cellPx = cell.toPx()
            val gapPx = gap.toPx()
            val radius = CornerRadius(2.5.dp.toPx())
            pageStates.forEachIndexed { index, state ->
                val row = index / perRow
                val column = index % perRow
                val x = size.width - (column + 1) * cellPx - column * gapPx
                val y = row * (cellPx + gapPx)
                drawRoundRect(
                    color = stateColor(state),
                    topLeft = Offset(x, y),
                    size = Size(cellPx, cellPx),
                    cornerRadius = radius,
                )
            }
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            MemorizationState.STRONG to R.string.progress_state_strong,
            MemorizationState.MEMORIZED to R.string.progress_state_memorized,
            MemorizationState.LEARNING to R.string.progress_state_learning,
            MemorizationState.NOT_STARTED to R.string.progress_state_not_started,
        ).forEach { (state, labelRes) ->
            val label = stringResource(labelRes)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    Modifier.size(9.dp).background(stateColor(state), RoundedCornerShape(2.5.dp)),
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ByJuzCard(juzProgress: List<JuzProgress>) {
    if (juzProgress.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 15.dp)) {
            Text(
                text = stringResource(R.string.progress_by_juz),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkMuted,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                juzProgress.forEach { juz -> JuzRow(juz) }
            }
        }
    }
}

@Composable
private fun JuzRow(juz: JuzProgress) {
    val fillColor = when (juz.status) {
        JuzStatus.COMPLETE -> AlkahfColors.AccentDeep
        JuzStatus.IN_PROGRESS -> AlkahfColors.Accent
        JuzStatus.LEARNING -> AlkahfColors.Learning
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.progress_juz_label, juz.juz),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkSecondaryDark,
            modifier = Modifier.width(64.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(7.dp)
                .background(AlkahfColors.MapEmpty, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(juz.fillFraction)
                    .height(7.dp)
                    .background(fillColor, RoundedCornerShape(4.dp)),
            )
        }
        Text(
            text = stringResource(R.string.progress_percent, juz.percent),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (juz.status == JuzStatus.COMPLETE) AlkahfColors.AccentDeep else AlkahfColors.InkSecondaryDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.width(38.dp),
        )
    }
}

@Composable
private fun ActivityRow(data: ProgressSnapshot) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ActivityTile(
            value = "${data.streakDays}",
            label = stringResource(R.string.progress_day_streak),
            modifier = Modifier.weight(1f),
        )
        ActivityTile(
            value = "${data.weekAyahCount}",
            label = stringResource(R.string.progress_ayat_this_week),
            modifier = Modifier.weight(1f),
        )
        ActivityTile(
            value = formatPracticeTime(data.totalPracticeMs),
            label = stringResource(R.string.progress_time_spent),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun formatPracticeTime(ms: Long): String {
    val minutes = ms / 60_000
    return if (minutes >= 60) {
        stringResource(R.string.progress_hours_value, minutes / 60f)
    } else {
        stringResource(R.string.progress_minutes_value, minutes)
    }
}

@Composable
private fun ActivityTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = AlkahfColors.InkFaint,
                maxLines = 1,
            )
        }
    }
}
