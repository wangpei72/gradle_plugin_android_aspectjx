package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.hujiang.gradle.plugin.android.aspectjx.AJXTransform
import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder
import com.hujiang.gradle.plugin.android.aspectjx.internal.concurrent.ITask
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.aspectj.weaver.Dump
import org.gradle.api.GradleException
import java.io.File

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-03-14
 */
class AJXTask : ITask {

    /**
     * Dump日志保存的目录
     */
    var dumpDirectory : File? = null

    /**
     * 编码
     */
    var encoding: String = ""

    /**
     * -source
     */
    var sourceCompatibility: String = ""

    /**
     * -target
     */
    var targetCompatibility: String = ""

    /**
     * -bootclasspath
     */
    var bootClassPath: String = ""

    /**
     * -classpath 的作用是在当解析一个类的时候，当这个类是不在inpath 中，会从classpath 中寻找
     */
    var classPath: String = ""

    /**
     * ajc参数，aspectjtools.jar 提供的参数
     */
    var ajcArgs = mutableListOf<String>()

    /**
     * 定义了切面规则的class
     */
    var aspectPath = mutableListOf<File>()

    /**
     * 需要处理的字节码文件，需要为jar或者目录，不能为文件
     */
    var inPath = mutableListOf<File>()

    /**
     * 输出到指定目录（不能为文件）
     */
    var outputDir: String? = null

    /**
     * 输出为jar包
     */
    var outputJar: String? = null

    override fun call(): Any? {
        // 使用文档：https://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html
        /*
        -classpath：class和source 的位置
        -aspectpath： 定义了切面规则的class
        -d：指定输出的目录
        -outjar：指定输出的jar上
        -inpath：需要处理的class文件，需要为jar或者目录
        -classpath 的作用是在当解析一个类的时候，当这个类是不在inpath 中，会从classpath 中寻找。
        * */
        val args = mutableListOf(
            "-showWeaveInfo",
            "-encoding", encoding,
            "-source", sourceCompatibility,
            "-target", targetCompatibility,
            "-classpath", classPath,
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

        // set aspectj build log output dir
        dumpDirectory?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
            // 内部已修改为ThreadLocal支持多线程
            Dump.setDumpDirectory(it.absolutePath)
        }
        // 执行
        val handler = MessageHandler(true)
        val m = Main()
        m.run(args.toTypedArray(), handler)
        for (message in handler.getMessages(null, true)) {
            val msg = "[${AJXTransform.TAG}][${message.kind}] " + message.message
            when (message.kind) {
                IMessage.ABORT, IMessage.ERROR, IMessage.FAIL -> {
                    throw GradleException(msg, message.thrown)
                }
                IMessage.WARNING -> {
                    logger().warn(msg, message.thrown)
                }
//                IMessage.INFO -> {
//                    logger().info(msg, message.thrown)
//                }
//                IMessage.DEBUG -> {
//                    logger().debug(msg, message.thrown)
//                }
                else -> {}
            }
        }

        return null
    }

    private fun logger() = LoggerHolder.logger
}
