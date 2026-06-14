package app.alkahf.ui.khatam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import app.alkahf.data.toArabicIndic
import app.alkahf.ui.theme.AlkahfColors

/**
 * The khatam progress ring: a [KhatamRingTrack] full circle with a
 * [KhatamProgressFill] arc swept clockwise from 12 o'clock (−90°) by
 * [fraction] of a turn, with a rounded cap. [center] is drawn over the ring
 * (percent + "x / 30 juzʼ").
 */
@Composable
internal fun KhatamRing(
    fraction: Float,
    diameter: Dp,
    stroke: Dp = 13.dp,
    center: @Composable () -> Unit,
) {
    val track = AlkahfColors.KhatamRingTrack
    val fill = AlkahfColors.KhatamProgressFill
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx),
            )
            val sweep = (fraction.coerceIn(0f, 1f)) * 360f
            if (sweep > 0f) {
                drawArc(
                    color = fill,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        center()
    }
}

/**
 * The 30-juzʼ map: an RTL row of equal bars (juzʼ 1 on the right). Bars before
 * [doneCount] are [KhatamProgressFill] (done); the [todayJuz] bar is
 * [KhatamToday]; the rest are [KhatamUpcoming]. [barHeight] tunes the variant
 * (tracker vs. mini log track). Optional Eastern-digit axis ticks below.
 */
@Composable
internal fun JuzMap(
    doneCount: Int,
    todayJuz: Int,
    barHeight: Dp,
    modifier: Modifier = Modifier,
    showAxis: Boolean = false,
) {
    Column(modifier) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                for (juz in 1..30) {
                    val color = when {
                        juz <= doneCount -> AlkahfColors.KhatamProgressFill
                        juz == todayJuz -> AlkahfColors.KhatamToday
                        else -> AlkahfColors.KhatamUpcoming
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
        if (showAxis) {
            // LTR axis with the row reversed so ticks read ١ ١٠ ٢٠ ٣٠ under the
            // RTL bars (juzʼ 1 sits at the right edge).
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(30, 20, 10, 1).forEach { tick ->
                    Text(
                        text = tick.toArabicIndic(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkFainter,
                    )
                }
            }
        }
    }
}
