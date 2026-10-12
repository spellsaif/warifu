package org.warifu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.components.TokenCard
import org.warifu.ui.components.WarifuLogo
import org.warifu.ui.theme.AccentCyan
import org.warifu.ui.viewmodel.AuthUiState
import uniffi.warifu_core.TokenView

/**
 * Top-tier 2FA accounts management screen.
 * Incorporates modern search filtering, multi-option account creation,
 * structured manual entry sheets, and clean cryptographic vault status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenListScreen(
    state: AuthUiState,
    onSearchChanged: (String) -> Unit,
    onToggleConceal: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDeleteToken: (String) -> Unit,
    onScanQrClicked: () -> Unit,
    onImportGoogleClicked: () -> Unit,
    onManualAddUri: (String) -> Unit,
    onLockClicked: () -> Unit,
    showManualInputDialog: Boolean = false,
    onManualInputDialogChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var internalShowManualInputDialog by remember { mutableStateOf(false) }
    val isManualDialogVisible = showManualInputDialog || internalShowManualInputDialog

    var tokenToDelete by remember { mutableStateOf<TokenView?>(null) }
    var filterOnlyPinned by remember { mutableStateOf(false) }

    val displayedTokens = remember(state.filteredTokens, filterOnlyPinned) {
        if (filterOnlyPinned) {
            state.filteredTokens.filter { it.isPinned }
        } else {
            state.filteredTokens
        }
    }

    // Deletion Confirmation Dialog
    tokenToDelete?.let { token ->
        AlertDialog(
            onDismissRequest = { tokenToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete 2FA Account?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${token.issuer.ifEmpty { token.accountName }}\"? " +
                            "Make sure you have an alternative 2FA backup configured on that service, or you may be permanently locked out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteToken(token.id)
                        tokenToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Account", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tokenToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Comprehensive Structured Manual Entry Dialog
    if (isManualDialogVisible) {
        ManualEntryDialog(
            onDismiss = {
                internalShowManualInputDialog = false
                onManualInputDialogChange(false)
            },
            onConfirm = { generatedUri ->
                onManualAddUri(generatedUri)
                internalShowManualInputDialog = false
                onManualInputDialogChange(false)
            }
        )
    }

    // Add Account Bottom Sheet (Speed Dial / Action Chooser)
    if (showAddBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddBottomSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Add 2FA Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Choose how you want to add an authenticator account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                AddOptionItem(
                    icon = Icons.Default.QrCodeScanner,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Scan QR Code",
                    subtitle = "Point camera at 2FA barcode from any website or service",
                    onClick = {
                        showAddBottomSheet = false
                        onScanQrClicked()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AddOptionItem(
                    icon = Icons.Default.Download,
                    iconTint = AccentCyan,
                    title = "Import from Google Authenticator",
                    subtitle = "Scan Google Authenticator Transfer QR code in bulk",
                    onClick = {
                        showAddBottomSheet = false
                        onImportGoogleClicked()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AddOptionItem(
                    icon = Icons.Default.Edit,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Enter Setup Key Manually",
                    subtitle = "Type or paste secret key or full otpauth:// URI",
                    onClick = {
                        showAddBottomSheet = false
                        internalShowManualInputDialog = true
                        onManualInputDialogChange(true)
                    }
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        WarifuLogo(size = 32.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Warifu",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Count badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${state.tokens.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "割符 • Hardware-Secured Vault",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Conceal/Reveal Toggle
                    IconButton(onClick = onToggleConceal) {
                        Icon(
                            imageVector = if (state.areCodesConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (state.areCodesConcealed) "Reveal Codes" else "Conceal Codes",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Direct Quick Lock Button
                    IconButton(onClick = onLockClicked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Scan Google Migration QR") },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onImportGoogleClicked()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Enter Setup Key Manually") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    internalShowManualInputDialog = true
                                    onManualInputDialogChange(true)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Lock Vault Now") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onLockClicked()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Account",
                        modifier = Modifier.size(22.dp)
                    )
                },
                text = {
                    Text(
                        text = "Add Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sleek Search Bar & Filter Chips
            if (state.tokens.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = {
                                Text(
                                    text = "Search accounts or services…",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChanged("") },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Filter row (All / Pinned)
                val pinnedCount = state.tokens.count { it.isPinned }
                if (pinnedCount > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !filterOnlyPinned,
                            onClick = { filterOnlyPinned = false },
                            label = { Text("All (${state.tokens.size})") }
                        )
                        FilterChip(
                            selected = filterOnlyPinned,
                            onClick = { filterOnlyPinned = true },
                            label = { Text("Pinned ($pinnedCount)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area: Token Cards vs Empty State
            if (displayedTokens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.searchQuery.isNotEmpty()) {
                        // Search not found
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 60.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No matching accounts found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching for a different issuer or account name",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { onSearchChanged("") }) {
                                Text("Clear Search Filter")
                            }
                        }
                    } else {
                        // Fresh Vault Empty State (Welcoming & Actionable)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 60.dp)
                        ) {
                            WarifuLogo(size = 80.dp)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Your Vault is Empty",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Protect your online accounts with hardware-secured two-factor authentication.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Quick Action Buttons
                            Button(
                                onClick = onScanQrClicked,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Scan 2FA QR Code", fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = onImportGoogleClicked,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Import from Google Authenticator")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = {
                                    internalShowManualInputDialog = true
                                    onManualInputDialogChange(true)
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Secret Key Manually")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = displayedTokens,
                        key = { it.id }
                    ) { token ->
                        TokenCard(
                            token = token,
                            isConcealed = state.areCodesConcealed,
                            onToggleConceal = onToggleConceal,
                            onCopyCode = onCopyCode,
                            onDelete = { tokenToDelete = token }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOptionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Structured manual account addition dialog supporting both form-based setup
 * and direct otpauth:// URI pasting with live input validation.
 */
