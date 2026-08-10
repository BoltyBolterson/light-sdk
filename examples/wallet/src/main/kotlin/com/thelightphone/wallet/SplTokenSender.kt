package com.thelightphone.wallet

import java.math.BigInteger
import org.bouncycastle.math.ec.rfc8032.Ed25519
import org.sol4k.Connection
import org.sol4k.Constants
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.CreateAssociatedTokenAccountInstruction
import org.sol4k.instruction.Instruction
import org.sol4k.instruction.SplTransferInstruction

enum class TokenSendRefusal {
    MINT_NOT_ALLOWED,
    BAD_AMOUNT,
    BAD_DESTINATION,
    DESTINATION_CANNOT_HOLD_TOKENS,
    NOT_ENOUGH_TOKEN,
    NOT_ENOUGH_SOL_FOR_FEE,
}

internal fun TokenSendResult.signatureOrThrow(symbol: String): String = when (this) {
    is TokenSendResult.Sent -> signature
    is TokenSendResult.Refused -> error(reason.message(symbol))
}

internal fun TokenSendRefusal.message(symbol: String): String = when (this) {
    TokenSendRefusal.MINT_NOT_ALLOWED -> "$symbol can't be sent from this wallet."
    TokenSendRefusal.BAD_AMOUNT -> "Enter a valid $symbol amount before sending."
    TokenSendRefusal.BAD_DESTINATION ->
        "Enter a valid ${Chain.SOLANA.displayName} address before sending."
    TokenSendRefusal.DESTINATION_CANNOT_HOLD_TOKENS -> "That address can't hold $symbol."
    TokenSendRefusal.NOT_ENOUGH_TOKEN -> "Not enough $symbol in this wallet."
    TokenSendRefusal.NOT_ENOUGH_SOL_FOR_FEE ->
        "Not enough SOL to pay the network fee. Sending $symbol still costs SOL."
}

sealed interface TokenSendResult {
    data class Sent(val signature: String, val createdRecipientAccount: Boolean) : TokenSendResult

    data class Refused(val reason: TokenSendRefusal) : TokenSendResult
}

internal object SplTokenTransfer {

    /** sol4k's PublicKey accepts any base58 length, so the 32-byte check has to happen here. */
    fun recipientKey(destination: String): PublicKey? {
        val address = AddressValidator.validate(destination, Chain.SOLANA) ?: return null
        val key = runCatching { PublicKey(address) }.getOrNull() ?: return null
        return key.takeIf { it.bytes().size == Ed25519.PUBLIC_KEY_SIZE }
    }

    /** Off-curve addresses are program-derived, so nobody can ever sign for tokens left there. */
    fun canHoldTokens(key: PublicKey): Boolean =
        key.bytes().size == Ed25519.PUBLIC_KEY_SIZE && Ed25519.validatePublicKeyFull(key.bytes(), 0)

    fun associatedTokenAccount(owner: PublicKey, mint: PublicKey): PublicKey =
        PublicKey.findProgramDerivedAddress(owner, mint).publicKey
}

private const val TOKEN_ACCOUNT_BYTES = 165

internal class SplTokenSender(
    private val rpcUrl: String,
    private val allowed: (String) -> AllowedStablecoin? = SolanaStablecoins::allowed,
) {

    fun send(
        privateKey: ByteArray,
        mintAddress: String,
        destination: String,
        amount: String,
    ): TokenSendResult {
        val token = allowed(mintAddress)
            ?: return refuse(TokenSendRefusal.MINT_NOT_ALLOWED)
        val units = token.toBaseUnits(amount)
            ?: return refuse(TokenSendRefusal.BAD_AMOUNT)
        val recipient = SplTokenTransfer.recipientKey(destination)
            ?: return refuse(TokenSendRefusal.BAD_DESTINATION)
        if (!SplTokenTransfer.canHoldTokens(recipient)) {
            return refuse(TokenSendRefusal.DESTINATION_CANNOT_HOLD_TOKENS)
        }

        val mint = PublicKey(token.mintAddress)
        val connection = Connection(rpcUrl)
        if (!connection.isWalletAccount(recipient)) {
            return refuse(TokenSendRefusal.DESTINATION_CANNOT_HOLD_TOKENS)
        }

        val target = SplTokenTransfer.associatedTokenAccount(recipient, mint)
        val existing = connection.getAccountInfo(target)
        if (existing != null && existing.owner != Constants.TOKEN_PROGRAM_ID) {
            return refuse(TokenSendRefusal.DESTINATION_CANNOT_HOLD_TOKENS)
        }

        val mintInfo = connection.getAccountInfo(mint)
            ?: error("${token.symbol} does not exist on this network.")
        if (mintInfo.owner != Constants.TOKEN_PROGRAM_ID) {
            error("${token.symbol} is not an SPL token on this network.")
        }

        val keypair = Keypair.fromSecretKey(privateKey)
        val source = SplTokenTransfer.associatedTokenAccount(keypair.publicKey, mint)
        val held = if (connection.getAccountInfo(source) == null) {
            BigInteger.ZERO
        } else {
            connection.getTokenAccountBalance(source).amount
        }
        if (held < BigInteger.valueOf(units)) return refuse(TokenSendRefusal.NOT_ENOUGH_TOKEN)

        val rent = if (existing == null) {
            connection.getMinimumBalanceForRentExemption(TOKEN_ACCOUNT_BYTES)
        } else {
            0L
        }
        if (connection.getBalance(keypair.publicKey).longValueExact() < LAMPORTS_PER_SIGNATURE + rent) {
            return refuse(TokenSendRefusal.NOT_ENOUGH_SOL_FOR_FEE)
        }

        val instructions = buildList<Instruction> {
            if (existing == null) {
                add(CreateAssociatedTokenAccountInstruction(keypair.publicKey, target, recipient, mint))
            }
            add(SplTransferInstruction(source, target, mint, keypair.publicKey, units, token.decimals))
        }
        val transaction = Transaction(connection.getLatestBlockhash(), instructions, keypair.publicKey)
        transaction.sign(keypair)
        return TokenSendResult.Sent(connection.sendTransaction(transaction), existing == null)
    }

    private fun refuse(reason: TokenSendRefusal): TokenSendResult = TokenSendResult.Refused(reason)

    private fun Connection.isWalletAccount(key: PublicKey): Boolean {
        val info = getAccountInfo(key) ?: return true
        return !info.executable && info.owner == Constants.SYSTEM_PROGRAM
    }
}
