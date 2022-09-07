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
package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.AJXCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.procedure.*
import org.aspectj.org.eclipse.jdt.internal.compiler.batch.ClasspathJar
import org.gradle.api.Project

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
class AJXTransform(val project: Project) : Transform() {

    private val ajxCache: AJXCache = AJXCache(project)

    override fun getName(): String {
        return "ajx"
    }

    override fun getInputTypes(): Set<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        //是否支持增量编译
        return true
    }

    override fun transform(transformInvocation: TransformInvocation) {
        // 每个变种都会执行
        val transformTaskVariantName = transformInvocation.context.variantName
        val cost = System.currentTimeMillis()
        project.logger.warn("ajx[$transformTaskVariantName] transform start...")
        // 之前可能是构建失败，也关闭所有打开的文件
        ClasspathJar.closeAllOpenedArchives()
        val variantCache = VariantCache(project, ajxCache, transformTaskVariantName)
        val ajxProcedure = AJXProcedure(project, variantCache, transformInvocation)
        //check enable
        ajxProcedure.with(CheckAspectJXEnableProcedure(project, variantCache, transformInvocation))
        val incremental = transformInvocation.isIncremental
        project.logger.warn("ajx[$transformTaskVariantName] incremental=${incremental}")
        if (incremental) {
            //incremental build
            ajxProcedure
                .with(UpdateAspectFilesProcedure(project, variantCache, transformInvocation))
                .with(UpdateInputFilesProcedure(project, variantCache, transformInvocation))
                .with(UpdateAspectOutputProcedure(project, variantCache, transformInvocation))
        } else {
            //delete output and cache before full build
            transformInvocation.outputProvider.deleteAll()
            //full build
            ajxProcedure
                .with(CacheAspectFilesProcedure(project, variantCache, transformInvocation))
                .with(CacheInputFilesProcedure(project, variantCache, transformInvocation))
                .with(DoAspectWorkProcedure(project, variantCache, transformInvocation))
        }

        ajxProcedure.with(OnFinishedProcedure(project, variantCache, transformInvocation))

        ajxProcedure.doWorkContinuously()
        // 构建结束后关闭所有打开的文件
        ClasspathJar.closeAllOpenedArchives()
        project.logger.warn("ajx[$transformTaskVariantName] transform finish.spend ${System.currentTimeMillis() - cost}ms")
    }
}
