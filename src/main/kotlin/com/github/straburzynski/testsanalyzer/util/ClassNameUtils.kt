package com.github.straburzynski.testsanalyzer.util

object ClassNameUtils {

    fun extractPackage(classname: String): String {
        val lastDot = classname.lastIndexOf('.')
        return if (lastDot > 0) classname.substring(0, lastDot) else ""
    }

    fun extractSimpleClassName(classname: String): String {
        val lastDot = classname.lastIndexOf('.')
        return if (lastDot >= 0) classname.substring(lastDot + 1) else classname
    }
}
