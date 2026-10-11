package org.openauth.ui.viewmodel

import android.content.Context
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
import org.openauth.security.SecurityHardening
import uniffi.openauth_core.TokenView
import uniffi.openauth_core.VaultSession

data class AuthUiState(
    val isUnlocked: Boolean = false,
    val tokens: List<TokenView> = emptyList(),
    val filteredTokens: List<TokenView> = emptyList(),
    val searchQuery: String = "",
    val areCodesConcealed: Boolean = true,
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
            _uiState.update { it.copy(statusMessage = "Unlock failed: ${e.message}") }
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
            _uiState.update { it.copy(statusMessage = "Vault creation failed: ${e.message}") }
            false
        }
    }

    fun lockVault() {
        tickerJob?.cancel()
        session?.lock()
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
        } catch (e: Exception) {
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

    fun addTokenFromUri(uri: String): Boolean {
        val currentSession = session ?: return false
        return try {
            currentSession.addTokenFromUri(uri)
            refreshTokens()
            _uiState.update { it.copy(statusMessage = "Account added successfully") }
            true
        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "Invalid QR code: ${e.message}") }
            false
        }
    }

    fun importGoogleMigration(migrationUri: String): Int {
        val currentSession = session ?: return 0
        return try {
            val count = currentSession.importGoogleMigration(migrationUri).toInt()
            refreshTokens()
            _uiState.update { it.copy(statusMessage = "Imported $count accounts from Google") }
            count
        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "Google Import failed: ${e.message}") }
            0
        }
    }

    fun exportGoogleMigration(): String? {
        val currentSession = session ?: return null
        return try {
            currentSession.exportGoogleMigration()
        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "Export failed: ${e.message}") }
            null
        }
    }

    fun deleteToken(id: String) {
        val currentSession = session ?: return
        try {
            currentSession.removeToken(id)
            refreshTokens()
        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "Failed to delete: ${e.message}") }
        }
    }

    fun copyToken(context: Context, token: String) {
        SecurityHardening.copyTokenToClipboard(context, token)
        _uiState.update { it.copy(statusMessage = "Code copied (clears in 30s)") }
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
