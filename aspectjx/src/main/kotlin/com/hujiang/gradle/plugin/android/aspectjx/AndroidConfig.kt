package com.hujiang.gradle.plugin.android.aspectjx


import com.android.build.gradle.*
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
    fun getVariants(): Set<BaseVariant> {
        val set = mutableSetOf<BaseVariant>()
        if (android is TestedExtension) {
            // 加上testVariants
            set.addAll(android.testVariants)
        }
        when (android) {
            is AbstractAppExtension -> {
                set.addAll(android.applicationVariants)
            }
            is TestExtension -> {
                set.addAll(android.applicationVariants)
            }
            is LibraryExtension -> {
                set.addAll(android.libraryVariants)
            }
            else -> throw GradleException("nonsupport extension:$android")
        }
        return set
    }

    /**
     * Return all test variants.
     * @return DomainObjectSet<BaseVariant>?
     */
    fun getTestVariants(): DomainObjectSet<BaseVariant>? {
        return if (android is TestedExtension) {
            android.testVariants as DomainObjectSet<BaseVariant>
        } else {
            null
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