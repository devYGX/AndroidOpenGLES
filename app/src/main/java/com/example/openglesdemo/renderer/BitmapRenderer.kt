package com.example.openglesdemo.renderer

import android.content.Context
import android.graphics.*
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.core.content.res.ResourcesCompat
import com.example.openglesdemo.R
import com.example.openglesdemo.gles.GlesUtils
import com.example.openglesdemo.gles.TextureId
import com.example.openglesdemo.utils.LogUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BitmapRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "TriangleRenderer"

        private val COORDINATE = floatArrayOf(
            -1.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f, 0.0f
        )

        private val COORDINATE_BUFFER by lazy {
            ByteBuffer.allocateDirect(COORDINATE.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(COORDINATE)
                    position(0)
                }
        }

        private val TEX_COORDINATE = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
        )

        private val TEX_COORDINATE_BUFFER by lazy {
            ByteBuffer.allocateDirect(TEX_COORDINATE.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(TEX_COORDINATE)
                    position(0)
                }
        }

        private val COLOR = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    }

    private var mVertexMatrix: FloatArray = FloatArray(16)
    private var mSourceMatrix: FloatArray = FloatArray(16)

    private var mVertexShader: Int = -1
    private var mFragShader: Int = -1
    private var mProgram: Int = -1

    private var mAttrPosition: Int = -1
    private var mAttrTexPosition: Int = -1
    private var mUniformMatrix: Int = -1
    private var mUniformSampler2D: Int = -1

    private var mBitmapTextureArray = IntArray(1)
    private var mBmpRatio: Float = 0f

    private var mIconTextureId: TextureId? = null
    private var mTextTextureId: TextureId? = null
    private var mWidth: Int = 0;
    private var mHeight: Int = 0


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertexShader = GlesUtils.createShader(
            context, GLES20.GL_VERTEX_SHADER,
            "glsl/vertex/bitmap_renderer.glsl"
        )
        val fragShader = GlesUtils.createShader(
            context, GLES20.GL_FRAGMENT_SHADER,
            "glsl/frag/bitmap_renderer.glsl"
        )
        val program = GlesUtils.createProgram(vertexShader, fragShader)

        if ((0 >= vertexShader) || (0 >= fragShader) || (0 >= program)) {
            GlesUtils.deleteShadersSafety(vertexShader, fragShader)

            if (0 < program) {
                GLES20.glDeleteProgram(program)
            }
        } else {
            mVertexShader = vertexShader
            mFragShader = fragShader
            mProgram = program

            mAttrPosition = GlesUtils.glGetAttribLocation(mProgram, "vPosition")
            mAttrTexPosition = GlesUtils.glGetAttribLocation(mProgram, "vTexPosition")
            mUniformMatrix = GlesUtils.glGetUniformLocation(mProgram, "mMatrix")
            mUniformSampler2D = GlesUtils.glGetUniformLocation(mProgram, "sSampler")

            initBitmapTextures()
            initWatermarkTextures()
        }
    }

    private fun textToBitmap(text: String): Bitmap {
        val paint = Paint().apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 60f
        }
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val textBmp = Bitmap.createBitmap(
            (bounds.width() * 1.3f).toInt(),
            (bounds.height() * 2f).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(textBmp)

        canvas.drawText(text, 0f, 60f, paint)

        return textBmp
    }

    private fun webpToBitmap(context: Context, id: Int) :Bitmap{
        var drawable = ResourcesCompat.getDrawable(context.resources, id, context.theme)
            ?: throw RuntimeException("webpToBitmap getDrawable null")

        var bmp = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(bmp)
        drawable.setBounds(0,  0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)

        return bmp
    }

    private fun initWatermarkTextures() {
        val textures = IntArray(1)
        val iconBmp = webpToBitmap(context, R.mipmap.ic_launcher_round)
        GLES20.glGenTextures(1, textures, 0)

        GlesUtils.bindBitmapToTexture(iconBmp, textures[0])
        mIconTextureId = TextureId(textures[0], iconBmp.width, iconBmp.height)

        val textBmp = textToBitmap("This is Watermark Text!")
        GLES20.glGenTextures(1, textures, 0)
        GlesUtils.bindBitmapToTexture(textBmp, textures[0])
        mTextTextureId = TextureId(textures[0], textBmp.width, textBmp.height)
    }

    private fun initBitmapTextures() {
        GLES20.glGenTextures(1, mBitmapTextureArray, 0)

        val panadaBmp = BitmapFactory.decodeResource(context.resources, R.mipmap.panada)
        GlesUtils.bindBitmapToTexture(panadaBmp, mBitmapTextureArray[0])

        mBmpRatio = panadaBmp.width * 1f / panadaBmp.height
    }

    private fun initVertexMatrix(width: Int, height: Int) {
        Matrix.setIdentityM(mVertexMatrix, 0)
        Matrix.setIdentityM(mSourceMatrix, 0)
        var ratio = width * 1f / height
        // val ratio = height * 1f / width

        val orthoMatrix = FloatArray(16)
        if (mBmpRatio > ratio) {
            ratio = (mBmpRatio / ratio)
        } else {
            ratio = (ratio / mBmpRatio)
        }
        Matrix.orthoM(orthoMatrix, 0, -1f, 1f, -ratio, ratio, 1f, 10f)
        val lookAtMatrix = FloatArray(16)
        Matrix.setLookAtM(lookAtMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)

        // Matrix.rotateM(mVertexMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.multiplyMM(mVertexMatrix, 0, lookAtMatrix, 0, mVertexMatrix, 0)
        Matrix.multiplyMM(mVertexMatrix, 0, orthoMatrix, 0, mVertexMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        Matrix.setIdentityM(mVertexMatrix, 0)
        Matrix.setIdentityM(mSourceMatrix, 0)

        initVertexMatrix(width, height)

        mWidth = width
        mHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        if (0 >= mProgram) {
            return
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgram)
        // 绘制熊猫图
        drawTexture(0, 0, mWidth, mHeight, mVertexMatrix, mBitmapTextureArray[0])
        // 设置颜色混合；绘制多张图时需要设置
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA)
        // 绘制图标
        mIconTextureId?.apply {
            drawTexture(mWidth / 2, mHeight / 3, width, height, mSourceMatrix, id)
        }

        // 绘制文字
        mTextTextureId?.apply {
            drawTexture(mWidth - width, mHeight / 4, width, height, mSourceMatrix, id)
        }

        // 关闭颜色混合
        GLES20.glDisable(GLES20.GL_BLEND)
    }

    private fun drawTexture(x: Int, y: Int, w: Int, h: Int, matrix: FloatArray, texId: Int) {
        GLES20.glViewport(x, y, w, h)
        GLES20.glEnableVertexAttribArray(mAttrPosition)
        GLES20.glEnableVertexAttribArray(mAttrTexPosition)
        GLES20.glVertexAttribPointer(
            mAttrPosition,
            2,
            GLES20.GL_FLOAT,
            false,
            4 * 4,
            COORDINATE_BUFFER
        )
        GLES20.glVertexAttribPointer(
            mAttrTexPosition,
            2,
            GLES20.GL_FLOAT,
            false,
            2 * 4,
            TEX_COORDINATE_BUFFER
        )
        GLES20.glUniformMatrix4fv(mUniformMatrix, 1, false, matrix, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(mUniformSampler2D, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, COORDINATE_BUFFER.limit() / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
        GLES20.glDisableVertexAttribArray(mAttrTexPosition)
    }

    fun release() {
        GLES20.glDeleteTextures(1, mBitmapTextureArray, 0)
        GLES20.glDeleteShader(mFragShader)
        GLES20.glDeleteShader(mVertexShader)
        GLES20.glDeleteProgram(mProgram)

        mTextTextureId?.apply {
            GLES20.glDeleteTextures(1, intArrayOf(id), 0)
        }

        mTextTextureId = null

        mIconTextureId?.apply {
            GLES20.glDeleteTextures(1, intArrayOf(id), 0)
        }

        mIconTextureId = null
    }
}