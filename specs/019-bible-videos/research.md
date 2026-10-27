# Research: Bible Videos and Reading Progress

## R1 — Backend contract not implemented yet

- **Decision**: build against the planned contract in `../TODO/specify_bible_videos_backend.md`; the uncertain
  points are isolated in DTOs/endpoints and listed in `contracts/api.md`.
- **Rationale**: the user chose to proceed in parallel with the backend and adjust later.
- **Open points to re-check**: the shape of the `409 BIBLE_VIDEO_EXISTS` body (assumed key `video` beside
  `error_code`/`detail`); place removal as query (`?book=&chapter=`); sync batch limit 500; `rejected[].reason`
  strings (only logged).

## R2 — Videos cache: `BaseSnapshotRepository`

- **Decision**: `BibleVideoRepositoryImpl : BaseSnapshotRepository<List<BibleVideoDto>, List<BibleVideo>>`, key
  `bible_videos`, authless fetcher with `If-None-Match`. Registered as `Preloadable` (cache in memory at boot, so
  the play icon decision needs no wait) and refreshed by `BibleVideosViewModel.init` (graph open) and after each
  write.
- **Alternatives**: plain in-memory fetch (no offline sheet) — rejected by the spec.

## R3 — Writes return the new state? Refresh instead

- **Decision**: after any successful write, `refresh()` the snapshot (ETag changed → full list). The POST response
  (the created video) is used only to open the places screen.
- **Rationale**: one source of truth; the list is small.

## R4 — 409 on add

- **Decision**: `AppError.Server(409, errorCode = "BIBLE_VIDEO_EXISTS")`; the existing video is decoded from
  `extras["video"]`. If absent or undecodable, the app refreshes the list and shows the message without opening the
  places screen.
- **Rationale**: `ResponseExt` is the only parser of error bodies and already exposes extra top-level fields.

## R5 — Progress queue: state items, collapse per target, remove what was sent

- **Decision**: `QueuedProgressChange(type, book?, chapter?, videoId?, done, changedAt)`; `upsert(changes)` replaces
  any item with the same target. After a sync, remove only items equal to the ones sent (target **and**
  `changedAt`), so a newer change made during the request survives.
- **Rationale**: the hymnal rule "remove every id sent" keyed by event id; here the key is the target, and a
  collapse could have replaced the sent item.
- **Cap**: 5 000 items, oldest dropped (all chapters of the Bible are 1 189).

## R6 — Merge for display

- **Decision**: `BibleProgress.applying(pending)` — pure: for each pending item, add or remove the target.
  Repository exposes `combine(server, queue)`.

## R7 — When to fetch progress

- **Decision**: `Refreshable` (boot network phase and post-login `refreshDataFromNetwork()`), `BibleViewModel` init
  (Bible opens), and after each successful sync. Skipped when `SessionPresenceProvider.isLoggedIn()` is false.
- **Auth error on fetch**: keep the cached state (the session layer handles logout).

## R8 — Logout

- **Decision**: `SessionScopedCache` for progress: if logged in, try one `syncOnce()` under a 5 s timeout, then clear
  queue and cache and reset in-memory state. `CoreViewModel.endSession` clears session caches before
  `logoutUseCase()`, so the token is still there. When the session drops by refresh failure the flush just fails.

## R9 — Signed-in state in the Bible

- **Decision**: `bibleGraph(navController, isLoggedIn: StateFlow<Boolean>)`, as `galleryGraph`. Screens pass the
  value to their VMs (`setSignedIn`).

## R10 — Change time

- **Decision**: `OffsetDateTime.now().toString()` (ISO-8601 with offset) through an injectable `Clock` provider.
  Check whether a `Clock` binding exists; otherwise inject `() -> OffsetDateTime` default.

## R11 — Whole-book `null`

- **Decision**: write bodies via `buildJsonObject`, `put("chapter", JsonNull)` for whole-book places, because the
  global `Json` has `explicitNulls = false`. The sync change DTO omits absent fields on purpose.

## R12 — Reorder

- **Decision**: "Reordenar" switches the sheet into a mode that lists only that section with drag handles
  (`rememberReorderableLazyListState`, `draggableHandle`); the local order changes while dragging and the order is
  saved (`PUT order`) when the drag stops. "Concluir" leaves the mode. Failure: snackbar + refresh.

## R13 — Opening the link

- **Decision**: `Intent(ACTION_VIEW, Uri.parse(url))` from the screen; Android routes to the YouTube app when
  installed. `ActivityNotFoundException` → toast.
