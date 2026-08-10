package com.thelightphone.wallet.restore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.wallet.Chain
import com.thelightphone.wallet.WalletAccountRepository
import com.thelightphone.wallet.settings.WalletPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val SOLANA_ACCOUNT_PATH = "m/44'/501'/0'/0'"

data class RestoreScanUiState(
    val scanning: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
    val results: List<ScanResult> = emptyList(),
    val summary: ScanSummary? = null,
    val message: String? = null,
)

class RestoreScanViewModel(
    private val repository: WalletAccountRepository,
    private val walletId: Long,
    private val dataStore: DataStore<Preferences>,
) : LightViewModel<Unit>() {
    private val _state = MutableStateFlow(RestoreScanUiState())
    val state: StateFlow<RestoreScanUiState> = _state.asStateFlow()

    private var scanner: RestoreScanner? = null
    private var job: Job? = null

    init {
        start()
    }

    fun start() {
        if (job?.isActive == true) return
        _state.value = RestoreScanUiState(scanning = true)
        job = viewModelScope.launch(Dispatchers.IO) {
            val rpcUrl = WalletPreferences.solanaRpcUrl(dataStore)
            if (rpcUrl == null) {
                _state.value = RestoreScanUiState(
                    message = "No Solana RPC endpoint is set. Add one in Settings, then scan again.",
                )
                return@launch
            }

            val targets = runCatching { targets() }.getOrElse {
                _state.value = RestoreScanUiState(
                    message = "Couldn't read this wallet's addresses.",
                )
                return@launch
            }
            if (targets.isEmpty()) {
                _state.value = RestoreScanUiState(message = "This wallet has no Solana address to scan.")
                return@launch
            }

            val active = RestoreScanner(rpcUrl).also { scanner = it }
            try {
                val results = active.scan(targets) { update ->
                    _state.value = _state.value.copy(
                        done = update.done,
                        total = update.total,
                        results = update.results,
                    )
                }
                _state.value = _state.value.copy(
                    scanning = false,
                    results = results,
                    summary = summarize(results.map { it.status }, targets.size),
                )
            } finally {
                active.close()
                scanner = null
            }
        }
    }

    override fun onCleared() {
        scanner?.close()
        scanner = null
    }

    private fun targets(): List<ScanTarget> =
        repository.listAccounts(walletId, listOf(Chain.SOLANA))
            .map { ScanTarget(address = it.address, pathLabel = SOLANA_ACCOUNT_PATH) }
}
