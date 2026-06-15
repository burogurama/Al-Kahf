package app.alkahf.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.AlkahfApplication
import app.alkahf.R
import app.alkahf.data.exercises.ExerciseScope
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.data.exercises.SurahChoice
import app.alkahf.ui.theme.AlkahfColors
import app.alkahf.ui.theme.AmiriQuran

private val LENGTHS = listOf(5, 10, 15, 20)

/** Whether the test covers everything memorized, or a hand-picked sūrah set. */
private enum class ScopeMode { ALL, PICK }

/**
 * Frame 01 — "Set up a test". The user chooses the scope (all memorized, or a
 * picked sūrah set), the exercise types (≥1 required), and the length, then
 * generates the session. [onGenerate] builds the controller's session and the
 * caller pushes the runner.
 */
@Composable
fun ExercisesSetupScreen(
    controller: ExercisesController,
    onClose: () -> Unit,
    onGenerate: (ExerciseScope, Set<ExerciseType>, Int) -> Unit,
) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    LaunchedEffect(Unit) { controller.loadChoices() }

    var scopeMode by remember { mutableStateOf(ScopeMode.ALL) }
    val pickedSurahs = remember { mutableStateMapOf<Int, Boolean>() }
    val enabledTypes = remember {
        mutableStateMapOf(
            ExerciseType.GUESS_SURAH to true,
            ExerciseType.FINISH_AYAH to true,
            ExerciseType.ORDER_AYAT to true,
        )
    }
    var lengthIndex by remember { mutableIntStateOf(1) } // default 10

    val selectedIds = pickedSurahs.filter { it.value }.keys.toList()
    val types = enabledTypes.filter { it.value }.keys.toSet()
    val length = LENGTHS[lengthIndex]
    val scopeValid = scopeMode == ScopeMode.ALL || selectedIds.isNotEmpty()
    val canGenerate = types.isNotEmpty() && scopeValid

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.Paper)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ExercisesTopBar(
            title = stringResource(R.string.ex_setup_title),
            subtitle = stringResource(R.string.ex_setup_subtitle),
            onClose = onClose,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionKicker(
                text = stringResource(R.string.ex_setup_what_to_test),
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
            )
            ScopeSegmented(scopeMode) { scopeMode = it }
            if (scopeMode == ScopeMode.PICK) {
                SurahPicker(
                    choices = state.surahChoices,
                    picked = pickedSurahs,
                    onToggle = { id -> pickedSurahs[id] = !(pickedSurahs[id] ?: false) },
                )
                SelectionFooter(count = selectedIds.size)
            }

            SectionKicker(
                text = stringResource(R.string.ex_setup_exercise_types),
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            TypeSwitchRow(
                name = stringResource(R.string.ex_type_guess_name),
                description = stringResource(R.string.ex_type_guess_desc),
                checked = enabledTypes[ExerciseType.GUESS_SURAH] == true,
                onToggle = { enabledTypes[ExerciseType.GUESS_SURAH] = it },
            )
            TypeSwitchRow(
                name = stringResource(R.string.ex_type_finish_name),
                description = stringResource(R.string.ex_type_finish_desc),
                checked = enabledTypes[ExerciseType.FINISH_AYAH] == true,
                onToggle = { enabledTypes[ExerciseType.FINISH_AYAH] = it },
            )
            TypeSwitchRow(
                name = stringResource(R.string.ex_type_order_name),
                description = stringResource(R.string.ex_type_order_desc),
                checked = enabledTypes[ExerciseType.ORDER_AYAT] == true,
                onToggle = { enabledTypes[ExerciseType.ORDER_AYAT] = it },
            )
            if (types.isEmpty()) {
                Text(
                    text = stringResource(R.string.ex_setup_pick_one_type),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.ClayText,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }

            SectionKicker(
                text = stringResource(R.string.ex_setup_length),
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            LengthSegmented(selectedIndex = lengthIndex, onSelect = { lengthIndex = it })
            Spacer(Modifier.height(6.dp))
        }
        GenerateDock(
            length = length,
            enabled = canGenerate,
            onGenerate = {
                val chosenScope = if (scopeMode == ScopeMode.ALL) {
                    ExerciseScope.AllMemorized
                } else {
                    ExerciseScope.Surahs(selectedIds)
                }
                onGenerate(chosenScope, types, length)
            },
        )
    }
}

@Composable
private fun ScopeSegmented(mode: ScopeMode, onSelect: (ScopeMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AlkahfColors.ExerciseSegmentedTrack, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        listOf(
            ScopeMode.ALL to stringResource(R.string.ex_setup_all_memorized),
            ScopeMode.PICK to stringResource(R.string.ex_setup_pick_surahs),
        ).forEach { (option, label) ->
            val isSelected = option == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .let {
                        if (isSelected) it.background(AlkahfColors.SegmentedSelected, RoundedCornerShape(9.dp)) else it
                    }
                    .clickable { onSelect(option) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) AlkahfColors.AccentDeep else AlkahfColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun SurahPicker(
    choices: List<SurahChoice>,
    picked: Map<Int, Boolean>,
    onToggle: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        // A bounded, scrollable list so the long sūrah set never pushes the dock
        // off-screen; the outer column scrolls past it once chosen.
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
        ) {
            items(choices, key = { it.number }) { choice ->
                SurahRow(
                    choice = choice,
                    checked = picked[choice.number] == true,
                    onToggle = { onToggle(choice.number) },
                )
            }
        }
    }
}

@Composable
private fun SurahRow(choice: SurahChoice, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GreenCheckbox(checked = checked)
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Text(
                text = choice.nameLatin,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
            )
            Text(
                text = stringResource(R.string.ex_setup_surah_number, choice.number),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AlkahfColors.InkFaint,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = choice.nameArabic,
                fontSize = 19.sp,
                fontFamily = AmiriQuran,
                color = AlkahfColors.InkSecondaryDark,
            )
        }
    }
}

