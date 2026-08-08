package com.thelightphone.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.wallet.nfc.NfcTapTarget
import com.thelightphone.wallet.nfc.UnavailableNfcTapTarget
import kotlinx.coroutines.launch

private const val QR_PLACEHOLDER_WIDTH_FRACTION = 0.58f
private const val QR_PLACEHOLDER_MODULE_COUNT = 21
private const val QR_PLACEHOLDER_CORNER_RADIUS_DP = 6
private const val QR_PLACEHOLDER_PADDING_DP = 10

class WalletReceiveScreen(
    sealedActivity: SealedLightActivity,
    private val account: WalletAccount,
    private val nfcTapTarget: NfcTapTarget = UnavailableNfcTapTarget,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val scope = rememberCoroutineScope()
        var errorModal by remember { mutableStateOf<String?>(null) }

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
                    center = LightTopBarCenter.Text("Receive ${account.chain.ticker}"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 1f.gridUnitsAsDp()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2f.gridUnitsAsDp()),
                    ) {
                        QrPlaceholder(
                            modifier = Modifier
                                .fillMaxWidth(QR_PLACEHOLDER_WIDTH_FRACTION)
                                .aspectRatio(1f),
                        )
                        LightText(
                            text = account.address,
                            variant = LightTextVariant.Detail,
                            align = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (account.chain == Chain.SOLANA && nfcTapTarget.isAvailable) {
                    LightBottomBar(
                        items = listOf(
                            LightBarButton.Text(
                                text = "TAP TO PAY",
                                onClick = {
                                    scope.launch {
                                        nfcTapTarget.awaitTap()
                                            .onFailure { errorModal = it.message }
                                    }
                                },
                            ),
                        ),
                    )
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

@Composable
private fun QrPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(QR_PLACEHOLDER_CORNER_RADIUS_DP.dp))
            .padding(QR_PLACEHOLDER_PADDING_DP.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cell = size.minDimension / QR_PLACEHOLDER_MODULE_COUNT
            for (row in 0 until QR_PLACEHOLDER_MODULE_COUNT) {
                for (col in 0 until QR_PLACEHOLDER_MODULE_COUNT) {
                    val filled = isQrPlaceholderFinderModule(row, col) || isQrPlaceholderFillerModule(row, col)
                    if (filled) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(col * cell, row * cell),
                            size = Size(cell, cell),
                        )
                    }
                }
            }
        }
    }
}

private fun isQrPlaceholderFinderModule(row: Int, col: Int): Boolean {
    val lastBlockStart = QR_PLACEHOLDER_MODULE_COUNT - 7
    return isQrPlaceholderFinderBlock(row, col, 0, 0) ||
        isQrPlaceholderFinderBlock(row, col, 0, lastBlockStart) ||
        isQrPlaceholderFinderBlock(row, col, lastBlockStart, 0)
}

private fun isQrPlaceholderFinderBlock(row: Int, col: Int, blockRow: Int, blockCol: Int): Boolean {
    val r = row - blockRow
    val c = col - blockCol
    if (r !in 0..6 || c !in 0..6) return false
    val isOuterRing = r == 0 || r == 6 || c == 0 || c == 6
    val isInnerCore = r in 2..4 && c in 2..4
    return isOuterRing || isInnerCore
}

private fun isQrPlaceholderFillerModule(row: Int, col: Int): Boolean =
    (row * 7 + col * 13 + row * col) % 5 < 2
