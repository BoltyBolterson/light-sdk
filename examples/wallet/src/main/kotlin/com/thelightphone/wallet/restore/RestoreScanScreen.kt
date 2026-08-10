package com.thelightphone.wallet.restore

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
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightProgressBar
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.wallet.WalletStore

class RestoreScanScreen(
    sealedActivity: SealedLightActivity,
    private val walletId: Long,
) : LightScreen<Unit, RestoreScanViewModel>(sealedActivity) {

    private val repository = WalletStore.accounts(lightContext)

    override val viewModelClass: Class<RestoreScanViewModel>
        get() = RestoreScanViewModel::class.java

    override fun createViewModel() =
        RestoreScanViewModel(repository, walletId, lightContext.dataStore)

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val state by viewModel.state.collectAsState()

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
                    center = LightTopBarCenter.Text("Scan"),
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                val message = state.message
                if (message != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        LightText(
                            text = message,
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
                            .padding(horizontal = 1f.gridUnitsAsDp()),
                    ) {
                        if (state.scanning) {
                            LightProgressBar(
                                colors = LightThemeTokens.colors,
                                progress = if (state.total > 0) state.done.toFloat() / state.total else 0f,
                            )
                            LightText(
                                text = "checking ${state.done} of ${state.total}…",
                                variant = LightTextVariant.Detail,
                                modifier = Modifier.padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }

                        state.summary?.let { summary ->
                            LightText(
                                text = scanHeadline(summary),
                                variant = LightTextVariant.Heading,
                                modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                            )
                            scanCaveat(summary)?.let { caveat ->
                                LightText(
                                    text = caveat,
                                    variant = LightTextVariant.Detail,
                                    modifier = Modifier.padding(bottom = 0.5f.gridUnitsAsDp()),
                                )
                            }
                        }

                        state.results.forEach { result ->
                            ScanResultRow(
                                result = result,
                                modifier = Modifier.padding(vertical = 0.5f.gridUnitsAsDp()),
                            )
                        }
                    }
                }

                LightBottomBar(
                    items = listOf(
                        LightBarButton.Text(
                            text = if (state.scanning) "SCANNING…" else "SCAN AGAIN",
                            onClick = { if (!state.scanning) viewModel.start() },
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun ScanResultRow(
    result: ScanResult,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            LightText(
                text = result.target.address.truncatedMiddle(),
                variant = LightTextVariant.Copy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LightText(
                text = result.target.pathLabel,
                variant = LightTextVariant.Detail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LightText(
            text = statusLabel(result.status),
            variant = LightTextVariant.Detail,
            maxLines = 1,
            modifier = Modifier.padding(start = 1f.gridUnitsAsDp()),
        )
    }
}

private fun String.truncatedMiddle(head: Int = 6, tail: Int = 6): String =
    if (length <= head + tail + 3) this else "${take(head)}…${takeLast(tail)}"
