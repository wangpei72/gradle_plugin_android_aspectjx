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

import com.android.build.api.transform.Format
import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class UpdateInputFilesProcedure(
    variantCache: VariantCache,
    transformInvocation: TransformInvocation
) : AbsProcedure(variantCache, transformInvocation) {

    override fun doWorkContinuously(): Boolean {
        LoggerHolder.logger.debug("~~~~~~~~~~~~~~~~~~~~update input files")
        val taskScheduler = BatchTaskScheduler()

        transformInvocation.inputs.forEach { input ->

            // class文件处理
            input.directoryInputs.forEach { dirInput ->
                taskScheduler.addTask(object : ITask {
                    override fun call(): Any? {
                        dirInput.changedFiles.forEach { (file, status) ->
                            LoggerHolder.logger.debug("~~~~~~~~~~~~~~~~changed file::${status.name}::${file.absolutePath}")

                            val path = file.absolutePath
                            val subPath = path.substring(dirInput.file.absolutePath.length)
                            val transPath = subPath.replace(File.separator, ".")

                            val isInclude = AJXUtils.isIncludeFilterMatched(
                                transPath,
                                ajxExtensionConfig.includes
                            )
                                    && !AJXUtils.isExcludeFilterMatched(
                                transPath,
                                ajxExtensionConfig.excludes
                            )

                            if (isInclude) {
                                variantCache.incrementalStatus.isIncludeFileChanged = true
                            } else {
                                variantCache.incrementalStatus.isExcludeFileChanged = true
                            }

                            val target = File(
                                if (isInclude)
                                    variantCache.getIncludeFileDir()
                                else
                                    variantCache.getExcludeFileDir(),
                                subPath
                            )
                            when (status) {
                                Status.ADDED -> {
                                    variantCache.add(file, target)
                                }
                                Status.CHANGED -> {
                                    FileUtils.deleteQuietly(target)
                                    variantCache.add(file, target)
                                }
                                Status.REMOVED -> {
                                    FileUtils.deleteQuietly(target)
                                }
                                else -> {}
                            }
                        }
                        return null
                    }

                })
            }
            // jar处理
            input.jarInputs
                .asSequence()
                .filter { it.status != Status.NOTCHANGED }
                .forEach { jarInput ->
                    taskScheduler.addTask(object : ITask {
                        override fun call(): Any? {
                            LoggerHolder.logger.debug("~~~~~~~changed file::${jarInput.status.name}::${jarInput.file.absolutePath}")

                            val filePath = jarInput.file.absolutePath
                            val outputJar = transformInvocation.outputProvider.getContentLocation(
                                jarInput.name,
                                jarInput.contentTypes,
                                jarInput.scopes,
                                Format.JAR
                            )

                            when (jarInput.status) {
                                Status.REMOVED -> {
                                    variantCache.removeIncludeJar(filePath)
                                    FileUtils.deleteQuietly(outputJar)
                                }
                                Status.ADDED -> {
                                    AJXUtils.filterJar(
                                        jarInput,
                                        variantCache,
                                        ajxExtensionConfig.includes,
                                        ajxExtensionConfig.excludes
                                    )
                                }
                                Status.CHANGED -> {
                                    FileUtils.deleteQuietly(outputJar)
                                }
                                else -> {}
                            }

                            return null
                        }
                    })
                }
        }

        taskScheduler.execute()
        taskScheduler.shutDown()

        //如果include files 发生变化，则删除include输出jar
        if (variantCache.incrementalStatus.isIncludeFileChanged) {
            val includeOutputJar = transformInvocation.outputProvider.getContentLocation(
                "include",
                variantCache.contentTypes,
                variantCache.scopes,
                Format.JAR
            )
            FileUtils.deleteQuietly(includeOutputJar)
        }

        //如果exclude files发生变化，则重新生成exclude jar到输出目录
        if (variantCache.incrementalStatus.isExcludeFileChanged) {
            val excludeOutputJar = transformInvocation.outputProvider.getContentLocation(
                "exclude",
                variantCache.contentTypes,
                variantCache.scopes,
                Format.JAR
            )
            FileUtils.deleteQuietly(excludeOutputJar)
            AJXUtils.mergeJar(variantCache.getExcludeFileDir(), excludeOutputJar)
        }

        variantCache.commitIncludeJarConfig()

        return true
    }
}
