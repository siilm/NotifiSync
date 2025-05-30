package com.deaworks.notifisync.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("unused")
object LogSaver {
    fun e(tag: String, msg: String, context: Context, e: Exception) {
        Log.e(tag, msg)
        saveToFile(tag, msg, context)
    }

    fun e(tag: String, msg: String, context: Context) {
        Log.e(tag, msg)
        saveToFile(tag, msg, context)
    }

    fun i(tag: String, msg: String, context: Context) {
        Log.i(tag, msg)
        saveToFile(tag, msg, context)
    }

    fun d(tag: String, msg: String, context: Context) {
        Log.d(tag, msg)
        saveToFile(tag, msg, context)
    }
}

private fun saveToFile(tag: String, msg: String, context: Context) {
    try {
        val logFile = File(
            context.cacheDir,
            "app_log.log"
        )
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HH:mm:ss_SSS")
        val formattedTime = dateTimeFormatter.format(LocalDateTime.now())

        val logMessage = "$formattedTime -> $tag:\n$msg\n"
        logFile.appendText(logMessage)
    } catch (e: IOException) {
        e.printStackTrace()
    }
}