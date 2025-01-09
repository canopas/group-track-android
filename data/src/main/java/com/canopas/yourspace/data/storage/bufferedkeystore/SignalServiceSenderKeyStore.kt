package com.canopas.yourspace.data.storage.bufferedkeystore

import org.signal.libsignal.protocol.groups.state.SenderKeyStore

/**
 * And extension of the normal protocol sender key store interface that has additional methods that are
 * needed in the service layer, but not the protocol layer.
 */
interface SignalServiceSenderKeyStore : SenderKeyStore
