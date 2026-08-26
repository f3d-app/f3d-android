package app.f3d.F3D.android

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.f3d.F3D.Log
import app.f3d.F3D.android.Utils.ConsoleLog
import com.google.android.material.chip.Chip

class ConsolePanel(baseContext: Context, private val view: MainView) {
    private val context = ContextThemeWrapper(baseContext, R.style.Theme_F3D_Console)

    var onDismiss: (() -> Unit)? = null

    private var dialog: Dialog? = null
    private var layoutManager: LinearLayoutManager? = null
    private var empty: TextView? = null

    private val adapter = LogAdapter()

    private var stickToBottom = true

    private val shownLevels = mutableSetOf(
        Log.VerboseLevel.ERROR,
        Log.VerboseLevel.WARN,
        Log.VerboseLevel.INFO,
        Log.VerboseLevel.DEBUG,
    )

    fun show() {
        val root = LayoutInflater.from(context).inflate(R.layout.console_panel, null)

        empty = root.findViewById(R.id.consoleEmpty)
        layoutManager = LinearLayoutManager(context)
        root.findViewById<RecyclerView>(R.id.consoleList).apply {
            layoutManager = this@ConsolePanel.layoutManager
            adapter = this@ConsolePanel.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(list: RecyclerView, dx: Int, dy: Int) {
                    stickToBottom = !list.canScrollVertically(1)
                }
            })
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

        val followTail = !keepScrollPosition && stickToBottom
        adapter.submit(entries)
        if (followTail && entries.isNotEmpty()) {
            layoutManager?.scrollToPositionWithOffset(entries.lastIndex, 0)
        }
    }

    private inner class LogAdapter : RecyclerView.Adapter<LineHolder>() {
        private var entries: List<ConsoleLog.Entry> = emptyList()

        fun submit(newEntries: List<ConsoleLog.Entry>) {
            entries = newEntries
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = entries.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LineHolder =
            LineHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.console_log_row, parent, false) as TextView
            )

        override fun onBindViewHolder(holder: LineHolder, position: Int) =
            holder.bind(entries[position])
    }

    private inner class LineHolder(private val line: TextView) : RecyclerView.ViewHolder(line) {
        fun bind(entry: ConsoleLog.Entry) {
            line.text = entry.message
            line.setTextColor(context.getColor(colorFor(entry.level)))
        }
    }

    private fun colorFor(level: Log.VerboseLevel): Int = when (level) {
        Log.VerboseLevel.ERROR -> R.color.red
        Log.VerboseLevel.WARN -> R.color.yellow
        Log.VerboseLevel.DEBUG -> R.color.log_debug
        else -> R.color.log_info
    }
}
