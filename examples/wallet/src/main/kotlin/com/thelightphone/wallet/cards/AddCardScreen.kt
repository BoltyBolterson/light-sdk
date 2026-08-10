package com.thelightphone.wallet.cards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.wallet.EditableField
import com.thelightphone.wallet.WalletScaffold
import com.thelightphone.wallet.WalletStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddCardScreen(
    sealedActivity: SealedLightActivity,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = WalletStore.cards(lightContext)

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var barcodeFormat by remember { mutableStateOf("") }
        var payload by remember { mutableStateOf("") }
        var errorModal by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        WalletScaffold(
            title = "Add Card",
            onBack = { goBack(Unit) },
            bottomBarItems = listOf(
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
        ) {
            LightScrollView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                EditableField(
                    label = "Name:",
                    title = "Name",
                    value = name,
                    placeholder = "Coffee shop rewards",
                    onValue = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                )

                EditableField(
                    label = "Barcode format:",
                    title = "Barcode format",
                    value = barcodeFormat,
                    placeholder = "QR_CODE, CODE_128, EAN_13…",
                    onValue = { barcodeFormat = it },
                    initialCaps = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                )

                EditableField(
                    label = "Payload:",
                    title = "Payload",
                    value = payload,
                    placeholder = "Raw barcode text or number",
                    onValue = { payload = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
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
