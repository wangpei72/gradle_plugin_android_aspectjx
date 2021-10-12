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
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.AJXCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.cache.VariantCache
import com.hujiang.gradle.plugin.android.aspectjx.internal.procedure.*
import org.gradle.api.Project

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-03-12
 */
class AJXTransform extends Transform {

    AJXCache ajxCache
    Project project

    AJXTransform(Project proj) {
        project = proj
        // 初始化缓存
        ajxCache = new AJXCache(proj)
    }

    @Override
    String getName() {
        return "ajx"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        //是否支持增量编译
        return true
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        // 每个变种都会执行
        String transformTaskVariantName = transformInvocation.context.getVariantName()
        long cost = System.currentTimeMillis()
        project.logger.warn("ajx[$transformTaskVariantName] transform start...")
        VariantCache variantCache = new VariantCache(project, ajxCache, transformTaskVariantName)
        def ajxProcedure = new AJXProcedure(project)
        //check enable
        ajxProcedure.with(new CheckAspectJXEnableProcedure(project, variantCache, transformInvocation))
        def incremental = transformInvocation.incremental
        project.logger.warn("ajx[$transformTaskVariantName] incremental=${incremental}")
        if (incremental) {
            //incremental build
            ajxProcedure
                    .with(new UpdateAspectFilesProcedure(project, variantCache, transformInvocation))
                    .with(new UpdateInputFilesProcedure(project, variantCache, transformInvocation))
                    .with(new UpdateAspectOutputProcedure(project, variantCache, transformInvocation))
        } else {
            //delete output and cache before full build
            transformInvocation.outputProvider.deleteAll()
            //full build
            ajxProcedure
                    .with(new CacheAspectFilesProcedure(project, variantCache, transformInvocation))
                    .with(new CacheInputFilesProcedure(project, variantCache, transformInvocation))
                    .with(new DoAspectWorkProcedure(project, variantCache, transformInvocation))
        }

        ajxProcedure.with(new OnFinishedProcedure(project, variantCache, transformInvocation))

        ajxProcedure.doWorkContinuously()
        project.logger.warn("ajx[$transformTaskVariantName] transform finish.spend ${System.currentTimeMillis() - cost}ms")
    }
}
