package com.thelightphone.wallet

import org.sol4k.Connection
import org.sol4k.Keypair
import org.sol4k.PublicKey
import org.sol4k.Transaction
import org.sol4k.instruction.TransferInstruction

internal class SolanaSender(private val rpcUrl: String) {

    fun send(privateKey: ByteArray, destination: String, lamports: Long): String {
        val keypair = Keypair.fromSecretKey(privateKey)
        val connection = Connection(rpcUrl)
        val transaction = Transaction(
            connection.getLatestBlockhash(),
            TransferInstruction(keypair.publicKey, PublicKey(destination), lamports),
            keypair.publicKey,
        )
        transaction.sign(keypair)
        return connection.sendTransaction(transaction)
    }
}
