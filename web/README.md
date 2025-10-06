This folder contains a minimal Vite + React TypeScript scaffold to build the `SensorStreamConfig.tsx` prototype.

Quick start (macOS):

1. cd abhaya-sensor-android/web
2. npm install
3. npm run dev

Build output will be placed in `abhaya-sensor-android/build/web` by default.

Notes:
- The web entry imports `../../SensorStreamConfig.tsx` from the module root. That file is part of the Android module source tree; no editing is required unless you want to adapt the module path.
- If you prefer to keep the web UI in a separate package, move `SensorStreamConfig.tsx` into this `src/` folder and update the import in `main.tsx`.
