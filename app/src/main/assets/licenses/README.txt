LICENSE INFORMATION FOR ABHAYA SENSOR ANDROID
=============================================

This directory contains license files and attribution information for the
Abhaya Sensor Android application and its third-party components.

FILES IN THIS DIRECTORY:
- NOTICE.txt: Copyright notices and attribution information
- licenses/AGPL-3.0.txt: GNU Affero General Public License v3.0 (main application)
- licenses/LGPL-3.0.txt: GNU Lesser General Public License v3.0 (Meshrabiya API)
- licenses/Apache-2.0.txt: Apache License 2.0 (Android libraries, Kotlin)

MAIN APPLICATION LICENSE:
This application is licensed under AGPL-3.0, which means:
✅ You can use, modify, and distribute this software
✅ Commercial use is permitted  
✅ Perfect for research, education, and open source projects
⚠️ You must share source code if you distribute the application
⚠️ If you run this as a network service, users must have access to source code

THIRD-PARTY COMPONENTS:

1. MESHRABIYA API (LGPL-3.0)
   - Local project module providing AIDL interfaces
   - Allows this app to use any license (including AGPL)
   - API improvements must be shared if modified

2. ANDROID LIBRARIES (Apache-2.0)
   - CameraX: Advanced camera functionality with lifecycle awareness
   - Compose: Modern declarative UI framework  
   - AndroidX: Core Android components and lifecycle management
   - Kotlin: Programming language and coroutines

3. ACCOMPANIST (Apache-2.0)
   - Compose utility libraries for permissions and system UI

SENSOR DATA & PRIVACY:
This application processes camera, accelerometer, gyroscope, and other sensor
data for mesh networking and distributed computing. All processing is designed
to respect user privacy and comply with data protection regulations.

LICENSE COMPATIBILITY:
All included licenses are compatible with AGPL-3.0:
- LGPL-3.0: Compatible (dynamic linking allowed)
- Apache-2.0: Compatible (permissive with patent protection)

TESTING COVERAGE:
Current test coverage: ~60% (49/81 tests passing)
Test categories:
- 8 Camera integration tests (CameraX lifecycle, capture, preview)
- 9 Lifecycle tests (background/foreground state management)  
- 16 Compose UI tests (screen rendering, navigation, interactions)
- 5 AIDL service tests (Meshrabiya API integration)
- 2 Permission tests (camera, storage access)

HOW TO VIEW LICENSES:
Open the in-app license viewer by going to:
Settings → About → View Licenses

BUILDING FROM SOURCE:
1. Clone: git clone https://github.com/dreadstar/abhaya-sensor-android.git
2. Build: ./gradlew assembleDebug
3. Test: ./gradlew test
4. Coverage: ./gradlew testDebugUnitTestCoverage

COMPLIANCE REQUIREMENTS:
1. Include this NOTICE.txt file with any distribution
2. Include all license files in the licenses/ directory
3. Provide access to complete source code (AGPL-3.0 requirement)
4. Maintain all copyright notices in derivative works
5. Share modifications to LGPL components if distributed

DEVELOPMENT:
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 36 (Android 15)
- Language: Kotlin with Jetpack Compose
- Architecture: MVVM with Compose State Management

QUESTIONS & SUPPORT:
For licensing questions, technical support, or contributions:
- Documentation: https://github.com/dreadstar/abhaya-sensor-android/blob/master/THIRD_PARTY_LICENSES.md
- Issues: https://github.com/dreadstar/abhaya-sensor-android/issues
- Parent Project: https://github.com/dreadstar/orbot-abhaya-android
- Discussions: https://github.com/dreadstar/orbot-abhaya-android/discussions

LAST UPDATED: October 11, 2025
VERSION: Based on licensing implementation v2.0