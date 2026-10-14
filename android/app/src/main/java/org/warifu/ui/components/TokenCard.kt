package org.warifu.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.warifu.ui.theme.SakuraPink
import org.warifu.ui.theme.StatusCritical
import org.warifu.ui.theme.WarifuBorder
import org.warifu.ui.theme.WarifuSurface
import org.warifu.ui.theme.WarifuTextSecondary
import uniffi.warifu_core.TokenView

/**
 * Production-ready 2FA token card matching ui.png Screen 2.
 * Clean, tactile, high legibility, with quick copy and detail navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TokenCard(
    token: TokenView,
    isConcealed: Boolean,
    isIndividuallyRevealed: Boolean = false,
    onClick: () -> Unit,
    onCopyCode: (String) -> Unit,
    onTogglePin: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var justCopied by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1500)
            justCopied = false
        }
    }

    val handleCopy = {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        justCopied = true
        onCopyCode(token.currentCode)
    }

    val isActuallyRevealed = !isConcealed || isIndividuallyRevealed
    val displayName = token.issuer.ifBlank { token.accountName.ifBlank { "Account" } }
    val displayAccount = if (token.issuer.isNotBlank() && token.accountName.isNotBlank()) token.accountName else ""

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = handleCopy
            )
            .animateContentSize()
            .semantics {
                contentDescription = "$displayName account. Tap to view details, hold to copy."
            },
        shape = RoundedCornerShape(16.dp),
        color = WarifuSurface,
        border = BorderStroke(1.dp, if (token.isPinned) Color.White.copy(alpha = 0.35f) else WarifuBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Service Avatar (44dp rounded squircle)
            ServiceAvatar(
                issuer = token.issuer,
                accountName = token.accountName,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info: Issuer, Account email, and large OTP code
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (token.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (token.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1A1A1E),
                            border = BorderStroke(1.dp, Color(0xFF282830))
                        ) {
                            Text(
                                text = token.tags[0],
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = WarifuTextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (displayAccount.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayAccount,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarifuTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // OTP Code
                val formattedCode = if (!isActuallyRevealed) {
                    "••••••"
                } else {
                    formatOtpCode(token.currentCode)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedCode,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        color = if (token.remainingSeconds <= 4u) StatusCritical else Color.White
                    )

                    if (justCopied) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.height(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Copied",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Right side: Countdown ring + Chevron right
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountdownRing(
                    remainingSeconds = token.remainingSeconds,
                    period = token.period,
                    size = 34.dp,
                    strokeWidth = 2.5.dp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "View account details",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatOtpCode(code: String): String {
    return when (code.length) {
        5 -> code
        6 -> "${code.substring(0, 3)} ${code.substring(3)}"
        8 -> "${code.substring(0, 4)} ${code.substring(4)}"
        else -> code
    }
}


