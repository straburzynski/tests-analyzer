plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.20"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Integration test source set — shares compile/runtime dependencies with test
sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val integrationTestImplementation by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
val integrationTestCompileOnly by configurations.getting {
    extendsFrom(configurations.testCompileOnly.get())
}
val integrationTestRuntimeOnly by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(providers.gradleProperty("platformVersion").get())
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.java")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.JUnit5)
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Required: IntelliJ's testFramework transitively references JUnit 4's TestRule at runtime
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("com.google.code.gson:gson:2.11.0")
    integrationTestImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    patchPluginXml {
        sinceBuild.set("251")
        untilBuild.set(provider { null })
    }
    test {
        useJUnitPlatform()
        systemProperty("junit.jupiter.extensions.autodetection.enabled", "false")
    }

    val integrationTest by registering(Test::class) {
        group = "verification"
        description = "Runs integration tests against IntelliJ Platform."
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        useJUnitPlatform()
        systemProperty("junit.jupiter.extensions.autodetection.enabled", "false")
        shouldRunAfter(test)
        dependsOn("prepareTest", "instrumentTestCode")

        classpath = sourceSets["integrationTest"].runtimeClasspath + test.get().classpath
    }

    afterEvaluate {
        val testTask = test.get()
        val intTestTask = named<Test>("integrationTest").get()
        intTestTask.jvmArgumentProviders.addAll(testTask.jvmArgumentProviders)
    }
}
