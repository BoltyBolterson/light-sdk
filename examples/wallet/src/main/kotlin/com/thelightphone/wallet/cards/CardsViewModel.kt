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

class CardsViewModel(
    private val repository: CardRepository,
) : LightViewModel<Unit>() {
    private val _cards = MutableStateFlow<List<CardSummary>>(emptyList())
    val cards: StateFlow<List<CardSummary>> = _cards.asStateFlow()

    init {
        loadCards()
    }

    override fun onScreenShow(screen: SimpleLightScreen<Unit>) {
        loadCards()
    }

    private fun loadCards() {
        viewModelScope.launch(Dispatchers.IO) {
            _cards.value = repository.listCards()
        }
    }
}
