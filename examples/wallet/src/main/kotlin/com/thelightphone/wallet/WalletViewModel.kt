package com.thelightphone.wallet

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.wallet.settings.WalletPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: WalletAccountRepository,
    private val walletId: Long,
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val _accounts = MutableStateFlow<List<WalletAccount>>(emptyList())
    val accounts: StateFlow<List<WalletAccount>> = _accounts.asStateFlow()

    private val _errorModal = MutableStateFlow<String?>(null)
    val errorModal: StateFlow<String?> = _errorModal.asStateFlow()

    init {
        loadAccounts()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        loadAccounts()
    }

    fun showError(message: String) {
        _errorModal.value = message
    }

    fun dismissError() {
        _errorModal.value = null
    }

    private fun loadAccounts() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.listAccounts(walletId, WalletPreferences.enabledChains(dataStore)) }
                .onSuccess { _accounts.value = it }
                .onFailure { failure ->
                    showError(
                        (failure as? ScreenLockRequiredException)?.message
                            ?: "Couldn't load this wallet's accounts. The stored data may be corrupted.",
                    )
                }
        }
    }
}
