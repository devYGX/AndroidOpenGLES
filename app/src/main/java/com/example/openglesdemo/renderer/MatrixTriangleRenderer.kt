package com.example.openglesdemo.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.openglesdemo.gles.GlesUtils
import com.example.openglesdemo.utils.LogUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MatrixTriangleRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        const val TAG = "TriangleRenderer"

        private val COORDINATE = floatArrayOf(
            -1.0f, 1.0f, 0.0f, 0.0f,
            -1.0f, -1.0f, 0.0f, 0.0f,
            1.0f, -1.0f, 0.0f, 0.0f
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

        private val COLOR = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
    }
    private var mVertexMatrix: FloatArray = FloatArray(16)

    private var mVertexShader: Int = -1
    private var mFragShader: Int = -1
    private var mProgram: Int = -1

    private var mAttrPosition: Int = -1
    private var mUniformColor: Int = -1
    private var mUniformMatrix: Int = -1


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val vertexShader = GlesUtils.createShader(
            context, GLES20.GL_VERTEX_SHADER,
            "glsl/vertex/matrix_triangle.glsl"
        )
        val fragShader = GlesUtils.createShader(
            context, GLES20.GL_FRAGMENT_SHADER,
            "glsl/frag/simple_triangle.glsl"
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
            mUniformColor = GlesUtils.glGetUniformLocation(mProgram, "vColor")
            mUniformMatrix = GlesUtils.glGetUniformLocation(mProgram, "mMatrix")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Matrix.setIdentityM(mVertexMatrix, 0)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glViewport(0, 0, width, height)

        Matrix.translateM(mVertexMatrix, 0, 1f, 0f, 0f)
        Matrix.rotateM(mVertexMatrix, 0, 30f, 0f, 0f, 1f)
        Matrix.scaleM(mVertexMatrix, 0, 0.5f, 1.2f, 1f)
        val ratio = width * 1f / height
        LogUtils.d(TAG, "onSurfaceChanged $ratio")
        // val ratio = height * 1f / width
        val orthoMatrix = FloatArray(16)
        // float[] m, int mOffset, float left, float right, float bottom, float top, float near, float far
        Matrix.orthoM(orthoMatrix, 0, - ratio, ratio, -1f, 1f, 1f, 10f)
        val lookAtMatrix = FloatArray(16)
        // float[] rm, int rmOffset, float eyeX, float eyeY, float eyeZ,
        // float centerX, float centerY, float centerZ, float upX, float upY, float upZ
        Matrix.setLookAtM(lookAtMatrix, 0, 0f, 0f, 3f,0f, 0f, 0f, 0f, 1f, 0f)

        // Matrix.rotateM(mVertexMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.multiplyMM(mVertexMatrix, 0, lookAtMatrix, 0,mVertexMatrix, 0)
        Matrix.multiplyMM(mVertexMatrix, 0, orthoMatrix, 0,mVertexMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        if (0 >= mProgram) {
            return
        }

        // 清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        // 指定视图窗口位置和size; 原点是屏幕左下角，往上Y为正，往右X为正
        // GLES20.glViewport(0, 0, width, height)
        // 启用程序，否则后面的操作都没用
        GLES20.glUseProgram(mProgram)
        GLES20.glEnableVertexAttribArray(mAttrPosition)
        // 传递顶点坐标；参数一是句柄；
        // 参数二是取多少位坐标; 我们每个顶点都传了xyzw共4位，但实际只有xy有效，所以这里是2表示只取xy
        // 参数三是传入的数据类型 是FLOAT
        // 参数四是指传入的数据是否需要标准化，映射成0-1之间；我们选false表示不需要；
        // 		我的理解是标准化后的值只体现在着色器代码中，如果并不影响我们的Android代码
        // 参数五是步长，指的是上一个顶点坐标到下一个顶点坐标的间隔字节数；由于一个顶点坐标我们是传4位（xyzw）,
        //		float占4个字节，所以是步长stride是 4 * 4
        // 参数六就是顶点坐标
        GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 4 * 4, COORDINATE_BUFFER)
        GLES20.glUniformMatrix4fv(mUniformMatrix, 1, false, mVertexMatrix, 0)
        // GLES20.glVertexAttribPointer(mAttrPosition, 2, GLES20.GL_FLOAT, false, 4 * 4, GlesUtils.COORDINATE_BUFFER)
        // 传递颜色值；参数一是句柄；参数2是传递的数量；参数三是传递的实际内容，参数4是数组的offset
        // uniform 4f v：传递uniform 类型，4维float类型的值
        GLES20.glUniform4fv(mUniformColor, 1, COLOR, 0)
        // 绘制；参数1是绘制规则; 参数2是起始点，默认是0；参数三是绘制的顶点数，这里计算出来是3
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, COORDINATE_BUFFER.limit() / 4)
        // GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, GlesUtils.COORDINATE_BUFFER.limit() / 4)
        GLES20.glDisableVertexAttribArray(mAttrPosition)
    }
}