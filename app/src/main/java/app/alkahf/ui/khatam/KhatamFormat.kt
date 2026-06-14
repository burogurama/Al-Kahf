package app.alkahf.ui.khatam

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.alkahf.R
import app.alkahf.data.KhatamPortion
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Sat · 5 Jul" style finish/start label for an epoch day. */
internal fun epochDayLabel(epochDay: Long): String =
    LocalDate.ofEpochDay(epochDay)
        .format(DateTimeFormatter.ofPattern("EEE · d MMM", Locale.getDefault()))

/** "5:10 AM"-style label for a reminder time (minutes after midnight). */
internal fun minuteOfDayLabel(minuteOfDay: Int): String =
    LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

/** Rough minutes for a portion: ≈ 1 min per page (an even read pace). */
internal fun portionMinutes(portion: KhatamPortion): Int =
    portion.pageCount.coerceAtLeast(1)

/** "SurahFrom from → SurahTo to" range label, using resolved surah names. */
@Composable
internal fun portionRangeLabel(portion: KhatamPortion, surahName: (Int) -> String): String =
    stringResource(
        R.string.khatam_portion_range,
        surahName(portion.surahFrom),
        portion.ayahFrom,
        surahName(portion.surahTo),
        portion.ayahTo,
    )
