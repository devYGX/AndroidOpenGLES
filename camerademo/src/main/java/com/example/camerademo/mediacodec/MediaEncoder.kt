package com.example.camerademo.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.util.Log
import com.example.camerademo.mediacodec.MediaVideoEncoder.Companion.RECOGNIZEDFORMATS
import java.nio.ByteBuffer

fun tryCatch(runnable: Runnable) {
    try {
        runnable.run()
    } catch (_: Exception) {}
}

abstract class MediaEncoder(private val mediaMuxer: MediaMuxerWrapper, private val listener: MediaEncoderListener) : Runnable{

    companion object {
        const val TAG = "MediaEncoder"
        const val TIMEOUT_USEC = 10000L;	// 10[msec]

        @JvmStatic fun selectAudioCodec(mimeType: String) : MediaCodecInfo?{
            var mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (codecInfo in mediaCodecList.codecInfos) {
                if (!codecInfo.isEncoder) {
                    continue
                }

                for (type in codecInfo.supportedTypes) {
                    if (type.equals(mimeType, true)) {
                        return codecInfo
                    }
                }
            }

            return null
        }

        @JvmStatic fun selectVideoCodec(mimeType: String) : MediaCodecInfo?{
            var mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (codecInfo in mediaCodecList.codecInfos) {
                if (!codecInfo.isEncoder) {
                    continue
                }

                for (type in codecInfo.supportedTypes) {
                    if (type.equals(mimeType, true)) {
                        return codecInfo
                    }
                }
            }

            return null
        }

        fun selectColorFormat(codecInfo: MediaCodecInfo, mimeType: String): Int {
            var result = 0
            var caps : MediaCodecInfo.CodecCapabilities= codecInfo.getCapabilitiesForType(mimeType)

            var colorFormats = caps.colorFormats
            for (i in 0 until colorFormats.size) {
                var colorFmt = colorFormats[i]

                if (RECOGNIZEDFORMATS.any { it == colorFmt }) {
                    result = colorFmt
                }
            }

            if (0 == result) {
                Log.e(
                    TAG, "selectColorFormat, couldn't find a good color format for "
                            + codecInfo.name + " / " + mimeType)
            }

            return result
        }
    }

    private var mMediaCodecBufferInfo: MediaCodec.BufferInfo

    private var mSync = Object()
    protected var mMediaCodec: MediaCodec? = null

    protected var mbMuxerStarted = false
    protected var mbIsEOS = false
    @Volatile protected var mbIsCapturing = false
    @Volatile protected var mbRequestStop = false
    private var mRequestDrain = 0
    private var mPrevOutputPTSUs = 0L
    protected var mTrackIndex: Int = 0

    init {
        mediaMuxer.addEncoder(this)

        synchronized(mSync) {
            Thread(this, javaClass.simpleName).start()
            mMediaCodecBufferInfo = MediaCodec.BufferInfo()

            try {
                mSync.wait()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }
    interface MediaEncoderListener {
        fun onPrepared(encoder: MediaEncoder)

        fun onStopped(encoder: MediaEncoder)
    }

    override fun run() {
        synchronized(mSync) {
            mbRequestStop = false
            mRequestDrain = 0
            mSync.notify()
        }

        var isRunning = true
        var localRequestStop = false
        var localRequestDrain = false

        while (isRunning) {
            synchronized(mSync) {
                localRequestStop = mbRequestStop
                localRequestDrain = (0 < mRequestDrain)

                if (localRequestDrain) {
                    mRequestDrain --
                }
            }


            if (localRequestStop) {
                drain()
                signalEndOfInputStream()
                drain()
                release()

                break
            }

            if (localRequestDrain) {
                drain()
            } else {
                synchronized(mSync) {
                    try {
                        mSync.wait()
                    } catch (e: InterruptedException) {
                        //
                        e.printStackTrace()
                        isRunning = false
                    }
                }
            }
        }

        synchronized(mSync) {
            mbRequestStop = true
            mbIsCapturing = false
        }
    }


    open fun startRecording() {
        synchronized(mSync) {
            mbIsCapturing = true
            mbRequestStop = false
            mSync.notifyAll()
        }
    }

    open fun stopRecording() {
        synchronized(mSync) {
            if (!mbIsCapturing || mbRequestStop) {
                return
            }

            mbRequestStop = true
            mSync.notifyAll()
        }
    }

    protected open fun release() {
        listener.onStopped(this)

        mbIsCapturing = false


        if (null != mMediaCodec) {
            tryCatch{ mMediaCodec?.stop() }
            tryCatch{ mMediaCodec?.release() }

            mMediaCodec = null
        }

        if (mbMuxerStarted) {
            mediaMuxer.stop()

            mbMuxerStarted = false
        }
    }
    protected fun getPTSUs(): Long {
        var result = System.nanoTime() / 1000L

        if (result < mPrevOutputPTSUs) {
            result += (mPrevOutputPTSUs - result)
        }

        return result
    }



    protected abstract fun signalEndOfInputStream()

    // 执行编码任务
    protected open fun drain() {
        var mediaCodec = mMediaCodec?:return

        var outputBuffers = mediaCodec.getOutputBuffers()
        var encoderStatus = 0
        var count = 0

        while (mbIsCapturing) {
            encoderStatus = mediaCodec.dequeueOutputBuffer(mMediaCodecBufferInfo, TIMEOUT_USEC)

            if (MediaCodec.INFO_TRY_AGAIN_LATER == encoderStatus) {
                if (!mbIsEOS && (5 < ++count)) {
                    break
                }
            } else if (MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED == encoderStatus) {
                outputBuffers = mediaCodec.getOutputBuffers()
            } else if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == encoderStatus) {
                if (mbMuxerStarted) {
                    throw RuntimeException("format changed twice")
                }

                var format = mediaCodec.outputFormat
                mTrackIndex = mediaMuxer.addTrack(format)
                mbMuxerStarted = true

                if (!mediaMuxer.start()) {
                    synchronized(mediaMuxer.getSync()) {
                        while (!mediaMuxer.isStarted()) {
                            mediaMuxer.getSync().wait(100)
                        }
                    }
                }
            } else if (0 > encoderStatus) {
                Log.w(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: $encoderStatus")
            } else {
                var encodedBuffer: ByteBuffer = outputBuffers[encoderStatus]
                    ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                if (0 != (mMediaCodecBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG)) {
                    mMediaCodecBufferInfo.size = 0
                }

                if (0 != mMediaCodecBufferInfo.size) {
                    count = 0

                    if (!mbMuxerStarted) {
                        throw RuntimeException("drain:muxer hasn't started")
                    }

                    mMediaCodecBufferInfo.presentationTimeUs = getPTSUs()
                    mediaMuxer.writeSampleData(mTrackIndex, encodedBuffer, mMediaCodecBufferInfo)
                    mPrevOutputPTSUs = mMediaCodecBufferInfo.presentationTimeUs
                }

                mediaCodec.releaseOutputBuffer(encoderStatus, false)

                if (0 != (mMediaCodecBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM)) {
                    mbIsCapturing = false
                }
            }
        }
    }

    abstract fun prepare()

    open fun frameAvailableSoon() : Boolean {
        synchronized(mSync) {
            if (!mbIsCapturing || mbRequestStop) {
                return false
            }

            mRequestDrain ++
            mSync.notifyAll()
        }

        return true
    }
}