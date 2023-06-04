package com.example.openglesdemo

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.openglesdemo.renderer.BitmapRenderer
import com.example.openglesdemo.renderer.HelloRenderer
import com.example.openglesdemo.renderer.MatrixTriangleRenderer
import com.example.openglesdemo.renderer.TriangleRenderer

class GLActivity : AppCompatActivity() {

    private lateinit var mGlView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_glactivity)

        mGlView = findViewById(R.id.glSurfaceView)

        mGlView.setEGLContextClientVersion(2)
        mGlView.setRenderer(HelloRenderer(mGlView))
        mGlView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }
}