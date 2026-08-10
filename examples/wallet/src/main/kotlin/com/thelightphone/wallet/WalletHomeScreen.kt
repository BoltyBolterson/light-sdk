package com.thelightphone.wallet

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
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
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

class WalletHomeScreen(
    sealedActivity: SealedLightActivity,
    private val walletId: Long,
) : LightScreen<Unit, WalletViewModel>(sealedActivity) {

    private val repository = WalletStore.accounts(lightContext)

    override val viewModelClass: Class<WalletViewModel>
        get() = WalletViewModel::class.java

    override fun createViewModel() = WalletViewModel(repository, walletId, lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val accounts by viewModel.accounts.collectAsState()
        val errorModal by viewModel.errorModal.collectAsState()

        LightTheme(colors = themeColors) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LightThemeTokens.colors.background),
                ) {
                    LightTopBar(
                        leftButton = LightBarButton.LightIcon(
                            icon = LightIcons.BACK,
                            onClick = { goBack(Unit) },
                        ),
                        center = LightTopBarCenter.Text("Wallet"),
                        modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                    )

                    if (accounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "setting up your wallet…",
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
                            accounts.forEach { account ->
                                WalletAccountRow(
                                    account = account,
                                    modifier = Modifier
                                        .lightClickable {
                                            navigateTo(screenFactory = {
                                                WalletReceiveScreen(it, account)
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
                                text = "SEND",
                                onClick = {
                                    val chains = accounts.map { it.chain }.ifEmpty { Chain.entries }
                                    navigateTo(screenFactory = { WalletScanScreen(it, chains) }) { destination ->
                                        navigateTo(screenFactory = {
                                            WalletSendScreen(it, destination, walletId)
                                        })
                                    }
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
