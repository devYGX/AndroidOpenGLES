package com.example.camerademo.mediacodec

import android.graphics.SurfaceTexture
import android.opengl.*
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import com.example.camerademo.utils.LogUtils

class EGLBase(eglContext: EGLContext?, val widthDepthBuffer: Boolean, val isRecordable: Boolean) {

    companion object {
        const val DEBUG = true
        const val TAG = "EGLBase"

        const val EGL_RECORDABLE_ANDROID = 0x3142
    }

    private var mEglDisplay = EGL14.EGL_NO_DISPLAY
    private var mEGLContext: EGLContext? = EGL14.EGL_NO_CONTEXT
    private var mDefaultEGLContext: EGLContext? = EGL14.EGL_NO_CONTEXT
    private var mEglConfig: EGLConfig? = null

    init {
        init(eglContext)
    }

    private fun init(sharedEglContext: EGLContext?) {
        if (EGL14.EGL_NO_DISPLAY != mEglDisplay) {
            throw RuntimeException("EGL already set up")
        }

        mEglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

        if (EGL14.EGL_NO_DISPLAY == mEglDisplay) {
            throw RuntimeException("eglGetDisplay failed")
        }

        val version = IntArray(2)

        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = null

            throw RuntimeException("eglInitialize failed")
        }

        var eglContext = sharedEglContext ?: EGL14.EGL_NO_CONTEXT

        if (EGL14.EGL_NO_CONTEXT == mEGLContext) {
            mEglConfig = getConfig(widthDepthBuffer, isRecordable)
                ?: throw RuntimeException("chooseConfig failed")

            mEGLContext = createContext(eglContext)
        }

        var values = IntArray(1)
        LogUtils.d(TAG, "init , mEglDisplay: $mEglDisplay, $mEGLContext")
        EGL14.eglQueryContext(mEglDisplay, mEGLContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0)

        if (DEBUG) {
            Log.d(TAG, "EGLContext created, client version: ${values[0]}")
        }

