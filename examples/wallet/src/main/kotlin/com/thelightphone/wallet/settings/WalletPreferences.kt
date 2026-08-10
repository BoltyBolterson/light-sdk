package com.thelightphone.wallet.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.thelightphone.wallet.Chain
import kotlinx.coroutines.flow.first

internal object WalletPreferences {
    val CHAIN_BITCOIN_ENABLED = booleanPreferencesKey("chain_bitcoin_enabled")
    val CHAIN_ETHEREUM_ENABLED = booleanPreferencesKey("chain_ethereum_enabled")
    val CHAIN_SOLANA_ENABLED = booleanPreferencesKey("chain_solana_enabled")

    fun chainEnabledKey(chain: Chain) = when (chain) {
        Chain.BITCOIN -> CHAIN_BITCOIN_ENABLED
        Chain.ETHEREUM -> CHAIN_ETHEREUM_ENABLED
        Chain.SOLANA -> CHAIN_SOLANA_ENABLED
    }

    suspend fun enabledChains(dataStore: DataStore<Preferences>): List<Chain> {
        val prefs = runCatching { dataStore.data.first() }.getOrNull() ?: return Chain.entries
        return Chain.entries.filter { prefs[chainEnabledKey(it)] ?: true }
    }
}
