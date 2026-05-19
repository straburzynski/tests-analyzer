package com.github.straburzynski.testsanalyzer.action

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile

val KNOWN_TEST_SOURCE_SETS = setOf(
    "test",
    "it",
    "integration",
    "integrationTest",
    "integration-test",
)

fun isTestRelated(file: VirtualFile, rootManager: ProjectRootManager): Boolean {
    val fileIndex = rootManager.fileIndex
    if (fileIndex.isInTestSourceContent(file)) return true
    if (isAncestorOfTestSource(file, rootManager, fileIndex)) return true
    if (isInsideTrustedTestDir(file)) return true
    if (isModuleRoot(file)) return true
    return false
}

private fun isAncestorOfTestSource(
    file: VirtualFile,
    rootManager: ProjectRootManager,
    fileIndex: ProjectFileIndex,
): Boolean {
    if (!file.isDirectory) return false
    val filePath = file.path + "/"
    for (root in rootManager.contentSourceRoots) {
        if (fileIndex.isInTestSourceContent(root) && root.path.startsWith(filePath)) {
            return true
        }
    }
    return false
}

private fun isInsideTrustedTestDir(file: VirtualFile): Boolean {
    var current: VirtualFile? = if (file.isDirectory) file else file.parent
    while (current != null) {
        if (current.name in KNOWN_TEST_SOURCE_SETS) return true
        // Stop walking above "src" to avoid false positives
        if (current.name == "src") break
        current = current.parent
    }
    return false
}

private fun isModuleRoot(file: VirtualFile): Boolean {
    if (!file.isDirectory) return false
    return file.findChild("build.gradle.kts") != null ||
        file.findChild("build.gradle") != null ||
        file.findChild("pom.xml") != null
}
