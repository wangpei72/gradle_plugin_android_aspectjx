package com.lancewu.aspectj;

import android.app.Activity;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Arrays;

/**
 * Created by LanceWu on 2022/12/19.<br>
 * TODO 类说明
 */
@Aspect
public class PermissionsAspect {

    /**
     * 方法切入点
     */
    @Pointcut("execution(@com.lancewu.aspectj.Permissions * *(..))")
    public void method() {
    }

    /**
     * 在连接点进行方法替换
     */
    @Around("method() && @annotation(permissions)")
    public void aroundJoinPoint(ProceedingJoinPoint joinPoint, Permissions permissions) {
        Activity activity = null;

        // 方法参数值集合
        Object[] parameterValues = joinPoint.getArgs();
        for (Object arg : parameterValues) {
            if (!(arg instanceof Activity)) {
                continue;
            }
            activity = (Activity) arg;
            break;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Log.e("PermissionsAspect", "The activity has been destroyed and permission requests cannot be made");
            return;
        }

        requestPermissions(joinPoint, activity, permissions.value());
    }

    private void requestPermissions(ProceedingJoinPoint joinPoint, Activity activity, String[] permissions) {
        Log.d("PermissionsAspect", "申请权限：" + Arrays.toString(permissions));
        // FIXME: 2022/12/20 嵌套around-advice时，采用延期调用或是中断-恢复的方式，来调用joinPoint.proceed()，
        //  会出现EmptyStackException异常，原因是aspectj设计上，对于嵌套的around-advice，采取栈的方式存储；
        //  相关问题可参照：https://github.com/eclipse/org.aspectj/issues/128
        //  该issues是多线程调用，在1.9.9版本已修复，但类似以下的使用方式不符合设计
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setMessage("模拟申请权限弹窗")
                .setPositiveButton("点击模拟获取权限结果", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d("PermissionsAspect", "模拟申请权限结束");
                        // 获得权限，执行原方法
                        try {
                            joinPoint.proceed();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                })
                .show();
    }

}
