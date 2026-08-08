package com.thelightphone.wallet.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightTextField
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.wallet.CardRepository
import com.thelightphone.wallet.WalletAccountRepository
import com.thelightphone.wallet.WalletDatabase
import com.thelightphone.wallet.WalletEditorRequest
import com.thelightphone.wallet.WalletTextEditorScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCardScreen(
    sealedActivity: SealedLightActivity,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = CardRepository.getInstance {
        WalletDatabase.build(lightContext)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var barcodeFormat by remember { mutableStateOf("") }
        var payload by remember { mutableStateOf("") }
        var errorModal by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        LightTheme(colors = themeColors) {
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
                    center = LightTopBarCenter.Text("Add Card"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LightTextField(
                        label = "Name:",
                        value = name,
                        placeholder = "Coffee shop rewards",
                        onClick = {
                            navigateTo(
                                screenFactory = {
                                    WalletTextEditorScreen(
                                        it,
                                        WalletEditorRequest(title = "Name", initialValue = name),
                                    )
                                },
                                resultCallback = { name = it },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    )

                    LightTextField(
                        label = "Barcode format:",
                        value = barcodeFormat,
                        placeholder = "QR_CODE, CODE_128, EAN_13…",
                        onClick = {
                            navigateTo(
                                screenFactory = {
                                    WalletTextEditorScreen(
                                        it,
                                        WalletEditorRequest(
                                            title = "Barcode format",
                                            initialValue = barcodeFormat,
                                            initialCaps = true,
                                        ),
                                    )
                                },
                                resultCallback = { barcodeFormat = it },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    )

                    LightTextField(
                        label = "Payload:",
                        value = payload,
                        placeholder = "Raw barcode text or number",
                        onClick = {
                            navigateTo(
                                screenFactory = {
                                    WalletTextEditorScreen(
                                        it,
                                        WalletEditorRequest(title = "Payload", initialValue = payload),
                                    )
                                },
                                resultCallback = { payload = it },
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    )
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = "SCAN",
                            onClick = {
                                errorModal = "Scanning isn't available yet. Enter the card by hand."
                            },
                        ),
                        LightBarButton.Text(
                            text = "SAVE",
                            onClick = {
                                if (name.isBlank() || barcodeFormat.isBlank() || payload.isBlank()) {
                                    errorModal = "Fill in name, barcode format, and payload first."
                                } else if (!isSaving) {
                                    isSaving = true
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                repository.addCard(name, barcodeFormat, payload)
                                            }
                                            goBack(Unit)
                                        } finally {
                                            isSaving = false
                                        }
                                    }
                                }
                            },
                        ),
                    ),
                )
            }
        }

        errorModal?.let { message ->
            LightFullscreenModal(
                message = message,
                onClose = { errorModal = null },
            )
        }
    }
}
