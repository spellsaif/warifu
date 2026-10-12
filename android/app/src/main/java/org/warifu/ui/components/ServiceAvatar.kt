package org.warifu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs

/**
 * Modern, deterministic brand/issuer avatar for 2FA accounts.
 * Provides instant visual recognition for popular services (Google, GitHub, Discord, AWS, etc.)
 * and falls back to elegant, harmonized gradient monograms for custom issuers.
 */
@Composable
fun ServiceAvatar(
    issuer: String,
    accountName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val effectiveName = issuer.ifBlank { accountName }.trim()
    val brandStyle = resolveBrandStyle(effectiveName)
    val monogram = extractMonogram(effectiveName)

    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(brush = brandStyle.backgroundBrush)
            .border(
                width = 1.dp,
                color = brandStyle.borderColor,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = brandStyle.textColor,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
    }
}

private data class BrandStyle(
    val backgroundBrush: Brush,
    val textColor: Color,
    val borderColor: Color = Color.White.copy(alpha = 0.12f)
)

private fun resolveBrandStyle(name: String): BrandStyle {
    val lower = name.lowercase(Locale.ROOT)

    return when {
        lower.contains("google") || lower.contains("gmail") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF34A853))),
            textColor = Color.White
        )
        lower.contains("github") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF24292E), Color(0xFF0D1117))),
            textColor = Color.White,
            borderColor = Color(0xFF404854)
        )
        lower.contains("gitlab") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFFFC6D26), Color(0xFFE24329))),
            textColor = Color.White
        )
        lower.contains("discord") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF5865F2), Color(0xFF4752C4))),
            textColor = Color.White
        )
        lower.contains("microsoft") || lower.contains("azure") || lower.contains("outlook") || lower.contains("office") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF0078D4), Color(0xFF002050))),
            textColor = Color.White
        )
        lower.contains("amazon") || lower.contains("aws") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFFFF9900), Color(0xFF232F3E))),
            textColor = Color.White
        )
        lower.contains("apple") || lower.contains("icloud") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF333333), Color(0xFF111111))),
            textColor = Color.White,
            borderColor = Color(0xFF555555)
        )
        lower.contains("steam") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF171A21), Color(0xFF1B2838))),
            textColor = Color(0xFF66C0F4),
            borderColor = Color(0xFF2A475E)
        )
        lower.contains("twitter") || lower.contains(" x") || lower == "x" -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF1DA1F2), Color(0xFF0C7ABF))),
            textColor = Color.White
        )
        lower.contains("reddit") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFFFF4500), Color(0xFFCC3700))),
            textColor = Color.White
        )
        lower.contains("facebook") || lower.contains("meta") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF1877F2), Color(0xFF0D5BBF))),
            textColor = Color.White
        )
        lower.contains("proton") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF6D4AFF), Color(0xFF4D22B2))),
            textColor = Color.White
        )
        lower.contains("binance") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFFF3BA2F), Color(0xFFB38615))),
            textColor = Color.Black
        )
        lower.contains("coinbase") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF0052FF), Color(0xFF0038B3))),
            textColor = Color.White
        )
        lower.contains("dropbox") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF0061FF), Color(0xFF0040A8))),
            textColor = Color.White
        )
        lower.contains("cloudflare") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFFF38020), Color(0xFFFAAE40))),
            textColor = Color.White
        )
        lower.contains("openai") || lower.contains("chatgpt") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF10A37F), Color(0xFF0B6B53))),
            textColor = Color.White
        )
        lower.contains("twitch") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF9146FF), Color(0xFF6422CC))),
            textColor = Color.White
        )
        lower.contains("slack") -> BrandStyle(
            backgroundBrush = Brush.linearGradient(listOf(Color(0xFF4A154B), Color(0xFF611F69))),
            textColor = Color.White
        )
        else -> {
            // Elegant curated gradient pairs derived deterministically from the string hash
            val palette = CURATED_GRADIENTS[abs(name.hashCode()) % CURATED_GRADIENTS.size]
            BrandStyle(
                backgroundBrush = Brush.linearGradient(listOf(palette.first, palette.second)),
                textColor = Color.White
            )
        }
    }
}

private val CURATED_GRADIENTS = listOf(
    Pair(Color(0xFF3B82F6), Color(0xFF1D4ED8)), // Sapphire
    Pair(Color(0xFF10B981), Color(0xFF047857)), // Emerald
    Pair(Color(0xFF8B5CF6), Color(0xFF6D28D9)), // Violet
    Pair(Color(0xFFF59E0B), Color(0xFFB45309)), // Amber
    Pair(Color(0xFFEC4899), Color(0xFFBE185D)), // Rose
    Pair(Color(0xFF06B6D4), Color(0xFF0E7490)), // Cyan
    Pair(Color(0xFF6366F1), Color(0xFF4338CA)), // Indigo
    Pair(Color(0xFF14B8A6), Color(0xFF0F766E))  // Teal
)

private fun extractMonogram(name: String): String {
    if (name.isBlank()) return "2F"
    val clean = name.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
    val parts = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase(Locale.ROOT)
        clean.length >= 2 -> clean.take(2).uppercase(Locale.ROOT)
        clean.isNotEmpty() -> clean.take(1).uppercase(Locale.ROOT)
        else -> "2F"
    }
}
