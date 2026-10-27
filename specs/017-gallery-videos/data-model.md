# Data Model: Gallery Videos

## GalleryPhoto (changed) — `domain/model/GalleryPhoto.kt`

A gallery item, photo or video. New fields:

| Field        | Type        | Source (`Photo` resource) | Notes |
|--------------|-------------|---------------------------|-------|
| `mediaType`  | `MediaType` | `media_type`              | `PHOTO` / `VIDEO`; missing or unknown = `PHOTO` |
| `durationMs` | `Long?`     | `duration_ms`             | videos only |
| `fileSize`   | `Long?`     | `file_size`               | bytes of `image_url`; may be `null` for old photos |

- `fileExtension()`: photos as today; videos `mov` when `image_url` ends with `.mov`, else `mp4`.
- `isVideo` convenience.

## GalleryLocalState (changed)

| Field       | Type              | Notes |
|-------------|-------------------|-------|
| `originals` | `Map<Long, File>` | photo files (`gallery/photos/`) |
| `videos`    | `Map<Long, File>` | **new**: complete video files (`gallery/videos/{id}.{ext}`); partials never listed |
| `covers`    | `Map<String, File>` | unchanged |

## Visible index — `GalleryVisibility.visibleIndex(index, videos)`

- Item visible ⇔ photo, or video whose id ∈ `videos`.
- Album visible ⇔ has a visible item ∨ has a visible child ∨ its subtree has no item at all.
- Returns a `GalleryIndex` with only visible items and albums (same cursor).

## TreeTarget (changed)

`onDevice: Boolean = true` — `false` for an album hidden by visibility (shown dimmed in the move picker).

## Tiles (presentation)

- `PhotoTile(id, image, isVideo = false, duration: String? = null, onDevice = true)`.
- `AlbumTile(..., onDevice = true)`.
- `PhotoImage.VideoFrame(file)` — a frame of the local video (Coil video decoder).
- `ViewerPhoto(..., isVideo, videoFile: File?, mimeType)`.

## MediaFilter

`ALL` / `PHOTOS` / `VIDEOS`; AND with the people set.

## Preferences (`@SettingsPrefs`, survive sign-out)

| Key | Type | Default | Meaning |
|-----|------|---------|---------|
| `gallery_auto_download_videos` | Boolean | `false` | the switch |
| `gallery_video_card_dismissed` | Boolean | `false` | the discovery card was answered; never cleared |
| `gallery_photo_downloads_paused` | Boolean | `false` | only photos were deleted; photo downloads paused |

## Upload queue item (changed) — `UploadItem` / `UploadItemDto`

| Field        | DTO key       | Default   | Notes |
|--------------|---------------|-----------|-------|
| `kind`       | `kind`        | `photo`   | `photo` / `video` |
| `posterFileName` | `poster_file` | `null` | video poster in `gallery/uploads/` |
| `durationMs` | `duration_ms` | `null`    | video |
| `dateTaken`  | `date_taken`  | `null`    | `yyyy-MM-dd`, video metadata |
| `wifiOnly`   | `wifi_only`   | `false`   | "Esperar WiFi" chosen for this video |

A video item is `Prepared` as soon as it is probed (nothing else to prepare).

## Video probe — `VideoProbe` / `VideoProbeResult`

`container: VideoContainer (MP4, MOV, OTHER)`, `videoMime: String?`, `audioMimes: List<String>`, `byteSize: Long`,
`durationMs: Long?`, `dateTaken: String?`, `poster: File?`. `VideoUploadCheck.refusal(result): String?`.

## Core port — `core/domain/gallery/GalleryStoragePort`

- `GalleryUsage(photoBytes, videoBytes)`.
- `VideoSummary(total, downloaded, totalBytes, isDownloading)`.
- `MissingMedia(photos: Int, videos: Int)`.

## Work

| Unique work | Constraint | Policy | Purpose |
|-------------|-----------|--------|---------|
| `gallery_auto_download` | UNMETERED / CONNECTED | as today | photos only |
| `gallery_video_download` | UNMETERED | KEEP (REPLACE on "Baixar") | videos |
| `gallery_upload` | CONNECTED | APPEND_OR_REPLACE | queue (skips wifi-only on metered) |
| `gallery_upload_wifi` | UNMETERED | KEEP | queue, for wifi-only items |
