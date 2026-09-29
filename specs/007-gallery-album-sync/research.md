# Research: Gallery Album Tree and Sync

Decisions taken while planning `007-gallery-album-sync`. Each entry: decision, rationale, alternatives considered.

---

## R1. The index is one snapshot, cursor included

**Decision**: `SnapshotCache<GalleryIndexSnapshot>` from `SnapshotCacheFactory`, key `gallery_index`, provided by a
new `GallerySnapshotModule` (same shape as `HymnalSnapshotModule`). The snapshot holds `albums`, `photos` and `cursor`.
Saved once per sync, after the delta is applied.

**Rationale**: Spec resolved decision 1 — the cursor can never be ahead of the data. `LocalSnapshotCache.load()`
already deletes an undecodable file and returns `null`; with the cursor inside, a torn write simply means "no index,
no cursor", which is a full read on the next sync. Nothing is lost for good because originals and covers are matched by
id/URL, not by the index. At ~3,000 photos the JSON is well under 2 MB.

**Alternatives**: cursor in `GalleryPreferences` (DataStore) — two writes, a crash between them leaves a cursor ahead
of the data and silently skips changes. Room — a new library and schema for a file that is read whole anyway.

---

## R2. Disk layout: flat originals, covers by URL hash

**Decision**:

```
filesDir/gallery/photos/{photoId}.{ext}      originals (ext from image_url, as today)
filesDir/gallery/covers/{sha1(cover_url)}.jpg covers
cacheDir/gallery-*.part                        temp files (atomic write, unchanged)
cacheDir/gallery_thumbs/                       preview cache (R6)
```

A new `GalleryMediaStore` replaces `GalleryPhotoStorage`: `saveOriginal`, `originalFile(photoId)`,
`listOriginalIds()`, `deleteOriginal`, `saveCover(url, stream)`, `coverFile(url)`, `listCoverNames()`,
`deleteCover`, `clearAll()`.

**Rationale**: Moving a photo never touches its file (FR-003). Naming a cover by its URL hash makes a new URL a new
file (FR-005) and lets two albums that inherit the same cover share one file; a cover is deleted only when no album in
the index still points at its URL (spec edge case "two albums showing the same inherited cover").

**Alternatives**: `covers/{albumId}.jpg` — cannot tell a replaced cover from the old one without storing the URL, and
duplicates inherited covers.

---

## R3. Reconcile disk against the index after every sync

**Decision**: After each successful sync (delta or full), under the sync lock:

1. delete every original whose id is not a photo in the index;
2. delete every cover file whose name is not the hash of some album's `cover_url`;
3. download every `cover_url` in the index whose file is missing (any network; failure skipped, retried next sync).

**Rationale**: One invariant — *disk ⊆ index* — covers deletions, `full_sync_required`, migration orphans (FR-017) and
covers replaced or removed, with a single code path and a single set of tests. Listing ~3,000 file names is cheap.

**Alternatives**: delete only the ids in `deleted_*_ids` — misses orphans after a full sync and after the migration,
and needs a second path for each.

---

## R4. Migration is only the file move

**Decision**: `GalleryLegacyMigration.run()` — for every numeric folder `filesDir/gallery/{albumId}/`: move each
non-`.json` file into `photos/` (if the target exists, delete the source), delete the `.json` files, delete the folder.
Then set `gallery_layout_version = 2` in `GalleryPreferences`. Runs at the start of every sync until the marker is set
(so the periodic worker and the download worker also migrate). The old `gallery_auto_download_triggered` key is
removed.

**Rationale**: No index exists after the update, so the first sync is a full read, and R3 prunes the orphans. Pruning
therefore needs no migration-specific code. Every step is idempotent: a rerun after a kill finds fewer files and does
the same thing (FR-018). The files are moved by rename inside `filesDir` (same filesystem), never re-downloaded.

**Alternatives**: build a provisional index from the old `.json` files — rejected in clarification Q2.

---

## R5. Sync engine: one lock, skip when busy, cancellable by logout

**Decision**: `GallerySyncer` (`@Singleton`, data layer) with a `Mutex`:

- `sync(): GallerySyncResult` — `tryLock()`; if held, return `Skipped` (the running sync serves the caller, FR-012).
  Inside: migrate (R4), check session, load index, `GET gallery/changes/?since=cursor`, on
  `full_sync_required` read again without `since` and replace, else apply the delta; check session again; save
  snapshot; reconcile (R3); publish the new local state.
- The coroutine job of the running sync is kept; `clear()` cancels it, then takes the lock and deletes everything
  (FR-015, FR-027). Any write after cancellation is impossible because every write happens under the lock after a
  session check.

`GallerySyncResult`: `Synced(missingOriginals: Int)`, `Skipped`, `Failed(AppError)`. 401/403/other all map through
`Response.toAppError()` (constitution: one parsing point); the local copy is untouched on any failure (FR-014).

**Rationale**: No application-scoped `CoroutineScope` is needed, and callers (ViewModels, worker) stay in their own
scopes. Skipping instead of queuing is fine because the next trigger (foreground, gallery open, 6 h) comes soon.

**Alternatives**: single-flight `Deferred` in an app scope — adds a manual scope, which the project avoids. Running
every sync through WorkManager — foreground syncs would wait on the scheduler and the gallery-open sync could not report
its state directly.

---

## R6. Previews: a dedicated Coil loader on `@Client`, with a disk cache

