package app.f3d.F3D.android.Utils

import android.os.Handler
import android.os.Looper
import app.f3d.F3D.Log

object ConsoleLog {
    data class Entry(val id: Long, val level: Log.VerboseLevel, val message: String)

    private const val MAX_ENTRIES = 2000

    private val lock = Any()
    private val entries = ArrayDeque<Entry>()
    private var nextId = 0L
    private var installed = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var listener: (() -> Unit)? = null
    private var notifyPending = false

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
        synchronized(lock) {
            if (notifyPending || listener == null) return
            notifyPending = true
        }
        mainHandler.post {
            val callback = synchronized(lock) {
                notifyPending = false
                listener
            }
            callback?.invoke()
        }
    }
}
