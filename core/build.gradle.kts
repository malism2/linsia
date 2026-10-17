import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    android {
        namespace = "com.malism.linsia.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            // Compose / Material 3
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Coroutines / Serialization / DateTime / IO
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.datetime)
            api(libs.okio)

            // DI
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)

            // Network
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.client.logging)
            api(libs.ktor.client.auth)
            api(libs.ktor.serialization.kotlinx.json)

            // Database
            api(libs.androidx.room3.runtime)
            api(libs.androidx.sqlite.bundled)

            // Logging / KV
            api(libs.kermit)
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.coroutines)
            api(libs.multiplatform.settings.serialization)

            // Navigation
            api(libs.decompose)
            api(libs.decompose.compose)

            // Image / Chart / Media / Files / WebView
            api(libs.coil.compose)
            api(libs.coil.network.ktor3)
            api(libs.koalaplot.core)
            api(libs.filekit.core)
            api(libs.filekit.dialogs)
            api(libs.filekit.dialogs.compose)
            api(libs.filekit.coil)
            api(libs.compose.media.player)
            api(libs.kmp.audio.recorder.player)
            api(libs.image.picker.kmp)
            api(libs.compose.webview)

            // Backend
            api(project.dependencies.platform(libs.supabase.bom))
            api(libs.supabase.auth)
            api(libs.supabase.postgrest)
            api(libs.supabase.storage)
            api(libs.supabase.realtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.crashlytics)

            // MOKO Permissions: Android + iOS only (no Desktop)
            api(libs.moko.permissions)
            api(libs.moko.permissions.compose)
            api(libs.moko.permissions.camera)
            api(libs.moko.permissions.microphone)
            api(libs.moko.permissions.gallery)
            api(libs.moko.permissions.storage)
            api(libs.moko.permissions.notifications)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

            api(libs.moko.permissions)
            api(libs.moko.permissions.compose)
            api(libs.moko.permissions.camera)
            api(libs.moko.permissions.microphone)
            api(libs.moko.permissions.gallery)
            api(libs.moko.permissions.storage)
            api(libs.moko.permissions.notifications)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
    add("kspIosArm64", libs.androidx.room3.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
