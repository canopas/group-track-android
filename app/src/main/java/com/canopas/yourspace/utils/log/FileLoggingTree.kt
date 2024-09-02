package com.canopas.yourspace.utils.log

import android.content.Context
import android.util.Log
import com.canopas.yourspace.data.models.location.LogEntry
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(val context: Context) : Timber.Tree() {

    private val database = LocationTableDatabase.getInstance(context).logDao()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch {
                val logEntry = LogEntry(
                    message = message,
                    priority = getPriorityString(priority),
                    tag = tag,
                    timestamp = getCurrentTime()
                )
                database.insert(logEntry)
            }
        }
    }

    private fun getPriorityString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "VERBOSE"
            Log.DEBUG -> "DEBUG"
            Log.INFO -> "INFO"
            Log.WARN -> "WARN"
            Log.ERROR -> "ERROR"
            Log.ASSERT -> "ASSERT"
            else -> "UNKNOWN"
        }
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }
}
