package org.warifu.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Authentic brand avatar matching ui.png for popular services (GitHub, Google, Microsoft, Discord, Binance, Solana, etc.)
 * with clean fallback monogram.
 */
@Composable
fun ServiceAvatar(
    issuer: String,
    accountName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val effectiveName = issuer.ifBlank { accountName }.trim().lowercase(Locale.ROOT)
    val shape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .border(
                width = 1.dp,
                color = Color(0xFF22242C),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            effectiveName.contains("github") -> GitHubAvatar(size)
            effectiveName.contains("google") -> GoogleAvatar(size)
            effectiveName.contains("microsoft") || effectiveName.contains("outlook") || effectiveName.contains("office") -> MicrosoftAvatar(size)
            effectiveName.contains("discord") -> DiscordAvatar(size)
            effectiveName.contains("binance") -> BinanceAvatar(size)
            effectiveName.contains("solana") || effectiveName.contains("phantom") -> SolanaAvatar(size)
            effectiveName.contains("facebook") || effectiveName.contains("meta") -> FacebookAvatar(size)
            effectiveName.contains("instagram") -> InstagramAvatar(size)
            effectiveName.contains("amazon") || effectiveName.contains("aws") -> AmazonAvatar(size)
            else -> FallbackMonogram(issuer = issuer, accountName = accountName, size = size)
        }
    }
}

@Composable
private fun GitHubAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF0D1117)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.65f)) {
            val w = this.size.width
            val h = this.size.height

            // GitHub Cat silhouette
            val headPath = Path().apply {
                moveTo(w * 0.5f, h * 0.12f)
                cubicTo(w * 0.28f, h * 0.12f, w * 0.12f, h * 0.28f, w * 0.12f, h * 0.5f)
                cubicTo(w * 0.12f, h * 0.68f, w * 0.24f, h * 0.82f, w * 0.4f, h * 0.88f)
                cubicTo(w * 0.42f, h * 0.88f, w * 0.43f, h * 0.87f, w * 0.43f, h * 0.86f)
                lineTo(w * 0.43f, h * 0.77f)
                cubicTo(w * 0.32f, h * 0.79f, w * 0.30f, h * 0.72f, w * 0.30f, h * 0.72f)
                cubicTo(w * 0.28f, h * 0.67f, w * 0.25f, h * 0.66f, w * 0.25f, h * 0.66f)
                cubicTo(w * 0.22f, h * 0.64f, w * 0.25f, h * 0.64f, w * 0.25f, h * 0.64f)
                cubicTo(w * 0.29f, h * 0.64f, w * 0.31f, h * 0.69f, w * 0.31f, h * 0.69f)
                cubicTo(w * 0.34f, h * 0.74f, w * 0.39f, h * 0.72f, w * 0.42f, h * 0.71f)
                cubicTo(w * 0.42f, h * 0.68f, w * 0.43f, h * 0.66f, w * 0.45f, h * 0.64f)
                cubicTo(w * 0.36f, h * 0.63f, w * 0.27f, h * 0.60f, w * 0.27f, h * 0.44f)
                cubicTo(w * 0.27f, h * 0.39f, w * 0.29f, h * 0.35f, w * 0.32f, h * 0.32f)
                cubicTo(w * 0.31f, h * 0.30f, w * 0.30f, h * 0.25f, w * 0.33f, h * 0.18f)
                cubicTo(w * 0.33f, h * 0.18f, w * 0.37f, h * 0.17f, w * 0.45f, h * 0.23f)
                cubicTo(w * 0.48f, h * 0.22f, w * 0.52f, h * 0.22f, w * 0.55f, h * 0.23f)
                cubicTo(w * 0.63f, h * 0.17f, w * 0.67f, h * 0.18f, w * 0.67f, h * 0.18f)
                cubicTo(w * 0.70f, h * 0.25f, w * 0.69f, h * 0.30f, w * 0.68f, h * 0.32f)
                cubicTo(w * 0.71f, h * 0.35f, w * 0.73f, h * 0.39f, w * 0.73f, h * 0.44f)
                cubicTo(w * 0.73f, h * 0.60f, w * 0.64f, h * 0.63f, w * 0.55f, h * 0.64f)
                cubicTo(w * 0.57f, h * 0.66f, w * 0.58f, h * 0.70f, w * 0.58f, h * 0.75f)
                lineTo(w * 0.58f, h * 0.86f)
                cubicTo(w * 0.58f, h * 0.87f, w * 0.59f, h * 0.88f, w * 0.61f, h * 0.88f)
                cubicTo(w * 0.76f, h * 0.82f, w * 0.88f, h * 0.68f, w * 0.88f, h * 0.5f)
                cubicTo(w * 0.88f, h * 0.28f, w * 0.72f, h * 0.12f, w * 0.5f, h * 0.12f)
                close()
            }
            drawPath(path = headPath, color = Color.White)
        }
    }
}

