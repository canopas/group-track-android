package com.canopas.yourspace.data.security.helper

import android.util.Base64
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.SignalKeys
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import org.signal.libsignal.protocol.util.KeyHelper
import org.signal.libsignal.protocol.util.Medium
import java.util.LinkedList
import java.util.Random
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalKeyHelper @Inject constructor() {

    private fun generateIdentityKeyPair(): IdentityKeyPair {
        val djbKeyPair = Curve.generateKeyPair()
        val djbIdentityKey = IdentityKey(djbKeyPair.publicKey)
        val djbPrivateKey = djbKeyPair.privateKey

        return IdentityKeyPair(djbIdentityKey, djbPrivateKey)
    }

    @Throws(InvalidKeyException::class)
    fun generateSignedPreKey(
        identityKeyPair: IdentityKeyPair,
        signedPreKeyId: Int
    ): SignedPreKeyRecord {
        val keyPair = Curve.generateKeyPair()
        val signature =
            Curve.calculateSignature(identityKeyPair.privateKey, keyPair.publicKey.serialize())
        return SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature)
    }

    private fun generatePreKeys(): List<PreKeyRecord> {
        val records: MutableList<PreKeyRecord> = LinkedList()
        val preKeyIdOffset = Random().nextInt(Medium.MAX_VALUE - 101)
        for (i in 0 until 100) {
            val preKeyId = (preKeyIdOffset + i) % Medium.MAX_VALUE
            val keyPair = Curve.generateKeyPair()
            val record = PreKeyRecord(preKeyId, keyPair)

            records.add(record)
        }

        return records
    }

    fun generateSignalKeys(): SignalKeys {
        val identityKeyPair = generateIdentityKeyPair()
        val signedPreKey = generateSignedPreKey(identityKeyPair, Random().nextInt(Medium.MAX_VALUE - 1))
        val preKeys = generatePreKeys()
        val registrationId = KeyHelper.generateRegistrationId(false)

        return SignalKeys(
            identityKeyPair = identityKeyPair,
            signedPreKey = signedPreKey,
            preKeys = preKeys,
            registrationId = registrationId
        )
    }

    fun createDistributionKey(
        user: ApiUser,
        deviceId: String,
        spaceId: String
    ): Pair<String, String> {
        val signalProtocolAddress = SignalProtocolAddress(user.id, deviceId.hashCode())
        val identityKeyPair = IdentityKeyPair(
            IdentityKey(Curve.decodePoint(Base64.decode(user.public_key, Base64.DEFAULT), 0)),
            Curve.decodePrivatePoint(Base64.decode(user.private_key, Base64.DEFAULT))
        )
        val signalProtocolStore = InMemorySignalProtocolStore(identityKeyPair, user.registration_id)
        val signedPreKeyId = SignedPreKeyRecord(Helper.decodeToByteArray(user.signed_pre_key)).id
        val preKeys = SignedPreKeyRecord(Helper.decodeToByteArray(user.signed_pre_key))
        signalProtocolStore.storeSignedPreKey(signedPreKeyId, preKeys)

        user.pre_keys?.let { preKeyRecords ->
            val deserializedPreKeys =
                preKeyRecords.map { PreKeyRecord(Helper.decodeToByteArray(it)) }
            for (record in deserializedPreKeys) {
                signalProtocolStore.storePreKey(record.id, record)
            }
        }
        val validSpaceId = try {
            UUID.fromString(spaceId) // Validate if it's a proper UUID string
        } catch (e: IllegalArgumentException) {
            UUID.randomUUID() // Fallback to a new valid UUID if parsing fails
        }
        signalProtocolStore.storeSenderKey(
            signalProtocolAddress,
            validSpaceId,
            SenderKeyRecord(Helper.decodeToByteArray(user.signed_pre_key))
        )

        val sessionBuilder = GroupSessionBuilder(signalProtocolStore)
        val senderKeyDistributionMessage =
            sessionBuilder.create(signalProtocolAddress, validSpaceId)
        val senderKeyRecord = signalProtocolStore.loadSenderKey(signalProtocolAddress, validSpaceId)

        return Pair(
            Helper.encodeToBase64(senderKeyDistributionMessage.serialize()),
            Helper.encodeToBase64(senderKeyRecord.serialize())
        )
    }

    private fun encryptAESKeyWithECDH(
        aesKey: SecretKey,
        publicKey: String,
        senderPrivateKey: String
    ): String {
        val ecPublicKey = Curve.decodePoint(Base64.decode(publicKey, Base64.DEFAULT), 0)
        val ecPrivateKey = Curve.decodePrivatePoint(Base64.decode(senderPrivateKey, Base64.DEFAULT))
        val sharedSecret = Curve.calculateAgreement(ecPublicKey, ecPrivateKey)
        val secretKeySpec = SecretKeySpec(sharedSecret, 0, 32, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encryptedAESKey = cipher.doFinal(aesKey.encoded)

        return Base64.encodeToString(encryptedAESKey, Base64.NO_WRAP)
    }

    private fun decryptAESKeyWithECDH(
        encryptedAESKey: String,
        privateKey: String,
        senderPublicKey: String
    ): SecretKey {
        val ecPublicKey = Curve.decodePoint(Base64.decode(senderPublicKey, Base64.DEFAULT), 0)
        val ecPrivateKey = Curve.decodePrivatePoint(Base64.decode(privateKey, Base64.DEFAULT))
        val sharedSecret = Curve.calculateAgreement(ecPublicKey, ecPrivateKey)
        val secretKeySpec = SecretKeySpec(sharedSecret, 0, 32, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val decryptedAESKeyBytes = cipher.doFinal(Base64.decode(encryptedAESKey, Base64.DEFAULT))

        return SecretKeySpec(decryptedAESKeyBytes, "AES")
    }

    fun encryptSenderKeyForGroup(
        senderKey: String,
        senderPrivateKey: String,
        recipients: List<ApiUser?>
    ): Map<String, Pair<String, String>> {
        val encryptedKeys = mutableMapOf<String, Pair<String, String>>()
        val keyGen = KeyGenerator.getInstance("AES")
        val aesKey: SecretKey = keyGen.generateKey()
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val encryptedSenderKey =
            Base64.encodeToString(cipher.doFinal(senderKey.toByteArray()), Base64.NO_WRAP)
        recipients.forEach { recipient ->
            recipient?.let {
                val recipientPublicKey = recipient.public_key!!
                val encryptedAESKey =
                    encryptAESKeyWithECDH(aesKey, recipientPublicKey, senderPrivateKey)
                encryptedKeys[recipient.id] = Pair(encryptedSenderKey, encryptedAESKey)
            }
        }

        return encryptedKeys
    }

    fun decryptSenderKey(
        encryptedSenderKey: String,
        encryptedAESKey: String,
        recipientPrivateKey: String,
        senderPublicKey: String
    ): String {
        val aesKey = decryptAESKeyWithECDH(encryptedAESKey, recipientPrivateKey, senderPublicKey)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, aesKey)
        val decryptedSenderKeyBytes =
            cipher.doFinal(Base64.decode(encryptedSenderKey, Base64.DEFAULT))

        return String(decryptedSenderKeyBytes)
    }
}
