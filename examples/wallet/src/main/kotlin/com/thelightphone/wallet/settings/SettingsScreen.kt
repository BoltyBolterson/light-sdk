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
import com.thelightphone.wallet.keyboard.KeyboardLayout

/**
 * Wallet-wide settings: keyboard layout, per-chain enable/disable, BYO RPC endpoints, offline
 * mode, and manual refresh mode. Unlike `WeatherScreenMode.Settings` (a mode of that example's
 * single screen), this is its own screen class - these settings span the whole wallet rather
 * than one screen's concerns, so there's no natural "parent" screen mode to fold them into.
 */
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
                        SectionHeading("Keyboard Layout")
                        KeyboardLayout.entries.forEach { layout ->
                            KeyboardLayoutRow(
                                layout = layout,
                                selected = state.keyboardLayout == layout,
                                onSelect = { viewModel.setKeyboardLayout(layout) },
                            )
                        }

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

                        SectionHeading("RPC Endpoints")
                        Chain.entries.forEach { chain ->
                            val endpoint = state.rpcEndpoints[chain] ?: ""
                            LightTextField(
                                label = "${chain.displayName} RPC:",
                                value = endpoint,
                                placeholder = "Default",
                                onClick = {
                                    navigateTo(
                                        screenFactory = {
                                            WalletTextEditorScreen(
                                                it,
                                                WalletEditorRequest(
                                                    title = "${chain.displayName} RPC",
                                                    initialValue = endpoint,
                                                ),
                                            )
                                        },
                                        resultCallback = { result -> viewModel.setRpcEndpoint(chain, result) },
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 0.5f.gridUnitsAsDp()),
                            )
                        }

                        SectionHeading("Network")
                        ToggleRow(
                            label = "Offline mode",
                            checked = state.offlineMode,
                            onToggle = { viewModel.setOfflineMode(!state.offlineMode) },
                        )
                        ToggleRow(
                            label = "Manual refresh only",
                            checked = state.manualRefreshMode,
                            onToggle = { viewModel.setManualRefreshMode(!state.manualRefreshMode) },
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

/**
 * Tappable row toggling a boolean setting on/off, built directly from [LightIcons.TOGGLE_STATE_ON]
 * / [LightIcons.TOGGLE_STATE_OFF] - this SDK has no dedicated switch/toggle composable yet.
 */
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

/** Tappable row for one option in a single-select group (here, [KeyboardLayout]). */
@Composable
private fun KeyboardLayoutRow(
    layout: KeyboardLayout,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lightClickable(onClick = onSelect)
            .padding(vertical = 0.5f.gridUnitsAsDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LightText(
            text = layout.label,
            variant = LightTextVariant.Copy,
            modifier = Modifier.weight(1f),
        )
        LightIcon(
            icon = if (selected) LightIcons.SELECT_ON else LightIcons.SELECT_OFF,
            contentDescription = "${layout.label}${if (selected) " (selected)" else ""}",
        )
    }
}
