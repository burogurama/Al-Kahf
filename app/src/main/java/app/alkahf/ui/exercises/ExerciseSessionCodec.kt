package app.alkahf.ui.exercises

import app.alkahf.data.exercises.AyatItem
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.ExerciseScope
import app.alkahf.data.exercises.ExerciseType
import org.json.JSONArray
import org.json.JSONObject

/** A session decoded from a persisted payload, ready to reload into the controller. */
data class DecodedSession(
    val scope: ExerciseScope,
    val types: Set<ExerciseType>,
    val length: Int,
    val questions: List<ExerciseQuestion>,
    val answers: List<QuestionState>,
    val currentIndex: Int,
    val durationMs: Long,
)

/**
 * Serializes an Exercises session to/from a compact JSON string for persistence.
 * Self-contained (uses [org.json]) so no serialization dependency is needed; the
 * questions carry their own answer keys, so a decoded session grades exactly like
 * a freshly generated one.
 */
object ExerciseSessionCodec {

    fun encode(
        scope: ExerciseScope,
        types: Set<ExerciseType>,
        length: Int,
        questions: List<ExerciseQuestion>,
        answers: List<QuestionState>,
        currentIndex: Int,
        durationMs: Long,
    ): String = JSONObject().apply {
        put("scope", encodeScope(scope))
        put("types", JSONArray(types.map { it.name }))
        put("length", length)
        put("currentIndex", currentIndex)
        put("durationMs", durationMs)
        put("questions", JSONArray(questions.map(::encodeQuestion)))
        put("answers", JSONArray(answers.map(::encodeAnswer)))
    }.toString()

    fun decode(payload: String): DecodedSession {
        val root = JSONObject(payload)
        val types = root.getJSONArray("types").let { arr ->
            (0 until arr.length()).map { ExerciseType.valueOf(arr.getString(it)) }.toSet()
        }
        val questions = root.getJSONArray("questions").let { arr ->
            (0 until arr.length()).map { decodeQuestion(arr.getJSONObject(it)) }
        }
        val answers = root.getJSONArray("answers").let { arr ->
            (0 until arr.length()).map { decodeAnswer(arr.getJSONObject(it)) }
        }
        return DecodedSession(
            scope = decodeScope(root.getJSONObject("scope")),
            types = types,
            length = root.optInt("length", questions.size),
            questions = questions,
            answers = answers,
            currentIndex = root.optInt("currentIndex", 0),
            durationMs = root.optLong("durationMs", 0L),
        )
    }

    // --- scope ---

    private fun encodeScope(scope: ExerciseScope): JSONObject = when (scope) {
        ExerciseScope.AllMemorized -> JSONObject().put("k", "all")
        is ExerciseScope.Surahs -> JSONObject().put("k", "surahs").put("ids", JSONArray(scope.ids))
    }

    private fun decodeScope(o: JSONObject): ExerciseScope = when (o.getString("k")) {
        "surahs" -> {
            val arr = o.getJSONArray("ids")
            ExerciseScope.Surahs((0 until arr.length()).map { arr.getInt(it) })
        }
        else -> ExerciseScope.AllMemorized
    }

    // --- questions ---

    private fun encodeQuestion(q: ExerciseQuestion): JSONObject = when (q) {
        is ExerciseQuestion.GuessSurah -> JSONObject()
            .put("t", "guess")
            .put("ayahId", q.ayahId)
            .put("ayahText", q.ayahText)
            .put("correctSurah", q.correctSurah)
        is ExerciseQuestion.FinishAyah -> JSONObject()
            .put("t", "finish")
            .put("ayahId", q.ayahId)
            .put("surah", q.surah)
            .put("number", q.number)
            .put("promptText", q.promptText)
            .put("continuationText", q.continuationText)
        is ExerciseQuestion.OrderAyat -> JSONObject()
            .put("t", "order")
            .put("surah", q.surah)
            .put("correct", JSONArray(q.correctOrder.map(::encodeItem)))
            .put("shuffled", JSONArray(q.shuffledOrder.map(::encodeItem)))
    }

    private fun decodeQuestion(o: JSONObject): ExerciseQuestion = when (o.getString("t")) {
        "finish" -> ExerciseQuestion.FinishAyah(
            ayahId = o.getInt("ayahId"),
            surah = o.getInt("surah"),
            number = o.getInt("number"),
            promptText = o.getString("promptText"),
            continuationText = o.getString("continuationText"),
        )
        "order" -> ExerciseQuestion.OrderAyat(
            surah = o.getInt("surah"),
            correctOrder = decodeItems(o.getJSONArray("correct")),
            shuffledOrder = decodeItems(o.getJSONArray("shuffled")),
        )
        else -> ExerciseQuestion.GuessSurah(
            ayahId = o.getInt("ayahId"),
            ayahText = o.getString("ayahText"),
            correctSurah = o.getInt("correctSurah"),
        )
    }

    private fun encodeItem(item: AyatItem): JSONObject = JSONObject()
        .put("id", item.id).put("number", item.number).put("text", item.text)

    private fun decodeItems(arr: JSONArray): List<AyatItem> =
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            AyatItem(o.getInt("id"), o.getInt("number"), o.getString("text"))
        }

    // --- answers ---

    private fun encodeAnswer(a: QuestionState): JSONObject = JSONObject().apply {
        put("status", a.status.name)
        a.pickedSurah?.let { put("picked", it) }
        if (a.written.isNotEmpty()) put("written", a.written)
        a.order?.let { put("order", JSONArray(it)) }
    }

    private fun decodeAnswer(o: JSONObject): QuestionState = QuestionState(
        status = AnswerStatus.valueOf(o.optString("status", AnswerStatus.UNANSWERED.name)),
        pickedSurah = if (o.has("picked")) o.getInt("picked") else null,
        written = o.optString("written", ""),
        order = if (o.has("order")) o.getJSONArray("order").let { arr -> (0 until arr.length()).map { arr.getInt(it) } } else null,
    )
}
