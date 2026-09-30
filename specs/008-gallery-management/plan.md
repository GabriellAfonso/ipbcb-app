# Implementation Plan: Gallery Management

**Branch**: `008-gallery-management` | **Date**: 2026-09-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/008-gallery-management/spec.md`

## Summary

Managers build the gallery from the screens members already browse. Controls are composed only for the user's level
on `gallery` (`manage` / `owner`), read from the existing `ObserveAccessUseCase`. Every write goes through a new
`GalleryManageRepository` that calls the backend, applies the answer to the local index under the syncer's lock
without moving the cursor, and asks for a sync that is never skipped. Photos are uploaded by a persistent queue
processed by a foreground WorkManager job, one photo per request with a fixed `client_upload_id`, after being prepared
on the phone (≤ 4000 px, upright JPEG, EXIF date kept).

Key technical choices (details in [research.md](research.md)):

- **`applyLocal` under the sync lock**, cursor untouched, disk reconciled (R1); **`syncAfterWrite` re-runs** instead of
  skipping (R2).
- **Everything in `features/gallery`**; admin card navigates to `AppRoutes.GALLERY_GRAPH` (R3, R17).
- **Access from `ObserveAccessUseCase`**; 403 refresh already done by `PermissionDeniedInterceptor` (R4).
- **`JsonObject` PATCH bodies** because `explicitNulls = false` (R5); **error extras** added generically to the core
  parser (contract §1).
- **Queue = JSON snapshot + files under `gallery/uploads/`** (R6); picked images copied at once (R7).
- **Pure preparation planner + Android codec**, `androidx.exifinterface` 1.4.2, pixels rotated (R8).
- **`GalleryUploadWorker`**: `APPEND_OR_REPLACE`, any network, foreground `dataSync`, pure outcome classifier (R9).
- **`sh.calvin.reorderable` 3.1.0** on the existing album grid, groups never mix (R10).
- **Pure tree functions** for move targets and delete counts (R11); dialogs and bottom sheet, no new routes (R12).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (JVM 17 target), Android, `minSdk 24`, `targetSdk 36`

**Primary Dependencies**: existing — Jetpack Compose (BOM 2026.02, Material 3), Hilt (+ `hilt-work`), WorkManager
2.10, Retrofit 3 + OkHttp 5, kotlinx.serialization, Coil 2.6, `activity-compose` 1.12.4 (`PickMultipleVisualMedia`,
`PickVisualMedia`). **New**: `sh.calvin.reorderable:reorderable:3.1.0`, `androidx.exifinterface:exifinterface:1.4.2`

**Storage**: snapshot `filesDir/snapshots/gallery_upload_queue.json`; upload files `filesDir/gallery/uploads/`;
adopted originals `filesDir/gallery/photos/` (007 layout); temp cover files in `cacheDir`

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fakes preferred (`FakeGalleryApi` extended with the
writes, in-memory `SnapshotCache`, `FakeImageCodec`, temp-dir file tests); no Robolectric — bitmap work verified by the
manual checks in [quickstart.md](quickstart.md)

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: a write is visible locally right after the server answers (no extra round trip); preparation
≤ ~1 s per 12 MP photo on a mid-range phone; 50 queued photos proceed with the screen off (SC-002)

**Constraints**: writes online only (except the queue); `domain/` without Android types (URIs as `String`); only
`ResponseExt.kt` reads error bodies; features never import each other; Portuguese strings hardcoded; 120-char lines;
server limits 10 MB / 50 MP; nothing of the gallery (queue included) survives logout

**Scale/Scope**: tens of photos per event, occasionally ~200; ~25 new production files, ~15 touched; ~15 test classes;
`specs/gallery/spec.md` and `specs/admin/spec.md` updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ repositories return `Result<T>` with `AppError`; the worker classifies `AppError`s; `GalleryWriteError` is derived in domain from `AppError`, never from HTTP types |
| Conversion in the data layer | ✅ `GalleryManageRepositoryImpl`, `GalleryPhotoUploader` use `toAppError()` |
| `message` technical, `userMessage` for screen | ✅ server `detail` only when structured (`error_code`); English order-mismatch detail never shown (R12) |
| One HTTP error parsing point | ✅ extras added **inside** `ApiErrorParser`/`ResponseExt`; the gallery reads `AppError.Server.extras`, never `errorBody()` |
| 403 is permission, not login | ✅ 403 shows the server/permission text and hides controls; never the login button |
| Screen text via `toUserMessage()`; no per-screen generic variations | ✅ "Sem conexão" is set as `userMessage` by the repository for `AppError.Network` on gallery writes (the constitution's prescribed way for a screen-specific text) |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| UI → ViewModel → UseCase → Repository | ✅ `domain/manage` and `domain/upload` use cases; screens only know `GalleryViewModel` |
| Features don't import each other | ✅ admin uses `AppRoutes.GALLERY_GRAPH`; gallery uses core `access` |
| `domain/` without Android | ✅ URIs as `String`; `File` only where 007 already uses it (`GalleryLocalState`), plus `originalForCover` returning `File` — same exception |
| Screen/Content split, dumb composables, three states | ✅ new dialogs and sheets receive state + lambdas |
| `StateFlow` only; one-time events | ⚠️ keeps 007's consumable `StateFlow<GalleryMessage?>` (already justified); dialogs are state, not events |
| `viewModelScope`, no manual scopes | ✅ copying in `viewModelScope`; upload in WorkManager |
| Graph-scoped ViewModel | ✅ unchanged |
| No unnecessary abstractions; check lib compatibility | ✅ two small libraries, compatibility checked (R10) with a no-library fallback; `ImageCodec` interface exists only to test the preparer without Robolectric |
| Snapshot cache pattern (Pitfall #3) | ✅ `gallery_upload_queue` via `SnapshotCacheFactory` |
| Qualifiers | ✅ all writes on `@AuthedRetrofit` |
| Security: no PII in logs | ✅ logs carry ids and HTTP codes only; EXIF location not carried over |
| Tests: happy path + 1 error per use case, fakes | ✅ quickstart table |
| Spec and code in the same commit | ✅ `specs/gallery/spec.md`, `specs/admin/spec.md` updated with the code |

**Gate: PASS.** Post-design re-check: PASS — the core change (error extras) is additive and generic; the only new
manifest entries are the foreground-service declarations WorkManager needs.

## Project Structure

### Documentation (this feature)

```text
specs/008-gallery-management/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R17
├── data-model.md
├── quickstart.md
├── contracts/
│   └── gallery-management-client.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
gradle/libs.versions.toml, app/build.gradle.kts           # + reorderable 3.1.0, exifinterface 1.4.2
app/src/main/AndroidManifest.xml                          # FOREGROUND_SERVICE(_DATA_SYNC), SystemForegroundService dataSync

