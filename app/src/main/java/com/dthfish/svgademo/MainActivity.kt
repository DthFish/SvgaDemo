package com.dthfish.svgademo

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

/**
 * 一个组织多个 svga/图片，实现点击计分交互的 demo
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ggv.visibility = View.GONE
        show.setOnClickListener {
            if (ggv.isShow()) {
                Log.d("DDDDDD", "正在动画")
            } else {
                ggv.show(40 * 99)
            }
        }
    }
}
