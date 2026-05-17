package com.github.straburzynski.testsanalyzer.executor

import java.io.File

enum class BuildSystem(val reportDir: String) {
    GRADLE("build/reports/tests-analyzer"),
    MAVEN("target/reports/tests-analyzer");

    companion object {
        fun detect(moduleDir: File): BuildSystem {
            if (moduleDir.resolve("build.gradle.kts").exists() ||
                moduleDir.resolve("build.gradle").exists()
            ) {
                return GRADLE
            }
            if (moduleDir.resolve("pom.xml").exists()) {
                return MAVEN
            }
            return GRADLE // fallback
        }

        fun isModuleRoot(dir: File): Boolean {
            return dir.resolve("build.gradle.kts").exists() ||
                    dir.resolve("build.gradle").exists() ||
                    dir.resolve("pom.xml").exists()
        }
    }
}
