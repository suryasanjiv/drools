rootProject.name = "drools"

// perf-tests is included as a subproject but is NOT wired into the root build lifecycle.
// Run explicitly: ./gradlew :perf-tests:gatlingRun
include(":perf-tests")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}
