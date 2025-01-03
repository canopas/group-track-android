package com.canopas.yourspace.data.storage.bufferedkeystore

import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyStore

/**
 * And extension of the normal protocol sender key store interface that has additional methods that are
 * needed in the service layer, but not the protocol layer.
 */
interface SignalServiceSenderKeyStore : SenderKeyStore {
    /**
     * @return A set of protocol addresses that have previously been sent the sender key data for the provided distributionId.
     */
    fun getSenderKeySharedWith(distributionId: DistributionId?): Set<SignalProtocolAddress?>?

    /**
     * Marks the provided addresses as having been sent the sender key data for the provided distributionId.
     */
    fun markSenderKeySharedWith(
        distributionId: DistributionId?,
        addresses: Collection<SignalProtocolAddress?>?
    )

    /**
     * Marks the provided addresses as not knowing about any distributionIds.
     */
    fun clearSenderKeySharedWith(addresses: Collection<SignalProtocolAddress?>?)
}
