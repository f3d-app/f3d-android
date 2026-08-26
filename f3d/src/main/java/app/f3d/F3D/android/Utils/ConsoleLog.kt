package app.f3d.F3D.android.Utils

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import app.f3d.F3D.Log

object ConsoleLog {
    data class Entry(val id: Long, val level: Log.VerboseLevel, val message: String)

    private const val MAX_ENTRIES = 2000
    private const val MIN_NOTIFY_INTERVAL_MS = 200L
    private val lock = Any()
    private val entries = ArrayDeque<Entry>()
    private var nextId = 0L
    private var installed = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: (() -> Unit)? = null
    private var notifyPending = false
    private var lastNotifyMs = 0L

    /** Must be called before any libf3d call whose output matters, e.g. plugin autoloading. */
    fun install() {
        synchronized(lock) {
            if (installed) return
            installed = true
        }
        Log.forward { level, message -> append(level, message) }
    }

    fun snapshot(): List<Entry> = synchronized(lock) { entries.toList() }

    fun setListener(callback: (() -> Unit)?) {
        synchronized(lock) { listener = callback }
    }

    fun clear() {
        synchronized(lock) { entries.clear() }
        notifyChanged()
    }

    private fun append(level: Log.VerboseLevel, message: String) {
        synchronized(lock) {
            entries.addLast(Entry(nextId++, level, message))
            while (entries.size > MAX_ENTRIES) {
                entries.removeFirst()
            }
        }
        notifyChanged()
    }

    private fun notifyChanged() {
        val delay: Long
        synchronized(lock) {
            if (notifyPending || listener == null) return
            notifyPending = true
            delay = (MIN_NOTIFY_INTERVAL_MS - (SystemClock.uptimeMillis() - lastNotifyMs))
                .coerceIn(0L, MIN_NOTIFY_INTERVAL_MS)
        }
        mainHandler.postDelayed({
            val callback = synchronized(lock) {
                notifyPending = false
                lastNotifyMs = SystemClock.uptimeMillis()
                listener
            }
            callback?.invoke()
        }, delay)
    }
}
