import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jogamp.gluegen" || requested.group == "org.jogamp.jogl") {
            useVersion("2.6.0")
            because("JogAmp 2.5.0 is not on Maven Central; jogamp.org is unreachable")
        }
    }
}

dependencies {
    implementation(project(":app:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)

    // Explicit JogAmp from Maven Central (KCEF transitively asks for 2.5.0)
    implementation(libs.gluegen.rt)
    implementation(libs.jogl.all)
    runtimeOnly(libs.gluegen.rt)
    runtimeOnly(libs.jogl.all)
}

compose.desktop {
    application {
        mainClass = "com.malism.linsia.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.malism.linsia"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("icons/icon.png"))
            }
            windows {
                iconFile.set(project.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.file("icons/icon.png"))
            }
        }
    }
}