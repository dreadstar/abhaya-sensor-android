pluginManagement {
	repositories {
		gradlePluginPortal()
		google()
		mavenCentral()
	}
}

dependencyResolutionManagement {
	repositories {
		google()
		mavenCentral()
		mavenLocal()
	}
}

rootProject.name = "abhaya-sensor-android"
include(":app")

// WARNING: This file is intentionally disabled to prevent subproject isolation.
// Use only the root settings.gradle.kts for builds.
// All module resolution must occur from the root build for correct dependency wiring.
