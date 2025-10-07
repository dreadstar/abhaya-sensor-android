import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    // Required for Kotlin 2.x when using Compose
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

val sensorBaseVersionCode = 1000000001

fun getVersionName(): String {
    // Gets the version name from the latest Git tag
    return providers.exec {
        commandLine("git", "describe", "--tags", "--always")
    }.standardOutput.asText.get().trim()
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ustadmobile.meshrabiya.sensor"
        minSdk = 24
        targetSdk = 36
        versionCode = sensorBaseVersionCode
        versionName = getVersionName()
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        flavorDimensions += "sensor"
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("x86", "armeabi-v7a", "x86_64", "arm64-v8a")
            isUniversalApk = true
        }
    }

    buildTypes {
        getByName("release") { 
            isMinifyEnabled = false 
            isShrinkResources = false
        }
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    productFlavors {
        create("fullperm") { 
            dimension = "sensor" 
        }
        create("nightly") {
            dimension = "sensor"
            // overwrites defaults from defaultConfig
            applicationId = "com.ustadmobile.meshrabiya.sensor.nightly"
            versionCode = (Date().time / 1000).toInt()
        }
    }

    composeOptions {
        // Compose compiler compatible with Kotlin 2.x (trial)
        kotlinCompilerExtensionVersion = "1.6.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    namespace = "com.ustadmobile.meshrabiya.sensor"
    kotlin { jvmToolchain(21) }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

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
    // Robolectric for Android framework APIs in JVM tests
    testImplementation("org.robolectric:robolectric:4.11")
    // Allow referencing the Orbot service implementation in unit tests (Robolectric)
    testImplementation(project(":orbotservice"))
    // Include org.json on the test classpath so JSONObject behaves in JVM unit tests
    testImplementation("org.json:json:20230227")
    testImplementation("androidx.test:core:1.5.0")
    
    // Android Instrumentation Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    // For AIDL service testing on actual Android devices/emulators
    androidTestImplementation(project(":orbotservice"))
    androidTestImplementation(project(":meshrabiya-api"))
    
    // Core library desugaring (required by Meshrabiya lib)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

// Increments versionCode by ABI type
android.applicationVariants.all {
    outputs.configureEach {
        if (versionCode == sensorBaseVersionCode) {
            val incrementMap =
                mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 4, "x86_64" to 5)
            val increment = incrementMap[filters.find { it.filterType == "ABI" }?.identifier] ?: 0
            (this as ApkVariantOutputImpl).versionCodeOverride = sensorBaseVersionCode + increment
        }
    }
}

// Packaging configuration
android {
    packaging {
        resources {
            excludes += listOf(
                "META-INF/androidx.localbroadcastmanager_localbroadcastmanager.version",
                "**/*.bak"  // Exclude .bak files from APK packaging
            )
        }
    }
}

// Allow unit tests to use Android framework resources and classes (required for ParcelFileDescriptor.createPipe)
android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}


