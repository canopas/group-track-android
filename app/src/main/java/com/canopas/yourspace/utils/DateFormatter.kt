package com.canopas.yourspace.utils

import android.content.Context
import com.canopas.yourspace.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.formattedTimeAgoString(context: Context): String {
    val now = System.currentTimeMillis()
    val elapsedTime = now - this
    return when {
        elapsedTime < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.map_user_item_location_updated_now)
        elapsedTime < TimeUnit.HOURS.toMillis(1) -> context.getString(
            R.string.map_user_item_location_updated_minutes_ago,
            "${TimeUnit.MILLISECONDS.toMinutes(elapsedTime)}"
        )

        elapsedTime < TimeUnit.DAYS.toMillis(1) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))

        else -> SimpleDateFormat("h:mm a • d MMM", Locale.getDefault()).format(Date(this))
    }
}

fun Long.formattedMessageTimeString(context: Context): String {
    val now = System.currentTimeMillis()
    val elapsedTime = now - this
    return when {
        elapsedTime < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.map_user_item_location_updated_now)
        elapsedTime < TimeUnit.HOURS.toMillis(1) -> context.getString(
            R.string.messages_time_format_minutes,
            "${TimeUnit.MILLISECONDS.toMinutes(elapsedTime)}"
        )

        elapsedTime < TimeUnit.DAYS.toMillis(1) ->
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(this))

        else -> SimpleDateFormat("h:mm a • d MMM", Locale.getDefault()).format(Date(this))
    }
}