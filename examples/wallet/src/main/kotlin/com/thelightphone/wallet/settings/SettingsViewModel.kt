package com.thelightphone.wallet.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.wallet.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val chainEnabled: Map<Chain, Boolean> = Chain.entries.associateWith { true },
)

class SettingsViewModel(
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
}
