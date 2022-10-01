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
package com.hujiang.gradle.plugin.android.aspectjx.internal.utils

import com.android.build.api.transform.JarInput
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.hujiang.gradle.plugin.android.aspectjx.AJXPlugin
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Type
import java.util.jar.JarFile

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-02-01
 */
object AJXUtils {

    private val gson: Gson = GsonBuilder().create()

    fun isAspectClass(classFile: File): Boolean {

        if (isClassFile(classFile)) {
            return isAspectClass(FileUtils.readFileToByteArray(classFile))
        }

        return false
    }

    fun isAspectClass(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.isEmpty()) {
            return false
        }

        try {
            val classReader = ClassReader(bytes)
            val classWriter =
                ClassWriter(classReader, ClassWriter.COMPUTE_MAXS or ClassWriter.COMPUTE_FRAMES)
            val aspectJClassVisitor = AspectJClassVisitor(classWriter)
            classReader.accept(aspectJClassVisitor, ClassReader.EXPAND_FRAMES)

            return aspectJClassVisitor.isAspectClass
        } catch (e: Exception) {
//            logger().warn("unexpected error", e)
        }

        return false
    }

    fun fileType(file: File): FileType {
        val filePath = file.absolutePath.lowercase()
        when {
            filePath.endsWith(".java") -> {
                return FileType.JAVA
            }
            filePath.endsWith(".class") -> {
                return FileType.CLASS
            }
            filePath.endsWith(".jar") -> {
                return FileType.JAR
            }
            filePath.endsWith(".kt") -> {
                return FileType.KOTLIN
            }
            filePath.endsWith(".groovy") -> {
                return FileType.GROOVY
            }
            else -> {
                return FileType.DEFAULT
            }
        }
    }

    fun isClassFile(file: File): Boolean {
        return fileType(file) == FileType.CLASS
    }

    fun isClassFile(filePath: String?): Boolean {
        return filePath?.toLowerCase()?.endsWith(".class") == true
    }

    fun <T> fromJsonStringThrowEx(jsonString: String, clazz: Class<T>): T {
        return gson.fromJson(jsonString, clazz)
    }

    fun <T> optFromJsonString(jsonString: String, clazz: Class<T>): T? {
        try {
            return gson.fromJson(jsonString, clazz)
        } catch (e: Throwable) {
            logger()
                .warn("optFromJsonString(${jsonString}, $clazz", e)
        }
        return null
    }

    fun <T> fromJsonStringThrowEx(jsonString: String, typeOfT: Type): T {
        return gson.fromJson(jsonString, typeOfT)
    }

    fun <T> optFromJsonString(jsonString: String, typeOfT: Type): T? {
        try {
            return gson.fromJson(jsonString, typeOfT)
        } catch (e: JsonSyntaxException) {
            logger()
                .warn("optFromJsonString(${jsonString}, $typeOfT", e)
        }
        return null
    }

    fun toJsonStringThrowEx(any: Any): String {
        return gson.toJson(any)
    }

    fun optToJsonString(any: Any): String? {
        try {
            return gson.toJson(any)
        } catch (throwable: Throwable) {
            logger().warn("optToJsonString(${any}", throwable)
        }
        return null
    }

    fun isExcludeFilterMatched(str: String?, filters: List<String>?): Boolean {
        return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
    }

    fun isIncludeFilterMatched(str: String?, filters: List<String>?): Boolean {
        return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
    }

    private fun isFilterMatched(
        str: String?,
        filters: List<String>?,
        filterPolicy: FilterPolicy
    ): Boolean {
        if (str == null) {
            return false
        }

        if (filters.isNullOrEmpty()) {
            return filterPolicy == FilterPolicy.INCLUDE
        }

        for (s in filters) {
            if (isContained(str, s)) {
                return true
            }
        }

        return false
    }

    private fun isContained(str: String?, filter: String): Boolean {
        if (str == null) {
            return false
        }

        if (str.contains(filter)) {
            return true
        } else {
            if (filter.contains("/")) {
                return str.contains(filter.replace("/", File.separator))
            } else if (filter.contains("\\")) {
                return str.contains(filter.replace("\\", File.separator))
            }
        }

        return false
    }

    enum class FilterPolicy {
        INCLUDE,
        EXCLUDE
    }

    fun countOfFiles(file: File): Int {
        return if (file.isFile) {
            1
        } else {
            val files = file.listFiles()
            var total = 0
            files?.let {
                for (f in it) {
                    total += countOfFiles(f)
                }
            }

            total
        }
    }

    fun isJarInputMatched(
        jarInput: JarInput,
        includes: List<String>,
        excludes: List<String>
    ): Boolean {
        JarFile(jarInput.file).use {
            var isIncludeMatched = includes.isEmpty()
            var isExcludeMatched = false
            val entries = it.entries()
            while (entries.hasMoreElements()) {
                val jarEntry = entries.nextElement()
                val entryName = jarEntry.name
                // 只对class文件进行判断，jar如果是不包含任何class文件，则不需要处理
                if (!jarEntry.isDirectory && isClassFile(entryName)) {
                    val tranEntryName = entryName.replace("/", ".")
                        .replace("\\", ".")
                    if (!isIncludeMatched && isIncludeFilterMatched(tranEntryName, includes)) {
                        isIncludeMatched = true
                    }
                    if (isExcludeFilterMatched(tranEntryName, excludes)) {
                        isExcludeMatched = true
                        break
                    }
                }
            }
            return isIncludeMatched && isExcludeMatched.not()
        }
    }

    fun mergeJar(sourceDir: File, targetJar: File) {
        mergeJar(listOf(sourceDir), targetJar)
    }

    fun mergeJar(sourceDirList: List<File>, targetJar: File) {
        if (!targetJar.parentFile.exists()) {
            FileUtils.forceMkdir(targetJar.parentFile)
        }

        FileUtils.deleteQuietly(targetJar)

        val jarMerger = JarMerger(targetJar)
        try {
            jarMerger.setFilter(object : JarMerger.IZipEntryFilter {
                override fun checkEntry(archivePath: String): Boolean {
                    return archivePath.endsWith(".class")
                }
            })

            sourceDirList.forEach {
                jarMerger.addFolder(it)
            }
        } catch (e: Exception) {
            logger()
                .warn("mergeJar(${sourceDirList}, $targetJar", e)
        } finally {
            jarMerger.close()
        }
    }

    private fun logger() = LoggerFactory.getLogger(AJXPlugin::class.java)
}


/**
 * Processes each descendant file in this directory and any sub-directories.
 * Processing consists of potentially calling `closure` passing it the current
 * file (which may be a normal file or subdirectory) and then if a subdirectory was encountered,
 * recursively processing the subdirectory. Whether the closure is called is determined by whether
 * the file was a normal file or subdirectory and the value of fileType.
 *
 */
fun File.eachFileRecurse(
    block: (file: File) -> Unit
) {
    val files = this.listFiles() ?: return
    for (file in files) {
        if (file.isDirectory) {
            file.eachFileRecurse(block)
        } else if (file.isFile) {
            block.invoke(file)
        }
    }
}

