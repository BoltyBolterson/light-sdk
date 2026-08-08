package com.thelightphone.wallet.cards

import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.wallet.CardRepository
import com.thelightphone.wallet.CardSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Same shape as WalletViewModel: loads a summary list fresh on every show so cards added
 * from AddCardScreen show up immediately on back-navigation. */
class CardsViewModel(
    private val repository: CardRepository,
) : LightViewModel<Unit>() {
    private val _cards = MutableStateFlow<List<CardSummary>>(emptyList())
    val cards: StateFlow<List<CardSummary>> = _cards.asStateFlow()

    private val _errorModal = MutableStateFlow<String?>(null)
    val errorModal: StateFlow<String?> = _errorModal.asStateFlow()

    init {
        loadCards()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        loadCards()
    }

    fun showError(message: String) {
        _errorModal.value = message
    }

    fun dismissError() {
        _errorModal.value = null
    }

    private fun loadCards() {
        viewModelScope.launch(Dispatchers.IO) {
            _cards.value = repository.listCards()
        }
    }
}