        makeDefault()
    }

    private fun makeDefault() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            if (DEBUG) {
                Log.w(TAG, "EGL makeDefault failed ${EGL14.eglGetError()}")
            }
        }
    }

    private fun createContext(context: EGLContext): EGLContext {
        val attributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val eglContext = EGL14.eglCreateContext(mEglDisplay, mEglConfig, context, attributes, 0)
        checkEglError("createEGLContext")

        return eglContext
    }

    private fun getConfig(widthDepthBuffer: Boolean, recordable: Boolean): EGLConfig? {
        val attributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE, EGL14.EGL_NONE,
            EGL14.EGL_NONE, EGL14.EGL_NONE,
            EGL14.EGL_NONE, EGL14.EGL_NONE,
            EGL14.EGL_NONE, EGL14.EGL_NONE,
            EGL14.EGL_NONE,
        )

        var offset = 10

        /*if (false) {
            attributes[offset++] = EGL14.EGL_STENCIL_SIZE
            attributes[offset++] = 8
        }*/

        if (widthDepthBuffer) {
            attributes[offset++] = EGL14.EGL_DEPTH_SIZE
            attributes[offset++] = 1
        }

        //  recordable && (Build.VERSION.SDK_INT >= 18)
        if (recordable) {
            attributes[offset++] = EGL_RECORDABLE_ANDROID
            attributes[offset++] = 1
        }

        for (i in attributes.size - 1 downTo offset) {
            attributes[i] = EGL14.EGL_NONE
        }

        val configs = arrayOfNulls<EGLConfig?>(1)
        val numConfigs = IntArray(1)

        if (!EGL14.eglChooseConfig(mEglDisplay, attributes, 0, configs, 0, configs.size, numConfigs, 0)) {
            if (DEBUG) Log.d(TAG, "unable to find RGBA8888 / EGLConfig")

            return null
        }

        return configs[0]
    }

    private fun checkEglError(msg: String) {
        val err = EGL14.eglGetError()

        if (EGL14.EGL_SUCCESS != err) {
            throw RuntimeException("$msg: EGL error: 0x${Integer.toHexString(err)}")
        }
    }

    private fun createWindowSurface(surface: Any): EGLSurface {
        val attrs = intArrayOf(EGL14.EGL_NONE)

        LogUtils.d(TAG, "createWindowSurface, $mEglDisplay  ${mEglConfig} $surface")
        return EGL14.eglCreateWindowSurface(mEglDisplay, mEglConfig, surface, attrs, 0)
    }

    private fun querySurface(eglSurface: EGLSurface, what: Int) : Int{
        val arr = IntArray(1)
        EGL14.eglQuerySurface(mEglDisplay, eglSurface, what, arr, 0)

        return arr[0]
    }

    fun createFromSurface(surface: Any, width: Int, height: Int): EglSurfaceWrapper {
        val eglSurface = EglSurfaceWrapper(this, surface, width, height)
        eglSurface.makeCurrent()

        return eglSurface
    }

    fun release() {
        if (EGL14.EGL_NO_DISPLAY != mEglDisplay) {
            destroyContext()
            EGL14.eglTerminate(mEglDisplay)
            EGL14.eglReleaseThread()
        }

        mEGLContext = EGL14.EGL_NO_CONTEXT
        mEglDisplay = EGL14.EGL_NO_DISPLAY
    }

    private fun destroyContext() {
        if (!EGL14.eglDestroyContext(mEglDisplay, mEGLContext)) {
            Log.d(TAG, "eglDestroyContext error: ${EGL14.eglGetError()}")
        }

        mEGLContext = EGL14.EGL_NO_CONTEXT

        if (EGL14.EGL_NO_CONTEXT != mDefaultEGLContext) {
            if (!EGL14.eglDestroyContext(mEglDisplay, mDefaultEGLContext)) {
                Log.d(TAG, "eglDestroyContext defaultContext, error: ${EGL14.eglGetError()}")
            }

            mDefaultEGLContext = EGL14.EGL_NO_CONTEXT
        }
    }

    private fun swap(eglSurface: EGLSurface?) : Boolean {
        if ((null == eglSurface) || (EGL14.EGL_NO_SURFACE == eglSurface)) {
            Log.e(TAG, "swap with bad eglSurface: $eglSurface")

            return false
        }

        if (!EGL14.eglSwapBuffers(mEglDisplay, eglSurface)) {
            Log.e(TAG, "swap error: ${EGL14.eglGetError()}")

            return false
        }

        return true
    }

    private fun makeCurrent(eglSurface: EGLSurface?): Boolean {
        if (null == mEglDisplay) {
            if (DEBUG) {
                Log.d(TAG, "makeCurrent, eglDisplay not initialized!")
            }
        }

        if ((null == eglSurface) || (EGL14.EGL_NO_SURFACE == eglSurface)) {
            Log.e(TAG, "makeCurrent, makeCurrent with bad eglSurface: $eglSurface")

            return false
        }

        if (!EGL14.eglMakeCurrent(mEglDisplay, eglSurface, eglSurface, mEGLContext)) {
            Log.e(TAG, "eglMakeCurrent error: ${EGL14.eglGetError()}")

            return false
        }

        return true
    }

    private fun destroyWindowSurface(eglSurface: EGLSurface?) {
        if ((null != eglSurface) && (EGL14.EGL_NO_SURFACE != eglSurface)) {
            EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(mEglDisplay, eglSurface)
        }
    }

    class EglSurfaceWrapper {
        private var mEglBase: EGLBase? = null
        private var mEGLSurface: EGLSurface? = null
        private var mWidth = 0
        private var mHeight = 0

        constructor(eglBase: EGLBase, surface: Any) {
            if ((surface !is Surface) && (surface !is SurfaceHolder) && (surface !is SurfaceTexture)) {
                throw RuntimeException("create EglSurface with unsupported surface type: $surface")
            }
            mEglBase = eglBase
            mEGLSurface = eglBase.createWindowSurface(surface)
            mWidth =  eglBase.querySurface(mEGLSurface!!, EGL14.EGL_WIDTH)
            mHeight =  eglBase.querySurface(mEGLSurface!!, EGL14.EGL_HEIGHT)

            if (DEBUG) {
                Log.d(TAG, "EglSurface, size: $mWidth x $mHeight")
            }
        }

        constructor(eglBase: EGLBase, surface: Any, width: Int, height: Int) {
            if ((surface !is Surface) && (surface !is SurfaceHolder) && (surface !is SurfaceTexture)) {
                throw RuntimeException("create EglSurface with unsupported surface type: $surface")
            }
            mEglBase = eglBase
            mEGLSurface = eglBase.createWindowSurface(surface)
            mWidth =  width
            mHeight =  height
        }

        fun makeCurrent() {
            mEglBase?.makeCurrent(mEGLSurface)
        }

        fun swap() {
            mEglBase?.swap(mEGLSurface)
        }

        fun getContext(): EGLContext? = mEglBase?.mEGLContext

        fun getWidth(): Int = mWidth

        fun getHeight(): Int = mHeight

        fun release() {
            mEglBase?.makeDefault()
            mEglBase?.destroyWindowSurface(mEGLSurface)
        }
    }
}