app/src/main/java/com/ipb/castelobranco/
├── core/network/error/ApiErrorBody.kt, ApiErrorParser.kt, ResponseExt.kt   # + extras
├── core/domain/error/AppError.kt                         # Server.extras
├── features/admin/panel/…/AdminScreen.kt, navigation     # GALLERY card enabled → AppRoutes.GALLERY_GRAPH
└── features/gallery/
    ├── data/
    │   ├── api/GalleryApi.kt, GalleryEndpoints.kt        # + 10 write calls
    │   ├── dto/PhotoUploadResultDto.kt (new), GalleryWriteBodies.kt (new, JsonObject builders)
    │   ├── manage/GalleryManageRepositoryImpl.kt (new), GalleryWriteErrors.kt (new, AppError → GalleryWriteError)
    │   ├── sync/GallerySyncer.kt                         # + applyLocal, syncAfterWrite (re-run flag)
    │   ├── local/GalleryMediaStore.kt                    # + uploadsDir, adoptOriginal, tempFile helpers
    │   ├── upload/
    │   │   ├── GalleryUploadQueueStore.kt (new)          # snapshot + StateFlow, mutex
    │   │   ├── PickedImageCopier.kt (new)                # ContentResolver → uploads/
    │   │   ├── UploadPreparationPlanner.kt (new, pure)
    │   │   ├── ImageCodec.kt (new, interface), AndroidImageCodec.kt (new)
    │   │   ├── GalleryImagePreparer.kt (new)
    │   │   ├── UploadOutcomeClassifier.kt (new, pure)
    │   │   ├── GalleryUploadRun.kt (new)                 # the loop, testable without WorkManager
    │   │   └── GalleryUploadRepositoryImpl.kt (new)
    │   └── work/GalleryUploadWorker.kt (new), WorkManagerGalleryUploadScheduler.kt (new),
    │       GalleryUploadNotifications.kt (new)
    ├── di/GalleryModule.kt (+ bindings), GallerySnapshotModule.kt (+ queue snapshot)
    ├── domain/
    │   ├── model/GalleryIndex.kt (+ apply), GalleryLocalChange.kt (new), GalleryTree.kt (+ targets, counts)
    │   ├── manage/ (new) GalleryManageRepository, drafts, GalleryNames, GalleryWriteError, BatchResult, use cases
    │   ├── upload/ (new) UploadItem, UploadState, UploadOutcome, GalleryUploadRepository, GalleryUploadScheduler,
    │   │   EnqueueUploadsUseCase, DismissUploadUseCase
    │   └── usecase/SyncGalleryUseCase.kt (+ afterWrite), GalleryAutoDownloadUseCase.kt (logout clears queue)
    └── presentation/
        ├── state/GalleryUiState.kt                       # permissions, organize, selection, uploads; GalleryMessage sealed
        ├── state/GalleryDialogState.kt (new)             # album/photo forms, move picker, confirmations
        ├── viewmodel/GalleryViewModel.kt                 # access, management actions, dialog state
        ├── viewmodel/OrganizeSession.kt, PhotoSelection.kt (new state holders)
        ├── components/GalleryManageComponents.kt (new)   # forms, picker sheet, confirm, selection bar, upload strip
        └── screens/GalleryScreen.kt, AlbumScreen.kt, PhotoScreen.kt   # controls by permissions, pickers, reorder

