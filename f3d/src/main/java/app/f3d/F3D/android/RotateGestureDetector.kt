package app.f3d.F3D.android

import android.view.MotionEvent

/**
 * Provides a gesture detector to detect rotation.
 */
class RotateGestureDetector(private val mGestureListener: OnRotateGestureListener) {
    private var mPointerId: Int
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f

    /**
     * Gets the x distance of the swipe.
     *
     * @return The x distance of the swipe.
     */
    var distanceX: Float = 0f
        private set

    /**
     * Gets the y distance of the swipe.
     *
     * @return The y distance of the swipe.
     */
    var distanceY: Float = 0f
        private set

    /**
     * Constructor.
     *
     * @param mGestureListener The gesture listener that will receive callbacks.
     */
    init {
        mPointerId = INVALID_POINTER_ID
    }

    /**
     * Processes a touch motion event and calculates the rotation gesture.
     *
     * @param event The motion event that occurred.
     */
    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPointerId = event.getPointerId(event.actionIndex)
                mLastTouchX = event.x
                mLastTouchY = event.y
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // a second finger is down, disable rotation
                mPointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_MOVE -> {
                if (mPointerId != INVALID_POINTER_ID) {
                    val pointerIndex = event.findPointerIndex(mPointerId)
                    if (pointerIndex < 0) return

                    val x = event.getX(pointerIndex)
                    val y = event.getY(pointerIndex)

                    // Calculate the distance moved
                    this.distanceX = x - mLastTouchX
                    this.distanceY = y - mLastTouchY

                    mLastTouchX = x
                    mLastTouchY = y

                    mGestureListener.onRotate(this)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mPointerId = INVALID_POINTER_ID
            }
        }
    }

    /**
     * Provides callbacks to process pan gestures.
     */
    open class OnRotateGestureListener {
        /**
         * Called on receipt of a motion event when a pan is detected.
         *
         * @param detector The pan gesture detector.
         */
        open fun onRotate(detector: RotateGestureDetector) {
            throw RuntimeException("Not implemented!")
        }
    }

    companion object {
        private const val INVALID_POINTER_ID = -1
    }
}
