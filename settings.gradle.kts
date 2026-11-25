pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies/")
    }
}

rootProject.name = "heph-jetbrains"

include("shared", "frontend", "backend")
