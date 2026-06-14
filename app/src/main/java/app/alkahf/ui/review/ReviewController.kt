package app.alkahf.ui.review

import app.alkahf.data.QuranRepository
import app.alkahf.data.ReviewPortion
import app.alkahf.data.WordStumble
import app.alkahf.data.review.ReviewGrade
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Loads the due murājaʿah queue and commits each portion's grade and stumbles.
 * The per-portion self-test progress (reveals, stumbles, chosen grade) is UI
 * state and stays in the screen.
 */
class ReviewController(private val repository: QuranRepository) {
    private val _portions = MutableStateFlow<List<ReviewPortion>?>(null)
    val portions: StateFlow<List<ReviewPortion>?> = _portions.asStateFlow()

    /** SM-2 growth factor and stumble auto-lowering, read once for the session. */
    val growthFactor: Double = repository.reviewPacing.growthFactor
    val autoLower: Boolean = repository.autoLowerOnStumble

    suspend fun load() {
        _portions.value = repository.dueReviewPortions()
    }

    suspend fun commit(portion: ReviewPortion, grade: ReviewGrade, stumbles: List<WordStumble>) {
        repository.commitReviewGrade(portion, grade)
        stumbles.forEach { repository.addStumble(it) }
    }
}
