import org.jetbrains.intellij.platform.gradle.TestFrameworkType

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.platform.frontend")
        bundledModule("intellij.platform.rpc")
    }

    implementation(project(":shared"))

    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
}
