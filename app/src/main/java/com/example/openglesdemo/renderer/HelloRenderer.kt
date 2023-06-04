package com.example.openglesdemo.renderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.example.openglesdemo.utils.LogUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class HelloRenderer(private val glView: GLSurfaceView): GLSurfaceView.Renderer {
    companion object {
        const val TAG = "HelloRenderer"
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        var array = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, array, 0)
        LogUtils.d(TAG, "onSurfaceCreated ${array[0]}")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glClearColor(0.4f, 0f, 0f,1.0f)

        LogUtils.d(TAG, "onSurfaceChanged")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        LogUtils.d(TAG, "onDrawFrame")
    }

    fun release() {
        glView.queueEvent {
            // do some release action
        }
    }
}