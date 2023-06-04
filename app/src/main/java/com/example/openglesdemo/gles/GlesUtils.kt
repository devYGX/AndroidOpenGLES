package com.example.openglesdemo.gles

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLUtils
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class GlesUtils {

    companion object {

        /**
         * 顶点坐标系
         * A(-1.0, 1.0) ---------- D(1.0, 1.0)
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |      (0.0, 0.0)    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         * B(-1.0, -1.0)-----------C(1.0, -1.0)
         *
         *
         */
        val COORDINATE = floatArrayOf(
            -1.0f, 1.0f, 0.0f, 0.0f,  // A
            -1.0f, -1.0f, 0.0f, 0.0f, // B
            1.0f, -1.0f, 0.0f, 0.0f,  // C
            1.0f, 1.0f, 0.0f, 0.0f,   // D
        )
        val COORDINATE_BUFFER: FloatBuffer by lazy {
            ByteBuffer.allocateDirect(COORDINATE.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(COORDINATE)
                    position(0)
                }
        }

        /**
         * 纹理坐标系
         * A(0.0, 0.0) ---------- D(1.0, 0.0)
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         *        |                    |
         * B(0.0, 1.0)-----------C(1.0, 1.0)
         *
         *
         */
        val TEX_COORDINATE = floatArrayOf(
            0.0f, 0.0f,  // A
            0.0f, 1.0f,  // B
            1.0f, 1.0f,  // C
            1.0f, 0.0f   // D
        )
        val TEX_COORDINATE_BUFFER: FloatBuffer by lazy {
            ByteBuffer.allocateDirect(TEX_COORDINATE.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(TEX_COORDINATE)
                    position(0)
                }
        }

        fun deleteShadersSafety(vararg shaders: Int) {
            for (shader in shaders) {
                if (0 < shader) {
                    GLES20.glDeleteShader(shader)
                }
            }
        }

         fun createShader(context: Context, type: Int, path: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, loadFromAssets(context, path))
            GLES20.glCompileShader(shader)

            if (0 != GLES20.glGetError()) {
                GLES20.glDeleteShader(shader)

                return -1
            }

            return shader
        }

        @JvmStatic
        fun createProgram(vertexShader: Int, fragShader: Int): Int {

            val program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragShader)
            GLES20.glLinkProgram(program)

            if (0 != GLES20.glGetError()) {
                deleteShadersSafety(vertexShader, fragShader)
                GLES20.glDeleteProgram(program)

                return -1
            }

            return program
        }

        @JvmStatic
        fun loadFromAssets(context: Context, path: String): String {
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
        fun bindBitmapToTexture(bmp: Bitmap, texture: Int) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

    }
}