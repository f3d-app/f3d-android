package app.f3d.F3D.android

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import app.f3d.F3D.Engine
import app.f3d.F3D.Image
import app.f3d.F3D.Log
import app.f3d.F3D.Options
import app.f3d.F3D.android.Utils.OptionSpec
import app.f3d.F3D.android.Utils.OptionWidget
import app.f3d.F3D.android.PanGestureDetector.OnPanGestureListener
import app.f3d.F3D.android.RotateGestureDetector.OnRotateGestureListener
import com.google.android.material.snackbar.Snackbar
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainView(context: Context) : GLSurfaceView(context) {
    private var mEngine: Engine? = null

    private val mScaleDetector: ScaleGestureDetector
    private val mPanDetector: PanGestureDetector
    private val mRotateDetector: RotateGestureDetector
    private var mActiveUri: Uri? = null

    var last = System.nanoTime()

    init {
        start()

        copyAssetFolder("usd", context.filesDir.absolutePath + "/usd")

        this.mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        this.mPanDetector = PanGestureDetector(PanListener())
        this.mRotateDetector = RotateGestureDetector(RotateListener())
    }

    fun start() {
        setEGLConfigChooser(8, 8, 8, 0, 16, 0)
        setEGLContextClientVersion(3)
        preserveEGLContextOnPause = true

        this.setRenderer(Renderer())
        this.renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun loadFile() {
        if (mActiveUri != null) {
            try {
                this@MainView.context.contentResolver.openInputStream(mActiveUri!!)
                    .use { inputStream ->
                        if (inputStream != null) {
                            val fileBytes = ByteArray(inputStream.available())
                            inputStream.read(fileBytes)

                            this@MainView.mEngine!!.scene.clear()
                            this@MainView.mEngine!!.scene.add(fileBytes)
                            this@MainView.mEngine!!.window.camera.resetToBounds()
                            mActiveUri = null
                        }
                    }
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Snackbar.make(this@MainView, "Failed to load file: " + e.message, Snackbar.LENGTH_SHORT).apply {
                        view.setBackgroundColor(context.getColor(R.color.white))
                        setTextColor(context.getColor(R.color.black))
                    }.show()
                }
                e.printStackTrace()
            }
        }
    }

    private inner class Renderer : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: GL10?) {
            this@MainView.loadFile()

            val now = System.nanoTime()
            val dt = (now - last) / 1e6
            last = now

            Log.debug("frame dt = $dt ms")

            this@MainView.mEngine!!.window.render()
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            this@MainView.mEngine!!.window.setSize(width, height)
            this@MainView.requestRender()
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

            Log.setVerboseLevel(Log.VerboseLevel.DEBUG)
            Engine.autoloadPlugins()

            Engine.setReaderOption("USD.resources_path", context.filesDir.absolutePath + "/usd")

            this@MainView.mEngine = Engine.createExternalEGL()

            this@MainView.mEngine!!.setCachePath(
                this@MainView.context.cacheDir.absolutePath
            )

            applySceneDefaults(this@MainView.mEngine!!.options)
            this@MainView.mEngine!!.options.toggle("ui.loader_progress")

            this@MainView.requestRender()
        }
    }

    private fun copyAssetFolder(assetPath: String, destPath: String) {
        val children = context.assets.list(assetPath)
        if (children.isNullOrEmpty()) {
            context.assets.open(assetPath).use { input ->
                java.io.File(destPath).outputStream().use { input.copyTo(it) }
            }
        } else {
            java.io.File(destPath).mkdirs()
            children.forEach { child -> copyAssetFolder("$assetPath/$child", "$destPath/$child") }
        }
    }

    fun updateActiveUri(uri: Uri?) {
        // Use the new file path as needed in MainView
        mActiveUri = uri
    }

    fun renderToImage(): Image {
        return mEngine!!.window.renderToImage()
    }

    fun snapshotOptions(specs: List<OptionSpec>, onReady: (List<OptionWidget>) -> Unit) {
        val engine = mEngine
        if (engine == null) {
            onReady(emptyList())
            return
        }
        queueEvent {
            val widgets = specs.mapNotNull { resolveWidget(engine.options, it) }
            post { onReady(widgets) }
        }
    }

    fun resetOptions(specs: List<OptionSpec>, onReady: (List<OptionWidget>) -> Unit) {
        val engine = mEngine
        if (engine == null) {
            onReady(emptyList())
            return
        }
        queueEvent {
            specs.forEach { spec ->
                try {
                    engine.options.reset(spec.name)
                } catch (_: Exception) {
                }
            }
            applySceneDefaults(engine.options)
            requestRender()
            val widgets = specs.mapNotNull { resolveWidget(engine.options, it) }
            post { onReady(widgets) }
        }
    }

    // Scene options the app enables on top of the libf3d defaults; re-applied after a reset.
    private fun applySceneDefaults(options: Options) {
        options.setAsBool("render.grid.enable", true)
        options.setAsBool("render.effect.tone_mapping", true)
        options.setAsBool("render.hdri.ambient", true)
    }

    private fun resolveWidget(opts: Options, spec: OptionSpec): OptionWidget? = try {
        val isSet = opts.hasValue(spec.name)
        when (opts.getType(spec.name)) {
            Options.OptionType.BOOL ->
                OptionWidget.Toggle(spec, if (isSet) opts.getAsBool(spec.name) else false, isSet)
            Options.OptionType.COLOR ->
                OptionWidget.Color(
                    spec,
                    if (isSet) opts.getAsDoubleVector(spec.name)
                    else doubleArrayOf(0.0, 0.0, 0.0),
                    isSet
                )
            Options.OptionType.STRING ->
                if (opts.hasDomain(spec.name) && opts.getDomainStyle(spec.name) == Options.DomainStyle.ENUM) {
                    OptionWidget.Enum(
                        spec,
                        opts.getEnumDomain(spec.name),
                        if (isSet) opts.getAsStringRepresentation(spec.name) else "",
                        isSet
                    )
                } else {
                    null
                }
            Options.OptionType.DIRECTION ->
                OptionWidget.Enum(
                    spec,
                    listOf("+Y", "+Z"),
                    if (isSet) opts.getAsStringRepresentation(spec.name) else "",
                    isSet
                )
            Options.OptionType.DOUBLE, Options.OptionType.RATIO ->
                if (opts.hasDomain(spec.name) && opts.getDomainStyle(spec.name) == Options.DomainStyle.RANGE) {
                    val range = opts.getRangeDomainAsDouble(spec.name)
                    OptionWidget.Range(
                        spec,
                        range.min,
                        range.max,
                        range.increment,
                        if (isSet) opts.getAsDouble(spec.name) else range.min,
                        isSet
                    )
                } else {
                    null
                }
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    fun applyOption(action: (Options) -> Unit) {
        queueEvent {
            mEngine?.let {
                action(it.options)
                requestRender()
            }
        }
    }

    fun rotateCamera(azimuth: Double, elevation: Double) {
        val window = mEngine!!.window
        val camera = window.camera

        camera.azimuth(azimuth)
        camera.elevation(elevation)

        requestRender()
    }

    private inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            this@MainView.mEngine!!.window.camera
                .dolly(detector.getScaleFactor().toDouble())
            return true
        }
    }

    private inner class PanListener : OnPanGestureListener() {
        override fun onPan(detector: PanGestureDetector) {
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
        }
    }

    private inner class RotateListener : OnRotateGestureListener() {
        override fun onRotate(detector: RotateGestureDetector) {
            val window = this@MainView.mEngine!!.window
            val camera = window.camera

            val deltaElevation = 200.0 / window.width
            val deltaAzimuth = -200.0 / window.height

            camera.azimuth(detector.distanceX * deltaAzimuth)
            camera.elevation(detector.distanceY * deltaElevation)
        }
    }

    // forward events to rendering thread for it to handle
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eventCopy = MotionEvent.obtain(event)
        queueEvent {
            mPanDetector.onTouchEvent(eventCopy)
            mScaleDetector.onTouchEvent(eventCopy)
            mRotateDetector.onTouchEvent(eventCopy)
            eventCopy.recycle()
        }

        return true
    }
}
