package com.example.camerademo.filter

import android.opengl.GLES20
import com.example.camerademo.utils.GlesUtils
import java.nio.ByteBuffer

class CaptureFilter(private val glEnv: IGLEnv): AbsFilter(glEnv) {
    private var mUniformTexture: Int = -1
    private var mAttrPosition: Int = -1
    private var mAttrTexPosition: Int = -1
    private var mUniformMatrix: Int = -1

    private var mCaptureBuffer: ByteBuffer? = null
    private var mCaptureCallback: CaptureCallback? = null

    override fun isAllowCreateProgram(): Boolean = true

    override fun getCodeFilePath(): Array<String>? {
        return arrayOf("draw.vert", "draw.frag")
    }

    override fun onCreated() {
        super.onCreated()
        mAttrPosition = glGetAttribLocation("vPosition")
        mAttrTexPosition = glGetAttribLocation("vTexturePosition")
        mUniformMatrix = glGetUniformLocation("mMatrix")
        mUniformTexture = glGetUniformLocation("sTexture")
    }

    override fun onSizeChanged() {
        super.onSizeChanged()

        GLES20.glClearColor(0f, 0f, 0f, 1f)
    }

    override fun onDraw() {
        val size = getSourceSize()?:return

        GLES20.glViewport(0, 0, size.width, size.height)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GlesUtils.bindFrameBuffer(mFrameArray[0], mRenderArray[0], mOutTexArray[0])

        GLES20.glUseProgram(mProgram)
        GLES20.glEnableVertexAttribArray(mAttrPosition)
        GLES20.glEnableVertexAttribArray(mAttrTexPosition)
        // 参数二为每组坐标取多少位坐标，2:xy或3:xyz都可以;
        // 参数5为每个顶点的步长, 坐标位数 * sizeof(float)
        GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 4 * 4,
            GlesUtils.COORDINATE_BUFFER)
        GLES20.glVertexAttribPointer(mAttrTexPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
            GlesUtils.TEX_COORDINATE_BUFFER)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getInputTexture())
        GLES20.glUniform1i(mUniformTexture, 0)


        GLES20.glUniformMatrix4fv(
            mUniformMatrix, 1, false,
            mVertexMatrix, 0
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, GlesUtils.COORDINATE.size / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
        GLES20.glDisableVertexAttribArray(mAttrTexPosition)

        if (null != mCaptureCallback) {
            mCaptureBuffer?.position(0)

            GLES20.glReadPixels(
                0, 0, size.width, size.height, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, mCaptureBuffer
            )

            mCaptureCallback!!.onCapture(mCaptureBuffer!!, size.width, size.height)
        }

        GlesUtils.unbindFrameBuffer()
    }

    override fun createFBO(width: Int, height: Int) {
        super.createFBO(width, height)

        mCaptureBuffer = ByteBuffer.allocate(width * height * 4)
    }

    override fun onRelease() {
        // empty
    }

    override fun supportFBO(): Boolean = true

    fun setCaptureCallback(callback: CaptureCallback) {
        mCaptureCallback = callback
    }

    interface CaptureCallback {
        fun onCapture(buffer: ByteBuffer, width: Int, height: Int)
    }
}