package app.alkahf.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.alkahf.R

@OptIn(ExperimentalTextApi::class)
private fun hanken(weight: FontWeight) = Font(
    resId = R.font.hanken_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val HankenGrotesk = FontFamily(
    hanken(FontWeight.Normal),
    hanken(FontWeight.Medium),
    hanken(FontWeight.SemiBold),
    hanken(FontWeight.Bold),
)

val AmiriQuran = FontFamily(
    Font(R.font.amiri_quran, weight = FontWeight.Normal),
)

/** KFGQPC Uthmanic Script HAFS — the Hafs mushaf font. */
val KfgqpcHafs = FontFamily(
    Font(R.font.kfgqpc_hafs, weight = FontWeight.Normal),
)

/** KFGQPC Uthmanic Script WARSH — the Warsh mushaf font. */
val KfgqpcWarsh = FontFamily(
    Font(R.font.kfgqpc_warsh, weight = FontWeight.Normal),
)

/** The mushaf font for a riwāyah ("hafs" | "warsh"). */
fun quranFontFor(riwayah: String): FontFamily =
    if (riwayah == "warsh") KfgqpcWarsh else KfgqpcHafs

/** The Qur'an font for the active riwāyah, provided at each screen root. */
val LocalQuranFont = staticCompositionLocalOf { KfgqpcHafs }

val AlkahfTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = HankenGrotesk),
        displayMedium = displayMedium.copy(fontFamily = HankenGrotesk),
        displaySmall = displaySmall.copy(fontFamily = HankenGrotesk),
        headlineLarge = headlineLarge.copy(fontFamily = HankenGrotesk),
        headlineMedium = headlineMedium.copy(fontFamily = HankenGrotesk),
        headlineSmall = headlineSmall.copy(fontFamily = HankenGrotesk),
        titleLarge = titleLarge.copy(fontFamily = HankenGrotesk),
        titleMedium = titleMedium.copy(fontFamily = HankenGrotesk),
        titleSmall = titleSmall.copy(fontFamily = HankenGrotesk),
        bodyLarge = bodyLarge.copy(fontFamily = HankenGrotesk),
        bodyMedium = bodyMedium.copy(fontFamily = HankenGrotesk),
        bodySmall = bodySmall.copy(fontFamily = HankenGrotesk),
        labelLarge = labelLarge.copy(fontFamily = HankenGrotesk),
        labelMedium = labelMedium.copy(fontFamily = HankenGrotesk),
        labelSmall = labelSmall.copy(fontFamily = HankenGrotesk),
    )
}

/** Ayah text style: 25sp, line-height 1.88. Override fontFamily per riwāyah. */
val AyahTextStyle = TextStyle(
    fontFamily = KfgqpcHafs,
    fontWeight = FontWeight.Normal,
    fontSize = 25.sp,
    lineHeight = 47.sp,
    color = AlkahfColors.Ink,
)
