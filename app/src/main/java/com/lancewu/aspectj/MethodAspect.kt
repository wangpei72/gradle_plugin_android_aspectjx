package com.lancewu.aspectj

import android.content.Context
import android.widget.Toast
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

/**
 * Created by LanceWu on 2022/9/6
 *
 * 函数切面
 */
@Aspect
class MethodAspect {

    @Around("call(* com.lancewu.aspectj.MainActivity.toast())")
    fun around_MainActivity_toast(joinPoint: ProceedingJoinPoint): Any? {
        aroundOldResult(joinPoint)
        val context = joinPoint.`this` as Context
        Toast.makeText(context, "代码织入toast", Toast.LENGTH_LONG).show()
        return null
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