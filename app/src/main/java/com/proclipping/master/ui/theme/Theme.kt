package com.proclipping.master.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorErrorSoft = androidx.compose.ui.graphics.Color(0xFFFCA5A5)

private val ProClippingDarkColorScheme = darkColorScheme(
    primary = ElectricPurple,
    onPrimary = InkPrimary,
    primaryContainer = DeepPurpleGlow,
    onPrimaryContainer = VividViolet,
    secondary = ElectricBlue,
    onSecondary = InkPrimary,
    secondaryContainer = DeepBlueGlow,
    onSecondaryContainer = NeonSky,
    tertiary = NeonSky,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = InkPrimary,
    surface = DeepCharcoal,
    onSurface = InkPrimary,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepCharcoal,
    surfaceContainer = CharcoalCard,
    surfaceContainerHigh = CharcoalElevated,
    surfaceContainerHighest = HairlineBorder,
    outline = HairlineBorder,
    outlineVariant = HairlineHighlight,
    error = StatusCrimson,
    onError = InkPrimary,
    errorContainer = StatusCrimsonBg,
    onErrorContainer = ColorErrorSoft
)

val ProClippingShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ProClippingMasterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProClippingDarkColorScheme,
        typography = ProClippingTypography,
        shapes = ProClippingShapes,
        content = content
    )
}
