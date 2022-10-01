package com.lancewu.aspectj.testlibrary

import com.lancewu.aspectj.testlibrary.TestLibrary

/**
 * Created by LanceWu on 2022/9/27
 *
 * 排除测试
 */
class LibraryExcludeTest {

    fun test() {
        TestLibrary().printLog("call from LibraryExcludeTest")
    }
}