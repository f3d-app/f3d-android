package app.f3d.F3D.android;

/**
 * Provides a simple implementation of a line with two points.
 */
public class Line {
	private Point mPoint1, mPoint2;

	/**
	 * Default constructor that uses two default points.
	 */
	public Line() {
		this(new Point(), new Point());
	}

	/**
	 * Constructor that uses specified points.
	 * @param firstPoint The first point of the line.
	 * @param secondPoint The second point of the line.
	 */
	public Line(final Point firstPoint, final Point secondPoint) {
		setPoint1(firstPoint);
		setPoint2(secondPoint);
	}

	/**
	 * Calculates the center point of the line.
	 * @return A point representing the center of the line.
	 */
	public Point getCenter() {
		return new Point((getX1() + getX2()) / 2, (getY1() + getY2()) / 2);
	}

	/**
	 * Sets the first point of the line.
	 * @param firstPoint The first point of the line.
	 */
	public void setPoint1(final Point firstPoint) {
		mPoint1 = firstPoint;
	}

	/**
	 * Sets the second point of the line.
	 * @param secondPoint The second point of the line.
	 */
	public void setPoint2(final Point secondPoint) {
		mPoint2 = secondPoint;
	}

	/**
	 * Gets the x coordinate of the first point.
	 * @return The x coordinate of the first point.
	 */
	public float getX1() {
		return mPoint1.getX();
	}

	/**
	 * Gets the y coordinate of the first point.
	 * @return The y coordinate of the first point.
	 */
	public float getY1() {
		return mPoint1.getY();
	}

	/**
	 * Gets the x coordinate of the second point.
	 * @return The x coordinate of the second point.
	 */
	public float getX2() {
		return mPoint2.getX();
	}

	/**
	 * Gets the y coordinate of the second point.
	 * @return The y coordinate of the second point.
	 */
	public float getY2() {
		return mPoint2.getY();
	}

	/**
	 * Sets the x coordinate of the first point.
	 * @param x1 The x coordinate of the first point.
	 */
	public void setX1(final float x1) {
		mPoint1.setX(x1);
	}

	/**
	 * Sets the y coordinate of the first point.
	 * @param y1 The y coordinate of the first point.
	 */
	public void setY1(final float y1) {
		mPoint1.setY(y1);
	}

	/**
	 * Sets the x coordinate of the second point.
	 * @param x2 The x coordinate of the second point.
	 */
	public void setX2(final float x2) {
		mPoint2.setX(x2);
	}

	/**
	 * Sets the y coordinate of the second point.
	 * @param y2 The y coordinate of the second point.
	 */
	public void setY2(final float y2) {
		mPoint2.setY(y2);
	}
}
