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
package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File
import java.util.jar.JarFile

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class UpdateAspectFilesProcedure(
    variantCache: VariantCache,
    transformInvocation: TransformInvocation
) : AbsProcedure(variantCache, transformInvocation) {

    override fun doWorkContinuously(): Boolean {
        LoggerHolder.logger.debug("~~~~~~~~~~~~~~~~~~~~update aspect files")
        //update aspect files
        val taskScheduler = BatchTaskScheduler()

        transformInvocation.inputs.forEach { input ->

            input.directoryInputs.forEach { dirInput ->
                taskScheduler.addTask(object : ITask {
                    override fun call(): Any? {
                        dirInput.changedFiles.forEach { (file, status) ->
                            if (AJXUtils.isAspectClass(file)) {
                                LoggerHolder.logger.warn("[ajx] collect aspect file from Dir:${file.absolutePath}")
                                variantCache.incrementalStatus.isAspectChanged = true
                                val path = file.absolutePath
                                val subPath = path.substring(dirInput.file.absolutePath.length)
                                val cacheFile = File(variantCache.aspectPath + subPath)

                                when (status) {
                                    Status.REMOVED -> {
                                        FileUtils.deleteQuietly(cacheFile)
                                    }
                                    Status.CHANGED -> {
                                        FileUtils.deleteQuietly(cacheFile)
                                        variantCache.add(file, cacheFile)
                                    }
                                    Status.ADDED -> {
                                        variantCache.add(file, cacheFile)
                                    }
                                    else -> {}
                                }
                            }
                        }

                        return null
                    }
                })
            }

            input.jarInputs.asSequence()
                .filter {
                    it.status != Status.NOTCHANGED && it.file.exists().also { exist ->
                        if (exist.not()) {
                            LoggerHolder.logger.warn("[ajx] UpdateAspectFilesProcedure: jarInput[state=${it.status}] not exist [${it.file}]")
                        }
                    }
                }
                .forEach { jarInput ->
                    taskScheduler.addTask(object : ITask {
                        override fun call(): Any? {
                            val jarFile = JarFile(jarInput.file)
                            val entries = jarFile.entries()
                            while (entries.hasMoreElements()) {
                                val jarEntry = entries.nextElement()
                                val entryName = jarEntry.name
                                if (!jarEntry.isDirectory && AJXUtils.isClassFile(entryName)) {
                                    val bytes = jarFile.getInputStream(jarEntry).readBytes()
                                    if (AJXUtils.isAspectClass(bytes)) {
                                        LoggerHolder.logger.warn("[ajx] UpdateAspectFilesProcedure:collect aspect file[${entryName}] from JAR:${jarFile}")
                                        val cacheFile =
                                            File(variantCache.aspectPath + File.separator + entryName)
                                        variantCache.incrementalStatus.isAspectChanged = true
                                        when (jarInput.status) {
                                            Status.REMOVED -> {
                                                // todo 这个机制有问题，remove的jar文件不存在，无法打开，假设该jar包含aspectClass，则无法被正确删除
                                                FileUtils.deleteQuietly(cacheFile)
                                            }
                                            Status.CHANGED -> {
                                                FileUtils.deleteQuietly(cacheFile)
                                                variantCache.add(bytes, cacheFile)
                                            }
                                            Status.ADDED -> {
                                                variantCache.add(bytes, cacheFile)
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }

                            jarFile.close()

                            return null
                        }
                    })
                }
        }

        taskScheduler.execute()
        taskScheduler.shutDown()

        if (AJXUtils.countOfFiles(variantCache.getAspectDir()) == 0) {
            //do work with no aspectj
            AJXUtils.fullCopyFiles(transformInvocation)
            return false
        }

        return true
    }
}
