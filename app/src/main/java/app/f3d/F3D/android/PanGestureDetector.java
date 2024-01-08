package app.f3d.F3D.android;

import android.view.MotionEvent;

/**
 * Provides a gesture detector to detect panning.
 * Panning is considered to be a two finger swipe.
 */
public class PanGestureDetector {
    private static final int INVALID_POINTER_ID = -1;
    private Line mPreviousLine;
    private int mPointerId1, mPointerId2;
    private float mXDistance, mYDistance;

    private final OnPanGestureListener mGestureListener;

    /**
     * Constructor.
     *
     * @param listener The gesture listener that will receive callbacks.
     */
    public PanGestureDetector(final OnPanGestureListener listener) {
        mGestureListener = listener;
        mPointerId1 = INVALID_POINTER_ID;
        mPointerId2 = INVALID_POINTER_ID;
        mXDistance = 0f;
        mYDistance = 0f;
        mPreviousLine = new Line();
    }

    /**
     * Gets the x distance of the pan.
     *
     * @return The x distance of the pan.
     */
    public float getDistanceX() {
        return mXDistance;
    }

    /**
     * Gets the y distance of the pan.
     *
     * @return The y distance of the pan.
     */
    public float getDistanceY() {
        return mYDistance;
    }

    /**
     * Processes a touch motion event and calculates the panning gesture.
     *
     * @param event The motion event that occurred.
     */
    public void onTouchEvent(final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPointerId1 = event.getPointerId(event.getActionIndex());
                mPointerId2 = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() == 2) {
                    if (mPointerId1 == INVALID_POINTER_ID) {
                        mPointerId1 = event.getPointerId(event.getActionIndex());
                    } else {
                        mPointerId2 = event.getPointerId(event.getActionIndex());
                    }

                    if (mPointerId1 != INVALID_POINTER_ID && mPointerId2 != INVALID_POINTER_ID) {
                        unpackLinePosition(event, mPreviousLine);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() == 2 && mPointerId1 != INVALID_POINTER_ID && mPointerId2 != INVALID_POINTER_ID) {
                    Line currentLine = new Line();
                    unpackLinePosition(event, currentLine);

                    updateDistanceBetweenLines(mPreviousLine, currentLine);

                    mGestureListener.onPan(this);

                    mPreviousLine = currentLine;
                }
                break;
            case MotionEvent.ACTION_UP:
                mPointerId1 = INVALID_POINTER_ID;
                mPointerId2 = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (mPointerId1 == event.getPointerId(event.getActionIndex())) {
                    mPointerId1 = INVALID_POINTER_ID;
                } else if (mPointerId2 == event.getPointerId(event.getActionIndex())) {
                    mPointerId2 = INVALID_POINTER_ID;
                }
                break;
        }
    }

    private void unpackLinePosition(final MotionEvent event, final Line line) {
        int index1 = event.findPointerIndex(mPointerId1);
        int index2 = event.findPointerIndex(mPointerId2);

        if (index1 >= 0) {
            line.setX1(event.getX(index1));
            line.setY1(event.getY(index1));
        }

        if (index2 >= 0) {
            line.setX2(event.getX(index2));
            line.setY2(event.getY(index2));
        }
    }

    private void updateDistanceBetweenLines(final Line line1, final Line line2) {
        Point center1 = line1.getCenter();
        Point center2 = line2.getCenter();

        mXDistance = center2.getX() - center1.getX();
        mYDistance = center2.getY() - center1.getY();
    }

    /**
     * Provides callbacks to process pan gestures.
     */
    public static class OnPanGestureListener {
        /**
         * Called on receipt of a motion event when a pan is detected.
         *
         * @param detector The pan gesture detector.
         */
        void onPan(PanGestureDetector detector) {
            throw new RuntimeException("Not implemented!");
        }
    }
}