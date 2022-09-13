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

import com.android.build.api.transform.QualifiedContent
import com.google.common.collect.ImmutableSet
import com.google.gson.reflect.TypeToken
import com.hujiang.gradle.plugin.android.aspectjx.compat.AgpApiCompat
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.model.JarInfo
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-13
 */
class VariantCache(val ajxCache: AJXCache, val variantName: String) {

    lateinit var cachePath: String
    lateinit var aspectPath: String
    lateinit var includeFilePath: String
    lateinit var excludeFilePath: String
    lateinit var includeJarConfigPath: String

    val incrementalStatus = IncrementalStatus()
    val contentTypes: Set<QualifiedContent.ContentType> =
        ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES)
    val scopes: MutableSet<QualifiedContent.Scope> =
        mutableSetOf(QualifiedContent.Scope.EXTERNAL_LIBRARIES)

    private val includeJarInfos = ConcurrentHashMap<String, JarInfo>()

    init {
        init()
    }

    private fun init() {
        cachePath =
            ajxCache.buildDir.absolutePath + File.separator + AgpApiCompat.FD_INTERMEDIATES + "/ajx/" + variantName
        aspectPath = cachePath + File.separator + "aspecs"
        includeFilePath = cachePath + File.separator + "includeFiles"
        excludeFilePath = cachePath + File.separator + "excludeFiles"
        includeJarConfigPath = cachePath + File.separator + "includeJars.json"

        val aspectDir = getAspectDir()
        if (!aspectDir.exists()) {
            aspectDir.mkdirs()
        }

        val includeFileDir = getIncludeFileDir()
        if (!includeFileDir.exists()) {
            includeFileDir.mkdirs()
        }

        val excludeFileDir = getExcludeFileDir()
        if (!excludeFileDir.exists()) {
            excludeFileDir.mkdirs()
        }

        val includeJarConfig = getIncludeJarConfig()
        if (includeJarConfig.exists()) {
            val jsonString = FileUtils.readFileToString(includeJarConfig)
            val type = object : TypeToken<List<JarInfo>>() {}.type
            val jarInfoList = AJXUtils.optFromJsonString<List<JarInfo>>(jsonString, type)
            jarInfoList?.forEach {
                includeJarInfos[it.path] = it
            }
        }
    }

    fun getCacheDir(): File {
        return File(cachePath)
    }

    fun getAspectDir(): File {
        return File(aspectPath)
    }

    fun getIncludeFileDir(): File {
        return File(includeFilePath)
    }

    fun getExcludeFileDir(): File {
        return File(excludeFilePath)
    }

    fun getIncludeJarConfig(): File {
        return File(includeJarConfigPath)
    }

    fun add(sourceFile: File?, cacheFile: File?) {
        if (sourceFile == null || cacheFile == null) {
            return
        }
        val bytes = FileUtils.readFileToByteArray(sourceFile)
        add(bytes, cacheFile)
    }

    fun add(classBytes: ByteArray?, cacheFile: File?) {
        if (classBytes == null || cacheFile == null) {
            return
        }

        FileUtils.writeByteArrayToFile(cacheFile, classBytes)
    }

    fun remove(cacheFile: File?) {
        cacheFile?.delete()
    }

    fun addIncludeJar(jarPath: String?) {
        if (jarPath != null) {
            includeJarInfos[jarPath] = JarInfo().apply { path = jarPath }
        }
    }

    fun removeIncludeJar(jarPath: String) {
        includeJarInfos.remove(jarPath)
    }

    fun isIncludeJar(jarPath: String?): Boolean {
        if (jarPath == null) {
            return false
        }

        return includeJarInfos.containsKey(jarPath)
    }

    fun commitIncludeJarConfig() {
        val includeJarConfig = getIncludeJarConfig()
        FileUtils.deleteQuietly(includeJarConfig)

        if (!includeJarConfig.exists()) {
            includeJarConfig.createNewFile()
        }

        val jarInfoList = includeJarInfos.values.toList()
        FileUtils.write(includeJarConfig, AJXUtils.optToJsonString(jarInfoList), "UTF-8")
    }

    fun reset() {
        close()

        init()
    }

    fun close() {
        FileUtils.deleteDirectory(getCacheDir())
        includeJarInfos.clear()
    }
}
