package com.proclipping.master.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Core Obsidian & Charcoal Surfaces
val ObsidianBlack = Color(0xFF07080D)
val DeepCharcoal = Color(0xFF0F111A)
val CharcoalCard = Color(0xFF151824)
val CharcoalElevated = Color(0xFF1D2132)
val CharcoalInput = Color(0xFF0C0E16)
val HairlineBorder = Color(0xFF282D44)
val HairlineHighlight = Color(0xFF3A4163)

// Purple & Electric Blue Gradient Accents
val ElectricPurple = Color(0xFF8B5CF6)
val VividViolet = Color(0xFFA78BFA)
val DeepPurpleGlow = Color(0xFF4C1D95)
val ElectricBlue = Color(0xFF3B82F6)
val NeonSky = Color(0xFF38BDF8)
val DeepBlueGlow = Color(0xFF1E3A8A)

// Text Ink Tokens
val InkPrimary = Color(0xFFF8FAFC)
val InkSecondary = Color(0xFFCBD5E1)
val InkMuted = Color(0xFF8893AD)
val InkSubtle = Color(0xFF5D6782)

// Semantic Status Tokens
val StatusEmerald = Color(0xFF10B981)
val StatusEmeraldBg = Color(0xFF0D2B24)
val StatusAmber = Color(0xFFF59E0B)
val StatusAmberBg = Color(0xFF2B200B)
val StatusCrimson = Color(0xFFEF4444)
val StatusCrimsonBg = Color(0xFF2D1116)

// Reusable Brand Brushes
val PrimaryGradientBrush = Brush.horizontalGradient(
    colors = listOf(ElectricPurple, ElectricBlue)
)

val AccentDiagonalBrush = Brush.linearGradient(
    colors = listOf(VividViolet, ElectricBlue, NeonSky)
)

val SubtleBorderGradientBrush = Brush.linearGradient(
    colors = listOf(
        ElectricPurple.copy(alpha = 0.55f),
        ElectricBlue.copy(alpha = 0.35f),
        HairlineBorder
    )
)
