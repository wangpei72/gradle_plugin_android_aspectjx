package com.lancewu.aspectj

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lancewu.aspectj.testlibrary.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toast()
        TestLibrary().printLog("call from MainActivity")
        AppJavaFileTest().test()
        AppExcludeTest().test()
        LibraryIncludeTest().test()
        LibraryExcludeTest().test()
        LibraryJavaTest().test()
        LibraryCompileOnlyTest().test(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        clickAspect()
        findViewById<Button>(R.id.two_annotation_btn).setOnClickListener {
            testAnnotationsWrapper(this)
        }
    }

    private fun clickAspect() {
        findViewById<Button>(R.id.btn).apply {
            text = "点击弹出toast"
            // lambda表达式无法正确织入：https://stackoverflow.com/a/31098062/5193118
            setOnClickListener(object : OnClickListener {
                override fun onClick(v: View?) {
                    Toast.makeText(this@MainActivity, this@apply.text, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun toast() {
        // 通过aop织入
    }

    @Permissions([Manifest.permission.CAMERA])
    private fun testAnnotationsWrapper(activity: MainActivity) {
        Log.d("testAnnotationsWrapper", "call")
        testAnnotations()
    }

    /**
     * 多个注解织入，在织入处理时如果存在延期调用，会出现EmptyStackException异常，因为这不符合aspectj机制设计。
     * 解决办法是，存在延期调用业务时，独立一个函数，比如这边的testAnnotationsWrapper，注解拆分出来处理
     *
     * [详情：多注解异常](https://github.com/wurensen/gradle_plugin_android_aspectjx/issues/60)
     */
    @AOPLog
//    @Permissions([Manifest.permission.CAMERA])
    private fun testAnnotations() {
        Log.d("testAnnotations", "call")
    }
}