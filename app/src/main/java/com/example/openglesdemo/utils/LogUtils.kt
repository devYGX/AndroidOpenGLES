package com.example.openglesdemo.utils

import android.util.Log

class LogUtils {
    companion object {
        private fun getTag(tag: String): String {
            return "YGX_$tag"
        }

        @JvmStatic
        fun d(tag: String, msg: String) {
            Log.d(getTag(tag), msg)
        }
    }
}