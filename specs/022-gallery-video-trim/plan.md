# Implementation Plan: Trim a Video Before Uploading It to the Gallery

**Branch**: `022-gallery-video-trim` | **Date**: 2026-11-10 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/022-gallery-video-trim/spec.md`

## Summary

A batch with videos is held in memory by a controller owned by the graph-scoped `GalleryViewModel`. A new editor route
decides one video at a time. A cut runs in the foreground with Media3 `Transformer` behind a domain interface, reading
the picker URI directly and writing an MP4 into `gallery/uploads/`. When the last video is decided, the mobile-data
question runs with the final sizes and the batch is enqueued in picked order through a contract that accepts a picked
source or an already-cut file.

## Technical Context

**Language/Version**: Kotlin 2.3.10, AGP 9.0.1

**Primary Dependencies**: Jetpack Compose (BOM 2026.02.00), Hilt, Media3 1.11.1 (`exoplayer`, `ui`, new `transformer`),
Coil 2 with `coil-video` (timeline frames)

**Storage**: `filesDir/gallery/uploads/` for cut files; no new persisted state (the batch is memory only)

**Testing**: JUnit4, MockK, kotlinx-coroutines-test, turbine; fakes for `VideoTrimmer` and the upload repository

**Target Platform**: Android, `minSdk` 24

**Project Type**: Single-module Android app

**Performance Goals**: Near-lossless cut where allowed; UI stays responsive (progress polled every 200 ms)

**Constraints**: Backend unchanged; never compress; a whole video is sent byte for byte

**Scale/Scope**: One new screen, one new engine class, changes to the upload contract

## Constitution Check

`specs/constitution.md` covers error handling only.

| Rule | How this feature complies |
|------|---------------------------|
| No raw `Throwable` reaches the ViewModel | `VideoTrimmer.trim` returns `TrimmedFile?`; `TrimVideoUseCase` returns the sealed `TrimOutcome`. Only coroutine cancellation propagates. |
| Texts are authored by the app, hardcoded in Portuguese | `VideoTexts` |
| `CLAUDE.md`: `domain/` has no Android | `VideoTrimmer` and the models use only `String`, `Long`, lambdas |
| `CLAUDE.md`: no `Context` in the ViewModel | The engine lives in `data/upload`, frames come from Coil in the composable |
| `CLAUDE.md`: Screen + pure content | `VideoTrimScreen` collects, `VideoTrimContent` receives data and lambdas |

No violations.

## Project Structure

### Documentation

```text
specs/022-gallery-video-trim/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/upload-enqueue.md
└── tasks.md
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/features/gallery/
├── domain/upload/
│   ├── UploadModels.kt          # PickedMedia.trimmable, TrimmableVideo, UploadEntry
│   ├── VideoUploadCheck.kt      # canTrim
│   ├── VideoTrim.kt             # new: VideoTrimmer, TrimmedFile, TrimOutcome, TrimVideoUseCase
│   ├── UploadBatch.kt           # new: pure batch of decisions
│   ├── GalleryUploadRepository.kt, UploadUseCases.kt   # enqueue takes entries
├── domain/manage/GalleryManageUseCases.kt              # trimVideo
├── data/upload/
│   ├── Media3VideoTrimmer.kt    # new: the engine
│   ├── AndroidPickedVideoReader.kt   # new: reads a URI without copying
│   ├── VideoProbe.kt            # PickedVideoReader
│   ├── AndroidVideoProbe.kt     # shared track and date readers
│   ├── PickedImageCopier.kt     # display name
│   └── GalleryUploadRepositoryImpl.kt  # entries, orphan sweep
├── data/local/GalleryMediaStore.kt   # cut files
├── di/GalleryModule.kt
└── presentation/
    ├── state/VideoTrimUiState.kt     # new
    ├── viewmodel/VideoTrimController.kt   # new
    ├── viewmodel/GalleryViewModel.kt, VideoTexts.kt
    ├── state/GalleryDialogState.kt   # UploadOnMobileData carries entries
    ├── components/VideoTrimComponents.kt  # new: player, timeline
    ├── screens/VideoTrimScreen.kt    # new
    ├── screens/AlbumScreen.kt        # navigates when a batch opens
    └── navigation/GalleryNavGraph.kt # VIDEO_TRIM route
```

**Structure Decision**: Everything stays in `features/gallery`. The batch logic is a plain controller, like
`OrganizeSession`, so `GalleryViewModel` (already 1171 lines) only delegates and the rules are testable alone.

## Implementation Order

1. Dependency and domain: `media3-transformer`, models, `VideoUploadCheck.canTrim`, `VideoTrimmer`, use case, batch.
2. Data: URI reader, engine, repository entries and orphan sweep.
3. Presentation logic: controller, ViewModel integration, mobile-data question after the editor.
4. UI: route, player, timeline, screen.
5. Specs 017 and `gallery/spec.md` updated in the same change.

Risk: the engine cannot be exercised by unit tests. It is kept small, and `quickstart.md` lists the device checks.

## Complexity Tracking

None.
