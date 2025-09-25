StreamIngestor interface (Kotlin) — design notes

This document sketches the `StreamIngestor` interface and a simple implementation outline to be added into the main `app` module of Orbot.

Responsibilities
- Accept sensor/time-series protobuf messages and media segments
- Chunk and upload segments to DistributedStorageAgent
- Provide backpressure/queueing and persistent buffering when offline
- Emit metadata/index updates so consumers can discover new segments

Minimal API (Kotlin)

interface StreamIngestor {
  fun start()
  fun stop()
  fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray)
  fun ingestMediaSegment(streamId: String, metadata: SegmentMetadata, data: ByteArray)
}

Implementation notes
- Use coroutines and a bounded channel for ingestion
- Persist buffered chunks to local cache when network unavailable
- Use DistributedStorageAgent.putObjectChunked or similar API for large uploads
