plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.testng:testng:7.10.2")
}

tasks.test {
    useTestNG {
        parallel = "methods"
        threadCount = 4
    }
}
