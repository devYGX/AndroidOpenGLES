package com.example.camerademo.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class MediaMuxerWrapper(private val outputFile: File) {

    companion object {
        const val TAG = "MediaMuxerWrapper"
    }

    private val mSync = Object()
    private var mbIsStarted = false
    private var mStartedCount: Int = 0
    private var mEncoderCount: Int = 0

    private var mVideoEncoder: MediaVideoEncoder? = null
    private var mAudioEncoder: MediaAudioEncoder? = null

    private val mMediaMuxer: MediaMuxer by lazy {
        MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun getSync(): Object = mSync

    fun addEncoder(encoder: MediaEncoder) {
        if (encoder is MediaVideoEncoder) {
            if (null != mVideoEncoder) {
                throw RuntimeException("set VideoEncoder repeat")
            }

            mVideoEncoder = encoder
        } else if (encoder is MediaAudioEncoder){
            if (null != mAudioEncoder) {
                throw RuntimeException("set AudioEncoder repeat")
            }

            mAudioEncoder = encoder
        } else {
            throw RuntimeException("unsupported encoder")
        }

        var videoEncode = if (null != mVideoEncoder) 1 else 0
        var audioEncode = if (null != mAudioEncoder) 1 else 0

        mEncoderCount = audioEncode + videoEncode
    }

    fun addTrack(format: MediaFormat) : Int{
        if (mbIsStarted) {
            throw RuntimeException("muxer already started")
        }

        var trackId = mMediaMuxer.addTrack(format)

        Log.d(TAG, "addTrack, trackNum: $mEncoderCount, trackId: $trackId, format: $format")

        return trackId
    }

    fun writeSampleData(trackIndex: Int, buffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        synchronized(mSync) {
            if (0 < mStartedCount) {
                mMediaMuxer.writeSampleData(trackIndex, buffer, bufferInfo)
            }
        }
    }

    fun stop() {
        synchronized(mSync) {
            mStartedCount --

            if ((0 < mEncoderCount) && (0 >= mStartedCount)) {
                mMediaMuxer.stop()
                mMediaMuxer.release()
                mbIsStarted = false
                mSync.notifyAll()
            }
        }
    }

    fun start(): Boolean {
        synchronized(mSync) {
            mStartedCount ++

            if ((0 < mEncoderCount) && (mStartedCount == mEncoderCount)) {
                mMediaMuxer.start()
                mbIsStarted = true
                mSync.notifyAll()
            }
        }

        return mbIsStarted
    }

    fun prepare() {
        mAudioEncoder?.prepare()
        mVideoEncoder?.prepare()
    }

    fun isStarted(): Boolean = mbIsStarted

    fun startRecording() {
        mAudioEncoder?.startRecording()
        mVideoEncoder?.startRecording()
    }

    fun stopRecording() {
        mAudioEncoder?.stopRecording()
        mVideoEncoder?.stopRecording()
    }

    fun getOutputFile(): File = outputFile
}