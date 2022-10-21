package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.*
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import com.hujiang.gradle.plugin.android.aspectjx.internal.utils.eachFileRecurse
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * 执行织入任务
 */
class DoAspectProcedure(procedureContext: ProcedureContext) : AbsProcedure(procedureContext) {

    override fun doWorkContinuously(transformInvocation: TransformInvocation): Boolean {
        // 需要织入
        process(transformInvocation)
        // 保存配置
        procedureContext.saveBuildConfig()
        return true
    }

    private fun process(transformInvocation: TransformInvocation) {
        val batchTaskScheduler = BatchTaskScheduler()
        val isIncremental = transformInvocation.isIncremental
        val outputProvider = transformInvocation.outputProvider

        transformInvocation.inputs.forEach { transformInput ->

            // 目录类型（java和kotlin产物目录）
            transformInput.directoryInputs.forEach { dirInput ->
                batchTaskScheduler.schedule {
                    processDirectoryInput(isIncremental, dirInput, outputProvider)
                }
            }

            // jar类型（module被app使用时也是以jar体现）
            transformInput.jarInputs.forEach { jarInput ->
                batchTaskScheduler.schedule {
                    val format = Format.JAR
                    val inputFile = jarInput.file
                    val outputFile = outputProvider.getContentLocation(
                        jarInput.name,
                        jarInput.contentTypes,
                        jarInput.scopes,
                        format
                    )
                    val inputOutput = InputOutput(format, inputFile, outputFile, outputFile)
                    val status = if (isIncremental) jarInput.status else Status.ADDED
                    processJarInput(inputOutput, status)
                }
            }
        }

        batchTaskScheduler.execute()
        batchTaskScheduler.shutDown()
    }

    private fun processDirectoryInput(
        isIncrement: Boolean,
        dirInput: DirectoryInput,
        outputProvider: TransformOutputProvider
    ) {
        val buildConfig = procedureContext.buildConfig
        val lastBuildConfig = procedureContext.lastBuildConfig
        val existAspectFiles = buildConfig.existAspectFiles()
        // 对于目录类型，把不需要处理的直接拷贝到输出目录；需要处理的，先拷贝到临时输入目录，然后处理输出到临时输出目录，记录信息后，再拷贝到最终目录
        val format = Format.DIRECTORY
        val dest = outputProvider.getContentLocation(
            dirInput.name,
            dirInput.contentTypes,
            dirInput.scopes,
            format
        )
        val matchedInputDir =
            File(procedureContext.weaveTmpDir, dirInput.name + File.separator + "input")
        val matchedOutputDir =
            File(procedureContext.weaveTmpDir, dirInput.name + File.separator + "output")
        // 1.织入文件变更，需要重新处理；2.输入文件发生变更，需要重新处理
        var matchedInputInvalidate = procedureContext.isAspectFilesChanged()
        // 收集匹配输入文件
        val collectMatchedInputFile = collectMatchedInputFile@{ inputFile: File, status: Status ->
            logInfo("collectMatchedInputFile:$status-${inputFile.path}")
            if (inputFile.isDirectory) {
                // 新增类为放在新包下面时，目录也会触发ADDED，过滤掉
                return@collectMatchedInputFile
            }
            // 截取到包名的文件路径
            val packagePath = inputFile.absolutePath.removePrefix(dirInput.file.absolutePath)
            val isMatchedInput = buildConfig.isMatchedInput(inputFile)
            val isMatchedInputLastBuild = lastBuildConfig.isMatchedInput(inputFile)
            val matchedInputFile = File(matchedInputDir, packagePath)
            when (status) {
                Status.ADDED -> {
                    // 符合匹配，拷贝到临时输入
                    if (isMatchedInput) {
                        matchedInputInvalidate = true
                        FileUtils.copyFile(inputFile, matchedInputFile)
                    } else {
                        // 不需要处理的，直接拷贝
                        val outputFile = File(dest, packagePath)
                        FileUtils.copyFile(inputFile, outputFile)
                    }
                }
                Status.CHANGED -> {
                    // 如果变更的文件之前是符合匹配规则的，重新拷贝到临时输入目录
                    if (isMatchedInputLastBuild) {
                        matchedInputInvalidate = true
                        FileUtils.copyFile(inputFile, matchedInputFile)
                    } else {
                        // 之前是拷贝的，重新拷贝
                        val outputFile = File(dest, packagePath)
                        FileUtils.copyFile(inputFile, outputFile)
                    }
                }
                Status.REMOVED -> {
                    // 如果被移除文件之前是符合匹配规则的，从临时输入目录中移除
                    if (isMatchedInputLastBuild) {
                        matchedInputInvalidate = true
                        FileUtils.deleteQuietly(matchedInputFile)
                    } else {
                        // 之前是拷贝的，直接删除
                        val outputFile = File(dest, packagePath)
                        FileUtils.deleteQuietly(outputFile)
                    }
                }
                else -> {
                    // 无需处理
                }
            }

        }

        // 增量遍历变更列表
        if (isIncrement) {
            dirInput.changedFiles.forEach { (file, status) ->
                collectMatchedInputFile(file, status)
            }
        } else {
            // 全量遍历所有文件
            dirInput.file.eachFileRecurse {
                collectMatchedInputFile(it, Status.ADDED)
            }
        }

        // 输入已失效，需要重新处理
        if (matchedInputInvalidate) {
            // 清除旧产物重新处理
            procedureContext.buildConfig.removeWeaveOutput(matchedInputDir)
            // 清除织入临时目录
            FileUtils.deleteQuietly(matchedOutputDir)
            // 无任何匹配文件，无需处理
            if (matchedInputDir.list().isNullOrEmpty()) {
                return
            }
            // 存在织入规则文件，需要执行织入
            if (existAspectFiles) {
                val inputOutput = InputOutput(format, matchedInputDir, matchedOutputDir, dest)
                runAJXTask(inputOutput)
            } else {
                // 直接拷贝到最终目录
                matchedInputDir.eachFileRecurse {
                    val target = it.absolutePath.replaceFirst(
                        matchedInputDir.absolutePath,
                        dest.absolutePath
                    )
                    FileUtils.copyFile(it, File(target))
                }
            }
        }
    }

