package com.example.openglesdemo.renderer

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.openglesdemo.gles.GlesUtils
import com.example.openglesdemo.gles.IGLEnvProvider
import com.example.openglesdemo.utils.LogUtils

abstract class AbsRenderer(provider: IGLEnvProvider) {
    companion object {
        const val TAG = "AbsRenderer"
        const val STATE_RELEASE = -1
        const val STATE_NONE = 0
        const val STATE_CREATED = 1
        const val STATE_PREPARED = 2
    }

    private var mState = STATE_NONE
    private var mVertexShader: Int = -1
    private var mFragShader: Int = -1
    protected var mProgram: Int = -1

    protected var mWidth:Int = 0
    protected var mHeight:Int = 0

    protected var mVertexMatrix = FloatArray(16)

    protected var mGLEnvProvider = provider

    fun create() {
        if (STATE_CREATED <= mState) {
            // maybe call 'create' repeat
            return
        }

        // create program
        getGlslCodePath().apply {
            mVertexShader = GlesUtils.createShader(mGLEnvProvider.getContext(), GLES20.GL_VERTEX_SHADER, this[0])
            mFragShader = GlesUtils.createShader(mGLEnvProvider.getContext(), GLES20.GL_FRAGMENT_SHADER, this[1])
            mProgram = GlesUtils.createProgram(mVertexShader, mFragShader)

            if ((0 >= mVertexShader) || (0 >= mFragShader) || (0 >= mProgram)) {
                releaseGlesHandle()

                LogUtils.d(TAG, "create program failed!")
            } else {
                mState = STATE_CREATED
                Matrix.setIdentityM(mVertexMatrix, 0)
                onCreated()
            }
        }
    }

    protected abstract fun getGlslCodePath():Array<String>
    protected abstract fun onCreated()
    protected abstract fun onPrepared()
    protected abstract fun onDraw()
    protected abstract fun onRelease()

    fun prepare(width: Int, height: Int) {
        if (STATE_PREPARED <= mState || (STATE_NONE >= mState)) {
            return
        }

        mWidth = width
        mHeight = height
        mState = STATE_PREPARED
        onPrepared()
    }

    fun draw() {
        if (STATE_PREPARED != mState) {
            return
        }

        onDraw()
    }

    fun release() {
        mState = STATE_RELEASE

        releaseGlesHandle()
        onRelease()
    }

    protected fun glGetUniformLocation(name: String): Int {
        if (STATE_CREATED > mState) {
            throw RuntimeException("glGetAttribLocation, Program has not create")
        }

        return GlesUtils.glGetUniformLocation(mProgram, name)
    }

    protected fun glGetAttribLocation(name: String): Int {
        if (STATE_CREATED > mState) {
            throw RuntimeException("glGetAttribLocation, Program has not create")
        }

        return GLES20.glGetAttribLocation(mProgram, name)
    }

    private fun releaseGlesHandle() {
        GlesUtils.deleteShadersSafety(mVertexShader, mFragShader)

        if (0 < mProgram) {
            GLES20.glDeleteProgram(mProgram)
        }

        mVertexShader = -1
        mVertexShader = -1
        mProgram = -1
    }
}