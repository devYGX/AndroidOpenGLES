package com.example.camerademo.utils

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GlesUtils {

    companion object {
        const val TAG = "GlUtils"
        const val TYPE_FITXY = 0
        const val TYPE_CENTERCROP = 1
        const val TYPE_CENTERINSIDE = 2
        const val TYPE_FITSTART = 3
        const val TYPE_FITEND = 4
        const val TYPE_FITCENTER = 5

        const val TYPE_CAPTURE_SOURCE = 0
        const val TYPE_CAPTURE_SURFACE = 1

        val SOURCE_MATRIX = floatArrayOf(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        )

        var COORDINATE = floatArrayOf(
            -1f, 1f, 0.0f, 0.0f,
            -1f, -1f, 0.0f, 0.0f,
            1f, -1f, 0.0f, 0.0f,
            1f, 1f, 0.0f, 0.0f,
        )
        var TEX_COORDINATE = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 1f,
            1f, 0f
        )

        val COORDINATE_BUFFER by lazy {
            ByteBuffer.allocateDirect(COORDINATE.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(COORDINATE)
                    position(0)
                }
        }

        val TEX_COORDINATE_BUFFER by lazy {
            ByteBuffer.allocateDirect(TEX_COORDINATE.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer().apply {
                    put(TEX_COORDINATE)
                    position(0)
                }
        }

        const val INVALID_SHADER = -1
        const val INVALID_PROGRAM = -2
        const val INVALID_BITMAP = -3

        private fun deleteShadersSafety(vararg shaders: Int) {
            for (shader in shaders) {
                if (0 < shader) {
                    GLES20.glDeleteShader(shader)
                }
            }
        }

        private fun createShader(type: Int, code: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, code)
            GLES20.glCompileShader(shader)

            if (0 != GLES20.glGetError()) {
                GLES20.glDeleteShader(shader)

                return INVALID_SHADER
            }

            return shader
        }

        @JvmStatic
        fun createProgram(vertexCode: String, fragCode: String): Int {
            val vertexShader = createShader(GLES20.GL_VERTEX_SHADER, vertexCode)

            if (INVALID_SHADER == vertexShader) {
                return INVALID_SHADER
            }

            val fragShader = createShader(GLES20.GL_FRAGMENT_SHADER, fragCode)

            if (INVALID_SHADER == fragShader) {
                return INVALID_SHADER
            }

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragShader)
            GLES20.glLinkProgram(program)

            if (0 != GLES20.glGetError()) {
                deleteShadersSafety(vertexShader, fragShader)
                GLES20.glDeleteProgram(program)

                return INVALID_PROGRAM
            }

            return program
        }

        @JvmStatic
        fun loadShaderCodeFromAssets(context: Context, path: String): String {
            val byteArrayOutputStream = ByteArrayOutputStream()

            context.assets.open(path).use {
                val buffer = ByteArray(1024)
                var read = it.read(buffer)

                while (-1 != read) {
                    byteArrayOutputStream.write(buffer, 0, read)
                    read = it.read(buffer)
                }
            }

            return byteArrayOutputStream.toString()
        }

        @JvmStatic
        fun glGetUniformLocation(program: Int, name: String): Int {
            val location = GLES20.glGetUniformLocation(program, name)

            if (0 > location) {
                throw RuntimeException("glGetUniformLocation failed with name: $name")
            }

            return location
        }

        @JvmStatic
        fun glGetAttribLocation(program: Int, name: String): Int {
            val location = GLES20.glGetAttribLocation(program, name)

            if (0 > location) {
                throw RuntimeException("glGetAttribLocation failed with name: $name")
            }

            return location
        }

        @JvmStatic
        fun createTextureIdByBitmap(textures: IntArray, bmp: Bitmap, autoRecycle: Boolean): Int {
            if (bmp.isRecycled) {
                return INVALID_BITMAP
            }

            GLES20.glGenTextures(1, textures, 0)
            bindTextureIdBitmap(bmp, textures[0], autoRecycle)

            return textures[0]
        }

        @JvmStatic
        fun bindTextureIdBitmap(bmp: Bitmap, textureId: Int, autoRecycle: Boolean) {

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
            )
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            if (autoRecycle)
                bmp.recycle()
        }

        @JvmStatic
        fun createExternalTextureId(): Int {
            val texture = IntArray(1)
            GLES20.glGenTextures(1, texture, 0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0])
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
            )

            // 这应该是和前面的Parameteri  GL_LINEAR_MIPMAP_LINEAR或者GL_NEAREST_MIPMAP_LINEAR 等设置项有关系
            //
            // GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

            return texture[0]
        }

        fun bindFrameBuffer(frameId: Int, renderId: Int, texId: Int) {
            // 绑定缓冲区
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameId)
            // 缓冲区绑定到输出纹理上
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texId, 0
            )
            GLES20.glFramebufferRenderbuffer(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderId
            )
        }

        fun unbindFrameBuffer() {
            // maybe not need
            GLES20.glFramebufferRenderbuffer(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, 0
            )
            // maybe not need
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, 0, 0
            )

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        }

        @JvmStatic
        fun getMatrix(matrix: FloatArray?, type: Int, imgWidth: Int, imgHeight: Int, viewWidth: Int,
                      viewHeight: Int) {
            if ((0 > imgWidth) || (0 > imgHeight) || (0 > viewWidth) || (0 > viewHeight)) {
                return
            }

            val projection = FloatArray(16)
            val camera = FloatArray(16)

            if (type == TYPE_FITXY) {
                Matrix.orthoM(projection, 0, -1f, 1f, -1f, 1f, 1f, 3f)
                Matrix.setLookAtM(camera, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
                Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0)

                return
            }

            val sWhView = viewWidth.toFloat() / viewHeight
            val sWhImg = imgWidth.toFloat() / imgHeight
            if (sWhImg > sWhView) {
                when (type) {
                    // Image的宽肯定比View的大；因此
                    TYPE_CENTERCROP -> Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f, 1f, 3f)
                    TYPE_FITCENTER -> Matrix.orthoM(projection, 0, -1f, 1f,-sWhImg / sWhView, sWhImg/sWhView, 1f, 3f)
                    TYPE_CENTERINSIDE -> Matrix.orthoM(projection, 0, -1f, 1f, -sWhImg / sWhView, sWhImg / sWhView, 1f, 3f)
                    TYPE_FITSTART -> Matrix.orthoM(projection, 0, -1f, 1f, 1 - 2 * sWhImg / sWhView, 1f, 1f, 3f)
                    TYPE_FITEND -> Matrix.orthoM(projection, 0, -1f, 1f, -1f, 2 * sWhImg / sWhView - 1, 1f, 3f)
                }
            } else {
                when (type) {
                    TYPE_CENTERCROP -> Matrix.orthoM(projection, 0, -1f, 1f, -sWhImg / sWhView, sWhImg / sWhView, 1f, 3f)
                    TYPE_FITCENTER -> Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f,  1f, 3f)
                    TYPE_CENTERINSIDE -> Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f, 1f, 3f)
                    TYPE_FITSTART -> Matrix.orthoM(projection, 0, -1f, 2 * sWhView / sWhImg - 1, -1f, 1f, 1f, 3f)
                    TYPE_FITEND -> Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1f, -1f, 1f, 1f, 3f)
                }
            }

            Matrix.setLookAtM(camera, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0)
        }

        @JvmStatic
        fun getMatrix(matrix: FloatArray?, inputMatrix: FloatArray, type: Int, imgWidth: Int, imgHeight: Int, viewWidth: Int,
                      viewHeight: Int) {
            if ((0 > imgWidth) || (0 > imgHeight) || (0 > viewWidth) || (0 > viewHeight)) {
                return
            }

            val projection = FloatArray(16)
            val camera = FloatArray(16)

            if (type == TYPE_FITXY) {
                Matrix.orthoM(projection, 0, -1f, 1f, -1f, 1f, 1f, 3f)
                Matrix.setLookAtM(camera, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)
                Matrix.multiplyMM(matrix, 0, camera, 0, inputMatrix, 0)
                Matrix.multiplyMM(matrix, 0, projection, 0, matrix, 0)

                return
            }

            val sWhView = viewWidth.toFloat() / viewHeight
            val sWhImg = imgWidth.toFloat() / imgHeight
            if (sWhImg > sWhView) {
                when (type) {
                    // Image的宽肯定比View的大；因此
                    TYPE_CENTERCROP -> Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f, 1f, 3f)
                    TYPE_FITCENTER -> Matrix.orthoM(projection, 0, -1f, 1f,-sWhImg / sWhView, sWhImg/sWhView, 1f, 3f)
                    TYPE_CENTERINSIDE -> Matrix.orthoM(projection, 0, -1f, 1f, -sWhImg / sWhView, sWhImg / sWhView, 1f, 3f)
                    TYPE_FITSTART -> Matrix.orthoM(projection, 0, -1f, 1f, 1 - 2 * sWhImg / sWhView, 1f, 1f, 3f)
                    TYPE_FITEND -> Matrix.orthoM(projection, 0, -1f, 1f, -1f, 2 * sWhImg / sWhView - 1, 1f, 3f)
                }
            } else {
                when (type) {
                    TYPE_CENTERCROP -> Matrix.orthoM(projection, 0, -1f, 1f, -sWhImg / sWhView, sWhImg / sWhView, 1f, 3f)
                    TYPE_FITCENTER -> Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f,  1f, 3f)
                    TYPE_CENTERINSIDE -> Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1f, 1f, 1f, 3f)
                    TYPE_FITSTART -> Matrix.orthoM(projection, 0, -1f, 2 * sWhView / sWhImg - 1, -1f, 1f, 1f, 3f)
                    TYPE_FITEND -> Matrix.orthoM(projection, 0, 1 - 2 * sWhView / sWhImg, 1f, -1f, 1f, 1f, 3f)
                }
            }

            Matrix.setLookAtM(camera, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f)

            Matrix.multiplyMM(matrix, 0, camera, 0, inputMatrix, 0)
            Matrix.multiplyMM(matrix, 0, projection, 0, matrix, 0)
        }
    }
}