    private fun processJarInput(
        inputOutput: InputOutput,
        status: Status
    ) {
        val inputFile = inputOutput.inputFile
        val outputFile = inputOutput.outputFile
        val buildConfig = procedureContext.buildConfig
        val existAspectFiles = buildConfig.existAspectFiles()
        val isMatchedInput = buildConfig.isMatchedInput(inputFile)
        val needAspect = existAspectFiles && isMatchedInput
        logInfo("processJarInput:$status-${inputFile.path}")
        // 增量编译
        when (status) {
            Status.ADDED -> {
                if (needAspect) {
                    runAJXTask(inputOutput)
                } else {
                    // 不需要处理，直接拷贝
                    FileUtils.copyFile(inputFile, outputFile)
                }
            }
            Status.CHANGED -> {
                // 输入文件变更，需要处理
                deleteOutput(inputOutput)
                if (needAspect) {
                    runAJXTask(inputOutput)
                } else {
                    // 不需要处理，直接拷贝
                    FileUtils.copyFile(inputFile, outputFile)
                }
            }
            Status.REMOVED -> {
                // 直接删除
                deleteOutput(inputOutput)
            }
            Status.NOTCHANGED -> {
                // 输入文件未修改，但是织入规则发生变更，所有匹配规则的文件都需要重新处理
                if (isMatchedInput && procedureContext.isAspectFilesChanged()) {
                    deleteOutput(inputOutput)
                    if (needAspect) {
                        runAJXTask(inputOutput)
                    } else {
                        // 不需要处理，直接拷贝
                        FileUtils.copyFile(inputFile, outputFile)
                    }
                }
            }
        }
    }

    private fun runAJXTask(inputOutput: InputOutput) {
        val compileOptions = procedureContext.compileOptions
        val format = inputOutput.format
        val inputFile = inputOutput.inputFile
        val outputFile = inputOutput.outputFile
        val dest = inputOutput.dest
        val extension = procedureContext.buildConfig.extension
        AJXTask().run {
            // 设置通用参数
            this.logWeaveInfo = extension.debug
            this.loggerPrefix = this@DoAspectProcedure.loggerPrefix
            this.dumpDirectory = procedureContext.logsDir
            this.encoding = compileOptions.encoding
            this.sourceCompatibility = compileOptions.sourceCompatibility
            this.targetCompatibility = compileOptions.targetCompatibility
            this.bootClassPath = compileOptions.bootClassPath
            this.classPath = compileOptions.javaCompileClasspath
            this.ajcArgs = extension.ajcArgs
            // 设置织入规则文件所在目录
            this.aspectPath.add(procedureContext.aspectFilesDir)
            if (format == Format.JAR) {
                // 输入jar
                this.inPath.add(inputFile)
                // 输出需要为jar或目录，给定jar
                this.outputJar = dest.absolutePath
            } else {
                // 输入需要为jar或目录
                this.inPath.add(inputFile)
                // 输出到临时目录，结束后拷贝到最终目录，并记录输入和输出的对应关系
                this.outputDir = outputFile.absolutePath
            }
            // 执行织入
            val startTime = System.currentTimeMillis()
            call()
            val cost = System.currentTimeMillis() - startTime
            logInfo("weave [${inputFile}].[${cost}ms]")
            // 对于JAR类型：结束后记录输入和输出的对应关系
            if (format == Format.JAR) {
                val outputFiles = mutableListOf(outputJar!!)
                procedureContext.buildConfig.addWeaveOutput(inputFile, outputFiles, cost)
            } else {
                // 对于class类型：结束后拷贝到最终目录，并记录输入和输出的对应关系
                val outputFiles = mutableListOf<String>()
                outputFile.eachFileRecurse {
                    val target = it.absolutePath.replaceFirst(
                        outputFile.absolutePath,
                        dest.absolutePath
                    )
                    outputFiles.add(target)
                    FileUtils.copyFile(it, File(target))
                }
                procedureContext.buildConfig.addWeaveOutput(inputFile, outputFiles, cost)
            }
        }
    }

    private fun deleteOutput(inputOutput: InputOutput) {
        // 删除拷贝输出
        FileUtils.deleteQuietly(inputOutput.outputFile)
        // 删除上次构建的织入输出
        procedureContext.buildConfig.removeWeaveOutput(inputOutput.inputFile)
    }

    /**
     * 织入输入输出封装
     * @property format Format
     * @property inputFile File 对于[DirectoryInput]，表示需要被织入的文件目录
     * @property outputFile File 对于[DirectoryInput]，表示被织入的临时存放目录
     * @property dest File 对于[DirectoryInput]，表示最终结果需要存放的目录
     * @constructor
     */
    data class InputOutput(
        val format: Format,
        val inputFile: File,
        val outputFile: File,
        val dest: File
    )

}
