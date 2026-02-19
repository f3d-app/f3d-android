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
        when (event.getActionMasked()) {
            MotionEvent.ACTION_DOWN -> {
                mPointerId = event.getPointerId(event.getActionIndex())
                mLastTouchX = event.getX()
                mLastTouchY = event.getY()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // a second finger is down, disable rotation
                mPointerId = INVALID_POINTER_ID
            }

            MotionEvent.ACTION_MOVE -> {
                if (mPointerId != INVALID_POINTER_ID) {
                    val x = event.getX()
                    val y = event.getY()

                    // Calculate the distance moved
                    this.distanceX = x - mLastTouchX
                    this.distanceY = y - mLastTouchY

                    mLastTouchX = x
                    mLastTouchY = y

                    mGestureListener.onRotate(this)
                }
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
        private val INVALID_POINTER_ID = -1
    }
}
