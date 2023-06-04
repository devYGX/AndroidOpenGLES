package com.example.openglesdemo.gles

import android.content.Context
import android.opengl.GLSurfaceView

class GLSurfaceViewProvider(private val glView: GLSurfaceView): IGLEnvProvider {

    override fun getContext(): Context = glView.context

    override fun requestRender() {
        glView.requestRender()
    }

    override fun queueEvent(runnable: Runnable) {
        glView.queueEvent(runnable)
    }
}