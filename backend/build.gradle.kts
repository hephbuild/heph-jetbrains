import org.jetbrains.intellij.platform.gradle.TestFrameworkType

dependencies {
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        bundledModule("intellij.platform.backend")
        bundledModule("intellij.platform.rpc")
        bundledModule("intellij.platform.rpc.backend")
        bundledModule("intellij.platform.kernel.backend")
        bundledModule("intellij.platform.lsp")
        bundledModule("intellij.platform.lsp.impl")
    }

    implementation(project(":shared"))

    compileOnly(libs.kotlinx.serialization.core)
    compileOnly(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)
}
