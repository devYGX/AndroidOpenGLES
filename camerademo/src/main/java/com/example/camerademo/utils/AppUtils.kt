package com.example.camerademo.utils

import android.media.Image
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import java.io.File
import java.io.FileOutputStream

class AppUtils {
    companion object {
        fun setActionBarStatusBar(aty: AppCompatActivity) {
            aty.supportActionBar?.hide()
            ViewCompat.getWindowInsetsController(aty.window.decorView)
                ?.apply {
                    isAppearanceLightStatusBars = true
                    hide(WindowInsets.Type.statusBars())
                    hide(WindowInsets.Type.navigationBars())
                    hide(WindowInsets.Type.systemBars())
                }
        }

        fun saveImage(f: File, image: Image) {
            val nv21Buffer = ByteArray(image.width * image.height * 3 / 2)
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            yPlane.buffer.get(nv21Buffer, 0, yPlane.buffer.capacity())
            vPlane.buffer.get(nv21Buffer, yPlane.buffer.capacity(), vPlane.buffer.capacity())
            val lastByte = uPlane.buffer.get(uPlane.buffer.capacity() - 1)
            nv21Buffer[nv21Buffer.size - 1] = lastByte

            FileOutputStream(f).use {
                it.write(nv21Buffer)
            }
        }

        fun getNv21Buffer(image: Image): ByteArray {
            val nv21Buffer = ByteArray(image.width * image.height * 3 / 2)
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            // uPlane和vPlane的size 都是image.width * image.height / 2 - 1;
            // uPlane:u v u v u v ... u (v); (v)是最后的一个v分量
            // vPlane:(u) v u v u v u ... v; (u)是第一个u分量，但因为这是vPlane, 因此只记录v ~ v

            // 在Image转NV21的时候，先复制Y分量
            yPlane.buffer.get(nv21Buffer, 0, yPlane.buffer.capacity())
            // 再复制vPlane：v u v u v...vuv, capacity是width * height / 2 - 1
            vPlane.buffer.get(nv21Buffer, yPlane.buffer.capacity(), vPlane.buffer.capacity())
            // uPanel的最后一个元素必定是U
            val lastByte = uPlane.buffer.get(uPlane.buffer.capacity() - 1)
            nv21Buffer[nv21Buffer.size - 1] = lastByte

            return nv21Buffer
        }
    }
}