package app.f3d.F3D.android

import android.view.MotionEvent

/**
 * Provides a gesture detector to detect panning.
 * Panning is considered to be a two finger swipe.
 */
class PanGestureDetector(private val mGestureListener: OnPanGestureListener) {
    private var mPreviousLine: Line
    private var mPointerId1: Int
    private var mPointerId2: Int

    /**
     * Gets the x distance of the pan.
     *
     * @return The x distance of the pan.
     */
    var distanceX: Float = 0f
        private set

    /**
     * Gets the y distance of the pan.
     *
     * @return The y distance of the pan.
     */
    var distanceY: Float = 0f
        private set

    /**
     * Constructor.
     *
     * @param mGestureListener The gesture listener that will receive callbacks.
     */
    init {
        mPointerId1 = INVALID_POINTER_ID
        mPointerId2 = INVALID_POINTER_ID
        mPreviousLine = Line()
    }

    /**
     * Processes a touch motion event and calculates the panning gesture.
     *
     * @param event The motion event that occurred.
     */
    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPointerId1 = event.getPointerId(event.actionIndex)
                mPointerId2 = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) {
                if (mPointerId1 == INVALID_POINTER_ID) {
                    mPointerId1 = event.getPointerId(event.actionIndex)
                } else {
                    mPointerId2 = event.getPointerId(event.actionIndex)
                }

                if (mPointerId1 != INVALID_POINTER_ID && mPointerId2 != INVALID_POINTER_ID) {
                    unpackLinePosition(event, mPreviousLine)
                }
            }

            MotionEvent.ACTION_MOVE -> if (event.pointerCount == 2 && mPointerId1 != INVALID_POINTER_ID && mPointerId2 != INVALID_POINTER_ID) {
                val currentLine = Line()
                unpackLinePosition(event, currentLine)

                updateDistanceBetweenLines(mPreviousLine, currentLine)

                mGestureListener.onPan(this)

                mPreviousLine = currentLine
            }

            MotionEvent.ACTION_UP -> {
                mPointerId1 = INVALID_POINTER_ID
                mPointerId2 = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_POINTER_UP -> if (mPointerId1 == event.getPointerId(event.actionIndex)) {
                mPointerId1 = INVALID_POINTER_ID
            } else if (mPointerId2 == event.getPointerId(event.actionIndex)) {
                mPointerId2 = INVALID_POINTER_ID
            }
        }
    }

    private fun unpackLinePosition(event: MotionEvent, line: Line) {
        val index1 = event.findPointerIndex(mPointerId1)
        val index2 = event.findPointerIndex(mPointerId2)

        if (index1 >= 0) {
            line.p1.x = event.getX(index1)
            line.p1.y = event.getY(index1)
        }

        if (index2 >= 0) {
            line.p2.x = event.getX(index2)
            line.p2.y = event.getY(index2)
        }
    }

    private fun updateDistanceBetweenLines(line1: Line, line2: Line) {
        val center1 = line1.center
        val center2 = line2.center

        this.distanceX = center2.x - center1.x
        this.distanceY = center2.y - center1.y
    }

    /**
     * Provides callbacks to process pan gestures.
     */
    open class OnPanGestureListener {
        /**
         * Called on receipt of a motion event when a pan is detected.
         *
         * @param detector The pan gesture detector.
         */
        open fun onPan(detector: PanGestureDetector) {
            throw RuntimeException("Not implemented!")
        }
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }
}
