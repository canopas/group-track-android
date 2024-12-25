package com.canopas.yourspace.data.utils

import org.bouncycastle.crypto.InvalidCipherTextException
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kdf.HKDF
import timber.log.Timber
import java.nio.ByteBuffer
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun encryptWithPublicKey(publicKey: ByteArray, data: ByteArray): ByteArray {
        val keyFactory = java.security.KeyFactory.getInstance("RSA", "BC")
        val publicKeySpec = java.security.spec.X509EncodedKeySpec(publicKey)
        val publicKeyObj: PublicKey = keyFactory.generatePublic(publicKeySpec)

        val cipher = try {
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize cipher", e)
        }

        cipher.init(Cipher.ENCRYPT_MODE, publicKeyObj)

        return cipher.doFinal(data)
    }

    fun decryptWithPrivateKey(privateKey: ByteArray, encryptedData: ByteArray): ByteArray {
        val keyFactory = java.security.KeyFactory.getInstance("RSA", "BC")
        val privateKeySpec = java.security.spec.PKCS8EncodedKeySpec(privateKey)
        val privateKeyObj: PrivateKey = keyFactory.generatePrivate(privateKeySpec)

        val cipher = try {
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC")
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize cipher", e)
        }

        cipher.init(Cipher.DECRYPT_MODE, privateKeyObj)

        return cipher.doFinal(encryptedData)
    }
}

/**
 * A production-ready example of ephemeral ECDH encryption ("ECIES") with:
 *  - Curve25519 for ECDH,
 *  - HKDF for key derivation,
 *  - AES/GCM for authenticated encryption.
 *
 * Data layout = [ ephemeralPub(32|33 bytes) || iv(12 bytes) || ciphertext+authTag(...) ].
 */
object CryptoUtils {

    private const val KEY_LENGTH_BYTES = 32 // 256-bit AES key
    private const val IV_LENGTH_BYTES = 12 // 96-bit GCM IV
    private const val GCM_TAG_BITS = 128

    private val secureRandom = SecureRandom()

    /**
     * Encrypts [plaintext] for [recipientPub] using ephemeral ECDH -> HKDF -> AES/GCM.
     * Returns a byte array containing:
     *   ephemeralPub + iv + AES-GCM ciphertext (which includes auth tag).
     */
    fun encryptForPublicKey(plaintext: ByteArray, recipientPub: ECPublicKey): ByteArray {
        // 1) Generate ephemeral key pair
        val ephemeralPair = Curve.generateKeyPair() // Curve25519
        val ephemeralPubBytes = ephemeralPair.publicKey.serialize() // 32 or 33 bytes

        // 2) Compute ECDH shared secret
        val sharedSecret = Curve.calculateAgreement(recipientPub, ephemeralPair.privateKey)
        // 3) Derive AES key with HKDF
        val derivedAesKey = hkdfDeriveKey(sharedSecret, "ECDH-Encryption")

        // 4) AES/GCM encrypt
        val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derivedAesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val cipherText = cipher.doFinal(plaintext)

        // 5) Build final output: ephemeralPub + iv + ciphertext
        return ByteBuffer.allocate(ephemeralPubBytes.size + iv.size + cipherText.size)
            .put(ephemeralPubBytes)
            .put(iv)
            .put(cipherText)
            .array()
    }

    /**
     * Decrypts [ciphertext] with our Curve25519 [myPrivate] key.
     * Expects the layout: ephemeralPub(32|33) + iv(12) + actualCiphertext.
     */
    fun decryptWithPrivateKey(ciphertext: ByteArray, myPrivate: ECPrivateKey): ByteArray {
        // 1) Parse ephemeralPub from the front
        //    Typically 32 bytes for Curve25519. Some libs produce 33. Check your library's format.
        val ephemeralPubSize = getEphemeralPubLength(ciphertext)
        val ephemeralPubBytes = ciphertext.sliceArray(0 until ephemeralPubSize)
        val ephemeralPub = Curve.decodePoint(ephemeralPubBytes, 0) as ECPublicKey

        // 2) Next 12 bytes = IV
        val ivStart = ephemeralPubSize
        val ivEnd = ivStart + IV_LENGTH_BYTES
        val iv = ciphertext.sliceArray(ivStart until ivEnd)

        // 3) The rest is the GCM ciphertext+authTag
        val actualCipherBytes = ciphertext.sliceArray(ivEnd until ciphertext.size)

        // 4) ECDH
        val sharedSecret = Curve.calculateAgreement(ephemeralPub, myPrivate)
        val derivedAesKey = hkdfDeriveKey(sharedSecret, "ECDH-Encryption")

        // 5) AES/GCM decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(derivedAesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(actualCipherBytes)
    }

