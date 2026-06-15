package app.alkahf.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.alkahf.R
import app.alkahf.data.exercises.ExerciseType
import app.alkahf.ui.theme.AlkahfColors

/** Converts Western digits in [s] to Eastern Arabic-Indic numerals (muṣḥaf context only). */
fun toEasternArabicDigits(s: String): String = buildString {
    for (c in s) append(if (c in '0'..'9') EASTERN_DIGITS[c - '0'] else c)
}

fun toEasternArabicDigits(n: Int): String = toEasternArabicDigits(n.toString())

private val EASTERN_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

/** The colour role of a type chip: green (Guess), gold (Finish), sage (Order). */
data class TypeChipColors(val bg: Color, val ink: Color)

@Composable
fun typeChipColors(type: ExerciseType): TypeChipColors = when (type) {
    ExerciseType.GUESS_SURAH -> TypeChipColors(AlkahfColors.AccentTint2, AlkahfColors.AccentDeep)
    ExerciseType.FINISH_AYAH -> TypeChipColors(AlkahfColors.GoldBg, AlkahfColors.GoldText)
    ExerciseType.ORDER_AYAT -> TypeChipColors(AlkahfColors.KeyboardUthmaniBg, AlkahfColors.KeyboardUthmaniGlyph)
}

@Composable
fun typeLabel(type: ExerciseType): String = stringResource(
    when (type) {
        ExerciseType.GUESS_SURAH -> R.string.ex_type_guess_name
        ExerciseType.FINISH_AYAH -> R.string.ex_type_finish_name
        ExerciseType.ORDER_AYAT -> R.string.ex_type_order_name
    },
)

/** A small pill carrying the exercise type, coloured by its role. */
@Composable
fun TypeChip(type: ExerciseType, modifier: Modifier = Modifier) {
    val colors = typeChipColors(type)
    Box(
        modifier = modifier
            .background(colors.bg, RoundedCornerShape(9.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(
            text = typeLabel(type).uppercase(),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = colors.ink,
        )
    }
}

/** A shared section kicker: 11sp, bold, 1.4px tracking, uppercase, muted. */
@Composable
fun SectionKicker(text: String, modifier: Modifier = Modifier, color: Color = AlkahfColors.InkMuted) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp,
        color = color,
        modifier = modifier,
    )
}

/**
 * The shared pushed-flow top bar: a ✕ close on the leading edge, a centered
 * title + subtitle. Mirrors the Khatam program / Review top bars.
 */
@Composable
fun ExercisesTopBar(title: String, subtitle: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = AlkahfColors.InkChrome,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlkahfColors.Ink)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AlkahfColors.InkFaint,
                )
            }
        }
        Spacer(Modifier.width(40.dp))
    }
}
