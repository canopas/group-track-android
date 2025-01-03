package com.canopas.yourspace.data.utils

import com.canopas.yourspace.data.models.space.EncryptedDistribution
import com.google.firebase.firestore.Blob
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.util.ByteUtil
import timber.log.Timber
import java.nio.charset.Charset
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for encryption and decryption using Ephemeral Elliptic Curve Diffie-Hellman (ECDH) mechanism.
 * This class provides methods to encrypt and decrypt data securely using a recipient's public key
 * and an ephemeral key pair.
 */
object EphemeralECDHUtils {

    private const val SYNTHETIC_IV_LENGTH = 16 // Length of the synthetic initialization vector (IV).

    /**
     * Encrypts the provided plaintext for a specific recipient using their public key.
     *
     * @param receiverId The unique identifier of the receiver.
     * @param plaintext The data to be encrypted as a byte array.
     * @param receiverPub The receiver's public key.
     * @return EncryptedDistribution The encrypted data and associated metadata.
     */
    fun encrypt(
        receiverId: String,
        plaintext: ByteArray,
        receiverPub: ECPublicKey
    ): EncryptedDistribution {
        val ephemeralKeyPair: ECKeyPair = Curve.generateKeyPair()
        val masterSecret: ByteArray = Curve.calculateAgreement(receiverPub, ephemeralKeyPair.privateKey)
        val syntheticIv: ByteArray = computeSyntheticIv(masterSecret, plaintext)
        val cipherKey: ByteArray = computeCipherKey(masterSecret, syntheticIv)

        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(cipherKey, "AES"), IvParameterSpec(syntheticIv))
        val cipherText = cipher.doFinal(plaintext)

        return EncryptedDistribution(
            recipientId = receiverId,
            ephemeralPub = Blob.fromBytes(ephemeralKeyPair.publicKey.serialize()),
            iv = Blob.fromBytes(syntheticIv),
            ciphertext = Blob.fromBytes(cipherText)
        )
    }

    /**
     * Decrypts an encrypted message using the recipient's private key.
     *
     * @param message The encrypted distribution containing ciphertext and metadata.
     * @param receiverPrivateKey The receiver's private key.
     * @return ByteArray? The decrypted plaintext or null if decryption fails.
     */
    fun decrypt(
        message: EncryptedDistribution,
        receiverPrivateKey: ECPrivateKey
    ): ByteArray? {
        return try {
            val syntheticIv = message.iv.toBytes()
            val cipherText = message.ciphertext.toBytes()
            val ephemeralPublic = Curve.decodePoint(message.ephemeralPub.toBytes(), 0)
            val masterSecret = Curve.calculateAgreement(ephemeralPublic, receiverPrivateKey)

            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(masterSecret, "HmacSHA256"))
            val cipherKeyPart1 = mac.doFinal("cipher".toByteArray())

            mac.init(SecretKeySpec(cipherKeyPart1, "HmacSHA256"))
            val cipherKey = mac.doFinal(syntheticIv)

            val cipher = Cipher.getInstance("AES/CTR/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(cipherKey, "AES"), IvParameterSpec(syntheticIv))
            val plaintext = cipher.doFinal(cipherText)

            mac.init(SecretKeySpec(masterSecret, "HmacSHA256"))
            val verificationPart1 = mac.doFinal("auth".toByteArray())

            mac.init(SecretKeySpec(verificationPart1, "HmacSHA256"))
            val verificationPart2 = mac.doFinal(plaintext)
            val ourSyntheticIv = ByteUtil.trim(verificationPart2, 16)

            if (!MessageDigest.isEqual(ourSyntheticIv, syntheticIv)) {
                throw GeneralSecurityException("The computed syntheticIv didn't match the actual syntheticIv.")
            }
            plaintext
        } catch (e: GeneralSecurityException) {
            Timber.e(e, "Error while decrypting EphemeralCipherMessage")
            null
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error while decrypting EphemeralCipherMessage")
            null
        }
    }

    /**
     * Computes a synthetic IV using the master secret and plaintext.
     *
     * @param masterSecret The shared master secret.
     * @param plaintext The plaintext data.
     * @return ByteArray The computed synthetic IV.
     */
    private fun computeSyntheticIv(masterSecret: ByteArray, plaintext: ByteArray): ByteArray {
        val input = "auth".toByteArray(Charset.forName("UTF-8"))

        val keyMac = Mac.getInstance("HmacSHA256")
        keyMac.init(SecretKeySpec(masterSecret, "HmacSHA256"))
        val syntheticIvKey: ByteArray = keyMac.doFinal(input)

        val ivMac = Mac.getInstance("HmacSHA256")
        ivMac.init(SecretKeySpec(syntheticIvKey, "HmacSHA256"))
        return ivMac.doFinal(plaintext).sliceArray(0 until SYNTHETIC_IV_LENGTH)
    }

    /**
     * Computes the cipher key using the master secret and synthetic IV.
     *
     * @param masterSecret The shared master secret.
     * @param syntheticIv The synthetic IV.
     * @return ByteArray The computed cipher key.
     */
    private fun computeCipherKey(masterSecret: ByteArray, syntheticIv: ByteArray): ByteArray {
        val input = "cipher".toByteArray(Charset.forName("UTF-8"))

        val keyMac = Mac.getInstance("HmacSHA256")
        keyMac.init(SecretKeySpec(masterSecret, "HmacSHA256"))
        val cipherKeyKey: ByteArray = keyMac.doFinal(input)

        val cipherMac = Mac.getInstance("HmacSHA256")
        cipherMac.init(SecretKeySpec(cipherKeyKey, "HmacSHA256"))
        return cipherMac.doFinal(syntheticIv)
    }
}
