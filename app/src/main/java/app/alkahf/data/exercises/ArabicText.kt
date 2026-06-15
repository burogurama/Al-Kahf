package app.alkahf.data.exercises

/**
 * Pure Arabic-text normalisation for grading the "Finish the Āyah" exercise.
 *
 * The handoff's grading policy (the "Validation" section) is: grade on the bare
 * consonant skeleton (the *rasm*), with tashkīl and ʿUthmānī annotation marks
 * optional — "minor tashkīl differences are accepted". A ḥāfiẓ who recalls the
 * āyah perfectly but types plain (non-ʿUthmānī) Arabic, or omits ḥarakāt, must
 * still score correct. So both the user's input and the reference text are
 * reduced to the same skeleton before comparison.
 *
 * No Android dependencies — this is plain Kotlin over Unicode code points and is
 * fully unit-reasoned from the ranges below.
 */
object ArabicText {

    /**
     * Reduce [s] to its bare consonant skeleton: strip every diacritic and
     * Qurʾānic annotation mark, fold the visually/orthographically equivalent
     * letter variants together, and collapse whitespace.
     *
     * The transformation is, in order:
     *  1. Drop all combining marks and standalone annotation symbols (see
     *     [isStripped]) — ḥarakāt, tanwīn, shadda, sukūn, the dagger alif, the
     *     ʿUthmānī small marks and waqf signs, tatweel, and the zero-width joins.
     *  2. Fold letter variants to a single canonical consonant (see [fold]):
     *     all alif forms → ا, alif-maqṣūra → ي, tāʾ-marbūṭa → ه, and the hamza
     *     seats → their carrier (ؤ → و, ئ → ي) with the standalone hamza dropped.
     *  3. Collapse any run of whitespace to a single space and trim the ends.
     */
    fun normalizeRasm(s: String): String {
        val out = StringBuilder(s.length)
        var pendingSpace = false
        for (ch in s) {
            if (ch.isWhitespace()) {
                // Defer spaces so trailing/leading/duplicate runs collapse away.
                if (out.isNotEmpty()) pendingSpace = true
                continue
            }
            if (isStripped(ch)) continue
            val folded = fold(ch) ?: continue // null = drop (e.g. standalone hamza)
            if (pendingSpace) {
                out.append(' ')
                pendingSpace = false
            }
            out.append(folded)
        }
        return out.toString()
    }

    /**
     * True when the user's [input] reproduces the [reference] āyah text on the
     * rasm level: equal after [normalizeRasm] (which is whitespace-insensitive
     * beyond single spaces). This is the grader for "Finish the Āyah".
     */
    fun matchesAyah(input: String, reference: String): Boolean =
        normalizeRasm(input) == normalizeRasm(reference)

    /**
     * Characters removed outright — they carry no rasm. Each Unicode class:
     *  - U+0610–U+061A  Arabic honorifics / Qurʾānic annotation signs above letters.
     *  - U+064B–U+065F  tanwīn, the three short vowels, sukūn, shadda, and the
     *                    extended Arabic combining marks (small high/low vowels).
     *  - U+0670         the superscript (dagger) alif.
     *  - U+06D6–U+06DC  small high Qurʾānic words and the waqf cluster start.
     *  - U+06DF–U+06E8  the round/rectangular zero sukūn, small high marks,
     *                    iqlāb mīm, and the small high seen/letter marks.
     *  - U+06EA–U+06ED  empty-centre low stop, small wāw/yāʾ/nūn ʿUthmānī marks.
     *  - U+0640         tatweel (kashīda) — a pure elongation glyph, never rasm.
     *  - U+08D3–U+08FF  the Arabic Extended-B combining block (rare Qurʾānic marks).
     *  - U+200C/U+200D  ZWNJ / ZWJ — invisible joiners that don't change the skeleton.
     */
    private fun isStripped(ch: Char): Boolean {
        val c = ch.code
        return c in 0x0610..0x061A ||
            c in 0x064B..0x065F ||
            c == 0x0670 ||
            c in 0x06D6..0x06DC ||
            c in 0x06DF..0x06E8 ||
            c in 0x06EA..0x06ED ||
            c == 0x0640 ||
            c in 0x08D3..0x08FF ||
            c == 0x200C || c == 0x200D
    }

    /**
     * Map a (non-stripped) character to its canonical skeleton form. Returns the
     * character unchanged when it carries no equivalence, or `null` when it is a
     * standalone hamza (U+0621 ء) — which has no consonant carrier and so is
     * dropped so e.g. "السماء" and "السما" compare equal.
     *
     * Folds:
     *  - أ إ آ ٱ (U+0623/0625/0622/0671) → ا  — all alif seats are one consonant.
     *  - ى (U+0649) → ي  — alif-maqṣūra reads as yāʾ on the skeleton.
     *  - ة (U+0629) → ه  — tāʾ-marbūṭa and final hāʾ are not distinguished.
     *  - ؤ (U+0624) → و,  ئ (U+0626) → ي  — hamza on a wāw/yāʾ seat folds to the seat.
     */
    private fun fold(ch: Char): Char? = when (ch.code) {
        0x0623, 0x0625, 0x0622, 0x0671 -> 'ا'
        0x0649 -> 'ي'
        0x0629 -> 'ه'
        0x0624 -> 'و'
        0x0626 -> 'ي'
        0x0621 -> null
        else -> ch
    }
}
