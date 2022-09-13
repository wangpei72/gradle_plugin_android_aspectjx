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

import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.GradleException
import java.io.File

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-03-14
 */
class AJXTask : ITask {

    var encoding: String = ""
    var inPath = mutableListOf<File>()
    var aspectPath = mutableListOf<File>()
    var classPath = mutableListOf<File>()
    var ajcArgs = mutableListOf<String>()
    var bootClassPath: String = ""
    var sourceCompatibility: String = ""
    var targetCompatibility: String = ""
    var outputDir: String? = null
    var outputJar: String? = null

    override fun call(): Any? {
        /*
        -classpath：class和source 的位置
        -aspectpath： 定义了切面规则的class
        -d：指定输出的目录
        -outjar：指定输出的jar上
        -inpath：需要处理的.class
        classpath 的作用是在当解析一个类的时候，当这个类是不在inpath 中，会从classpath 中寻找。
        在使用AspectJ的时候, 我们用以下几个方面来优化我们的速度。
        * */
        val args = mutableListOf(
            "-showWeaveInfo",
            "-encoding", encoding,
            "-source", sourceCompatibility,
            "-target", targetCompatibility,
            "-classpath", classPath.joinToString(separator = File.pathSeparator),
            "-bootclasspath", bootClassPath
        )

        if (inPath.isNotEmpty()) {
            args.add("-inpath")
            args.add(inPath.joinToString(separator = File.pathSeparator))
        }
        if (aspectPath.isNotEmpty()) {
            args.add("-aspectpath")
            args.add(aspectPath.joinToString(separator = File.pathSeparator))
        }

        if (outputDir.isNullOrEmpty().not()) {
            args.add("-d")
            args.add(outputDir!!)
        }

        if (outputJar.isNullOrEmpty().not()) {
            args.add("-outjar")
            args.add(outputJar!!)
        }

        if (ajcArgs.isNotEmpty()) {
            if (!ajcArgs.contains("-Xlint")) {
                args.add("-Xlint:ignore")
            }
            if (!ajcArgs.contains("-warn")) {
                args.add("-warn:none")
            }

            args.addAll(ajcArgs)
        } else {
            args.add("-Xlint:ignore")
            args.add("-warn:none")
        }

        inPath.forEach {
            LoggerHolder.logger.debug("~~~~~~~~~~~~~input file: ${it.absolutePath}")
        }

        val handler = MessageHandler(true)
        val m = Main()
        m.run(args.toTypedArray(), handler)
        for (message in handler.getMessages(null, true)) {
            when (message.kind) {
                IMessage.ABORT, IMessage.ERROR, IMessage.FAIL -> {
                    throw GradleException(message.message, message.thrown)
                }
                IMessage.WARNING -> {
                    LoggerHolder.logger.warn(message.message, message.thrown)
                }
                IMessage.INFO -> {
                    LoggerHolder.logger.info(message.message, message.thrown)
                }
                IMessage.DEBUG -> {
                    LoggerHolder.logger.debug(message.message, message.thrown)
                }
                else -> {}
            }
        }

        return null
    }
}
