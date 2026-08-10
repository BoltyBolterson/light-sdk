package com.thelightphone.wallet.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.wallet.Chain
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_RPC_ENDPOINT_LENGTH = 2048

data class SettingsUiState(
    val chainEnabled: Map<Chain, Boolean> = Chain.entries.associateWith { true },
    val solanaRpc: String = "",
)

class SettingsViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _errorModal = MutableStateFlow<String?>(null)
    val errorModal: StateFlow<String?> = _errorModal.asStateFlow()

    fun dismissError() {
        _errorModal.value = null
    }

    init {
        loadStoredState()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadStoredState()
    }

    private fun loadStoredState() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = runCatching { dataStore.data.first() }.getOrNull() ?: return@launch
            _uiState.value = SettingsUiState(
                chainEnabled = Chain.entries.associateWith { chain ->
                    prefs[WalletPreferences.chainEnabledKey(chain)] ?: true
                },
                solanaRpc = prefs[WalletPreferences.RPC_SOLANA] ?: "",
            )
        }
    }

    fun setChainEnabled(chain: Chain, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(chainEnabled = state.chainEnabled + (chain to enabled))
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[WalletPreferences.chainEnabledKey(chain)] = enabled
            }
        }
    }

    fun setSolanaRpc(endpoint: String) {
        val trimmed = endpoint.trim()
        if (trimmed.length > MAX_RPC_ENDPOINT_LENGTH) {
            _errorModal.value = "That endpoint is too long."
            return
        }
        val uri = runCatching { URI(trimmed) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        if (trimmed.isNotEmpty() && (uri == null || (scheme != "http" && scheme != "https") || uri.host.isNullOrBlank())) {
            _errorModal.value = "Enter a valid http:// or https:// RPC URL."
            return
        }
        _uiState.update { it.copy(solanaRpc = trimmed) }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs -> prefs[WalletPreferences.RPC_SOLANA] = trimmed }
        }
    }
}
