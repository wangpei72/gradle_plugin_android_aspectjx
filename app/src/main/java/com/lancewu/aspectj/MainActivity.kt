package com.lancewu.aspectj

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lancewu.aspectj.testlibrary.LibraryCompileOnlyTest
import com.lancewu.aspectj.testlibrary.LibraryExcludeTest
import com.lancewu.aspectj.testlibrary.LibraryIncludeTest
import com.lancewu.aspectj.testlibrary.TestLibrary

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
        LibraryCompileOnlyTest().test(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        clickAspect()
    }

    private fun clickAspect() {
        findViewById<Button>(R.id.btn).apply {
            text = "点击弹出toast"
            setOnClickListener {
                Toast.makeText(this@MainActivity, this@apply.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toast() {
        // 通过aop织入
    }
}