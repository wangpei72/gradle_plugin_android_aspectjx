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
// This plugin is based on https://github.com/JakeWharton/hugo
package com.hujiang.gradle.plugin.android.aspectjx

import com.android.build.gradle.BaseExtension
import com.hujiang.gradle.plugin.android.aspectjx.internal.TimeTrace
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.util.GradleVersion

/**
 * aspectj plugin,
 * @author simon* @version 1.0.0* @since 2016-04-20
 */
class AJXPlugin implements Plugin<Project> {

    // any one of these plugins should be sufficient to proceed with applying this plugin
    private static final List<String> PREREQ_PLUGIN_OPTIONS = [
            'com.android.application',
            'com.android.feature',
            'com.android.dynamic-feature',
            'com.android.library',
            'android',
            'android-library',
    ]

    private boolean wasApplied = false

    @Override
    void apply(Project project) {
        project.extensions.create("aspectjx", AJXExtension)
        // At least one of the prerequisite plugins must by applied before this plugin can be applied, so
        // we will use the PluginManager.withPlugin() callback mechanism to delay applying this plugin until
        // after that has been achieved. If project evaluation completes before one of the prerequisite plugins
        // has been applied then we will assume that none of prerequisite plugins were specified and we will
        // throw an Exception to alert the user of this configuration issue.
        Action<? super AppliedPlugin> applyWithPrerequisitePlugin = { AppliedPlugin prerequisitePlugin ->
            if (wasApplied) {
                project.logger.info('The android-aspectjx plugin was already applied to the project: ' + project.path
                        + ' and will not be applied again after plugin: ' + prerequisitePlugin.id)
            } else {
                wasApplied = true

                doApply(project)
            }
        }

        PREREQ_PLUGIN_OPTIONS.each {
            project.pluginManager.withPlugin(it, applyWithPrerequisitePlugin)
        }
        project.afterEvaluate {
            if (!wasApplied) {
                throw new GradleException('The android-aspectjx plugin could not be applied during project evaluation.'
                        + ' One of the Android plugins must be applied to the project first.')
            }
        }
    }

    private static void doApply(Project project) {
        def gradleVersion = project.gradle.gradleVersion
        def dependencyGav = 'org.aspectj:aspectjrt:1.9.6'
        if (GradleVersion.current() > GradleVersion.version("4.0")) {
            project.logger.quiet("[ajx] gradle version[$gradleVersion] > 4.0")
            project.getDependencies().add("implementation", dependencyGav)
        } else {
            project.logger.quiet("[ajx] gradle version[$gradleVersion] < 4.0")
            project.getDependencies().add("compile", dependencyGav)
        }

        project.logger.quiet("[ajx] ${project.plugins.findAll()}")
        //build time trace
//        project.gradle.addListener(new TimeTrace())
        //register AspectTransform
        BaseExtension android = project.extensions.getByType(BaseExtension)
        android.registerTransform(new AJXTransform(project))
        project.logger.quiet("[ajx] register AJXTransform:$android.transforms")
    }
}
