---

description: "Task list for 008-gallery-management"
---

# Tasks: Gallery Management

**Input**: Design documents from `specs/008-gallery-management/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/gallery-management-client.md](contracts/gallery-management-client.md), [quickstart.md](quickstart.md)

**Tests**: Requested by the spec (SC-010 and the request's test list). JUnit4 + MockK + coroutines-test + Turbine,
fakes preferred. Within each story, write the tests first and see them fail.

**Organization**: One phase per user story, in priority order (P1: US1, US2, US3; P2: US4, US5, US6; P3: US7, US8).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1…US8 from spec.md

## Path Conventions

- `MAIN` = `app/src/main/java/com/ipb/castelobranco`
- `TEST` = `app/src/test/java/com/ipb/castelobranco`
- `GAL` = `MAIN/features/gallery`, `GALT` = `TEST/features/gallery`
- Run tests: `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"` (never `clean`,
  `--rerun-tasks` or `--no-daemon`)
- Research decisions are cited as R1…R17 ([research.md](research.md)); texts come from contract §4.

---

## Phase 1: Setup (Dependencies and Manifest)

**Purpose**: New libraries confirmed to build with the project's Compose BOM and Kotlin (R8, R10).

- [X] T001 Add `reorderable = "3.1.0"` (`sh.calvin.reorderable:reorderable`) and `exifinterface = "1.4.2"` (`androidx.exifinterface:exifinterface`) to `gradle/libs.versions.toml` and `implementation(libs.reorderable)`, `implementation(libs.androidx.exifinterface)` to `app/build.gradle.kts`
- [X] T002 Run `./gradlew :app:assembleDebug` and `./gradlew :app:dependencies --configuration debugRuntimeClasspath`; confirm `androidx.compose.foundation:foundation` resolves to the BOM version (1.10.x). If the build or resolution fails, remove `reorderable` and record in `specs/008-gallery-management/research.md` R10 that the fallback (long-press drag with manual index math) is used
- [X] T003 [P] Add `android.permission.FOREGROUND_SERVICE` and `android.permission.FOREGROUND_SERVICE_DATA_SYNC` permissions and `<service android:name="androidx.work.impl.foreground.SystemForegroundService" android:foregroundServiceType="dataSync" tools:node="merge" />` to `app/src/main/AndroidManifest.xml` (R9)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Error extras, local apply, post-write sync, write API, write-error mapping and messages — every story
uses them.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Tests for the foundation ⚠️

