package camera.zbq.com.camerarender.widget;

import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import camera.zbq.com.camerarender.camera.CameraDrawer;
import camera.zbq.com.camerarender.camera.ICameraHelper;
import camera.zbq.com.camerarender.camera.KittyCamera;

public class CameraView extends GLSurfaceView implements GLSurfaceView.Renderer,ICameraHelper.PreviewFrameCallback{
    private KittyCamera mCamera2;
    private CameraDrawer mCameraDrawer;
    private int cameraId = 0;

    private Runnable mRunnable;
    public CameraView(Context context) {
        super(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    private void init() {
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        mCamera2 = new KittyCamera();
        mCameraDrawer = new CameraDrawer(getResources());
        mCameraDrawer.setCameraId(0);
    }
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        mCameraDrawer.onSurfaceCreated(gl10, eglConfig);
        if (mRunnable != null) {
            mRunnable.run();
            mRunnable = null;
        }
        mCamera2.open(cameraId);
        mCameraDrawer.setCameraId(cameraId);
        Point point = mCamera2.getPreviewSize();
        mCameraDrawer.setDataSize(point.x, point.y);
        mCamera2.setPreviewTexture(mCameraDrawer.getSurfaceTexture());
        mCamera2.setOnPreviewFrameCallback(this);
        mCameraDrawer.getSurfaceTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
        mCamera2.preview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        mCameraDrawer.setViewSize(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        mCameraDrawer.onDrawFrame(gl10);
    }

    @Override
    public void onPreviewFrame(byte[] bytes, int width, int height) {

    }
}
