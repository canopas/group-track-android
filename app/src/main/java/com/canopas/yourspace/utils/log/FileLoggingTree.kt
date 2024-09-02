package com.canopas.yourspace.utils.log

import android.content.Context
import android.util.Log
import com.canopas.yourspace.data.models.location.LogEntry
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLoggingTree(val context: Context) : Timber.Tree() {

    private val maxLogSize = 5 * 1024 * 1024 // 5 MB
    private val logFile: File = File(context.filesDir, "your_space_logs.txt")
    private val database = LocationTableDatabase.getInstance(context).logDao()

    init {
        val logDir = logFile.parentFile
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs()
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.DEBUG) {
            val log = generateLog(priority, tag, message)
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            writeLog(logFile, log)
            ensureLogSize(logFile)
        }

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

    private fun generateLog(priority: Int, tag: String?, message: String): String {
        return StringBuilder().append(getCurrentTime()).append(" ")
            .append(getPriorityString(priority)).append(": ")
            .append(tag).append(" - ")
            .append(message).append('\n').toString()
    }

    private fun writeLog(logFile: File, log: String) {
        val writer = FileWriter(logFile, true)
        writer.append(log)
        writer.flush()
        writer.close()
    }

    @Throws(IOException::class)
    private fun ensureLogSize(logFile: File) {
        if (logFile.length() < maxLogSize) return

        // We remove first 25% part of logs
        val startIndex = logFile.length() / 4

        val randomAccessFile = RandomAccessFile(logFile, "r")
        randomAccessFile.seek(startIndex)

        val into = ByteArrayOutputStream()

        val buf = ByteArray(4096)
        var n: Int
        while (true) {
            n = randomAccessFile.read(buf)
            if (n < 0) break
            into.write(buf, 0, n)
        }

        randomAccessFile.close()

        val outputStream = FileOutputStream(logFile)
        into.writeTo(outputStream)

        outputStream.close()
        into.close()
    }
}