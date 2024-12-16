package com.canopas.yourspace.data.security.entity

import org.signal.libsignal.protocol.SignalProtocolAddress

abstract class BaseEncryptedEntity protected constructor(
    val registrationId: Int,
    val signalProtocolAddress: SignalProtocolAddress
)
