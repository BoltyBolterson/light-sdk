package com.thelightphone.wallet

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.wallet.price.PythAuditedPriceResult
import com.thelightphone.wallet.price.PythPriceClient
import com.thelightphone.wallet.price.millisUntilStale
import com.thelightphone.wallet.settings.WalletPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant

class WalletViewModel(
    private val repository: WalletAccountRepository,
    private val walletId: Long,
    private val dataStore: DataStore<Preferences>,
    private val priceClient: PythPriceClient = PythPriceClient(),
) : LightViewModel<Unit>() {
    private val _accounts = MutableStateFlow<List<WalletAccount>>(emptyList())
    val accounts: StateFlow<List<WalletAccount>> = _accounts.asStateFlow()

    private val _solUsdPrice = MutableStateFlow<BigDecimal?>(null)
    val solUsdPrice: StateFlow<BigDecimal?> = _solUsdPrice.asStateFlow()

    private val _errorModal = MutableStateFlow<String?>(null)
    val errorModal: StateFlow<String?> = _errorModal.asStateFlow()

    private var priceJob: Job? = null

    init {
        loadAccounts()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        loadAccounts()
    }

    override fun onCleared() {
        priceClient.close()
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
                .onSuccess { loaded ->
                    val carried = _accounts.value.associate { it.address to it.balance }
                    _accounts.value = loaded.map {
                        it.copy(balance = carried[it.address] ?: it.chain.unreadBalance())
                    }
                    if (loaded.any { it.chain == Chain.SOLANA }) {
                        refreshSolanaBalances()
                        refreshSolPrice()
                    }
                }
                .onFailure { failure ->
                    showError(
                        (failure as? ScreenLockRequiredException)?.message
                            ?: "Couldn't load this wallet's accounts. The stored data may be corrupted.",
                    )
                }
        }
    }

    private suspend fun refreshSolanaBalances() {
        val addresses = _accounts.value.filter { it.chain == Chain.SOLANA }.map { it.address }
        val rpcUrl = WalletPreferences.solanaRpcUrl(dataStore)
        if (rpcUrl == null) {
            addresses.forEach { applyBalance(it, WalletBalance.Unavailable) }
            return
        }
        val reader = SolanaBalanceReader(rpcUrl)
        try {
            for (address in addresses) {
                val lamports = reader.lamports(address)
                applyBalance(address, lamports?.let { WalletBalance.Lamports(it) } ?: WalletBalance.Unavailable)
            }
        } finally {
            reader.close()
        }
    }

    private fun applyBalance(address: String, balance: WalletBalance) {
        _accounts.value = _accounts.value.map {
            if (it.address == address) it.copy(balance = balance) else it
        }
    }

    private fun refreshSolPrice() {
        priceJob?.cancel()
        priceJob = viewModelScope.launch(Dispatchers.IO) {
            val audited = try {
                priceClient.getAuditedPrice(Chain.SOLANA)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            val trusted = audited as? PythAuditedPriceResult.Trusted
            if (trusted == null) {
                _solUsdPrice.value = null
                return@launch
            }
            _solUsdPrice.value = trusted.price.usdPrice
            val remaining = trusted.price.millisUntilStale(PythPriceClient.DEFAULT_MAX_PRICE_AGE, Instant.now())
            if (remaining > 0) delay(remaining)
            _solUsdPrice.value = null
        }
    }
}
