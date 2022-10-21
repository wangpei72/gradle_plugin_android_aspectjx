package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation
import com.hujiang.gradle.plugin.android.aspectjx.LoggerHolder

/**
 * 抽象处理类
 */
abstract class AbsProcedure(val procedureContext: ProcedureContext) {

    private val logger = LoggerHolder.logger
    protected val loggerPrefix = "[${procedureContext.transformInvocation.context.path}]:"

    abstract fun doWorkContinuously(transformInvocation: TransformInvocation): Boolean

    fun logQuiet(msg: String) {
        logger.quiet("$loggerPrefix $msg")
    }

    fun logInfo(msg: String) {
        if (procedureContext.buildConfig.extension.debug) {
            logQuiet(msg)
        } else {
            logger.info("$loggerPrefix $msg")
        }
    }
}
