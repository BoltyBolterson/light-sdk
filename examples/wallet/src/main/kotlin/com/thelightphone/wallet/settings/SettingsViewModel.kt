package com.thelightphone.wallet.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.wallet.Chain
import com.thelightphone.wallet.keyboard.KeyboardLayout
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Custom RPC endpoints are capped at this length before being rejected. */
private const val MAX_RPC_ENDPOINT_LENGTH = 2048

data class SettingsUiState(
    val keyboardLayout: KeyboardLayout = KeyboardLayout.EN_QWERTY,
    val chainEnabled: Map<Chain, Boolean> = Chain.entries.associateWith { true },
    val rpcEndpoints: Map<Chain, String> = Chain.entries.associateWith { "" },
    val offlineMode: Boolean = false,
    val manualRefreshMode: Boolean = false,
)

/** [Chain]-to-[DataStore] key lookups so callers can index by chain rather than repeat a `when`. */
private fun chainEnabledKey(chain: Chain) = when (chain) {
    Chain.BITCOIN -> WalletPreferences.CHAIN_BITCOIN_ENABLED
    Chain.ETHEREUM -> WalletPreferences.CHAIN_ETHEREUM_ENABLED
    Chain.SOLANA -> WalletPreferences.CHAIN_SOLANA_ENABLED
}

private fun rpcKey(chain: Chain) = when (chain) {
    Chain.BITCOIN -> WalletPreferences.RPC_BITCOIN
    Chain.ETHEREUM -> WalletPreferences.RPC_ETHEREUM
    Chain.SOLANA -> WalletPreferences.RPC_SOLANA
}

class SettingsViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _errorModal = MutableStateFlow<String?>(null)
    val errorModal: StateFlow<String?> = _errorModal.asStateFlow()

    init {
        loadStoredState()
    }

    fun dismissError() {
        _errorModal.value = null
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        super.onScreenShow(screen)
        loadStoredState()
    }

    private fun loadStoredState() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = dataStore.data.first()

            val layout = prefs[WalletPreferences.KEYBOARD_LAYOUT]
                ?.let { stored -> runCatching { KeyboardLayout.valueOf(stored) }.getOrNull() }
                ?: KeyboardLayout.EN_QWERTY

            val chainEnabled = Chain.entries.associateWith { chain ->
                runCatching { prefs[chainEnabledKey(chain)] }.getOrNull() ?: true
            }

            val rpcEndpoints = Chain.entries.associateWith { chain ->
                runCatching { prefs[rpcKey(chain)] }.getOrNull() ?: ""
            }

            val offlineMode = runCatching { prefs[WalletPreferences.OFFLINE_MODE] }.getOrNull() ?: false
            val manualRefreshMode =
                runCatching { prefs[WalletPreferences.MANUAL_REFRESH_MODE] }.getOrNull() ?: false

            _uiState.value = SettingsUiState(
                keyboardLayout = layout,
                chainEnabled = chainEnabled,
                rpcEndpoints = rpcEndpoints,
                offlineMode = offlineMode,
                manualRefreshMode = manualRefreshMode,
            )
        }
    }

    fun setKeyboardLayout(layout: KeyboardLayout) {
        _uiState.update { it.copy(keyboardLayout = layout) }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[WalletPreferences.KEYBOARD_LAYOUT] = layout.name
            }
        }
    }

    fun setChainEnabled(chain: Chain, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(chainEnabled = state.chainEnabled + (chain to enabled))
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[chainEnabledKey(chain)] = enabled
            }
        }
    }

    /**
     * Validates and persists a BYO RPC endpoint. A blank value clears the custom endpoint
     * (reverting to the default, represented the same way as "unset" elsewhere in this class:
     * an empty string). A non-blank value must parse as an absolute `http`/`https` URL with a
     * non-empty host, and stay under [MAX_RPC_ENDPOINT_LENGTH] - anything else is rejected
     * without touching DataStore, and reported via [errorModal] so the future code path that
     * actually connects to this endpoint never sees attacker-controlled schemes (e.g. `file:`,
     * `javascript:`) or unparsable garbage.
     */
    fun setRpcEndpoint(chain: Chain, endpoint: String) {
        val trimmed = endpoint.trim()

        if (trimmed.isEmpty()) {
            persistRpcEndpoint(chain, "")
            return
        }

        if (trimmed.length > MAX_RPC_ENDPOINT_LENGTH) {
            _errorModal.value = "RPC endpoint is too long (max $MAX_RPC_ENDPOINT_LENGTH characters)."
            return
        }

        val uri = runCatching { URI(trimmed) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        val host = uri?.host

        if (uri == null || (scheme != "http" && scheme != "https") || host.isNullOrBlank()) {
            _errorModal.value = "Enter a valid http:// or https:// RPC URL."
            return
        }

        persistRpcEndpoint(chain, trimmed)
    }

    private fun persistRpcEndpoint(chain: Chain, endpoint: String) {
        _uiState.update { state ->
            state.copy(rpcEndpoints = state.rpcEndpoints + (chain to endpoint))
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[rpcKey(chain)] = endpoint
            }
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        _uiState.update { it.copy(offlineMode = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[WalletPreferences.OFFLINE_MODE] = enabled
            }
        }
    }

    fun setManualRefreshMode(enabled: Boolean) {
        _uiState.update { it.copy(manualRefreshMode = enabled) }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { prefs ->
                prefs[WalletPreferences.MANUAL_REFRESH_MODE] = enabled
            }
        }
    }
}
