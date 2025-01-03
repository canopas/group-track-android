package com.canopas.yourspace.data.storage.bufferedkeystore

import java.util.Objects
import java.util.UUID

/**
 * Represents the distributionId that is used to identify this group's sender key session.
 *
 * This is just a UUID, but we wrap it in order to provide some type safety and limit confusion
 * around the multiple UUIDs we throw around.
 */
class DistributionId private constructor(private val uuid: UUID) {
    /**
     * Some devices appear to have a bad UUID.toString() that misrenders an all-zero UUID as "0000-0000".
     * To account for this, we will keep our own string value, to prevent queries from going awry and such.
     */
    private var stringValue: String? = null

    init {
        if (uuid.leastSignificantBits == 0L && uuid.mostSignificantBits == 0L) {
            this.stringValue = MY_STORY_STRING
        } else {
            this.stringValue = uuid.toString()
        }
    }

    fun asUuid(): UUID {
        return uuid
    }

    override fun toString(): String {
        return stringValue!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as DistributionId
        return uuid == that.uuid
    }

    override fun hashCode(): Int {
        return Objects.hash(uuid)
    }

    companion object {
        private const val MY_STORY_STRING = "00000000-0000-0000-0000-000000000000"

        fun from(id: String?): DistributionId {
            return DistributionId(UUID.fromString(id))
        }

        fun from(uuid: UUID): DistributionId {
            return DistributionId(uuid)
        }

        fun create(): DistributionId {
            return DistributionId(UUID.randomUUID())
        }
    }
}
