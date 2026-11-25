import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    }
}

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.20.1"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    }
}

rootProject.name = "heph-jetbrains"

include("shared", "frontend", "backend")
