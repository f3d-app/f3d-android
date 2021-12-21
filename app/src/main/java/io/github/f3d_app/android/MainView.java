package io.github.f3d_app.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import io.github.f3d_app.f3d.*;

public class MainView extends GLSurfaceView {
    private static String TAG = "F3DMainView";

    private Engine mEngine;

    private Renderer mRenderer;

    private ScaleGestureDetector mScaleDetector;
    private PanGestureDetector mPanDetector;
    private RotateGestureDetector mRotateDetector;

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

        this.mRenderer = new Renderer();
        this.setRenderer(mRenderer);
        this.setRenderMode(RENDERMODE_WHEN_DIRTY);
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
            MainView.this.mEngine = new Engine(Window.Type.NATIVE);

            MainView.this.mEngine.getOptions().toggle("interactor.axis");
            MainView.this.mEngine.getOptions().toggle("render.grid");
            MainView.this.mEngine.getOptions().toggle("render.effect.fxaa");
            MainView.this.mEngine.getOptions().toggle("render.effect.tone-mapping");
            MainView.this.mEngine.getOptions().toggle("ui.filename");
            MainView.this.mEngine.getOptions().toggle("ui.loader-progress");

            MainView.this.mEngine.getLoader().addFile("/data/local/tmp/WaterBottle.glb");
            MainView.this.mEngine.getLoader().addFile("/data/local/tmp/MetalRoughSpheresNoTextures.glb");
            MainView.this.mEngine.getLoader().addFile("/data/local/tmp/MetalRoughSpheres.glb");
            MainView.this.mEngine.getLoader().addFile("/data/local/tmp/CesiumMan.glb");
            MainView.this.mEngine.getLoader().addFile("/data/local/tmp/BoxTextured.glb");

            MainView.this.mEngine.getLoader().loadFile(Loader.LoadFileEnum.LOAD_FIRST);
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
        public boolean onPan(PanGestureDetector detector) {

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

            return true;
        }
    }

    private class RotateListener extends RotateGestureDetector.OnRotateGestureListener {
        @Override
        public boolean onRotate(RotateGestureDetector detector) {

            Window window = MainView.this.mEngine.getWindow();
            Camera camera = window.getCamera();

            double delta_elevation = 200.0 / window.getWidth();
            double delta_azimuth = -200.0 / window.getHeight();

            camera.azimuth(detector.getDistanceX() * delta_azimuth);
            camera.elevation(detector.getDistanceY() * delta_elevation);

            window.render();
            return true;
        }
    }

    // forward events to rendering thread for it to handle
    public boolean onTouchEvent(MotionEvent event) {
        queueEvent(new Runnable() {
            public void run() {
                mPanDetector.onTouchEvent(event);
                mScaleDetector.onTouchEvent(event);
                mRotateDetector.onTouchEvent(event);
            }
        });

        return true;
    }

    public boolean loadPrevious() {
        queueEvent(new Runnable() {
            public void run() {
                MainView.this.mEngine.getLoader().loadFile(Loader.LoadFileEnum.LOAD_PREVIOUS);
                MainView.this.mEngine.getWindow().render();
            }
        });

        return true;
    }

    public boolean loadNext() {
        queueEvent(new Runnable() {
            public void run() {
                MainView.this.mEngine.getLoader().loadFile(Loader.LoadFileEnum.LOAD_NEXT);
                MainView.this.mEngine.getWindow().render();
            }
        });

        return true;
    }
}
