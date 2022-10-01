package com.lancewu.aspectj.testlibrary

import android.util.Log

/**
 * Created by LanceWu on 2022/9/16
 *
 * 测试
 */
class TestLibrary {

    companion object {
        const val TAG = "TestLibrary"
    }

    fun printLog(msg: String) {
        Log.d(TAG, msg)
    }

}