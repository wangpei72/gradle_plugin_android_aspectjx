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
class UpdateAspectOutputProcedure(
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
        LoggerHolder.logger.debug("~~~~~~~~~~~~~~~~~~~~update aspect output")
        ajxTaskManager.aspectPath.add(variantCache.getAspectDir())
        ajxTaskManager.classPath.add(variantCache.getIncludeFileDir())
        ajxTaskManager.classPath.add(variantCache.getExcludeFileDir())

        val isAspectChanged = variantCache.incrementalStatus.isAspectChanged
        val isIncludeFileChanged = variantCache.incrementalStatus.isIncludeFileChanged
        if (isAspectChanged) {
            LoggerHolder.logger.warn("[ajx][${variantCache.variantName}] aspect file changed, need rerun.")
        }
        if (isAspectChanged || isIncludeFileChanged) {
            //process class files
            val ajxTask = AJXTask()
            val outputJar = transformInvocation.outputProvider.getContentLocation(
                "include",
                variantCache.contentTypes,
                variantCache.scopes,
                Format.JAR
            )
            FileUtils.deleteQuietly(outputJar)

            ajxTask.outputJar = outputJar.absolutePath
            ajxTask.inPath.add(variantCache.getIncludeFileDir())

            ajxTaskManager.addTask(ajxTask)
        }

        transformInvocation.inputs.forEach { input ->
            input.jarInputs.asSequence()
                .filter { it.file.exists() }
                .forEach { jarInput ->
                    ajxTaskManager.classPath.add(jarInput.file)
                    val outputJar = transformInvocation.outputProvider.getContentLocation(
                        jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        Format.JAR
                    )

                    if (outputJar.parentFile?.exists() == false) {
                        outputJar.parentFile?.mkdirs()
                    }

                    if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                        // 规则文件变更，删除重新处理
                        if (isAspectChanged) {
                            FileUtils.deleteQuietly(outputJar)
                        }
                        if (!outputJar.exists()) {
                            val jarTask = AJXTask()
                            jarTask.inPath.add(jarInput.file)

                            jarTask.outputJar = outputJar.absolutePath

                            ajxTaskManager.addTask(jarTask)
                        }
                    } else {
                        // 将不需要做AOP处理的文件原样copy到输出目录
                        FileUtils.copyFile(jarInput.file, outputJar)
                    }
                }
        }

        ajxTaskManager.batchExecute()

        return true
    }
}
