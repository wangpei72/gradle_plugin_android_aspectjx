package com.hujiang.gradle.plugin.android.aspectjx.compat;

import com.android.SdkConstants
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Created by LanceWu on 2022/9/2.<br>
 * agp不同版本接口兼容
 */
object AgpApiCompat {

    val FD_INTERMEDIATES = try {
        // agp 7.2修改
        SdkConstants.FD_INTERMEDIATES
    } catch (e: Exception) {
        // 7.2以下，反射获取
        Class.forName("com.android.builder.model.AndroidProject")
            .getField("FD_INTERMEDIATES")
            .get(null) as String
    }

    fun getJavaCompile(variant: BaseVariant): JavaCompile {
        // 兼容agp版本
        val javaCompile: JavaCompile = try {
            //android gradle 3.3.0 +
            variant.javaCompileProvider.get()
        } catch (e: Exception) {
            variant.javaCompile
        }
        return javaCompile
    }
}
