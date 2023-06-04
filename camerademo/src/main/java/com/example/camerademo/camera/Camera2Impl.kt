package com.example.camerademo.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import androidx.core.app.ActivityCompat
import com.example.camerademo.constants.Constants
import com.example.camerademo.constants.PrefKeys
import com.example.camerademo.utils.CameraUtils
import com.example.camerademo.utils.LogUtils
import com.example.camerademo.utils.SharedPrefsHelper
import java.io.File
import java.util.concurrent.Executor

class Camera2Impl(private val mContext: Context) : ICamera {

    companion object {
        const val TAG = "Camera2Impl"

        const val STATE_NONE = 0
        const val STATE_PROCESSING = 1
        const val STATE_SUCCESS = 3
    }

    private var mSt: SurfaceTexture? = null
    private var mSurface: Surface? = null
    private var mCallback: ICamera.Callback? = null
    private var mCameraManager: CameraManager? = null
    private var mCameraDevice: CameraDevice? = null
    private var mCaptureReqBuilder: CaptureRequest.Builder? = null
    private var mSession: CameraCaptureSession? = null
    private var mPreviewSize: Size? = null
    private var mCameraId: String? = null
    private var mImageReader: ImageReader? = null

    private var mCameraState: Int = STATE_NONE
    private var mSessionState: Int = STATE_NONE
    private var mDumpIndex: Int = 0
    private var mbFirstFrameArrived: Boolean = false
    private var mbDestroy: Boolean = false
    private var mbDumpImage = false

    private val mWorkerHandler by lazy {
        Handler(HandlerThread("Camera2WorkHandler").apply { start() }.looper)
    }

    private val mCameraHandler by lazy {
        Handler(HandlerThread("Camera2Handler").apply { start() }.looper)
    }

    private val mImageUploadHandler by lazy {
        Handler(HandlerThread("ImageUploadHandler").apply { start() }.looper)
    }

    override fun setCallback(callback: ICamera.Callback) {
        mCallback = callback
    }

    override fun setSurfaceTexture(st: SurfaceTexture) {
        mSt = st
        createSession()
    }

    private fun prepare() {
        if (mbDestroy) {
            LogUtils.d(TAG, "startCamera, mbDestroy is true, so return")

            return
        }

        if (null != mCameraManager) {
            return
        }

        val cm = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraManager = cm
        val cameraId = SharedPrefsHelper.getSharedPrefs()
            .getString(PrefKeys.CAMERA_ID, Constants.CAMERA_FRONT)!!
        val videoType = SharedPrefsHelper.getSharedPrefs()
            .getString(PrefKeys.VIDEO_TYPE, Constants.VIDEO_TYPE_720P)!!
        val targetSize = CameraUtils.getVideoSize(videoType)
        val cc = cm.getCameraCharacteristics(cameraId)
        val previewSize = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(SurfaceHolder::class.java).also {
                it?.forEach {
                    LogUtils.d(TAG, "prepare size: $it")
                }
            }
            ?.find {
                (targetSize.height == it.height) && (targetSize.width == it.width)
            }

        mPreviewSize = previewSize
        mCameraId = cameraId

        if (null == previewSize) {
            mCallback?.onError("UnSupported Video Type: $videoType")
        } else {
            mCallback?.onPrepared(mPreviewSize!!)
        }
    }

    override fun startCamera() {
        if (mbDestroy) {
            LogUtils.d(TAG, "startCamera, mbDestroy is true, so return")

            return
        }

        LogUtils.d(TAG, "startCamera, state: $mCameraState")

        if (STATE_NONE != mCameraState) {
            return
        }

        mWorkerHandler.post {
            if (STATE_NONE != mCameraState) {
                return@post
            }

            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                    mContext,
                    Manifest.permission.CAMERA
                )
            ) {
                LogUtils.d(TAG, "startCamera, Camera Permission DENIED, so return!")

                return@post
            }

            mCameraState = STATE_PROCESSING
            prepare()
            mCameraManager!!.openCamera(mCameraId!!, mOpenCameraStateCallback, mCameraHandler)

