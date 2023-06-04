package com.example.camerademo.camera

import android.graphics.SurfaceTexture
import android.util.Size

interface ICamera {
    fun setCallback(callback: Callback)

    fun setSurfaceTexture(st: SurfaceTexture)

    fun startCamera()

    fun closeCamera()

    fun dumpImage(flag: Boolean)

    fun destroy()

    interface Callback {
        fun onError(msg: String)

        fun onPrepared(size: Size)
    }
}