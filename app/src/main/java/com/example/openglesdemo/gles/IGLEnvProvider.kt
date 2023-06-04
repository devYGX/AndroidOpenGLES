package com.example.openglesdemo.gles

import android.content.Context

interface IGLEnvProvider {
    fun getContext(): Context

    fun requestRender()

    fun queueEvent(runnable: Runnable)
}