            LogUtils.d(TAG, "startCamera, $mCameraManager, $mCameraId, $mCameraHandler")
        }
    }

    override fun dumpImage(flag: Boolean) {
        if (flag) {
            mDumpIndex = 0
        }

        mbDumpImage = flag
    }
    override fun closeCamera() {
        if ((null != mSession) || (null != mCameraDevice)) {
            LogUtils.d(TAG, "closeCamera")

            mSession?.close()
            mSession = null

            mCameraDevice?.close()
            mCameraDevice = null

            mCameraState = STATE_NONE
            mSessionState = STATE_NONE

            LogUtils.d(TAG, "closeCamera X")
        }
    }

    override fun destroy() {
        mbDestroy = true

        mWorkerHandler.looper.quitSafely()
        mCameraHandler.looper.quitSafely()
        mImageUploadHandler.looper.quitSafely()
        mSurface?.release()
        mImageReader?.close()
        mImageReader = null
        mSt = null
        mCallback = null
        mSurface = null
    }

    private fun createSession() {
        mWorkerHandler.post {
            if ((null == mCameraDevice) || (null == mSt)) {
                return@post
            }

            if (STATE_NONE != mSessionState) {
                return@post
            }

            LogUtils.d(
                TAG,
                "createSession.setDefaultBufferSize: ${mPreviewSize?.width}x${mPreviewSize?.height}  $mSt"
            )

            mSessionState = STATE_PROCESSING
            mSt!!.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)



            if (null == mSurface) {
                val surface = Surface(mSt)
                mSurface = surface
            }

            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureBuilder.addTarget(mSurface!!)
            captureBuilder.addTarget(getImageReader().surface)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val outConfig = OutputConfiguration(mSurface!!)
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    mutableListOf(outConfig, OutputConfiguration(getImageReader().surface)),
                    mSessionExecutor, mSessionStateCallback
                )
                mCameraDevice!!.createCaptureSession(sessionConfig)
            } else {
                mCameraDevice!!.createCaptureSession(
                    mutableListOf(mSurface!!, getImageReader().surface), mSessionStateCallback,
                    mCameraHandler
                )
            }
            mCaptureReqBuilder = captureBuilder

            LogUtils.d(TAG, "call createCaptureSession")
        }
    }

    private fun getImageReader(): ImageReader {
        if (null == mImageReader) {
            mImageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({
                    val image = it.acquireLatestImage()

                    if (mbDumpImage) {
                        val file = File(mContext.getExternalFilesDir("nv21"), "${mDumpIndex}_${image.width}x${image.height}.nv21")
                        // AppUtils.saveImage(file, image)
                        mDumpIndex ++
                        mbDumpImage = false

                        LogUtils.d(TAG, "onImageAvailable, saveImage: ${file.absolutePath}")
                    }

                    image.close()

                }, mImageUploadHandler)
            }
        }

        return mImageReader!!
    }

    private var mSessionExecutor = Executor { command -> mCameraHandler.post { command.run() } }

    private var mSessionCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)

            if (!mbFirstFrameArrived) {
                mbFirstFrameArrived = true

                LogUtils.d(TAG, "mSessionCaptureCallback, onCaptureCompleted, first frame arrived")
            }
        }
    }
    private var mSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            LogUtils.d(TAG, "onConfigured")

            mSessionState = STATE_SUCCESS
            mSession = session
            mbFirstFrameArrived = false
            session.setRepeatingBurst(
                mutableListOf(mCaptureReqBuilder!!.build()),
                mSessionCaptureCallback,
                mCameraHandler
            )
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            mCallback?.onError("create session failed!")

            mSurface = null
            mCaptureReqBuilder = null
            mSessionState = STATE_NONE

        }

    }
    private var mOpenCameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            createSession()

            LogUtils.d(TAG, "mOpenCameraStateCallback, onOpened")
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraState = STATE_NONE

            LogUtils.d(TAG, "mOpenCameraStateCallback, onDisconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraState = STATE_NONE

            LogUtils.d(TAG, "mOpenCameraStateCallback, onError")
        }
    }
}