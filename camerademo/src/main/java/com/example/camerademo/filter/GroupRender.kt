package com.example.camerademo.filter

import android.util.Size
import com.example.camerademo.render.ExternalRender
import com.example.camerademo.utils.LogUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 从相机取出来的数据是上下翻转的
 */
class GroupRender(env: IGLEnv) : AbsRender(env) {

    // 用于从相机导出数据的
    private var mOesRender: OesRender
    // 用于抓拍的
    private var mCaptureRender: CaptureFilter
    // 水印
    private var mWatermarkFilter: WatermarkFilter
    // 用于绘制到屏幕上的
    private var mDrawRender: DrawRender
    // filter, 可以自定义一些特效
    private var mFilters = ArrayList<AbsFilter>()
    // 用于提供给外部进行渲染的；比如想再
    private var mExternalRenders = ArrayList<ExternalRender>()

    private var mbCapture: Boolean = false

    init {
        mOesRender = OesRender(env, true)
        mCaptureRender = CaptureFilter(env)
        mWatermarkFilter = WatermarkFilter(env)
        mDrawRender = DrawRender(env)
    }

    fun setSurfaceTextureListener(listener: OesRender.Listener) {
        mOesRender.setSurfaceTextureListener(listener)
    }

    fun setCaptureCallback(callback: CaptureFilter.CaptureCallback) {
        mCaptureRender.setCaptureCallback(callback)
    }

    fun setActivityIsLandscape(landscape: Boolean) {
        mOesRender.setActivityIsLandscape(landscape)
    }

    override fun setDisplayScaleType(scaleType: Int) {
        mOesRender.setDisplayScaleType(scaleType)
        mFilters.forEach { it.setDisplayScaleType(scaleType) }
        mCaptureRender.setDisplayScaleType(scaleType)
        mWatermarkFilter.setDisplayScaleType(scaleType)
        mDrawRender.setDisplayScaleType(scaleType)
    }

    override fun setDataSize(source: Size) {
        super.setDataSize(source)
        mOesRender.setDataSize(source)

        mFilters.forEach { it.setDataSize(source) }

        mCaptureRender.setDataSize(source)
        mWatermarkFilter.setDataSize(source)
        mDrawRender.setDataSize(source)
    }

    override fun isAllowCreateProgram(): Boolean = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)

        mOesRender.onSurfaceCreated(gl, config)

        mFilters.forEach { it.onSurfaceCreated(gl, config) }

        mCaptureRender.onSurfaceCreated(gl, config)
        mWatermarkFilter.onSurfaceCreated(gl, config)
        mDrawRender.onSurfaceCreated(gl, config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)

        mOesRender.onSurfaceChanged(gl, width, height)

        mFilters.forEach { it.onSurfaceChanged(gl, width, height) }

        mCaptureRender.onSurfaceChanged(gl, width, height)
        mWatermarkFilter.onSurfaceChanged(gl, width, height)
        mDrawRender.onSurfaceChanged(gl, width, height)

        LogUtils.d(TAG, "onSurfaceChanged, mOesRender.supportFBO: ${mOesRender.supportFBO()}")
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)

        val sourceSize = getSourceSize()

        mOesRender.onDrawFrame(gl)

        if (mOesRender.supportFBO()) {
            var texId = mOesRender.getOutputTexture()

            mFilters.forEach {
                it.setInputTexture(texId)
                it.onDrawFrame(gl)
                val outTex = it.getOutputTexture()

                if (-1 != outTex) {
                    texId = outTex
                }
            }

            var noWatermarkTexId = texId

            mWatermarkFilter.setInputTexture(texId)
            mWatermarkFilter.onDrawFrame(gl)
            texId = mWatermarkFilter.getOutputTexture()

            if (mbCapture) {
                mbCapture = false

                mCaptureRender.setInputTexture(noWatermarkTexId)
                mCaptureRender.onDrawFrame(gl)
            }

            drawExternalRender(texId, sourceSize?.width?:-1, sourceSize?.height?:-1)
            mDrawRender.setInputTexture(texId)
            mDrawRender.onDrawFrame(gl)
        }
    }

    private fun drawExternalRender(texId: Int, width: Int, height: Int) {
        if (0 == mExternalRenders.size) {
            return
        }

        if ((-1 == width) || (-1 == height)) {
            return
        }

        synchronized(mExternalRenders) {
            for (render in mExternalRenders) {
                when(render.getState()) {
                    ExternalRender.STATE_NONE -> {
                        render.onCreated()
                        render.onPrepared(width, height)
                    }

                    ExternalRender.STATE_CREATED -> {
                        render.onPrepared(width, height)
                    }
                }

                render.draw(texId)
            }
        }
    }

    fun capture() {
        mbCapture = true

        LogUtils.d(TAG, "capture")
    }

    fun addExternalRender(r: ExternalRender) {
        synchronized(mExternalRenders) {
            mExternalRenders.add(r)
        }
    }
    fun removeExternalRender(r: ExternalRender) {
        synchronized(mExternalRenders) {
            mExternalRenders.remove(r)
        }
    }

    override fun onCreated() { //nothing
    }

    override fun onDraw() {//nothing
    }

    override fun onRelease() {//nothing
        mExternalRenders.forEach {
            it.onDestroy()
        }
    }

    override fun supportFBO(): Boolean = false
}