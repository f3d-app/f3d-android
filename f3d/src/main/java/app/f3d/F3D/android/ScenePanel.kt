package app.f3d.F3D.android

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * A Material bottom sheet showing what the loaded scene contains: a few counters, then its
 * hierarchy as an indented tree with a visibility toggle on every node.
 */
class ScenePanel(baseContext: Context, private val view: MainView, sheet: View) :
    SheetPanel(sheet, sheet.findViewById<RecyclerView>(R.id.sceneTree)) {

    private val title: TextView = sheet.findViewById(R.id.sceneTitle)
    private val tree: RecyclerView = sheet.findViewById(R.id.sceneTree)
    private val empty: TextView = sheet.findViewById(R.id.sceneEmpty)
    private val infoLabel: TextView = sheet.findViewById(R.id.sceneInfo)

    private val res = baseContext.resources
    private val density = res.displayMetrics.density
    private val numbers = NumberFormat.getIntegerInstance()

    private var nodes: MutableList<SceneNode> = mutableListOf()

    private val adapter = NodeAdapter()

    init {
        tree.layoutManager = LinearLayoutManager(baseContext)
        tree.adapter = adapter
    }

    fun refresh() {
        view.snapshotScene { snapshot -> populate(snapshot) }
    }

    private fun populate(snapshot: SceneSnapshot) {
        nodes = snapshot.nodes.toMutableList()

        val loaded = nodes.isNotEmpty()
        title.text = (if (loaded) view.loadedFileName else null) ?: res.getString(R.string.scene)
        empty.visibility = if (loaded) View.GONE else View.VISIBLE
        infoLabel.visibility = if (loaded) View.VISIBLE else View.GONE
        if (loaded) {
            infoLabel.text = describe(snapshot.info)
        }

        rebuildRows(scrollToTop = true)
    }

    private fun describe(info: SceneInfo): String = listOf(
        count(R.plurals.scene_info_actors, info.actors.toLong()),
        count(R.plurals.scene_info_points, info.points),
        count(R.plurals.scene_info_cells, info.cells),
    ).joinToString(SEPARATOR)

    private fun count(plural: Int, value: Long): String =
        res.getQuantityString(plural, if (value == 1L) 1 else 2, numbers.format(value))

    private fun rebuildRows(scrollToTop: Boolean = false) {
        val folded = HashSet<Int>()
        val rows = ArrayList<SceneNode>(nodes.size)
        for (node in nodes) {
            if (node.parentId in folded) {
                folded.add(node.id)
                continue
            }
            rows.add(node)
            if (node.hasChildren && node.collapsed) {
                folded.add(node.id)
            }
        }
        adapter.submitList(rows) {
            if (scrollToTop) {
                tree.scrollToPosition(0)
            }
        }
    }

    private fun toggleCollapse(node: SceneNode) {
        val index = nodes.indexOfFirst { it.id == node.id }
        if (index < 0) return

        nodes[index] = node.copy(collapsed = !node.collapsed)
        rebuildRows()
    }

    private fun toggleVisibility(node: SceneNode) {
        val index = nodes.indexOfFirst { it.id == node.id }
        if (index < 0) return

        val visible = !node.visible
        view.setNodeVisibility(node.id, visible)

        var i = index
        while (i < nodes.size && (i == index || nodes[i].level > node.level)) {
            nodes[i] = nodes[i].copy(visible = visible)
            i++
        }
        rebuildRows()
    }

    private inner class NodeAdapter : ListAdapter<SceneNode, NodeHolder>(DIFF) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeHolder =
            NodeHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.scene_node_row, parent, false)
            )

        override fun onBindViewHolder(holder: NodeHolder, position: Int) =
            holder.bind(getItem(position))
    }

    private inner class NodeHolder(private val row: View) : RecyclerView.ViewHolder(row) {
        private val chevron: ImageView = row.findViewById(R.id.nodeChevron)
        private val label: TextView = row.findViewById(R.id.nodeLabel)
        private val visibility: ImageButton = row.findViewById(R.id.nodeVisibility)

        fun bind(node: SceneNode) {
            label.text = node.label
            label.alpha = if (node.visible) 1f else INACTIVE_ALPHA

            val indent = node.level.coerceAtMost(MAX_INDENT_LEVEL) * INDENT_DP
            row.setPaddingRelative(dp(ROW_PADDING_DP + indent), 0, dp(ROW_PADDING_DP), 0)

            chevron.visibility = if (node.hasChildren) View.VISIBLE else View.INVISIBLE
            chevron.rotation = if (node.collapsed) 0f else CHEVRON_OPEN_ROTATION

            visibility.setImageResource(
                if (node.visible) R.drawable.ic_baseline_visibility_24
                else R.drawable.ic_baseline_visibility_off_24
            )
            visibility.imageTintList = ColorStateList.valueOf(
                row.context.getColor(if (node.visible) R.color.white else R.color.log_debug)
            )

            row.setOnClickListener { if (node.hasChildren) toggleCollapse(node) }
            row.isClickable = node.hasChildren
            visibility.setOnClickListener { toggleVisibility(node) }
        }
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private companion object {
        const val INACTIVE_ALPHA = 0.4f
        const val ROW_PADDING_DP = 12
        const val INDENT_DP = 16
        const val MAX_INDENT_LEVEL = 8
        const val CHEVRON_OPEN_ROTATION = 90f
        const val SEPARATOR = "  \u00b7  "

        val DIFF = object : DiffUtil.ItemCallback<SceneNode>() {
            override fun areItemsTheSame(old: SceneNode, new: SceneNode) = old.id == new.id

            override fun areContentsTheSame(old: SceneNode, new: SceneNode) = old == new
        }
    }
}
