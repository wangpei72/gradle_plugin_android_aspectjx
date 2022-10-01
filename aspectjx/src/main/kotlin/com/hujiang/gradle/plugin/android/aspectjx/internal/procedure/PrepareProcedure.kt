package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.*
import com.hujiang.gradle.plugin.android.aspectjx.internal.utils.AJXUtils
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.utils.eachFileRecurse
import java.io.File
import java.util.jar.JarFile

/**
 * 准备工作
 *
 * - 收集所有织入文件
 * - 记录所有符合匹配规则的文件
 */
class PrepareProcedure(procedureContext: ProcedureContext) :
    AbsProcedure(procedureContext) {

    override fun doWorkContinuously(transformInvocation: TransformInvocation): Boolean {
        val buildConfig = procedureContext.buildConfig
        val enable = buildConfig.isEnable()
        // 插件被禁用，移除所有收集信息
        if (enable.not()) {
            logQuiet("aspect disable!!! Remove buildConfig cache.")
            buildConfig.removeAspectFiles()
            buildConfig.removeMatchedInputs()
            buildConfig.removeWeaveOutputs()
            return true
        }
        // 插件启用，进行相关准备工作
        prepare(transformInvocation)

        return true
    }

    private fun prepare(transformInvocation: TransformInvocation) {
        val batchTaskScheduler = BatchTaskScheduler()
        val isIncremental = transformInvocation.isIncremental

        transformInvocation.inputs.forEach { input ->
            // class文件类型（java和kotlin）
            input.directoryInputs.forEach { dirInput ->
                when {
                    isIncremental -> {
                        // 增量编译，遍历变化文件
                        dirInput.changedFiles.forEach { (inputFile, status) ->
                            batchTaskScheduler.schedule {
                                doCollectForIncrement(inputFile, dirInput, status)
                            }
                        }
                    }
                    else -> {
                        // dirInput目录形式存在，遍历每一个文件
                        dirInput.file.eachFileRecurse { inputFile ->
                            batchTaskScheduler.schedule {
                                doCollectForIncrement(inputFile, dirInput, Status.ADDED)
                            }
                        }
                    }
                }
            }

            // jar类型（module被app使用时也是以jar体现）
            input.jarInputs.forEach { jarInput ->
                batchTaskScheduler.schedule {
                    val inputFile = jarInput.file
                    when {
                        isIncremental -> {
                            // 增量编译
                            doCollectForIncrement(inputFile, jarInput, jarInput.status)
                        }
                        else -> {
                            doCollectForIncrement(inputFile, jarInput, Status.ADDED)
                        }
                    }
                }

            }
        }

        batchTaskScheduler.execute()
        batchTaskScheduler.shutDown()
    }

    private fun doCollectForIncrement(
        inputFile: File,
        qualifiedContent: QualifiedContent,
        status: Status
    ) {
        val buildConfig = procedureContext.buildConfig
        when (status) {
            Status.ADDED -> {
                collect(inputFile, qualifiedContent)
            }
            Status.CHANGED -> {
                if (buildConfig.removeAspectFile(inputFile)) {
                    logQuiet("remove aspect file from:${inputFile.absolutePath}")
                }
                buildConfig.removeMatchedInput(inputFile)
                collect(inputFile, qualifiedContent)
            }
            Status.REMOVED -> {
                if (buildConfig.removeAspectFile(inputFile)) {
                    logQuiet("remove aspect file from:${inputFile.absolutePath}")
                }
                buildConfig.removeMatchedInput(inputFile)
            }
            Status.NOTCHANGED -> {}
        }
    }

    private fun collect(inputFile: File, qualifiedContent: QualifiedContent) {
        when (qualifiedContent) {
            is DirectoryInput -> {
                collectFromDirectoryInput(inputFile, qualifiedContent)
            }
            is JarInput -> {
                collectFromJarInput(inputFile)
            }
        }
    }

    private fun collectFromJarInput(inputFile: File) {
        val buildConfig = procedureContext.buildConfig
        val extension = procedureContext.buildConfig.extension
        val includes = extension.includes
        val excludes = extension.excludes
        JarFile(inputFile).use { jarFile ->
            val entries = jarFile.entries()
            var isIncludeMatched = false
            var isExcludeMatched = false
            while (entries.hasMoreElements()) {
                val jarEntry = entries.nextElement()
                val entryName = jarEntry.name
                if (!jarEntry.isDirectory && AJXUtils.isClassFile(entryName)) {
                    val bytes = jarFile.getInputStream(jarEntry).readBytes()
                    if (AJXUtils.isAspectClass(bytes)) {
                        logQuiet("collect aspect file[${entryName}] from JarInput:$inputFile")
                        val target = File(procedureContext.aspectFilesDir, entryName)
                        buildConfig.addAspectFile(inputFile, target, bytes)
                    }
                    // 只对class文件进行判断，jar如果是不包含任何class文件，则不需要处理
                    val tranEntryName = entryName.replace("/", ".")
                        .replace("\\", ".")
                    if (isExcludeMatched.not() && isIncludeMatched.not()) {
                        isIncludeMatched = AJXUtils.isIncludeFilterMatched(tranEntryName, includes)
                    }
                    if (isExcludeMatched.not()) {
                        isExcludeMatched = AJXUtils.isExcludeFilterMatched(tranEntryName, excludes)
                    }
                }
            }
            // 符合includes且不符合excludes
            if (isIncludeMatched && isExcludeMatched.not()) {
                buildConfig.addMatchedInput(inputFile)
            }
        }
    }

    private fun collectFromDirectoryInput(file: File, dirInput: DirectoryInput) {
        val buildConfig = procedureContext.buildConfig
        val path = file.absolutePath
        // 保留完整包名路径
        val subPath = path.substring(dirInput.file.absolutePath.length)
        // 获取织入规则文件
        if (AJXUtils.isAspectClass(file)) {
            logQuiet("collect aspect file from DirectoryInput:${file}")
            val targetFile = File(procedureContext.aspectFilesDir, subPath)
            buildConfig.addAspectFile(file, targetFile)
        }
        collectMatchedInput(file, dirInput)
    }

    private fun collectMatchedInput(inputFile: File, qualifiedContent: QualifiedContent) {
        val buildConfig = procedureContext.buildConfig
        val extension = procedureContext.buildConfig.extension
        val includes = extension.includes
        val excludes = extension.excludes
        when (qualifiedContent) {
            is DirectoryInput -> {
                // 只处理字节码文件文件
                if (AJXUtils.isClassFile(inputFile).not()) {
                    return
                }
                // 保留完整包名路径
                val packagePath = inputFile.absolutePath
                    .substring(qualifiedContent.file.absolutePath.length)
                    .replace(File.separator, ".")
                // 判断是否符合过滤条件
                val matched = AJXUtils.isIncludeFilterMatched(packagePath, includes)
                        && !AJXUtils.isExcludeFilterMatched(packagePath, excludes)
                if (matched) {
                    buildConfig.addMatchedInput(inputFile)
                }
            }
            is JarInput -> {
                if (AJXUtils.isJarInputMatched(qualifiedContent, includes, excludes)) {
                    buildConfig.addMatchedInput(inputFile)
                }
            }
        }

    }

}
