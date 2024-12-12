package com.canopas.yourspace.data.security.helper

import android.util.Base64

object Helper {
    fun encodeToBase64(value: ByteArray?): String {
        return Base64.encodeToString(value, Base64.NO_WRAP)
    }

    fun decodeToByteArray(base64: String?): ByteArray {
        return Base64.decode(base64, Base64.NO_WRAP)
    }
}