@Composable
private fun GoogleAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val h = this.size.height
            val strokeW = w * 0.22f

            // Red arc (top)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = 180f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = strokeW),
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(w - strokeW, h - strokeW)
            )

            // Yellow arc (top-right to left)
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 120f,
                sweepAngle = 60f,
                useCenter = false,
                style = Stroke(width = strokeW),
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(w - strokeW, h - strokeW)
            )

            // Green arc (bottom)
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 0f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = strokeW),
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(w - strokeW, h - strokeW)
            )

            // Blue arc (right)
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = 270f,
                sweepAngle = 90f,
                useCenter = false,
                style = Stroke(width = strokeW),
                topLeft = Offset(strokeW / 2, strokeW / 2),
                size = Size(w - strokeW, h - strokeW)
            )

            // Blue horizontal bar
            drawRect(
                color = Color(0xFF4285F4),
                topLeft = Offset(w * 0.45f, (h - strokeW) * 0.5f),
                size = Size(w * 0.55f, strokeW)
            )
        }
    }
}

@Composable
private fun MicrosoftAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF16181E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.52f)) {
            val s = this.size.width
            val rectSize = s * 0.44f
            val gap = s * 0.12f

            // Red (Top Left)
            drawRect(color = Color(0xFFF25022), topLeft = Offset(0f, 0f), size = Size(rectSize, rectSize))
            // Green (Top Right)
            drawRect(color = Color(0xFF7FBA00), topLeft = Offset(rectSize + gap, 0f), size = Size(rectSize, rectSize))
            // Blue (Bottom Left)
            drawRect(color = Color(0xFF00A4EF), topLeft = Offset(0f, rectSize + gap), size = Size(rectSize, rectSize))
            // Yellow (Bottom Right)
            drawRect(color = Color(0xFFFFB900), topLeft = Offset(rectSize + gap, rectSize + gap), size = Size(rectSize, rectSize))
        }
    }
}

@Composable
private fun DiscordAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF5865F2)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🎮",
            fontSize = (size.value * 0.42f).sp
        )
    }
}

@Composable
private fun BinanceAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF1E2026)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.52f)) {
            val cx = this.size.width / 2
            val cy = this.size.height / 2
            val radius = this.size.width * 0.42f

            val diamondPath = Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius, cy)
                lineTo(cx, cy + radius)
                lineTo(cx - radius, cy)
                close()
            }
            drawPath(diamondPath, Color(0xFFF3BA2F), style = Stroke(width = radius * 0.35f))
            drawCircle(Color(0xFFF3BA2F), radius = radius * 0.22f, center = Offset(cx, cy))
        }
    }
}

@Composable
private fun SolanaAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF14151C)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.52f)) {
            val w = this.size.width
            val h = this.size.height
            val barH = h * 0.20f
            val grad = Brush.horizontalGradient(listOf(Color(0xFF14F195), Color(0xFF9945FF)))

            drawRoundRect(
                brush = grad,
                topLeft = Offset(0f, 0f),
                size = Size(w, barH),
                cornerRadius = CornerRadius(barH * 0.5f, barH * 0.5f)
            )
            drawRoundRect(
                brush = grad,
                topLeft = Offset(w * 0.12f, h * 0.40f),
                size = Size(w * 0.88f, barH),
                cornerRadius = CornerRadius(barH * 0.5f, barH * 0.5f)
            )
            drawRoundRect(
                brush = grad,
                topLeft = Offset(0f, h * 0.80f),
                size = Size(w, barH),
                cornerRadius = CornerRadius(barH * 0.5f, barH * 0.5f)
            )
        }
    }
}

@Composable
private fun FacebookAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF1877F2)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "f",
            color = Color.White,
            fontSize = (size.value * 0.65f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun InstagramAvatar(size: Dp) {
    val igGradient = Brush.linearGradient(
        listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045))
    )
    Box(
        modifier = Modifier
            .size(size)
            .background(igGradient),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.55f)) {
            val w = this.size.width
            val strokeW = w * 0.12f
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(0f, 0f),
                size = Size(w, w),
                cornerRadius = CornerRadius(w * 0.28f, w * 0.28f),
                style = Stroke(width = strokeW)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.22f,
                center = Offset(w / 2, w / 2),
                style = Stroke(width = strokeW)
            )
            drawCircle(
                color = Color.White,
                radius = strokeW * 0.65f,
                center = Offset(w * 0.75f, w * 0.25f),
                style = Fill
            )
        }
    }
}

@Composable
private fun AmazonAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF232F3E)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "a",
            color = Color(0xFFFF9900),
            fontSize = (size.value * 0.6f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun FallbackMonogram(issuer: String, accountName: String, size: Dp) {
    val effectiveName = issuer.ifBlank { accountName }.trim()
    val monogram = extractMonogram(effectiveName)

    Box(
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

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

