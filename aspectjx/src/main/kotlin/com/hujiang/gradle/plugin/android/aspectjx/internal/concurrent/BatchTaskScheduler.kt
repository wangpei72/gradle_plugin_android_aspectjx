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
package com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-04-04
 */
class BatchTaskScheduler(corePoolSize: Int = Runtime.getRuntime().availableProcessors() + 1) {

    private val executorService: ExecutorService
    private val tasks = mutableListOf<ITask>()

    init {
        executorService = Executors.newScheduledThreadPool(corePoolSize)
    }

    fun schedule(block: () -> Unit) {
        this.addTask(object : ITask {
            override fun call(): Any? {
                block()
                return null
            }
        })
    }

    fun <T : ITask> addTask(task: T) {
        tasks.add(task)
    }

    fun execute() {
        val all = executorService.invokeAll(tasks)
        all.forEach {
            val get = it.get()
            if (get is Throwable) {
                // 发生异常，中断构建
                throw get
            }
        }

        tasks.clear()
    }

    fun shutDown() {
        executorService.shutdown()
    }
}
