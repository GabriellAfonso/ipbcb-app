# Implementation Plan: Gallery Videos

**Branch**: `017-gallery-videos` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/017-gallery-videos/spec.md`

## Summary

`GalleryPhoto` gains `mediaType`, `durationMs`, `fileSize` (DTO and snapshot additive, old snapshots read as
photos). Video files live in `gallery/videos/{id}.{mp4|mov}`, partials as `{id}.{ext}.part` next to them, so
`GalleryLocalState` carries `videos` apart from `originals`. A pure `GalleryVisibility` builds the **visible
index** (undownloaded videos and albums whose subtree holds only those removed); every normal screen builds its
`GalleryTree` from it, while "Organizar" and the move picker use the full tree and mark what is not on the device.
A second worker (`GalleryVideoDownloadWorker`, unique `gallery_video_download`, always `UNMETERED`) downloads videos
with HTTP `Range` resume and a 1 GB free-space floor, only while the new switch is on. The viewer embeds a Media3
`PlayerView` per video page (muted autoplay, graph-scoped sound state, built-in repeat toggle, landscape full
screen). Upload accepts images and videos from one picker; videos are checked with `MediaExtractor`, get a poster
and duration from `MediaMetadataRetriever` at pick time, and go through the same queue as photos with a `video`
multipart; a batch with videos on a metered network asks first, and "Esperar WiFi" marks its videos `wifi_only`,
served by a second unique upload work constrained to `UNMETERED`. Settings becomes `settingsGraph` with a "Galeria"
screen talking to the gallery through a new core port (`core/domain/gallery/GalleryStoragePort`). Decisions are in
[research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, Retrofit + OkHttp, kotlinx.serialization, Coil 2.6, WorkManager 2.10 (present);
**new**: `androidx.media3:media3-exoplayer` + `media3-ui` 1.11.1 (minCompileSdk 36, minSdk 23, Kotlin stdlib
2.2.10 — compatible with compileSdk 36 / minSdk 24 / Kotlin 2.3.10), `io.coil-kt:coil-video` 2.6.0 (same version as
Coil, for offline video thumbnails).

**Storage**: index snapshot `gallery_index` (additive fields); upload queue snapshot `gallery_upload_queue`
(additive fields); `@SettingsPrefs` keys `gallery_auto_download_videos`, `gallery_video_card_dismissed`,
`gallery_photo_downloads_paused`; files under `filesDir/gallery/videos/`.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred (`FakeGalleryApi`,
`GalleryTestFixtures`, `FakeGalleryManageRepository`).

**Target Platform**: Android (minSdk 24, compileSdk 36)

**Project Type**: mobile app, single `:app` module

**Performance Goals**: visibility computed once per local-state change off the main thread (as the tree is today);
one player instance per open viewer.

**Constraints**: no streaming; partial files never visible; videos only on WiFi; 1 GB free floor; 100 MB upload
cap; 120-char lines; Portuguese user strings; features never import each other.

**Scale/Scope**: ~3000 photos and tens of videos per gallery; 6 gallery screens touched, 1 new settings screen,
2 new workers, 1 new multipart upload.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule (`specs/constitution.md`, `CLAUDE.md`) | Status |
|---|---|
| `AppError` is the only error type crossing data → presentation | Pass: video download/upload outcomes are sealed results; refusals are `String` reasons |
| Only `ResponseExt.kt` parses error bodies | Pass: the upload classifier keeps using `toAppError()` and `extras["rejected"]` |
| `userMessage` only from a structured `detail` | Pass: rejected reasons come from the server's structured `rejected` list, as today |
| Features never import each other | Pass after this feature: settings stops importing `features.gallery` and uses `core/domain/gallery/GalleryStoragePort` (bible import untouched, out of scope) |
| `domain/` has no Android | Pass: `GalleryVisibility`, `VideoUploadCheck`, `VideoDuration` are pure; `MediaExtractor`/`MediaMetadataRetriever` live in `data/upload/AndroidVideoProbe` behind `VideoProbe` |
| Screen/content split; `StateFlow` + events | Pass: `GallerySettingsScreen` = collector + `GallerySettingsContent`; player state in UI, mute in VM |
| `viewModelScope` only | Pass |
| Graph-scoped VM | Pass: mute state lives in the graph-scoped `GalleryViewModel` (spec: resets on the next gallery visit) |
| Navigation: multiple screens → graph | Pass: `settingsGraph` (main, gallery, logs) in `SettingsNavGraph.kt` |
| No PII in logs | Pass: logs carry ids and HTTP codes only |

No violations; Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/017-gallery-videos/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/ui.md
├── checklists/requirements.md
└── tasks.md
specs/gallery/spec.md, specs/settings/spec.md   # domain specs updated with the feature
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/gallery/GalleryStoragePort.kt            # NEW: sizes, video switch, delete/download by type
│   └── presentation/navigation/AppNavHost.kt           # settings → settingsGraph
├── features/gallery/
│   ├── domain/model/GalleryPhoto.kt                    # + mediaType, durationMs, fileSize; MediaType
│   ├── domain/model/GalleryLocalState.kt               # + videos
│   ├── domain/model/GalleryVisibility.kt               # NEW: visible index
│   ├── domain/model/GalleryTree.kt                     # moveTargets carry `onDevice`
│   ├── domain/model/VideoDuration.kt                   # NEW: m:ss / h:mm:ss
│   ├── domain/tags/GalleryPeople.kt                    # + media-type filter
│   ├── domain/download/GalleryVideoDownloadScheduler.kt # NEW
│   ├── domain/usecase/…                                # DeleteGalleryUseCase(types), GalleryVideoSettings,
│   │                                                   #   SyncGalleryUseCase queues videos
│   ├── domain/upload/VideoUploadCheck.kt               # NEW: pure rules (size, container, codecs)
│   ├── data/dto/GalleryPhotoDto.kt, GalleryMappers.kt  # new fields
│   ├── data/local/GalleryMediaStore.kt                 # videos dir, partials, bytes per type
│   ├── data/local/GalleryPreferences.kt                # 3 keys
│   ├── data/sync/GallerySyncer.kt                      # videos in state, prune partials, clearPhotos/Videos
│   ├── data/download/GalleryVideoDownloader.kt         # NEW: Range resume + free-space floor
│   ├── data/download/GalleryVideoDownloadJob.kt        # NEW
│   ├── data/work/GalleryVideoDownloadWorker.kt, WorkManagerGalleryVideoDownloadScheduler.kt  # NEW
│   ├── data/upload/AndroidVideoProbe.kt, VideoProbe.kt # NEW
│   ├── data/upload/PickedImageCopier.kt → PickedMediaCopier # images and videos
│   ├── data/upload/GalleryUploadRun.kt, …QueueStore, UploadQueueSnapshot, UploadModels  # video items, wifi_only
│   ├── data/work/WorkManagerGalleryUploadScheduler.kt  # + enqueueWifiOnly (second unique work)
│   ├── data/GalleryStoragePortImpl.kt                  # NEW: implements the core port
│   └── presentation/…                                  # tiles, card, filter chips, player, upload dialog
└── features/settings/
    ├── presentation/navigation/SettingsNavGraph.kt     # NEW
    ├── presentation/screens/GallerySettingsScreen.kt   # NEW
    └── presentation/viewmodel/GallerySettingsViewModel.kt # NEW
```

**Structure Decision**: everything stays in the gallery feature except the settings screen, which reaches the
gallery only through the core port.

## Implementation order

1. **Model and storage** (P1 foundation): fields, snapshot compatibility, videos dir, local state, visibility.
2. **Visibility in screens** (US2): visible tree everywhere; full tree in Organizar / move picker.
3. **Video download** (US1): preference, worker, Range resume, free space, scheduling from sync and switch.
4. **Player and parity** (US1, US6): tiles with play icon and duration, Media3 page, share/save, cover from poster,
   media-type filter.
5. **Discovery card** (US3).
6. **Upload** (US4): probe, copier, queue fields, multipart, metered dialog, wifi-only work, adopt.
7. **Settings** (US5): core port, settingsGraph, gallery settings screen and dialogs, delete/download by type.
8. Specs (`specs/gallery/spec.md`, `specs/settings/spec.md`) updated alongside each step.
