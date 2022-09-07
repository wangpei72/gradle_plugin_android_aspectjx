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
package com.hujiang.gradle.plugin.android.aspectjx.internal

import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.BatchTaskScheduler
import java.io.File

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-03-14
 */
class AJXTaskManager {

    val aspectPath = mutableListOf<File>()
    val classPath = mutableListOf<File>()
    var ajcArgs = mutableListOf<String>()
    var encoding: String = ""
    var bootClassPath: String = ""
    var sourceCompatibility: String = ""
    var targetCompatibility: String = ""

    private val batchTaskScheduler = BatchTaskScheduler()

    fun addTask(task: AJXTask) {
        batchTaskScheduler.tasks.add(task)
    }

    fun batchExecute() {
        batchTaskScheduler.tasks.forEach { it ->
            val task = it as AJXTask
            task.encoding = encoding
            task.aspectPath = aspectPath
            task.classPath = classPath
            task.targetCompatibility = targetCompatibility
            task.sourceCompatibility = sourceCompatibility
            task.bootClassPath = bootClassPath
            task.ajcArgs = ajcArgs
        }

        batchTaskScheduler.execute()
        batchTaskScheduler.shutDown()
    }
}
