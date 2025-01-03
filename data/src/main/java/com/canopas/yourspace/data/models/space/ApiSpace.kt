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
    val docUpdatedAt: Long = System.currentTimeMillis(), // To be updated whenever users are added/removed
    val memberKeys: Map<String, MemberKeyData> = emptyMap()
)

/*
 * Data class that represents the entire "groupKeys/{senderUserId}" doc
 * in Firestore for a single sender's key distribution.
 */
@Keep
data class MemberKeyData(
    val memberDeviceId: Int = 0,
    val distributions: List<EncryptedDistribution> = emptyList(),
    val dataUpdatedAt: Long = System.currentTimeMillis() // To be updated whenever a new distribution is added
)

/**
 * Represents one encrypted distribution for a specific recipient.
 * Each recipient gets their own ciphertext, which is the group SenderKeyDistributionMessage
 * encrypted with the recipient's public key.
 */
data class EncryptedDistribution(
    val recipientId: String = "",
    val ephemeralPub: Blob = Blob.fromBytes(ByteArray(0)), // 32 bytes
    val iv: Blob = Blob.fromBytes(ByteArray(0)), // 12 bytes
    val ciphertext: Blob = Blob.fromBytes(ByteArray(0)) // AES/GCM ciphertext
) {
    init {
        validateFieldSizes()
    }

    private fun validateFieldSizes() {
        require(ephemeralPub.toBytes().size == 33 || ephemeralPub.toBytes().isEmpty()) {
            "Invalid size for ephemeralPub: expected 33 bytes, got ${ephemeralPub.toBytes().size} bytes."
        }
        require(iv.toBytes().size == 16 || iv.toBytes().isEmpty()) {
            "Invalid size for iv: expected 16 bytes, got ${iv.toBytes().size} bytes."
        }
        require(ciphertext.toBytes().size <= 64 * 1024 || ciphertext.toBytes().isEmpty()) {
            "Invalid size for ciphertext: maximum allowed size is 64 KB, got ${ciphertext.toBytes().size} bytes."
        }
    }
}
