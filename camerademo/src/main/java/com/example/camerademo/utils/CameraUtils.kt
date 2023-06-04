package com.example.camerademo.utils

import android.util.Size
import com.example.camerademo.constants.Constants

object CameraUtils {
    fun getVideoSize(type: String): Size {
        return when (type) {
            Constants.VIDEO_TYPE_1080P -> Size(1920, 1080)
            Constants.VIDEO_TYPE_720P -> Size(1280, 720)
            Constants.VIDEO_TYPE_480P -> Size(640, 480)
            else -> throw RuntimeException("UnSupported Size Type: $type")
        }
    }
}