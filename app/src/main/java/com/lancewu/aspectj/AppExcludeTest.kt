package com.lancewu.aspectj

import com.lancewu.aspectj.testlibrary.TestLibrary

/**
 * Created by LanceWu on 2022/9/27
 *
 * 排除测试
 */
class AppExcludeTest {

    fun test() {
        TestLibrary().printLog("call from AppExcludeTest")
    }
}