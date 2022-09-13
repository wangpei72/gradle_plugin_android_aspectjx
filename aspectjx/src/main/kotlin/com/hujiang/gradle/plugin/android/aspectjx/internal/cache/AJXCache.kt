/*
 * Copyright 2018 firefly1126, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.gradle_plugin_android_aspectjx
 */
package com.hujiang.gradle.plugin.android.aspectjx.internal.cache

import com.hujiang.gradle.plugin.android.aspectjx.AJXConfig
import com.hujiang.gradle.plugin.android.aspectjx.AJXExtension
import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder
import com.hujiang.gradle.plugin.android.aspectjx.compat.AgpApiCompat
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.model.AJXExtensionConfig
import org.apache.commons.io.FileUtils
import org.aspectj.weaver.Dump
import org.gradle.api.Project
import java.io.File

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-03
 */
class AJXCache(project: Project) {

    private lateinit var cachePath: String
    private lateinit var extensionConfigPath: String
    val buildDir: File = project.buildDir

    var ajxExtensionConfig = AJXExtensionConfig()

    //for aspectj
    var encoding: String? = null
    lateinit var bootClassPath: String
    lateinit var sourceCompatibility: String
    lateinit var targetCompatibility: String

    init {
        init()

        val ajxCache = this
        val configuration = AJXConfig(project)
        project.afterEvaluate {
            val variants = configuration.getVariants()
            if (!variants.isEmpty()) {
                val variant = variants.first()
                // 兼容agp版本
                val javaCompile = AgpApiCompat.getJavaCompile(variant)
                ajxCache.encoding = javaCompile.options.encoding
                ajxCache.sourceCompatibility = javaCompile.sourceCompatibility
                ajxCache.targetCompatibility = javaCompile.targetCompatibility
            }
            ajxCache.bootClassPath =
                configuration.getBootClasspath().joinToString(separator = File.pathSeparator)
            LoggerHolder.logger.warn("[ajx] bootClassPath=${bootClassPath}")

            val ajxExtension =
                project.extensions.findByType(AJXExtension::class.java) ?: AJXExtension()
            LoggerHolder.logger.warn("[ajx] project.aspectjx=${ajxExtension}")

            //当过滤条件发生变化，clean掉编译缓存
            val isExtensionChanged = ajxCache.isExtensionChanged(ajxExtension)
            LoggerHolder.logger.warn("[ajx] isExtensionChanged=$isExtensionChanged")
            if (isExtensionChanged) {
                LoggerHolder.logger.warn("[ajx] cache changed, clean '${project.name}' before preBuild")
                project.tasks.findByName("preBuild")?.dependsOn(project.tasks.findByName("clean"))
            }
            // 更新配置
            ajxCache.putExtensionConfig(ajxExtension)

            // 设置运行变量
            System.setProperty("aspectj.multithreaded", "true")
            // set aspectj build log output dir
            val logDir = File(
                project.buildDir.absolutePath + File.separator + "outputs"
                        + File.separator + "logs"
            )
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            Dump.setDumpDirectory(logDir.absolutePath)
        }
    }

    private fun init() {
        cachePath = buildDir.absolutePath + File.separator + AgpApiCompat.FD_INTERMEDIATES + "/ajx"
        extensionConfigPath = cachePath + File.separator + "extensionConfig.json"
        val cacheDir = getCacheDir()
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        //extension config
        val extensionConfig = getExtensionConfigFile()
        if (extensionConfig.exists()) {
            ajxExtensionConfig = AJXUtils.optFromJsonString(
                FileUtils.readFileToString(extensionConfig),
                AJXExtensionConfig::class.java
            ) ?: AJXExtensionConfig()
        }
    }

    private fun getCacheDir(): File {
        return File(cachePath)
    }

    private fun getExtensionConfigFile(): File {
        return File(extensionConfigPath)
    }

    fun reset() {
        FileUtils.deleteDirectory(getCacheDir())
        init()
    }

    fun commit() {
        val extensionConfigFile = getExtensionConfigFile()
        LoggerHolder.logger.debug("putExtensionConfig:${extensionConfigFile}")

        FileUtils.deleteQuietly(extensionConfigFile)

        val parent = extensionConfigFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        if (!extensionConfigFile.exists()) {
            extensionConfigFile.createNewFile()
        }

        val jsonString = AJXUtils.optToJsonString(ajxExtensionConfig)
        LoggerHolder.logger.debug(jsonString)
        FileUtils.write(extensionConfigFile, jsonString, "UTF-8")
    }

    private fun putExtensionConfig(extension: AJXExtension?) {
        if (extension == null) {
            return
        }

        ajxExtensionConfig.enabled = extension.enabled
        ajxExtensionConfig.ajcArgs = extension.ajcArgs
        ajxExtensionConfig.includes = extension.includes
        ajxExtensionConfig.excludes = extension.excludes
    }

    private fun isExtensionChanged(extension: AJXExtension): Boolean {
        if (extension.enabled != ajxExtensionConfig.enabled) {
            return true
        }

        val isSourceIncludesExists = ajxExtensionConfig.includes.isNotEmpty()
        val isTargetIncludeExists = extension.includes.isNotEmpty()
        val isSourceExcludeExists = ajxExtensionConfig.excludes.isNotEmpty()
        val isTargetExcludeExists = extension.excludes.isNotEmpty()

        if ((!isSourceIncludesExists && isTargetIncludeExists)
            || (isSourceIncludesExists && !isTargetIncludeExists)
            || (!isSourceExcludeExists && isTargetExcludeExists)
            || (isSourceExcludeExists && !isTargetExcludeExists)
        ) {
            return true
        }

        if ((!isSourceIncludesExists && !isTargetIncludeExists)
            && (!isSourceExcludeExists && !isTargetExcludeExists)
        ) {
            return false
        }

        if (ajxExtensionConfig.includes.size != extension.includes.size
            || ajxExtensionConfig.excludes.size != extension.excludes.size
        ) {
            return true
        }

        if (!ajxExtensionConfig.includes.containsAll(extension.includes)) {
            return true
        }

        if (!ajxExtensionConfig.excludes.containsAll(extension.excludes)) {
            return true
        }

        return false
    }
}
