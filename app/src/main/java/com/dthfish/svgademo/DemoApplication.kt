package com.dthfish.svgademo

import android.app.Application
import android.net.http.HttpResponseCache
import java.io.File

/**
 * Description
 * Author DthFish
 * Date  2020/5/25.
 */
class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val cacheDir = File(this.applicationContext.cacheDir, "http")
        HttpResponseCache.install(cacheDir, 1024 * 1024 * 128)
    }
}