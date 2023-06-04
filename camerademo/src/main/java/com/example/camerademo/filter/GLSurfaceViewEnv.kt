package com.example.camerademo.filter

import android.content.Context
import android.graphics.*
import android.opengl.GLSurfaceView
import android.util.Size
import android.view.Surface
import com.example.camerademo.camera.Camera2Impl
import com.example.camerademo.camera.ICamera
import com.example.camerademo.mediacodec.MediaCodecRecorder
import com.example.camerademo.mediacodec.MediaEncoder
import com.example.camerademo.recorder.RecordRender
import com.example.camerademo.utils.LogUtils
import com.example.camerademo.utils.ThreadPool
import com.example.camerademo.utils.ToastUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class GLSurfaceViewEnv(private val glView: GLSurfaceView) : IGLEnv {
    companion object {
        const val TAG = "GLSurfaceViewEnv"
    }

    private var mCamera: ICamera? = null

    private var mRecordSync = Object()
    private var mRender: GroupRender? = null
    private var mRecordRender: RecordRender? = null
    private var mRecorder: MediaCodecRecorder? = null
    private var mObserver: Observer?  = null
    private var mbActivityLandscape = true
    private var mbDestroy = false


    private var mRecordListener = object : MediaCodecRecorder.Listener {
        override fun onPrepared(encoder: MediaEncoder, surface: Surface) {
            LogUtils.d(TAG, "Record Prepared $surface")

            val render = RecordRender.createRender(glView.context, encoder, surface)
            mRecordRender = render
            mRender?.addExternalRender(render)

            mRecorder?.startRecording()
            mObserver?.onStartRecord()
        }

        override fun onStopped(output: File?) {
            mRender?.removeExternalRender(mRecordRender!!)
            mRecordRender = null
            mObserver?.onStopRecord()

            ToastUtils.showLongToast("Stop Recording: ${output?.name}")

            LogUtils.d(TAG, "Record Stopped: ${output?.absolutePath}")
        }
    }
    private var mListener = object : OesRender.Listener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture) {
            mCamera?.setSurfaceTexture(surface)
        }
    }

    private var mCaptureCallback = object : CaptureFilter.CaptureCallback {
        override fun onCapture(buffer: ByteBuffer, width: Int, height: Int) {
            val fboBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(buffer)
            }

            ThreadPool.execute {
                val captureDir = glView.context.getExternalFilesDir("capture")
                val file = File(captureDir, "${System.currentTimeMillis()}.png")

                ToastUtils.showLongToast("Capture Success, ${file.name}")

                LogUtils.d(TAG, "Capture Success! ${file.name}")

                FileOutputStream(file).use {
                    fboBmp.compress(Bitmap.CompressFormat.PNG, 100, it);
                    it.close()
                }

                var bmp: Bitmap
                var rect: Rect

                if (fboBmp.width > fboBmp.height) {
                    bmp = Bitmap.createBitmap(fboBmp.height, fboBmp.height, fboBmp.config)
                    rect = Rect(fboBmp.width / 2 - fboBmp.height / 2,
                        0,
                        fboBmp.width / 2 + fboBmp.height / 2,
                        fboBmp.height,
                    )
                } else {
                    bmp = Bitmap.createBitmap(fboBmp.width, fboBmp.width, fboBmp.config)
                    rect = Rect(0,
                        fboBmp.height / 2 - fboBmp.width / 2,
                        fboBmp.width,
                        fboBmp.height / 2 + fboBmp.width / 2,
                    )
                }

                var canvas = Canvas(bmp)
                canvas.drawBitmap(fboBmp, rect, Rect(0, 0, bmp.width, bmp.height),
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })

                mObserver?.onCaptureUpdated(bmp, file)
            }
        }
    }

    private var mCameraCallback = object : ICamera.Callback {
        override fun onError(msg: String) {
            LogUtils.d(TAG, "")
        }

        override fun onPrepared(size: Size) {
            if (!mbActivityLandscape) {
                mRender?.setDataSize(Size(size.height, size.width))
            } else {
                mRender?.setDataSize(Size(size.width, size.height))
            }
        }
    }

    init {
        mCamera = Camera2Impl(glView.context)
        glView.setEGLContextClientVersion(2)
        mRender = GroupRender(this)
        glView.setRenderer(mRender)
        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        mCamera?.setCallback(mCameraCallback)

        mRender?.apply {
            setSurfaceTextureListener(mListener)
            setCaptureCallback(mCaptureCallback)
            setActivityIsLandscape(mbActivityLandscape)
        }
    }

    override fun getContext(): Context = glView.context

    override fun requestRender() {
        glView.requestRender()
    }

    override fun queueEvent(runnable: Runnable) {
        glView.queueEvent(runnable)
    }

    fun capture() {
        if (mbDestroy) {
            LogUtils.d(TAG, "capture, mbDestroy is true, so return!")

            return
        }

        mRender?.capture()
    }

    fun startRecording() {
        if (mbDestroy) {
            LogUtils.d(TAG, "startRecording, mbDestroy is true, so return!")

            return
        }

        if (null == mRender) {
            LogUtils.d(TAG, "startRecording failed, may be camera is not open")

            return
        }

        synchronized(mRecordSync) {
            if (null != mRecorder) {
                LogUtils.d(TAG, "call startRecording repeat!")

                return
            }

            mRecorder = MediaCodecRecorder()
            val captureDir = glView.context.getExternalFilesDir("capture")
            val file = File(captureDir, "${System.currentTimeMillis()}.mp4")
            val size = mRender!!.getSourceSize()!!
            mRecorder!!.prepare(file, size.width, size.height, mRecordListener)
            mCamera?.dumpImage(true)

            LogUtils.d(TAG, "startRecording")
        }
    }

    fun stopRecording() {
        if (null == mRecorder) {
            return
        }

        synchronized(mRecordSync) {
            if (null == mRecorder) {
                return
            }

            mCamera?.dumpImage(false)
            mRecorder!!.stopRecording()
            mRecorder = null
        }
    }

    fun startCamera() {
        if (mbDestroy) {
            LogUtils.d(TAG, "startCamera, mbDestroy is true, so return!")

            return
        }

        mCamera?.startCamera()
    }

    fun closeCamera() {
        mCamera?.closeCamera()
    }

    fun onDestroy() {
        stopRecording()
        closeCamera()

        mbDestroy = true
        mRender?.destroy()
        mRecordRender?.release()
        mCamera?.destroy()
        mRecorder?.release()

        mRender = null
        mRecordRender = null
        mCamera = null
        mRecorder = null
    }

    fun setObserver(observer: Observer) {
        mObserver = observer
    }
    interface Observer {
        fun onCaptureUpdated(thumbnail: Bitmap, f: File)

        fun onStartRecord()

        fun onStopRecord()
    }
}