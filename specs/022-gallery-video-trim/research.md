# Research: Trim a Video Before Uploading It to the Gallery

Findings on Media3 come from reading the `media3-transformer` 1.11.1 sources (`Transformer`, `TrimOptimizedExportOperation`,
`TransformerUtil`, `Mp4Info`, `ExportResult`). Nothing here was run on a device. Device checks are in
[quickstart.md](quickstart.md).

## 1. Engine and trim optimization

- **Decision**: `Transformer` with `experimentalSetTrimOptimizationEnabled(true)` and `start(MediaItem, path)` with a
  `ClippingConfiguration`.
- **Rationale**: Chosen by the user. Only the part up to the first keyframe is re-encoded, the rest is copied.
- **Alternatives**: `MediaExtractor` + `MediaMuxer` and full re-encoding, both rejected in the decision conversation.

## 2. Keeping the source formats does not disable the optimization

- **Decision**: Call `setVideoMimeType(source)` and `setAudioMimeType(AAC)`.
- **Rationale**: `shouldTranscodeVideo` only forces a transcode when the requested MIME differs from the input. An equal
  MIME keeps the optimization and also fixes the format used by a fallback export, so it passes `VideoUploadCheck`.

## 3. Moving only the end handle

- **Finding**: With the start at 0, the first sync sample after the start is the first video sample. The operation
  reports `OPTIMIZATION_ABANDONED_KEYFRAME_PLACEMENT_OPTIMAL_FOR_TRIM` and runs a normal export with
  `startsAtKeyFrame = true`, which lets the video be copied without re-encoding.
- **Same path** when the end comes before the next keyframe: `processFullInput` without `startsAtKeyFrame`, so that
  short selection is re-encoded. This is the accepted fallback.

## 4. MOV sources

- **Finding**: The specify expected the fallback. The code reads the source with `Mp4Extractor`, whose sniffer is the
  same for MP4 and QuickTime brands, so a MOV may also get the optimization. Either way the output is an MP4 and the
  fallback is accepted. To confirm on a device.

## 5. Audio at the cut

- **Finding**: When the first keyframe is within one AAC buffer of the start, the operation moves the start to that
  keyframe (about 23 ms at most) to keep audio samples in the range. Audio sync of an optimized cut remains a device
  check.

## 6. Threading and progress

- **Decision**: Build and drive `Transformer` on the main thread, and poll `getProgress` every 200 ms.
- **Rationale**: Every public method verifies the application thread, so `cancel` cannot go in `invokeOnCancellation`,
  which may run on another thread. Cancellation is handled in a `catch` on the main dispatcher. `getProgress` may
  answer "waiting" or "unavailable", in which case the last value stays on screen.

## 7. Reading the picker URI

- **Finding**: `Mp4Info` opens the input through `DefaultDataSource`, which handles `content://`, so a picker URI works
  without copying the file.

## 8. Timeline frames

- **Decision**: Coil `AsyncImage` with `videoFrameMillis` through the existing `@GalleryThumbnailLoader`.
- **Rationale**: Its `VideoFrameDecoder` uses `MediaMetadataRetriever` as the specify asks, needs no new code, and keeps
  `Bitmap` out of the domain.

## 9. Mobile-data check after the editor

- **Decision**: Read `connectivityObserver.isOnWifi.first()` when the batch finishes.
- **Rationale**: The ViewModel's `isOnWifi` is `WhileSubscribed`, and no screen observing it may be active during the
  editor, so it can be stale. The raw flow emits the current state on collection.

## 10. Orphan cut files

- **Finding**: `GalleryMediaStore` has no cleanup for `gallery/uploads/`, confirmed by reading it.
- **Decision**: Cut files are named `trim-{uuid}.mp4`. A `Preloadable` deletes `trim-*` files the queue does not
  reference, once at app start, after `queue.load()`.
- **Rationale**: At process start no batch exists, so nothing can be in progress. The prefix leaves copies of an
  in-flight `enqueue` alone.

## 11. Back during a cut

- **Decision**: The confirmation opens while the cut goes on. If the cut ends while it is open, the batch does not
  finish until the person answers.
- **Rationale**: An accidental back should not lose the cut, and the person must not be taken away from a question.

## Still open, device only

APK size added by `media3-transformer`, and the matrix in `quickstart.md`. The dependency AAR itself is about 500 KB
before R8.