@Composable
private fun ManualEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Form, 1 = Raw URI / Secret

    // Form fields
    var issuerText by remember { mutableStateOf("") }
    var accountText by remember { mutableStateOf("") }
    var secretText by remember { mutableStateOf("") }

    // Raw URI field
    var rawInputText by remember { mutableStateOf("") }

    val isFormValid = remember(issuerText, accountText, secretText) {
        val cleanSecret = secretText.replace(" ", "").replace("-", "")
        cleanSecret.isNotBlank() && cleanSecret.matches(Regex("^[A-Za-z2-7=]+$"))
    }

    val isRawValid = remember(rawInputText) {
        val trimmed = rawInputText.trim()
        trimmed.startsWith("otpauth://", ignoreCase = true) ||
        trimmed.startsWith("otpauth-migration://", ignoreCase = true) ||
        (trimmed.isNotBlank() && trimmed.replace(" ", "").replace("-", "").matches(Regex("^[A-Za-z2-7=]+$")))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Account Manually",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Account Details", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Paste URI / Key", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Form fields
                    OutlinedTextField(
                        value = issuerText,
                        onValueChange = { issuerText = it },
                        label = { Text("Service / Issuer (e.g. GitHub)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = accountText,
                        onValueChange = { accountText = it },
                        label = { Text("Account Name / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = secretText,
                        onValueChange = { secretText = it },
                        label = { Text("Secret Key (Base32)") },
                        placeholder = { Text("JBSWY3DPEHPK3PXP") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        supportingText = {
                            Text("Spaces and hyphens are automatically ignored")
                        }
                    )
                } else {
                    // Raw input
                    Text(
                        text = "Paste an otpauth:// URI, Google Migration URI, or Base32 secret string:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = rawInputText,
                        onValueChange = { rawInputText = it },
                        placeholder = { Text("otpauth://totp/... or secret") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        val cleanSecret = secretText.replace(" ", "").replace("-", "").uppercase()
                        val finalIssuer = issuerText.trim().ifEmpty { "Account" }
                        val finalAccount = accountText.trim().ifEmpty { "User" }
                        val uri = "otpauth://totp/$finalIssuer:$finalAccount?secret=$cleanSecret&issuer=$finalIssuer"
                        onConfirm(uri)
                    } else {
                        onConfirm(rawInputText.trim())
                    }
                },
                enabled = if (selectedTab == 0) isFormValid else isRawValid
            ) {
                Text("Add Account", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
