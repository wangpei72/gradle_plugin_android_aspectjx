package com.hujiang.gradle.plugin.android.aspectjx.compat;

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.tasks.compile.JavaCompile

/**
 * Created by LanceWu on 2022/9/2.<br>
 * agp不同版本接口兼容
 */
object AgpApiCompat {

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
