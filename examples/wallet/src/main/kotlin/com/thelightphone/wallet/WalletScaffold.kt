package com.thelightphone.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.LightTopBar
import com.thelightphone.sdk.ui.LightTopBarCenter
import com.thelightphone.sdk.ui.gridUnitsAsDp

@Composable
internal fun WalletScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    rightButton: LightBarButton? = null,
    bottomBarItems: List<LightBarButton> = emptyList(),
    overlay: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val themeColors by LightThemeController.colors.collectAsState()

    LightTheme(colors = themeColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                LightTopBar(
                    leftButton = onBack?.let {
                        LightBarButton.LightIcon(icon = LightIcons.BACK, onClick = it)
                    },
                    center = LightTopBarCenter.Text(title),
                    rightButton = rightButton,
                    modifier = Modifier.padding(bottom = 1f.gridUnitsAsDp()),
                )

                content()

                if (bottomBarItems.isNotEmpty()) {
                    LightBottomBar(items = bottomBarItems)
                }
            }

            overlay()
        }
    }
}
