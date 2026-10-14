package org.warifu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.security.OtpPreview
import org.warifu.ui.components.ServiceAvatar
import org.warifu.ui.components.TokenCard
import org.warifu.ui.components.WarifuLogo
import org.warifu.ui.model.PendingAccount
import org.warifu.ui.theme.StatusWarning
import org.warifu.ui.viewmodel.AuthUiState
import org.warifu.ui.viewmodel.TokenSortOrder
import uniffi.warifu_core.TokenView

/**
 * World-class 2FA account management screen.
 * Features fast search, pinning, tag filters, sorting, live preview validation,
 * duplicate detection, and Google migration account previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenListScreen(
    state: AuthUiState,
    pendingAccount: PendingAccount?,
    onPendingAccountChange: (PendingAccount?) -> Unit,
    onSavePendingAccount: (issuer: String, account: String, secret: String, period: Int, digits: Int, tag: String, isSteam: Boolean) -> Unit,
    onImportGoogleConfirmed: (String) -> Unit,
    onEditToken: (id: String, oldIssuer: String, oldAccount: String, newIssuer: String, newAccount: String, newTag: String) -> Unit,
    onTogglePin: (String) -> Unit,
    onQuickReveal: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onSortOrderChanged: (TokenSortOrder) -> Unit,
    onSearchChanged: (String) -> Unit,
    onToggleConceal: () -> Unit,
    onCopyCode: (String) -> Unit,
    onDeleteToken: (String) -> Unit,
    onScanQrClicked: () -> Unit,
    onLockClicked: () -> Unit,
    onSelectToken: (TokenView) -> Unit = {},
    onAddAccountClicked: () -> Unit = onScanQrClicked,
    onSettingsClicked: () -> Unit = onLockClicked,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
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
        var editTag by remember(token) { mutableStateOf(token.tags.firstOrNull().orEmpty()) }

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

                Spacer(modifier = Modifier.height(14.dp))

                // Tag Selector
                Text(
                    text = "Category / Folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Personal", "Work", "Finance", "Social", "Dev").forEach { tagOption ->
                        FilterChip(
                            selected = editTag == tagOption,
                            onClick = { editTag = if (editTag == tagOption) "" else tagOption },
                            label = { Text(tagOption, fontSize = 12.sp) }
                        )
                    }
                }

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
                            onEditToken(
                                token.id,
                                token.issuer,
                                token.accountName,
                                editIssuer.trim(),
                                editAccount.trim(),
                                editTag.trim()
                            )
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

    // Pending Account Save Sheet
    pendingAccount?.let { pending ->
        if (pending.isMigration) {
            // Google Migration Preview & Confirmation Sheet
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

                    Spacer(modifier = Modifier.height(6.dp))

                    val duplicateCount = pending.migrationPreviews.count { it.isDuplicate }
                    Text(
                        text = if (duplicateCount > 0) {
                            "Found ${pending.migrationPreviews.size} accounts ($duplicateCount already exist in your vault):"
                        } else {
                            "Found ${pending.migrationPreviews.size} accounts ready to import:"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable list of accounts in migration payload
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pending.migrationPreviews) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ServiceAvatar(issuer = item.issuer, accountName = item.accountName, size = 32.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.issuer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        if (item.accountName.isNotBlank()) {
                                            Text(item.accountName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (item.isDuplicate) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StatusWarning.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Exists",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = StatusWarning,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

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
                            Text("Import All (${pending.migrationPreviews.size})")
                        }
                    }
                }
            }
        } else {
            // Standard Account Naming & Save Sheet with live preview & duplicate detection
            var issuerInput by remember(pending) { mutableStateOf(pending.initialIssuer) }
            var accountInput by remember(pending) { mutableStateOf(pending.initialAccount) }
            var secretInput by remember(pending) { mutableStateOf(pending.secret) }
            var selectedTagInput by remember(pending) { mutableStateOf(pending.tag) }
            var isSteamType by remember(pending) { mutableStateOf(pending.isSteam) }
            var selectedPeriod by remember(pending) { mutableStateOf(pending.period) }

            // Live Base32 validation error
            val secretError = remember(secretInput) {
                if (secretInput.isBlank()) null else OtpPreview.validateBase32Secret(secretInput)
            }

            // Live generated OTP code preview
            val livePreviewCode = remember(secretInput, isSteamType, selectedPeriod) {
                if (secretError == null && secretInput.isNotBlank()) {
                    OtpPreview.generateLivePreview(
                        secretBase32 = secretInput,
                        isSteam = isSteamType,
                        period = selectedPeriod,
                        digits = 6
                    )
                } else null
            }

            // Live Duplicate Check
            val duplicateFound = remember(issuerInput, accountInput, state.tokens) {
                if (issuerInput.isNotBlank()) {
                    state.tokens.firstOrNull {
                        it.issuer.trim().equals(issuerInput.trim(), ignoreCase = true) &&
                        it.accountName.trim().equals(accountInput.trim(), ignoreCase = true)
                    }
                } else null
            }

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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Duplicate Warning Banner
                    if (duplicateFound != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "An account for \"${duplicateFound.issuer}\" already exists in your vault.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = issuerInput,
                        onValueChange = { issuerInput = it },
                        label = { Text("Service Name") },
                        placeholder = { Text("e.g. GitHub, Google, Discord") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        label = { Text("Account Name or Email (Optional)") },
                        placeholder = { Text("e.g. user@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = { Text("Secret Key") },
                        placeholder = { Text("Base32 secret key") },
                        singleLine = true,
                        isError = secretError != null,
                        supportingText = {
                            if (secretError != null) {
                                Text(secretError, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Letters A-Z and digits 2-7. Spaces are automatically stripped.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Live Code Preview Pill
                    if (livePreviewCode != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Live Code Preview:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = livePreviewCode,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Token Type Selection Chips
                    Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isSteamType && selectedPeriod == 30,
                            onClick = { isSteamType = false; selectedPeriod = 30 },
                            label = { Text("TOTP (30s)", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = isSteamType,
                            onClick = { isSteamType = true },
                            label = { Text("Steam Guard", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = !isSteamType && selectedPeriod == 60,
                            onClick = { isSteamType = false; selectedPeriod = 60 },
                            label = { Text("TOTP (60s)", fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category Tags
                    Text("Category / Folder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Personal", "Work", "Finance", "Social", "Dev").forEach { tagOption ->
                            FilterChip(
                                selected = selectedTagInput == tagOption,
                                onClick = { selectedTagInput = if (selectedTagInput == tagOption) "" else tagOption },
                                label = { Text(tagOption, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val canSave = (issuerInput.isNotBlank() || accountInput.isNotBlank()) &&
                            secretInput.isNotBlank() &&
                            secretError == null

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
                                    secretInput.trim(),
                                    selectedPeriod,
                                    6,
                                    selectedTagInput.trim(),
                                    isSteamType
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
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
                        if (state.tokens.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${state.tokens.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search icon
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) onSearchChanged("")
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search accounts",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Sort icon & dropdown
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort accounts",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Service (A → Z)") },
                                    leadingIcon = { if (state.sortOrder == TokenSortOrder.ISSUER_ASC) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        onSortOrderChanged(TokenSortOrder.ISSUER_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Service (Z → A)") },
                                    leadingIcon = { if (state.sortOrder == TokenSortOrder.ISSUER_DESC) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        onSortOrderChanged(TokenSortOrder.ISSUER_DESC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Account Name (A → Z)") },
                                    leadingIcon = { if (state.sortOrder == TokenSortOrder.ACCOUNT_ASC) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        onSortOrderChanged(TokenSortOrder.ACCOUNT_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Expiring Soon First") },
                                    leadingIcon = { if (state.sortOrder == TokenSortOrder.EXPIRING_SOON) Icon(Icons.Default.Check, null) },
                                    onClick = {
                                        onSortOrderChanged(TokenSortOrder.EXPIRING_SOON)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Conceal codes icon
                        IconButton(onClick = onToggleConceal) {
                            Icon(
                                imageVector = if (state.areCodesConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle conceal codes",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Lock vault icon
                        IconButton(onClick = onLockClicked) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock vault",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // More options
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options menu",
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

                // Inline search bar
                AnimatedVisibility(visible = isSearchActive) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search accounts or tags...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Horizontal Category / Tag Filters row (Screen 2 ui.png)
                if (state.tokens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val predefinedCategories = listOf("All", "Personal", "Work", "Crypto")
                        predefinedCategories.forEach { category ->
                            val isSelected = if (category == "All") state.selectedTag == null || state.selectedTag == "All"
                            else state.selectedTag.equals(category, ignoreCase = true)

                            FilterPill(
                                label = category,
                                isSelected = isSelected,
                                onClick = {
                                    if (category == "All") onTagSelected(null)
                                    else onTagSelected(if (isSelected) null else category)
                                }
                            )
                        }

                        // Any dynamic custom tags from user's accounts
                        val extraTags = state.tokens.flatMap { it.tags }
                            .distinct()
                            .filterNot { tag -> predefinedCategories.any { it.equals(tag, ignoreCase = true) } }

                        extraTags.forEach { customTag ->
                            val isSelected = state.selectedTag.equals(customTag, ignoreCase = true)
                            FilterPill(
                                label = customTag,
                                isSelected = isSelected,
                                onClick = { onTagSelected(if (isSelected) null else customTag) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF141519),
                border = BorderStroke(1.dp, Color(0xFF22242C))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomBarItem(
                        icon = Icons.Default.Home,
                        label = "Tokens",
                        isSelected = true,
                        onClick = {}
                    )
                    BottomBarItem(
                        icon = Icons.Outlined.QrCodeScanner,
                        label = "Scan",
                        isSelected = false,
                        onClick = onScanQrClicked
                    )
                    BottomBarItem(
                        icon = Icons.Outlined.AddCircleOutline,
                        label = "Add",
                        isSelected = false,
                        onClick = onAddAccountClicked
                    )
                    BottomBarItem(
                        icon = Icons.Outlined.Settings,
                        label = "Settings",
                        isSelected = false,
                        onClick = onSettingsClicked
                    )
                }
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
                        text = if (state.searchQuery.isEmpty() && state.selectedTag == null) "No Accounts" else "No matching accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (state.searchQuery.isEmpty() && state.selectedTag == null) {
                            "Tap Add or Scan below to add your first 2FA code."
                        } else {
                            "Check for typos or clear your active filters."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8E929E),
                        textAlign = TextAlign.Center
                    )

                    if (state.searchQuery.isEmpty() && state.selectedTag == null) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onScanQrClicked,
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = org.warifu.ui.theme.SakuraPink,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR Code")
                        }
                    }
                }
            } else {
                // High-density, clean token list matching Screen 2 in ui.png
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = state.filteredTokens,
                        key = { it.id }
                    ) { token ->
                        TokenCard(
                            token = token,
                            isConcealed = state.areCodesConcealed,
                            isIndividuallyRevealed = state.revealedTokenIds.contains(token.id),
                            onClick = { onSelectToken(token) },
                            onCopyCode = onCopyCode,
                            onTogglePin = { onTogglePin(token.id) },
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
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isSelected) org.warifu.ui.theme.SakuraPink else Color(0xFF1C1D22),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF262832))
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF8E929E),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = org.warifu.ui.theme.SakuraPink
    val inactiveColor = Color(0xFF8E929E)
    val color = if (isSelected) activeColor else inactiveColor

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = color
            )
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
