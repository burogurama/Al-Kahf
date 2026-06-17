package app.alkahf.ui.bookmarks

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.alkahf.R
import app.alkahf.data.Bookmark
import app.alkahf.data.BookmarkLabel
import app.alkahf.ui.theme.AlkahfColors

/** Resolved colors for a label's dot/spine, chip background, and chip text. */
data class LabelColors(val accent: Color, val chipBg: Color, val chipText: Color)

@Composable
fun labelColors(label: BookmarkLabel): LabelColors = when (label) {
    BookmarkLabel.REFLECTION -> LabelColors(AlkahfColors.Accent, AlkahfColors.AccentTint2, AlkahfColors.AccentDeep)
    BookmarkLabel.TAFSIR -> LabelColors(AlkahfColors.BookmarkRibbon, AlkahfColors.GoldBg, AlkahfColors.GoldText)
    BookmarkLabel.MEMORIZE -> LabelColors(AlkahfColors.LabelViolet, AlkahfColors.LabelVioletBg, AlkahfColors.LabelVioletText)
    BookmarkLabel.DUA -> LabelColors(AlkahfColors.LabelTerracotta, AlkahfColors.LabelTerracottaBg, AlkahfColors.LabelTerracottaText)
    BookmarkLabel.NONE -> LabelColors(AlkahfColors.BookmarkRibbon, AlkahfColors.ChipBg, AlkahfColors.InkMuted)
}

/** The label's display name, or null for [BookmarkLabel.NONE]. */
@Composable
fun labelName(label: BookmarkLabel): String? = when (label) {
    BookmarkLabel.REFLECTION -> stringResource(R.string.bookmark_label_reflection)
    BookmarkLabel.TAFSIR -> stringResource(R.string.bookmark_label_tafsir)
    BookmarkLabel.MEMORIZE -> stringResource(R.string.bookmark_label_memorize)
    BookmarkLabel.DUA -> stringResource(R.string.bookmark_label_dua)
    BookmarkLabel.NONE -> null
}

/** "Al-Kahf · 9–10" for a range, or "Al-Baqarah · 255" for a single āyah. */
@Composable
fun bookmarkTitle(surahName: String, from: Int, to: Int): String =
    if (from == to) {
        stringResource(R.string.bookmark_single_title, surahName, from)
    } else {
        stringResource(R.string.bookmark_range_title, surahName, from, to)
    }

@Composable
fun bookmarkTitle(bookmark: Bookmark): String =
    bookmarkTitle(bookmark.surahNameLatin, bookmark.from, bookmark.to)

/** A relative day stamp: "Today · 9:32", "Yesterday", "3 days ago", "12 Jun". */
fun bookmarkDate(context: Context, millis: Long): String =
    if (DateUtils.isToday(millis)) {
        context.getString(
            R.string.bookmark_today_at,
            DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME),
        )
    } else {
        DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.DAY_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
    }

/** A short relative stamp for the "edited" tile: "2 min ago". */
fun bookmarkRelative(context: Context, millis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        millis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
