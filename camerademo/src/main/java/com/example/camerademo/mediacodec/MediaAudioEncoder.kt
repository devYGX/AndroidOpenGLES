package com.example.camerademo.mediacodec

import android.annotation.SuppressLint
import android.media.*
import android.util.Log
import com.example.camerademo.utils.LogUtils
import java.nio.ByteBuffer

class MediaAudioEncoder(muxerWrapper: MediaMuxerWrapper, private val listener: MediaEncoderListener) :
    MediaEncoder(muxerWrapper, listener) {

    companion object {
        const val TAG = "MediaAudioEncoder"
        const val MIME_TYPE = "audio/mp4a-latm"
        const val SAMPLE_RATE = 44100  // 采样率44.1khz
        const val BIT_RATE = 64000
        const val SAMPLES_PER_FRAME = 1024
        const val FRAMES_PER_BUFFER = 25

        val AUDIO_SOURCES = intArrayOf(
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION)
    }

    private var mTraceIndex = -1
    private var mAudioThread: Thread? = null

    override fun prepare() {
        mTraceIndex = -1
        mbMuxerStarted = false
        mbIsEOS = false

        var codecInfo = selectAudioCodec(MIME_TYPE)

        if (null == codecInfo) {
            LogUtils.d(TAG, "prepare unsupport codec: $MIME_TYPE")

            return
        }

        var audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1)
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        var mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
        mMediaCodec = mediaCodec
        mediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec.start()

        listener.onPrepared(this)
    }

    override fun startRecording() {
        super.startRecording()

        if (null == mAudioThread) {
            mAudioThread = AudioThread("AudioThread").apply { start() }
        }
    }

    override fun release() {
        super.release()

        mAudioThread = null
    }

    override fun signalEndOfInputStream() {
        encode(null, 0, getPTSUs())
    }

    fun encode(buffer: ByteBuffer?, length : Int, presentationTimeUs: Long) {
        if (!mbIsCapturing) {
            return
        }

        var mediaCodec = mMediaCodec?:return
        var inputBuffers = mediaCodec.inputBuffers

        while (mbIsCapturing) {
            var inputBufferIndex = mediaCodec.dequeueInputBuffer(TIMEOUT_USEC)

            if (0 <= inputBufferIndex) {
                var inputBuffer = inputBuffers[inputBufferIndex]
                inputBuffer.clear()

                if (null != buffer) {
                    inputBuffer.put(buffer)
                }

                if (length <= 0) {
                    mbIsEOS = true // end of stream

                    Log.d(javaClass.simpleName, "BUFFER_FLAG_END_OF_STREAM")

                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, presentationTimeUs,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)

                    break
                } else {
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, presentationTimeUs, 0)
                }

                break
            } /*else if (MediaCodec.INFO_TRY_AGAIN_LATER == inputBufferIndex) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            } */
        }
    }
    inner class AudioThread(name: String): Thread(name) {
        private val mLock = Object()

        @SuppressLint("MissingPermission")
        override fun run() {
            // 提高线程优先级
            LogUtils.d(javaClass.simpleName, "AudioThread")

            var cnt:Int = 0

            try {
                var minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
                var bufferSize = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER

                if (bufferSize < minBufferSize) {
                    bufferSize = ((minBufferSize / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2
                }

                var audioRecord: AudioRecord? = null

                audioRecord = initializeAudioRecord(bufferSize)

                if (null == audioRecord) {
                    LogUtils.d(TAG, "AudioThread, failed to initialize AudioRecord, so return!")

                    return
                }

                try{
                    if (mbIsCapturing) {
                        var buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME)
                        var readBytes = 0
                        audioRecord.startRecording()

                        try{
                            while (mbIsCapturing && !mbRequestStop && !mbIsEOS) {
                                buf.clear()
                                readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);

                                if (0 < readBytes) {
                                    buf.position(readBytes)
                                    buf.flip()
                                    encode(buf, readBytes, getPTSUs())
                                    frameAvailableSoon()
                                    cnt++;
                                }
                            }

                            frameAvailableSoon()
                        } finally {
                            audioRecord.stop()
                        }
                    }
                } finally {
                    audioRecord.release()
                }

            } catch (e: Exception) {
                LogUtils.d(TAG, "run with exception: $e")
            }

            if (0 == cnt) {
                val buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME)

                var i = 0
                while (mbIsCapturing && (5 > i)) {
                    i ++

                    buf.position(SAMPLES_PER_FRAME)
                    buf.flip()

                    try {
                        encode(buf, SAMPLES_PER_FRAME, getPTSUs())
                        frameAvailableSoon()
                    } catch (e: Exception) {
                        LogUtils.d(TAG, "cnt is 0, run with exception: $e")

                        break
                    }

                    synchronized(mLock) {
                        try {
                            mLock.wait(50)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun initializeAudioRecord(
            bufferSize: Int
        ): AudioRecord? {
            var audioRecord: AudioRecord? = null
            try {
                for (source in AUDIO_SOURCES) {
                    audioRecord = AudioRecord(
                        source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize
                    )

                    if (AudioRecord.STATE_INITIALIZED == audioRecord.state) {
                        return audioRecord
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }
}