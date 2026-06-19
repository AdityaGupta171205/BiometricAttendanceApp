pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // libs.versions.toml in gradle/ is auto-discovered by Gradle 8.1+
    // No manual versionCatalogs {} block needed
}

rootProject.name = "Attendance"
include(":app")