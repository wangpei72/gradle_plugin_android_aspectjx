package com.lancewu.aspectj.testlibrary

import android.util.Log
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

/**
 * Created by LanceWu on 2022/9/6
 *
 * 函数切面
 */
@Aspect
class LibraryMethodAspect {

    @Around("call(* com.lancewu.aspectj.testlibrary.TestLibrary.printLog(..))")
    fun around_TestLibrary_printLog(joinPoint: ProceedingJoinPoint): Any? {
        Log.d("LibraryMethodAspect", "before printLog")
        return aroundOldResult(joinPoint)
    }

    private fun aroundOldResult(joinPoint: ProceedingJoinPoint): Any? {
        try {
            return joinPoint.proceed(joinPoint.args)
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
        }
        return null
    }
}