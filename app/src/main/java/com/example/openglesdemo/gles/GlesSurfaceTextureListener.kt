package com.example.openglesdemo.gles

import android.graphics.SurfaceTexture

interface GlesSurfaceTextureListener {
    fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int)

    fun onSurfaceTextureDestroy()
}