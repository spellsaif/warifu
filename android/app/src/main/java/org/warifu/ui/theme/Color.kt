package org.warifu.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Warifu Palette (Inspired by ui.png & logo.png)
// ============================================================================
val SakuraPink = Color(0xFFFF637E)               // Primary Warifu Sakura Pink
val SakuraPinkMuted = Color(0xFFFF859B)          // Soft petal highlight
val SakuraPinkGlow = Color(0x33FF637E)           // Subtle glow / selection tint
val SakuraDark = Color(0xFFCC4A62)               // Deep pressed state

val WarifuBackground = Color(0xFF0B0C0E)         // Deep obsidian background
val WarifuSurface = Color(0xFF141519)            // Elevated card background
val WarifuSurfaceElevated = Color(0xFF1C1D22)    // Secondary elevated / interactive surface
val WarifuBorder = Color(0xFF22242C)             // Subtle hairline border
val WarifuBorderSubtle = Color(0xFF181A20)       // Divider stroke
val WarifuTextPrimary = Color(0xFFFFFFFF)        // High contrast primary text
val WarifuTextSecondary = Color(0xFF8E929E)      // Muted metadata text
val WarifuTextMuted = Color(0xFF555865)          // Helper captions

// ============================================================================
// Monochromatic & Minimalist Palette
// ============================================================================
val MonoDarkBackground = WarifuBackground
val MonoDarkSurface = WarifuSurface
val MonoDarkSurfaceElevated = WarifuSurfaceElevated
val MonoDarkBorder = WarifuBorder
val MonoDarkBorderSubtle = WarifuBorderSubtle
val MonoDarkTextPrimary = WarifuTextPrimary
val MonoDarkTextSecondary = WarifuTextSecondary
val MonoDarkTextMuted = WarifuTextMuted
val MonoDarkAction = SakuraPink
val MonoDarkActionText = Color(0xFFFFFFFF)

// ============================================================================
// Minimalist Light Mode
// ============================================================================
val MonoLightBackground = Color(0xFFF9FAFB)
val MonoLightSurface = Color(0xFFF3F4F6)
val MonoLightSurfaceElevated = Color(0xFFFFFFFF)
val MonoLightBorder = Color(0xFFE4E4E7)
val MonoLightBorderSubtle = Color(0xFFF4F4F5)
val MonoLightTextPrimary = Color(0xFF09090B)
val MonoLightTextSecondary = Color(0xFF52525B)
val MonoLightTextMuted = Color(0xFFA1A1AA)
val MonoLightAction = SakuraPink
val MonoLightActionText = Color(0xFFFFFFFF)

// ============================================================================
// Functional Semantic Status (Urgency & Error Only)
// ============================================================================
val StatusWarning = Color(0xFFF59E0B) // Amber
val StatusCritical = Color(0xFFF87171) // Red (under 4 seconds / delete action)
val StatusSuccess = Color(0xFF10B981) // Emerald

// Backward compatibility tokens
val PrimaryBlue = SakuraPink
val AccentCyan = SakuraPinkMuted
val TimerGreen = SakuraPink
val TimerAmber = StatusWarning
val TimerRed = StatusCritical
val DarkSurface = MonoDarkSurface
val DarkCard = MonoDarkSurfaceElevated
val TextPrimary = MonoDarkTextPrimary
val TextSecondary = MonoDarkTextSecondary

