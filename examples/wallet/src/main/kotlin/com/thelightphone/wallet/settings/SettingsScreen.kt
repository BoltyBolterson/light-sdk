package com.thelightphone.wallet.settings

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
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.thelightphone.wallet.Chain
import com.thelightphone.wallet.WalletEditorRequest
import com.thelightphone.wallet.WalletTextEditorScreen

class SettingsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, SettingsViewModel>(sealedActivity) {

    override val viewModelClass: Class<SettingsViewModel>
        get() = SettingsViewModel::class.java

    override fun createViewModel(): SettingsViewModel = SettingsViewModel(lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.uiState.collectAsState()
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
                    center = LightTopBarCenter.Text("Settings"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    SectionHeading("Chains")
                    Chain.entries.forEach { chain ->
                        ToggleRow(
                            label = chain.displayName,
                            checked = state.chainEnabled[chain] ?: true,
                            onToggle = {
                                viewModel.setChainEnabled(chain, !(state.chainEnabled[chain] ?: true))
                            },
                        )
                    }

                    SectionHeading("Solana RPC")
                    LightTextField(
                        label = "Endpoint:",
                        value = state.solanaRpc,
                        placeholder = "Required to send",
                        onClick = {
                            navigateTo(
                                screenFactory = {
                                    WalletTextEditorScreen(
                                        it,
                                        WalletEditorRequest(
                                            title = "Solana RPC",
                                            initialValue = state.solanaRpc,
                                        ),
                                    )
                                },
                                resultCallback = { viewModel.setSolanaRpc(it) },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 0.5f.gridUnitsAsDp()),
                    )
                }
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

@Composable
private fun SectionHeading(text: String) {
    LightText(
        text = text,
        variant = LightTextVariant.Heading,
        modifier = Modifier.padding(top = 1.5f.gridUnitsAsDp(), bottom = 0.5f.gridUnitsAsDp()),
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onToggle)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(
            text = label,
            variant = LightTextVariant.Copy,
            modifier = Modifier.weight(1f),
        )
        LightIcon(
            icon = if (checked) LightIcons.TOGGLE_STATE_ON else LightIcons.TOGGLE_STATE_OFF,
            contentDescription = "$label: ${if (checked) "on" else "off"}",
        )
    }
}
