Abhaya Sensor Android

This is a small Android app module skeleton intended as a starting point for the `abhaya-sensor-android` project. The included `SensorStreamConfig.tsx` is a UI guidance/prototype. The Android portion below is minimal and intended to be imported into Android Studio; you should adapt Gradle plugin and Kotlin versions to your main workspace.

Quick import

1. In Android Studio: File → New → Import project and point to this folder.
2. Or add this folder as a module to an existing Android Studio project.

Files added

- `src/main/AndroidManifest.xml` — app manifest
- `src/main/java/org/abhaya/sensor/MainActivity.kt` — simple activity with a WebView placeholder (so the TSX guidance can be loaded into a web UI or replaced with native UI later)
- `src/main/res/layout/activity_main.xml` — simple layout
- `SensorStreamConfig.tsx` — UI prototype (React/TSX) for reference

Next steps

- Hook up permissions (camera, microphone, sensors).
- Implement native `StreamIngestor` and storage integration.
- Replace web-based prototype with native Jetpack Compose or keep a WebView and host React UI.
