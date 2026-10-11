package org.warifu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.warifu.ui.components.TokenCard
import org.warifu.ui.components.WarifuLogo
import org.warifu.ui.viewmodel.AuthUiState

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
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualUriText by remember { mutableStateOf("") }

    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = {
                showManualInputDialog = false
                manualUriText = ""
            },
            title = {
                Text("Enter Key or URI", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Paste an otpauth:// URI, Google migration link (otpauth-migration://), or base32 secret key:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualUriText,
                        onValueChange = { manualUriText = it },
                        placeholder = { Text("otpauth://totp/... or secret", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualUriText.isNotBlank()) {
                            onManualAddUri(manualUriText.trim())
                            showManualInputDialog = false
                            manualUriText = ""
                        }
                    }
                ) {
                    Text("Add Account")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showManualInputDialog = false
                        manualUriText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        WarifuLogo(size = 30.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Warifu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "割符 • Offline 2FA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleConceal) {
                        Icon(
                            imageVector = if (state.areCodesConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Conceal Codes"
                        )
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
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
                            text = { Text("Enter Key or URI Manually") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showManualInputDialog = true
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Lock Vault") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onLockClicked()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanQrClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan 2FA QR Code",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sleek, beautifully padded modern search bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = {
                            Text(
                                "Search accounts or issuers…",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Account List / Empty State
            if (state.filteredTokens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 64.dp)
                    ) {
                        WarifuLogo(size = 80.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = if (state.searchQuery.isEmpty()) "No 2FA Accounts Yet" else "No matching accounts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.searchQuery.isEmpty()) {
                                "Tap the scanner icon below or use the menu to import from Google Authenticator."
                            } else {
                                "Check for typos or try searching by account name or issuer."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.filteredTokens,
                        key = { it.id }
                    ) { token ->
                        TokenCard(
                            token = token,
                            isConcealed = state.areCodesConcealed,
                            onToggleConceal = onToggleConceal,
                            onCopyCode = onCopyCode
                        )
                    }
                }
            }
        }
    }
}
