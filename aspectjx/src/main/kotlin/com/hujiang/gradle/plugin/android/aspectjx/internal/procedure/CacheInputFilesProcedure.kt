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
import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import com.hujiang.gradle.plugin.android.aspectjx.internal.eachFileRecurse
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class CacheInputFilesProcedure(
    variantCache: VariantCache,
    transformInvocation: TransformInvocation
) : AbsProcedure(variantCache, transformInvocation) {

    override fun doWorkContinuously(): Boolean {
        //过滤规则
        //
        // "*" 所有class文件和jar
        // "**" 所有class文件和jar
        // "com.hujiang" 过滤 含"com.hujiang"的文件和jar
        //
        LoggerHolder.logger.debug("~~~~~~~~~~~~~~~~~~~~cache input files")
        val taskScheduler = BatchTaskScheduler()

        val excludeDir = variantCache.getExcludeFileDir()
        transformInvocation.inputs.forEach { input ->
            input.directoryInputs.forEach { dirInput ->
                taskScheduler.addTask(object : ITask {
                    override fun call(): Any? {
                        dirInput.file.eachFileRecurse { item ->
                            if (AJXUtils.isClassFile(item)) {
                                val path = item.absolutePath
                                val subPath = path.substring(dirInput.file.absolutePath.length)
                                val transPath = subPath.replace(File.separator, ".")

                                val isInclude = AJXUtils.isIncludeFilterMatched(
                                    transPath,
                                    ajxExtensionConfig.includes
                                ) &&
                                        !AJXUtils.isExcludeFilterMatched(
                                            transPath,
                                            ajxExtensionConfig.excludes
                                        )
                                val cacheDir =
                                    if (isInclude) variantCache.getIncludeFileDir() else excludeDir
                                variantCache.add(
                                    item,
                                    File(cacheDir, subPath)
                                )
                            }
                        }

                        return null
                    }
                })
            }

            input.jarInputs.forEach { jarInput ->
                taskScheduler.addTask(object : ITask {
                    override fun call(): Any? {
                        AJXUtils.filterJar(
                            jarInput,
                            variantCache,
                            ajxExtensionConfig.includes,
                            ajxExtensionConfig.excludes
                        )
                        if (!variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                            val dest = transformInvocation.outputProvider.getContentLocation(
                                jarInput.name,
                                jarInput.contentTypes,
                                jarInput.scopes,
                                Format.JAR
                            )
                            FileUtils.copyFile(jarInput.file, dest)
                        }

                        return null
                    }
                })

            }
        }

        taskScheduler.execute()
        taskScheduler.shutDown()

        //put exclude files into jar
        if (AJXUtils.countOfFiles(excludeDir) > 0) {
            val excludeJar = transformInvocation.outputProvider.getContentLocation(
                "exclude",
                variantCache.contentTypes,
                variantCache.scopes,
                Format.JAR
            )
            FileUtils.deleteQuietly(excludeJar)
            AJXUtils.mergeJar(excludeDir, excludeJar)
        }

        variantCache.commitIncludeJarConfig()

        return true
    }
}
