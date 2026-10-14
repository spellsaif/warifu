package org.warifu.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// True Monochromatic System (Inspired by Linear / Raycast / Apple Keychain)
// ============================================================================
val WarifuBackground = Color(0xFF09090B)         // Deep obsidian canvas (Zinc-950)
val WarifuSurface = Color(0xFF121215)            // Subtle dark card surface
val WarifuSurfaceElevated = Color(0xFF1A1A1E)    // Elevated dropdowns, dialogs, inputs
val WarifuBorder = Color(0xFF242429)             // Hairline subtle border
val WarifuBorderSubtle = Color(0xFF1C1C20)       // Divider stroke
val WarifuTextPrimary = Color(0xFFFAFAFA)        // Zinc-50 crisp high-contrast white
val WarifuTextSecondary = Color(0xFFA1A1AA)      // Zinc-400 balanced neutral gray
val WarifuTextMuted = Color(0xFF71717A)          // Zinc-500 captions

// Monochromatic High-Contrast Actions
val MonoAction = Color(0xFFFFFFFF)               // Pure white active trigger
val MonoActionText = Color(0xFF09090B)           // Dark text on white button
val MonoActionInactive = Color(0xFF18181C)       // Unselected chip surface
val MonoActionInactiveText = Color(0xFFA1A1AA)   // Unselected chip text

// Subtle Brand Sakura Touch (Optional highlight)
val SakuraPink = Color(0xFFFF637E)
val SakuraPinkMuted = Color(0xFFFF859B)

// Functional Semantic Urgency
val StatusWarning = Color(0xFFF59E0B)            // Amber (under 7s)
val StatusCritical = Color(0xFFEF4444)           // Red (under 4s & delete action)
val StatusSuccess = Color(0xFF10B981)            // Emerald

// Backwards compatibility mappings
val MonoDarkBackground = WarifuBackground
val MonoDarkSurface = WarifuSurface
val MonoDarkSurfaceElevated = WarifuSurfaceElevated
val MonoDarkBorder = WarifuBorder
val MonoDarkBorderSubtle = WarifuBorderSubtle
val MonoDarkTextPrimary = WarifuTextPrimary
val MonoDarkTextSecondary = WarifuTextSecondary
val MonoDarkTextMuted = WarifuTextMuted
val MonoDarkAction = MonoAction
val MonoDarkActionText = MonoActionText

val MonoLightBackground = Color(0xFFF9FAFB)
val MonoLightSurface = Color(0xFFF3F4F6)
val MonoLightSurfaceElevated = Color(0xFFFFFFFF)
val MonoLightBorder = Color(0xFFE4E4E7)
val MonoLightBorderSubtle = Color(0xFFF4F4F5)
val MonoLightTextPrimary = Color(0xFF09090B)
val MonoLightTextSecondary = Color(0xFF52525B)
val MonoLightTextMuted = Color(0xFFA1A1AA)
val MonoLightAction = Color(0xFF09090B)
val MonoLightActionText = Color(0xFFFFFFFF)

val PrimaryBlue = MonoAction
val AccentCyan = MonoActionInactiveText
val TimerGreen = WarifuTextPrimary
val TimerAmber = StatusWarning
val TimerRed = StatusCritical
val DarkSurface = MonoDarkSurface
val DarkCard = MonoDarkSurfaceElevated
val TextPrimary = MonoDarkTextPrimary
val TextSecondary = MonoDarkTextSecondary


