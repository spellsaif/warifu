package org.warifu

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class WarifuApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("WarifuCrash", "FATAL UNCAUGHT EXCEPTION on thread ${thread.name}", throwable)
            try {
                val crashFile = File(filesDir, "last_crash.txt")
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                crashFile.writeText(sw.toString())
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