    /**
     * Derive a 32-byte key from [sharedSecret] + optional [info] using HKDF (SHA-256).
     */
    private fun hkdfDeriveKey(sharedSecret: ByteArray, info: String): ByteArray {
        // salt = null => effectively empty
        // We use HKDFv3 from libsignal-protocol
        return HKDF.deriveSecrets(
            sharedSecret,
            ByteArray(0), // no salt
            info.toByteArray(), // info context
            KEY_LENGTH_BYTES // 32-byte key
        )
    }

    /**
     * Attempt to detect ephemeralPub length. Typically 32 for Curve25519.
     * If your ephemeral keys might be 33, adjust logic as needed.
     */
    private fun getEphemeralPubLength(ciphertext: ByteArray): Int {
        if (ciphertext.size < 32 + 12) {
            throw InvalidCipherTextException("Ciphertext too short to contain ephemeralPub + IV")
        }
        return 32
    }
}

/**
 * Demonstrates ephemeral ECDH encryption ("ECIES") with ephemeral Curve25519 keys
 * for **each message**, providing forward secrecy in a simplified manner.
 *
 * Layout of the resulting ciphertext:
 *   [ ephemeralPub(32 bytes) || iv(12 bytes) || AES/GCM ciphertext+tag(...) ].
 *
 * 1) For each message:
 *    - Generate ephemeral key pair (sender).
 *    - ECDH with recipient’s long-term public key => sharedSecret.
 *    - HKDF => AES key.
 *    - AES/GCM => final ciphertext.
 * 2) Recipient uses ephemeralPub + their private key to derive the same AES key.
 */
object EphemeralECDHUtils {

    private const val AES_KEY_SIZE = 32 // 256-bit AES key
    private const val IV_SIZE = 12 // 96-bit GCM IV
    private const val GCM_TAG_BITS = 128
    private val secureRandom = SecureRandom()

    /**
     * Encrypt [plaintext] using ephemeral Curve25519 for each message.
     * [recipientPub] is the recipient's **long-term** public key.
     */
    fun encrypt(plaintext: ByteArray, recipientPub: ECPublicKey): ByteArray {
        // 1) Generate ephemeral key pair
        val ephemeralPair = Curve.generateKeyPair() // ephemeral keys
        val ephemeralPubBytes = ephemeralPair.publicKey.serialize() // 32 bytes

        // 2) ECDH => shared secret
        val sharedSecret = Curve.calculateAgreement(recipientPub, ephemeralPair.privateKey)

        // 3) HKDF => 256-bit AES key
        val aesKey = deriveAesKey(sharedSecret, "Ephemeral-FS")

        // 4) AES/GCM encrypt
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        // 5) Combine ephemeralPub + iv + ciphertext
        return ByteBuffer.allocate(ephemeralPubBytes.size + iv.size + ciphertext.size)
            .put(ephemeralPubBytes)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    /**
     * Decrypt using recipient's **private** key + ephemeral pub from the ciphertext.
     */
    fun decrypt(ciphertext: ByteArray, recipientPrivate: ECPrivateKey): ByteArray {
        if (ciphertext.size < 32 + IV_SIZE) {
            throw IllegalArgumentException("Ciphertext too small. Expected at least 44 bytes.")
        }

        Timber.e("Decrypting ciphertext of size: ${ciphertext.size}")
        // 1) Extract ephemeralPub(32 bytes) from the front
        val ephemeralPubBytes = ciphertext.sliceArray(0 until 32)
        val ephemeralPub = Curve.decodePoint(ephemeralPubBytes, 0)

        // 2) Next 12 bytes => IV
        val ivStart = 32
        val ivEnd = ivStart + IV_SIZE
        val iv = ciphertext.sliceArray(ivStart until ivEnd)

        // 3) Remainder => GCM ciphertext
        val actualCipher = ciphertext.sliceArray(ivEnd until ciphertext.size)

        // 4) ECDH => sharedSecret
        val sharedSecret = Curve.calculateAgreement(ephemeralPub, recipientPrivate)

        // 5) HKDF => AES key
        val aesKey = deriveAesKey(sharedSecret, "Ephemeral-FS")

        // 6) AES/GCM decrypt
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(actualCipher)
    }

    private fun deriveAesKey(sharedSecret: ByteArray, info: String): ByteArray {
        // Use org.signal.libsignal.protocol.kdf.HKDF (SHA-256 based)
        // No salt => pass ByteArray(0)
        return HKDF.deriveSecrets(
            sharedSecret,
            ByteArray(0),
            info.toByteArray(),
            AES_KEY_SIZE
        )
    }
}
