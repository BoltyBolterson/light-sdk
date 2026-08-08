package com.thelightphone.wallet.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore key definitions for wallet-wide settings (as opposed to per-wallet or per-account
 * state, which lives in [com.thelightphone.wallet.WalletDatabase]). Follows the same pattern as
 * `WeatherPreferences` in the weather example: plain preference keys, no defaults baked in here -
 * callers decide fallback values when reading (see [SettingsViewModel]).
 */
internal object WalletPreferences {
    /** Stores the [com.thelightphone.wallet.keyboard.KeyboardLayout] enum name. */
    val KEYBOARD_LAYOUT = stringPreferencesKey("keyboard_layout")

    val CHAIN_BITCOIN_ENABLED = booleanPreferencesKey("chain_bitcoin_enabled")
    val CHAIN_ETHEREUM_ENABLED = booleanPreferencesKey("chain_ethereum_enabled")
    val CHAIN_SOLANA_ENABLED = booleanPreferencesKey("chain_solana_enabled")

    /** Empty/absent means "use the built-in default endpoint" for that chain. */
    val RPC_BITCOIN = stringPreferencesKey("rpc_bitcoin")
    val RPC_ETHEREUM = stringPreferencesKey("rpc_ethereum")
    val RPC_SOLANA = stringPreferencesKey("rpc_solana")

    /** Suppresses all network calls entirely when true. */
    val OFFLINE_MODE = booleanPreferencesKey("offline_mode")

    /**
     * Keeps network available but disables any auto-fetch (no polling on screen-show, no
     * timers) when true - data only updates on an explicit user-triggered refresh. Distinct
     * from [OFFLINE_MODE], which suppresses network calls outright.
     */
    val MANUAL_REFRESH_MODE = booleanPreferencesKey("manual_refresh_mode")
}
