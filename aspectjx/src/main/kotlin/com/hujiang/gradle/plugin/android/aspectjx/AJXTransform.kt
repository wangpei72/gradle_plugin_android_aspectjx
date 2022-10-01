package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.pipeline.TransformManager
import com.hujiang.gradle.plugin.android.aspectjx.compat.AgpApiCompat
import com.hujiang.gradle.plugin.android.aspectjx.internal.procedure.DoAspectProcedure
import com.hujiang.gradle.plugin.android.aspectjx.internal.procedure.PrepareProcedure
import com.hujiang.gradle.plugin.android.aspectjx.internal.procedure.ProcedureChain
import com.hujiang.gradle.plugin.android.aspectjx.internal.procedure.ProcedureContext
import com.hujiang.gradle.plugin.android.aspectjx.internal.utils.AJXUtils
import org.aspectj.org.eclipse.jdt.internal.compiler.batch.ClasspathJar
import org.gradle.api.Project
import java.io.File

/**
 * Aspect处理<br>
 * 自定义transform几个问题需要注意：
 * <ul>
 *     <li>对于多flavor的构建，每个flavor都会执行transform</li>
 *     <li>对于开启gradle daemon的情况（默认开启的，一般也不会去关闭），每次构建都是运行在同一个进程上，
 *     所以要注意到有没有使用到有状态的静态或者单例，如果有的话，需要在构建结束进行处理，否则会影响到后续的构建</li>
 *     <li>增量构建时，要注意是否需要删除之前在outputProvider下已产生的产物</li>
 * </ul>
 * @author simon* @version 1.0.0* @since 2018-03-12
 */
class AJXTransform(project: Project) : Transform() {

    companion object {
        const val TAG = "ajx"
    }

    /**
     * 是否是library
     */
    private val isLibrary = project.plugins.hasPlugin(LibraryPlugin::class.java)

    /**
     * 变种对应编译选项
     */
    private val variantCompileOptions = mutableMapOf<String, ProcedureContext.CompileOptions>()

    /**
     * 插件配置
     */
    private lateinit var ajxExtension: AJXExtension

    init {
        project.afterEvaluate {
            // 获取配置
            ajxExtension = project.extensions.findByType(AJXExtension::class.java) ?: AJXExtension()
            // 规则重整
            optimizeExtension(ajxExtension)
            LoggerHolder.logger.quiet("[$TAG] AJXExtension after optimize:$ajxExtension")
            // 获取android配置以及对应编译选项
            createVariantCompileOptions(AndroidConfig(project))
            // 设置运行变量
            System.setProperty("aspectj.multithreaded", "true")
        }
    }

    private fun optimizeExtension(extension: AJXExtension) {
        extension.apply {
            // 排除所有，等同于禁用
            if (this.excludes.contains("*") || this.excludes.contains("**")) {
                this.enabled = false
            }
            // 插件禁用，清除其它所有配置项
            if (this.enabled.not()) {
                this.includes.clear()
                this.excludes.clear()
                this.ajcArgs.clear()
            } else {
                // 包含所有，简化包含规则
                if (this.includes.contains("*") || this.includes.contains("**")) {
                    this.includes.clear()
                }
                // 重新排序
                includes.sort()
                excludes.sort()
                ajcArgs.sort()
            }
        }
    }

    private fun createVariantCompileOptions(androidConfig: AndroidConfig) {
        for (variant in androidConfig.getVariants()) {
            val compileOptions = ProcedureContext.CompileOptions().apply {
                // 兼容agp版本
                val javaCompile = AgpApiCompat.getJavaCompile(variant)
                encoding = javaCompile.options.encoding ?: "UTF-8"
                sourceCompatibility = javaCompile.sourceCompatibility
                targetCompatibility = javaCompile.targetCompatibility
                // 记录参与编译的文件，包括javac、kotlin-class以及jar
                val javac = javaCompile.destinationDirectory.asFile.get().absolutePath
                javaCompileClasspath = javaCompile.classpath.asPath + File.pathSeparator + javac
                bootClassPath =
                    androidConfig.getBootClasspath().joinToString(separator = File.pathSeparator)
            }
            variantCompileOptions[variant.name] = compileOptions
        }
    }

    override fun getName(): String {
        return TAG
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        // library只支持PROJECT_ONLY
        return if (isLibrary) TransformManager.PROJECT_ONLY else TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        //是否支持增量编译
        return true
    }

    override fun getParameterInputs(): MutableMap<String, Any> {
        // 插件拓展配置也当成输入，这样可以在修改配置时候使task触发全量编译
        return mutableMapOf(
            "AJXExtension" to AJXUtils.optToJsonString(ajxExtension)!!
        )
    }

    override fun transform(transformInvocation: TransformInvocation) {
        // 每个变种都会执行
        val startTime = System.currentTimeMillis()
        logQuiet(
            transformInvocation,
            "transform start.[isIncrement=${transformInvocation.isIncremental}]"
        )
        // 之前可能是构建失败，也关闭所有打开的文件
        ClasspathJar.closeAllOpenedArchives()
        process(transformInvocation)
        // 构建结束后关闭所有打开的文件
        ClasspathJar.closeAllOpenedArchives()
        logQuiet(
            transformInvocation,
            "transform finish.[${System.currentTimeMillis() - startTime}ms]"
        )
    }

    private fun process(transformInvocation: TransformInvocation) {
        val compileOptions = variantCompileOptions[transformInvocation.context.variantName]
        val procedureContext = ProcedureContext(transformInvocation, compileOptions!!, ajxExtension)
        ProcedureChain(procedureContext)
            .with(PrepareProcedure(procedureContext))
            .with(DoAspectProcedure(procedureContext))
            .doWorkContinuously(transformInvocation)
    }

    private fun logQuiet(transformInvocation: TransformInvocation, msg: String) {
        LoggerHolder.logger.quiet("[${transformInvocation.context.path}]: $msg")
    }
}
