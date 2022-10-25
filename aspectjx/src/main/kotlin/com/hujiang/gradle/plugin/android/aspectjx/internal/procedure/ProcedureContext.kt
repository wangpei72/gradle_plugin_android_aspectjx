package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.AJXExtension
import com.hujiang.gradle.plugin.android.aspectjx.internal.utils.AJXUtils
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * Created by LanceWu on 2022/9/22
 *
 * 任务处理上下文
 */
class ProcedureContext(
    /**
     * TransformInvocation
     */
    val transformInvocation: TransformInvocation,
    /**
     * 编译选项
     */
    val compileOptions: CompileOptions,
    /**
     * 拓展参数
     */
    extension: AJXExtension
) {

    /**
     * 临时目录
     */
    private val temporaryDir: File = transformInvocation.context.temporaryDir

    /**
     * 日志目录
     */
    val logsDir = File(temporaryDir, "logs")

    /**
     * 规则文件目录
     */
    val aspectFilesDir = File(temporaryDir, "aspectFiles")

    /**
     * 用于织入的临时文件目录
     */
    val weaveTmpDir = File(temporaryDir, "weaveTmp")

    /**
     * 构建缓存文件
     */
    private val buildConfigCacheFile = File(temporaryDir, "buildConfigCache.json")

    /**
     * 上次构建配置
     */
    val lastBuildConfig: BuildConfig

    /**
     * 当前构建配置
     */
    val buildConfig = BuildConfig()

    init {
        // 全量编译，清除产物和自定义的缓存文件
        if (transformInvocation.isIncremental.not()) {
            transformInvocation.outputProvider.deleteAll()
            FileUtils.deleteDirectory(temporaryDir)
        }
        // 加载上次构建配置
        lastBuildConfig = BuildConfig.loadBuildConfig(buildConfigCacheFile)
        // 初始化当前构建配置
        buildConfig.apply {
            // 配置
            this.extension = extension
            // 从上次构建中继承
            this.aspectFiles.addAll(lastBuildConfig.aspectFiles)
            this.matchedInputs.addAll(lastBuildConfig.matchedInputs)
            this.weaveOutputs.addAll(lastBuildConfig.weaveOutputs)
        }
    }

    /**
     * 前后构建织入规则文件发生改变
     * @return Boolean
     */
    fun isAspectFilesChanged(): Boolean {
        return buildConfig.aspectFiles != lastBuildConfig.aspectFiles
    }

    fun saveBuildConfig() {
        FileUtils.write(buildConfigCacheFile, AJXUtils.optToJsonString(buildConfig), "UTF-8")
    }

    /**
     * 织入文件
     * @property source 来源：class或者jar
     * @property target 拷贝的织入文件路径
     * @property lastModified 最后修改时间，用来判断织入文件是否发生修改
     */
    data class AspectFile(
        var source: String = "",
        var target: String = "",
        var lastModified: Long = 0L
    )

    /**
     * 符合过滤规则的输入文件
     * @property source 源文件路径
     */
    data class MatchedInput(
        var source: String = ""
    )

    /**
     * 织入输出
     * @property source String
     * @property outputFiles 所有输出文件（aspectj织入后可能会输出多个文件）
     * @property costMillis 耗时
     */
    data class WeaveOutput(
        var source: String = "",
        var outputFiles: MutableList<String> = mutableListOf(),
        var costMillis: Long = 0L
    )

    /**
     * 编译选项
     * @property encoding String
     * @property bootClassPath String
     * @property sourceCompatibility String
     * @property targetCompatibility String
     * @property javaCompileClasspath String
     */
    class CompileOptions {
        var encoding: String = ""
        var bootClassPath: String = ""
        var sourceCompatibility: String = ""
        var targetCompatibility: String = ""
        var javaCompileClasspath = mutableSetOf<File>()
    }


    /**
     * 构建配置
     * @property extension AJXExtension
     * @property aspectFiles MutableList<AspectFile>
     * @property matchedInputs MutableList<MatchedInput>
     */
    class BuildConfig {

        companion object {

            fun loadBuildConfig(buildConfigCacheFile: File): BuildConfig {
                return if (buildConfigCacheFile.exists()) {
                    val jsonString = FileUtils.readFileToString(buildConfigCacheFile)
                    AJXUtils.optFromJsonString(jsonString, BuildConfig::class.java) ?: BuildConfig()
                } else {
                    BuildConfig()
                }
            }
        }

        /**
         * 配置缓存文件
         */
        var extension: AJXExtension = AJXExtension()

        /**
         * 织入规则文件列表
         */
        var aspectFiles = mutableListOf<AspectFile>()

        /**
         * 符合过滤规则的输入文件，包括class和jar
         */
        var matchedInputs = mutableListOf<MatchedInput>()

        /**
         * 织入输出
         */
        var weaveOutputs = mutableListOf<WeaveOutput>()

        /**
         * 是否存在织入规则文件
         */
        fun existAspectFiles(): Boolean {
            // 有织入规则文件并且有需要被处理的文件
            return aspectFiles.isNotEmpty()
        }

        /**
         * 是否启用
         * @return Boolean
         */
        fun isEnable(): Boolean {
            return extension.enabled
        }

        /**
         * 添加织入文件（来源[classBytes]）
         * @param source File
         * @param target File
         * @param classBytes ByteArray
         */
        fun addAspectFile(source: File, target: File, classBytes: ByteArray) {
            FileUtils.writeByteArrayToFile(target, classBytes)
            addAspectFileToList(source, target)
        }

        /**
         * 添加织入文件（从[source]进行拷贝）
         * @param source File
         * @param target String
         */
        fun addAspectFile(source: File, target: File) {
            // 拷贝
            FileUtils.copyFile(source, target)
            addAspectFileToList(source, target)
        }

        private fun addAspectFileToList(source: File, target: File) {
            val aspectFile = AspectFile().apply {
                this.source = source.absolutePath
                this.target = target.absolutePath
                this.lastModified = target.lastModified()
            }
            synchronized(aspectFiles) {
                aspectFiles.add(aspectFile)
            }
        }

        /**
         * 删除对应aspect文件，如果存在
         * @param source File
         * @return Boolean
         */
        fun removeAspectFile(source: File): Boolean {
            synchronized(aspectFiles) {
                // 对于jar，不同aspect文件可能来自同一个jar，所以要完整遍历
                return aspectFiles.removeIf {
                    val matched = it.source == source.absolutePath
                    if (matched) {
                        FileUtils.deleteQuietly(File(it.target))
                    }
                    matched
                }
            }
        }

        /**
         * 移除所有
         * @return Boolean
         */
        fun removeAspectFiles() {
            synchronized(aspectFiles) {
                aspectFiles.removeIf {
                    FileUtils.deleteQuietly(File(it.target))
                    true
                }
            }
        }

        /**
         * 是否符合当前过滤规则
         * @param source File
         * @return Boolean
         */
        fun isMatchedInput(source: File): Boolean {
            return matchedInputs.firstOrNull { it.source == source.absolutePath } != null
        }

        /**
         * 添加到符合过滤规则的文件列表
         * @param source File
         */
        fun addMatchedInput(source: File) {
            val matchedInput = MatchedInput().apply {
                this.source = source.absolutePath
            }
            synchronized(matchedInputs) {
                matchedInputs.add(matchedInput)
            }
        }

        /**
         * 从符合过滤规则的文件列表移除
         * @param source File
         * @return Boolean
         */
        fun removeMatchedInput(source: File): Boolean {
            synchronized(matchedInputs) {
                return matchedInputs.removeIf { it.source == source.absolutePath }
            }
        }

        /**
         * 移除所有
         */
        fun removeMatchedInputs() {
            synchronized(matchedInputs) {
                matchedInputs.clear()
            }
        }

        /**
         * 添加到织入输出列表
         * @param source File
         * @param outputFiles MutableList<String>
         * @param costTimeMillis Long 耗时
         */
        fun addWeaveOutput(source: File, outputFiles: MutableList<String>, costTimeMillis: Long) {
            val weaveOutput = WeaveOutput().apply {
                this.source = source.absolutePath
                this.outputFiles = outputFiles
                this.costMillis = costTimeMillis
            }
            synchronized(weaveOutputs) {
                weaveOutputs.add(weaveOutput)
            }
        }

        /**
         * 从织入输出的列表移除
         * @param source File
         * @return Boolean
         */
        fun removeWeaveOutput(source: File): Boolean {
            synchronized(weaveOutputs) {
                return weaveOutputs.removeIf {
                    val matched = it.source == source.absolutePath
                    if (matched) {
                        for (outputFile in it.outputFiles) {
                            FileUtils.deleteQuietly(File(outputFile))
                        }
                    }
                    matched
                }
            }
        }

        /**
         * 移除所有
         */
        fun removeWeaveOutputs() {
            synchronized(weaveOutputs) {
                weaveOutputs.removeIf {
                    for (outputFile in it.outputFiles) {
                        FileUtils.deleteQuietly(File(outputFile))
                    }
                    true
                }
            }
        }
    }
}