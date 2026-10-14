package org.warifu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.warifu.security.HardwareKeystoreManager
import org.warifu.security.SecurityHardening
import androidx.activity.compose.BackHandler
import org.warifu.ui.ScannerActivity
import org.warifu.ui.model.PendingAccount
import org.warifu.ui.screens.AddAccountScreen
import org.warifu.ui.screens.LockScreen
import org.warifu.ui.screens.TokenDetailScreen
import org.warifu.ui.screens.TokenListScreen
import org.warifu.ui.theme.WarifuTheme
import org.warifu.ui.viewmodel.AuthViewModel
import uniffi.warifu_core.ffiGenerateVaultKey
import java.io.File

class MainActivity : FragmentActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private val keystoreManager = HardwareKeystoreManager()

    private val vaultFile: File by lazy {
        File(filesDir, "vault.bin")
    }

    private val wrappedKeyFile: File by lazy {
        File(filesDir, "wrapped_kek.bin")
    }

    private var pendingAccountSetup by mutableStateOf<PendingAccount?>(null)
    private var selectedTokenId by mutableStateOf<String?>(null)
    private var showAddAccountScreen by mutableStateOf(false)
    private var showCameraPermissionDeniedDialog by mutableStateOf(false)
    private var crashDumpText by mutableStateOf<String?>(null)

    companion object {
        private const val RC_CAMERA_PERMISSION = 101
        private const val RC_SCAN = 102
    }

    private fun launchScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, ScannerActivity::class.java)
            startActivityForResult(intent, RC_SCAN)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), RC_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(this, ScannerActivity::class.java)
                startActivityForResult(intent, RC_SCAN)
            } else {
                showCameraPermissionDeniedDialog = true
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SCAN && resultCode == RESULT_OK && data != null) {
            val manualInput = data.getBooleanExtra(ScannerActivity.EXTRA_MANUAL_INPUT, false)
            if (manualInput) {
                pendingAccountSetup = PendingAccount()
            } else {
                val scannedText = data.getStringExtra(ScannerActivity.EXTRA_SCAN_RESULT)
                if (!scannedText.isNullOrBlank()) {
                    handleIncomingTokenUri(scannedText)
                }
            }
        }
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            // When the application as a whole is backgrounded (home button, app switch, screen lock),
            // sanitize plaintext memory and lock the vault per security rulebook.
            viewModel.lockVault()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inviolable Security Rule: Block screenshots, screen recording, and OS task previews
        SecurityHardening.enableFlagSecure(this)

        // 2. Check for crash dump from previous execution
        try {
            val crashFile = File(filesDir, "last_crash.txt")
            if (crashFile.exists()) {
                val content = crashFile.readText()
                if (content.isNotBlank()) {
                    crashDumpText = content
                }
                crashFile.delete()
            }
        } catch (_: Throwable) {}

        // 3. Register application-wide lifecycle observer for strict memory sanitization on backgrounding
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        setContent {
            WarifuTheme {
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(state.statusMessage) {
                    state.statusMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearStatusMessage()
                    }
                }

                // Crash diagnostics dialog (if previous session crashed)
                crashDumpText?.let { dump ->
                    AlertDialog(
                        onDismissRequest = { crashDumpText = null },
                        title = { Text("Crash Diagnostics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                        text = {
                            Text(
                                text = dump.take(1500),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            Button(onClick = { crashDumpText = null }) {
                                Text("Dismiss")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                SecurityHardening.copyTokenToClipboard(this@MainActivity, dump, "Crash Log")
                                Toast.makeText(this@MainActivity, "Crash log copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Copy Log")
                            }
                        }
                    )
                }

                if (showCameraPermissionDeniedDialog) {
                    AlertDialog(
                        onDismissRequest = { showCameraPermissionDeniedDialog = false },
                        title = {
                            Text("Camera Permission Required", fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Text(
                                "Warifu scans 2FA QR codes completely offline on your device. " +
                                "Camera access is needed to scan automatically, or you can enter your key manually."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showCameraPermissionDeniedDialog = false
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }
                                        startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(this@MainActivity, "Cannot open Settings", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showCameraPermissionDeniedDialog = false
                                    pendingAccountSetup = PendingAccount()
                                }
                            ) {
                                Text("Enter Key Manually")
                            }
                        }
                    )
                }

                when {
                    !state.isUnlocked -> {
                        LockScreen(
                            hasExistingVault = vaultFile.exists() && wrappedKeyFile.exists(),
                            onUnlockClicked = { promptBiometricUnlock() },
                            onCreateVaultClicked = { initializeFreshVault() },
                            onImportClicked = { launchScanner() }
                        )
                    }
                    selectedTokenId != null -> {
                        val currentToken = state.tokens.firstOrNull { it.id == selectedTokenId }
                        if (currentToken != null) {
                            BackHandler { selectedTokenId = null }
                            TokenDetailScreen(
                                token = currentToken,
                                onBack = { selectedTokenId = null },
                                onCopyCode = { code -> viewModel.copyToken(this@MainActivity, code) },
                                onRefresh = { viewModel.refreshTokens() },
                                onEdit = {
                                    pendingAccountSetup = PendingAccount(
                                        initialIssuer = currentToken.issuer,
                                        initialAccount = currentToken.accountName,
                                        tag = if (currentToken.tags.isNotEmpty()) currentToken.tags[0] else ""
                                    )
                                },
                                onEditToken = { id, oldIssuer, oldAccount, newIssuer, newAccount, newTag ->
                                    viewModel.editToken(id, oldIssuer, oldAccount, newIssuer, newAccount, newTag)
                                },
                                onDelete = {
                                    viewModel.deleteToken(currentToken.id)
                                    selectedTokenId = null
                                },
                                onTogglePin = { viewModel.togglePin(currentToken.id) }
                            )
                        } else {
                            selectedTokenId = null
                        }
                    }
                    showAddAccountScreen -> {
                        BackHandler { showAddAccountScreen = false }
                        AddAccountScreen(
                            onBack = { showAddAccountScreen = false },
                            onScanQrClicked = {
                                showAddAccountScreen = false
                                launchScanner()
                            },
                            onEnterKeyManuallyClicked = { prefilledIssuer ->
                                showAddAccountScreen = false
                                pendingAccountSetup = PendingAccount(initialIssuer = prefilledIssuer ?: "")
                            }
                        )
                    }
                    else -> {
                        TokenListScreen(
                            state = state,
                            pendingAccount = pendingAccountSetup,
                            onPendingAccountChange = { pendingAccountSetup = it },
                            onSavePendingAccount = { issuer, account, secret, period, digits, tag, isSteam ->
                                viewModel.addAccount(issuer, account, secret, period, digits, tag, isSteam)
                            },
                            onImportGoogleConfirmed = { rawUri ->
                                viewModel.importGoogleMigration(rawUri)
                            },
                            onEditToken = { id, oldIssuer, oldAccount, newIssuer, newAccount, newTag ->
                                viewModel.editToken(id, oldIssuer, oldAccount, newIssuer, newAccount, newTag)
                            },
                            onTogglePin = { id -> viewModel.togglePin(id) },
                            onQuickReveal = { id -> viewModel.quickRevealToken(id) },
                            onTagSelected = { tag -> viewModel.onTagSelected(tag) },
                            onSortOrderChanged = { sort -> viewModel.onSortOrderChanged(sort) },
                            onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                            onToggleConceal = { viewModel.toggleConcealCodes() },
                            onCopyCode = { code -> viewModel.copyToken(this@MainActivity, code) },
                            onDeleteToken = { id -> viewModel.deleteToken(id) },
                            onScanQrClicked = { launchScanner() },
                            onLockClicked = {
                                selectedTokenId = null
                                showAddAccountScreen = false
                                viewModel.lockVault()
                            },
                            onSelectToken = { selectedTokenId = it.id },
                            onAddAccountClicked = { showAddAccountScreen = true },
                            onSettingsClicked = {
                                selectedTokenId = null
                                showAddAccountScreen = false
                                viewModel.lockVault()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appLifecycleObserver)
    }

    private fun handleIncomingTokenUri(rawText: String) {
        val parsed = PendingAccount.parseFromRaw(rawText)
        if (parsed != null) {
            pendingAccountSetup = parsed
        } else {
            Toast.makeText(this, "Unrecognized QR code or secret key format", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeFreshVault() {
        try {
            // 1. Generate 32-byte cryptographic Vault Key
            val rawVaultKey = ffiGenerateVaultKey()

            // 2. Encrypt & wrap Vault Key with StrongBox/TEE hardware KEK
            val wrappedKey = keystoreManager.wrapVaultKey(rawVaultKey)
            wrappedKeyFile.writeBytes(wrappedKey)

            // 3. Create initial empty vault on disk
            val success = viewModel.createNewVault(rawVaultKey, vaultFile.absolutePath)
            if (success) {
                Toast.makeText(this, "Vault initialized and secured with hardware keys", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            Toast.makeText(this, "Vault creation error: $errMsg", Toast.LENGTH_LONG).show()
        }
    }

    private fun promptBiometricUnlock() {
        if (!wrappedKeyFile.exists() || !vaultFile.exists()) {
            Toast.makeText(this, "No vault found", Toast.LENGTH_SHORT).show()
            return
        }

        val wrappedKeyPayload = try {
            wrappedKeyFile.readBytes()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to read vault key: ${e.localizedMessage ?: e.javaClass.simpleName}", Toast.LENGTH_SHORT).show()
            return
        }

        val biometricManager = BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(this)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Warifu")
                .setSubtitle("Confirm biometrics or device PIN to access your vault")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    try {
                        val unwrappedKey = keystoreManager.unwrapVaultKey(wrappedKeyPayload)
                        viewModel.unlockWithVaultKey(unwrappedKey, vaultFile.absolutePath)
                    } catch (e: Exception) {
                        val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                        Toast.makeText(this@MainActivity, "Decryption failed: $errMsg", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Auth canceled: $errString", Toast.LENGTH_SHORT).show()
                }
            })

            biometricPrompt.authenticate(promptInfo)
        } else {
            // If device has no biometrics or screen lock enrolled, unwrap directly
            try {
                val unwrappedKey = keystoreManager.unwrapVaultKey(wrappedKeyPayload)
                viewModel.unlockWithVaultKey(unwrappedKey, vaultFile.absolutePath)
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                Toast.makeText(this, "Unlock failed: $errMsg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // If the activity is finishing (user exited the app), lock vault immediately
        if (isFinishing) {
            viewModel.lockVault()
        }
    }
}
