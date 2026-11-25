import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.aware.SplitModeAware
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.rpc) apply false
}

group = "io.github.hephbuild"
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(25)
}

val intellijPlatformVersion = providers.gradleProperty("intellijPlatformVersion").get()

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.intellij.platform.module")
    apply(plugin = "rpc")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(25)
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(intellijPlatformVersion)
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
            create(IntelliJPlatformType.IntellijIdea, intellijPlatformVersion)
            create(IntelliJPlatformType.GoLand, intellijPlatformVersion)
        }
    }
}

// Local split mode with the plugin installed in both backend and frontend.
val runIdeSplit by intellijPlatformTesting.runIde.registering {
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH
}

val runIdeSplitGoLand by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.GoLand
    version = intellijPlatformVersion
    splitMode = true
    pluginInstallationTarget = SplitModeAware.PluginInstallationTarget.BOTH
}
