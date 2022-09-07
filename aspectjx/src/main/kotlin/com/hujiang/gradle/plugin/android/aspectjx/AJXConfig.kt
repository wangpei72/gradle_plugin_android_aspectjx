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


import com.android.build.gradle.AbstractAppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectSet
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

/**
 * obtain information about the project configuration
 * @author simon* @version 1.0.0* @since 2016-04-20
 */
class AJXConfig(project: Project) {

    private val android: BaseExtension = project.extensions.getByName("android") as BaseExtension

    /**
     * Return all variants.
     *
     * @return Collection of variants.
     */
    fun getVariants(): DomainObjectSet<BaseVariant> {
        return when (android) {
            is AbstractAppExtension -> {
                android.applicationVariants as DomainObjectSet<BaseVariant>
            }
            is TestExtension -> {
                android.applicationVariants as DomainObjectSet<BaseVariant>
            }
            is LibraryExtension -> {
                android.libraryVariants as DomainObjectSet<BaseVariant>
            }
            else -> throw GradleException("nonsupport extension:$android")
        }
    }

    /**
     * Return boot classpath.
     * @return Collection of classes.
     */
    fun getBootClasspath(): List<File> {
        return android.bootClasspath
    }
}