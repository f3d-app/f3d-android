package app.f3d.F3D.android

import android.app.Dialog
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import app.f3d.F3D.Log
import app.f3d.F3D.android.Utils.ConsoleLog
import com.google.android.material.chip.Chip

class ConsolePanel(baseContext: Context, private val view: MainView) {
    private val context = ContextThemeWrapper(baseContext, R.style.Theme_F3D_Console)

    var onDismiss: (() -> Unit)? = null

    private var dialog: Dialog? = null
    private var scroll: ScrollView? = null
    private var text: TextView? = null
    private var empty: TextView? = null

    private var stickToBottom = true

    private val shownLevels = mutableSetOf(
        Log.VerboseLevel.ERROR,
        Log.VerboseLevel.WARN,
        Log.VerboseLevel.INFO,
        Log.VerboseLevel.DEBUG,
    )

    fun show() {
        val root = LayoutInflater.from(context).inflate(R.layout.console_panel, null)

        scroll = root.findViewById(R.id.consoleScroll)
        text = root.findViewById(R.id.consoleText)
        empty = root.findViewById(R.id.consoleEmpty)
        scroll?.setOnScrollChangeListener { _, _, _, _, _ ->
            val sv = scroll ?: return@setOnScrollChangeListener
            stickToBottom = sv.height > 0 && !sv.canScrollVertically(1)
        }
        bindFilters(root)

        root.findViewById<ImageButton>(R.id.consoleCloseButton).setOnClickListener { dismiss() }
        root.findViewById<ImageButton>(R.id.consoleClearButton).setOnClickListener { ConsoleLog.clear() }

        val input = root.findViewById<AutoCompleteTextView>(R.id.consoleInput)
        root.findViewById<ImageButton>(R.id.consoleSendButton).setOnClickListener { submit(input) }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit(input)
                true
            } else {
                false
            }
        }
        view.snapshotCommandActions { actions ->
            input.setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, actions))
        }

        ConsoleLog.setListener { refresh() }

        dialog = Dialog(context, R.style.Theme_F3D_Console).apply {
            setContentView(root)
            window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            setOnDismissListener {
                ConsoleLog.setListener(null)
                onDismiss?.invoke()
            }
            show()
        }

        refresh()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }

    private fun submit(input: AutoCompleteTextView) {
        val command = input.text.toString().trim()
        if (command.isEmpty()) return
        view.triggerCommand(command)
        input.text.clear()
        stickToBottom = true
    }

    private fun bindFilters(root: View) {
        val chips = mapOf(
            R.id.filterError to Log.VerboseLevel.ERROR,
            R.id.filterWarning to Log.VerboseLevel.WARN,
            R.id.filterInfo to Log.VerboseLevel.INFO,
            R.id.filterDebug to Log.VerboseLevel.DEBUG,
        )
        chips.forEach { (id, level) ->
            root.findViewById<Chip>(id).apply {
                isChecked = level in shownLevels
                setOnCheckedChangeListener { _, checked ->
                    if (checked) shownLevels.add(level) else shownLevels.remove(level)
                    refresh(keepScrollPosition = true)
                }
            }
        }
    }

    private fun refresh(keepScrollPosition: Boolean = false) {
        val all = ConsoleLog.snapshot()
        val entries = all.filter { it.level in shownLevels }
        empty?.apply {
            visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            setText(if (all.isEmpty()) R.string.console_empty else R.string.console_empty_filtered)
        }

        val sv = scroll ?: return
        val followTail = !keepScrollPosition && stickToBottom
        text?.text = render(entries)
        if (followTail) {
            sv.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    sv.viewTreeObserver.removeOnPreDrawListener(this)
                    sv.scrollTo(0, text?.height ?: 0)
                    return true
                }
            })
        }
    }

    private fun render(entries: List<ConsoleLog.Entry>): CharSequence {
        val out = SpannableStringBuilder()
        entries.forEachIndexed { index, entry ->
            val start = out.length
            out.append(entry.message)
            if (index < entries.lastIndex) {
                out.append('\n')
            }
            out.setSpan(
                ForegroundColorSpan(context.getColor(colorFor(entry.level))),
                start,
                out.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        return out
    }

    private fun colorFor(level: Log.VerboseLevel): Int = when (level) {
        Log.VerboseLevel.ERROR -> R.color.red
        Log.VerboseLevel.WARN -> R.color.yellow
        Log.VerboseLevel.DEBUG -> R.color.log_debug
        else -> R.color.log_info
    }
}
