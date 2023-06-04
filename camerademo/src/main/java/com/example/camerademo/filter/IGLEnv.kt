package com.example.camerademo.filter

import android.content.Context

interface IGLEnv {
    fun getContext(): Context

    fun requestRender()

    fun queueEvent(runnable: Runnable)
}