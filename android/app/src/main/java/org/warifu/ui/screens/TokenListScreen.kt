package org.warifu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import org.warifu.ui.model.PendingAccount
import org.warifu.ui.viewmodel.AuthUiState
import uniffi.warifu_core.TokenView

/**
 * World-class 2FA account management screen.
 * Monochromatic elegance, native ergonomics, and full account naming control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenListScreen(
    state: AuthUiState,
    pendingAccount: PendingAccount?,
    onPendingAccountChange: (PendingAccount?) -> Unit,
    onSavePendingAccount: (issuer: String, account: String, secret: String) -> Unit,
    onImportGoogleConfirmed: (String) -> Unit,
    onEditToken: (id: String, oldIssuer: String, oldAccount: String, newIssuer: String, newAccount: String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onToggleConceal: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDeleteToken: (String) -> Unit,
    onScanQrClicked: () -> Unit,
    onLockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showAddMenuSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    var tokenToDelete by remember { mutableStateOf<TokenView?>(null) }
    var tokenToEdit by remember { mutableStateOf<TokenView?>(null) }

    // Deletion Dialog
    tokenToDelete?.let { token ->
        AlertDialog(
            onDismissRequest = { tokenToDelete = null },
            title = {
                Text(
                    text = "Delete Account?",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${token.issuer.ifEmpty { token.accountName }}\"? Make sure you have backup codes or an alternative 2FA method.",
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
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { tokenToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Existing Account Sheet
    tokenToEdit?.let { token ->
        var editIssuer by remember(token) { mutableStateOf(token.issuer) }
        var editAccount by remember(token) { mutableStateOf(token.accountName) }

        ModalBottomSheet(
            onDismissRequest = { tokenToEdit = null },
            containerColor = MaterialTheme.colorScheme.surface,
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = editIssuer,
                    onValueChange = { editIssuer = it },
                    label = { Text("Service Name") },
                    placeholder = { Text("e.g. GitHub") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = editAccount,
                    onValueChange = { editAccount = it },
                    label = { Text("Account Name / Email") },
                    placeholder = { Text("e.g. user@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { tokenToEdit = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onEditToken(token.id, token.issuer, token.accountName, editIssuer.trim(), editAccount.trim())
                            tokenToEdit = null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = editIssuer.isNotBlank() || editAccount.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    // Pending Account Save Sheet (Prompts user for Service Name & Account Name)
    pendingAccount?.let { pending ->
        if (pending.isMigration) {
            // Google Migration Confirmation Sheet
            ModalBottomSheet(
                onDismissRequest = { onPendingAccountChange(null) },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = "Import Google Authenticator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "A Google Authenticator transfer QR code was recognized. Import all accounts into your vault?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPendingAccountChange(null) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onImportGoogleConfirmed(pending.rawMigrationUri)
                                onPendingAccountChange(null)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Import Accounts")
                        }
                    }
                }
            }
        } else {
            // Standard Account Naming & Save Sheet
            var issuerInput by remember(pending) { mutableStateOf(pending.initialIssuer) }
            var accountInput by remember(pending) { mutableStateOf(pending.initialAccount) }
            var secretInput by remember(pending) { mutableStateOf(pending.secret) }

            ModalBottomSheet(
                onDismissRequest = { onPendingAccountChange(null) },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = "Add 2FA Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Confirm or set the name for this account:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = issuerInput,
                        onValueChange = { issuerInput = it },
                        label = { Text("Service Name") },
                        placeholder = { Text("e.g. GitHub, Google, Discord") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        label = { Text("Account Name or Email (Optional)") },
                        placeholder = { Text("e.g. user@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = { Text("Secret Key") },
                        placeholder = { Text("Base32 secret key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        readOnly = pending.secret.isNotBlank()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val canSave = (issuerInput.isNotBlank() || accountInput.isNotBlank()) && secretInput.isNotBlank()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPendingAccountChange(null) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                onSavePendingAccount(
                                    issuerInput.trim(),
                                    accountInput.trim(),
                                    secretInput.trim()
                                )
                                onPendingAccountChange(null)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            enabled = canSave
                        ) {
                            Text("Save Account")
                        }
                    }
                }
            }
        }
    }

    // Add Method Chooser Bottom Sheet
    if (showAddMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenuSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Add Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                AddActionRow(
                    icon = Icons.Default.QrCodeScanner,
                    title = "Scan QR Code",
                    subtitle = "Scan camera barcode from any website or service",
                    onClick = {
                        showAddMenuSheet = false
                        onScanQrClicked()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AddActionRow(
                    icon = Icons.Outlined.Edit,
                    title = "Enter Key Manually",
                    subtitle = "Type secret key provided by your account provider",
                    onClick = {
                        showAddMenuSheet = false
                        onPendingAccountChange(PendingAccount())
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                AddActionRow(
                    icon = Icons.Outlined.Download,
                    title = "Import from Google Authenticator",
                    subtitle = "Scan transfer QR code to import accounts in bulk",
                    onClick = {
                        showAddMenuSheet = false
                        onScanQrClicked()
                    }
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WarifuLogo(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Warifu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search icon
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) onSearchChanged("")
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Conceal codes icon
                        IconButton(onClick = onToggleConceal) {
                            Icon(
                                imageVector = if (state.areCodesConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Conceal",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Lock vault icon
                        IconButton(onClick = onLockClicked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // More options
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Scan QR Code") },
                                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onScanQrClicked()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Enter Setup Key") },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onPendingAccountChange(PendingAccount())
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Lock Vault Now") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showMenu = false
                                        onLockClicked()
                                    }
                                )
                            }
                        }
                    }
                }

                // Inline search bar when toggled
                AnimatedVisibility(visible = isSearchActive) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search accounts...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenuSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.filteredTokens.isEmpty()) {
                // Clean, quiet Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WarifuLogo(size = 56.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (state.searchQuery.isEmpty()) "No Accounts" else "No matching accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (state.searchQuery.isEmpty()) {
                            "Tap + to scan a QR code or enter a setup key."
                        } else {
                            "Check for typos or clear your search query."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (state.searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onScanQrClicked,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR Code")
                        }
                    }
                }
            } else {
                // High-density, clean token list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.filteredTokens,
                        key = { it.id }
                    ) { token ->
                        TokenCard(
                            token = token,
                            isConcealed = state.areCodesConcealed,
                            onCopyCode = onCopyCode,
                            onEdit = { tokenToEdit = token },
                            onDelete = { tokenToDelete = token }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
