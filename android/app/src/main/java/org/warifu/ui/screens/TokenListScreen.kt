package org.warifu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.warifu.ui.theme.StatusCritical
import org.warifu.ui.theme.StatusSuccess
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
    onAddCategory: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSortSubMenu by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
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

    // Add Custom Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryText = ""
            },
            containerColor = Color(0xFF141417),
            titleContentColor = Color(0xFFFAFAFA),
            textContentColor = Color(0xFFA1A1AA),
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "New Category",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Create a custom tab to organize your 2FA accounts.",
                        fontSize = 13.sp,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newCategoryText,
                        onValueChange = { newCategoryText = it },
                        placeholder = { Text("e.g. Gaming, Finance, Dev", color = Color(0xFF71717A), fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFAFAFA),
                            unfocusedBorderColor = Color(0xFF27272A),
                            focusedTextColor = Color(0xFFFAFAFA),
                            unfocusedTextColor = Color(0xFFFAFAFA),
                            cursorColor = Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryText.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddCategory(trimmed)
                            onTagSelected(trimmed)
                        }
                        showAddCategoryDialog = false
                        newCategoryText = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF09090B)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = newCategoryText.isNotBlank()
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCategoryDialog = false
                        newCategoryText = ""
                    }
                ) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
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
            containerColor = Color(0xFF141417),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color(0xFF3F3F46),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ServiceAvatar(
                        issuer = editIssuer,
                        accountName = editAccount,
                        size = 46.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Edit Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAFAFA)
                        )
                        Text(
                            text = "Update service name, account label, or category",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SheetInputField(
                    value = editIssuer,
                    onValueChange = { editIssuer = it },
                    label = "Service Name",
                    placeholder = "e.g. GitHub"
                )

                Spacer(modifier = Modifier.height(12.dp))

                SheetInputField(
                    value = editAccount,
                    onValueChange = { editAccount = it },
                    label = "Account Name / Email",
                    placeholder = "e.g. user@gmail.com"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Category / Folder",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA1A1AA)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val editCategories = (listOf("Personal", "Work", "Crypto") + state.customCategories).distinct()
                    editCategories.forEach { tagOption ->
                        SheetSelectPill(
                            label = tagOption,
                            isSelected = editTag.equals(tagOption, ignoreCase = true),
                            onClick = { editTag = if (editTag.equals(tagOption, ignoreCase = true)) "" else tagOption }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { tokenToEdit = null },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF18181C),
                            contentColor = Color(0xFFA1A1AA)
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF09090B),
                            disabledContainerColor = Color(0xFF242429),
                            disabledContentColor = Color(0xFF71717A)
                        ),
                        enabled = editIssuer.isNotBlank() || editAccount.isNotBlank()
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.SemiBold)
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
                containerColor = Color(0xFF141417),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFF3F3F46),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                    }
                },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                        color = Color(0xFFFAFAFA)
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
                        color = Color(0xFFA1A1AA)
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
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF18181C),
                                border = BorderStroke(1.dp, Color(0xFF27272A)),
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
                                        Text(item.issuer, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFFAFAFA))
                                        if (item.accountName.isNotBlank()) {
                                            Text(item.accountName, fontSize = 12.sp, color = Color(0xFFA1A1AA))
                                        }
                                    }
                                    if (item.isDuplicate) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = StatusWarning.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f))
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
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF18181C),
                                contentColor = Color(0xFFA1A1AA)
                            )
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                onImportGoogleConfirmed(pending.rawMigrationUri)
                                onPendingAccountChange(null)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF09090B)
                            )
                        ) {
                            Text("Import All (${pending.migrationPreviews.size})", fontWeight = FontWeight.SemiBold)
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
                containerColor = Color(0xFF141417),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFF3F3F46),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                    }
                },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                ) {
                    // Hero Header with Live Vector Service Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ServiceAvatar(
                            issuer = issuerInput,
                            accountName = accountInput,
                            size = 46.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (issuerInput.isBlank()) "Add 2FA Account" else issuerInput,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFAFAFA)
                            )
                            Text(
                                text = "Enter secret key or review details",
                                fontSize = 12.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Duplicate Warning Banner
                    if (duplicateFound != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusWarning.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "An account for \"${duplicateFound.issuer}\" already exists in your vault.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFAFAFA)
                                )
                            }
                        }
                    }

                    SheetInputField(
                        value = issuerInput,
                        onValueChange = { issuerInput = it },
                        label = "Service Name",
                        placeholder = "e.g. GitHub, Google, Discord"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SheetInputField(
                        value = accountInput,
                        onValueChange = { accountInput = it },
                        label = "Account Name / Email (Optional)",
                        placeholder = "e.g. user@gmail.com"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SheetInputField(
                        value = secretInput,
                        onValueChange = { secretInput = it },
                        label = "Secret Key",
                        placeholder = "Base32 secret key",
                        isError = secretError != null,
                        supportingText = {
                            if (secretError != null) {
                                Text(secretError, color = StatusCritical, fontSize = 11.sp)
                            } else {
                                Text("Letters A-Z and digits 2-7. Spaces are automatically stripped.", color = Color(0xFF71717A), fontSize = 11.sp)
                            }
                        }
                    )

                    // Live Code Preview Pill
                    if (livePreviewCode != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF18181C),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StatusSuccess)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Live Preview:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                                Text(
                                    text = livePreviewCode,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp,
                                    color = Color(0xFFFAFAFA)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Token Type Selection Pills
                    Text("Type", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SheetSelectPill(
                            label = "TOTP (30s)",
                            isSelected = !isSteamType && selectedPeriod == 30,
                            onClick = { isSteamType = false; selectedPeriod = 30 }
                        )
                        SheetSelectPill(
                            label = "Steam Guard",
                            isSelected = isSteamType,
                            onClick = { isSteamType = true }
                        )
                        SheetSelectPill(
                            label = "TOTP (60s)",
                            isSelected = !isSteamType && selectedPeriod == 60,
                            onClick = { isSteamType = false; selectedPeriod = 60 }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Category Tags Pills
                    Text("Category / Folder", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFA1A1AA))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val availableCategories = (listOf("Personal", "Work", "Crypto") + state.customCategories).distinct()
                        availableCategories.forEach { tagOption ->
                            SheetSelectPill(
                                label = tagOption,
                                isSelected = selectedTagInput.equals(tagOption, ignoreCase = true),
                                onClick = {
                                    selectedTagInput = if (selectedTagInput.equals(tagOption, ignoreCase = true)) "" else tagOption
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

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
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFF18181C),
                                contentColor = Color(0xFFA1A1AA)
                            )
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium)
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF09090B),
                                disabledContainerColor = Color(0xFF242429),
                                disabledContentColor = Color(0xFF71717A)
                            ),
                            enabled = canSave
                        ) {
                            Text("Save Account", fontWeight = FontWeight.SemiBold)
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
            containerColor = Color(0xFF141417),
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Color(0xFF3F3F46),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                    color = Color(0xFFFAFAFA)
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
        containerColor = Color(0xFF09090B),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Professional Monochromatic Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Brand: Logo + Warifu Title + Count Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WarifuLogo(size = 28.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Warifu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = Color(0xFFFAFAFA)
                        )
                        if (state.tokens.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF18181B),
                                border = BorderStroke(1.dp, Color(0xFF27272A))
                            ) {
                                Text(
                                    text = "${state.tokens.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFA1A1AA),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Right Actions: Search + Polished Three-Dots Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Search icon
                        IconButton(
                            onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) onSearchChanged("")
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search accounts",
                                tint = if (isSearchActive) Color.White else Color(0xFFA1A1AA),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Polished Three-Dots Menu Popover
                        Box {
                            IconButton(
                                onClick = {
                                    showMenu = true
                                    showSortSubMenu = false
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options menu",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            MaterialTheme(
                                colorScheme = MaterialTheme.colorScheme.copy(
                                    surface = Color(0xFF141417)
                                )
                            ) {
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = {
                                        showMenu = false
                                        showSortSubMenu = false
                                    },
                                    modifier = Modifier
                                        .width(220.dp)
                                        .background(Color(0xFF141417), RoundedCornerShape(14.dp))
                                        .border(BorderStroke(1.dp, Color(0xFF27272A)), RoundedCornerShape(14.dp))
                                ) {
                                    if (showSortSubMenu) {
                                        DropdownMenuItem(
                                            text = { Text("Back", fontSize = 13.sp, color = Color(0xFFA1A1AA)) },
                                            leadingIcon = {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                                            },
                                            onClick = { showSortSubMenu = false }
                                        )
                                        HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                                        DropdownMenuItem(
                                            text = { Text("Service (A → Z)", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                if (state.sortOrder == TokenSortOrder.ISSUER_ASC) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                onSortOrderChanged(TokenSortOrder.ISSUER_ASC)
                                                showMenu = false
                                                showSortSubMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Service (Z → A)", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                if (state.sortOrder == TokenSortOrder.ISSUER_DESC) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                onSortOrderChanged(TokenSortOrder.ISSUER_DESC)
                                                showMenu = false
                                                showSortSubMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Account Name (A → Z)", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                if (state.sortOrder == TokenSortOrder.ACCOUNT_ASC) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                onSortOrderChanged(TokenSortOrder.ACCOUNT_ASC)
                                                showMenu = false
                                                showSortSubMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Expiring Soon First", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                if (state.sortOrder == TokenSortOrder.EXPIRING_SOON) {
                                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                } else {
                                                    Spacer(modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            onClick = {
                                                onSortOrderChanged(TokenSortOrder.EXPIRING_SOON)
                                                showMenu = false
                                                showSortSubMenu = false
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = if (state.areCodesConcealed) "Reveal Codes" else "Conceal Codes",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFFAFAFA)
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (state.areCodesConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onToggleConceal()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Sort Accounts...", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color(0xFF71717A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                showSortSubMenu = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("New Category", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                showAddCategoryDialog = true
                                            }
                                        )
                                        HorizontalDivider(color = Color(0xFF27272A), thickness = 0.5.dp)
                                        DropdownMenuItem(
                                            text = { Text("Lock Vault", fontSize = 13.sp, color = Color(0xFFFAFAFA)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA1A1AA),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            onClick = {
                                                showMenu = false
                                                onLockClicked()
                                            }
                                        )
                                    }
                                }
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
                            placeholder = { Text("Search accounts or tags...", fontSize = 14.sp, color = Color(0xFF71717A)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFA1A1AA), modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFAFAFA),
                                unfocusedBorderColor = Color(0xFF27272A),
                                focusedTextColor = Color(0xFFFAFAFA),
                                unfocusedTextColor = Color(0xFFFAFAFA),
                                cursorColor = Color(0xFFFAFAFA),
                                focusedContainerColor = Color(0xFF121215),
                                unfocusedContainerColor = Color(0xFF121215)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Horizontal Category / Tag Filters row with custom categories & + button
                if (state.tokens.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "All" is always first
                        val isAllSelected = state.selectedTag == null || state.selectedTag == "All"
                        FilterPill(
                            label = "All",
                            isSelected = isAllSelected,
                            onClick = { onTagSelected(null) }
                        )

                        // Custom Categories & Token tags
                        val allCategories = (state.customCategories + state.tokens.flatMap { it.tags })
                            .distinct()
                            .filterNot { it.equals("All", ignoreCase = true) }

                        allCategories.forEach { category ->
                            val isSelected = state.selectedTag.equals(category, ignoreCase = true)
                            FilterPill(
                                label = category,
                                isSelected = isSelected,
                                onClick = {
                                    onTagSelected(if (isSelected) null else category)
                                }
                            )
                        }

                        // "+ New" Custom Category Chip Button
                        Surface(
                            onClick = { showAddCategoryDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF141417),
                            border = BorderStroke(1.dp, Color(0xFF27272A))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add custom category",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA1A1AA)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xF5141418),
                    border = BorderStroke(1.dp, Color(0xFF27272F)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingDockItem(
                            icon = Icons.Outlined.Shield,
                            label = "Tokens",
                            isSelected = true,
                            onClick = {}
                        )
                        FloatingDockItem(
                            icon = Icons.Outlined.QrCodeScanner,
                            label = "Scan",
                            isSelected = false,
                            onClick = onScanQrClicked
                        )
                        FloatingDockItem(
                            icon = Icons.Outlined.Add,
                            label = "Add",
                            isSelected = false,
                            onClick = onAddAccountClicked,
                            isHighlight = true
                        )
                        FloatingDockItem(
                            icon = Icons.Outlined.Settings,
                            label = "Settings",
                            isSelected = false,
                            onClick = onSettingsClicked
                        )
                    }
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
                                containerColor = Color.White,
                                contentColor = Color(0xFF09090B)
                            )
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan QR Code", fontWeight = FontWeight.SemiBold)
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
        color = if (isSelected) Color(0xFFFAFAFA) else Color(0xFF141417),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF09090B) else Color(0xFFA1A1AA),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun FloatingDockItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isHighlight: Boolean = false
) {
    if (isHighlight) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = Color(0xFF1E1E24),
            border = BorderStroke(1.dp, Color(0xFF32323D)),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        val activeColor = Color(0xFFFAFAFA)
        val inactiveColor = Color(0xFF71717A)
        val color = if (isSelected) activeColor else inactiveColor

        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) Color(0xFF222228) else Color.Transparent,
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(19.dp)
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp, color = Color(0xFF71717A)) },
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color(0xFF27272A),
            errorBorderColor = StatusCritical,
            focusedContainerColor = Color(0xFF18181C),
            unfocusedContainerColor = Color(0xFF18181C),
            focusedTextColor = Color(0xFFFAFAFA),
            unfocusedTextColor = Color(0xFFFAFAFA),
            focusedLabelColor = Color(0xFFA1A1AA),
            unfocusedLabelColor = Color(0xFF71717A),
            cursorColor = Color.White
        )
    )
}

@Composable
private fun SheetSelectPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFFFAFAFA) else Color(0xFF18181C),
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF09090B) else Color(0xFFA1A1AA),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
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
