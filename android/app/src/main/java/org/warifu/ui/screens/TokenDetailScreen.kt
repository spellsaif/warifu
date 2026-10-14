package org.warifu.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.warifu.ui.components.CountdownRing
import org.warifu.ui.components.ServiceAvatar
import org.warifu.ui.theme.SakuraPink
import org.warifu.ui.theme.StatusCritical
import org.warifu.ui.theme.WarifuBackground
import org.warifu.ui.theme.WarifuBorder
import org.warifu.ui.theme.WarifuSurface
import org.warifu.ui.theme.WarifuTextSecondary
import uniffi.warifu_core.TokenView

/**
 * Screen 3 from ui.png: Focused Token Detail View.
 * Displays high-contrast enlarged OTP, full-size circular countdown ring, and core action tiles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenDetailScreen(
    token: TokenView,
    onBack: () -> Unit,
    onCopyCode: (String) -> Unit,
    onRefresh: () -> Unit = {},
    onEdit: () -> Unit = {},
    onEditToken: ((id: String, oldIssuer: String, oldAccount: String, newIssuer: String, newAccount: String, newTag: String) -> Unit)? = null,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }
    var justCopied by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }

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

    val displayName = token.issuer.ifBlank { token.accountName.ifBlank { "Account" } }
    val displayAccount = if (token.issuer.isNotBlank() && token.accountName.isNotBlank()) token.accountName else ""
    val formattedCode = formatOtpCode(token.currentCode)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WarifuBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (token.isPinned) "Unpin Account" else "Pin to Top") },
                            leadingIcon = {
                                Icon(
                                    if (token.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Details") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Account", color = StatusCritical) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = StatusCritical) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Service Avatar Hero
            ServiceAvatar(
                issuer = token.issuer,
                accountName = token.accountName,
                size = 76.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Service Title
            Text(
                text = displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (displayAccount.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayAccount,
                    fontSize = 14.sp,
                    color = WarifuTextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // One-time code label
            Text(
                text = "One-time code",
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = WarifuTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Large OTP digits
            Text(
                text = formattedCode,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                color = if (token.remainingSeconds <= 4u) StatusCritical else Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Big circular countdown indicator (Screen 3 center)
            CountdownRing(
                remainingSeconds = token.remainingSeconds,
                period = token.period,
                size = 136.dp,
                strokeWidth = 9.dp,
                showSecondsLabel = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom 4 Action Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailActionTile(
                    icon = if (justCopied) Icons.Default.Check else Icons.Outlined.ContentCopy,
                    label = if (justCopied) "Copied" else "Copy",
                    onClick = handleCopy,
                    modifier = Modifier.weight(1f),
                    tint = Color.White
                )

                DetailActionTile(
                    icon = Icons.Default.Refresh,
                    label = "Refresh",
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onRefresh()
                    },
                    modifier = Modifier.weight(1f)
                )

                DetailActionTile(
                    icon = Icons.Outlined.Edit,
                    label = "Edit",
                    onClick = {
                        if (onEditToken != null) {
                            showEditSheet = true
                        } else {
                            onEdit()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                DetailActionTile(
                    icon = Icons.Outlined.Delete,
                    label = "Delete",
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    tint = StatusCritical
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Text(
                    text = "Are you sure you want to remove \"$displayName\"? Make sure you have backup codes or an alternative 2FA method.",
                    color = WarifuTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showEditSheet) {
        var editIssuer by remember(token) { mutableStateOf(token.issuer) }
        var editAccount by remember(token) { mutableStateOf(token.accountName) }
        var editTag by remember(token) { mutableStateOf(token.tags.firstOrNull().orEmpty()) }

        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = WarifuSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Edit Account",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = editIssuer,
                    onValueChange = { editIssuer = it },
                    label = { Text("Service Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editAccount,
                    onValueChange = { editAccount = it },
                    label = { Text("Account Name / Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditSheet = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = {
                            onEditToken?.invoke(
                                token.id,
                                token.issuer,
                                token.accountName,
                                editIssuer.trim(),
                                editAccount.trim(),
                                editTag.trim()
                            )
                            showEditSheet = false
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuraPink,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(76.dp)
            .clip(shape)
            .background(WarifuSurface)
            .border(1.dp, WarifuBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = tint
            )
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
