package com.lancewu.aspectj.testlibrary



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