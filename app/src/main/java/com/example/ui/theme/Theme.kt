package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

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

@Immutable
data class QamusiSemanticColors(
    val isDark: Boolean,
    val synonymBg: Color,
    val synonymText: Color,
    val antonymBg: Color,
    val antonymText: Color,
    val importantNoteBg: Color,
    val importantNoteBorder: Color,
    val warningBg: Color,
    val warningBorder: Color,
    val warningText: Color,
    val successBg: Color,
    val successBorder: Color,
    val successText: Color,
    val dangerBg: Color,
    val dangerBorder: Color,
    val dangerText: Color,
    val srsAgainBg: Color,
    val srsAgainText: Color,
    val srsHardBg: Color,
    val srsHardText: Color,
    val srsGoodBg: Color,
    val srsGoodText: Color,
    val srsEasyBg: Color,
    val srsEasyText: Color,
    val bookMoasserBg: Color,
    val bookMoasserText: Color,
    val bookA1Bg: Color,
    val bookA1Text: Color,
    val bookMinistryBg: Color,
    val bookMinistryText: Color,
    val bookCustomBg: Color,
    val bookCustomText: Color
)

private val LightSemanticColors = QamusiSemanticColors(
    isDark = false,
    synonymBg = SynonymChipBg,
    synonymText = SynonymChipText,
    antonymBg = AntonymChipBg,
    antonymText = AntonymChipText,
    importantNoteBg = ImportantNoteBg,
    importantNoteBorder = ImportantNoteBorder,
    warningBg = Color(0xFFFFFBEB),
    warningBorder = Color(0xFFF59E0B),
    warningText = Color(0xFF92400E),
    successBg = Color(0xFFDCFCE7),
    successBorder = Color(0xFF16A34A),
    successText = Color(0xFF14532D),
    dangerBg = Color(0xFFFEE2E2),
    dangerBorder = Color(0xFFDC2626),
    dangerText = Color(0xFF7F1D1D),
    srsAgainBg = Color(0xFFFEE2E2),
    srsAgainText = Color(0xFFB91C1C),
    srsHardBg = Color(0xFFFEF3C7),
    srsHardText = Color(0xFFB45309),
    srsGoodBg = Color(0xFFDBEAFE),
    srsGoodText = Color(0xFF1D4ED8),
    srsEasyBg = Color(0xFFDCFCE7),
    srsEasyText = Color(0xFF15803D),
    bookMoasserBg = BookMoasserBg,
    bookMoasserText = BookMoasserText,
    bookA1Bg = BookA1Bg,
    bookA1Text = BookA1Text,
    bookMinistryBg = BookMinistryBg,
    bookMinistryText = BookMinistryText,
    bookCustomBg = BookCustomBg,
    bookCustomText = BookCustomText
)

private val DarkSemanticColors = QamusiSemanticColors(
    isDark = true,
    synonymBg = Color(0xFF0C3B5E),
    synonymText = Color(0xFFBAE6FD),
    antonymBg = Color(0xFF1E293B),
    antonymText = Color(0xFFCBD5E1),
    importantNoteBg = Color(0xFF112A54),
    importantNoteBorder = Color(0xFF3B82F6),
    warningBg = Color(0xFF3A280A),
    warningBorder = Color(0xFFF59E0B),
    warningText = Color(0xFFFDE68A),
    successBg = Color(0xFF0F3622),
    successBorder = Color(0xFF22C55E),
    successText = Color(0xFFBBF7D0),
    dangerBg = Color(0xFF421418),
    dangerBorder = Color(0xFFEF4444),
    dangerText = Color(0xFFFECACA),
    srsAgainBg = Color(0xFF451419),
    srsAgainText = Color(0xFFFCA5A5),
    srsHardBg = Color(0xFF422C08),
    srsHardText = Color(0xFFFCD34D),
    srsGoodBg = Color(0xFF163268),
    srsGoodText = Color(0xFF93C5FD),
    srsEasyBg = Color(0xFF0F3822),
    srsEasyText = Color(0xFF86EFAC),
    bookMoasserBg = Color(0xFF17366E),
    bookMoasserText = Color(0xFFBFDBFE),
    bookA1Bg = Color(0xFF0E3C5C),
    bookA1Text = Color(0xFFBAE6FD),
    bookMinistryBg = Color(0xFF222B68),
    bookMinistryText = Color(0xFFC7D2FE),
    bookCustomBg = Color(0xFF143161),
    bookCustomText = Color(0xFF93C5FD)
)

val LocalQamusiColors = staticCompositionLocalOf { LightSemanticColors }

val QamusiShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun QamusiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) BlueDarkColorScheme else BlueLightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalQamusiColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = QamusiTypography,
            shapes = QamusiShapes,
            content = content
        )
    }
}
