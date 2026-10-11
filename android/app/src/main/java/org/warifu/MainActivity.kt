package org.warifu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import org.warifu.security.HardwareKeystoreManager
import org.warifu.security.SecurityHardening
import org.warifu.ui.screens.LockScreen
import org.warifu.ui.screens.QrScannerScreen
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

    private var isScanningQr by mutableStateOf(false)

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            isScanningQr = true
        } else {
            Toast.makeText(this, "Camera permission required to scan QR codes", Toast.LENGTH_SHORT).show()
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

                when {
                    !state.isUnlocked -> {
                        LockScreen(
                            hasExistingVault = vaultFile.exists() && wrappedKeyFile.exists(),
                            onUnlockClicked = { promptBiometricUnlock() },
                            onCreateVaultClicked = { initializeFreshVault() }
                        )
                    }
                    isScanningQr -> {
                        QrScannerScreen(
                            onQrCodeDetected = { scannedText ->
                                isScanningQr = false
                                if (scannedText.startsWith("otpauth-migration://")) {
                                    viewModel.importGoogleMigration(scannedText)
                                } else {
                                    viewModel.addTokenFromUri(scannedText)
                                }
                            },
                            onCloseClicked = { isScanningQr = false }
                        )
                    }
                    else -> {
                        TokenListScreen(
                            state = state,
                            onSearchChanged = { viewModel.onSearchQueryChanged(it) },
                            onToggleConceal = { viewModel.toggleConcealCodes() },
                            onCopyCode = { code -> viewModel.copyToken(this@MainActivity, code) },
                            onDeleteToken = { id -> viewModel.deleteToken(id) },
                            onScanQrClicked = { checkCameraPermissionAndScan() },
                            onImportGoogleClicked = { checkCameraPermissionAndScan() },
                            onLockClicked = { viewModel.lockVault() }
                        )
                    }
                }
            }
        }
    }

    private fun checkCameraPermissionAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isScanningQr = true
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            Toast.makeText(this, "Vault creation error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun promptBiometricUnlock() {
        if (!wrappedKeyFile.exists()) {
            Toast.makeText(this, "No vault found", Toast.LENGTH_SHORT).show()
            return
        }

        val wrappedKeyPayload = wrappedKeyFile.readBytes()
        val cryptoObject = try {
            keystoreManager.createBiometricDecryptCryptoObject(wrappedKeyPayload)
        } catch (e: Exception) {
            Toast.makeText(this, "Hardware keystore error: ${e.message}", Toast.LENGTH_LONG).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Warifu")
            .setSubtitle("Confirm biometrics to unwrap hardware keys")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancel")
            .build()

        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val authenticatedCrypto = result.cryptoObject
                if (authenticatedCrypto != null) {
                    try {
                        // 1. Hardware unwrapping of Vault Key inside TEE/StrongBox
                        val unwrappedKey = keystoreManager.unwrapVaultKey(authenticatedCrypto, wrappedKeyPayload)

                        // 2. Pass unwrapped key to Rust Core
                        viewModel.unlockWithVaultKey(unwrappedKey, vaultFile.absolutePath)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Decryption failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, "Auth canceled: $errString", Toast.LENGTH_SHORT).show()
            }
        })

        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

    override fun onStop() {
        super.onStop()
        // Lock and sanitize memory whenever user navigates away or turns off screen
        viewModel.lockVault()
    }
}
