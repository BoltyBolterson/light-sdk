package com.thelightphone.wallet

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightFullscreenModal
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
        val accounts by viewModel.accounts.collectAsState()
        val errorModal by viewModel.errorModal.collectAsState()
        val solUsdPrice by viewModel.solUsdPrice.collectAsState()

        WalletScaffold(
            title = "Wallet",
            onBack = { goBack(Unit) },
            bottomBarItems = listOf(
                LightBarButton.Text(
                    text = "SEND",
                    onClick = {
                        navigateTo(screenFactory = { WalletSendScreen(it, "", walletId) })
                    },
                ),
            ),
            overlay = {
                errorModal?.let { message ->
                    LightFullscreenModal(
                        message = message,
                        onClose = viewModel::dismissError,
                    )
                }
            },
        ) {
            ListOrEmpty(isEmpty = accounts.isEmpty(), emptyText = "setting up your wallet…") {
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
                        usdPrice = solUsdPrice,
                    )
                }
            }
        }
    }
}
