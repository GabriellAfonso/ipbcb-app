# Contract: Upload enqueue and trimmer (internal)

The server contract does not change (`backend/specs/023-gallery-videos/contracts/video-upload-api.md`).

## `GalleryUploadRepository.enqueue(albumId, entries: List<UploadEntry>, videosWifiOnly)`

Entries are queued in order.

| Entry | Effect |
|-------|--------|
| `Picked(source)` | As before: copied, a video is probed and checked, a refused one enters as failed |
| `Cut(fileName, displayName, dateTaken)` | Not copied. Probed and checked in place. `dateTaken` comes from the entry, never from the output. A refused cut enters as failed and its file is deleted |

## `GalleryUploadRepository.inspect(sources)`

Returns `PickedMedia` per source and fills `trimmable` for a video that may open the editor. Reads the URI, never copies.

## `VideoTrimmer`

| Call | Contract |
|------|----------|
| `trim(source, startMs, endMs, videoMime, onProgress)` | Suspends until done. Returns `TrimmedFile(fileName, sizeBytes)` or `null` on failure, with the partial output deleted. Coroutine cancellation stops the export, deletes the partial output and rethrows |
| `delete(fileName)` | Deletes a cut file |
