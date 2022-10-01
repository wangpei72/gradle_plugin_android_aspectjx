package com.hujiang.gradle.plugin.android.aspectjx


import com.android.build.gradle.AbstractAppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

/**
 * Android相关配置
 */
class AndroidConfig(project: Project) {

    private val android: BaseExtension = project.extensions.getByName("android") as BaseExtension

    /**
     * Return all variants.
     *
     * @return Collection of variants.
     */
    fun getVariants(): DomainObjectSet<BaseVariant> {
        return when (android) {
            is AbstractAppExtension -> {
                android.applicationVariants as DomainObjectSet<BaseVariant>
            }
            is TestExtension -> {
                android.applicationVariants as DomainObjectSet<BaseVariant>
            }
            is LibraryExtension -> {
                android.libraryVariants as DomainObjectSet<BaseVariant>
            }
            else -> throw GradleException("nonsupport extension:$android")
        }
    }

    /**
     * Return boot classpath.
     * @return Collection of classes.
     */
    fun getBootClasspath(): List<File> {
        return android.bootClasspath
    }
}