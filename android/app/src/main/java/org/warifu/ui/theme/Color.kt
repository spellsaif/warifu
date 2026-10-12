package org.warifu.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Monochromatic Dark Mode (Shades of Pure & Neutral Dark)
// ============================================================================
val MonoDarkBackground = Color(0xFF09090B)       // Zinc-950
val MonoDarkSurface = Color(0xFF131316)          // Refined dark surface
val MonoDarkSurfaceElevated = Color(0xFF1C1C20)  // Elevated card background
val MonoDarkBorder = Color(0xFF27272A)           // Hairline subtle stroke (Zinc-800)
val MonoDarkBorderSubtle = Color(0xFF1E1E22)     // Ultra-soft card divider
val MonoDarkTextPrimary = Color(0xFFFAFAFA)      // Zinc-50
val MonoDarkTextSecondary = Color(0xFFA1A1AA)    // Zinc-400
val MonoDarkTextMuted = Color(0xFF71717A)        // Zinc-500
val MonoDarkAction = Color(0xFFFFFFFF)           // Pure white high-contrast action
val MonoDarkActionText = Color(0xFF09090B)       // Dark text on white button

// ============================================================================
// Minimalist Light Mode (Crisp Paper & Neutral Grays)
// ============================================================================
val MonoLightBackground = Color(0xFFF9FAFB)      // Clean off-white background
val MonoLightSurface = Color(0xFFF3F4F6)         // Soft gray surface
val MonoLightSurfaceElevated = Color(0xFFFFFFFF) // Pure white card
val MonoLightBorder = Color(0xFFE4E4E7)          // Subtle card border (Zinc-200)
val MonoLightBorderSubtle = Color(0xFFF4F4F5)    // Divider
val MonoLightTextPrimary = Color(0xFF09090B)     // Zinc-950
val MonoLightTextSecondary = Color(0xFF52525B)   // Zinc-600
val MonoLightTextMuted = Color(0xFFA1A1AA)       // Zinc-400
val MonoLightAction = Color(0xFF09090B)          // Pitch black action
val MonoLightActionText = Color(0xFFFFFFFF)      // White text on dark button

// ============================================================================
// Functional Semantic Status (Urgency & Error Only)
// ============================================================================
val StatusWarning = Color(0xFFF59E0B) // Amber (under 7 seconds)
val StatusCritical = Color(0xFFEF4444) // Red (under 4 seconds)
val StatusSuccess = Color(0xFF10B981) // Emerald

// Backward compatibility tokens
val PrimaryBlue = Color(0xFF2563EB)
val AccentCyan = Color(0xFF06B6D4)
val TimerGreen = Color(0xFF10B981)
val TimerAmber = Color(0xFFF59E0B)
val TimerRed = Color(0xFFEF4444)
val DarkSurface = MonoDarkSurface
val DarkCard = MonoDarkSurfaceElevated
val TextPrimary = MonoDarkTextPrimary
val TextSecondary = MonoDarkTextSecondary