**Decision**: `@GalleryThumbnailLoader ImageLoader` (Coil 2.6, already in the project) built like
`MemberPhotoLoaderModule`: `okHttpClient(@Client)`, `respectCacheHeaders(false)`, disk cache in
`cacheDir/gallery_thumbs` capped at 100 MB, default memory cache. Cleared (`diskCache.clear()`,
`memoryCache.clear()`) by the gallery's logout path. Loaded on any network (clarification Q3).

The loader reaches the screens through `GalleryViewModel.previewLoader` (the ViewModel injects it). The composable gets a `PhotoImage` model (`Original(file)` / `Preview(url)` / `None`) computed by the ViewModel; it
never decides which to use.

**Rationale**: The authenticated client adds the JWT and follows the refresh path. Unlike member photos (LGPD, memory
only), gallery previews are member content already stored in full on the device, so a disk cache is consistent with
the rest of the gallery and makes offline revisits work (US3 scenario 4).

**Alternatives**: download previews into `filesDir` during sync — up to 3,000 × ~150 KB on mobile data, contrary to the
"originals are WiFi-first" rule.

---

## R7. Two background jobs

**Decision**:

| Work | Unique name | Kind | Constraint | Policy |
|------|-------------|------|------------|--------|
| `GallerySyncWorker` | `gallery_sync_periodic` | periodic, 6 h | `CONNECTED` | `KEEP` (enqueued on login and on app start with session) |
| `GalleryDownloadWorker` | `gallery_auto_download` (unchanged) | one-time | `UNMETERED` / `CONNECTED` (mobile data button) | `KEEP` after a sync; buttons unchanged |

After `Synced(missingOriginals > 0)`, the caller (`SyncGalleryUseCase`) enqueues the download with `KEEP`. `KEEP` on a
finished (e.g. `FAILED`) unique work replaces it, so an old failure does not block new downloads.

The download worker no longer calls `GET api/photos/`: it reads the index; when there is no index yet (login on WiFi
before any sync finished) it runs `syncer.sync()` first, and a `Failed` there maps to the old "photo list failed" row
of the download rules (401/403 → failure with code, others → retry up to 3). Everything after that —
`GalleryPhotoDownloader`, `GalleryWorkDecision`, the 401/403/429 stop rules, progress — is unchanged, fed with the
index's photos in tree order.

**Rationale**: Spec resolved decision 2. The download rules (spec sections 3 and 4 of the app's gallery spec) stay
valid verbatim.

---

## R8. Foreground trigger without a new library

**Decision**: `AppNavHost` (where `CoreViewModel` is resolved in Activity scope) adds
`LifecycleEventEffect(Lifecycle.Event.ON_START) { coreViewModel.onAppForeground() }`
(`lifecycle-runtime-compose`, already present through `collectAsStateWithLifecycle`). `onAppForeground()` launches
`syncGallery()` when logged in. It replaces `galleryAutoDownload.triggerIfNeeded()` in `startAppInitialization()`;
`ON_START` also fires on the first start, which covers "app start".

**Rationale**: Single-Activity app: Activity `ON_START` = app visible. A config change also fires it; R5's skip rule
and the cheap delta make that harmless.

**Alternatives**: `ProcessLifecycleOwner` — needs `lifecycle-process`, a new dependency, for the same signal.

---

## R9. Routes and back stack

**Decision**: `GalleryMain`, `Album/{albumId}`, `Photo/{albumId}/{photoId}`. The album id in the photo route is the
album the viewer was opened from.

**Rationale**: The viewer pages "the photos of that album"; keeping that album in the route survives process death
and makes "photo moved to another album while open" detectable (photo still in the index, but `albumId` differs).
Each album navigation pushes an entry, so back goes up one level (FR-022).

**Removed items while open** (FR-024): the graph-scoped ViewModel posts a one-shot message
(`GalleryMessage.PhotoRemoved | PhotoMoved | AlbumRemoved`) held in a `StateFlow<GalleryMessage?>` and consumed by the
top screen. An album screen whose album left the index calls `back()`; the entry below, if also removed (a deleted
subtree), does the same when it becomes top, so the stack unwinds to the nearest existing ancestor or the root. The
message is shown once, by the screen that stays.

**Alternatives**: `popBackStack(route)` to a specific album — every album entry shares one destination id, so Navigation
cannot target an entry by argument.

---

## R10. One graph-scoped ViewModel, per-album state by function

**Decision**: `GalleryViewModel` (graph-scoped, as today) exposes `rootState: StateFlow<GalleryRootUiState>`,
`fun albumState(albumId): StateFlow<AlbumUiState>` and `fun viewerState(albumId, photoId): StateFlow<PhotoViewerUiState>`,
each memoized per key and derived from `repository.localState` (index + original files + cover files). Pure tree logic
(`GalleryTree`: roots, children, photos of an album, parent, ordering by `position` then `id`) lives in
`domain/model` and is tested without Android.

**Rationale**: Follows the user's request (graph-scoped ViewModel) and CLAUDE.md (Screen collects a `StateFlow`
UiState; content composable gets data and lambdas). Tree building is O(n) over ≤3,000 items and runs off the main
thread via `flowOn(Default)`.

**Alternatives**: one ViewModel per back stack entry — cleaner per-album lifetime but contradicts the requested
graph-scoped ViewModel and the project's Pitfall #1 convention.

---

## R11. `members` stored, not modeled for display

**Decision**: `GalleryPhotoDto.members: List<GalleryPhotoMemberDto> = emptyList()` (`id`, `name`), carried into the
snapshot and the domain `GalleryPhoto`. No UI reads it.

**Rationale**: Feature 4 (tags) needs it on the device; storing it now avoids a forced full resync later.
