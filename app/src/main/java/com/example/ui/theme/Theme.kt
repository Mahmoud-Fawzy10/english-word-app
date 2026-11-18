package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BlueLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = IceBlueContainer,
    onPrimaryContainer = DeepNavy,
    secondary = SkyAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFF4FF),
    onSecondaryContainer = Color(0xFF063B5C),
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF1E1B4B),
    background = SoftIceBlue,
    onBackground = DeepNavy,
    surface = CardWhite,
    onSurface = DeepNavy,
    surfaceVariant = Color(0xFFEAF1FF),
    onSurfaceVariant = MutedBlueSlate,
    outline = IceBlueBorder,
    outlineVariant = Color(0xFFDCE7FF),
    error = ErrorSoft,
    onError = Color.White
)

private val BlueDarkColorScheme = darkColorScheme(
    primary = LightSkyPrimary,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF133A7C),
    onPrimaryContainer = IceTextLight,
    secondary = LightCyanSecondary,
    onSecondary = Color(0xFF042638),
    secondaryContainer = Color(0xFF0E3D5A),
    onSecondaryContainer = Color(0xFFD6F3FF),
    tertiary = Color(0xFF93C5FD),
    onTertiary = DeepNavy,
    background = DarkOceanBg,
    onBackground = IceTextLight,
    surface = DarkOceanSurface,
    onSurface = IceTextLight,
    surfaceVariant = DarkOceanSurfaceHigh,
    onSurfaceVariant = MutedIceText,
    outline = Color(0xFF28477D),
    outlineVariant = Color(0xFF1D3561),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val QamusiShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QamusiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QamusiTypography,
        shapes = QamusiShapes,
        content = content
    )
}
