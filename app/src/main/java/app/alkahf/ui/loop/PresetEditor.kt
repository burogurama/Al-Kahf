package app.alkahf.ui.loop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.LoopPreset
import app.alkahf.data.SurahOption
import app.alkahf.data.audio.RECITERS
import app.alkahf.ui.theme.AlkahfColors

/**
 * Full-screen drill preset editor: surah, ayah range, reciter, counts, gap,
 * speed. Saving persists the preset and restarts the drill with it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetEditor(
    initial: LoopPreset,
    surahs: List<SurahOption>,
    onSave: (LoopPreset) -> Unit,
    onDismiss: () -> Unit,
) {
    var surahIndex by remember {
        mutableStateOf(surahs.indexOfFirst { it.number == initial.surah }.coerceAtLeast(0))
    }
    var ayahFrom by remember { mutableStateOf(initial.ayahFrom) }
    var ayahTo by remember { mutableStateOf(initial.ayahTo) }
    var reciter by remember {
        mutableStateOf(RECITERS.firstOrNull { it.path == initial.reciterPath } ?: RECITERS.first())
    }
    var perAyah by remember { mutableStateOf(initial.perAyah) }
    var perChain by remember { mutableStateOf(initial.perChain) }
    var gap by remember { mutableStateOf(initial.gapMultiplier) }
    var speed by remember { mutableStateOf(initial.speed) }
    var isDefault by remember { mutableStateOf(initial.isDefault) }

    val surah = surahs.getOrNull(surahIndex) ?: return
    val maxTo = minOf(ayahFrom + LoopController.MAX_SPAN - 1, surah.ayahCount)
    val presetName = stringResource(R.string.loop_preset_name, surah.nameLatin, ayahFrom, ayahTo)

    fun clampRange() {
        ayahFrom = ayahFrom.coerceIn(1, surah.ayahCount)
        ayahTo = ayahTo.coerceIn(ayahFrom, minOf(ayahFrom + LoopController.MAX_SPAN - 1, surah.ayahCount))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AlkahfColors.SessionBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.loop_close_editor),
                    tint = AlkahfColors.InkChrome,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = stringResource(R.string.loop_drill_preset),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AlkahfColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(40.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            EditorCard(stringResource(R.string.loop_card_passage)) {
                StepperRow(
                    label = stringResource(R.string.loop_field_surah),
                    value = stringResource(R.string.loop_surah_value, surah.number, surah.nameLatin),
                    onDecrement = {
                        if (surahIndex > 0) {
                            surahIndex--
                            ayahFrom = 1
                            ayahTo = minOf(5, surahs[surahIndex].ayahCount)
                        }
                    },
                    onIncrement = {
                        if (surahIndex < surahs.lastIndex) {
                            surahIndex++
                            ayahFrom = 1
                            ayahTo = minOf(5, surahs[surahIndex].ayahCount)
                        }
                    },
                )
                StepperRow(
                    label = stringResource(R.string.loop_field_from_ayah),
                    value = "$ayahFrom",
                    onDecrement = { ayahFrom = (ayahFrom - 1).coerceAtLeast(1); clampRange() },
                    onIncrement = { ayahFrom = (ayahFrom + 1).coerceAtMost(surah.ayahCount); clampRange() },
                )
                StepperRow(
                    label = stringResource(R.string.loop_field_to_ayah),
                    value = "$ayahTo",
                    onDecrement = { ayahTo = (ayahTo - 1).coerceAtLeast(ayahFrom) },
                    onIncrement = { ayahTo = (ayahTo + 1).coerceAtMost(maxTo) },
                )
            }
            EditorCard(stringResource(R.string.loop_card_reciter)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    RECITERS.forEach { option ->
                        val selected = option.path == reciter.path
                        Surface(
                            onClick = { reciter = option },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) AlkahfColors.AccentTint else AlkahfColors.ChipBg,
                            border = BorderStroke(
                                1.dp,
                                if (selected) AlkahfColors.Accent else AlkahfColors.CardBorder,
                            ),
                        ) {
                            Text(
                                text = option.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) AlkahfColors.AccentDeep else AlkahfColors.InkSecondaryDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
            EditorCard(stringResource(R.string.loop_card_drill)) {
                StepperRow(
                    label = stringResource(R.string.loop_field_per_ayah),
                    value = "$perAyah×",
                    onDecrement = { perAyah = (perAyah - 1).coerceAtLeast(1) },
                    onIncrement = { perAyah = (perAyah + 1).coerceAtMost(10) },
                )
                StepperRow(
                    label = stringResource(R.string.loop_field_per_chain),
                    value = "$perChain×",
                    onDecrement = { perChain = (perChain - 1).coerceAtLeast(1) },
                    onIncrement = { perChain = (perChain + 1).coerceAtMost(10) },
                )
                StepperRow(
                    label = stringResource(R.string.loop_field_recite_back_gap),
                    value = formatFactor(gap),
                    onDecrement = { gap = (gap - 0.5f).coerceAtLeast(0.5f) },
                    onIncrement = { gap = (gap + 0.5f).coerceAtMost(3f) },
                )
                StepperRow(
                    label = stringResource(R.string.loop_field_speed),
                    value = formatFactor(speed),
                    onDecrement = { speed = (speed - 0.25f).coerceAtLeast(0.75f) },
                    onIncrement = { speed = (speed + 0.25f).coerceAtMost(1.5f) },
                )
            }
            EditorCard(stringResource(R.string.loop_card_default)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.loop_resume_uses_preset),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AlkahfColors.InkSecondaryDark,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        onClick = { isDefault = !isDefault },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDefault) AlkahfColors.AccentTint else AlkahfColors.ChipBg,
                        border = BorderStroke(
                            1.dp,
                            if (isDefault) AlkahfColors.Accent else AlkahfColors.CardBorder,
                        ),
                    ) {
                        Text(
                            text = if (isDefault) {
                                stringResource(R.string.loop_default_badge)
                            } else {
                                stringResource(R.string.loop_make_default)
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDefault) AlkahfColors.AccentDeep else AlkahfColors.InkMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
        Button(
            onClick = {
                onSave(
                    LoopPreset(
                        id = initial.id,
                        name = presetName,
                        surah = surah.number,
                        surahNameLatin = surah.nameLatin,
                        ayahFrom = ayahFrom,
                        ayahTo = ayahTo,
                        reciterPath = reciter.path,
                        reciterName = reciter.displayName,
                        perAyah = perAyah,
                        perChain = perChain,
                        gapMultiplier = gap,
                        speed = speed,
                        isDefault = isDefault,
                    ),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(54.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                    spotColor = AlkahfColors.Accent.copy(alpha = 0.28f),
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AlkahfColors.Accent,
                contentColor = AlkahfColors.OnAccent,
            ),
        ) {
            Text(
                text = stringResource(R.string.loop_save_start_drill),
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatFactor(value: Float): String =
    if (value == value.toInt().toFloat()) "${value.toInt()}×" else "$value×"

@Composable
private fun EditorCard(caption: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.LoopCardBorder),
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = caption,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = AlkahfColors.InkFooter,
            )
            content()
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlkahfColors.InkSecondaryDark,
            modifier = Modifier.weight(1f),
        )
        StepperButton("−", AlkahfColors.InkMuted, onDecrement)
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = AlkahfColors.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.width(124.dp),
        )
        StepperButton("+", AlkahfColors.AccentDeep, onIncrement)
    }
}

@Composable
private fun StepperButton(glyph: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = AlkahfColors.PageSurface,
        border = BorderStroke(1.dp, AlkahfColors.ControlBorder),
        modifier = Modifier.size(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = glyph, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
