package com.thelightphone.wallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightTextField

@Composable
internal fun SimpleLightScreen<*>.EditableField(
    label: String,
    title: String,
    value: String,
    placeholder: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    initialCaps: Boolean = false,
) {
    LightTextField(
        label = label,
        value = value,
        placeholder = placeholder,
        onClick = {
            navigateTo(
                screenFactory = {
                    WalletTextEditorScreen(
                        it,
                        WalletEditorRequest(
                            title = title,
                            initialValue = value,
                            initialCaps = initialCaps,
                        ),
                    )
                },
                resultCallback = onValue,
            )
        },
        modifier = modifier,
    )
}
