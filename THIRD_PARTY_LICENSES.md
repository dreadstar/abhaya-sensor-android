# Third-Party Licenses - abhaya-sensor-android

This document contains the complete license texts for all third-party software used in abhaya-sensor-android.

## Table of Contents

- [Summary](#summary)
- [Direct Dependencies](#direct-dependencies)
  - [meshrabiya-api (LGPL-3.0)](#meshrabiya-api-lgpl-30)
  - [Android Libraries (Apache-2.0)](#android-libraries-apache-20)
  - [Kotlin (Apache-2.0)](#kotlin-apache-20)
- [Complete License Texts](#complete-license-texts)

---

## Summary

**abhaya-sensor-android** is a sensor data capture and streaming application licensed under **AGPLv3**.

This module integrates with orbot-abhaya-android for mesh-networked sensor data streaming.

### Quick Reference

| Component | License | Source |
|-----------|---------|--------|
| abhaya-sensor-android | AGPL-3.0 | This module |
| meshrabiya-api | LGPL-3.0 | Local project module |
| Android CameraX | Apache-2.0 | androidx.camera |
| Jetpack Compose | Apache-2.0 | androidx.compose |
| AndroidX Core & Lifecycle | Apache-2.0 | androidx.core, androidx.lifecycle |
| Kotlin | Apache-2.0 | org.jetbrains.kotlin |
| Google Accompanist | Apache-2.0 | com.google.accompanist |

---

## Direct Dependencies

### meshrabiya-api (LGPL-3.0)

**Local Project Module**

- **License**: LGPL-3.0
- **Purpose**: AIDL interface definitions for Meshrabiya integration
- **Location**: Project module `:meshrabiya-api`

This module provides the interfaces for communicating with Meshrabiya mesh networking components.

---

### Android Libraries (Apache-2.0)

#### AndroidX Core & AppCompat
- `androidx.core:core-ktx`
- `androidx.appcompat:appcompat`
- `com.google.android.material:material`

#### Jetpack Compose
- `androidx.compose.ui:ui`
- `androidx.compose.material:material`
- `androidx.compose.material:material-icons-extended`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.activity:activity-compose`

#### AndroidX Lifecycle
- `androidx.lifecycle:lifecycle-runtime-ktx`
- `androidx.lifecycle:lifecycle-viewmodel-compose`

#### CameraX
- `androidx.camera:camera-core`
- `androidx.camera:camera-camera2`
- `androidx.camera:camera-lifecycle`
- `androidx.camera:camera-view`

#### Google Accompanist
- `com.google.accompanist:accompanist-flowlayout`

**Copyright**: © The Android Open Source Project  
**License**: Apache License 2.0  
**Source**: https://source.android.com/

---

### Kotlin (Apache-2.0)

#### Kotlin Standard Library & Coroutines
- Kotlin Standard Library
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`

**Copyright**: © JetBrains s.r.o. and Kotlin Programming Language contributors  
**License**: Apache License 2.0  
**Source**: https://github.com/JetBrains/kotlin

---

## Complete License Texts

### GNU Affero General Public License v3.0

**This is the license for abhaya-sensor-android.**

See: [../LICENSE](../LICENSE) or https://www.gnu.org/licenses/agpl-3.0.txt

---

### GNU Lesser General Public License v3.0

**This license applies to meshrabiya-api module.**

See: [../licenses/LGPL-3.0.txt](../licenses/LGPL-3.0.txt) or https://www.gnu.org/licenses/lgpl-3.0.txt

**Key Points**:
- meshrabiya-api provides AIDL interfaces
- Used as separate project module
- Licensed under LGPL-3.0 for compatibility with Meshrabiya

---

### Apache License 2.0

**This license applies to Android and Kotlin libraries.**

See: [../licenses/Apache-2.0.txt](../licenses/Apache-2.0.txt) or https://www.apache.org/licenses/LICENSE-2.0.txt

**Attribution**:

```
Copyright (C) The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
```

```
Copyright (C) JetBrains s.r.o. and Kotlin Programming Language contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
```

---

## License Compatibility

All dependencies are compatible with AGPLv3:

| Dependency License | Compatible with AGPL-3.0? |
|-------------------|---------------------------|
| LGPL-3.0 | ✅ Yes |
| Apache-2.0 | ✅ Yes |

---

## Questions

For questions about this module's license:
- **Repository**: https://github.com/dreadstar/abhaya-sensor-android
- **Main Project**: https://github.com/dreadstar/orbot-abhaya-android
- **Issues**: https://github.com/dreadstar/orbot-abhaya-android/issues

---

## Document Information

- **Last Updated**: October 11, 2025
- **Module**: abhaya-sensor-android
- **Maintained By**: Tyrone Thomas/BreakThrough Technologies
