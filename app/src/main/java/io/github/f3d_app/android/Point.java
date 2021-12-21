package io.github.f3d_app.android;

/**
 * Provides a simple implementation of a two-dimensional point with floats.
 */
public class Point {
	private float mX, mY;

	/**
	 * Default constructor that sets the position to 0, 0.
	 */
	public Point() {
		this(0.0f, 0.0f);
	}

	/**
	 * Constructor that uses a specified position.
	 * @param xPosition The x coordinate for the point.
	 * @param yPosition The y coordinate for the point.
	 */
	public Point(final float xPosition, final float yPosition) {
		setX(xPosition);
		setY(yPosition);
	}

	/**
	 * Gets the x coordinate of the point.
	 * @return The x coordinate of the point.
	 */
	public float getX() {
		return mX;
	}

	/**
	 * Gets the y coordinate of the point.
	 * @return The y coordinate of the point.
	 */
	public float getY() {
		return mY;
	}

	/**
	 * Sets the x coordinate of the point.
	 * @param xPosition The x coordinate of the point.
	 */
	public void setX(final float xPosition) {
		mX = xPosition;
	}

	/**
	 * Sets the y coordinate of the point.
	 * @param yPosition The y coordinate of the point.
	 */
	public void setY(final float yPosition) {
		mY = yPosition;
	}
}
