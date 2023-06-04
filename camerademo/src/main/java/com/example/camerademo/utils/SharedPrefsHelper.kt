package com.example.camerademo.utils

import android.content.Context
import android.content.SharedPreferences

object SharedPrefsHelper {

    private lateinit var mPrefs: SharedPreferences

    fun init(context: Context) {
        mPrefs = context.getSharedPreferences("CameraDemo", Context.MODE_PRIVATE)
    }

    fun getSharedPrefs(): SharedPreferences {
        return  mPrefs
    }
}