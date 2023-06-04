package com.example.camerademo.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {

    private var mToast: Toast? = null

    fun init(context: Context) {
        mToast = Toast.makeText(context, "", Toast.LENGTH_LONG)
    }

    fun showLongToast(msg: String) {
        mToast?.apply { setText(msg) }?.show()
    }
}