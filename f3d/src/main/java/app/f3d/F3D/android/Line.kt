package app.f3d.F3D.android

/**
 * Provides a simple implementation of a line with two points.
 */
class Line @JvmOverloads constructor(firstPoint: Point = Point(), secondPoint: Point = Point()) {
    var p1: Point
    var p2: Point

    /**
     * Constructor that uses specified points.
     * @param firstPoint The first point of the line.
     * @param secondPoint The second point of the line.
     */
    init {
        this.p1 = firstPoint
        this.p2 = secondPoint
    }

    val center: Point
        /**
         * Calculates the center point of the line.
         * @return A point representing the center of the line.
         */
        get() = Point((this.p1.x + this.p2.x) / 2, (this.p1.y + this.p2.y) / 2)
}
