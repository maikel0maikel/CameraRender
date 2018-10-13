/*
 *
 * SGLRender.java
 *
 * Created by Wuwang on 2016/10/15
 */
package camera.zbq.com.camerarender.gl.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import camera.zbq.com.camerarender.gl.image.filter.AFilter;
import camera.zbq.com.camerarender.gl.image.filter.ColorFilter;
import camera.zbq.com.camerarender.gl.image.filter.ContrastColorFilter;


/**
 * Description:
 */
public class SGLRender implements GLSurfaceView.Renderer {

    private AFilter mFilter;
    private Bitmap bitmap;
    private int width, height;
    private boolean refreshFlag = false;
    private EGLConfig config;
    private GLSurfaceView mTargetSurface;
    private boolean hasVideo;

    public SGLRender(Context context, GLSurfaceView surface) {
        mFilter = new ContrastColorFilter(context, ColorFilter.Filter.NONE);
        mTargetSurface = surface;
    }

    public void setFilter(AFilter filter) {
        refreshFlag = true;
        mFilter = filter;
        if (bitmap != null) {
            mFilter.setBitmap(bitmap);
        }
    }

    public void setImageBuffer(int[] buffer, int width, int height) {
        bitmap = Bitmap.createBitmap(buffer, width, height, Bitmap.Config.RGB_565);
        mFilter.setBitmap(bitmap);
    }

    public void refresh() {
        refreshFlag = true;
    }

    public AFilter getFilter() {
        return mFilter;
    }

    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
        mFilter.setBitmap(bitmap);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        this.config = config;
        mFilter.onSurfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        mFilter.onSurfaceChanged(gl, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (hasVideo) {
            if (refreshFlag && width != 0 && height != 0) {
                mFilter.onSurfaceCreated(gl, config);
                mFilter.onSurfaceChanged(gl, width, height);
                refreshFlag = false;
            }
            mFilter.onDrawFrame(gl);
        } else {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }
    }

    public void updateRGB(byte[] rgb, int w, int h) {
        hasVideo = true;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(rgb);
        bitmap.copyPixelsFromBuffer(buffer);
        mFilter.setBitmap(bitmap);
        mTargetSurface.requestRender();
    }
    public void updateRGB(Bitmap bitmap) {
        hasVideo = true;
        mFilter.setBitmap(bitmap);
        mTargetSurface.requestRender();
    }
    public void disconnect() {
        if (hasVideo) {
            hasVideo = false;
            mTargetSurface.requestRender();
        }
    }
}
