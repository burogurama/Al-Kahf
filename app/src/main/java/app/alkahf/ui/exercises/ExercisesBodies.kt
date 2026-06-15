package app.alkahf.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.exercises.AyatItem
import app.alkahf.data.exercises.ExerciseQuestion
import app.alkahf.data.exercises.SurahChoice
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran

// ── Question heading + Arabic prompt card (shared by the bodies) ─────────────

@Composable
private fun QuestionHeading(text: String) {
    Text(
        text = text,
        fontSize = 18.5.sp,
        fontWeight = FontWeight.Bold,
        color = AlkahfColors.Ink,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

/** An Amiri āyah / prompt in an alt-card, RTL and justified. */
@Composable
private fun AyahPromptCard(text: String, trailingEllipsis: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = if (trailingEllipsis) "$text …" else text,
                fontFamily = AmiriQuran,
                fontSize = 24.sp,
                lineHeight = 46.sp,
                color = AlkahfColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp),
            )
        }
    }
}

// ── 02 · Guess the Sūrah ─────────────────────────────────────────────────────

@Composable
fun GuessSurahBody(
    question: ExerciseQuestion.GuessSurah,
    state: QuestionState,
    choices: List<SurahChoice>,
    answered: Boolean,
    onPick: (Int) -> Unit,
) {
    QuestionHeading(stringResource(R.string.ex_guess_heading))
    AyahPromptCard(question.ayahText)
    Spacer(Modifier.height(16.dp))
    SectionKicker(stringResource(R.string.ex_guess_type_name))
    Spacer(Modifier.height(8.dp))

    // The field shows the picked sūrah's name; typing re-opens the type-ahead.
    val pickedName = state.pickedSurah?.let { id -> choices.firstOrNull { it.number == id }?.nameLatin }
    var query by remember(state.pickedSurah) { mutableStateOf(pickedName ?: "") }

    val matches = remember(query, choices) {
        if (query.isBlank()) {
            emptyList()
        } else {
            val q = query.trim().lowercase()
            choices.filter {
                it.nameLatin.lowercase().contains(q) || it.nameArabic.contains(query.trim())
            }.take(8)
        }
    }
    // Hide the dropdown once a sūrah whose name exactly fills the field is picked.
    val showDropdown = !answered && matches.isNotEmpty() &&
        !(state.pickedSurah != null && query == pickedName)

    SearchField(
        query = query,
        enabled = !answered,
        matchCount = if (showDropdown) matches.size else null,
        onQueryChange = { query = it },
    )
    if (showDropdown) {
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = AlkahfColors.Surface,
            border = BorderStroke(1.dp, AlkahfColors.CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                matches.forEachIndexed { index, choice ->
                    val selected = choice.number == state.pickedSurah
                    SurahMatchRow(
                        choice = choice,
                        selected = selected,
                        onClick = {
                            onPick(choice.number)
                            query = choice.nameLatin
                        },
                    )
                    if (index != matches.lastIndex) {
                        Box(
                            Modifier.fillMaxWidth().height(1.dp)
                                .padding(horizontal = 14.dp)
                                .background(AlkahfColors.Hairline),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, enabled: Boolean, matchCount: Int?, onQueryChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = AlkahfColors.InkFaint,
                modifier = Modifier.size(18.dp),
            )
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = app.alkahf.ui.theme.HankenGrotesk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.Ink,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AlkahfColors.Accent),
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.ex_guess_field_hint),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = AlkahfColors.InkFaint,
                        )
                    }
                    inner()
                },
            )
            if (matchCount != null) {
                Text(
                    text = stringResource(R.string.ex_guess_matches, matchCount),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AlkahfColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun SurahMatchRow(choice: SurahChoice, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) AlkahfColors.ExerciseSelectedCard else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = choice.nameLatin,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) AlkahfColors.AccentDeep else AlkahfColors.Ink,
                )
                Text(
                    text = stringResource(R.string.ex_guess_match_number, choice.number),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = choice.nameArabic,
                fontSize = 18.sp,
                fontFamily = AmiriQuran,
                color = if (selected) AlkahfColors.AccentDeep else AlkahfColors.InkSecondaryDark,
            )
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AlkahfColors.Accent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── 03 / 06 · Finish the Āyah ────────────────────────────────────────────────

