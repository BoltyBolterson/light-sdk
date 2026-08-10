package com.thelightphone.wallet

import org.sol4k.Constants
import org.sol4k.Keypair
import org.sol4k.PublicKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun testKey(offset: Int): PublicKey =
    Keypair.fromSecretKey(ByteArray(32) { (it + offset).toByte() }).publicKey

class SplTokenTransferTest {

    private val usdc = PublicKey(SolanaStablecoins.USDC.mintAddress)
    private val usdt = PublicKey(SolanaStablecoins.USDT.mintAddress)
    private val owner = testKey(0)
    private val other = testKey(7)

    @Test
    fun onlyAllowListedMintsResolve() {
        assertEquals(SolanaStablecoins.USDC, SolanaStablecoins.allowed(SolanaStablecoins.USDC.mintAddress))
        assertEquals(SolanaStablecoins.USDT, SolanaStablecoins.allowed(SolanaStablecoins.USDT.mintAddress))
        assertNull(SolanaStablecoins.allowed(owner.toBase58()))
        assertNull(SolanaStablecoins.allowed(SolanaStablecoins.USDC.mintAddress.lowercase()))
        assertNull(SolanaStablecoins.allowed(""))
    }

    @Test
    fun onlyWellFormedSolanaAddressesBecomeRecipients() {
        assertEquals(owner, SplTokenTransfer.recipientKey(owner.toBase58()))
        assertEquals(owner, SplTokenTransfer.recipientKey("  ${owner.toBase58()}\n"))
        assertNull(SplTokenTransfer.recipientKey("0x0000000000000000000000000000000000000000"))
        assertNull(SplTokenTransfer.recipientKey("not an address"))
        assertNull(SplTokenTransfer.recipientKey(""))
    }

    @Test
    fun base58OfTheRightLengthButTheWrongByteCountIsRejected() {
        assertNull(SplTokenTransfer.recipientKey("1".repeat(44)))
        assertNull(SplTokenTransfer.recipientKey("1".repeat(33)))
    }

    @Test
    fun aWalletCanHoldTokensAndAProgramDerivedAddressCannot() {
        assertTrue(SplTokenTransfer.canHoldTokens(owner))
        assertTrue(SplTokenTransfer.canHoldTokens(other))
        assertFalse(SplTokenTransfer.canHoldTokens(SplTokenTransfer.associatedTokenAccount(owner, usdc)))
        assertFalse(SplTokenTransfer.canHoldTokens(PublicKey(ByteArray(32))))
    }

    @Test
    fun theAssociatedAccountUsesTheDocumentedSeedOrder() {
        val expected = PublicKey.findProgramAddress(
            listOf(owner, Constants.TOKEN_PROGRAM_ID, usdc),
            Constants.ASSOCIATED_TOKEN_PROGRAM_ID,
        ).publicKey
        assertEquals(expected, SplTokenTransfer.associatedTokenAccount(owner, usdc))
    }

    @Test
    fun theAssociatedAccountIsStableAndDistinctPerOwnerAndMint() {
        val ata = SplTokenTransfer.associatedTokenAccount(owner, usdc)
        assertEquals(ata, SplTokenTransfer.associatedTokenAccount(owner, usdc))
        assertNotEquals(ata, SplTokenTransfer.associatedTokenAccount(owner, usdt))
        assertNotEquals(ata, SplTokenTransfer.associatedTokenAccount(other, usdc))
        assertNotEquals(ata, owner)
    }

    @Test
    fun everyRefusalLandsBeforeAnyNetworkCallOrKeyUse() {
        val sender = SplTokenSender("http://127.0.0.1:1")
        val unusedKey = ByteArray(32)
        val usdcMint = SolanaStablecoins.USDC.mintAddress
        val destination = other.toBase58()

        assertEquals(
            TokenSendResult.Refused(TokenSendRefusal.MINT_NOT_ALLOWED),
            sender.send(unusedKey, owner.toBase58(), destination, "1"),
        )
        assertEquals(
            TokenSendResult.Refused(TokenSendRefusal.BAD_AMOUNT),
            sender.send(unusedKey, usdcMint, destination, "0.0000001"),
        )
        assertEquals(
            TokenSendResult.Refused(TokenSendRefusal.BAD_AMOUNT),
            sender.send(unusedKey, usdcMint, destination, "0"),
        )
        assertEquals(
            TokenSendResult.Refused(TokenSendRefusal.BAD_DESTINATION),
            sender.send(unusedKey, usdcMint, "nope", "1"),
        )
        assertEquals(
            TokenSendResult.Refused(TokenSendRefusal.DESTINATION_CANNOT_HOLD_TOKENS),
            sender.send(
                unusedKey,
                usdcMint,
                SplTokenTransfer.associatedTokenAccount(other, usdc).toBase58(),
                "1",
            ),
        )
    }
}
