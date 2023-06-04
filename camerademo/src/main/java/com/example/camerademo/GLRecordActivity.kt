package com.example.camerademo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.camerademo.filter.GLSurfaceViewEnv
import com.example.camerademo.utils.AppUtils
import com.example.camerademo.utils.LogUtils
import com.example.camerademo.utils.ToastUtils
import com.example.camerademo.view.TimerTextView
import java.io.File

class GLRecordActivity : AppCompatActivity(), GLSurfaceViewEnv.Observer {

    companion object {
        const val TAG = "MainActivity"
        const val REQUEST_CODE = 103

        val NECESSARY_PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }

    private lateinit var mRecordTimeTextView: TimerTextView
    private lateinit var mRecordButton: ImageButton
    private lateinit var mGlView: GLSurfaceView
    private lateinit var mGLEnv: GLSurfaceViewEnv
    private var mCurImageVideoFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.setActionBarStatusBar(this)

        setContentView(R.layout.activity_glrecord)

        mGlView = findViewById(R.id.glSurfaceView)
        mRecordTimeTextView = findViewById(R.id.textViewRecord)
        mGLEnv = GLSurfaceViewEnv(mGlView)
        mGLEnv.setObserver(this)

        findViewById<ImageButton>(R.id.btnCapture).setOnClickListener {
            mGLEnv.capture()
        }

        mRecordButton = findViewById<ImageButton>(R.id.btnRecord).apply {
            setOnClickListener {
                val recording = (null != it.tag)

                if (recording) {
                    mGLEnv.stopRecording()
                    (it as ImageButton).setImageResource(R.mipmap.ic_rec_normal)
                    it.tag = null
                } else {
                    mGLEnv.startRecording()
                    (it as ImageButton).setImageResource(R.mipmap.ic_rec_red)
                    it.tag = true
                }
            }
        }

        findViewById<ImageButton>(R.id.btnSetting).setOnClickListener {
            ToastUtils.showLongToast(getString(R.string.feature_not_available))
        }

        LogUtils.d(TAG, "onCreate X")
    }

    private fun checkAndRequestPermission() {
        NECESSARY_PERMISSIONS.filter {
            PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, it)
        }.apply {
            if (isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this@GLRecordActivity,
                    toTypedArray(),
                    REQUEST_CODE
                )
            } else {
                mGLEnv.startCamera()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        checkAndRequestPermission()

        LogUtils.d(TAG, "onStart X")
    }

    override fun onResume() {
        super.onResume()

        LogUtils.d(TAG, "onResume X")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (REQUEST_CODE != requestCode) {
            return
        }

        NECESSARY_PERMISSIONS.filter {
            PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(this, it)
        }.apply {
            if (isNotEmpty()) {
                ToastUtils.showLongToast(getString(R.string.request_permission_rejected))
            } else {
                mGLEnv.startCamera()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        mGLEnv.stopRecording()
        mRecordButton.setImageResource(R.mipmap.ic_rec_normal)
        mRecordButton.tag = null
        mRecordTimeTextView.stop()

        LogUtils.d(TAG, "onPause X")
    }

    override fun onStop() {
        super.onStop()

        mGLEnv.closeCamera()

        LogUtils.d(TAG, "onStop X")
    }

    override fun onDestroy() {
        super.onDestroy()

        mGLEnv.onDestroy()

        LogUtils.d(TAG, "onDestroy X")
    }

    override fun onCaptureUpdated(thumbnail: Bitmap, f: File) {
        // empty implement
    }

    override fun onStartRecord() {
        mRecordTimeTextView.start()
    }

    override fun onStopRecord() {
        mRecordTimeTextView.stop()
    }
}