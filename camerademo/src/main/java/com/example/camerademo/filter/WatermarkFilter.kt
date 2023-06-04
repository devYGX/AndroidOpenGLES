package com.example.camerademo.filter

import android.graphics.*
import android.opengl.GLES20
import android.util.Size
import com.example.camerademo.R
import com.example.camerademo.utils.GlesUtils
import com.example.camerademo.utils.LogUtils
import java.text.SimpleDateFormat
import java.util.*

class WatermarkFilter(private val glEnv: IGLEnv): AbsFilter(glEnv) {

    private var mUniformTexture: Int = -1
    private var mAttrPosition: Int = -1
    private var mAttrTexPosition: Int = -1
    private var mUniformMatrix: Int = -1

    private var mWatermarkTexIdMap = mutableMapOf<String, Int>()
    private var mReusableIntArray: IntArray = IntArray(1)
    private var mGetVelocityTime = 0L
    private var mGetVelocityTimeGap = 0
    private var mCacheVelocity = ""
    private var mScaleType = GlesUtils.TYPE_CENTERCROP

    private val mDateFormater by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    private val mDate by lazy {
        Date()
    }

    private val mRandom by lazy {
        Random()
    }

    private val mWatermarkPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = getContext().resources.getDimension(R.dimen.watermark_text_size)
        }
    }

    override fun setDisplayScaleType(scaleType: Int) {
        mScaleType = scaleType
    }

    private fun getVelocity():String {
        if ((0L == mGetVelocityTime) || ((System.currentTimeMillis() - mGetVelocityTime) > mGetVelocityTimeGap)) {
            mCacheVelocity = (80 + mRandom.nextInt(40) + (mRandom.nextInt(10) / 10f)).toString()
            mGetVelocityTimeGap = mRandom.nextInt(2000) + 1000
            mGetVelocityTime = System.currentTimeMillis()
        }

        return mCacheVelocity
    }

    override fun isAllowCreateProgram(): Boolean = true

    override fun getCodeFilePath(): Array<String> {
        return arrayOf("draw.vert", "draw.frag")
    }

    override fun createFBO(width: Int, height: Int) {
        super.createFBO(width, height)
    }

    override fun onCreated() {
        super.onCreated()

        mAttrPosition = glGetAttribLocation("vPosition")
        mAttrTexPosition = glGetAttribLocation("vTexturePosition")
        mUniformMatrix = glGetUniformLocation("mMatrix")
        mUniformTexture = glGetUniformLocation("sTexture")

        createWatermarkTextureIds()
    }

    override fun onDraw() {
        val size = getSourceSize()?:return

        GLES20.glViewport(0, 0, size.width, size.height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GlesUtils.bindFrameBuffer(mFrameArray[0], mRenderArray[0], mOutTexArray[0])

        GLES20.glUseProgram(mProgram)
        drawTexture(getInputTexture(), mVertexMatrix)

        val enableDepth = GLES20.glIsEnabled(GLES20.GL_DEPTH_TEST)

        if (enableDepth) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        }

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_COLOR, GLES20.GL_DST_ALPHA)

        drawWatermark(size)

        GLES20.glDisable(GLES20.GL_BLEND)

        if (enableDepth) {
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        }

        GlesUtils.unbindFrameBuffer()
    }

    private fun drawWatermark(size: Size) {
        mDate.time = System.currentTimeMillis()
        val nowTime = mDateFormater.format(mDate)
        var x = 10
        var y = 10

        for (i in nowTime.indices) {
            val texId = mWatermarkTexIdMap.get(nowTime[i].toString())

            if (null != texId) {                //画水印
                GLES20.glViewport(x + i * 18, y, 100, 150)
                drawTexture(texId, mVertexMatrix)
            }
        }

        val velocity = getVelocity()

        x = size.width * 7 / 8

        for (i in velocity.indices) {
            val texId = mWatermarkTexIdMap.get(velocity[i].toString())

            if (null != texId) {                //画水印
                GLES20.glViewport(x + i * 18, y, 100, 150)
                drawTexture(texId, mVertexMatrix)
            }
        }

        mWatermarkTexIdMap.get("km/h")?.also {
            x += velocity.length * 18
            GLES20.glViewport(x, y, 200, 150)
            drawTexture(it, mVertexMatrix)
        }

        x = size.width / 2 - 180
    }

    private fun drawTexture(texId: Int, matrix: FloatArray) {
        GLES20.glEnableVertexAttribArray(mAttrPosition)
        GLES20.glEnableVertexAttribArray(mAttrTexPosition)
        // 参数二为每组坐标取多少位坐标，2:xy或3:xyz都可以;
        // 参数5为每个顶点的步长, 坐标位数 * sizeof(float)
        GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 4 * 4,
            GlesUtils.COORDINATE_BUFFER)
        GLES20.glVertexAttribPointer(mAttrTexPosition, 2, GLES20.GL_FLOAT, false, 2 * 4,
            GlesUtils.TEX_COORDINATE_BUFFER)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(mUniformTexture, 0)


        GLES20.glUniformMatrix4fv(
            mUniformMatrix, 1, false,
            matrix, 0
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, GlesUtils.COORDINATE.size / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
        GLES20.glDisableVertexAttribArray(mAttrTexPosition)
    }

    override fun onRelease() {
        mWatermarkTexIdMap.entries.forEach {
            LogUtils.d(TAG, "createWatermarkTextureIds, ${it.key}")

            mReusableIntArray[0] = it.value
            GLES20.glDeleteTextures(1, mReusableIntArray, 0)
        }

        mWatermarkTexIdMap.clear()
    }

    override fun supportFBO(): Boolean = true

    private fun createWatermarkTextureIds() {
        for (i in 0..9) {
            val s = i.toString()
            val texId = GlesUtils.createTextureIdByBitmap(mReusableIntArray, textToBitmap(s, 100, 150), true)
            mWatermarkTexIdMap[s] = texId
        }

        mWatermarkTexIdMap["-"] =
            GlesUtils.createTextureIdByBitmap(mReusableIntArray, textToBitmap("-", 100, 150), true)
        mWatermarkTexIdMap[":"] =
            GlesUtils.createTextureIdByBitmap(mReusableIntArray, textToBitmap(":", 100, 150), true)
        mWatermarkTexIdMap["."] =
            GlesUtils.createTextureIdByBitmap(mReusableIntArray, textToBitmap(".", 100, 150), true)
        mWatermarkTexIdMap["km/h"] = GlesUtils.createTextureIdByBitmap(mReusableIntArray, textToBitmap("km/h", 200, 150), true)

        mWatermarkTexIdMap.entries.forEach {
            LogUtils.d(TAG, "createWatermarkTextureIds, ${it.key}")
        }
    }

    private fun textToBitmap(s: String, width: Int, height: Int) : Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.drawText(s, 0f, 50f, mWatermarkPaint)

        return bmp
    }
}