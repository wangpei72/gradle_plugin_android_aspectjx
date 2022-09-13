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
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXTask
import com.hujiang.gradle.plugin.android.aspectjx.internal.AJXTaskManager
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-23
 */
class DoAspectWorkProcedure(
    variantCache: VariantCache,
    transformInvocation: TransformInvocation
) : AbsProcedure(variantCache, transformInvocation) {

    private val ajxTaskManager: AJXTaskManager

    init {
        val ajxCache = variantCache.ajxCache
        ajxTaskManager = AJXTaskManager().apply {
            encoding = ajxCache.encoding ?: ""
            ajcArgs = ajxCache.ajxExtensionConfig.ajcArgs
            bootClassPath = ajxCache.bootClassPath
            sourceCompatibility = ajxCache.sourceCompatibility
            targetCompatibility = ajxCache.targetCompatibility
        }
    }

    override fun doWorkContinuously(): Boolean {
        //do aspectj real work
        LoggerHolder.logger.debug("~~~~~~~~~~~~~~~~~~~~do aspectj real work")
        ajxTaskManager.aspectPath.add(variantCache.getAspectDir())
        ajxTaskManager.classPath.add(variantCache.getIncludeFileDir())
        ajxTaskManager.classPath.add(variantCache.getExcludeFileDir())

        //process class files
        val ajxTask = AJXTask()
        val includeJar = transformInvocation.outputProvider.getContentLocation(
            "include",
            variantCache.contentTypes,
            variantCache.scopes,
            Format.JAR
        )

        if (!includeJar.parentFile.exists()) {
            FileUtils.forceMkdir(includeJar.parentFile)
        }

        FileUtils.deleteQuietly(includeJar)

        ajxTask.outputJar = includeJar.absolutePath
        ajxTask.inPath.add(variantCache.getIncludeFileDir())
        ajxTaskManager.addTask(ajxTask)

        //process jar files
        transformInvocation.inputs.forEach { input ->
            input.jarInputs.forEach { jarInput ->
                ajxTaskManager.classPath.add(jarInput.file)

                if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                    val jarTask = AJXTask()
                    jarTask.inPath.add(jarInput.file)

                    val outputJar = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR
                    )
                    if (outputJar.parentFile?.exists() == false) {
                        outputJar.parentFile?.mkdirs()
                    }

                    jarTask.outputJar = outputJar.absolutePath

                    ajxTaskManager.addTask(jarTask)
                }
            }
        }

        ajxTaskManager.batchExecute()

        return true
    }
}
