package com.example.openglesdemo.gles.render

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.example.openglesdemo.gles.GlesSurfaceTextureListener
import com.example.openglesdemo.gles.GlesUtils
import com.example.openglesdemo.gles.IGLEnvProvider
import com.example.openglesdemo.renderer.AbsRenderer
import com.example.openglesdemo.utils.LogUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class OesRender(glProvider: IGLEnvProvider): AbsRenderer(glProvider) {

    companion object {
        const val TAG = "OesRender"
    }

    private var mUniformTexture: Int = -1
    private var mAttrPosition: Int = -1
    private var mAttrTexPosition: Int = -1
    private var mUniformMatrix: Int = -1
    private var mUniformTexMatrix: Int = -1

    private var mOESTextureId: Int = -1
    private var mOESSurfaceTexture: SurfaceTexture? = null
    private var mListener: GlesSurfaceTextureListener? = null

    private var mDisplayWidth: Int = -1
    private var mDisplayHeight: Int = -1
    private var mbIsScreenLandscape = true

    private var mTexMatrix = FloatArray(16)
    private var mLandscapeTexCoordinates = floatArrayOf(
        0f, 1f,
        1f, 1f,
        1f, 0f,
        0f, 0f)

    private val mLandscapeTexCoordinatesBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(mLandscapeTexCoordinates.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(mLandscapeTexCoordinates)
                position(0)
            }
    }

    override fun getGlslCodePath(): Array<String> {
        return arrayOf("glsl/vertex/oes.glsl", "glsl/frag/oes.glsl")
    }

    override fun onCreated() {
        mAttrPosition = glGetAttribLocation("vPosition")
        mAttrTexPosition = glGetAttribLocation("vTexturePosition")
        mUniformMatrix = glGetUniformLocation("mMatrix")
        mUniformTexture = glGetUniformLocation("sTexture")
        mUniformTexMatrix = glGetUniformLocation("mTexMatrix")

        LogUtils.d(TAG, "onCreated")
    }

    override fun onPrepared() {
        GLES20.glClearColor(0f, 0f, 0f, 1f)

        val textureId = GlesUtils.createExternalTextureId()
        val surfaceTexture = SurfaceTexture(textureId)

        surfaceTexture.setOnFrameAvailableListener {
            mGLEnvProvider.requestRender()
        }

        mOESSurfaceTexture = surfaceTexture
        mOESTextureId = textureId
        mListener?.onSurfaceTextureAvailable(surfaceTexture, mWidth, mHeight)

        LogUtils.d(TAG, "onPrepared")
    }

    override fun onDraw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        mOESSurfaceTexture?.updateTexImage()
        mOESSurfaceTexture?.getTransformMatrix(mTexMatrix)

        /*if (supportFBO()) {
            GlesUtils.bindFrameBuffer(mFrameArray[0], mRenderArray[0], mOutTexArray[0])
        }*/

        GLES20.glUseProgram(mProgram)
        GLES20.glEnableVertexAttribArray(mAttrPosition)
        GLES20.glEnableVertexAttribArray(mAttrTexPosition)
        // 参数二为每组坐标取多少位坐标，2:xy或3:xyz都可以;
        // 参数5为每个顶点的步长, 坐标位数 * sizeof(float)
        GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 4 * 4,
            GlesUtils.COORDINATE_BUFFER)
        GLES20.glVertexAttribPointer(mAttrTexPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
            (if (mbIsScreenLandscape) mLandscapeTexCoordinatesBuffer else GlesUtils.TEX_COORDINATE_BUFFER))
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextureId)
        GLES20.glUniform1i(mUniformTexture, 0)


        GLES20.glUniformMatrix4fv(mUniformMatrix, 1, false, mVertexMatrix, 0)
        GLES20.glUniformMatrix4fv(mUniformTexMatrix, 1, false, mTexMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, GlesUtils.COORDINATE.size / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
        GLES20.glDisableVertexAttribArray(mAttrTexPosition)

        /*if (supportFBO()) {
            GlesUtils.unbindFrameBuffer()
        }*/
    }

    override fun onRelease() {
        mListener?.onSurfaceTextureDestroy()
        mListener = null
        mOESSurfaceTexture?.release()
        GLES20.glDeleteTextures(1, intArrayOf(mOESTextureId), 0)
        mOESTextureId = -1
    }

    fun setDisplaySize(width: Int, height: Int) {
        mDisplayWidth = width
        mDisplayHeight = height
    }

    fun setListener(l: GlesSurfaceTextureListener) {
        this.mListener = l
    }
}