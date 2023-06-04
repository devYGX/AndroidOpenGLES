package com.example.camerademo.render

import com.example.camerademo.utils.LogUtils


abstract class ExternalRender {
    companion object {
        const val TAG = "ExternalRender"
        const val STATE_NONE = 0
        const val STATE_CREATED = 1
        const val STATE_PREPARED = 2
        const val STATE_DESTROY = 3

    }

    private var mState = STATE_NONE

    fun getState() : Int = mState

    fun isCreated() : Boolean = STATE_CREATED <= mState

    fun isPrepared() : Boolean = STATE_PREPARED <= mState

    fun draw(texId: Int) {
        if (STATE_PREPARED != mState) {
            LogUtils.d(TAG, "draw with unprepared state: $mState, so return")

            return
        }

        onDraw(texId)
    }
    open fun onCreated() {
        mState = STATE_CREATED
    }

    open fun onPrepared(width: Int, height: Int) {
        mState = STATE_PREPARED
    }

    open fun onDestroy() {
        mState = STATE_DESTROY
    }

    abstract fun onDraw(texId: Int)
}