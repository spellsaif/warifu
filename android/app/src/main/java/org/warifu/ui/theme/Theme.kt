package org.warifu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val MonochromaticDarkColorScheme = darkColorScheme(
    primary = MonoDarkAction,
    onPrimary = MonoDarkActionText,
    primaryContainer = MonoDarkSurfaceElevated,
    onPrimaryContainer = MonoDarkTextPrimary,
    secondary = MonoDarkTextSecondary,
    onSecondary = MonoDarkBackground,
    secondaryContainer = MonoDarkSurface,
    onSecondaryContainer = MonoDarkTextSecondary,
    tertiary = MonoDarkTextMuted,
    background = MonoDarkBackground,
    surface = MonoDarkSurfaceElevated,
    surfaceVariant = MonoDarkSurface,
    onBackground = MonoDarkTextPrimary,
    onSurface = MonoDarkTextPrimary,
    onSurfaceVariant = MonoDarkTextSecondary,
    outline = MonoDarkBorder,
    outlineVariant = MonoDarkBorderSubtle,
    error = StatusCritical
)

private val MinimalistLightColorScheme = lightColorScheme(
    primary = MonoLightAction,
    onPrimary = MonoLightActionText,
    primaryContainer = MonoLightSurface,
    onPrimaryContainer = MonoLightTextPrimary,
    secondary = MonoLightTextSecondary,
    onSecondary = MonoLightBackground,
    secondaryContainer = MonoLightSurface,
    onSecondaryContainer = MonoLightTextSecondary,
    tertiary = MonoLightTextMuted,
    background = MonoLightBackground,
    surface = MonoLightSurfaceElevated,
    surfaceVariant = MonoLightSurface,
    onBackground = MonoLightTextPrimary,
    onSurface = MonoLightTextPrimary,
    onSurfaceVariant = MonoLightTextSecondary,
    outline = MonoLightBorder,
    outlineVariant = MonoLightBorderSubtle,
    error = StatusCritical
)

val WarifuTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.4).sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.2).sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = (-0.1).sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = 0.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun WarifuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MonochromaticDarkColorScheme else MinimalistLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WarifuTypography,
        content = content
    )
}
