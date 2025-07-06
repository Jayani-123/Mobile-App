pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
    // Add this block to pin versions
    plugins {
        id("com.android.application") version "8.3.0" // Latest stable (Feb 2024)
        id("org.jetbrains.kotlin.android") version "1.9.20" // Match Kotlin version
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AFL"
include(":app")
 