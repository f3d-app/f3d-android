package io.github.f3d_app.android;

import android.view.MotionEvent;

/**
 * Provides a gesture detector to detect rotation.
 */
public class RotateGestureDetector {
    private static final int INVALID_POINTER_ID = -1;
    private int mPointerId;
    private float mLastTouchX, mLastTouchY;
    private float mDistanceX, mDistanceY;

    private OnRotateGestureListener mGestureListener;

    /**
     * Constructor.
     *
     * @param listener The gesture listener that will receive callbacks.
     */
    public RotateGestureDetector(final OnRotateGestureListener listener) {
        mGestureListener = listener;
        mPointerId = INVALID_POINTER_ID;
        mLastTouchX = 0f;
        mLastTouchY = 0f;
        mDistanceX = 0f;
        mDistanceY = 0f;
    }

    /**
     * Gets the x distance of the swipe.
     *
     * @return The x distance of the swipe.
     */
    public float getDistanceX() {
        return mDistanceX;
    }

    /**
     * Gets the y distance of the swipe.
     *
     * @return The y distance of the swipe.
     */
    public float getDistanceY() {
        return mDistanceY;
    }

    /**
     * Processes a touch motion event and calculates the rotation gesture.
     *
     * @param event The motion event that occurred.
     * @return Whether the touch was consumed.
     */
    public boolean onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mPointerId = event.getPointerId(event.getActionIndex());
                mLastTouchX = event.getX();
                mLastTouchY = event.getY();
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                // a second finger is down, disable rotation
                mPointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (mPointerId != INVALID_POINTER_ID) {
                    float x = event.getX();
                    float y = event.getY();

                    // Calculate the distance moved
                    mDistanceX = x - mLastTouchX;
                    mDistanceY = y - mLastTouchY;

                    mLastTouchX = x;
                    mLastTouchY = y;

                    mGestureListener.onRotate(this);
                }
                break;
            }
        }

        return true;
    }

    /**
     * Provides callbacks to process pan gestures.
     */
    public static class OnRotateGestureListener {
        /**
         * Called on receipt of a motion event when a pan is detected.
         *
         * @param detector The pan gesture detector.
         * @return Whether the touch was consumed.
         */
        boolean onRotate(RotateGestureDetector detector) {
            throw new RuntimeException("Not implemented!");
        }
    }
}