@Composable
private fun GreenCheckbox(checked: Boolean) {
    if (checked) {
        Box(
            Modifier.size(22.dp).background(AlkahfColors.Accent, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = AlkahfColors.OnAccent,
                modifier = Modifier.size(15.dp),
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = AlkahfColors.Surface,
            border = BorderStroke(1.5.dp, AlkahfColors.Chevron),
            modifier = Modifier.size(22.dp),
        ) {}
    }
}

@Composable
private fun SelectionFooter(count: Int) {
    Text(
        text = stringResource(
            R.string.ex_setup_selection_footer,
            pluralStringResource(R.plurals.ex_surah_count, count, count),
        ),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = AlkahfColors.InkMuted,
        modifier = Modifier.padding(start = 2.dp, top = 2.dp),
    )
}

@Composable
private fun TypeSwitchRow(
    name: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AlkahfColors.Surface,
        border = BorderStroke(1.dp, AlkahfColors.CardBorder),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.Ink)
                Text(
                    text = description,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AlkahfColors.Accent,
                    checkedThumbColor = AlkahfColors.OnAccent,
                ),
            )
        }
    }
}

@Composable
private fun LengthSegmented(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(AlkahfColors.ExerciseSegmentedTrack, RoundedCornerShape(12.dp))
            .padding(3.dp),
    ) {
        LENGTHS.forEachIndexed { index, value ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .let {
                        if (isSelected) it.background(AlkahfColors.SegmentedSelected, RoundedCornerShape(9.dp)) else it
                    }
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$value",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) AlkahfColors.AccentDeep else AlkahfColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun GenerateDock(length: Int, enabled: Boolean, onGenerate: () -> Unit) {
    Surface(color = AlkahfColors.Paper) {
        Box(Modifier.padding(start = 18.dp, top = 8.dp, end = 18.dp, bottom = 14.dp)) {
            Button(
                onClick = onGenerate,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = if (enabled) 5.dp else 0.dp,
                        shape = RoundedCornerShape(15.dp),
                        ambientColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                        spotColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                    ),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlkahfColors.Accent,
                    contentColor = AlkahfColors.OnAccent,
                    disabledContainerColor = AlkahfColors.NotStarted,
                    disabledContentColor = AlkahfColors.InkFaint,
                ),
            ) {
                Text(
                    text = stringResource(R.string.ex_setup_generate, length),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
