package org.warifu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onLockClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = { Text("Search accounts…", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                },
                actions = {
                    IconButton(onClick = onToggleConceal) {
                        Icon(
                            imageVector = if (state.areCodesConcealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Conceal"
                        )
                    }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import from Google") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onImportGoogleClicked()
                            }
                        )
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
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Scan QR Code",
                    tint = MaterialTheme.colorScheme.onPrimary
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        WarifuLogo(size = 72.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (state.searchQuery.isEmpty()) "No 2FA Accounts Yet" else "No matching accounts found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the scanner button below to add your first account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
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
