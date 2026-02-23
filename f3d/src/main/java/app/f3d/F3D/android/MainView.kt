package app.f3d.F3D.android

import android.content.Context
import android.net.Uri
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import app.f3d.F3D.Engine
import app.f3d.F3D.android.PanGestureDetector.OnPanGestureListener
import app.f3d.F3D.android.RotateGestureDetector.OnRotateGestureListener
import java.io.IOException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainView(context: Context) : GLSurfaceView(context) {
    private var mEngine: Engine? = null

    private val mScaleDetector: ScaleGestureDetector
    private val mPanDetector: PanGestureDetector
    private val mRotateDetector: RotateGestureDetector
    private var mActiveUri: Uri? = null

    init {
        start()

        this.mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        this.mPanDetector = PanGestureDetector(PanListener())
        this.mRotateDetector = RotateGestureDetector(RotateListener())
    }

    fun start() {
        setEGLConfigChooser(8, 8, 8, 0, 16, 0)
        setEGLContextClientVersion(3)

        this.setRenderer(Renderer())
        this.renderMode = RENDERMODE_WHEN_DIRTY
    }

    private inner class Renderer : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: GL10?) {
            this@MainView.mEngine!!.getWindow().render()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this@MainView.mEngine!!.getWindow().setSize(width, height)
            this@MainView.requestRender()
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            Engine.autoloadPlugins()

            this@MainView.mEngine = Engine.createExternalEGL()

            this@MainView.mEngine!!.setCachePath(
                this@MainView.context.cacheDir.absolutePath
            )

            this@MainView.mEngine!!.options.toggle("ui.axis")
            this@MainView.mEngine!!.options.toggle("render.grid.enable")
            this@MainView.mEngine!!.options.toggle("render.effect.antialiasing.enable")
            this@MainView.mEngine!!.options.toggle("render.effect.tone_mapping")
            this@MainView.mEngine!!.options.toggle("render.hdri.ambient")
            this@MainView.mEngine!!.options.toggle("ui.filename")
            this@MainView.mEngine!!.options.toggle("ui.loader_progress")

            val activeUri = mActiveUri
            if (activeUri != null) {
                try {
                    this@MainView.context.contentResolver.openInputStream(activeUri)
                        .use { inputStream ->
                            if (inputStream != null) {
                                val fileBytes = ByteArray(inputStream.available())
                                inputStream.read(fileBytes)

                                this@MainView.mEngine!!.scene.add(fileBytes)
                            }
                        }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateActiveUri(uri: Uri?) {
        // Use the new file path as needed in MainView
        mActiveUri = uri
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            this@MainView.mEngine!!.window.camera
                .dolly(detector.getScaleFactor().toDouble())
            this@MainView.requestRender()
            return true
        }
    }

    private inner class PanListener : OnPanGestureListener() {
        public override fun onPan(detector: PanGestureDetector) {
            val window = this@MainView.mEngine!!.window
            val camera = window.camera

            val pos = camera.position
            val focus = camera.focalPoint
            val focusDC = window.getDisplayFromWorld(focus)

            val shiftDC = doubleArrayOf(
                focusDC[0] - detector.distanceX,
                focusDC[1] + detector.distanceY,
                focusDC[2]
            )
            val shift = window.getWorldFromDisplay(shiftDC)

            val motion =
                doubleArrayOf(shift[0] - focus[0], shift[1] - focus[1], shift[2] - focus[2])

            camera.focalPoint = doubleArrayOf(
                motion[0] + focus[0],
                motion[1] + focus[1],
                motion[2] + focus[2]
            )
            camera.position = doubleArrayOf(
                motion[0] + pos[0],
                motion[1] + pos[1],
                motion[2] + pos[2]
            )

            this@MainView.requestRender()
        }
    }

    private inner class RotateListener : OnRotateGestureListener() {
        public override fun onRotate(detector: RotateGestureDetector) {
            val window = this@MainView.mEngine!!.window
            val camera = window.camera

            val deltaElevation = 200.0 / window.width
            val deltaAzimuth = -200.0 / window.height

            camera.azimuth(detector.distanceX * deltaAzimuth)
            camera.elevation(detector.distanceY * deltaElevation)

            this@MainView.requestRender()
        }
    }

    // forward events to rendering thread for it to handle
    override fun onTouchEvent(event: MotionEvent): Boolean {
        queueEvent(Runnable {
            mPanDetector.onTouchEvent(event)
            mScaleDetector.onTouchEvent(event)
            mRotateDetector.onTouchEvent(event)
        }
        )

        return true
    }
}
