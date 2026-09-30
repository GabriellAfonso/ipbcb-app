# Research: Gallery Management

Decisions taken while planning `specs/008-gallery-management/spec.md`. Each entry: decision, rationale,
alternatives considered. R-numbers are referenced from `plan.md`, `data-model.md` and `contracts/`.

---

## R1 — Local apply goes through the syncer's lock

**Decision**: `GallerySyncer` gains `applyLocal(change: GalleryLocalChange)`, which takes the same `Mutex` as a sync
(waiting for it, not skipping), applies the change to the in-memory index, saves the snapshot **with the cursor
unchanged**, reconciles the disk (so an album delete drops its subtree's originals and covers), and publishes the new
`GalleryLocalState`. `GalleryLocalChange` is a pure domain type applied by `GalleryIndex.apply(change)`:

| Change | Effect on the index |
|--------|--------------------|
| `UpsertAlbum(album)` | `albums[id] = album` |
| `UpsertPhoto(photo)` | `photos[id] = photo` |
| `RemoveAlbumTree(albumId)` | removes the album, every descendant, and every photo in any of them |
| `RemovePhotos(ids)` | removes the photos |
| `ReorderAlbums(parentId, ids)` | `position = index in ids` for each listed sibling |
| `ReorderPhotos(albumId, ids)` | `position = index in ids` for each listed photo |

`applyDelta` stays as is; both are idempotent by id, so the feed repeating a write changes nothing (FR-007).

**Rationale**: the sync already holds the lock from "read current index" to "save"; a write applied outside it could
be overwritten by a sync that read the index before the write. Waiting on the lock orders the two. Keeping the cursor
means the next feed still includes the write and its derived changes (ancestor covers, `album_name`, positions of
others), which only the server can compute.

**Alternatives**: advancing the cursor (would lose derived changes); applying nothing and just syncing (a visible delay
of a full round trip after every save, and nothing at all when the sync fails).

**Known limit**: a sync that answers `full_sync_required` with a read taken just before a write lands replaces the
index without it; the following sync brings it back. Rare and self-healing.

---

## R2 — A sync after a write is never skipped

**Decision**: `GallerySyncer.sync()` keeps "skip when busy" for triggers (app foreground, gallery open, periodic), and
gains `syncAfterWrite()`: when a sync is already running it marks a **re-run** and returns; the running sync, on
finishing, runs once more. Several writes in a row collapse into one extra sync. `SyncGalleryUseCase` gets the matching
`afterWrite()` entry (still queues missing originals).

**Rationale**: a sync that started before the write fetched the feed before the server changed; skipping would leave
derived changes (ancestor covers, album names) until the next trigger, possibly hours.

**Alternatives**: `mutex.lock()` and wait (a batch of 20 moves would queue 20 syncs); a delay/debounce (arbitrary timing,
harder to test).

---

## R3 — Where management code lives

**Decision**: everything stays in `features/gallery`, split as the project does:

- `data/api/GalleryApi.kt` gains the write endpoints (same `@AuthedRetrofit`; one API per feature, as today).
- `data/manage/GalleryManageRepositoryImpl.kt` — every write except the upload queue: calls the API, converts errors
  with `Response.toAppError()` / `Throwable.toAppError()`, and on success calls `GallerySyncer.applyLocal` then
  `syncAfterWrite`.
- `data/upload/` — queue store, image preparer, uploader, worker (R6–R9).
- `domain/manage/` and `domain/upload/` — repository interfaces, pure models, use cases.
- `presentation/` — the existing three screens gain management UI; `GalleryViewModel` delegates management state to
  small state holders (`AlbumEditor`, `OrganizeSession`, `PhotoSelection`) owned by it, so the ViewModel file does not
  become the whole feature.

**Rationale**: CLAUDE.md "features never import each other"; the spec says "inside the existing gallery screens, no
separate admin area". The admin panel only needs `AppRoutes.GALLERY_GRAPH`, which is core.

**Alternatives**: a `features/admin/gallery` area (would import the gallery feature or duplicate it).

---

## R4 — Access gating

**Decision**: `GalleryViewModel` observes `ObserveAccessUseCase` (core) and maps it once to
`GalleryPermissions(canManage = allows(GALLERY, MANAGE), canDelete = allows(GALLERY, OWNER))`, carried in every
UiState. Controls render from those two booleans only; `canRemoveCover` = `canDelete && album.coverSourceAlbumId ==
album.id`.

A 403 on a write needs **no explicit `RefreshAccessUseCase` call**: `core/network/PermissionDeniedInterceptor` already
watches every authenticated response and, on `PERMISSION_DENIED`, emits `AuthEventBus.Event.PermissionDenied`, which
`CoreViewModel` answers with `refreshAccess()`. The gallery write paths use the same `@Client`, including the upload
worker. The gallery only shows the server message (`AppError.Auth(403).userMessage`) and the controls disappear when
the access flow emits. A second call from the gallery would be a duplicate request (`AccessRepository.refresh` is
single-flight, so harmless, but pointless).

**Rationale**: one refresh path for the whole app, already tested.

**Alternatives**: calling `RefreshAccessUseCase` from each write (duplicate); reading roles (the spec says levels).

**Worker caveat**: a 403 inside the upload worker while no Activity exists emits on the bus with no `CoreViewModel`
listening; the profile is re-read on the next app open anyway (existing `refreshProfileOnAppOpen`).

---

## R5 — Write requests and PATCH bodies

**Decision**: create/edit bodies are built as `kotlinx.serialization.json.JsonObject` by pure mapper functions
(`AlbumDraft.toCreateBody()`, `AlbumEdit.toPatchBody()`, `PhotoEdit.toPatchBody()`), so a field is present only when
it changed and a cleared field is sent as an explicit JSON `null`.

**Rationale**: the project's `Json` has `explicitNulls = false` (`core/di/SerializationModule.kt`); a data-class body
would silently drop `"parent_id": null` (move to root) and `"event_date": null` (clear the date), turning them into
"no change".

**Alternatives**: a second `Json` instance with explicit nulls (a new DI binding for one feature); `Map<String, Any?>`
(needs Gson, which is on the classpath but not the converter used first).

Order bodies (`{parent_id, ids}` / `{ids}`) use `@Serializable` DTOs — `parent_id: null` for the root must also be
explicit, so the album order body is a `JsonObject` too.

---

## R6 — Upload queue storage

**Decision** (spec decision 5): a JSON snapshot `gallery_upload_queue` via `SnapshotCacheFactory` (same pattern as
`gallery_index`, Pitfall #3), holding `List<UploadItemDto>`, owned by a `@Singleton GalleryUploadQueueStore` with its
own `Mutex` and exposing `items: StateFlow<List<UploadItem>>`. Files live at `filesDir/gallery/uploads/{uploadId}.{ext}`
— inside the gallery root, so `GalleryMediaStore.clearAll()` (logout) removes them with everything else; the
reconcile of R1 only touches `photos/` and `covers/`.

The album screen reads progress and failures by filtering `items` by `albumId`; the notification reads the whole list.

**Rationale**: WorkManager input data is limited to 10 KB, immutable per request, and invisible to the screen; the
queue must also survive the worker being replaced (`APPEND_OR_REPLACE`).

**Alternatives**: WorkManager input data only (limits above); Room (new library, one table).

---

## R7 — Copying picked images

**Decision**: the picker result (`List<Uri>`) goes from the screen to `GalleryViewModel.addPhotos(albumId, uris)`,
which calls `EnqueueUploadsUseCase` in `viewModelScope`. The data layer (`PickedImageCopier`) opens each URI with
`ContentResolver`, copies it to `uploads/{uploadId}.{ext}` (atomic, as `GalleryMediaStore` writes), reads the display
name from `OpenableColumns.DISPLAY_NAME`, and appends one item per copy to the queue **as it is copied**, then enqueues
the worker once. The album shows "Preparando fotos…" while copying. A URI that cannot be opened becomes a failed item
("Não foi possível ler esta imagem.").

URIs are passed as `String` into the domain use case so `domain/` stays free of `android.net.Uri`.

**Rationale**: picker URIs keep their grant only while the app process lives; the worker may run after process death.
Copying is I/O only (no decode), about a second per 20 photos. The graph-scoped ViewModel lives while any gallery screen
is on the stack.

**Alternatives**: `takePersistableUriPermission` and copying in the worker (works for the Photo Picker but not for the
`ACTION_OPEN_DOCUMENT` fallback on old devices without the back-port); an application scope (CLAUDE.md forbids manual
scopes).

**Known limit**: leaving the gallery entirely while 50 photos are still being copied cancels the copy; the photos
already copied stay queued.

---

## R8 — Image preparation

**Decision** (spec decisions 3, 4, 6): `GalleryImagePreparer` in the data layer, split in two:

- `UploadPreparationPlanner` — **pure Kotlin**, fully unit-tested: given source width/height, EXIF orientation, MIME
  type and byte size, decides `SendAsIs` (GIF within 10 MB and 50 MP) or `Reencode(sampleSize, targetW, targetH,
  rotationDegrees, flip)`; exposes the JPEG quality ladder `90, 85, 80, 75, 70, 60` and the limits.
- `AndroidImageCodec` (behind `ImageCodec`, faked in tests) — reads bounds and EXIF with `androidx.exifinterface`,
  decodes with `BitmapFactory` using `inSampleSize` (so a 200 MP image never fully decodes), scales to the exact target,
  applies the rotation/flip with a `Matrix`, draws on a white canvas (transparency → white), encodes JPEG at each
  quality of the ladder until ≤ 10 MB, then writes EXIF on the output: `DateTimeOriginal`, `OffsetTimeOriginal`,
  `SubSecTimeOriginal`, `DateTimeDigitized` and `DateTime` copied from the source, `Orientation = NORMAL`, and nothing
  else (location and device tags are not carried over).

HEIF/HEIC decode through `BitmapFactory` on API 28+. On API 24–27 a HEIF source fails as unreadable ("Não foi possível
ler esta imagem.") — such phones do not produce HEIF.

The prepared file replaces the raw copy under the same upload id (`uploads/{uploadId}.jpg`); the item moves from
`Waiting` to `Prepared`, so a retry never prepares twice.

**Rationale**: rotating pixels makes the image upright for every reader, including the server's thumbnail and 1000×1000
cover crop. Separating the arithmetic from the Android codec keeps the rules testable in plain JVM tests
(`isReturnDefaultValues = true`, no Robolectric in the project).

**Alternatives**: keep the EXIF orientation tag (depends on each reader honouring it); `ImageDecoder` (API 28+ only and
auto-applies orientation inconsistently with the fallback path); adding Robolectric to test real bitmaps (new library;
covered instead by the manual checks in `quickstart.md`).

**Dependency**: `androidx.exifinterface:exifinterface:1.4.2` (AndroidX, no transitive Compose/Kotlin constraints).

---

## R9 — Upload worker

**Decision** (spec decisions 1, 2): `GalleryUploadWorker` (`@HiltWorker`, `CoroutineWorker`), unique work
`gallery_upload`, `ExistingWorkPolicy.APPEND_OR_REPLACE`, constraint `NetworkType.CONNECTED` (any network),
exponential backoff from 30 s. `doWork()`:

1. Try `setForeground(ForegroundInfo(id, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC))`; if the platform refuses
   (background start restrictions, Android 12+), continue as a plain worker.
2. Loop: take the oldest item in `Waiting`/`Prepared` (read fresh each time, so items added meanwhile are picked up);
   stop when none. Before every write, check the session (`SessionPresenceProvider`); signed out → stop, write nothing.
3. Prepare if needed, send one request (`album_id`, one `image`, `client_upload_id`), classify with
   `UploadOutcomeClassifier` (pure, tested):

   | Answer | Outcome |
   |--------|---------|
   | 201/207 with the photo in `accepted` (new or deduplicated) | `Sent(photo)` |
   | 207/400 with `rejected` | `Failed(reason from rejected[0].reason)` |
   | 409 `CONFLICT` | `Failed(detail)` |
   | 404 | `AlbumGone` — this and every queued item of the album fail with "O álbum foi apagado" |
   | 403 | `AccessLost(detail)` — this and every remaining item fail |
   | 401 | `Stop` — the session path decides; items stay queued (logout clears them) |
   | `IOException`, 5xx, 429 | `Retry` — the worker returns `Result.retry()`; the item stays with its upload id |
   | 400 without `rejected` (malformed request) | `Failed("Não foi possível enviar esta foto.")`, logged |

4. `Sent`: `GallerySyncer.applyLocal(UpsertPhoto)` then adopt the prepared file as `photos/{photoId}.{ext}` (move,
   under the syncer's lock, after the upsert so the reconcile keeps it), remove the item. One `syncAfterWrite()` at the
   end of the run, not per photo.
5. Progress: `setProgress` + notification "Enviando X de N" where N counts items sent in this run plus those still
   pending; at the end, when any item failed in the run, one summary notification "N fotos não foram enviadas" that
   opens the app.

Notification permission: `CoreActivity` already requests `POST_NOTIFICATIONS` at launch; nothing new is asked. When it
is denied, the foreground notification is hidden by the system but the work still runs, and progress stays visible in
the album. The spec's decision 2 is amended accordingly.

Manifest: `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_DATA_SYNC` permissions, and WorkManager's
`SystemForegroundService` merged with `android:foregroundServiceType="dataSync"`. Notification channel
`gallery_upload`, low importance, created by the worker.

**Rationale**: `APPEND_OR_REPLACE` guarantees a new batch is never lost when it arrives as the current run finishes
(`KEEP` would ignore it); reading the queue fresh each iteration serves items added mid-run. Foreground keeps long
queues alive with the screen off (SC-002).

**Alternatives**: one work request per photo (N notifications, no sequence guarantee without chaining); expedited work
(quota-limited, same foreground requirement below API 31).

---

## R10 — Organize (reorder)

**Decision**: `sh.calvin.reorderable:reorderable:3.1.0`. It works on `LazyVerticalGrid` (the album screen's existing
6-column grid, sub-albums span 3, photos span 2) with `rememberReorderableLazyGridState` and `ReorderableItem`. The
`onMove` callback rejects moves across groups (keys `album-{id}` vs `photo-{id}`). The draft lives in
`OrganizeSession` (presentation state holder): two lists of ids captured on entry; "Salvar" compares each with the
entry order and sends only changed ones (sub-albums first, then photos). A mismatch (400 with `missing`/`unexpected`/
`repeated`) triggers `syncAfterWrite`, rebuilds the draft from the refreshed tree, keeps the mode open and shows "A
ordem mudou enquanto você editava. Confira e salve de novo.".

**Compatibility**: 3.1.0 depends on JetBrains Compose Multiplatform `foundation`/`runtime`/`animation` 1.7.0 and Kotlin
stdlib 1.9.0; on Android those resolve to the AndroidX artifacts, which the project's Compose BOM 2026.02 (1.10.x)
upgrades — Gradle picks the higher version, and the library only uses stable lazy-layout APIs. Kotlin 2.3 reads 1.9
metadata. To be confirmed by the first build (`./gradlew :app:assembleDebug`) in the implementation's first task; the
fallback is `detectDragGesturesAfterLongPress` on the grid with manual index math (no library).

**Alternatives**: hand-written drag (more code, auto-scroll by hand); up/down arrow buttons (poor for 50 photos).

---

## R11 — Tree picker and delete counts

**Decision**: pure functions on `GalleryTree`:

- `moveTargetsForAlbum(albumId): List<TreeTarget>` — "Raiz" first, then a pre-order walk of every reachable album
  **except** `albumId` and its descendants, each with its depth; the current parent is `selectable = false`.
- `moveTargetsForPhotos(currentAlbumId): List<TreeTarget>` — every album in pre-order, the current one not selectable,
  no root.
- `subtreeCounts(albumId): SubtreeCounts(subAlbums, photos)` — all levels.

The picker is a `ModalBottomSheet` listing targets indented by depth; no new route.

**Rationale**: the cycle can never be picked (the server still refuses it; R14 covers a tree changed meanwhile).

---

## R12 — Forms, confirmations and messages

**Decision**:

- Forms are dialogs (`AlertDialog` with `OutlinedTextField`s and a Material 3 `DatePickerDialog`), held in the
  ViewModel as `AlbumFormState` / `PhotoFormState` (fields, validation, `isSaving`, `nameError`). Name validation is
  pure (`GalleryNames.validate`: trimmed, 1–100). A duplicate-name 400 puts the server `detail` in `nameError`.
- `GalleryMessage` changes from an `enum` to a `sealed interface` with `text`, adding the management messages
  (`Saved`, `AlbumTrashed`, `PhotoTrashed`, `PhotosMoved(count, albumName)`, `BatchResult(...)`, `OrderChanged`,
  `Failure(text)`), still a consumable `StateFlow` (007's documented exception).
- A write the ViewModel itself made that removes the open album or photo is recorded in `selfRemovedIds`, so the
  existing "removed while open" detection shows the management message instead of "Este álbum foi removido" / "Esta
  foto foi removida".
- Error text: `AppError.toUserMessage()` (constitution) — the server's Portuguese `detail` when structured; the generic
  network text becomes "Sem conexão" via `userMessage` set in `GalleryManageRepositoryImpl` for `AppError.Network`; the
  English order-mismatch detail is never shown (R10 text instead).

---

## R13 — Batch move and delete

**Decision** (spec decision 8): `MovePhotosUseCase` / `DeletePhotosUseCase` loop in sequence, apply each success
locally at once, continue after failures, return `BatchResult(succeeded, failuresByMessage)`; one `syncAfterWrite()` at
the end. A 404 on delete counts as done and removes the photo locally. A 403 stops the loop (every following photo
would get the same answer) and reports the rest as failed with the server message.

---

## R14 — Refused album writes

**Decision**: duplicate name → form stays open with the message under the name. Cycle (400 with `chain`) or 404 on
move/edit → picker/form closes, message shown, `syncAfterWrite()` (the tree changed under the user).

---

## R15 — Covers

**Decision**: "Trocar capa" uses `PickVisualMedia` (single image), copies and prepares the image with the same
preparer (R8), sends `PUT albums/{id}/cover/` and upserts the returned album. "Usar como capa" sends the local original
as is (already within the server's limits, since the server accepted it as a photo); when the original is not on the
device it is downloaded to a temp file in `cacheDir` with `GalleryApi.downloadFile(imageUrl)` first. "Remover capa":
`DELETE albums/{id}/cover/` returns no album, so nothing is applied locally; `syncAfterWrite()` brings the resolved
cover. The new cover file itself arrives through the sync's existing cover download (reconcile).

---

## R16 — Logout

**Decision**: `GalleryAutoDownloadUseCase.clearOnLogout()` also cancels the unique work `gallery_upload` (through a
`GalleryUploadScheduler` interface, like the other two schedulers) and clears the queue store (snapshot + in-memory
list). The files go with `GalleryMediaStore.clearAll()` (R6). The worker checks the session before every write, so a
run that outlives logout writes nothing.

---

## R17 — Admin card

**Decision**: `PanelCard.GALLERY` becomes enabled with `onClick = nav.gallery`, where `adminGraph` builds
`nav.gallery = { navController.navigate(AppRoutes.GALLERY_GRAPH) }`. `AppRoutes` is core, so the admin feature never
imports the gallery. Back from the gallery root returns to the panel.
