package app.f3d.F3D.android

/**
 * Provides a simple implementation of a two-dimensional point with floats.
 */
class Point @JvmOverloads constructor(xPosition: Float = 0.0f, yPosition: Float = 0.0f) {

    var x: Float
    var y: Float

    init {
        this.x = xPosition
        this.y = yPosition
    }
}
