package com.hujiang.gradle.plugin.android.aspectjx

/**
 * 配置文件
 */
open class AJXExtension {

    /**
     * 包含规则
     */
    val includes = mutableListOf<String>()

    /**
     * 排除规则
     */
    val excludes = mutableListOf<String>()

    /**
     * aspectjtools.jar支持的参数，请误乱使用，否则可能会产生未知问题！！！
     */
    val ajcArgs = mutableListOf<String>()

    /**
     * 是否启用，默认启用
     */
    var enabled = true

    /**
     * 是否开启debug模式，输出调试相关信息，以便排查问题，默认关闭
     * @since 3.3.0
     */
    var debug = false

    fun include(vararg filters: String): AJXExtension {
        this.includes.addAll(filters)
        return this
    }

    fun exclude(vararg filters: String): AJXExtension {
        this.excludes.addAll(filters)
        return this
    }

    fun ajcArgs(vararg ajcArgs: String): AJXExtension {
        this.ajcArgs.addAll(ajcArgs)
        return this
    }

    override fun toString(): String {
        return "AJXExtension(includes=$includes, excludes=$excludes, ajcArgs=$ajcArgs, enabled=$enabled, debug=$debug)"
    }

}
