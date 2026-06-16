package app.alkahf.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * The user-selectable theme. [LIGHT]/[DARK]/[SYSTEM] use the default (green)
 * palette; [ROSE] is the "Rose & Blush" light reskin (always light — there is no
 * rose dark palette).
 */
enum class ThemeChoice { LIGHT, DARK, SYSTEM, ROSE }

private fun materialScheme(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = AlkahfColors.Accent,
        onPrimary = AlkahfColors.OnAccent,
        secondaryContainer = AlkahfColors.AccentTint,
        onSecondaryContainer = AlkahfColors.AccentDeep,
        background = AlkahfColors.Paper,
        onBackground = AlkahfColors.Ink,
        surface = AlkahfColors.Surface,
        onSurface = AlkahfColors.Ink,
        surfaceVariant = AlkahfColors.ChipBg,
        onSurfaceVariant = AlkahfColors.InkSecondary,
        outline = AlkahfColors.CardBorder,
        outlineVariant = AlkahfColors.CardBorder,
    )
} else {
    lightColorScheme(
        primary = AlkahfColors.Accent,
        onPrimary = AlkahfColors.OnAccent,
        secondaryContainer = AlkahfColors.AccentTint,
        onSecondaryContainer = AlkahfColors.AccentDeep,
        background = AlkahfColors.Paper,
        onBackground = AlkahfColors.Ink,
        surface = AlkahfColors.Surface,
        onSurface = AlkahfColors.Ink,
        surfaceVariant = AlkahfColors.ChipBg,
        onSurfaceVariant = AlkahfColors.InkSecondary,
        outline = AlkahfColors.CardBorder,
        outlineVariant = AlkahfColors.CardBorder,
    )
}

@Composable
fun AlkahfTheme(
    choice: ThemeChoice = ThemeChoice.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (choice) {
        ThemeChoice.LIGHT, ThemeChoice.ROSE -> false
        ThemeChoice.DARK -> true
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
    }
    val rose = choice == ThemeChoice.ROSE
    // Swap the token palette before content reads it this composition.
    remember(dark, rose) { AlkahfColors.select(dark, rose); dark }
    MaterialTheme(
        colorScheme = materialScheme(dark),
        typography = AlkahfTypography,
        content = content,
    )
}
