package com.example.camerademo.filter

import android.opengl.Matrix
import com.example.camerademo.utils.LogUtils

abstract class AbsFilter(glEnv: IGLEnv) : AbsRender(glEnv) {
    override fun onCreated() {
        /**
         * 是否Y方向镜像;
         *
         * 从相机导出来的数据是正的；经过FBO后输出的纹理就是Y方向镜像的了；
         * 因此凡是支持FBO的，都需要Y镜像
         */
        if (supportFBO()) {
            LogUtils.d(getTag(), "onCreated, flipY")

            Matrix.scaleM(mVertexMatrix, 0, 1f, -1f, 1f)
        }
    }
}