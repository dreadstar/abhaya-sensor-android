plugins {
    id("com.android.application")
    kotlin("android")
    // Required for Kotlin 2.x when using Compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ustadmobile.meshrabiya.sensor"
    minSdk = 24
    targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        getByName("release") { isMinifyEnabled = false }
    }

    composeOptions {
        // Compose compiler compatible with Kotlin 2.x (trial)
        kotlinCompilerExtensionVersion = "1.6.0"
    }

    buildFeatures {
        compose = true
    }

    namespace = "com.ustadmobile.meshrabiya.sensor"
    kotlin { jvmToolchain(21) }

    // Include the meshrabiya-api module's AIDL sources so the binder stubs are generated
    // locally for this app (Option A: consumer-side AIDL sourceSets). This avoids needing
    // variant-aware AAR repackaging and keeps compilation deterministic in the monorepo.
    sourceSets {
        getByName("main") {
            aidl.srcDir(project(":meshrabiya-api").file("src/main/aidl"))
        }
    }

}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")

    // Compose
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material:material:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // CameraX (placeholder)
    implementation("androidx.camera:camera-core:1.2.2")
    implementation("androidx.camera:camera-camera2:1.2.2")
    implementation("androidx.camera:camera-lifecycle:1.2.2")
    implementation("androidx.camera:camera-view:1.2.2")

    // Meshrabiya AIDL API (local project)
    implementation(project(":meshrabiya-api"))

    // Testing
    testImplementation("junit:junit:4.13.2")
}
