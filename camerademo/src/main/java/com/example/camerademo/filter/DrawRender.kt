package com.example.camerademo.filter

import android.opengl.GLES20
import com.example.camerademo.utils.GlesUtils
import com.example.camerademo.utils.LogUtils

class DrawRender(env: IGLEnv) : AbsRender(env) {

    private var mUniformTexture: Int = -1
    private var mAttrPosition: Int = -1
    private var mAttrTexPosition: Int = -1
    private var mUniformMatrix: Int = -1
    private var mDisplayScaleType: Int = GlesUtils.TYPE_CENTERCROP
    private var mDisplayScaleTypeChange = false

    override fun isAllowCreateProgram(): Boolean = true

    override fun getCodeFilePath(): Array<String> {
        return arrayOf("draw.vert", "draw.frag")
    }

    override fun onCreated() {
        mAttrPosition = glGetAttribLocation("vPosition")
        mAttrTexPosition = glGetAttribLocation("vTexturePosition")
        mUniformMatrix = glGetUniformLocation("mMatrix")
        mUniformTexture = glGetUniformLocation("sTexture")

        LogUtils.d(getTag(), "onCreated")
    }

    override fun setDisplayScaleType(scaleType: Int) {
        super.setDisplayScaleType(scaleType)

        mDisplayScaleType = scaleType
        mDisplayScaleTypeChange = true
    }

    override fun onSizeChanged()  {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        val sourceSize = getSourceSize()?:return
        val surfaceSize = getSurfaceSize()?:return
        GlesUtils.getMatrix(mVertexMatrix, mDisplayScaleType, sourceSize.width, sourceSize.height,
            surfaceSize.width, surfaceSize.height)
    }

    override fun onDraw() {
        val surfaceSize = getSurfaceSize()?:return

        if (mDisplayScaleTypeChange) {
            val sourceSize = getSourceSize()?:return

            GlesUtils.getMatrix(mVertexMatrix, mDisplayScaleType, sourceSize.width, sourceSize.height,
                surfaceSize.width, surfaceSize.height)

            LogUtils.d(getTag(), "onDraw, getMatrix: $mDisplayScaleType")

            mDisplayScaleTypeChange = false
        }

        GLES20.glViewport(0, 0, surfaceSize.width, surfaceSize.height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

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
    }

    override fun onRelease() {
        // empty
    }

    override fun supportFBO(): Boolean = false
}