import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
}

val gitCommitCount: Int by lazy {
    try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootProject.projectDir)
            .start()
        process.waitFor()
        process.inputStream.bufferedReader().readLine()?.trim()?.toIntOrNull() ?: 1
    } catch (e: Exception) { 1 }
}

val resolvedAndroidVersionCode = providers.gradleProperty("androidVersionCode")
    .orNull
    ?.toIntOrNull()
    ?: gitCommitCount

val resolvedAndroidVersionName = providers.gradleProperty("androidVersionName")
    .orNull
    ?: "1.0"

val androidReleaseStoreFile = providers.gradleProperty("androidReleaseStoreFile").orNull
val androidReleaseStorePassword = providers.gradleProperty("androidReleaseStorePassword").orNull
val androidReleaseKeyAlias = providers.gradleProperty("androidReleaseKeyAlias").orNull
val androidReleaseKeyPassword = providers.gradleProperty("androidReleaseKeyPassword").orNull
val hasAndroidReleaseSigning = listOf(
    androidReleaseStoreFile,
    androidReleaseStorePassword,
    androidReleaseKeyAlias,
    androidReleaseKeyPassword,
).all { !it.isNullOrBlank() }

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.ktor.client.okhttp)
            implementation(compose.components.uiToolingPreview)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.spbu.projecttrack"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        if (hasAndroidReleaseSigning) {
            create("release") {
                storeFile = file(androidReleaseStoreFile!!)
                storePassword = androidReleaseStorePassword
                keyAlias = androidReleaseKeyAlias
                keyPassword = androidReleaseKeyPassword
            }
        }
    }

    defaultConfig {
        applicationId = "com.spbu.projecttrack"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = resolvedAndroidVersionCode
        versionName = resolvedAndroidVersionName
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasAndroidReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
