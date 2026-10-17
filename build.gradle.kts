plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
}

// compose-webview → KCEF requires JogAmp 2.5.0, which is not on Maven Central
// and jogamp.org is often unreachable. Force 2.6.0 from Maven Central instead.
subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jogamp.gluegen" || requested.group == "org.jogamp.jogl") {
                useVersion("2.6.0")
                because("JogAmp 2.5.0 is missing from Maven Central; use 2.6.0")
            }
        }
    }
}
