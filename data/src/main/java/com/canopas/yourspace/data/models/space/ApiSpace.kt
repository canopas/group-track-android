package com.canopas.yourspace.data.models.space

import androidx.annotation.Keep
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.Exclude
import java.util.UUID
import java.util.concurrent.TimeUnit

@Keep
data class ApiSpace(
    val id: String = UUID.randomUUID().toString(),
    val admin_id: String = "",
    val name: String = "",
    val is_encryption_enabled: Boolean = true,
    val created_at: Long? = System.currentTimeMillis()
)

const val SPACE_MEMBER_ROLE_ADMIN = 1
const val SPACE_MEMBER_ROLE_MEMBER = 2

@Keep
data class ApiSpaceMember(
    val id: String = UUID.randomUUID().toString(),
    val space_id: String = "",
    val user_id: String = "",
    val role: Int = SPACE_MEMBER_ROLE_MEMBER,
    val location_enabled: Boolean = true,
    val identity_key_public: Blob? = Blob.fromBytes(ByteArray(0)),
    val created_at: Long? = System.currentTimeMillis()
)

@Keep
data class ApiSpaceInvitation(
    val id: String = UUID.randomUUID().toString(),
    val space_id: String = "",
    val code: String = "",
    val created_at: Long? = System.currentTimeMillis()
) {
    @get:Exclude
    val isExpired: Boolean
        get() {
            if (created_at == null) return true
            val currentTimeMillis = System.currentTimeMillis()
            val twoDaysMillis = TimeUnit.DAYS.toMillis(2)

            val differenceMillis = currentTimeMillis - this.created_at

            return differenceMillis > twoDaysMillis
        }
}

/**
 * Group key document structure for a single space.
 */
@Keep
data class GroupKeysDoc(
    val doc_updated_at: Long = System.currentTimeMillis(), // To be updated whenever users are added/removed
    val member_keys: Map<String, MemberKeyData> = emptyMap()
)

/**
 * Data class that represents the entire "groupKeys/{senderUserId}" doc
 * in Firestore for a single sender's key distribution.
 */
@Keep
data class MemberKeyData(
    val member_device_id: Int = 0,
    val distributions: List<EncryptedDistribution> = emptyList(),
    val data_updated_at: Long = System.currentTimeMillis() // To be updated whenever a new distribution is added
)

/**
 * Represents one encrypted distribution for a specific recipient.
 * Each recipient gets their own ciphertext, which is the group SenderKeyDistributionMessage
 * encrypted with the recipient's public key.
 */
data class EncryptedDistribution(
    val id: String = UUID.randomUUID().toString(),
    val recipient_id: String = "",
    val ephemeral_pub: Blob = Blob.fromBytes(ByteArray(0)), // 33 bytes (compressed distribution key)
    val iv: Blob = Blob.fromBytes(ByteArray(0)), // 16 bytes
    val ciphertext: Blob = Blob.fromBytes(ByteArray(0)), // AES/GCM ciphertext
    val created_at: Long = System.currentTimeMillis()
) {
    init {
        validateFieldSizes()
    }

    private fun validateFieldSizes() {
        require(ephemeral_pub.toBytes().size == 33 || ephemeral_pub.toBytes().isEmpty()) {
            "Invalid size for ephemeralPub: expected 33 bytes, got ${ephemeral_pub.toBytes().size} bytes."
        }
        require(iv.toBytes().size == 16 || iv.toBytes().isEmpty()) {
            "Invalid size for iv: expected 16 bytes, got ${iv.toBytes().size} bytes."
        }
        require(ciphertext.toBytes().size <= 64 * 1024 || ciphertext.toBytes().isEmpty()) {
            "Invalid size for ciphertext: maximum allowed size is 64 KB, got ${ciphertext.toBytes().size} bytes."
        }
    }
}
