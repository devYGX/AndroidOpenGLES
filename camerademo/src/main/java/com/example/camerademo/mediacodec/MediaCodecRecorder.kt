package com.example.camerademo.mediacodec

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import com.example.camerademo.utils.LogUtils
import java.io.File

class MediaCodecRecorder {
    companion object {
        const val TAG = "MediaCodecRecorder"
    }

    private var mMediaMuxerWrapper: MediaMuxerWrapper? = null
    private var mVideoEncoder: MediaVideoEncoder? = null
    private var mListener: Listener? = null
    private var mbPrepared = false
    private var mbVideoPrepared = false
    private var mbAudioPrepared = false
    private var mbReleased = false

    private var mbVideoStoped = false
    private var mbAudioStoped = false
    private var mSurface: Surface? = null

    private val mRecordHandler by lazy {
        Handler(HandlerThread(TAG).apply { start() }.looper)
    }

    private var mEncoderListener = object : MediaEncoder.MediaEncoderListener{
        override fun onPrepared(encoder: MediaEncoder) {
            if (encoder is MediaVideoEncoder) {
                mSurface = encoder.getSurface()
                mbVideoPrepared = true
                mVideoEncoder = encoder
            } else if (encoder is MediaAudioEncoder) {
                mbAudioPrepared = true
            }

            if (mbAudioPrepared && mbVideoPrepared) {
                mListener?.onPrepared(mVideoEncoder!!, mSurface!!)
            }

            LogUtils.d(TAG, "onPrepared")
        }

        override fun onStopped(encoder: MediaEncoder) {
            if (encoder is MediaVideoEncoder) {
                mbVideoStoped = true
            } else if (encoder is MediaAudioEncoder) {
                mbAudioStoped = true
            }

            if (mbAudioStoped && mbVideoStoped) {
                mListener?.onStopped(mMediaMuxerWrapper?.getOutputFile()?:null)
            }

            LogUtils.d(TAG, "onStopped")
        }
    }

    fun prepare(outputFile: File, width: Int, height: Int, listener: Listener) {
        if (mbReleased) {
            Log.w(TAG, "prepare record already release, so return!")

            return
        }

        if (mbPrepared) {
            return
        }

        mRecordHandler.post {
            if (mbPrepared) {
                return@post
            }

            mListener = listener
            val mediaMuxerWrapper = MediaMuxerWrapper(outputFile)
            mMediaMuxerWrapper = mediaMuxerWrapper
            var videoEncoder = MediaVideoEncoder(mediaMuxerWrapper, mEncoderListener, width, height)
            var audioEncoder = MediaAudioEncoder(mediaMuxerWrapper, mEncoderListener)

            mediaMuxerWrapper.prepare()
            mbPrepared = true

            LogUtils.d(TAG, "prepare")
        }
    }

    fun startRecording() {
        if (mbReleased) {
            Log.w(TAG, "startRecording, recorder already release, so return!")

            return
        }

        mRecordHandler.post {
            mMediaMuxerWrapper?.startRecording()
        }
    }

    fun stopRecording() {
        if (mbReleased) {
            Log.w(TAG, "startRecording, recorder already release, so return!")

            return
        }

        mRecordHandler.post {
            mMediaMuxerWrapper?.stopRecording()
        }
    }

    fun release() {
        mbReleased = true
        mbPrepared = false
        mMediaMuxerWrapper = null
        mRecordHandler.looper.quitSafely()
    }

    interface Listener{
        fun onPrepared(encoder: MediaEncoder, surface : Surface)

        fun onStopped(output :File?)
    }
}