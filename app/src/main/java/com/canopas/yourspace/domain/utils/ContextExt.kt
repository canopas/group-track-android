package com.canopas.yourspace.domain.utils

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.provider.Settings
import androidx.core.location.LocationManagerCompat

fun Context.isLocationServiceEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
}

fun Context.isNetWorkConnected():Boolean{
    val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
    return networkInfo?.isConnected == true
}