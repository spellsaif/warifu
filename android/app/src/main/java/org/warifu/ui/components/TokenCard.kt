package org.warifu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.warifu.ui.theme.TimerAmber
import org.warifu.ui.theme.TimerRed
import uniffi.warifu_core.TokenView

/**
 * Modern, beautifully designed OTP token card inspired by 1Password, Aegis, and Google Authenticator.
 * Features instant tap-to-copy with responsive visual state ("Copied ✓"),
 * prominent typographic legibility, service branding avatars, and grace period countdowns.
 */
@Composable
fun TokenCard(
    token: TokenView,
    isConcealed: Boolean,
    onToggleConceal: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var justCopied by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(1800)
            justCopied = false
        }
    }

    val handleCopy = {
        justCopied = true
        onCopyCode(token.currentCode)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { handleCopy() }
            .animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Service Avatar + Issuer/Account + Countdown Ring
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Service Brand Avatar
                ServiceAvatar(
                    issuer = token.issuer,
                    accountName = token.accountName,
                    size = 44.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Issuer Title & Account Subtitle
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = token.issuer.ifBlank { "Account" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (token.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (token.accountName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = token.accountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Countdown Timer Ring
                CountdownRing(
                    remainingSeconds = token.remainingSeconds,
                    period = token.period,
                    size = 38.dp,
                    strokeWidth = 3.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hero OTP Code Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val formattedCode = if (isConcealed) {
                    "••••••"
                } else {
                    formatOtpCode(token.currentCode)
                }

                Text(
                    text = formattedCode,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.5.sp,
                    color = if (token.remainingSeconds <= 5u) TimerRed else MaterialTheme.colorScheme.primary
                )

                // Quick Action Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Conceal / Reveal toggle
                    IconButton(
                        onClick = onToggleConceal,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isConcealed) "Reveal Code" else "Conceal Code",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Interactive Copy Pill Button
                    Surface(
                        onClick = { handleCopy() },
                        shape = RoundedCornerShape(20.dp),
                        color = if (justCopied) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (justCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = "Copy code",
                                tint = if (justCopied) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (justCopied) "Copied" else "Copy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (justCopied) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Card Options Menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Code") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    handleCopy()
                                }
                            )
                            if (token.nextCode != null) {
                                DropdownMenuItem(
                                    text = { Text("Copy Next Code") },
                                    leadingIcon = { Icon(Icons.Default.Update, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        token.nextCode?.let { onCopyCode(it) }
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete Account", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Expiry Notice / Next Code Preview Banner
            AnimatedVisibility(
                visible = token.isNearExpiry && token.nextCode != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, TimerAmber.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(TimerAmber, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Next Code: ${formatOtpCode(token.nextCode ?: "")}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Expiring in ${token.remainingSeconds}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TimerRed
                        )
                    }
                }
            }
        }
    }
}

private fun formatOtpCode(code: String): String {
    return when (code.length) {
        6 -> "${code.substring(0, 3)} ${code.substring(3)}"
        8 -> "${code.substring(0, 4)} ${code.substring(4)}"
        else -> code
    }
}
