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
import org.warifu.ui.ScannerActivity
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

    private var showManualInputDialog by mutableStateOf(false)
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
                showManualInputDialog = true
            } else {
                val scannedText = data.getStringExtra(ScannerActivity.EXTRA_SCAN_RESULT)
                if (!scannedText.isNullOrBlank()) {
                    try {
                        handleIncomingTokenUri(scannedText)
                    } catch (t: Throwable) {
                        Log.e("MainActivity", "Failed to process scanned QR code", t)
                        Toast.makeText(
                            this,
                            "Failed to process QR code: ${t.localizedMessage ?: "Invalid format"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
                            onScanQrClicked = { launchScanner() },
                            onImportGoogleClicked = { launchScanner() },
                            onManualAddUri = { inputUri -> handleIncomingTokenUri(inputUri) },
                            onLockClicked = { viewModel.lockVault() }
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
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        try {
            if (trimmed.startsWith("otpauth-migration://", ignoreCase = true) ||
                trimmed.startsWith("otpauth-migration:", ignoreCase = true)) {
                viewModel.importGoogleMigration(trimmed)
            } else if (trimmed.startsWith("otpauth://", ignoreCase = true)) {
                viewModel.addTokenFromUri(trimmed)
            } else {
                // Check for common non-2FA QR formats (URLs, WiFi, vCards)
                if (trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.startsWith("WIFI:", ignoreCase = true) ||
                    trimmed.startsWith("BEGIN:", ignoreCase = true)) {
                    Toast.makeText(this, "Unrecognized QR code: please scan a 2FA OTP QR code", Toast.LENGTH_LONG).show()
                    return
                }

                val sanitized = trimmed.replace(" ", "").replace("-", "").uppercase()
                // Validate Base32 characters: RFC 4648 Base32 alphabet is A-Z and 2-7, plus optional padding '='
                if (sanitized.matches(Regex("^[A-Z2-7=]+$"))) {
                    val uri = "otpauth://totp/Warifu:Account?secret=$sanitized&issuer=Warifu"
                    viewModel.addTokenFromUri(uri)
                } else {
                    Toast.makeText(this, "Invalid QR code format: not a valid 2FA secret or URI", Toast.LENGTH_LONG).show()
                }
            }
        } catch (t: Throwable) {
            Log.e("MainActivity", "Failed to parse token URI", t)
            Toast.makeText(this, "Failed to import token: ${t.localizedMessage ?: "Invalid payload"}", Toast.LENGTH_LONG).show()
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
