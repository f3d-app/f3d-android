package app.f3d.F3D.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import app.f3d.F3D.*;

public class MainView extends GLSurfaceView {
    private Engine mEngine;

    final private ScaleGestureDetector mScaleDetector;
    final private PanGestureDetector mPanDetector;
    final private RotateGestureDetector mRotateDetector;

    public MainView(Context context) {
        super(context);

        start();

        this.mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.mPanDetector = new PanGestureDetector(new PanListener());
        this.mRotateDetector = new RotateGestureDetector(new RotateListener());
    }

    void start() {
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setEGLContextClientVersion(3);

        this.setRenderer(new Renderer());
        this.setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public void openBuffer(String buffer, String mimeType) {
        // @TODO: to implement in C++ and expose the API
        Log.e("Not implemented yet in F3D: open with mimetype=", mimeType);
    }

    private class Renderer implements GLSurfaceView.Renderer {

        public void onDrawFrame(GL10 gl) {
            MainView.this.mEngine.getWindow().render();
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            MainView.this.mEngine.getWindow().setSize(width, height);
            MainView.this.mEngine.getWindow().render();
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Engine.autoloadPlugins();

            MainView.this.mEngine = new Engine();

            MainView.this.mEngine.setCachePath(MainView.this.getContext().getCacheDir().getAbsolutePath());

            MainView.this.mEngine.getOptions().toggle("interactor.axis");
            MainView.this.mEngine.getOptions().toggle("render.grid.enable");
            MainView.this.mEngine.getOptions().toggle("render.effect.anti-aliasing");
            MainView.this.mEngine.getOptions().toggle("render.effect.tone-mapping");
            MainView.this.mEngine.getOptions().toggle("render.hdri.ambient");
            MainView.this.mEngine.getOptions().toggle("render.background.skybox");
            MainView.this.mEngine.getOptions().toggle("ui.filename");
            MainView.this.mEngine.getOptions().toggle("ui.loader-progress");

            // hard-coded path, change it
            MainView.this.mEngine.getLoader().loadScene("/data/local/tmp/WaterBottle.glb");
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            MainView.this.mEngine.getWindow().getCamera().dolly(detector.getScaleFactor());
            MainView.this.mEngine.getWindow().render();
            return true;
        }
    }

    private class PanListener extends PanGestureDetector.OnPanGestureListener {
        @Override
        public void onPan(PanGestureDetector detector) {

            Window window = MainView.this.mEngine.getWindow();
            Camera camera = window.getCamera();

            double[] pos = camera.getPosition();
            double[] focus = camera.getFocalPoint();
            double[] focusDC = window.getDisplayFromWorld(focus);

            double[] shiftDC = { focusDC[0] - detector.getDistanceX(), focusDC[1] + detector.getDistanceY(), focusDC[2] };
            double[] shift = window.getWorldFromDisplay(shiftDC);

            double[] motion = { shift[0] - focus[0], shift[1] - focus[1], shift[2] - focus[2] };

            camera.setFocalPoint(new double[] { motion[0] + focus[0], motion[1] + focus[1], motion[2] + focus[2] });
            camera.setPosition(new double[] { motion[0] + pos[0], motion[1] + pos[1], motion[2] + pos[2] });

            window.render();
        }
    }

    private class RotateListener extends RotateGestureDetector.OnRotateGestureListener {
        @Override
        public void onRotate(RotateGestureDetector detector) {

            Window window = MainView.this.mEngine.getWindow();
            Camera camera = window.getCamera();

            double delta_elevation = 200.0 / window.getWidth();
            double delta_azimuth = -200.0 / window.getHeight();

            camera.azimuth(detector.getDistanceX() * delta_azimuth);
            camera.elevation(detector.getDistanceY() * delta_elevation);

            window.render();
        }
    }

    // forward events to rendering thread for it to handle
    public boolean onTouchEvent(MotionEvent event) {
        queueEvent(() -> {
                mPanDetector.onTouchEvent(event);
                mScaleDetector.onTouchEvent(event);
                mRotateDetector.onTouchEvent(event);
            }
        );

        return true;
    }
}