- [X] T004 [P] Extend `TEST/core/network/error/ApiErrorParserTest.kt`: a structured body with `rejected`, `missing`/`unexpected`/`repeated`, `chain` and `client_upload_id` yields `extras` with each key's raw JSON text; `error_code`, `detail`, `field_errors` never appear in `extras`; a body without extras yields an empty map
- [X] T005 [P] Extend `TEST/core/network/error/ResponseExtTest.kt`: `AppError.Server.extras` carries the parsed extras for a 400 and a 409; `AppError.Auth` unchanged
- [X] T006 [P] Create `GALT/domain/model/GalleryIndexApplyTest.kt`: each `GalleryLocalChange` variant (data-model table) applied keeps `cursor`; `RemoveAlbumTree` removes the album, all descendants at every depth and all their photos, and leaves siblings and their photos; unknown ids leave the index equal; `ReorderAlbums`/`ReorderPhotos` set `position` = index and leave unlisted items untouched; applying any change twice equals applying it once; a later `applyDelta` repeating an upserted item gives the same index
- [X] T007 [P] Extend `GALT/data/sync/GallerySyncerTest.kt`: `applyLocal` saves the snapshot with the previous cursor and publishes the new state; `applyLocal` called while a sync holds the lock runs after it (ordering observable through the fake API's gate); `applyLocal(RemoveAlbumTree)` deletes the originals of the removed photos from the temp dir; `syncAfterWrite` while a sync runs triggers exactly one extra `getChanges` after it; two `syncAfterWrite` calls during one sync still trigger one extra; `applyLocal` after `clear()` writes nothing when the session is gone
- [X] T008 [P] Create `GALT/data/dto/GalleryWriteBodiesTest.kt`: create body omits blank description and null date; album PATCH with `parentId = Set(null)` has `"parent_id": null`; `eventDate = Set(null)` has `"event_date": null`; `Unchanged` fields are absent; album order body always has `parent_id` (explicit null for root) and `ids`; photo PATCH with `dateTaken = Set(null)` has `"date_taken": null`
- [X] T009 [P] Create `GALT/data/manage/GalleryWriteErrorsTest.kt`: `AppError.Network` → `Offline` with "Sem conexão"; `Auth(403)` → `Forbidden(detail)`; `Server(404)` → `NotFound`; `Server(400)` with `chain` → `Cycle(detail)`; with `missing`/`unexpected`/`repeated` → `OrderMismatch`; with `rejected` → `Rejected(first reason)`; `VALIDATION_ERROR` without extras on a name write → `DuplicateName(detail)`; anything else → `Other(toUserMessage())`

### Implementation for the foundation

- [X] T010 Add `extras: Map<String, String> = emptyMap()` to `MAIN/core/network/error/ApiErrorBody.kt` and fill it in `parseApiError` (`MAIN/core/network/error/ApiErrorParser.kt`) with every top-level key except `error_code`, `detail`, `field_errors`, value as raw JSON text (`json.get(key).toString()`)
- [X] T011 Add `val extras: Map<String, String>? = null` to `AppError.Server` in `MAIN/core/domain/error/AppError.kt` and pass `parsed?.extras?.ifEmpty { null }` from `MAIN/core/network/error/ResponseExt.kt` (only parsing point; constitution)
- [X] T012 [P] Create `GalleryLocalChange` sealed interface (`UpsertAlbum`, `UpsertPhoto`, `RemoveAlbumTree`, `RemovePhotos`, `ReorderAlbums`, `ReorderPhotos`) in `GAL/domain/model/GalleryLocalChange.kt` and `fun apply(change: GalleryLocalChange): GalleryIndex` on `GalleryIndex` in `GAL/domain/model/GalleryIndex.kt` (R1; cursor untouched; subtree walk tolerant of cycles)
- [X] T013 Add to `GAL/data/sync/GallerySyncer.kt` (R1, R2): `suspend fun applyLocal(change: GalleryLocalChange, afterApply: suspend () -> Unit = {})` — `mutex.withLock`, `ensureLoaded`, session check, `index.apply(change)`, `snapshotCache.save`, `afterApply()` (used to adopt an uploaded original before the reconcile), `reconcile`, publish `withFiles`; and `suspend fun syncAfterWrite()` — sets a `rerunRequested` flag when the lock is busy (the running `sync()` loops once more before unlocking), otherwise runs `sync()`; a `clear()` resets the flag
- [X] T014 Add `suspend fun applyLocal(change: GalleryLocalChange)` and `suspend fun syncAfterWrite(): GallerySyncResult` to `GAL/domain/repository/GalleryRepository.kt` and delegate in `GAL/data/repository/GalleryRepositoryImpl.kt`; add `suspend fun afterWrite(): GallerySyncResult` to `GAL/domain/usecase/SyncGalleryUseCase.kt` (same missing-originals rule as `invoke`)
- [X] T015 [P] Add endpoint constants (`ALBUMS`, `ALBUM`, `ALBUM_ORDER`, `ALBUM_COVER`, `ALBUM_PHOTOS_ORDER`, `PHOTOS`, `PHOTO`, part/field names `album_id`, `image`, `client_upload_id`) to `GAL/data/api/GalleryEndpoints.kt` and the ten calls of contract §1 to `GAL/data/api/GalleryApi.kt` (`JsonObject` bodies for create/patch/album order, `@Multipart` for cover and upload, `Response<Unit>` for 204s)
- [X] T016 [P] Create `PhotoUploadResultDto` and `RejectedFileDto` in `GAL/data/dto/PhotoUploadResultDto.kt` (contract §1)
- [X] T017 [P] Create `Field<T>` (`Unchanged`, `Set(value)`), `AlbumDraft`, `AlbumEdit`, `PhotoEdit`, `BatchResult` in `GAL/domain/manage/GalleryDrafts.kt` and `GalleryWriteError` sealed interface (`Offline`, `Forbidden`, `NotFound`, `DuplicateName`, `Cycle`, `OrderMismatch`, `Rejected`, `Other`, each with `message: String`) in `GAL/domain/manage/GalleryWriteError.kt`
- [X] T018 Create the `JsonObject` builders `AlbumDraft.toCreateBody()`, `AlbumEdit.toPatchBody()`, `PhotoEdit.toPatchBody()`, `albumOrderBody(parentId, ids)`, `photoOrderBody(ids)` in `GAL/data/dto/GalleryWriteBodies.kt` (R5; make T008 pass)
- [X] T019 Create `fun AppError.toGalleryWriteError(): GalleryWriteError` in `GAL/data/manage/GalleryWriteErrors.kt`, reading only `AppError.Server.extras`/`errorCode`/`userMessage` and `toUserMessage()` (R12, contract §2; make T009 pass)
- [X] T020 Extend the test fake `GALT/data/api/FakeGalleryApi.kt` with scripted responses for every write call (queue per call, records bodies, parts and path ids) and builders for 400/403/404/409 structured error bodies in `GALT/data/GalleryTestFixtures.kt`
- [X] T021 Create the `GalleryManageRepository` interface (data-model table) in `GAL/domain/manage/GalleryManageRepository.kt` and `GalleryManageRepositoryImpl` skeleton in `GAL/data/manage/GalleryManageRepositoryImpl.kt` with a private `write(call, onSuccess)` helper: runs the call, maps non-2xx via `Response.toAppError()` and exceptions via `Throwable.toAppError()`, on success `repository.applyLocal(change)` then `syncGallery.afterWrite()`; failures returned as `Result.failure(AppError)`; bind it in `GAL/di/GalleryModule.kt`
- [X] T022 [P] Change `GalleryMessage` from `enum` to `sealed interface GalleryMessage { val text: String }` keeping `PhotoRemoved`, `PhotoMoved`, `AlbumRemoved` and adding `AlbumCreated`, `Saved`, `AlbumTrashed`, `PhotoTrashed`, `PhotoMovedTo(albumName)`, `Batch(text)`, `OrderChanged`, `CoverUpdated`, `CoverRemoved`, `Failure(text)` with contract §4 texts in `GAL/presentation/state/GalleryUiState.kt`; update the three screens' snackbar code to use `.text`
- [X] T023 [P] Create `GalleryDialogState` sealed interface (`AlbumForm(AlbumFormState)`, `PhotoForm(PhotoFormState)`, `MovePicker(MovePickerState)`, `Confirm(ConfirmState)`) and the four state classes of data-model "UiState additions" in `GAL/presentation/state/GalleryDialogState.kt`; add `dialog: StateFlow<GalleryDialogState?>` and `dismissDialog()` to `GAL/presentation/viewmodel/GalleryViewModel.kt`, plus a `selfRemovedIds` set consulted by the existing removal detection (album and viewer) so self-made removals post their own message instead of `AlbumRemoved`/`PhotoRemoved` (R12)

**Checkpoint**: extras parsed, local apply and post-write sync proven, write API available — stories can start.

---

## Phase 3: User Story 1 - Controls follow the user's level (Priority: P1) 🎯 MVP

**Goal**: Members see today's screens; `manage` and `owner` see exactly their controls; the admin card opens the
gallery; a 403 hides lost controls and shows the server message.

**Independent Test**: Sign in as M, G, O and open root, album and viewer: visible controls match contract §3; G taps
the admin card and lands on the gallery root.

### Tests for User Story 1 ⚠️

- [X] T024 [P] [US1] Create `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt` (fake `AccessRepository` flow): with `Access.NONE` every UiState has `GalleryPermissions.NONE`; with `gallery = MANAGE` `canManage = true, canDelete = false`; with `OWNER` both true; `canRemoveCover` true only for `OWNER` and `coverSourceAlbumId == albumId`; a new access emission updates the states without re-creating the ViewModel; a write failing with `Auth(403)` posts `GalleryMessage.Failure(detail)` and leaves the index unchanged
- [X] T025 [P] [US1] Extend the admin panel test (the existing `AdminViewModel`/panel test under `TEST/features/admin/panel/`) or add `TEST/features/admin/panel/AdminGalleryCardTest.kt`: `PanelCard.GALLERY` is enabled and its action invokes `AdminNav.gallery`

### Implementation for User Story 1

- [X] T026 [P] [US1] Create `GalleryPermissions(canManage, canDelete)` with `NONE` and `fun Access.toGalleryPermissions()` (`allows(Scope.GALLERY, MANAGE)` / `allows(Scope.GALLERY, OWNER)`) in `GAL/presentation/state/GalleryPermissions.kt` (R4)
- [X] T027 [US1] Inject `ObserveAccessUseCase` into `GAL/presentation/viewmodel/GalleryViewModel.kt`; expose a `permissions: StateFlow<GalleryPermissions>` and combine it into `rootState`, `albumState(id)` (plus `canRemoveCover`) and `viewerState(...)`; add the `permissions`/`canRemoveCover` fields to `GalleryRootUiState`, `AlbumUiState`, `PhotoViewerUiState` in `GAL/presentation/state/GalleryUiState.kt` and compute them in `GAL/presentation/viewmodel/GalleryUiMapper.kt`
- [X] T028 [US1] Add a `showWriteFailure(error: AppError)` helper in `GAL/presentation/viewmodel/GalleryViewModel.kt` that posts `GalleryMessage.Failure(error.toGalleryWriteError().message)`; no explicit access refresh (R4: `PermissionDeniedInterceptor` + `CoreViewModel` already refresh) — document this in a KDoc line
- [X] T029 [US1] In `GAL/presentation/screens/GalleryScreen.kt`, `AlbumScreen.kt`, `PhotoScreen.kt` pass `permissions` to the content composables and compose management slots (FAB, overflow `DropdownMenu`, long-press) only when the matching flag is true; members' layout must be byte-for-byte the current one (no empty action slots, no extra padding)
- [X] T030 [P] [US1] Add `gallery: () -> Unit` to `AdminNav` and build it as `{ navController.navigate(AppRoutes.GALLERY_GRAPH) }` in `MAIN/features/admin/panel/presentation/navigation/AdminNavGraph.kt`; enable the `PanelCard.GALLERY` action with `onClick = nav.gallery` in `MAIN/features/admin/panel/presentation/screens/AdminScreen.kt` (R17)

**Checkpoint**: gating works end to end; no control does anything yet beyond opening its (empty) slot.

---

## Phase 4: User Story 2 - Upload the photos of an event (Priority: P1)

**Goal**: "Adicionar fotos" copies picked images, prepares them, and a persistent foreground queue sends them one by
one with a fixed upload id, applying each accepted photo locally with its file adopted as the original.

**Independent Test**: Quickstart scenarios 3, 4, 5 and 6.

### Tests for User Story 2 ⚠️

- [X] T031 [P] [US2] Create `GALT/data/upload/UploadPreparationPlannerTest.kt`: 6000×4000 → long side 4000, aspect kept, `sampleSize` 1 (≥ target); 12000×9000 → `sampleSize` 2; 3000×2000 → unchanged size, still `Reencode` (JPEG output); EXIF orientations 3/6/8 → 180/90/270 degrees with target dimensions swapped for 90/270; 2/4/5/7 set `flipHorizontal`; GIF 5 MB 1000×1000 → `SendAsIs`; GIF 12 MB → `Reencode`; GIF above 50 MP → `Reencode`; quality ladder starts at 90 and ends at 60
- [X] T032 [P] [US2] Create `GALT/data/upload/GalleryImagePreparerTest.kt` with a `FakeImageCodec` (records calls, returns scripted sizes): a JPEG source is re-encoded at 90 when ≤ 10 MB; a result above 10 MB is re-encoded at 85, 80… until it fits; nothing fits → `PrepareResult.TooLarge`; EXIF `DateTimeOriginal`/`OffsetTimeOriginal`/`SubSecTimeOriginal`/`DateTimeDigitized`/`DateTime` copied from source and `Orientation` written as NORMAL; GPS tags never written; unreadable source → `PrepareResult.Unreadable`; GIF within limits → file sent as is, codec never asked to encode
- [X] T033 [P] [US2] Create `GALT/data/upload/UploadOutcomeClassifierTest.kt` covering every row of the R9 table: 201 new, 201 dedup with the photo in another album (→ `Sent` with that album), 207 with the item rejected, 400 with `rejected`, 400 without `rejected`, 409 `CONFLICT` (detail kept), 404, 403, 401, 500, 503, 429, `IOException`
- [X] T034 [P] [US2] Create `GALT/data/upload/GalleryUploadQueueStoreTest.kt` (in-memory `SnapshotCache`, temp dir): append persists and emits; `markPrepared` keeps `uploadId`; `markFailed` keeps the file; `dismiss` removes item and file; `failAllOfAlbum(albumId, reason)` fails only that album's pending items; `failAllPending(reason)`; `clear` empties the list and snapshot; reload from snapshot restores the same items; unknown `state` dropped with its file
- [X] T035 [P] [US2] Create `GALT/data/upload/GalleryUploadRunTest.kt` (fake API, fake preparer, real queue store, fake repository recording `applyLocal`): three items sent in `enqueuedAt` order, each request carrying its own `client_upload_id`, `album_id`, one `image`; a `Retry` outcome ends the run with `RunResult.Retry` and the item stays `Prepared` with the same id, and the next run sends the same id; a sent item calls `applyLocal(UpsertPhoto)` and its prepared file ends at `gallery/photos/{photoId}.jpg` with the upload file gone; dedup 201 applied the same way; 404 fails every pending item of that album with "O álbum foi apagado" and continues with other albums; 403 fails every remaining item with the server detail; 401 stops without failing items; session ended before a request → nothing sent, nothing written; `syncAfterWrite` called once at the end of a run that sent at least one photo; items added during the run are sent in the same run
- [X] T036 [P] [US2] Extend `GALT/domain/usecase/GalleryAutoDownloadUseCaseTest.kt`: `clearOnLogout` cancels the upload scheduler and clears the upload repository (in addition to the existing assertions)
- [X] T037 [P] [US2] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: `albumState(id).uploads` shows `pending`/`total` for that album only, `isCopying` while `addPhotos` runs, the failed list with reasons; `dismissUpload(id)` calls the use case

### Implementation for User Story 2

- [X] T038 [P] [US2] Create `UploadItem`, `UploadState` (`Waiting`, `Prepared`, `Failed`) and `UploadOutcome` (`Sent`, `Failed`, `AlbumGone`, `AccessLost`, `Stop`, `Retry`) in `GAL/domain/upload/UploadModels.kt`, and the `GalleryUploadRepository` (`items`, `enqueue(albumId, sources: List<String>)`, `dismiss`, `clear`) and `GalleryUploadScheduler` (`enqueue`, `cancel`) interfaces in `GAL/domain/upload/GalleryUploadRepository.kt` and `GAL/domain/upload/GalleryUploadScheduler.kt`
- [X] T039 [P] [US2] Create `UploadQueueSnapshot`/`UploadItemDto` (`@Serializable`, snake_case names of data-model "Snapshot") with mappers in `GAL/data/upload/UploadQueueSnapshot.kt` and provide `SnapshotCache<UploadQueueSnapshot>` with key `gallery_upload_queue` in `GAL/di/GallerySnapshotModule.kt`
- [X] T040 [US2] Add to `GAL/data/local/GalleryMediaStore.kt`: `uploadsDir`, `uploadFile(name)`, `saveUpload(name, input)` (atomic), `adoptOriginal(photoId, ext, source: File)` (rename into `photos/`, copy+delete fallback), `newTempFile(prefix)` in `cacheDir`; `clearAll` already covers `uploads/`
- [X] T041 [US2] Create `GalleryUploadQueueStore` (`@Singleton`, own `Mutex`, `items: StateFlow<List<UploadItem>>`, `load`, `append`, `markPrepared`, `markFailed`, `failAllOfAlbum`, `failAllPending`, `remove`, `dismiss`, `clear`, `nextPending()`) in `GAL/data/upload/GalleryUploadQueueStore.kt` (R6; make T034 pass)
- [X] T042 [P] [US2] Create `UploadPreparationPlanner` (pure; constants `MAX_LONG_SIDE`, `MAX_BYTES`, `MAX_PIXELS`, `QUALITY_LADDER`; `plan(width, height, exifOrientation, mimeType, byteSize)`) in `GAL/data/upload/UploadPreparationPlanner.kt` (R8; make T031 pass)
- [X] T043 [P] [US2] Create the `ImageCodec` interface (`readInfo(file): SourceInfo?` with width, height, orientation, mime, size, EXIF date tags; `encodeJpeg(source, plan, quality, output): Boolean`; `writeExif(output, tags)`) in `GAL/data/upload/ImageCodec.kt`
- [X] T044 [US2] Create `AndroidImageCodec` in `GAL/data/upload/AndroidImageCodec.kt`: bounds via `BitmapFactory.Options.inJustDecodeBounds`, `ExifInterface` for orientation and date tags, decode with `inSampleSize`, scale to target with `Bitmap.createScaledBitmap(filter = true)`, rotate/flip with `Matrix`, draw on a white `Canvas` (transparency → white), `compress(JPEG, quality)`, recycle bitmaps; write `DateTimeOriginal`, `OffsetTimeOriginal`, `SubSecTimeOriginal`, `DateTimeDigitized`, `DateTime` and `Orientation = ORIENTATION_NORMAL` then `saveAttributes()` (R8)
- [X] T045 [US2] Create `GalleryImagePreparer` (`prepare(item): PrepareResult` = `Ready(file, displayName, mime)` | `Unreadable` | `TooLarge`) in `GAL/data/upload/GalleryImagePreparer.kt`: plan, encode over the quality ladder into `uploads/{uploadId}.jpg` (or keep the GIF), carry the EXIF date tags, delete the raw copy, set the display name extension to `.jpg`/`.gif` (make T032 pass)
- [X] T046 [P] [US2] Create `UploadOutcomeClassifier.classify(response or throwable)` in `GAL/data/upload/UploadOutcomeClassifier.kt` using `toAppError()` and `AppError.Server.extras["rejected"]` decoded with the project `Json` (make T033 pass)
- [X] T047 [US2] Create `PickedImageCopier` in `GAL/data/upload/PickedImageCopier.kt`: for each source URI string opens `contentResolver.openInputStream`, reads `OpenableColumns.DISPLAY_NAME` (fallback `foto_yyyyMMdd_HHmmss.jpg`) and the MIME type, generates `UUID.randomUUID().toString()` once, saves `uploads/{uploadId}.{ext}` via `GalleryMediaStore.saveUpload`, appends a `Waiting` item; an unopenable URI appends a `Failed("Não foi possível ler esta imagem.")` item (R7)
- [X] T048 [US2] Create `GalleryUploadRun` in `GAL/data/upload/GalleryUploadRun.kt` (the loop of R9 step 2–4, independent of WorkManager): `suspend fun run(onProgress: suspend (sent: Int, total: Int) -> Unit): RunResult` (`Done(failedInRun)`, `Retry`, `Stopped`); session check before each request and before each write; `Sent` → `repository.applyLocal(UpsertPhoto) { mediaStore.adoptOriginal(...) }` then `queue.remove`; `AlbumGone` → `failAllOfAlbum`; `AccessLost` → `failAllPending`; one `syncGallery.afterWrite()` at the end when anything was sent (make T035 pass)
- [X] T049 [US2] Create `GalleryUploadNotifications` in `GAL/data/work/GalleryUploadNotifications.kt`: channel `gallery_upload` (low importance, created lazily), progress notification "Enviando {x} de {n}" (ongoing, determinate), summary "{n} fotos não foram enviadas" with a `PendingIntent` to `CoreActivity`'s launcher intent, `ForegroundInfo` builder with `ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC` on API 29+; posting guarded by `NotificationManagerCompat.areNotificationsEnabled()`
- [X] T050 [US2] Create `GalleryUploadWorker` (`@HiltWorker`, `CoroutineWorker`) in `GAL/data/work/GalleryUploadWorker.kt`: try `setForeground(notifications.foregroundInfo(0, 0))` catching `IllegalStateException`/`ForegroundServiceStartNotAllowedException`; `run` with progress → `setProgress` + notification update; map `RunResult` to `Result.success()`/`Result.retry()`; summary notification when `failedInRun > 0`
- [X] T051 [US2] Create `WorkManagerGalleryUploadScheduler` in `GAL/data/work/WorkManagerGalleryUploadScheduler.kt`: unique work `gallery_upload`, `ExistingWorkPolicy.APPEND_OR_REPLACE`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL` 30 s; `cancel()` = `cancelUniqueWork`; add `GALT/data/work/WorkManagerGalleryUploadSchedulerTest.kt` mirroring the existing scheduler tests
- [X] T052 [US2] Create `GalleryUploadRepositoryImpl` in `GAL/data/upload/GalleryUploadRepositoryImpl.kt` (`items` from the store, `enqueue` = copier then `scheduler.enqueue()`, `dismiss`, `clear` = store clear) and bind `GalleryUploadRepository`, `GalleryUploadScheduler`, `ImageCodec` → `AndroidImageCodec` in `GAL/di/GalleryModule.kt`; load the queue in the gallery `Preloadable`
- [X] T053 [P] [US2] Create `EnqueueUploadsUseCase` and `DismissUploadUseCase` in `GAL/domain/upload/UploadUseCases.kt`
- [X] T054 [US2] Extend `GAL/domain/usecase/GalleryAutoDownloadUseCase.kt`: inject `GalleryUploadScheduler` and `GalleryUploadRepository`; `clearOnLogout` cancels the upload work first, then clears the queue, then the existing clears (R16; make T036 pass)
- [X] T055 [US2] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `addPhotos(albumId, uris: List<String>)` (sets `isCopying`, calls `EnqueueUploadsUseCase` in `viewModelScope`), `dismissUpload(uploadId)`, and combine the queue into `AlbumUiState.uploads` (`AlbumUploadsUiState` in `GAL/presentation/state/GalleryUiState.kt`) filtered by album (make T037 pass)
- [X] T056 [US2] Create `UploadStrip` ("Preparando fotos…" / "Enviando {x} de {n}" with a `LinearProgressIndicator`) and `FailedUploadsList` (name, reason, dismiss `IconButton`) in `GAL/presentation/components/GalleryManageComponents.kt` with `@Preview`s using fake data
- [X] T057 [US2] In `GAL/presentation/screens/AlbumScreen.kt`: `rememberLauncherForActivityResult(PickMultipleVisualMedia())` with `PickVisualMediaRequest(ImageOnly)`, FAB menu item "Adicionar fotos" (manage) launching it and passing `uris.map { it.toString() }` to `viewModel.addPhotos`; show `UploadStrip` and `FailedUploadsList` above the grid when the album has queue items

**Checkpoint**: photos can be added to existing albums from the app; logout clears the queue.

---

## Phase 5: User Story 3 - Create and edit albums (Priority: P1)

**Goal**: "Novo álbum" on the root and inside an album; "Editar álbum" with name, description and event date.

**Independent Test**: Quickstart scenario 2.

### Tests for User Story 3 ⚠️

- [X] T058 [P] [US3] Create `GALT/domain/manage/GalleryNamesTest.kt`: "  Retiro  " → `Valid("Retiro")`; "" and "   " → `Empty`; 100 chars → valid; 101 → `TooLong`
- [X] T059 [P] [US3] Create `GALT/data/manage/GalleryManageRepositoryImplTest.kt` (fake API, fake `GalleryRepository` recording `applyLocal`/`syncAfterWrite`): `createAlbum` 201 → `UpsertAlbum` with the returned album and one `afterWrite`; `editAlbum` 200 → `UpsertAlbum`; `IOException` → failure `AppError.Network` with `userMessage` "Sem conexão" and no `applyLocal`; 400 duplicate → failure whose `toGalleryWriteError()` is `DuplicateName`; 403 → no `applyLocal`
- [X] T060 [P] [US3] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: `openCreateAlbum(parentId)` opens an `AlbumForm` in Create mode; `saveAlbumForm` with an invalid name sets `nameError` and sends nothing; a duplicate-name failure keeps the form open with the server detail in `nameError`; success closes the form and posts `AlbumCreated`/`Saved`; `isSaving` true during the call and a second save while saving is ignored (FR-008)

### Implementation for User Story 3

- [X] T061 [P] [US3] Create `GalleryNames.validate(raw): NameCheck` (`Valid(trimmed)`, `Empty`, `TooLong`; max 100) in `GAL/domain/manage/GalleryNames.kt`
- [X] T062 [US3] Implement `createAlbum` and `editAlbum` in `GAL/data/manage/GalleryManageRepositoryImpl.kt` (set `userMessage = "Sem conexão"` on `AppError.Network`)
- [X] T063 [P] [US3] Create `CreateAlbumUseCase` and `EditAlbumUseCase` in `GAL/domain/manage/AlbumUseCases.kt` (validate name first; trimmed name sent)
- [X] T064 [US3] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `openCreateAlbum(parentId: Long?)`, `openEditAlbum(albumId)` (prefilled from the index), `updateAlbumForm(...)`, `saveAlbumForm()` with the error handling of T060
- [X] T065 [US3] Create `AlbumFormDialog` (name, description multi-line, event date field opening a Material 3 `DatePickerDialog`, "Limpar data", "Salvar" disabled while invalid or saving, `nameError` under the field) with `@Preview` in `GAL/presentation/components/GalleryManageComponents.kt`, formatting dates `dd/MM/yyyy` and sending `yyyy-MM-dd`
- [X] T066 [US3] Wire "Novo álbum" (root FAB; album FAB menu) and "Editar álbum" (album overflow) in `GAL/presentation/screens/GalleryScreen.kt` and `GAL/presentation/screens/AlbumScreen.kt`; render `GalleryDialogState.AlbumForm` from `viewModel.dialog` in each screen

**Checkpoint**: the gallery can be built from the app (albums + photos) — MVP complete with US1–US3.

---

## Phase 6: User Story 4 - Delete albums and photos (Priority: P2)

**Goal**: Owners delete an album with its subtree or photos (viewer and selection), with confirmations stating what
goes and that it stays 30 days in the trash.

**Independent Test**: Quickstart scenario 10.

### Tests for User Story 4 ⚠️

- [X] T067 [P] [US4] Create `GALT/domain/model/GalleryTreeManageTest.kt` (counts part): `subtreeCounts` counts sub-albums and photos at every depth, excludes the album itself, is `(0, 0)` for an empty album and for an unknown id
- [X] T068 [P] [US4] Create `GALT/domain/manage/DeletePhotosUseCaseTest.kt` (fake repository): every photo attempted in order; a 404 counts as success and applies `RemovePhotos`; a mixed batch returns `BatchResult(ok, failures grouped by message)`; a 403 stops and counts the rest as failed with the server detail; one `afterWrite` at the end
- [X] T069 [P] [US4] Add to `GALT/data/manage/GalleryManageRepositoryImplTest.kt`: `deleteAlbum` 204 and 404 both apply `RemoveAlbumTree`; `deletePhoto` 204 and 404 both apply `RemovePhotos`
- [X] T070 [P] [US4] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: confirmation text for albums (all four forms of contract §4, singular/plural) and photos (1 / N); after a confirmed album delete the album state is removed and the message is `AlbumTrashed`, not `AlbumRemoved`; after a viewer delete the viewer moves to the next photo with `PhotoTrashed`; long press starts selection, taps toggle, back clears it; a selected photo removed by a sync leaves the selection

### Implementation for User Story 4

- [X] T071 [P] [US4] Add `SubtreeCounts` and `fun subtreeCounts(albumId): SubtreeCounts` to `GAL/domain/model/GalleryTree.kt` (R11)
- [X] T072 [US4] Implement `deleteAlbum` and `deletePhoto` in `GAL/data/manage/GalleryManageRepositoryImpl.kt` (404 = done; `deletePhoto` with a `syncAfter: Boolean` flag so batches sync once)
- [X] T073 [P] [US4] Create `DeleteAlbumUseCase` and `DeletePhotosUseCase` (R13) in `GAL/domain/manage/DeleteUseCases.kt`
- [X] T074 [US4] Create `PhotoSelection` state holder (selected ids per album, `start`, `toggle`, `clear`, prune on index change) in `GAL/presentation/viewmodel/PhotoSelection.kt`; expose `selection` in `AlbumUiState`
- [X] T075 [US4] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `askDeleteAlbum(albumId)`, `askDeletePhotos(albumId, ids)`, `confirm()` (runs the pending action, records `selfRemovedIds`, posts `AlbumTrashed` / `PhotoTrashed` / `Batch(text)`), selection actions `onPhotoLongPress`, `onPhotoTap` (toggle in selection mode, open viewer otherwise), `clearSelection`
- [X] T076 [US4] Create `ConfirmDialog` and `SelectionBar` (count, "Mover" for manage, "Apagar" for owner, close) with `@Preview`s in `GAL/presentation/components/GalleryManageComponents.kt`
- [X] T077 [US4] Wire in `GAL/presentation/screens/AlbumScreen.kt`: `combinedClickable` long press on photo tiles (manage), selected-tile overlay, `SelectionBar` replacing the top bar in selection mode, `BackHandler` clearing it, overflow "Apagar álbum" (owner) → confirmation, navigate up after `AlbumTrashed`; in `GAL/presentation/screens/PhotoScreen.kt` overflow "Apagar" (owner) → confirmation

**Checkpoint**: deletes work from the album, the selection and the viewer.

---

## Phase 7: User Story 5 - Move albums and photos (Priority: P2)

**Goal**: Move an album (tree picker without itself and its descendants, with "Raiz") and move photos (selection or
viewer) to another album.

**Independent Test**: Quickstart scenario 8.

### Tests for User Story 5 ⚠️

- [X] T078 [P] [US5] Add to `GALT/domain/model/GalleryTreeManageTest.kt`: `moveTargetsForAlbum` starts with "Raiz" (`albumId = null`), lists every reachable album in pre-order with depth, excludes the album and all its descendants, marks the current parent (or "Raiz" for a root album) not selectable; `moveTargetsForPhotos` has no "Raiz", includes every album, current one not selectable
- [X] T079 [P] [US5] Create `GALT/domain/manage/MovePhotosUseCaseTest.kt`: sends one PATCH per photo with only `album_id`, in order; applies each returned photo; continues after failures; returns `BatchResult`; a 403 stops; one `afterWrite` at the end
- [X] T080 [P] [US5] Add to `GALT/data/manage/GalleryManageRepositoryImplTest.kt`: album move sends `parent_id` (explicit null for root); a cycle 400 maps to `Cycle` and triggers `syncAfterWrite`; a 404 maps to `NotFound` and triggers `syncAfterWrite`
- [X] T081 [P] [US5] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: the move picker for an album offers the targets of T078; choosing a target sends the move and closes the picker; moving the photo open in the viewer posts `PhotoMovedTo(name)` (not `PhotoMoved`); a batch posts "{n} fotos movidas para '{álbum}'" or the partial text

### Implementation for User Story 5

- [X] T082 [P] [US5] Add `TreeTarget`, `moveTargetsForAlbum`, `moveTargetsForPhotos` to `GAL/domain/model/GalleryTree.kt` (R11)
- [X] T083 [US5] Implement album move (via `editAlbum` with `parentId = Set(...)`) error follow-up (`syncAfterWrite` on `Cycle`/`NotFound`) and `editPhoto` with `syncAfter` flag in `GAL/data/manage/GalleryManageRepositoryImpl.kt`
- [X] T084 [P] [US5] Create `MovePhotosUseCase` (R13) in `GAL/domain/manage/MovePhotosUseCase.kt`
- [X] T085 [US5] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `openMoveAlbum(albumId)`, `openMovePhotos(albumId, ids)`, `chooseMoveTarget(target)` with messages and `selfRemovedIds` for photos leaving the open album
- [X] T086 [US5] Create `MoveTargetSheet` (`ModalBottomSheet`, rows indented by depth, disabled rows greyed, "Raiz" row) with `@Preview` in `GAL/presentation/components/GalleryManageComponents.kt`
- [X] T087 [US5] Wire "Mover álbum" (album overflow), "Mover" (selection bar and viewer overflow) in `GAL/presentation/screens/AlbumScreen.kt` and `GAL/presentation/screens/PhotoScreen.kt`; render `GalleryDialogState.MovePicker`

**Checkpoint**: albums and photos can be moved without re-downloading anything.

---

## Phase 8: User Story 6 - Organize the order (Priority: P2)

**Goal**: "Organizar" on the root and in an album, drag within groups, "Salvar" sends only changed groups, mismatch →
sync + message with the mode kept open.

**Independent Test**: Quickstart scenario 7.

### Tests for User Story 6 ⚠️

- [X] T088 [P] [US6] Create `GALT/domain/manage/ReorderUseCaseTest.kt`: nothing changed → no request; only photos changed → one `reorderPhotos`; both → albums then photos; root → `reorderAlbums(null, ids)`; mismatch → `afterWrite` called and `ReorderResult.OrderChanged`; other failure → `ReorderResult.Failed(error)`
- [X] T089 [P] [US6] Add to `GALT/data/manage/GalleryManageRepositoryImplTest.kt`: `reorderAlbums`/`reorderPhotos` 204 apply `ReorderAlbums`/`ReorderPhotos` with the sent ids
- [X] T090 [P] [US6] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: entering organize closes selection and vice versa; `moveItem` across groups is ignored; `cancelOrganize` restores the entry order; save success closes the mode with `Saved`; mismatch keeps the mode, rebuilds the draft from the refreshed tree and posts `OrderChanged`

### Implementation for User Story 6

- [X] T091 [US6] Implement `reorderAlbums` and `reorderPhotos` in `GAL/data/manage/GalleryManageRepositoryImpl.kt`
- [X] T092 [P] [US6] Create `ReorderUseCase` (`ReorderResult`: `Saved`, `Unchanged`, `OrderChanged`, `Failed`) in `GAL/domain/manage/ReorderUseCase.kt`
- [X] T093 [US6] Create `OrganizeSession` (entry ids and draft per group, `move(fromKey, toKey)` rejecting cross-group moves, `changedGroups()`, `rebuild(tree)`) in `GAL/presentation/viewmodel/OrganizeSession.kt`; expose `OrganizeUiState` in `GalleryRootUiState` and `AlbumUiState`
- [X] T094 [US6] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `startOrganize(albumId?)`, `moveItem(fromKey, toKey)`, `saveOrganize()`, `cancelOrganize()`
- [X] T095 [US6] Organize mode UI with `rememberReorderableLazyGridState` + `ReorderableItem` + `Modifier.draggableHandle()`/long-press drag in `GAL/presentation/screens/GalleryScreen.kt` (root albums) and `GAL/presentation/screens/AlbumScreen.kt` (keys `album-{id}` / `photo-{id}`, header item not draggable); top bar shows "Cancelar" / "Salvar" and hides every other control; `BackHandler` = cancel (fallback of T002 if the library was dropped)

**Checkpoint**: the church can set the order of everything from the app.

---

## Phase 9: User Story 7 - Set and remove covers (Priority: P3)

**Goal**: "Trocar capa" from a picked image, "Usar como capa" in the viewer, "Remover capa" for owners on an own cover.

**Independent Test**: Quickstart scenario 9.

### Tests for User Story 7 ⚠️

- [X] T096 [P] [US7] Add to `GALT/data/manage/GalleryManageRepositoryImplTest.kt`: `setCover` sends one multipart `image` and applies `UpsertAlbum` with the returned album; a 400 keeps the index; `removeCover` 204 applies nothing and calls `syncAfterWrite`; `originalForCover` returns the local original when present and downloads to a temp file otherwise (fake `downloadFile`), failing with "Sem conexão" on `IOException`
- [X] T097 [P] [US7] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: "Remover capa" confirmation text; `CoverUpdated` / `CoverRemoved` messages; a failure posts `Failure(detail)`

### Implementation for User Story 7

- [X] T098 [US7] Implement `setCover`, `removeCover`, `originalForCover` in `GAL/data/manage/GalleryManageRepositoryImpl.kt` (temp files via `GalleryMediaStore.newTempFile`, deleted after the request)
- [X] T099 [P] [US7] Create `SetCoverUseCase` (from a picked URI string: copy + `GalleryImagePreparer`; from a photo: `originalForCover`) and `RemoveCoverUseCase` in `GAL/domain/manage/CoverUseCases.kt`; the picked-image path goes through a `GalleryUploadRepository.prepareSingle(source)` data-layer helper so the domain never sees `Uri`
- [X] T100 [US7] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `setCoverFromPicked(albumId, uri: String)`, `useAsCover(albumId, photoId)`, `askRemoveCover(albumId)`
- [X] T101 [US7] Wire "Trocar capa" (album overflow, `PickVisualMedia` single image) and "Remover capa" (owner, only when `canRemoveCover`) in `GAL/presentation/screens/AlbumScreen.kt`, and "Usar como capa" (viewer overflow) in `GAL/presentation/screens/PhotoScreen.kt`

**Checkpoint**: covers are managed from the app.

---

## Phase 10: User Story 8 - Edit a photo (Priority: P3)

**Goal**: "Editar foto" in the viewer: name, description, date taken.

**Independent Test**: In the viewer rename a photo, set a description, clear the date: the top bar changes at once,
the second device after a sync.

### Tests for User Story 8 ⚠️

- [X] T102 [P] [US8] Add to `GALT/data/manage/GalleryManageRepositoryImplTest.kt`: `editPhoto` sends only changed keys (`date_taken: null` when cleared) and applies `UpsertPhoto`; a 404 triggers `syncAfterWrite` and applies nothing
- [X] T103 [P] [US8] Add to `GALT/presentation/viewmodel/GalleryViewModelManageTest.kt`: `openEditPhoto` prefills name (with extension), description, date; invalid name blocks save; success updates the viewer title and posts `Saved`

### Implementation for User Story 8

- [X] T104 [P] [US8] Create `EditPhotoUseCase` in `GAL/domain/manage/EditPhotoUseCase.kt` (validates the name with `GalleryNames`)
- [X] T105 [US8] In `GAL/presentation/viewmodel/GalleryViewModel.kt` add `openEditPhoto(photoId)`, `updatePhotoForm(...)`, `savePhotoForm()`
- [X] T106 [US8] Create `PhotoFormDialog` (name, description, date taken with `DatePickerDialog` and "Limpar data") with `@Preview` in `GAL/presentation/components/GalleryManageComponents.kt` and wire "Editar foto" in the viewer overflow of `GAL/presentation/screens/PhotoScreen.kt`

**Checkpoint**: every management action of the spec is available.

---

## Phase 11: Polish & Cross-Cutting Concerns

- [X] T107 Update `specs/gallery/spec.md` to the final state (FR-038): remove "somente leitura"/"ainda não existem: gestão"; add controls per level (contract §3), writes and local apply (R1, R2), upload queue, preparation and outcomes (R6–R9), covers, organize, messages (contract §4), logout clearing the queue; admin card no longer "continua desabilitado"
- [X] T108 [P] Update `specs/admin/spec.md`: "Galeria" row from "Sem implementação" to enabled, opening the gallery graph, with the level rule unchanged
- [X] T109 [P] Check `GAL/presentation/screens/*.kt` and `GAL/presentation/components/GalleryManageComponents.kt` for the 120-char limit, no magic strings (texts as constants), Screen/Content split kept, previews present on the new shared components
- [X] T110 Run `./gradlew :app:testDebugUnitTest` (full suite, no `clean`) and fix failures, including the existing `GalleryViewModelTest` after the `GalleryMessage` change
- [ ] T111 Run the manual scenarios of `specs/008-gallery-management/quickstart.md` on a device against the `dev` backend and record anything that differs in `specs/008-gallery-management/tasks.md` as new tasks
- [ ] T112 Mark this feature's tasks done and commit spec + code together (Conventional Commits, e.g. `feat(gallery): manage albums and photos from the app`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none. T002 decides the R10 fallback before US6.
- **Foundational (Phase 2)**: after Setup; blocks every story.
- **US1 (Phase 3)**: after Foundational; every other story's UI uses its `GalleryPermissions` (T026–T029).
- **US2, US3 (Phases 4–5)**: after US1; independent of each other.
- **US4, US5, US6 (Phases 6–8)**: after US1. US5 reuses the selection mode created in US4 (T074–T077) — do US4 first,
  or move T074 into US5 if US5 goes first.
- **US7 (Phase 9)**: after US2 (reuses the copier and preparer) and US1.
- **US8 (Phase 10)**: after US1; uses `editPhoto` from T083 (US5) — implement T083's `editPhoto` part here if US5 is
  not done yet.
- **Polish (Phase 11)**: after the stories being shipped.

### Within Each User Story

- Tests first, failing; then domain (pure) → data → ViewModel → components → screens.
- Tasks touching `GalleryViewModel.kt`, `GalleryManageRepositoryImpl.kt`, `GalleryManageComponents.kt` or the same
  screen are sequential across stories.

### Parallel Opportunities

- Phase 2: T004–T009 together; T012, T015, T016, T017, T022, T023 together after T010–T011.
- US2: T031–T037 together; T038, T039, T042, T043, T046 together; T053 alongside T047–T052.
- US4/US5/US6 tests (T067–T070, T078–T081, T088–T090) can be written in parallel once Phase 3 is done.

---

## Parallel Example: User Story 2

```text
# Tests (all [P]):
T031 UploadPreparationPlannerTest   T032 GalleryImagePreparerTest   T033 UploadOutcomeClassifierTest
T034 GalleryUploadQueueStoreTest    T035 GalleryUploadRunTest       T036 GalleryAutoDownloadUseCaseTest
T037 GalleryViewModelManageTest (uploads)

# Pure/independent pieces:
T038 UploadModels + interfaces   T039 UploadQueueSnapshot   T042 UploadPreparationPlanner
T043 ImageCodec                  T046 UploadOutcomeClassifier
```

## Parallel Example: User Story 4

```text
T067 GalleryTreeManageTest (counts)   T068 DeletePhotosUseCaseTest
T069 repository delete tests          T070 ViewModel delete/selection tests
T071 subtreeCounts                    T073 Delete use cases
```

---

## Implementation Strategy

### MVP First (US1 + US3 + US2)

1. Phase 1 → Phase 2.
2. US1: controls gated; admin card opens the gallery.
3. US3: albums created and edited.
4. US2: photos uploaded.
5. **Stop and validate** (quickstart 1–6): the gallery can be built entirely from the app.

### Incremental Delivery

1. MVP above → ship.
2. US4 (delete) → US5 (move) → US6 (organize) → ship.
3. US7 (covers) → US8 (edit photo) → Polish → ship.

Each story adds controls behind the same permission flags without changing the member view.

## Notes

- `[P]` = different files and no dependency on an incomplete task.
- Every write path must keep the rule: nothing changes locally when the server did not accept.
- Never `clean` before tests; commit spec and code together.
