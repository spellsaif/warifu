package org.warifu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.SimpleIcons
import compose.icons.simpleicons.*
import org.warifu.ui.theme.WarifuBorder
import org.warifu.ui.theme.WarifuSurfaceElevated
import org.warifu.ui.theme.WarifuTextPrimary
import java.util.Locale

/**
 * High-precision, authentic brand avatar powered by the official open-source Simple Icons library.
 * Fully integrated with the refined monochromatic theme.
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

    val iconVector: ImageVector? = when {
        effectiveName.contains("github") -> SimpleIcons.Github
        effectiveName.contains("google") || effectiveName.contains("gmail") -> SimpleIcons.Google
        effectiveName.contains("microsoft") || effectiveName.contains("outlook") || effectiveName.contains("office") || effectiveName.contains("azure") || effectiveName.contains("live.com") -> SimpleIcons.Microsoft
        effectiveName.contains("discord") -> SimpleIcons.Discord
        effectiveName.contains("amazon") || effectiveName.contains("aws") -> SimpleIcons.Amazon
        effectiveName.contains("steam") -> SimpleIcons.Steam
        effectiveName.contains("facebook") || effectiveName.contains("meta") -> SimpleIcons.Facebook
        effectiveName.contains("instagram") -> SimpleIcons.Instagram
        effectiveName.contains("twitter") || effectiveName.contains(" x ") || effectiveName == "x" -> SimpleIcons.Twitter
        effectiveName.contains("reddit") -> SimpleIcons.Reddit
        effectiveName.contains("twitch") -> SimpleIcons.Twitch
        effectiveName.contains("dropbox") -> SimpleIcons.Dropbox
        effectiveName.contains("gitlab") -> SimpleIcons.Gitlab
        effectiveName.contains("proton") -> SimpleIcons.Protonmail
        effectiveName.contains("bitwarden") -> SimpleIcons.Bitwarden
        effectiveName.contains("binance") || effectiveName.contains("coinbase") -> SimpleIcons.Coinbase
        effectiveName.contains("solana") || effectiveName.contains("phantom") || effectiveName.contains("crypto") || effectiveName.contains("bitcoin") -> SimpleIcons.Bitcoin
        effectiveName.contains("ethereum") || effectiveName.contains("metamask") -> SimpleIcons.Ethereum
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(WarifuSurfaceElevated)
            .border(
                width = 1.dp,
                color = WarifuBorder,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (iconVector != null) {
            Icon(
                imageVector = iconVector,
                contentDescription = issuer.ifBlank { accountName },
                tint = WarifuTextPrimary,
                modifier = Modifier.size(size * 0.52f)
            )
        } else {
            val monogram = extractMonogram(issuer.ifBlank { accountName })
            Text(
                text = monogram,
                color = WarifuTextPrimary,
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif
            )
        }
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
