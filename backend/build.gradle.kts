import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform.module")
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.platform.backend")
    }

    implementation(project(":shared"))

    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
}
