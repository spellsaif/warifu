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

enum class TokenSortOrder {
    ISSUER_ASC,    // Service Name (A to Z)
    ISSUER_DESC,   // Service Name (Z to A)
    ACCOUNT_ASC,   // Account Name (A to Z)
    EXPIRING_SOON  // Expiring soon first
}

data class AuthUiState(
    val isUnlocked: Boolean = false,
    val tokens: List<TokenView> = emptyList(),
    val filteredTokens: List<TokenView> = emptyList(),
    val searchQuery: String = "",
    val selectedTag: String? = null,              // null = "All"
    val customCategories: List<String> = listOf("Personal", "Work", "Crypto"),
    val pinnedTokenIds: Set<String> = emptySet(),
    val tokenTags: Map<String, String> = emptyMap(), // tokenId -> Tag name
    val revealedTokenIds: Set<String> = emptySet(), // Individually revealed tokens
    val sortOrder: TokenSortOrder = TokenSortOrder.ISSUER_ASC,
    val areCodesConcealed: Boolean = false,
    val statusMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var session: VaultSession? = null
    private var tickerJob: Job? = null

    val availableTags = listOf("Work", "Personal", "Finance", "Social", "Dev")

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
                filteredTokens = emptyList(),
                revealedTokenIds = emptySet()
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
            val rawList = currentSession.listTokens(now.toULong())
            _uiState.update { state ->
                // Apply pinned flags
                val updatedTokens = rawList.map { t ->
                    val isPinned = state.pinnedTokenIds.contains(t.id) || t.isPinned
                    val tag = state.tokenTags[t.id]
                    if (tag != null && !t.tags.contains(tag)) {
                        TokenView(
                            id = t.id,
                            issuer = t.issuer,
                            accountName = t.accountName,
                            currentCode = t.currentCode,
                            remainingSeconds = t.remainingSeconds,
                            period = t.period,
                            isNearExpiry = t.isNearExpiry,
                            nextCode = t.nextCode,
                            iconId = t.iconId,
                            tags = listOf(tag),
                            isPinned = isPinned
                        )
                    } else if (isPinned != t.isPinned) {
                        TokenView(
                            id = t.id,
                            issuer = t.issuer,
                            accountName = t.accountName,
                            currentCode = t.currentCode,
                            remainingSeconds = t.remainingSeconds,
                            period = t.period,
                            isNearExpiry = t.isNearExpiry,
                            nextCode = t.nextCode,
                            iconId = t.iconId,
                            tags = t.tags,
                            isPinned = isPinned
                        )
                    } else {
                        t
                    }
                }

                state.copy(
                    tokens = updatedTokens,
                    filteredTokens = processTokens(
                        tokens = updatedTokens,
                        query = state.searchQuery,
                        tag = state.selectedTag,
                        sort = state.sortOrder,
                        pinnedIds = state.pinnedTokenIds
                    )
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
                filteredTokens = processTokens(
                    tokens = state.tokens,
                    query = query,
                    tag = state.selectedTag,
                    sort = state.sortOrder,
                    pinnedIds = state.pinnedTokenIds
                )
            )
        }
    }

    fun onTagSelected(tag: String?) {
        _uiState.update { state ->
            state.copy(
                selectedTag = tag,
                filteredTokens = processTokens(
                    tokens = state.tokens,
                    query = state.searchQuery,
                    tag = tag,
                    sort = state.sortOrder,
                    pinnedIds = state.pinnedTokenIds
                )
            )
        }
    }

    fun addCategory(category: String) {
        val clean = category.trim()
        if (clean.isBlank()) return
        _uiState.update { state ->
            if (state.customCategories.any { it.equals(clean, ignoreCase = true) }) {
                state
            } else {
                state.copy(customCategories = state.customCategories + clean)
            }
        }
    }

    fun deleteCategory(category: String) {
        _uiState.update { state ->
            val updated = state.customCategories.filterNot { it.equals(category, ignoreCase = true) }
            val newSelected = if (state.selectedTag.equals(category, ignoreCase = true)) null else state.selectedTag
            state.copy(
                customCategories = updated,
                selectedTag = newSelected,
                filteredTokens = processTokens(
                    tokens = state.tokens,
                    query = state.searchQuery,
                    tag = newSelected,
                    sort = state.sortOrder,
                    pinnedIds = state.pinnedTokenIds
                )
            )
        }
    }

    fun onSortOrderChanged(sortOrder: TokenSortOrder) {
        _uiState.update { state ->
            state.copy(
                sortOrder = sortOrder,
                filteredTokens = processTokens(
                    tokens = state.tokens,
                    query = state.searchQuery,
                    tag = state.selectedTag,
                    sort = sortOrder,
                    pinnedIds = state.pinnedTokenIds
                )
            )
        }
    }

    fun togglePin(id: String) {
        _uiState.update { state ->
            val updatedPinned = if (state.pinnedTokenIds.contains(id)) {
                state.pinnedTokenIds - id
            } else {
                state.pinnedTokenIds + id
            }
            state.copy(
                pinnedTokenIds = updatedPinned,
                filteredTokens = processTokens(
                    tokens = state.tokens,
                    query = state.searchQuery,
                    tag = state.selectedTag,
                    sort = state.sortOrder,
                    pinnedIds = updatedPinned
                )
            )
        }
        refreshTokens()
    }

    fun setTokenTag(id: String, tag: String) {
        _uiState.update { state ->
            val updatedMap = if (tag.isBlank()) {
                state.tokenTags - id
            } else {
                state.tokenTags + (id to tag.trim())
            }
            state.copy(
                tokenTags = updatedMap,
                filteredTokens = processTokens(
                    tokens = state.tokens,
                    query = state.searchQuery,
                    tag = state.selectedTag,
                    sort = state.sortOrder,
                    pinnedIds = state.pinnedTokenIds
                )
            )
        }
        refreshTokens()
    }

    fun toggleConcealCodes() {
        _uiState.update { it.copy(areCodesConcealed = !it.areCodesConcealed) }
    }

    /**
     * Temporarily reveals a concealed code for 5 seconds, then auto-conceals.
     */
    fun quickRevealToken(id: String) {
        _uiState.update { it.copy(revealedTokenIds = it.revealedTokenIds + id) }
        viewModelScope.launch {
            delay(5000L)
            _uiState.update { it.copy(revealedTokenIds = it.revealedTokenIds - id) }
        }
    }

    /**
     * Checks if a token with the same issuer and account name already exists.
     */
    fun findDuplicate(issuer: String, accountName: String): TokenView? {
        val cleanIssuer = issuer.trim().lowercase()
        val cleanAccount = accountName.trim().lowercase()
        return _uiState.value.tokens.firstOrNull {
            it.issuer.trim().lowercase() == cleanIssuer &&
            it.accountName.trim().lowercase() == cleanAccount
        }
    }

    /**
     * Adds an account with custom naming, tags, and Steam Guard support.
     */
    fun addAccount(
        issuer: String,
        accountName: String,
        secret: String,
        period: Int = 30,
        digits: Int = 6,
        tag: String = "",
        isSteam: Boolean = false
    ): Boolean {
        val cleanIssuer = issuer.trim().ifEmpty { "Account" }
        val cleanAccount = accountName.trim()
        val cleanSecret = secret.replace(" ", "").replace("-", "").uppercase()
        val encodedIssuer = Uri.encode(cleanIssuer)
        val encodedAccount = Uri.encode(cleanAccount)

        val schemeType = if (isSteam) "steam" else "totp"
        val uri = "otpauth://$schemeType/$encodedIssuer:$encodedAccount?secret=$cleanSecret&issuer=$encodedIssuer&period=$period&digits=$digits"

        val success = addTokenFromUri(uri)
        if (success && tag.isNotBlank()) {
            val added = _uiState.value.tokens.lastOrNull()
            if (added != null) {
                setTokenTag(added.id, tag)
            }
        }
        return success
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
            _uiState.update { it.copy(pinnedTokenIds = it.pinnedTokenIds - id, tokenTags = it.tokenTags - id) }
            refreshTokens()
            _uiState.update { it.copy(statusMessage = "Account deleted") }
        } catch (e: Exception) {
            val errMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            _uiState.update { it.copy(statusMessage = "Failed to delete: $errMsg") }
        }
    }

    /**
     * Renames an existing token's issuer, account name, and tag.
     */
    fun editToken(
        id: String,
        oldIssuer: String,
        oldAccount: String,
        newIssuer: String,
        newAccount: String,
        newTag: String = ""
    ): Boolean {
        val currentSession = session ?: return false
        return try {
            val migrationUri = currentSession.exportGoogleMigration()
            val secret = extractSecretFromMigration(migrationUri, oldIssuer, oldAccount)
            if (secret != null) {
                val wasPinned = _uiState.value.pinnedTokenIds.contains(id)
                currentSession.removeToken(id)

                val cleanIssuer = newIssuer.trim().ifEmpty { "Account" }
                val cleanAccount = newAccount.trim()
                val encodedIssuer = Uri.encode(cleanIssuer)
                val encodedAccount = Uri.encode(cleanAccount)
                val newUri = "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$secret&issuer=$encodedIssuer"
                currentSession.addTokenFromUri(newUri)

                refreshTokens()
                val newToken = _uiState.value.tokens.lastOrNull()
                if (newToken != null) {
                    if (wasPinned) togglePin(newToken.id)
                    if (newTag.isNotBlank()) setTokenTag(newToken.id, newTag)
                }

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

    private fun processTokens(
        tokens: List<TokenView>,
        query: String,
        tag: String?,
        sort: TokenSortOrder,
        pinnedIds: Set<String>
    ): List<TokenView> {
        // 1. Search Filter
        val searchFiltered = if (query.isBlank()) {
            tokens
        } else {
            val q = query.trim().lowercase()
            tokens.filter {
                it.issuer.lowercase().contains(q) ||
                it.accountName.lowercase().contains(q) ||
                it.tags.any { t -> t.lowercase().contains(q) }
            }
        }

        // 2. Tag / Pinned Filter
        val tagFiltered = when (tag) {
            null, "", "All" -> searchFiltered
            "Pinned" -> searchFiltered.filter { pinnedIds.contains(it.id) || it.isPinned }
            else -> searchFiltered.filter { it.tags.any { t -> t.equals(tag, ignoreCase = true) } }
        }

        // 3. Partition pinned to the top
        val (pinned, unpinned) = tagFiltered.partition { pinnedIds.contains(it.id) || it.isPinned }

        val comparator = when (sort) {
            TokenSortOrder.ISSUER_ASC -> compareBy<TokenView> { it.issuer.lowercase() }
            TokenSortOrder.ISSUER_DESC -> compareByDescending<TokenView> { it.issuer.lowercase() }
            TokenSortOrder.ACCOUNT_ASC -> compareBy<TokenView> { it.accountName.lowercase() }
            TokenSortOrder.EXPIRING_SOON -> compareBy<TokenView> { it.remainingSeconds }
        }

        return pinned.sortedWith(comparator) + unpinned.sortedWith(comparator)
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
