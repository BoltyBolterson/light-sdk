package com.thelightphone.wallet.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.wallet.CardRepository
import com.thelightphone.wallet.WalletAccountRepository
import com.thelightphone.wallet.WalletDatabase

class CardsHomeScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, CardsViewModel>(sealedActivity) {

    private val repository = CardRepository.getInstance {
        WalletDatabase.build(lightContext)
    }

    override val viewModelClass: Class<CardsViewModel>
        get() = CardsViewModel::class.java

    override fun createViewModel() = CardsViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val cards by viewModel.cards.collectAsState()
        val errorModal by viewModel.errorModal.collectAsState()

        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        center = LightTopBarCenter.Text("Cards"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    if (cards.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "no cards yet…",
                                variant = LightTextVariant.Copy,
                                align = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 1f.gridUnitsAsDp()),
                            )
                        }
                    } else {
                        LightScrollView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 1f.gridUnitsAsDp()),
                        ) {
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

                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "ADD CARD",
                                onClick = {
                                    navigateTo(screenFactory = { AddCardScreen(it) })
                                },
                            ),
                        ),
                    )
                }

                errorModal?.let { message ->
                    LightFullscreenModal(
                        message = message,
                        onClose = viewModel::dismissError,
                    )
                }
            }
        }
    }
}