@Composable
fun FinishAyahBody(
    question: ExerciseQuestion.FinishAyah,
    state: QuestionState,
    answered: Boolean,
) {
    val correct = state.status == AnswerStatus.CORRECT
    QuestionHeading(stringResource(R.string.ex_finish_heading))
    AyahPromptCard(question.promptText, trailingEllipsis = true)
    Spacer(Modifier.height(16.dp))
    SectionKicker(stringResource(R.string.ex_finish_your_answer))
    Spacer(Modifier.height(8.dp))

    // The live answer box: written Arabic + a caret while typing; once checked it
    // turns green (correct) or clay (not quite, with the right continuation below).
    val boxBorder = when {
        !answered -> AlkahfColors.CardBorder
        correct -> AlkahfColors.Accent
        else -> AlkahfColors.ClayBorder
    }
    val boxBg = when {
        !answered -> AlkahfColors.Surface
        correct -> AlkahfColors.ExerciseSelectedCard
        else -> AlkahfColors.ClayBg
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = boxBg,
        border = BorderStroke(if (answered) 1.5.dp else 1.dp, boxBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.written.ifEmpty { if (answered) "" else "" },
                    fontFamily = AmiriQuran,
                    fontSize = 22.sp,
                    lineHeight = 40.sp,
                    color = if (answered && !correct) AlkahfColors.ClayTextSoft else AlkahfColors.ArabicAnswerInk,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!answered) {
                    Spacer(Modifier.width(2.dp))
                    Box(
                        Modifier.width(2.dp).height(26.dp).background(AlkahfColors.Accent),
                    )
                }
            }
        }
    }

    if (answered && correct) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = AlkahfColors.Accent,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = stringResource(R.string.ex_finish_matches_note),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkSecondary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    } else if (answered) {
        // Not quite — reveal the correct continuation.
        Spacer(Modifier.height(8.dp))
        SectionKicker(stringResource(R.string.ex_finish_correct_continuation), color = AlkahfColors.ClayTextSoft)
        Spacer(Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AlkahfColors.PageSurface,
            border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = question.continuationText,
                    fontFamily = AmiriQuran,
                    fontSize = 22.sp,
                    lineHeight = 42.sp,
                    color = AlkahfColors.ArabicAnswerInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }

}

// ── 04 · Order the Āyāt ──────────────────────────────────────────────────────

@Composable
fun OrderAyatBody(
    question: ExerciseQuestion.OrderAyat,
    state: QuestionState,
    answered: Boolean,
    surahName: String,
    positions: List<Boolean>,
    onMove: (index: Int, delta: Int) -> Unit,
) {
    QuestionHeading(stringResource(R.string.ex_order_heading))
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = AlkahfColors.ChipBg,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Text(
            text = surahName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.InkSecondaryDark,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
    Spacer(Modifier.height(12.dp))
    SectionKicker(stringResource(R.string.ex_order_your_order))
    Spacer(Modifier.height(8.dp))

    val order = state.order ?: question.shuffledOrder.map { it.id }
    val byId = remember(question) { question.shuffledOrder.associateBy { it.id } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        order.forEachIndexed { index, id ->
            val item = byId[id] ?: return@forEachIndexed
            val positionCorrect = positions.getOrNull(index)
            OrderRow(
                position = index + 1,
                item = item,
                answered = answered,
                positionCorrect = positionCorrect,
                canMoveUp = !answered && index > 0,
                canMoveDown = !answered && index < order.lastIndex,
                onUp = { onMove(index, -1) },
                onDown = { onMove(index, 1) },
            )
        }
    }

    if (answered) {
        Spacer(Modifier.height(16.dp))
        SectionKicker(stringResource(R.string.ex_order_correct_order))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.correctOrder.forEachIndexed { index, item ->
                CorrectOrderRow(position = index + 1, item = item)
            }
        }
    }
}

@Composable
private fun OrderRow(
    position: Int,
    item: AyatItem,
    answered: Boolean,
    positionCorrect: Boolean?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    // Badge colour: neutral while ordering; green/clay once checked per position.
    val badgeBg = when (positionCorrect) {
        true -> AlkahfColors.Accent
        false -> AlkahfColors.ClayChip
        null -> AlkahfColors.ChipBg
    }
    val badgeInk = when (positionCorrect) {
        true -> AlkahfColors.OnAccent
        false -> AlkahfColors.ClayText
        null -> AlkahfColors.InkSecondaryDark
    }
    val rowBorder = when (positionCorrect) {
        false -> AlkahfColors.ClayBorder
        else -> AlkahfColors.CardBorder
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (positionCorrect == false) AlkahfColors.ClayBg else AlkahfColors.Surface,
        border = BorderStroke(1.dp, rowBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(26.dp).background(badgeBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                when (positionCorrect) {
                    true -> Icon(Icons.Outlined.Check, null, tint = badgeInk, modifier = Modifier.size(16.dp))
                    false -> Icon(Icons.Outlined.Close, null, tint = badgeInk, modifier = Modifier.size(16.dp))
                    null -> Text("$position", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = badgeInk)
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = item.text,
                    fontFamily = AmiriQuran,
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    color = if (positionCorrect == false) AlkahfColors.ClayTextSoft else AlkahfColors.Ink,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
            }
            if (!answered) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MoveButton(Icons.Filled.KeyboardArrowUp, enabled = canMoveUp, onClick = onUp)
                    MoveButton(Icons.Filled.KeyboardArrowDown, enabled = canMoveDown, onClick = onDown)
                }
            }
        }
    }
}

@Composable
private fun MoveButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(28.dp)
            .border(1.dp, AlkahfColors.ControlBorder, RoundedCornerShape(8.dp))
            .let { if (enabled) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) AlkahfColors.InkSecondary else AlkahfColors.InkFainter,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CorrectOrderRow(position: Int, item: AyatItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AlkahfColors.AccentTint2,
        border = BorderStroke(1.dp, AlkahfColors.CardBorderHero),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(26.dp).background(AlkahfColors.Accent, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Eastern Arabic-Indic ordinal — muṣḥaf context.
                Text(
                    text = toEasternArabicDigits(position),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlkahfColors.OnAccent,
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = item.text,
                    fontFamily = AmiriQuran,
                    fontSize = 20.sp,
                    lineHeight = 36.sp,
                    color = AlkahfColors.AccentDeep,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                )
            }
        }
    }
}
