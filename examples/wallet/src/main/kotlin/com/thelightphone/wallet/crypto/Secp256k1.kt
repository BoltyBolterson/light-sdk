package com.thelightphone.wallet.crypto

import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters

internal object Secp256k1 {
    private val curveParams = SECNamedCurves.getByName("secp256k1")
    val domain = ECDomainParameters(curveParams.curve, curveParams.g, curveParams.n, curveParams.h)
    val n: java.math.BigInteger = curveParams.n
}
