package com.thelightphone.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.gridUnitsAsDp

@Composable
internal fun ColumnScope.ListOrEmpty(
    isEmpty: Boolean,
    emptyText: String,
    rows: @Composable ColumnScope.() -> Unit,
) {
    if (isEmpty) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LightText(
                text = emptyText,
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
            content = rows,
        )
    }
}
