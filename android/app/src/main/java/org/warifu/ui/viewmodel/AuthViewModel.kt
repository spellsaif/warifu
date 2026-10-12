package org.warifu.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.warifu.security.SecurityHardening
import uniffi.warifu_core.TokenView
import uniffi.warifu_core.VaultSession
import java.io.ByteArrayInputStream

data class AuthUiState(
    val isUnlocked: Boolean = false,
    val tokens: List<TokenView> = emptyList(),
    val filteredTokens: List<TokenView> = emptyList(),
    val searchQuery: String = "",
    val areCodesConcealed: Boolean = false,
    val statusMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var session: VaultSession? = null
    private var tickerJob: Job? = null

    fun unlockWithVaultKey(key: ByteArray, vaultPath: String): Boolean {
        return try {
            val openedSession = VaultSession.open(vaultPath, key)
            session = openedSession
            _uiState.update { it.copy(isUnlocked = true) }
            startTicker()
            refreshTokens()
            true
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Unlock failed: $errMsg") }
            false
        }
    }

    fun createNewVault(key: ByteArray, vaultPath: String): Boolean {
        return try {
            val newSession = VaultSession.createNew(vaultPath, key)
            session = newSession
            _uiState.update { it.copy(isUnlocked = true) }
            startTicker()
            refreshTokens()
            true
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Vault creation failed: $errMsg") }
            false
        }
    }

    fun lockVault() {
        tickerJob?.cancel()
        try {
            session?.lock()
        } catch (_: Exception) {}
        session = null
        _uiState.update {
            it.copy(
                isUnlocked = false,
                tokens = emptyList(),
                filteredTokens = emptyList()
            )
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                refreshTokens()
                delay(1000L)
            }
        }
    }

    fun refreshTokens() {
        val currentSession = session ?: return
        val now = System.currentTimeMillis() / 1000L

        try {
            val list = currentSession.listTokens(now.toULong())
            _uiState.update { state ->
                state.copy(
                    tokens = list,
                    filteredTokens = filterTokens(list, state.searchQuery)
                )
            }
        } catch (_: Exception) {
            // Vault may be locked
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTokens = filterTokens(state.tokens, query)
            )
        }
    }

    fun toggleConcealCodes() {
        _uiState.update { it.copy(areCodesConcealed = !it.areCodesConcealed) }
    }

    /**
     * Adds a named token with user-specified issuer, account, and secret.
     */
    fun addAccount(
        issuer: String,
        accountName: String,
        secret: String,
        period: Int = 30,
        digits: Int = 6
    ): Boolean {
        val cleanIssuer = issuer.trim().ifEmpty { "Account" }
        val cleanAccount = accountName.trim()
        val cleanSecret = secret.replace(" ", "").replace("-", "").uppercase()
        val encodedIssuer = Uri.encode(cleanIssuer)
        val encodedAccount = Uri.encode(cleanAccount)
        val uri = "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$cleanSecret&issuer=$encodedIssuer&period=$period&digits=$digits"
        return addTokenFromUri(uri)
    }

    fun addTokenFromUri(uri: String): Boolean {
        val currentSession = session ?: run {
            _uiState.update { it.copy(statusMessage = "Vault is locked") }
            return false
        }
        return try {
            currentSession.addTokenFromUri(uri)
            refreshTokens()
            _uiState.update { it.copy(statusMessage = "Account added") }
            true
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Failed to add: $errMsg") }
            false
        }
    }

    fun importGoogleMigration(migrationUri: String): Int {
        val currentSession = session ?: run {
            _uiState.update { it.copy(statusMessage = "Vault is locked") }
            return 0
        }
        return try {
            val count = currentSession.importGoogleMigration(migrationUri).toInt()
            refreshTokens()
            _uiState.update { it.copy(statusMessage = "Imported $count accounts") }
            count
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Import failed: $errMsg") }
            0
        }
    }

    fun exportGoogleMigration(): String? {
        val currentSession = session ?: return null
        return try {
            currentSession.exportGoogleMigration()
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Export failed: $errMsg") }
            null
        }
    }

    fun deleteToken(id: String) {
        val currentSession = session ?: run {
            _uiState.update { it.copy(statusMessage = "Vault is locked") }
            return
        }
        try {
            currentSession.removeToken(id)
            refreshTokens()
            _uiState.update { it.copy(statusMessage = "Account deleted") }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Failed to delete: $errMsg") }
        }
    }

    /**
     * Renames an existing token's issuer and account name by extracting its secret from
     * the vault export, removing the old token, and re-adding it with updated metadata.
     */
    fun editToken(id: String, oldIssuer: String, oldAccount: String, newIssuer: String, newAccount: String): Boolean {
        val currentSession = session ?: return false
        return try {
            val migrationUri = currentSession.exportGoogleMigration()
            val secret = extractSecretFromMigration(migrationUri, oldIssuer, oldAccount)
            if (secret != null) {
                currentSession.removeToken(id)
                val cleanIssuer = newIssuer.trim().ifEmpty { "Account" }
                val cleanAccount = newAccount.trim()
                val encodedIssuer = Uri.encode(cleanIssuer)
                val encodedAccount = Uri.encode(cleanAccount)
                val newUri = "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$secret&issuer=$encodedIssuer"
                currentSession.addTokenFromUri(newUri)
                refreshTokens()
                _uiState.update { it.copy(statusMessage = "Account updated") }
                true
            } else {
                _uiState.update { it.copy(statusMessage = "Could not locate account secret") }
                false
            }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Update failed: $errMsg") }
            false
        }
    }

    fun copyToken(context: Context, token: String) {
        SecurityHardening.copyTokenToClipboard(context, token)
        _uiState.update { it.copy(statusMessage = "Copied to clipboard") }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun filterTokens(tokens: List<TokenView>, query: String): List<TokenView> {
        if (query.isBlank()) return tokens
        val q = query.trim().lowercase()
        return tokens.filter {
            it.issuer.lowercase().contains(q) ||
            it.accountName.lowercase().contains(q) ||
            it.tags.any { tag -> tag.lowercase().contains(q) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        lockVault()
    }
}

/**
 * Lightweight Protobuf parser to extract a token's raw secret from Google Migration URI.
 */
private fun extractSecretFromMigration(migrationUri: String, targetIssuer: String, targetAccount: String): String? {
    try {
        val uri = Uri.parse(migrationUri)
        val rawData = uri.getQueryParameter("data") ?: return null
        val payloadBytes = Base64.decode(rawData.replace(' ', '+'), Base64.DEFAULT)

        val stream = ByteArrayInputStream(payloadBytes)
        // Scan for OtpParameters field (tag 1, wire type 2 = 0x0a)
        while (stream.available() > 0) {
            val tag = readVarint(stream)
            val fieldNumber = tag shr 3
            val wireType = tag and 0x07

            if (wireType == 2) {
                val length = readVarint(stream)
                val fieldData = ByteArray(length)
                stream.read(fieldData)

                if (fieldNumber == 1) { // otp_parameters
                    val parsed = parseOtpParameter(fieldData)
                    if (parsed != null) {
                        val (secret, name, issuer) = parsed
                        if ((issuer.equals(targetIssuer, ignoreCase = true) || name.contains(targetAccount, ignoreCase = true)) && secret.isNotEmpty()) {
                            return base32Encode(secret)
                        }
                    }
                }
            } else if (wireType == 0) {
                readVarint(stream)
            } else {
                break
            }
        }
    } catch (_: Exception) {}
    return null
}

private data class ParsedOtp(val secret: ByteArray, val name: String, val issuer: String)

private fun parseOtpParameter(bytes: ByteArray): ParsedOtp? {
    val stream = ByteArrayInputStream(bytes)
    var secret: ByteArray? = null
    var name = ""
    var issuer = ""

    while (stream.available() > 0) {
        val tag = readVarint(stream)
        val fieldNumber = tag shr 3
        val wireType = tag and 0x07

        if (wireType == 2) {
            val length = readVarint(stream)
            val data = ByteArray(length)
            stream.read(data)

            when (fieldNumber) {
                1 -> secret = data
                2 -> name = String(data)
                3 -> issuer = String(data)
            }
        } else if (wireType == 0) {
            readVarint(stream)
        } else {
            break
        }
    }

    return if (secret != null) ParsedOtp(secret, name, issuer) else null
}

private fun readVarint(stream: ByteArrayInputStream): Int {
    var result = 0
    var shift = 0
    while (true) {
        val b = stream.read()
        if (b == -1) break
        result = result or ((b and 0x7F) shl shift)
        if ((b and 0x80) == 0) break
        shift += 7
    }
    return result
}

private fun base32Encode(bytes: ByteArray): String {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    val out = StringBuilder()
    var buffer = 0
    var bitsLeft = 0
    for (b in bytes) {
        buffer = (buffer shl 8) or (b.toInt() and 0xFF)
        bitsLeft += 8
        while (bitsLeft >= 5) {
            bitsLeft -= 5
            out.append(alphabet[(buffer shr bitsLeft) and 0x1F])
        }
    }
    if (bitsLeft > 0) {
        buffer = buffer shl (5 - bitsLeft)
        out.append(alphabet[buffer and 0x1F])
    }
    return out.toString()
}
