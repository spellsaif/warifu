package org.warifu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.warifu.security.HardwareKeystoreManager
import org.warifu.security.SecurityHardening
import org.warifu.ui.screens.LockScreen
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

    private var isExternalScannerActive by mutableStateOf(false)
    private var isRequestingPermission by mutableStateOf(false)
    private var showManualInputDialog by mutableStateOf(false)
    private var showPermissionDeniedDialog by mutableStateOf(false)

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        isExternalScannerActive = false
        if (result.contents != null) {
            handleIncomingTokenUri(result.contents)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isRequestingPermission = false
        if (isGranted) {
            startScanInternal()
        } else {
            showPermissionDeniedDialog = true
        }
    }

    private fun launchQrScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanInternal()
        } else {
            isRequestingPermission = true
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScanInternal() {
        try {
            isExternalScannerActive = true
            val options = ScanOptions().apply {
                setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                setPrompt("Point camera at 2FA QR code or Google migration QR")
                setBeepEnabled(false)
                setOrientationLocked(false)
                setBarcodeImageEnabled(false)
            }
            barcodeLauncher.launch(options)
        } catch (e: Exception) {
            isExternalScannerActive = false
            Toast.makeText(
                this,
                "Scanner error: ${e.localizedMessage ?: e.javaClass.simpleName}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inviolable Security Rule: Block screenshots, screen recording, and OS task previews
        SecurityHardening.enableFlagSecure(this)

        setContent {
            WarifuTheme {
                val state by viewModel.uiState.collectAsState()

                LaunchedEffect(state.statusMessage) {
                    state.statusMessage?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                        viewModel.clearStatusMessage()
                    }
                }

                if (showPermissionDeniedDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDeniedDialog = false },
                        title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "Warifu needs camera permission to scan QR codes offline. " +
                                "You can grant permission in App Settings, or enter your 2FA key manually."
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showPermissionDeniedDialog = false
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", packageName, null)
                                        }
                                        startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showPermissionDeniedDialog = false
                                    showManualInputDialog = true
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
                            onCreateVaultClicked = { initializeFreshVault() }
                        )
                    }
                    else -> {
                        TokenListScreen(
                            state = state,
                            showManualInputDialog = showManualInputDialog,
                            onManualInputDialogChange = { showManualInputDialog = it },
                            onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                            onToggleConceal = { viewModel.toggleConcealCodes() },
                            onCopyCode = { code -> viewModel.copyToken(this@MainActivity, code) },
                            onDeleteToken = { id -> viewModel.deleteToken(id) },
                            onScanQrClicked = { launchQrScanner() },
                            onImportGoogleClicked = { launchQrScanner() },
                            onManualAddUri = { inputUri -> handleIncomingTokenUri(inputUri) },
                            onLockClicked = { viewModel.lockVault() }
                        )
                    }
                }
            }
        }
    }

    private fun handleIncomingTokenUri(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.startsWith("otpauth-migration://") || trimmed.startsWith("otpauth-migration:")) {
            viewModel.importGoogleMigration(trimmed)
        } else if (trimmed.startsWith("otpauth://")) {
            viewModel.addTokenFromUri(trimmed)
        } else {
            val sanitized = trimmed.replace(" ", "").uppercase()
            val uri = "otpauth://totp/Warifu:Account?secret=$sanitized&issuer=Warifu"
            viewModel.addTokenFromUri(uri)
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
        // Only sanitize and lock memory when user actually leaves the app,
        // not during camera scans, permission dialogs, or configuration changes
        if (!isRequestingPermission && !isExternalScannerActive && !showManualInputDialog && !isChangingConfigurations) {
            viewModel.lockVault()
        }
    }
}
