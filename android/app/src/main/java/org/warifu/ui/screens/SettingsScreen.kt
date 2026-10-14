package org.warifu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.components.WarifuLogo
import org.warifu.ui.theme.StatusCritical
import org.warifu.ui.theme.StatusSuccess
import org.warifu.ui.theme.WarifuBackground
import org.warifu.ui.theme.WarifuBorder
import org.warifu.ui.theme.WarifuSurface
import org.warifu.ui.theme.WarifuSurfaceElevated
import org.warifu.ui.theme.WarifuTextMuted
import org.warifu.ui.theme.WarifuTextPrimary
import org.warifu.ui.theme.WarifuTextSecondary

/**
 * World-class, monochromatic Settings & Security Management Screen.
 * Provides granular control over vault security, biometrics, auto-lock policies,
 * zero-knowledge architecture info, and encrypted backup utilities.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLockVault: () -> Unit,
    onScanQrClicked: () -> Unit,
    onManualAddClicked: () -> Unit,
    onToggleConceal: () -> Unit,
    areCodesConcealed: Boolean,
    tokenCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = WarifuBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to vault",
                        tint = WarifuTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = WarifuTextPrimary
                )
            }

            // Scrollable Settings Sections
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // SECTION 1: VAULT SECURITY
                SettingsSection(title = "VAULT SECURITY") {
                    SettingsRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = "Hardware Keystore backed biometric gate",
                        badge = {
                            StatusBadge(label = "Active", color = StatusSuccess)
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Timer,
                        title = "Auto-Lock Policy",
                        subtitle = "Immediately upon app backgrounding",
                        badge = {
                            StatusBadge(label = "Enforced", color = Color(0xFFA1A1AA))
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Shield,
                        title = "Screen Protection",
                        subtitle = "FLAG_SECURE: blocks screenshots & OS previews",
                        badge = {
                            StatusBadge(label = "Enabled", color = StatusSuccess)
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Visibility,
                        title = "Mask Codes by Default",
                        subtitle = "Conceal 6-digit tokens until tapped",
                        trailing = {
                            Switch(
                                checked = areCodesConcealed,
                                onCheckedChange = { onToggleConceal() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF09090B),
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color(0xFFA1A1AA),
                                    uncheckedTrackColor = Color(0xFF27272A),
                                    uncheckedBorderColor = Color(0xFF3F3F46)
                                )
                            )
                        }
                    )
                }

                // SECTION 2: 2FA ACCOUNTS & MANAGEMENT
                SettingsSection(title = "ACCOUNTS & STORAGE") {
                    SettingsRow(
                        icon = Icons.Outlined.QrCodeScanner,
                        title = "Scan QR Code",
                        subtitle = "Scan TOTP / Google Authenticator QR",
                        onClick = onScanQrClicked
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Key,
                        title = "Add Account Manually",
                        subtitle = "Enter base32 secret key directly",
                        onClick = onManualAddClicked
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Dataset,
                        title = "Secured 2FA Tokens",
                        subtitle = "Zero-knowledge encrypted locally in vault.bin",
                        badge = {
                            StatusBadge(label = "$tokenCount tokens", color = WarifuTextPrimary)
                        }
                    )
                }

                // SECTION 3: INTERFACE & THEME
                SettingsSection(title = "INTERFACE & ASSETS") {
                    SettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "Color System",
                        subtitle = "True Monochromatic Obsidian (Zinc-950)",
                        badge = {
                            StatusBadge(label = "Active", color = WarifuTextPrimary)
                        }
                    )
                    SettingsDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Category,
                        title = "Service Logos",
                        subtitle = "Official Simple Icons Open-Source Library",
                        badge = {
                            StatusBadge(label = "v1.1.1", color = Color(0xFFA1A1AA))
                        }
                    )
                }

                // SECTION 4: ARCHITECTURE & ZERO-KNOWLEDGE
                SettingsSection(title = "ARCHITECTURE & PRIVACY") {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WarifuLogo(size = 32.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Warifu Authenticator",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarifuTextPrimary
                                )
                                Text(
                                    text = "v1.0.0 (Rust Core 2024)",
                                    fontSize = 12.sp,
                                    color = WarifuTextMuted
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Built on Rust Native UniFFI architecture with Argon2id + AES-256-GCM. 100% offline, zero network permissions, zero analytics, zero trackers.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = WarifuTextSecondary
                        )
                    }
                }

                // SECTION 5: IMMEDIATE LOCK ACTION
                Surface(
                    onClick = onLockVault,
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF141417),
                    border = BorderStroke(1.dp, Color(0xFF2E1C20)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = StatusCritical,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Lock Vault Now",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusCritical
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF52525B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = WarifuTextMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = WarifuSurface,
            border = BorderStroke(1.dp, WarifuBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(WarifuSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = WarifuTextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = WarifuTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = WarifuTextSecondary
            )
        }

        if (badge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            badge()
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF52525B),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Color(0xFF1E1E24),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 64.dp)
    )
}
