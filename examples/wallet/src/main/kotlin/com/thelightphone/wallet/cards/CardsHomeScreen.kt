package com.thelightphone.wallet.cards

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.wallet.ListOrEmpty
import com.thelightphone.wallet.WalletScaffold
import com.thelightphone.wallet.WalletStore

class CardsHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, CardsViewModel>(sealedActivity) {

    private val repository = WalletStore.cards(lightContext)

    override val viewModelClass: Class<CardsViewModel>
        get() = CardsViewModel::class.java

    override fun createViewModel() = CardsViewModel(repository)

    @Composable
    override fun Content() {
        val cards by viewModel.cards.collectAsState()

        WalletScaffold(
            title = "Cards",
            bottomBarItems = listOf(
                LightBarButton.Text(
                    text = "ADD CARD",
                    onClick = {
                        navigateTo(screenFactory = { AddCardScreen(it) })
                    },
                ),
            ),
        ) {
            ListOrEmpty(isEmpty = cards.isEmpty(), emptyText = "no cards yet…") {
                cards.forEach { card ->
                    CardRow(
                        card = card,
                        modifier = Modifier
                            .lightClickable {
                                navigateTo(screenFactory = {
                                    ShowCardScreen(it, card.id, card.name, card.barcodeFormat)
                                })
                            }
                            .padding(vertical = 0.75f.gridUnitsAsDp()),
                    )
                }
            }
        }
    }
}
