package org.warifu.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Clean, monochromatic service avatar.
 * Adheres to minimalist typography with a refined rounded tile aesthetic.
 */
@Composable
fun ServiceAvatar(
    issuer: String,
    accountName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val effectiveName = issuer.ifBlank { accountName }.trim()
    val monogram = extractMonogram(effectiveName)
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = shape
            ),
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
