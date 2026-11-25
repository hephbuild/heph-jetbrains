import org.jetbrains.intellij.platform.gradle.TestFrameworkType

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.platform.rpc")
    }

    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
    testImplementation(libs.kotlinx.serialization.core)
    testImplementation(libs.kotlinx.serialization.json)
}
