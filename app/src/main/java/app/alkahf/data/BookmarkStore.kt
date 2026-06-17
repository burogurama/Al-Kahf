package app.alkahf.data

import app.alkahf.data.user.BookmarkEntity
import app.alkahf.data.user.UserDao

/** How a bookmark is categorized. [NONE] is the default (no label chosen). */
enum class BookmarkLabel(val key: String) {
    REFLECTION("reflection"),
    TAFSIR("tafsir"),
    MEMORIZE("memorize"),
    DUA("dua"),
    NONE("none");

    companion object {
        fun of(key: String?): BookmarkLabel =
            entries.firstOrNull { it.key == key } ?: NONE
    }
}

/**
 * A saved bookmark with display fields resolved. [page]/[juz] are populated only
 * by [BookmarkStore.get] (the detail view); list rows leave them at 0.
 */
data class Bookmark(
    val id: Long,
    val surah: Int,
    val from: Int,
    val to: Int,
    val note: String,
    val label: BookmarkLabel,
    val createdAt: Long,
    val updatedAt: Long,
    val surahNameLatin: String,
    /** First āyah of the range, plain text — the single-line list snippet. */
    val snippet: String,
    val page: Int = 0,
    val juz: Int = 0,
) {
    val hasNote: Boolean get() = note.isNotBlank()
    val isSingleAyah: Boolean get() = from == to
}

/**
 * Stores the user's bookmarks — saved āyah ranges, each with an optional note and
 * a label. Persisted in the writable `user.db`; sūrah names, snippets, and the
 * page/juz are resolved from the read-only Qurʾān DB.
 */
class BookmarkStore(
    private val userDao: UserDao,
    private val quranText: QuranTextStore,
) {
    suspend fun add(surah: Int, from: Int, to: Int, note: String, label: BookmarkLabel, now: Long): Long =
        userDao.insertBookmark(
            BookmarkEntity(
                surah = surah,
                ayahFrom = from,
                ayahTo = to,
                note = note.trim().take(NOTE_MAX),
                label = label.key,
                createdAt = now,
                updatedAt = now,
            ),
        )

    suspend fun update(id: Long, note: String, label: BookmarkLabel, now: Long) =
        userDao.updateBookmark(id, note.trim().take(NOTE_MAX), label.key, now)

    suspend fun delete(id: Long) = userDao.deleteBookmark(id)

    /** All bookmarks, most-recently-updated first (list model: no page/juz). */
    suspend fun all(): List<Bookmark> = userDao.allBookmarks().map { e ->
        e.toBookmark(
            surahNameLatin = quranText.surahNameLatin(e.surah),
            snippet = firstAyahText(e.surah, e.ayahFrom),
        )
    }

    /** One bookmark with page + juz resolved, for the detail view. */
    suspend fun get(id: Long): Bookmark? {
        val e = userDao.bookmark(id) ?: return null
        val page = quranText.pageOfAyah(e.surah, e.ayahFrom)
        val juz = runCatching { quranText.page(page).juz }.getOrDefault(0)
        return e.toBookmark(
            surahNameLatin = quranText.surahNameLatin(e.surah),
            snippet = firstAyahText(e.surah, e.ayahFrom),
            page = page,
            juz = juz,
        )
    }

    private suspend fun firstAyahText(surah: Int, from: Int): String =
        quranText.ayahsForRange(surah, from, from)
            .firstOrNull()?.words?.joinToString(" ") ?: ""

    private fun BookmarkEntity.toBookmark(
        surahNameLatin: String,
        snippet: String,
        page: Int = 0,
        juz: Int = 0,
    ) = Bookmark(
        id = id,
        surah = surah,
        from = ayahFrom,
        to = ayahTo,
        note = note,
        label = BookmarkLabel.of(label),
        createdAt = createdAt,
        updatedAt = updatedAt,
        surahNameLatin = surahNameLatin,
        snippet = snippet,
        page = page,
        juz = juz,
    )

    companion object {
        const val NOTE_MAX = 500
    }
}
