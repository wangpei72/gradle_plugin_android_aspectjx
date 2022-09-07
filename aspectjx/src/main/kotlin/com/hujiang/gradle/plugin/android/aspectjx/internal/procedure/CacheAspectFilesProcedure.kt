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

import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import com.hujiang.gradle.plugin.android.aspectjx.internal.eachFileRecurse
import org.gradle.api.Project
import java.io.File
import java.util.jar.JarFile

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class CacheAspectFilesProcedure(
    project: Project,
    variantCache: VariantCache,
    transformInvocation: TransformInvocation
) : AbsProcedure(project, variantCache, transformInvocation) {

    override fun doWorkContinuously(): Boolean {
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~cache aspect files")
        //缓存aspect文件
        val batchTaskScheduler = BatchTaskScheduler()

        transformInvocation.inputs.forEach { input ->

            input.directoryInputs.forEach { dirInput ->
//                    collect aspect file
                batchTaskScheduler.addTask(object : ITask {
                    override fun call(): Any? {
                        dirInput.file.eachFileRecurse { item ->
                            if (AJXUtils.isAspectClass(item)) {
                                project.logger.warn("[ajx] collect aspect file:${item.absolutePath}")
                                val path = item.absolutePath
                                val subPath = path.substring(dirInput.file.absolutePath.length)
                                val cacheFile = File(variantCache.aspectPath + subPath)
                                variantCache.add(item, cacheFile)
                            }
                        }

                        return null
                    }
                })
            }

            input.jarInputs.asSequence()
                .filter { it.file.exists() }
                .forEach { jarInput ->
                    batchTaskScheduler.addTask(object : ITask {
                        override fun call(): Any? {
                            val jarFile = JarFile(jarInput.file)
                            val entries = jarFile.entries()
                            while (entries.hasMoreElements()) {
                                val jarEntry = entries.nextElement()
                                val entryName = jarEntry.name
                                if (!jarEntry.isDirectory && AJXUtils.isClassFile(entryName)) {
                                    val bytes = jarFile.getInputStream(jarEntry).readBytes()
                                    if (AJXUtils.isAspectClass(bytes)) {
                                        project.logger.warn("[ajx] collect aspect file[${entryName}] from JAR:${jarFile}")
                                        val cacheFile =
                                            File(variantCache.aspectPath + File.separator + entryName)
                                        variantCache.add(bytes, cacheFile)
                                    }
                                }
                            }

                            jarFile.close()

                            return null
                        }
                    })
                }
        }

        batchTaskScheduler.execute()
        batchTaskScheduler.shutDown()

        if (AJXUtils.countOfFiles(variantCache.getAspectDir()) == 0) {
            AJXUtils.doWorkWithNoAspectj(transformInvocation)
            variantCache.ajxCache.commit()
            return false
        }

        return true
    }
}
