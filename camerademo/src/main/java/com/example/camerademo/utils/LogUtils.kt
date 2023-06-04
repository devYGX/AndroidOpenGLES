package com.example.camerademo.utils

import android.util.Log

class LogUtils {
    companion object {
        fun d(tag: String, s: String) {
            Log.d(tag, s)
        }

        fun d(tag: String, s: String, t: Throwable) {
            Log.d(tag, s, t)
        }

        fun e(tag: String, s: String) {
            Log.e(tag, s)
        }

        fun w(tag: String, s: String) {
            Log.w(tag, s)
        }
    }
}