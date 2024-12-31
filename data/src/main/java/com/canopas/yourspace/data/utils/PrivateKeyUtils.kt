package com.canopas.yourspace.data.utils

import timber.log.Timber
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

private const val AES_ALGORITHM = "AES/GCM/NoPadding"
private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
private const val KEY_SIZE = 256 // bits
private const val ITERATION_COUNT = 10000
private const val GCM_IV_SIZE = 12 // bytes
private const val GCM_TAG_SIZE = 128 // bits

class EncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)

object PrivateKeyUtils {

    /**
     * Derives a SecretKey from the user's passkey/PIN using PBKDF2.
     */
    private fun deriveKeyFromPasskey(passkey: String, salt: ByteArray): SecretKey {
        return try {
            val spec = PBEKeySpec(passkey.toCharArray(), salt, ITERATION_COUNT, KEY_SIZE)
            val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } catch (e: Exception) {
            Timber.e(e, "Key derivation failed")
            throw EncryptionException("Key derivation failed", e)
        }
    }

    /**
     * Encrypts data using AES-GCM with the provided key.
     * Returns the IV prepended to the ciphertext.
     */
    private fun encryptData(data: ByteArray, key: SecretKey): ByteArray {
        return try {
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            val iv = ByteArray(GCM_IV_SIZE).apply { Random.nextBytes(this) }
            val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val encrypted = cipher.doFinal(data)
            // Prepend IV to ciphertext
            iv + encrypted
        } catch (e: Exception) {
            Timber.e(e, "Encryption failed")
            throw EncryptionException("Encryption failed", e)
        }
    }

    /**
     * Decrypts data using AES-GCM with the provided key.
     * Expects the IV to be prepended to the ciphertext.
     */
    private fun decryptData(encryptedData: ByteArray, key: SecretKey): ByteArray {
        return try {
            if (encryptedData.size < GCM_IV_SIZE) {
                throw EncryptionException("Encrypted data is too short")
            }
            val iv = encryptedData.copyOfRange(0, GCM_IV_SIZE)
            val ciphertext = encryptedData.copyOfRange(GCM_IV_SIZE, encryptedData.size)
            val cipher = Cipher.getInstance(AES_ALGORITHM)
            val spec = GCMParameterSpec(GCM_TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            Timber.e(e, "Decryption failed")
            throw EncryptionException("Decryption failed", e)
        }
    }

    /**
     * Encrypts the private key using the user's passkey/PIN.
     * Retrieves or generates the salt and stores it in Firestore via ApiUserService.
     */
    fun encryptPrivateKey(privateKey: ByteArray, passkey: String, salt: ByteArray): ByteArray {
        if (salt.isEmpty()) {
            throw EncryptionException("Salt is empty")
        }
        val key = deriveKeyFromPasskey(passkey, salt)
        return encryptData(privateKey, key)
    }

    /**
     * Decrypts the private key using the user's passkey/PIN and salt from ApiUser.
     */
    fun decryptPrivateKey(encryptedPrivateKey: ByteArray, salt: ByteArray, passkey: String): ByteArray? {
        return try {
            val key = deriveKeyFromPasskey(passkey, salt)
            decryptData(encryptedPrivateKey, key)
        } catch (e: EncryptionException) {
            Timber.e(e, "Failed to decrypt private key")
            null
        }
    }
}
