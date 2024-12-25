package com.canopas.yourspace.data.utils

import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import java.security.SecureRandom

private const val INTEGER_MAX = 0x7fffffff

object KeyHelper {
    fun generateIdentityKeyPair(): IdentityKeyPair {
        val keyPair = Curve.generateKeyPair()
        val publicKey = IdentityKey(keyPair.publicKey)
        return IdentityKeyPair(publicKey, keyPair.privateKey)
    }

    fun generateRegistrationId(extendedRange: Boolean): Int {
        return KeyHelper.generateRegistrationId(extendedRange)
    }

    fun generatePreKeys(start: Int, count: Int): List<PreKeyRecord> {
        val results = mutableListOf<PreKeyRecord>()
        for (i in 0 until count) {
            results.add(PreKeyRecord(start + i, Curve.generateKeyPair()))
        }
        return results
    }

    fun generateSignedPreKey(identityKeyPair: IdentityKeyPair, signedPreKeyId: Int): SignedPreKeyRecord {
        val keyPair = Curve.generateKeyPair()
        val signature =
            Curve.calculateSignature(identityKeyPair.privateKey, keyPair.publicKey.serialize())
        return SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature)
    }

    fun generateSignedPreKeyId(): Int {
        val random = SecureRandom()
        return random.nextInt(INTEGER_MAX)
    }
}
