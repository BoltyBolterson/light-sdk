package com.thelightphone.wallet

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WalletsListViewModel(
    private val repository: WalletAccountRepository,
) : LightViewModel<Unit>() {
    private val _wallets = MutableStateFlow<List<WalletSummary>>(emptyList())
    val wallets: StateFlow<List<WalletSummary>> = _wallets.asStateFlow()

    /** True while a createWallet() call is in flight, so rapid ADD WALLET taps can't kick off
     * more than one wallet creation (each one is a fresh, distinct BIP-39 mnemonic). */
    private val _isCreatingWallet = MutableStateFlow(false)
    val isCreatingWallet: StateFlow<Boolean> = _isCreatingWallet.asStateFlow()

    init {
        loadWallets()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        loadWallets()
    }

    fun addWallet() {
        if (_isCreatingWallet.value) return
        _isCreatingWallet.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.createWallet()
                loadWallets()
            } finally {
                _isCreatingWallet.value = false
            }
        }
    }

    fun renameWallet(id: Long, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameWallet(id, newName)
            loadWallets()
        }
    }

    private fun loadWallets() {
        viewModelScope.launch(Dispatchers.IO) {
            _wallets.value = repository.listWallets()
        }
    }
}
