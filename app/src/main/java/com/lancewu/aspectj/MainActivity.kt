package com.lancewu.aspectj

import android.graphics.Bitmap
import android.os.Bundle
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
    }

    private fun toast() {
        // 通过aop织入
    }
}