package com.example.camerademo

import android.app.Application
import com.example.camerademo.utils.SharedPrefsHelper
import com.example.camerademo.utils.ToastUtils

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        SharedPrefsHelper.init(applicationContext)
        ToastUtils.init(applicationContext)
    }
}