package com.canopas.catchme.utils

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import java.io.IOException
import java.util.Locale

fun LatLng.getAddress(context: Context): String? {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addressList = geocoder.getFromLocation(latitude, longitude, 1)
        if (addressList != null && addressList.size > 0) {
            val address = addressList[0]
            return address.getAddressLine(0)
        }
    } catch (e: IOException) {
        Timber.e(e)
    }
    return null
}