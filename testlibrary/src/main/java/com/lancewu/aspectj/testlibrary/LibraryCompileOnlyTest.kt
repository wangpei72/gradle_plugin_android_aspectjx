package com.lancewu.aspectj.testlibrary

import android.graphics.Bitmap
import androidx.palette.graphics.Palette

/**
 * Created by LanceWu on 2022/9/29
 *
 * 仅仅编译依赖测试
 */
class LibraryCompileOnlyTest {

    fun test(bitmap: Bitmap) {
        TestLibrary().printLog("call from LibraryCompileOnlyTest")
        // 测试compileOnly依赖的情况能不能正确织入
        Palette.from(bitmap).generate()
    }
}