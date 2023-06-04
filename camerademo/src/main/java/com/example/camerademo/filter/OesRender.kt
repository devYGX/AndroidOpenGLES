package com.example.camerademo.filter

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.example.camerademo.utils.GlesUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class OesRender(private val mGlEnv: IGLEnv, private val fbo: Boolean) : AbsRender(mGlEnv) {

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

    private var mSurfaceTextureListener: Listener? = null
    private var mbActivityIsLandscape = false

    private var mTexMatrix = FloatArray(16)
    /**  手机为横屏时候使用如下的纹理数组; 竖屏时则使用GlesUtils.SOURCE_MATRIX  **/
    private var mTexArray = floatArrayOf(
        0f, 1f,
        1f, 1f,
        1f, 0f,
        0f, 0f)

    private val mTexBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(mTexArray.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(mTexArray)
                position(0)
            }
    }

    fun setActivityIsLandscape(landscape: Boolean) {
        mbActivityIsLandscape = landscape
    }

    override fun isAllowCreateProgram(): Boolean = true

    override fun onCreated() {
        val textureId = GlesUtils.createExternalTextureId()
        val surfaceTexture = SurfaceTexture(textureId)

        surfaceTexture.setOnFrameAvailableListener {
            mGlEnv.requestRender()
        }

        mOESSurfaceTexture = surfaceTexture
        mOESTextureId = textureId
        mSurfaceTextureListener?.onSurfaceTextureAvailable(mOESSurfaceTexture!!)

        mAttrPosition = glGetAttribLocation("vPosition")
        mAttrTexPosition = glGetAttribLocation("vTexturePosition")
        mUniformMatrix = glGetUniformLocation("mMatrix")
        mUniformTexture = glGetUniformLocation("sTexture")
        mUniformTexMatrix = glGetUniformLocation("mTexMatrix")
    }

    override fun onDraw() {
        if (supportFBO()) {
            val size = getSourceSize() ?: return
            GLES20.glViewport(0, 0, size.width, size.height)
        } else {
            val size = getSurfaceSize() ?: return
            GLES20.glViewport(0, 0, size.width, size.height)
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        mOESSurfaceTexture?.updateTexImage()
        mOESSurfaceTexture?.getTransformMatrix(mTexMatrix)

        if (supportFBO()) {
            GlesUtils.bindFrameBuffer(mFrameArray[0], mRenderArray[0], mOutTexArray[0])
        }

        GLES20.glUseProgram(mProgram)
        GLES20.glEnableVertexAttribArray(mAttrPosition)
        GLES20.glEnableVertexAttribArray(mAttrTexPosition)
        // 参数二为每组坐标取多少位坐标，2:xy或3:xyz都可以;
        // 参数5为每个顶点的步长, 坐标位数 * sizeof(float)
        GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 4 * 4,
            GlesUtils.COORDINATE_BUFFER)
        GLES20.glVertexAttribPointer(mAttrTexPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
            (if (mbActivityIsLandscape) mTexBuffer else GlesUtils.TEX_COORDINATE_BUFFER))
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextureId)
        GLES20.glUniform1i(mUniformTexture, 0)


        GLES20.glUniformMatrix4fv(mUniformMatrix, 1, false, mVertexMatrix, 0)
        GLES20.glUniformMatrix4fv(mUniformTexMatrix, 1, false, mTexMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, GlesUtils.COORDINATE.size / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
        GLES20.glDisableVertexAttribArray(mAttrTexPosition)

        if (supportFBO()) {
            GlesUtils.unbindFrameBuffer()
        }
    }

    override fun onRelease() {
        // NOTHING TO DO
        deleteFBO()

        mOESSurfaceTexture?.release()
        GLES20.glDeleteTextures(1, intArrayOf( mOESTextureId), 0)
    }

    override fun supportFBO() : Boolean = fbo

    override fun getCodeFilePath(): Array<String> {
        return arrayOf("oes.vert", "oes.frag")
    }

    fun setSurfaceTextureListener(l: Listener) {
        this.mSurfaceTextureListener = l
    }

    interface Listener {
        fun onSurfaceTextureAvailable(surface: SurfaceTexture)
    }
}