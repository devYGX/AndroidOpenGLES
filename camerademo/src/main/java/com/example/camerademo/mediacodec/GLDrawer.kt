package com.example.camerademo.mediacodec

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.example.camerademo.utils.GlesUtils

class GLDrawer(val context: Context, val width: Int, val height: Int) {

    companion object {
        const val TAG = "GLDrawer"
    }

    private var mProgram: Int = 0
    private var mUniformTexture: Int = -1
    private var mAttrPosition: Int = -1
    private var mAttrTexPosition: Int = -1
    private var mUniformMatrix: Int = -1

    private var mVertexMatrix = FloatArray(16)

    init {
        var program = createProgram("draw.vert", "draw.frag")

        mAttrPosition = GLES20.glGetAttribLocation(program,"vPosition")
        mAttrTexPosition = GLES20.glGetAttribLocation(program,"vTexturePosition")
        mUniformMatrix = GLES20.glGetUniformLocation(program,"mMatrix")
        mUniformTexture = GLES20.glGetUniformLocation(program,"sTexture")

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        Matrix.setIdentityM(mVertexMatrix, 0)

        mProgram = program
    }

    fun onDraw(texId: Int) {
        GLES20.glViewport(0, 0, width, height)
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
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(mUniformTexture, 0)


        GLES20.glUniformMatrix4fv(
            mUniformMatrix, 1, false,
            mVertexMatrix, 0
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, GlesUtils.COORDINATE.size / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
        GLES20.glDisableVertexAttribArray(mAttrTexPosition)
    }

    fun createProgram(vertexName: String, fragName: String): Int {
        val vertexCode =
            GlesUtils.loadShaderCodeFromAssets(context, vertexName)
        val fragCode =
            GlesUtils.loadShaderCodeFromAssets(context, fragName)
        val program = GlesUtils.createProgram(vertexCode, fragCode)

        if (0 > program) {
            throw RuntimeException("createProgram failed, result: $program, vertex: $vertexName, fragment:$fragName")
        }

        return program
    }

    fun release() {
        GLES20.glDeleteProgram(mProgram)
        mProgram = -1
    }
}