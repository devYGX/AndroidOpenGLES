package com.example.camerademo.utils

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object ThreadPool {
    private val mPoolIndex = AtomicInteger(0)

    private val mCachePool by lazy {
        Executors.newCachedThreadPool { r -> Thread(r, "Pool-${mPoolIndex.getAndIncrement()}") }
    }

    fun execute(r: Runnable) {
        mCachePool.execute(r)
    }
}