package camera.zbq.com.camerarender.gl.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.sinohb.logger.LogTools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import camera.zbq.com.camerarender.utils.ShaderUtils;

public class RGBGLRenderer implements GLSurfaceView.Renderer {
    private Context mContext;
    private int mProgram;
    private int glHPosition;
    private int glHTexture;
    private int glHCoordinate;
    private int glHMatrix;
    private int hIsHalf;
    private int glHUxy;
    private Bitmap mBitmap;

    private int hChangeType;
    private int hChangeColor;

    private int textureId;

    private FloatBuffer bPos;
    private FloatBuffer bCoord;
    private boolean isHalf;
    private float uXY;
    private float[] mMVPMatrix = new float[16];
    private GLSurfaceView mTargetSurface;
    private final float[] sPos = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f
    };

    private final float[] sCoord = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };
    private float[] mViewMatrix = new float[16];
    private float[] mProjectMatrix = new float[16];
    private String vertexSource;
    private String fragmentSource;

    public RGBGLRenderer(Context context, GLSurfaceView surface) {
        mTargetSurface = surface;
        mContext = context;
        vertexSource = ShaderUtils.loadFromAssetsFile("filter/half_color_vertex.sh", mContext.getResources());
        fragmentSource = ShaderUtils.loadFromAssetsFile("filter/half_color_fragment.sh", mContext.getResources());
        ByteBuffer bb = ByteBuffer.allocateDirect(sPos.length * 4);
        bb.order(ByteOrder.nativeOrder());
        bPos = bb.asFloatBuffer();
        bPos.put(sPos);
        bPos.position(0);
        ByteBuffer cc = ByteBuffer.allocateDirect(sCoord.length * 4);
        cc.order(ByteOrder.nativeOrder());
        bCoord = cc.asFloatBuffer();
        bCoord.put(sCoord);
        bCoord.position(0);
    }

    private boolean hasVideo;

    public void updateRGB(Bitmap bitmap) {
        hasVideo = true;
        mBitmap = bitmap;
        mTargetSurface.requestRender();
    }

    public void updateRGB(byte[] rgb, int w, int h) {

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        ByteBuffer buffer = ByteBuffer.wrap(rgb);
        bitmap.copyPixelsFromBuffer(buffer);
        mBitmap = bitmap;
        mTargetSurface.requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        if (mProgram <= 0) {
            mProgram = createProgram(vertexSource, fragmentSource);
        }
        glHPosition = GLES20.glGetAttribLocation(mProgram, "vPosition");
        glHCoordinate = GLES20.glGetAttribLocation(mProgram, "vCoordinate");
        glHTexture = GLES20.glGetUniformLocation(mProgram, "vTexture");
        glHMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix");
        hIsHalf = GLES20.glGetUniformLocation(mProgram, "vIsHalf");
        glHUxy = GLES20.glGetUniformLocation(mProgram, "uXY");
        hChangeType = GLES20.glGetUniformLocation(mProgram, "vChangeType");
        hChangeColor = GLES20.glGetUniformLocation(mProgram, "vChangeColor");
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (mBitmap != null) {
            int w = mBitmap.getWidth();
            int h = mBitmap.getHeight();
            float sWH = w / (float) h;
            float sWidthHeight = width / (float) height;
            uXY = sWidthHeight;
            if (width > height) {
                if (sWH > sWidthHeight) {
                    Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight * sWH, sWidthHeight * sWH, -1, 1, 3, 5);
                } else {
                    Matrix.orthoM(mProjectMatrix, 0, -sWidthHeight / sWH, sWidthHeight / sWH, -1, 1, 3, 5);
                }
            } else {
                if (sWH > sWidthHeight) {
                    Matrix.orthoM(mProjectMatrix, 0, -1, 1, -1 / sWidthHeight * sWH, 1 / sWidthHeight * sWH, 3, 5);
                } else {
                    Matrix.orthoM(mProjectMatrix, 0, -1, 1, -sWH / sWidthHeight, sWH / sWidthHeight, 3, 5);
                }
            }
            //设置相机位置
            Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 5.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
            //计算变换矩阵
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectMatrix, 0, mViewMatrix, 0);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (mBitmap != null && hasVideo) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            GLES20.glUseProgram(mProgram);
            GLES20.glUniform1i(hChangeType, hChangeType);
            GLES20.glUniform3fv(hChangeColor, 1, new float[]{0.0f, 0.0f, 0.0f}, 0);
            GLES20.glUniform1i(hIsHalf, isHalf ? 1 : 0);
            GLES20.glUniform1f(glHUxy, uXY);
            GLES20.glUniformMatrix4fv(glHMatrix, 1, false, mMVPMatrix, 0);
            GLES20.glEnableVertexAttribArray(glHPosition);
            GLES20.glEnableVertexAttribArray(glHCoordinate);
            GLES20.glUniform1i(glHTexture, 0);
            textureId = createTexture();
            GLES20.glVertexAttribPointer(glHPosition, 2, GLES20.GL_FLOAT, false, 0, bPos);
            GLES20.glVertexAttribPointer(glHCoordinate, 2, GLES20.GL_FLOAT, false, 0, bCoord);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        }

    }

    private int createTexture() {
        int[] texture = new int[1];
        if (mBitmap != null && !mBitmap.isRecycled()) {
            //生成纹理
            GLES20.glGenTextures(1, texture, 0);
            //生成纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            //根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
            return texture[0];
        }
        return 0;
    }

    /**
     * create program and load shaders, fragment shader is very important.
     */
    public int createProgram(String vertexSource, String fragmentSource) {
        // create shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        // just check
        Log.d("zbq", "vertexShader = " + vertexShader);
        Log.d("zbq", "pixelShader = " + pixelShader);

        int program = GLES20.glCreateProgram();
//        if (program != 0) {
//            GLES20.glAttachShader(program, vertexShader);
//            checkGlError("glAttachShader");
//            GLES20.glAttachShader(program, pixelShader);
//            checkGlError("glAttachShader");
//            GLES20.glLinkProgram(program);
//            int[] linkStatus = new int[1];
//            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
//            if (linkStatus[0] != GLES20.GL_TRUE) {
//                Log.e("zbq", "Could not link program: ", null);
//                Log.e("zbq", GLES20.glGetProgramInfoLog(program), null);
//                GLES20.glDeleteProgram(program);
//                program = 0;
//            }
//        }
//        return program;
        if (pixelShader == 0) return 0;
        if (program != 0) {
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, pixelShader);
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
            if (linkStatus[0] != GLES20.GL_TRUE) {
                glError(1, "Could not link program:" + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    //    //加载shader
//    public static int uLoadShader(int shaderType,String source){
//        int shader= GLES20.glCreateShader(shaderType);
//        if(0!=shader){
//            GLES20.glShaderSource(shader,source);
//            GLES20.glCompileShader(shader);
//            int[] compiled=new int[1];
//            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS,compiled,0);
//            if(compiled[0]==0){
//                glError(1,"Could not compile shader:"+shaderType);
//                glError(1,"GLES20 Error:"+ GLES20.glGetShaderInfoLog(shader));
//                GLES20.glDeleteShader(shader);
//                shader=0;
//            }
//        }
//        return shader;
//    }
    public static void glError(int code, Object index) {
        LogTools.e("gl", "glError:" + code + "---" + index);
    }

    /**
     * create shader with given source.
     */
    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            int[] compiled = new int[1];
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
            if (compiled[0] == 0) {
                Log.e("zbq", "Could not compile shader " + shaderType + ":", null);
                Log.e("zbq", GLES20.glGetShaderInfoLog(shader), null);
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
        }
        return shader;
    }

    private void checkGlError(String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("zbq", "***** " + op + ": glError " + error, null);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

}
