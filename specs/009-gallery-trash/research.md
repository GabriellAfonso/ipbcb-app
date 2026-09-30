# Research: Gallery Trash

Decisions taken while planning spec 009. Each: decision, rationale, alternatives.

## R1 — Where the trash calls live

**Decision**: `GalleryManageRepository` gains `trash()`, `restoreAlbum(id)` and `restorePhoto(id)`, implemented in
`GalleryManageRepositoryImpl` with the existing `request` / `write` helpers. A restore is a write: the returned
resource is applied with `applyLocal(UpsertAlbum | UpsertPhoto)` (cursor untouched) and `syncAfterWrite` runs.

**Rationale**: The helpers already give "2xx body or `AppError`", the "Sem conexão" `userMessage`, local apply and
the never-skipped sync (008 R1/R2). A second repository would duplicate them. `SyncGalleryUseCase.afterWrite` already
queues missing originals on WiFi, which covers "a restored photo without its original joins the WiFi queue".

**Alternatives**: a separate `GalleryTrashRepository` (duplicated plumbing, second fake); rebuilding an album's
subtree locally (impossible: only the album comes back — the sync brings the rest).

## R2 — Classifying restore refusals

**Decision**: Domain `RestoreResult` sealed (`Restored`, `NotInTrash`, `TrashedParent(parentAlbumId)`,
`NameConflict(conflictingAlbumId, isOnDevice)`, `Failed(error)`), produced by `RestoreTrashItemUseCase` from the
`AppError` using new extras readers in `GalleryWriteErrorKinds.kt` (`trashedParentId()`, `conflictingAlbumId()`,
parsed from `AppError.Server.extras` strings to `Long`). `404` → `NotInTrash`. The presentation maps a result to
text (`TrashTexts`).

**Rationale**: Keeps the constitution's split (data converts to `AppError`, domain classifies, presentation words it)
exactly like 008's `GalleryWriteError`. Both the trash screen and "Desfazer" share one classification (FR-015).

**Alternatives**: mapping in each ViewModel (duplicated); reading the error body (only `ResponseExt` may).

## R3 — Conflicting album not on the device

**Decision**: `RestoreTrashItemUseCase`, on a name conflict, checks the local index; when the album is missing it
runs a sync (`SyncGalleryUseCase.afterWrite`) and checks again. `isOnDevice` decides whether "Abrir álbum" is shown.

**Rationale**: The album screen needs the album in the index; resolving it before showing the message avoids a
button that leads nowhere and keeps the composable dumb.

## R4 — Trash screen ViewModel

**Decision**: New `TrashViewModel` (`@HiltViewModel`), scoped to the trash route's own back-stack entry
(`hiltViewModel()` without the graph entry). One-shot messages via `SharedFlow<TrashEvent>`
(`Message(text, openAlbumId?)`, `Close(text)`), state via `StateFlow<TrashUiState>`.

**Rationale**: The list is online-only and must be read on every open (FR-004); a graph-scoped instance would keep a
stale list and grow `GalleryViewModel` further (683 lines). The entry stays on the back stack while "Abrir álbum" shows
the album, so coming back keeps the state. Pitfall #1 is about sharing a VM between screens of a graph; nothing is
shared here.

**Alternatives**: extend `GalleryViewModel` (bigger, stale list); a `StateFlow` message like the gallery's (not
needed: the trash screen is always the one collecting its own events).

## R5 — One restore at a time, refresh and highlight

**Decision**: `TrashUiState.restoring: TrashKey?`; `restore()` returns at once while it is set; pull-to-refresh is
disabled while it is set. `highlighted: TrashKey?` is set by `TrashedParent` when the parent album is a row
(`TrashKey(ALBUM, parentId)`), cleared on the next restore, reload or leaving. The screen scrolls to the highlighted
row with a `LaunchedEffect` keyed on it.

## R6 — Dates

**Decision**: `deleted_at` parsed with `OffsetDateTime.parse` (desugared `java.time`, already enabled) and shown in a
`ZoneId` passed to `TrashRowMapper` (`ZoneId.systemDefault()` in production, fixed in tests), format
`dd/MM/yyyy HH:mm`. `purge_on` (`yyyy-MM-dd`) reuses `GalleryUiMapper.formatDate`. Unparseable values are shown as they
came.

## R7 — Previews

**Decision**: `TrashViewModel` exposes the existing `@GalleryThumbnailLoader` `ImageLoader`, like `GalleryViewModel`
(`previewLoader`); the row uses `AsyncImage` with a grey placeholder/error. No full-size view (decision 3).

## R8 — Undo ("Desfazer") and snackbars

**Decision**:
- `GalleryMessage` gains `action: MessageAction?` (`Undo(kind, id)` "Desfazer", `OpenAlbum(albumId)` "Abrir álbum").
  `AlbumTrashed` / `PhotoTrashed` become data classes carrying the undo action, set only when the delete was of one
  item and the user has `owner` at that moment.
- A single photo deleted from the selection now also posts `PhotoTrashed` (previously only from the viewer).
- `GalleryViewModel.undo(action)` checks `permissions.canDelete` again and calls the shared restore use case; results
  are worded by `TrashTexts` (same texts as the trash, FR-015).
- `GalleryMessageEffect` shows a `Snackbar` (with the action label when present) instead of a `Toast`. The message is
  consumed before `showSnackbar` runs on a `rememberCoroutineScope` scope, so clearing the `StateFlow` does not cancel
  the snackbar. A screen that is leaving (`AlbumUiState.isRemoved`, `PhotoViewerUiState.isClosed`) does not consume it,
  so the screen below shows it.
- Each gallery screen wraps its content in `GalleryMessageHost`, a `Box` with a `SnackbarHost` overlay (the members
  screens' pattern); `BaseScreen` is not changed.

**Rationale**: The consume-once `StateFlow` survives the back-stack unwind (008 §1.4); only the rendering changes.
A multi-photo delete has no undo (decision 1).

**Alternatives**: a global snackbar host in `CoreScreen` (the gallery has no access to it and features must not import
each other); keeping Toast plus a separate undo banner (two message systems).

## R9 — Losing `owner` on the trash screen

**Decision**: `TrashViewModel` collects `ObserveAccessUseCase`; once it has seen `owner`, a later emission without it
emits `Close("Você não tem mais acesso à lixeira.")`; the screen shows a Toast (it is leaving) and pops.

**Rationale**: `AccessRepository.access` emits only on change; requiring a prior `owner` avoids closing on an initial
`NONE` before the profile is read.

## R10 — Leaving while a restore runs

**Decision**: The restore runs in `viewModelScope`; leaving the screen cancels waiting. No application scope is
introduced (CLAUDE.md forbids manual scopes in ViewModels). The server's result, if any, arrives with the next sync.
