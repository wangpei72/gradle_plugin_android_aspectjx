package com.lancewu.aspectj.testlibrary

/**
 * Created by LanceWu on 2022/9/27
 *
 * 测试
 */
class LibraryIncludeTest {

    fun test() {
        TestLibrary().printLog("call from LibraryIncludeTest")
    }
}