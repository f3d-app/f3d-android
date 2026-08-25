package app.f3d.F3D.android

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * Shared plumbing of the bottom sheets hosted in the activity layout.
 */
abstract class SheetPanel(protected val sheet: View, private val scroll: View) {

    protected val behavior: BottomSheetBehavior<View> = BottomSheetBehavior.from(sheet)

    val isOpen: Boolean
        get() = behavior.state != BottomSheetBehavior.STATE_HIDDEN

    /** How far the sheet is out, from -1 when fully hidden to 0 once it is at least collapsed. */
    var slideOffset: Float = -1f
        private set

    var onSlideOffset: ((Float) -> Unit)? = null

    init {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                publishSlideOffset(if (newState == BottomSheetBehavior.STATE_HIDDEN) -1f else 0f)
                bottomSheet.post { padScrollPastClip(bottomSheet) }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                publishSlideOffset(slideOffset)
                padScrollPastClip(bottomSheet)
            }
        })
    }

    private fun publishSlideOffset(offset: Float) {
        slideOffset = offset
        onSlideOffset?.invoke(offset)
    }

    open fun show() = openSheet()

    protected fun openSheet() {
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun dismiss() {
        behavior.state = BottomSheetBehavior.STATE_HIDDEN
    }

    protected fun fadeSheet(alpha: Float) {
        sheet.animate().alpha(alpha).setDuration(FADE_DURATION_MS).start()
    }

    private fun padScrollPastClip(sheet: View) {
        val clipped = (sheet.bottom - (sheet.parent as View).height).coerceAtLeast(0)
        if (scroll.paddingBottom != clipped) {
            scroll.setPadding(0, 0, 0, clipped)
        }
    }

    protected companion object {
        const val FADE_DURATION_MS = 120L
    }
}
