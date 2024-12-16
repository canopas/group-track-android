package com.canopas.yourspace.data.security.session

import android.util.Base64
import androidx.annotation.Keep
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.security.helper.Helper
import com.squareup.moshi.JsonClass
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.NoSessionException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import java.nio.charset.StandardCharsets
import java.util.UUID

class EncryptedSpaceSession(
    currentUser: ApiUser,
    keyRecord: String?,
    spaceId: String
) {
    private val spaceDistributionId: UUID = try {
        UUID.fromString(spaceId) // Validate if it's a proper UUID string
    } catch (e: IllegalArgumentException) {
        UUID.randomUUID() // Fallback to a new valid UUID if parsing fails
    }
    private val signalProtocolAddress = SignalProtocolAddress(currentUser.id, 1)
    private val identityKeyPair = IdentityKeyPair(
        IdentityKey(Curve.decodePoint(Base64.decode(currentUser.public_key, Base64.DEFAULT), 0)),
        Curve.decodePrivatePoint(Base64.decode(currentUser.private_key, Base64.DEFAULT))
    )

    private val protocolStore = InMemorySignalProtocolStore(identityKeyPair, currentUser.registration_id)
    private val encryptGroupCipher: GroupCipher
    private val decryptCiphers = mutableMapOf<String?, GroupCipher>()
    private val distributionStore = mutableMapOf<String?, String?>()
    private val sessionBuilder = GroupSessionBuilder(protocolStore)

    init {
        val signedPreKey = SignedPreKeyRecord(Helper.decodeToByteArray(currentUser.signed_pre_key))
        protocolStore.storeSignedPreKey(signedPreKey.id, signedPreKey)

        currentUser.pre_keys?.forEach { preKey ->
            val record = PreKeyRecord(Helper.decodeToByteArray(preKey))
            protocolStore.storePreKey(record.id, record)
        }

        if (keyRecord != null) {
            protocolStore.storeSenderKey(
                signalProtocolAddress,
                spaceDistributionId,
                SenderKeyRecord(Helper.decodeToByteArray(keyRecord))
            )
        }

        encryptGroupCipher = GroupCipher(protocolStore, signalProtocolAddress)
    }

    val keyRecord: String
        get() {
            val record = protocolStore.loadSenderKey(signalProtocolAddress, spaceDistributionId)
            return Helper.encodeToBase64(record.serialize())
        }

    fun createSession(members: List<SpaceKeyDistribution>) {
        members.forEach { member ->
            val distributionKey = Helper.decodeToByteArray(member.keyDistributionMessage)
            val keyMessage = SenderKeyDistributionMessage(distributionKey)
            val address = SignalProtocolAddress(member.userId, 1)

            if (!decryptCiphers.containsKey(member.userId) ||
                distributionStore[member.userId] != member.keyDistributionMessage
            ) {
                distributionStore.remove(member.userId)
                decryptCiphers.remove(member.userId)

                sessionBuilder.process(address, keyMessage)
                distributionStore[member.userId] = member.keyDistributionMessage
                decryptCiphers[member.userId] = GroupCipher(protocolStore, address)
            }
        }
    }

    fun encryptMessage(message: String): String {
        val encrypted = encryptGroupCipher.encrypt(
            spaceDistributionId,
            message.toByteArray(StandardCharsets.UTF_8)
        )
        return Helper.encodeToBase64(encrypted.serialize())
    }

    fun decryptMessage(encryptedMessage: String?, userId: String): String {
        val cipher = decryptCiphers[userId] ?: throw NoSessionException("No cipher for user $userId")
        val decrypted = cipher.decrypt(Helper.decodeToByteArray(encryptedMessage))
        return String(decrypted, StandardCharsets.UTF_8)
    }
}

@Keep
@JsonClass(generateAdapter = false)
data class SpaceKeyDistribution(
    val userId: String,
    val keyDistributionMessage: String
)
