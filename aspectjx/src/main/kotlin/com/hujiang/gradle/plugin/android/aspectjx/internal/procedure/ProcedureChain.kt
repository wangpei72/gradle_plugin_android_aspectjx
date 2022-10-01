package com.hujiang.gradle.plugin.android.aspectjx.internal.procedure

import com.android.build.api.transform.TransformInvocation

/**
 * 处理链
 */
class ProcedureChain(procedureContext: ProcedureContext) : AbsProcedure(procedureContext) {

    private val procedures = mutableListOf<AbsProcedure>()

    fun <T : AbsProcedure> with(procedure: T): ProcedureChain {
        procedures.add(procedure)
        return this
    }

    override fun doWorkContinuously(transformInvocation: TransformInvocation): Boolean {
        for (procedure in procedures) {
            val name = procedure::class.simpleName
            logQuiet("$name start.")
            val startTime = System.currentTimeMillis()
            val continuously = procedure.doWorkContinuously(transformInvocation)
            val cost = System.currentTimeMillis() - startTime
            logQuiet("$name finish.[${cost}ms]")
            if (continuously.not()) {
                logQuiet("[$name] break.")
                break
            }
        }
        return true
    }
}
