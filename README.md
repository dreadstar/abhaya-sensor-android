# abhaya-sensor-android

**Sensor Data Capture and Streaming for Mesh Networks**

A privacy-focused Android application for capturing and streaming sensor data (camera, audio, device sensors) over mesh networks using Meshrabiya integration.

## Features

- 📹 **Camera Capture**: Front/back camera with flash control (OFF/ON/AUTO)
- 🎤 **Audio Recording**: Microphone capture with quality settings
- 📊 **Device Sensors**: Accelerometer, Gyroscope, Magnetometer, Light, Proximity
- 📸 **Periodic Photos**: Automated photo capture at configurable intervals
- 🎥 **Video Recording**: Continuous video streaming
- 🌐 **Mesh Integration**: Stream data over Meshrabiya mesh networks
- 🔐 **Privacy-First**: AGPLv3 licensed, fully open source

## Architecture

- **UI**: Jetpack Compose Material Design
- **Camera**: AndroidX CameraX with lifecycle awareness
- **Networking**: Meshrabiya mesh networking (LGPL-3.0)
- **State Management**: Kotlin Coroutines + ViewModel
- **Build**: Gradle with Kotlin DSL

## Project Structure

```
abhaya-sensor-android/
├── app/                          # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/             # Kotlin source code
│   │   │   │   └── com/ustadmobile/meshrabiya/sensor/
│   │   │   │       ├── SensorApp.kt          # Main app UI
│   │   │   │       ├── capture/
│   │   │   │       │   └── CameraCapture.kt  # Camera implementation
│   │   │   │       └── ingest/
│   │   │   │           └── StreamIngestor.kt # Data ingestion
│   │   │   ├── res/              # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/          # Instrumented tests
│   └── build.gradle.kts
└── meshrabiya-api/               # AIDL interface definitions
```

## Dependencies

### Direct Dependencies

- **AndroidX Core & AppCompat**: UI components
- **Jetpack Compose**: Modern UI toolkit
- **CameraX**: Camera APIs with lifecycle management
- **Kotlin Coroutines**: Asynchronous programming
- **meshrabiya-api**: AIDL interfaces (LGPL-3.0)

See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for complete dependency list.

## Building

### Prerequisites

- Android Studio Arctic Fox or later
- Java 21 (JDK)
- Android SDK (API 24+)
- Gradle 9.0+

### Build Commands

```bash
# Clean build
./gradlew clean assembleFullpermDebug

# Build with tests
./gradlew clean assembleFullpermDebug assembleFullpermDebugAndroidTest

# Run instrumented tests
./gradlew connectedFullpermDebugAndroidTest

# Install on device
adb install app/build/outputs/apk/fullperm/debug/app-fullperm-armeabi-v7a-debug.apk
```

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.ustadmobile.meshrabiya.sensor.capture.CameraIntegrationTest
```

### Code Style

- Kotlin coding conventions
- 4-space indentation
- Max line length: 120 characters
- Use explicit types for public APIs

## Features in Detail

### Camera Capture

- **3-State Flash**: OFF (grey), ON (yellow), AUTO (blue)
- **Front/Back Toggle**: Switch between cameras
- **Preview**: Real-time camera preview
- **Main Thread Requirement**: CameraX requires initialization on main thread

### Sensor Monitoring

Supported sensors:
- Accelerometer
- Magnetometer (Compass)
- Light Sensor
- Proximity Sensor
- Gyroscope
- Step Detector
- Step Counter

Sensors show ON/OFF status based on hardware availability.

### Data Streaming

- Integration with Meshrabiya mesh networks
- AIDL-based service communication
- Configurable streaming intervals
- Background service support

## Testing

Current test coverage: **~60%** (49/81 tests passing)

### Test Categories

1. **Camera Tests** (8 tests): Camera initialization, preview, capture
2. **Lifecycle Tests** (9 tests): Background transitions, state management
3. **Compose UI Tests** (16 tests): UI components, user interactions
4. **AIDL Service Tests** (5 tests): Service binding, IPC
5. **Permission Tests** (2 tests): Runtime permission handling

See [ANDROIDTEST_ERRORS-10112025.md](../ANDROIDTEST_ERRORS-10112025.md) for detailed test analysis.

## License

**abhaya-sensor-android** is licensed under [AGPLv3](LICENSE).

This sensor application is designed to work with orbot-abhaya-android
for mesh-networked sensor data streaming.

### What This Means

- ✅ Free to use, modify, and distribute
- ✅ Must share source code when distributing
- ✅ **Network use = distribution** (AGPLv3 network copyleft)
- ✅ Modifications must be AGPLv3
- ✅ Strong copyleft protects open source nature

### Third-Party Components

- **meshrabiya-api** (LGPL-3.0): AIDL interfaces
  - Source: Project module
- **Android Libraries** (Apache-2.0): CameraX, Compose, AndroidX
  - Source: https://source.android.com/
- **Kotlin** (Apache-2.0): Programming language and coroutines
  - Source: https://github.com/JetBrains/kotlin

See [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md) for complete license information.

### Contributing

Contributions are welcome! By contributing, you agree that your contributions 
will be licensed under AGPLv3.

Guidelines:
1. Follow Kotlin coding conventions
2. Add tests for new features
3. Update documentation
4. Ensure all tests pass before submitting PR

## Roadmap

- [ ] Implement StreamIngestor storage backend
- [ ] Add audio recording functionality
- [ ] Complete sensor data streaming
- [ ] Improve test coverage to 80%+
- [ ] Add end-to-end mesh streaming tests
- [ ] Implement background service for continuous streaming
- [ ] Add data encryption for mesh transmission
- [ ] Create user documentation

## Related Projects

- **orbot-abhaya-android**: Main VPN and proxy application
  - Repository: https://github.com/dreadstar/orbot-abhaya-android
- **Meshrabiya**: Mesh networking library (LGPL-3.0)
  - Upstream: https://github.com/UstadMobile/Meshrabiya
  - Our Fork: https://github.com/dreadstar/Meshrabiya

## Support

- **Issues**: https://github.com/dreadstar/orbot-abhaya-android/issues
- **Discussions**: https://github.com/dreadstar/orbot-abhaya-android/discussions

## Acknowledgments

- Guardian Project for Orbot
- UstadMobile for Meshrabiya
- The Tor Project for privacy technology
- Android Open Source Project for excellent libraries

---

**Copyright © 2025 Tyrone Thomas/BreakThrough Technologies**

Licensed under AGPLv3. See [LICENSE](LICENSE) for details.

