package com.example.camerademo.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import java.util.*

class TimerTextView : AppCompatTextView {

    private var mTimer: Timer? = null
    private var mSecondIndex: Int = 0

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    @Synchronized
    fun start() {
        mTimer?.apply {
            return
        }


        mTimer = Timer()
        mTimer!!.schedule(object : TimerTask() {
            override fun run() {
                mSecondIndex++
                post { setTimeText() }
            }
        }, 1000, 1000)

        mSecondIndex = 0
        post { setTimeText() }
    }

    private fun setTimeText() {
        val hour = mSecondIndex / 60 / 60
        val minute = mSecondIndex / 60
        val second = mSecondIndex % 60

        val text = when {
            0 < hour -> {
                "${getTimeText(hour)} : ${getTimeText(minute)} : ${getTimeText(second)}"
            }

            else -> {
                "${getTimeText(minute)} : ${getTimeText(second)}"
            }
        }

        setText(text)
    }

    private fun getTimeText(time: Int): String = if (10 > time) "0$time" else "$time"

    @Synchronized
    fun stop() {
        mTimer?.apply {
            cancel()

            mTimer = null
        }

        post { text = "" }
    }
}