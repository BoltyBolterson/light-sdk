package com.thelightphone.wallet.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.buildDatabase
import com.thelightphone.sdk.ui.LightBarButton
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
import com.thelightphone.wallet.CardRepository
import com.thelightphone.wallet.WalletAccountRepository
import com.thelightphone.wallet.WalletDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShowCardScreen(
    sealedActivity: SealedLightActivity,
    private val cardId: Long,
    private val cardName: String,
    private val barcodeFormat: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    private val repository = CardRepository.getInstance {
        WalletDatabase.build(lightContext)
    }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        var payload by remember { mutableStateOf<String?>(null) }
        var errorModal by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(cardId) {
            val decrypted = withContext(Dispatchers.IO) {
                runCatching { repository.getDecryptedPayload(cardId) }
            }
            decrypted
                .onSuccess { payload = it }
                .onFailure { errorModal = it.message ?: "Couldn't decrypt this card." }
        }

        val isQr = barcodeFormat.contains("QR", ignoreCase = true)

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
                    center = LightTopBarCenter.Text(cardName),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                LightScrollView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                ) {
                    val current = payload
                    if (current == null) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "decrypting…",
                                variant = LightTextVariant.Copy,
                                align = TextAlign.Center,
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (isQr) 1f else 2.5f)
                                .border(width = 2.dp, color = LightThemeTokens.colors.content)
                                .padding(1f.gridUnitsAsDp()),
                            contentAlignment = Alignment.Center,
                        ) {
                            LightText(
                                text = "Not scannable yet\n\nthe code is below",
                                variant = LightTextVariant.Detail,
                                align = TextAlign.Center,
                            )
                        }

                        LightText(
                            text = "Raw payload:",
                            variant = LightTextVariant.Detail,
                            lighten = true,
                            modifier = Modifier.padding(top = 1.5f.gridUnitsAsDp()),
                        )
                        LightText(
                            text = current,
                            variant = LightTextVariant.Copy,
                            monospace = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 0.25f.gridUnitsAsDp()),
                        )
                    }
                }
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
