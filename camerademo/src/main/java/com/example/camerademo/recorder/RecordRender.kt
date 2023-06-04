package com.example.camerademo.recorder

import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLContext
import android.view.Surface
import com.example.camerademo.mediacodec.EGLBase
import com.example.camerademo.mediacodec.GLDrawer
import com.example.camerademo.mediacodec.MediaEncoder
import com.example.camerademo.render.ExternalRender

class RecordRender(val context: Context, private val encoder: MediaEncoder, surface: Surface): ExternalRender(), Runnable {

    companion object {
        const val TAG = "RecordRender"

        @JvmStatic
        fun createRender(context: Context, encoder: MediaEncoder, surface: Surface): RecordRender =
            RecordRender(context, encoder, surface).apply {
                prepare()
            }
    }

    private var mRecordSurface: Surface? = surface
    private val mSync = Object()
    private var mEglContext: EGLContext? = null
    private var mInputSurface: EGLBase.EglSurfaceWrapper? = null
    private var mEgl: EGLBase? = null
    private var mGLDrawer: GLDrawer? = null

    private var mbRequestSetEglContext = false
    @Volatile private var mbRequestRelease = false
    private var mbRequestDraw = 0
    private var mbIsRecordable = true

    private var mTexId: Int = -1
    private var mWidth: Int = -1
    private var mHeight: Int = -1

    private fun prepare() {
        Thread(this, TAG).start()
    }

    fun release() {
        synchronized(mSync) {
            if (!mbRequestRelease) {
                mbRequestRelease = true
                mSync.notifyAll()
                mSync.wait()
            }
        }
    }

    override fun onPrepared(width: Int, height: Int) {
        super.onPrepared(width, height)
        mWidth = width
        mHeight = height

        mEglContext = EGL14.eglGetCurrentContext()

        synchronized(mSync) {
            mbRequestSetEglContext = true
            mSync.notifyAll()
        }
    }

    override fun onDraw(texId: Int) {
        if (mbRequestRelease) {
            return
        }

        if (encoder.frameAvailableSoon()) {
            synchronized(mSync) {
                mTexId = texId
                mbRequestDraw ++
                mSync.notifyAll()
            }
        }
    }


    private fun internalPrepare() {
        internalRelease()
        mEgl = EGLBase(mEglContext, false, mbIsRecordable)
        mInputSurface = mEgl?.createFromSurface(mRecordSurface!!, mWidth, mHeight)
        mInputSurface?.makeCurrent()
        mGLDrawer = GLDrawer(context, mWidth, mHeight)
    }

    private fun internalRelease() {
        mInputSurface?.release()
        mGLDrawer?.release()
        mEgl?.release()
    }

    override fun run() {
        synchronized(mSync) {
            mbRequestSetEglContext = false
            mbRequestRelease = false
            mbRequestDraw = 0

            mSync.notifyAll()
        }

        var localRequestDraw = false
        var isRunning = true

        outer@while (isRunning) {
            synchronized(mSync) {
                localRequestDraw = false

                if (mbRequestRelease) {
                    isRunning = false
                } else if (mbRequestSetEglContext) {
                    mbRequestSetEglContext = false
                    internalPrepare()
                } else if (0 < mbRequestDraw) {
                    localRequestDraw = true
                    mbRequestDraw --
                }
            }

            if (!isRunning) {
                break
            }

            if (localRequestDraw) {
                if ((null != mEgl) && (0 < mTexId)) {
                    mInputSurface?.makeCurrent()
                    mGLDrawer?.onDraw(mTexId)
                    // draw
                    mInputSurface?.swap()
                }
            } else {
                synchronized(mSync) {
                    mSync.wait()
                }
            }
        }

        synchronized(mSync) {
            mbRequestRelease = true
            internalRelease()
            mRecordSurface = null
            mSync.notifyAll()
        }
    }
}