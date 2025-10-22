# STREAM_LIFECYCLE.md

## Purpose
This document defines the complete lifecycle for distributed sensor data streaming and its integration with the storage and replication system in the abhaya-sensor-android project. It is intended for developers and AI agents to guide implementation, testing, and validation of all related features. All test cases and agent protocols must reference this document for correctness.

---

## 1. Sensor Stream Initiation

- **User initiates a sensor data stream** via the SensorApp UI.
- The UI triggers the control layer (`SensorStreamManager`) to begin the streaming lifecycle.

---

## 2. Storage Node Discovery (Broadcast Request)

- On stream start, the control layer **broadcasts a Storage Node Request** to the mesh network using the gossip protocol ([MeshGossipService.kt](../../Meshrabiya/lib-meshrabiya/src/main/java/com/ustadmobile/meshrabiya/service/MeshGossipService.kt)).
- The request includes:
  - Stream identifier (unique for each sensor stream)
  - Any relevant metadata for candidate evaluation

---

## 3. Candidate Node Reception & Response

- **All nodes** on the mesh receive the broadcast.
- Each candidate node:
  - Checks if it is a **storage node** ([EmergentRoleManager.kt](../../Meshrabiya/lib-meshrabiya/src/main/java/com/ustadmobile/meshrabiya/vnet/EmergentRoleManager.kt)).
  - Evaluates **system state** (thermal, battery, etc.) and **available bandwidth/storage**.
  - If eligible, **responds** with:
    - Available bandwidth and space
    - System state
    - Node ID
    - Mesh URL for streaming endpoint
    - Latency estimate
    - Fitness score

---

## 4. Client Node Response Handling & Candidate Selection

- The client node **waits for responses** within a timeout window.
- If **no responses** are received:
  - UI alerts the user.
  - The client retries after a delay.
- If **responses are received**:
  - The client selects the **best candidate** based on:
    - Sufficient bandwidth and space
    - Healthy system state
    - Lowest latency
    - Highest fitness score
  - If **none are suitable**, UI alerts the user and retries after a delay.

---

## 5. Stream Initiation

- The client node **initiates streaming** to the selected storage node using the mesh URL provided.
- Streaming is managed by the control layer (`SensorStreamManager`), which configures the stream endpoint and starts the data flow.
- The stream is chunked and sent reliably, with ongoing monitoring for failures or stop signals.

---

## 6. Stream Monitoring & Failure Handling

- The control layer monitors the stream for:
  - Stop signals (user or network-initiated)
  - Failures (connection loss, endpoint unavailability)
- On failure or stop:
  - The control layer re-initiates the discovery and selection process.
  - UI is updated to reflect the current status.

---

## 7. Storage & Replication Integration

- **If the stream is archived or stored as files:**
  - The storage node saves the incoming stream data in its shared storage area.
  - The storage node generates a file identifier for each stored segment.
  - The storage node initiates replication of stored segments to other nodes, following the [STORAGE_LIFECYCLE.md](../../STORAGE_LIFECYCLE.md) protocol.
  - Replication continues until the desired replica count is reached, as configured in [AppSettings.kt](../../app/src/main/java/org/torproject/android/AppSettings.kt).

---

## 8. UI Updates

- The UI layer receives status updates from the control layer:
  - Streaming started (with endpoint info)
  - No storage nodes available (retrying)
  - Streaming stopped or failed (with reason)
  - Storage and replication status (if applicable)

---

## 9. Error Handling & Retries

- All steps include robust error handling:
  - Timeouts and retries for broadcasts and stream initiation.
  - UI alerts for user awareness.
  - Logging for diagnostics.
- All failures are surfaced to the user and retried according to protocol.

---

## 10. Security & Privacy

- Stream endpoints are anonymized and do not reveal user identity.
- Transfers and replication use encrypted mesh channels.
- Only eligible nodes participate in streaming, storage, and replication.

---

## 11. Test Case Guidance

- Test cases must cover:
  - Stream initiation and endpoint selection
  - Storage node discovery and candidate selection logic
  - Stream start, stop, and failure scenarios
  - Completion notification and UI update
  - Storage and replication initiation, progress, and completion
  - Replica count query and enforcement
  - Error handling, retries, and user alerts
  - Security and privacy compliance

---

## 12. References

- [SensorApp.kt](app/src/main/java/com/ustadmobile/meshrabiya/sensor/ui/SensorApp.kt)
- [SensorStreamManager.kt](app/src/main/java/com/ustadmobile/meshrabiya/sensor/stream/SensorStreamManager.kt)
- [MeshGossipService.kt](../../Meshrabiya/lib-meshrabiya/src/main/java/com/ustadmobile/meshrabiya/service/MeshGossipService.kt)
- [EmergentRoleManager.kt](../../Meshrabiya/lib-meshrabiya/src/main/java/com/ustadmobile/meshrabiya/vnet/EmergentRoleManager.kt)
- [AppSettings.kt](../../app/src/main/java/org/torproject/android/AppSettings.kt)
- [STORAGE_LIFECYCLE.md](../../STORAGE_LIFECYCLE.md)

---

**This document is the canonical reference for distributed sensor streaming and its integration with storage and replication in the abhaya-sensor-android project. All development, agent operations, and test cases must comply with these protocols.**