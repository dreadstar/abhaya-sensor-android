# Abhaya Sensor Android — Knowledge & Architecture

This document adapts the existing mesh-network media streaming architecture to stream sensor/time-series and media (camera/microphone) data into the project's Distributed Storage service so tasks running on the mesh can process them efficiently.

## Goals
- Capture sensor and media streams (camera, microphone, motion, GPS, ambient sensors).
- Stream data reliably to the distributed storage service so tasks can consume it.
- Support front/back camera selection with preview and advanced controls (focus, exposure, flash, FPS, aspect ratio, periodic capture).
- Support audio voice-activation (record when amplitude > threshold).
- Provide an API for tasks to consume time-series streams and media segments.
- Preserve privacy and power-savings via encryption, selective streaming, and adaptive quality.

---

## High-level architecture

- App: `abhaya-sensor-android` — responsible for capturing sensors and media and streaming into Distributed Storage.
- Transport: WebRTC Media/Data channels where suitable; fallback to Meshrabiya's existing data transports for unreliable networks.
- Format: Protocol Buffers for sensor telemetry; raw media segments for audio/video (plus metadata).
- Storage: Use existing DistributedStorageAgent API for storing segments and time-series entries. Each sensor stream maps to an append-only object in storage (time-series shard).
- Tasks: Mesh tasks can subscribe to stream metadata and read new segments or windows using existing storage read APIs.

---

## Stream model and storage mapping

- SensorStream: An append-only time-series represented as protocol-buffer messages; each message contains timestamp, sensor id, and value(s).
- MediaSegments: Video/audio stored as media chunks (webm/opus/vp8) with accompanying metadata in protobuf (startTimeMs, endTimeMs, encoding, fps, resolution, deviceId).
- Indexing: Each stream maintains a lightweight index object (metadata file) in Distributed Storage listing segment offsets, checksums, and timestamps for efficient seeking.
- Visibility: Streams have ACL metadata controlling which tasks/nodes can read them.

---

## API surface (Kotlin)

- StreamIngestor: Kotlin interface added to the main app which accepts sensor data and media segments and writes them into DistributedStorageAgent in a streaming-friendly way.
- Subscription API: Tasks can call `DistributedStorageAgent.listStreamSegments(streamId)` and `getSegment(streamId, segmentIndex)` to process newly-available data.

---

## Protocols & formats

- Sensor telemetry: Protobuf envelope, e.g. `SensorReading { string streamId; int64 timestampMs; bytes payload }`.
- Media segments: Containerized webm or fragmented mp4 for video; opus for audio. Each segment accompanied by a small JSON/Protobuf descriptor.

---

## Camera & Audio controls (UI)

- Camera config options:
  - Facing: front/back
  - Mode: live stream / periodic capture / photo
  - Resolution, frame rate, aspect ratio, codec
  - Focus: auto/manual
  - Flash: on/off/auto
  - Quality/compression controls

- Audio config options:
  - Sample rate, channels, codec, bitrate
  - Voice activation threshold (dB)
  - Noise suppression / AGC


---

## Security & privacy

- Encrypt media segments and sensor streams at rest using per-stream keys.
- Use ephemeral session keys for transport; store encrypted data in Distributed Storage.
- Provide user controls to pause/stop streams and to opt-out of sensitive sensors.

---

## Fault tolerance & performance

- Write segments with replication metadata; use chunked uploads for large segments.
- Use adaptive bitrate and simulcast where bandwidth allows.
- Use local buffering and backpressure to disk when network is unavailable.

---

## Integration notes for main project

- Add `StreamIngestor` interface in `app` module; implement `StreamIngestorImpl` using `DistributedStorageAgent`.
- Add manifest permissions and runtime permission flows for camera/microphone/sensors.
- Create a `SensorStream` manager to configure, start, and stop streams, and to publish metadata to storage.
- Extend `DistributedStorageAgent` indexing/model if necessary to support streaming indices.

---

## Next steps / Implementation plan
1. Add `abhaya-sensor-android` module skeleton to the workspace.
2. Add `KNOWLEDGE.md` (this file) to the module.
3. Implement `StreamIngestor` interface in `app` and a simple `StreamIngestorImpl` that writes sensor protobuf messages to `DistributedStorageAgent`.
4. Build a TSX prototype `SensorStreamConfig.tsx` UI for selecting sensors and camera options.
5. Implement camera preview and periodic capture flows.
6. Implement voice-activation and threshold recording for audio.
7. Add task-side helpers to consume stored streams efficiently.

---

## References
- WebRTC media & data channels
- Protocol Buffers for time-series
- Android CameraX for camera controls
- MediaCodec / MediaRecorder for efficient encoding
- Orbot's DistributedStorageAgent for chunked/replicated storage


*End of document.*
