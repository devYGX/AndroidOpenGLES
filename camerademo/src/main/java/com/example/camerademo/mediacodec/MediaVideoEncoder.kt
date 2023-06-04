package com.example.camerademo.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.example.camerademo.utils.LogUtils

class MediaVideoEncoder(muxerWrapper: MediaMuxerWrapper, private val listener: MediaEncoderListener,
                        private val mWidth: Int, private val mHeight: Int) : MediaEncoder(muxerWrapper, listener) {

    companion object {
        const val MIME_TYPE = "video/avc"
        const val FRAME_RATE = 25
        const val BPP = 0.25f

        val RECOGNIZEDFORMATS = intArrayOf(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
    }

    private var mSurface: Surface? = null

    override fun prepare() {
        mTrackIndex = -1
        mbMuxerStarted = false
        mbIsEOS = false

        var codecInfo = selectVideoCodec(MIME_TYPE)

        if (null == codecInfo) {
            LogUtils.e(TAG, "Unable to find an appropriate codec for $MIME_TYPE")

            return
        }

        var videoFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight)
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate())
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10)

        var mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        mMediaCodec = mediaCodec

        mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mSurface = mediaCodec.createInputSurface()
        mediaCodec.start()

        LogUtils.d(TAG, "prepare finishing")

        listener.onPrepared(this)
    }

    private fun calcBitRate(): Int {
        var bitrate = (BPP * FRAME_RATE * mWidth * mHeight).toInt()

        LogUtils.d(TAG, "calcBitRate, bitrate=${bitrate / 1024f / 1024f}[Mbps]")

        return bitrate
    }

    override fun release() {
        mSurface?.release()
        mSurface = null

        super.release()
    }

    override fun signalEndOfInputStream() {
        mMediaCodec?.signalEndOfInputStream()
        mbIsEOS = true
    }

    override fun frameAvailableSoon(): Boolean {
        var result = super.frameAvailableSoon()

        return result
    }

    fun getSurface(): Surface? = mSurface

}