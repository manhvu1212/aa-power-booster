package io.github.manhvu1212.aapowerbooster

import android.app.Application
import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Custom Application that installs a global uncaught-exception handler.
 *
 * Android Auto crashes show only a generic "unexpected error" dialog and logcat is hard to read
 * in the car, so we persist the stack trace to SharedPreferences. The phone companion screen can
 * then display & copy it (see CrashLogCard in MainActivity).
 */
class PowerBoosterApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrash(this, "Uncaught on thread '${thread.name}'", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val CRASH_PREFS = "crash_prefs"
        private const val KEY_CRASH = "last_crash"
        private const val KEY_STATUS = "aa_status"
        private const val MAX_STATUS_LINES = 25

        fun saveCrash(context: Context, header: String, throwable: Throwable) {
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                // commit() (synchronous) so the trace is flushed to disk BEFORE the process dies.
                context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CRASH, "$header\n$sw")
                    .commit()
            } catch (_: Throwable) {
                // Never let the crash reporter itself crash
            }
        }

        /**
         * Breadcrumb of how far the Android Auto screen got. Written synchronously so it survives a
         * crash even when the global exception handler never fires (e.g. host-side template rejection).
         */
        fun saveStatus(context: Context, stage: String) {
            try {
                val prefs = context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                val prev = prefs.getString(KEY_STATUS, "")
                    ?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                val lines = (prev + stage).takeLast(MAX_STATUS_LINES)
                prefs.edit().putString(KEY_STATUS, lines.joinToString("\n")).commit()
            } catch (_: Throwable) {
            }
        }

        fun getCrash(context: Context): String? {
            return context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_CRASH, null)
        }

        fun getStatus(context: Context): String? {
            return context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_STATUS, null)
        }

        fun clearCrash(context: Context) {
            context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
