rootProject.name = "tests-analyzer"

// Example test modules used for manual testing via the plugin.
// Included so IntelliJ recognizes them when opening the project, but their test tasks
// are not wired to the root build — run them via the plugin or directly in each module.
include("example-projects:example-junit")
include("example-projects:example-kotest")
include("example-projects:example-groovy")
include("example-projects:example-ngtest")

// Exclude example-projects from lifecycle tasks (clean, check, test, build).
// They exist only for manual plugin testing, not for CI/development builds.
gradle.beforeProject {
    if (path.startsWith(":example-projects")) {
        tasks.whenTaskAdded {
            if (name in setOf("check", "test", "build", "clean")) {
                enabled = false
            }
        }
    }
}
