import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.rpc) apply false
}

group = "io.github.hephbuild"
version = providers.gradleProperty("pluginVersion").get()

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
        intellijIdea("2026.3.0.1")
        pluginModule(implementation(project(":shared")))
        pluginModule(implementation(project(":frontend")))
        pluginModule(implementation(project(":backend")))
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
    testCompileOnly(libs.kotlinx.serialization.json)
}

intellijPlatform {
    projectName = "heph"

    pluginConfiguration {
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "263"
            untilBuild = provider { null }
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("publishChannel").map { listOf(it) }.orElse(listOf("default"))
    }

    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdea, "2026.3.0.1")
            create(IntelliJPlatformType.GoLand, "2026.3.0.1")
        }
    }
}

// Local split mode with the plugin installed in both backend and frontend.
val runIdeSplit by intellijPlatformTesting.runIde.registering {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH
}
