package app.alkahf.audio

enum class LoopMode { SINGLE, RANGE, CHAIN }

/**
 * One audio play in a drill: which ayah sounds, the span being repeated,
 * which pass this is, and the ayah that joins the chain after this block.
 */
data class LoopStep(
    val ayah: Int,
    val spanStart: Int,
    val spanEnd: Int,
    val pass: Int,
    val passCount: Int,
    val nextToAdd: Int?,
)

/**
 * Expands a drill configuration into the flat list of plays.
 *
 * Chain mode follows the handoff algorithm: 1×N, 2×N, 1–2×M, 3×N, 1–3×M …
 * — each new ayah is drilled solo ×N, then the chain grown to include it
 * runs ×M passes. A silent recite-back gap follows every play.
 */
object LoopSequencer {
    fun steps(
        mode: LoopMode,
        start: Int,
        end: Int,
        perAyah: Int,
        perChain: Int,
    ): List<LoopStep> = when (mode) {
        LoopMode.SINGLE ->
            (1..perAyah).map { pass -> LoopStep(start, start, start, pass, perAyah, null) }

        LoopMode.RANGE ->
            (1..perChain).flatMap { pass ->
                (start..end).map { ayah -> LoopStep(ayah, start, end, pass, perChain, null) }
            }

        LoopMode.CHAIN -> buildList {
            for (newAyah in start..end) {
                for (pass in 1..perAyah) {
                    add(LoopStep(newAyah, newAyah, newAyah, pass, perAyah, null))
                }
                if (newAyah > start) {
                    val joiningNext = (newAyah + 1).takeIf { it <= end }
                    for (pass in 1..perChain) {
                        for (ayah in start..newAyah) {
                            add(LoopStep(ayah, start, newAyah, pass, perChain, joiningNext))
                        }
                    }
                }
            }
        }
    }
}
