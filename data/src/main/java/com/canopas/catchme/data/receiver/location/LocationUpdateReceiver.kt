package com.canopas.catchme.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import timber.log.Timber

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"
class LocationUpdateReceiver :BroadcastReceiver(){

    override fun onReceive(context: Context, intent: Intent) {

        Timber.d("XXX onReceive result:${LocationResult.extractResult(intent)}")

    }
}