app/src/test/java/com/ipb/castelobranco/
├── core/network/error/ (extras)
└── features/gallery/ (see quickstart table)

specs/gallery/spec.md, specs/admin/spec.md                # updated with the code
```

**Structure Decision**: all gallery work stays in `features/gallery` with the project's data/domain/presentation
split; core changes are limited to the generic error extras and nothing else feature-specific; the admin feature only
enables its card.

## Implementation Order

1. **Dependencies + build check** — add reorderable and exifinterface; `assembleDebug`; confirm resolved Compose
   versions (R10 fallback decided here).
2. **Core error extras** — `ApiErrorBody.extras`, `AppError.Server.extras`, tests.
3. **Domain, pure** — `GalleryLocalChange` + `GalleryIndex.apply`, tree targets/counts, `GalleryNames`, write bodies,
   `UploadPreparationPlanner`, `UploadOutcomeClassifier` — test first.
4. **Syncer** — `applyLocal`, `syncAfterWrite`; `SyncGalleryUseCase.afterWrite`.
5. **Manage repository + use cases** — API calls, error mapping, local apply; batch and reorder use cases.
6. **Upload pipeline** — queue store, copier, codec + preparer, run loop, worker, scheduler, notifications, manifest;
   logout path.
7. **Presentation** — permissions in UiStates; root (FAB, organize); album (FAB menu, overflow, selection, organize,
   upload strip, failed list, pickers); viewer (overflow, edit, move, cover, delete); dialogs; messages.
8. **Admin card** enabled.
9. **Specs** — amend spec decision 2 wording already done in this plan's commit; update `specs/gallery/spec.md` and
   `specs/admin/spec.md`; run the quickstart.

## Risks

| Risk | Mitigation |
|------|-----------|
| `reorderable` compiled against Compose 1.7 misbehaves on 1.10 | Build check first (step 1); fallback: long-press drag with manual index math, no library |
| `setForeground` refused when the worker starts in the background (Android 12+) | Caught; runs as a plain worker (may be stopped after ~10 min, resumes on the next run with the same ids) |
| Android 15 `dataSync` 6 h/day limit | Far above any realistic queue; timeout ends the run, WorkManager retries later |
| Out-of-memory decoding huge images | `inSampleSize` from bounds before decoding; long side decoded ≤ 2× target |
| Full sync replaces the index between a write and its local apply | Self-heals on the next sync (R1) |
| Copy cancelled when the gallery is left mid-copy | Already-copied items stay queued; "Preparando fotos…" visible while copying (R7) |
| A manager not flagged as member sees "Disponível apenas para membros." | Operational rule (spec); no workaround |

## Implementation Notes

Where the code settled differently from the design above (the spec is unchanged):

- **Library check (T002)**: `reorderable` 3.1.0 builds; `androidx.compose.foundation` resolves to 1.10.3 (the
  library asks for 1.7.0). No fallback needed.
- **Write errors**: the *kind* of a refusal (`isCycle`, `isOrderMismatch`, `isNotFound`, `isForbidden`,
  `isValidation`) is pure domain (`domain/manage/GalleryWriteErrorKinds.kt`, reading `AppError.Server.extras`); the
  *text* (`GalleryWriteError`) is built in presentation (`presentation/viewmodel/GalleryWriteErrors.kt`) with
  `toUserMessage()`, so the data layer never imports presentation. `BatchResult` keeps one `AppError` per failed
  photo; the screen groups them by text (`GalleryManageTexts`).
- **Forms**: one `ItemFormState` with a `FormTarget` (`NewAlbum` / `Album` / `Photo`) instead of separate album and
  photo form states — same fields, one dialog.
- **Use cases**: injected into the ViewModel as one `GalleryManageUseCases` holder; the selection lives in the
  ViewModel as a small private `Selection` (no separate `PhotoSelection` file).
- **Upload files**: the raw copy is `uploads/{uploadId}-src.{ext}` and the prepared JPEG `uploads/{uploadId}.jpg`, so a
  picked `.jpg` never collides with its prepared file. `GalleryUploadRepository.prepareCover` prepares a picked cover
  without queueing it.
- **Album upload progress**: the album shows its own pending count and failures, and the queue's global "Enviando X de
  N" (the notification's numbers) while it has pending items.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `ImageCodec` interface with a single Android implementation | Lets `GalleryImagePreparer` be unit-tested (EXIF date and orientation written, JPEG output, quality ladder) without Robolectric | Adding Robolectric is a new test library for one class; testing only the planner would leave the EXIF carry-over untested |
| Consumable `StateFlow<GalleryMessage?>` instead of `SharedFlow` (inherited from 007) | Management messages must survive the back-stack change after deleting the open album | Same as 007: a `SharedFlow` would drop the message during navigation |
