package org.warifu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.components.WarifuLogo
import org.warifu.ui.theme.SakuraPink
import org.warifu.ui.theme.WarifuBackground
import org.warifu.ui.theme.WarifuSurfaceElevated
import org.warifu.ui.theme.WarifuTextSecondary

/**
 * Screen 1 from ui.png: Welcome & Onboarding Screen (or Lock Screen if vault exists).
 */
@Composable
fun LockScreen(
    hasExistingVault: Boolean,
    onUnlockClicked: () -> Unit,
    onCreateVaultClicked: () -> Unit,
    onImportClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = WarifuBackground
    ) {
        if (!hasExistingVault) {
            // Screen 1: Onboarding Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.7f))

                // Sakura Blossom Logo
                WarifuLogo(size = 92.dp)

                Spacer(modifier = Modifier.height(20.dp))

                // Title: Warifu
                Text(
                    text = "Warifu",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Open source authenticator\nfor a more private internet.",
                    fontSize = 14.sp,
                    color = WarifuTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                // Value proposition 1: Your keys, your device
                ValuePropRow(
                    icon = Icons.Outlined.Lock,
                    title = "Your keys, your device",
                    subtitle = "All data stays on your device."
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Value proposition 2: Clean & simple
                ValuePropRow(
                    icon = Icons.Outlined.Spa,
                    title = "Clean & simple",
                    subtitle = "No ads, no trackers, no clutter."
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Value proposition 3: Open source
                ValuePropRow(
                    icon = Icons.Outlined.DarkMode,
                    title = "Open source",
                    subtitle = "Built for everyone."
                )

                Spacer(modifier = Modifier.weight(1.2f))

                // Primary CTA Button: Get Started -> (Sakura pink pill)
                Button(
                    onClick = onCreateVaultClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SakuraPink,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Secondary CTA Link: Import from another app
                TextButton(
                    onClick = onImportClicked,
                    colors = ButtonDefaults.textButtonColors(contentColor = WarifuTextSecondary)
                ) {
                    Text(
                        text = "Import from another app",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            // Vault Locked Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                WarifuLogo(size = 80.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Warifu",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Vault is locked",
                    fontSize = 14.sp,
                    color = WarifuTextSecondary
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onUnlockClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SakuraPink,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ValuePropRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(WarifuSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = WarifuTextSecondary
            )
        }
    }
}

