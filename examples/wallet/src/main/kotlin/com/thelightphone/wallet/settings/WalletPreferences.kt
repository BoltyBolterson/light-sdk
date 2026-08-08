package com.thelightphone.wallet.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object WalletPreferences {
    val KEYBOARD_LAYOUT = stringPreferencesKey("keyboard_layout")

    val CHAIN_BITCOIN_ENABLED = booleanPreferencesKey("chain_bitcoin_enabled")
    val CHAIN_ETHEREUM_ENABLED = booleanPreferencesKey("chain_ethereum_enabled")
    val CHAIN_SOLANA_ENABLED = booleanPreferencesKey("chain_solana_enabled")

    val RPC_BITCOIN = stringPreferencesKey("rpc_bitcoin")
    val RPC_ETHEREUM = stringPreferencesKey("rpc_ethereum")
    val RPC_SOLANA = stringPreferencesKey("rpc_solana")

    val OFFLINE_MODE = booleanPreferencesKey("offline_mode")

    val MANUAL_REFRESH_MODE = booleanPreferencesKey("manual_refresh_mode")
}
