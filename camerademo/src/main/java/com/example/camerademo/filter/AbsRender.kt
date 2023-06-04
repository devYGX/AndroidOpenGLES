package com.example.camerademo.filter

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Size
import com.example.camerademo.utils.GlesUtils
import com.example.camerademo.utils.LogUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class AbsRender(private val glEnv: IGLEnv) : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "AbsRender"
    }

    protected var mProgram: Int = -1

    protected var mVertexMatrix = GlesUtils.SOURCE_MATRIX.copyOf()

    protected var mFrameArray = intArrayOf(-1)
    protected var mRenderArray = intArrayOf(-1)
    protected var mOutTexArray = intArrayOf(-1)

    protected var mSourceSize: Size? = null
    protected var mSurfaceSize: Size? = null
    protected var mbSizeChange = false

    private var mInputTexture: Int = -1
    private var mbRelease: Boolean = false
    private var mbCreated: Boolean = false

    protected fun getTag() : String = javaClass.name + hashCode()

    fun getContext() = glEnv.getContext()

    fun createProgram(vertexName: String, fragName: String): Int {
        val vertexCode =
            GlesUtils.loadShaderCodeFromAssets(getContext(), vertexName)
        val fragCode =
            GlesUtils.loadShaderCodeFromAssets(getContext(), fragName)
        val program = GlesUtils.createProgram(vertexCode, fragCode)

        if (0 > program) {
            throw RuntimeException("createProgram failed, result: $program, vertex: $vertexName, fragment:$fragName")
        }

        return program
    }

    fun setInputTexture(texId: Int) {
        mInputTexture = texId
    }

    open fun setDisplayScaleType(scaleType: Int){
        // empty
    }

    protected fun getInputTexture(): Int = mInputTexture

    open fun setDataSize(source: Size) {
        mSourceSize = Size(source.width, source.height)

        if (mbCreated) {
            glEnv.queueEvent {
                if (supportFBO()) {
                    mSourceSize?.apply {
                        createFBO(width, height)
                    }
                }

                mbSizeChange = true
            }
        }
    }

    fun destroy() {
        glEnv.queueEvent {
            deleteFBO()
            onRelease()
            GLES20.glDeleteProgram(mProgram)
            mProgram = -1
        }
    }

    fun getSourceSize(): Size? = mSourceSize

    fun getSurfaceSize(): Size? = mSurfaceSize

    abstract fun isAllowCreateProgram(): Boolean

    abstract fun onCreated()

    open fun onSizeChanged() {}

    abstract fun onDraw()

    abstract fun onRelease()

    abstract fun supportFBO(): Boolean

    open fun getCodeFilePath(): Array<String>? = null

    fun getOutputTexture(): Int {
        return if (supportFBO()) mOutTexArray[0] else -1
    }

    protected open fun deleteFBO() {
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        LogUtils.d(getTag(), "deleteFBO: ${mFrameArray[0]}, ${mRenderArray[0]}, ${mOutTexArray[0]}",
            RuntimeException("deleteFBO"))

        if (0 < mOutTexArray[0]) {
            GLES20.glDeleteTextures(1, mOutTexArray, 0)
            mOutTexArray[0] = -1
        }

        if (0 <mRenderArray[0]) {
            GLES20.glDeleteRenderbuffers(1, mRenderArray, 0)
            mRenderArray[0] = -1
        }

        if (0 < mFrameArray[0]) {
            GLES20.glDeleteFramebuffers(1, mFrameArray, 0)
            mFrameArray[0] = -1
        }
    }

    protected open fun createFBO(width: Int, height: Int) {
        LogUtils.d(getTag(), "createFBO, $width, $height  ${mFrameArray[0]} ${mOutTexArray[0]} ${mRenderArray[0]}")

        if (0 >= mFrameArray[0]) {
            GLES20.glGenFramebuffers(1, mFrameArray, 0)

            LogUtils.d(TAG, "createFBO, glGenFramebuffers: ${mFrameArray[0]}")
        }

        if (0 >= mRenderArray[0]) {
            GLES20.glGenRenderbuffers(1, mRenderArray, 0)

            LogUtils.d(TAG, "createFBO, glGenRenderbuffers: ${mRenderArray[0]}")
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameArray[0])
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mRenderArray[0])
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height)

        if (0 >= mOutTexArray[0]) {
            GLES20.glGenTextures(1, mOutTexArray, 0)

            LogUtils.d(TAG, "createFBO, glGenTextures: ${mOutTexArray[0]}")
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOutTexArray[0])

        // 这句代码相当于是提前给输出纹理分配空间和数据格式了
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, mOutTexArray[0], 0);
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, mRenderArray[0])

        // 下面三个很有必要
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        LogUtils.d(getTag(), "createFBO, $width, $height  ${mFrameArray[0]} ${mOutTexArray[0]} ${mRenderArray[0]}  X")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if (mbRelease) {
            return
        }

        mbCreated = true

        if (isAllowCreateProgram()) {
            getCodeFilePath()?.also {
                mProgram = createProgram(it[0], it[1])
            }
        }

        if (supportFBO()) {
            mSourceSize?.apply {
                createFBO(width, height)
            }
        }

        onCreated()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        if (mbRelease) {
            return
        }

        mbSizeChange = true

        mSurfaceSize = Size(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (mbRelease) {
            LogUtils.w(getTag(), "onDrawFrame, mbRelease is true, so return")

            return
        }

        if (mbSizeChange && ((null != mSourceSize) && (null != mSurfaceSize))) {
            mbSizeChange = false

            onSizeChanged()
        }

        onDraw()
    }

    fun glGetAttribLocation(name: String) : Int = GlesUtils.glGetAttribLocation(mProgram, name)

    fun glGetUniformLocation(name: String) : Int = GlesUtils.glGetUniformLocation(mProgram, name)
}