package com.thelightphone.wallet

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WalletViewModel(
    private val repository: WalletAccountRepository,
    private val walletId: Long,
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
            // repository.listAccounts() decrypts this wallet's wrapped BIP-39 entropy under the
            // hood (WalletKeyCipher.decrypt()), which can throw on a corrupted blob or an Android
            // Keystore key invalidated by a biometric/lock-screen change. Guard it the same way
            // cards/ShowCardScreen.kt guards its decrypt call, so a bad blob shows an error modal
            // instead of crashing the app.
            runCatching { repository.listAccounts(walletId) }
                .onSuccess { _accounts.value = it }
                .onFailure {
                    showError("Couldn't load this wallet's accounts. The stored data may be corrupted.")
                }
        }
    }
}
