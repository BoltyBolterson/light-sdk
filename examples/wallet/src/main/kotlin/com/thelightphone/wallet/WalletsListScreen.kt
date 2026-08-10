package com.thelightphone.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.thelightphone.sdk.InitialScreen
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcon
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
import com.thelightphone.wallet.cards.CardsHomeScreen
import com.thelightphone.wallet.settings.SettingsScreen

@InitialScreen
class WalletsListScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, WalletsListViewModel>(sealedActivity) {

    private val repository = WalletStore.accounts(lightContext)

    override val viewModelClass: Class<WalletsListViewModel>
        get() = WalletsListViewModel::class.java

    override fun createViewModel() = WalletsListViewModel(repository)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val wallets by viewModel.wallets.collectAsState()
        val isCreatingWallet by viewModel.isCreatingWallet.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    center = LightTopBarCenter.Text("Wallets"),
                    rightButton = LightBarButton.LightIcon(
                        icon = LightIcons.SETTINGS,
                        onClick = { navigateTo(screenFactory = { SettingsScreen(it) }) },
                    ),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                if (wallets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = "no wallets yet",
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
                        wallets.forEach { wallet ->
                            WalletsListRow(
                                wallet = wallet,
                                onOpen = {
                                    navigateTo(screenFactory = {
                                        WalletHomeScreen(it, wallet.id)
                                    })
                                },
                                onRename = {
                                    navigateTo(
                                        screenFactory = {
                                            WalletTextEditorScreen(
                                                it,
                                                WalletEditorRequest(
                                                    title = "Wallet name",
                                                    initialValue = wallet.name,
                                                ),
                                            )
                                        },
                                        resultCallback = { newName ->
                                            if (newName.isNotBlank()) {
                                                viewModel.renameWallet(wallet.id, newName)
                                            }
                                        },
                                    )
                                },
                                modifier = Modifier.padding(vertical = 0.75f.gridUnitsAsDp()),
                            )
                        }
                    }
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = "ADD WALLET",
                            onClick = {
                                if (!isCreatingWallet) {
                                    viewModel.addWallet()
                                }
                            },
                        ),
                        LightBarButton.Text(
                            text = "CARDS",
                            onClick = { navigateTo(screenFactory = { CardsHomeScreen(it) }) },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun WalletsListRow(
    wallet: WalletSummary,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(
            text = wallet.name,
            variant = LightTextVariant.Copy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .lightClickable(onClick = onOpen),
        )
        LightIcon(
            icon = LightIcons.PENCIL,
            size = 1.5f,
            contentDescription = "Rename ${wallet.name}",
            modifier = Modifier
                .padding(start = 1f.gridUnitsAsDp())
                .lightClickable(onClick = onRename),
        )
    }
}
