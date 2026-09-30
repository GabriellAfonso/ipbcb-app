# Research: Gallery Member Tags

Decisions taken while planning spec 010. Each: decision, rationale, alternatives.

## R1 — The user's member id reaches the gallery through a core port

**Decision**: `core/domain/member/CurrentMemberRepository` (`val memberId: Flow<Long?>`) and
`ObserveOwnMemberIdUseCase`. The profile feature implements it (`ProfileCurrentMemberRepository`) from the same `/me`
snapshot as `ProfileAccessRepository`: `MeProfileDto.memberId` (`member_id`, default `null`) → `MeProfile.memberId` →
`distinctUntilChanged`. No profile data (loading, error, signed out) emits `null`.

**Rationale**: Features never import each other, so the gallery needs a core port, exactly like `AccessRepository`.
A member id is not a permission (decision 8), so it stays out of `Access`. Reading the same snapshot means the value is
on screen from the disk preload and follows every re-read (including the one after a 403).

**Alternatives**: adding `memberId` to `Access` (mixes identity with permissions; every `Access` test and fake would
change); a gallery call to `/me` (second source of truth, no live update).

## R2 — Tag writes on `GalleryManageRepository`

**Decision**: three methods on the existing repository: `taggableMembers()` (`request`, no local effect),
`setPhotoMembers(photoId, memberIds)` (`write` → `UpsertPhoto`, sync after) and
`changePhotoMembers(photoIds, add, remove)` (`write` → new `GalleryLocalChange.UpsertPhotos`, `syncAfter = false`).
Bodies are built by hand in `GalleryWriteBodies.kt` like the others.

**Rationale**: `write` already gives the "2xx body or `AppError`" contract, the "Sem conexão" text, the local apply
under the syncer's lock without moving the cursor, and — for a `404` — the sync that brings the tree up to date
(`afterRequest` syncs on `isNotFound`). That is exactly FR-010, FR-014 and FR-016.

**Alternatives**: a `GalleryTagsRepository` (duplicated plumbing and fakes).

## R3 — Bulk writes: chunking and results in the domain

**Decision**: `ChangePhotoMembersUseCase(photoIds, memberIds, remove)` splits `photoIds` in chunks of
`MAX_PHOTOS_PER_REQUEST = 200`, sends them in order, keeps going after a failure (stops on `forbidden`, like
`runBatch`), and syncs once at the end when any chunk succeeded (a `404` chunk already synced in the repository).
It returns `TagBatchResult(updated, total, firstFailure: AppError?)`, `updated` counting the photos of successful
chunks.

`SetPhotoMembersUseCase(photoId, current, next)` returns `TagSaveResult.Unchanged` without a request when the sets are
equal, `Saved` or `Failed(error)` otherwise.

**Rationale**: Same all-attempted rule as 008's moves and deletes (section 8.6); the domain owns the loop so the
ViewModel only words the result. "Nothing sent when unchanged" is a business rule, so it is tested in the use case.

**Alternatives**: one request per photo (hundreds of requests); stopping at the first failure (inconsistent with 008).

## R4 — People, filter and search derived locally

**Decision**: pure functions in `domain/tags/GalleryPeople.kt`:

- `taggedPeople(tree)`: every member in at least one reachable photo, `photoCount`, ordered by name (normalized,
  then raw), then id; the name shown is the one of the photo read last in tree order (all equal after a sync).
- `photosWithAll(tree, memberIds)`: `tree.allPhotosInTreeOrder()` filtered by `memberIds ⊆ photo members`; empty for
  an empty set.
- `peopleIn(photos)`: the distinct members of the given photos, by name then id (the "Remover pessoas" list).

`NameSearch.normalize` = `Normalizer.normalize(NFD)`, drop `\p{Mn}`, `lowercase(Locale.ROOT)`; `matches(name, query)`
= normalized `contains`; a blank query matches all.

**Rationale**: The local index already holds every tag and every screen reads it (decisions 2 and 5; FR-021).
`GalleryTree.allPhotosInTreeOrder` already gives pre-order + position/id and ignores unreachable photos.
`java.text.Normalizer` is on every Android API level (decision 7).

**Alternatives**: the server's `tagged-members` and `member_id` filter (online only, can disagree with the index).

## R5 — Viewer with a second source

**Decision**: `ViewerSource` sealed (`Album(albumId)`, `People(memberIds: Set<Long>)`). New route
`PeoplePhoto/{memberIds}/{photoId}` with the ids comma-separated (`GalleryRoutes.peoplePhoto`), rendered by the same
`PhotoScreen`. The graph ViewModel memoizes viewers by `(source, photoId)`; the photo list is
`tree.photosOf(albumId)` or `photosWithAll(tree, memberIds)`. The existing album route and its signatures stay.

When the current photo leaves a `People` result: gone from the index → `PhotoRemoved`; still in the index (untagged,
or a member deleted) → new `GalleryMessage.PhotoLeftResult` ("Esta foto não está mais no resultado"). Then the same
next/previous/close logic as the album viewer. Photos this ViewModel removed or retagged itself post nothing extra.

Management from a `People` viewer uses the photo's own album: `ViewerPhoto` gains `albumId`, and "Mover", "Usar como
capa" and "Apagar" pass it.

**Rationale**: One viewer implementation, one set of management actions (FR-022). A path argument keeps the route
explicit and restorable after process death; member ids are few.

**Alternatives**: a shared mutable "current result" in the ViewModel (lost on process death, harder to test); a
second viewer screen (duplicated pager and menu).

## R6 — Picker in the gallery dialog host

**Decision**: `GalleryDialogState.PeoplePicker(PeoplePickerState)`, owned by the graph `GalleryViewModel`: mode
(`Photo(photoId)`, `Add(albumId, photoIds)`, `Remove(albumId, photoIds)`), loaded people, loading/error, query,
checked ids, saving. `visiblePeople` is derived in the state (search), so the composable is dumb. `Photo` mode lists
the photo's people first (photo order) then the rest in server order; `Remove` mode is built from the index and never
loads. The list lives only in the dialog state (decision 4).

Outcomes: success closes the picker (bulk also ends selection); `404` keeps it open, reloads its list (`Add`/`Photo`
from the server, `Remove` from the index after the sync) and shows the not-found text; `forbidden` closes it; other
failures keep it open (single) or close it keeping the selection (bulk, partial message). Losing `manage` closes an
open picker.

**Rationale**: Same host as forms, move picker and confirmations (one dialog at a time, busy state blocks dismiss).

**Alternatives**: a picker screen with its own ViewModel (a route for a transient choice; results would need to come
back to the album selection).

## R7 — Details sheet state

**Decision**: `ViewerPhoto` gains `description` (blank → `null`), `dateTaken` (formatted `dd/MM/yyyy` by
`GalleryUiMapper.formatDate`) and `people` (names in photo order). `PhotoScreen` keeps a `rememberSaveable` flag for
the sheet and renders it for the pager's current page, so it follows the page and the index (FR-004). The sheet is
hidden while the picker is open (decision 9).

**Rationale**: The data already flows through `viewerState`; showing/hiding a sheet is view state, like the menu's
`expanded`.

## R8 — People filter and "Minhas fotos" screen

**Decision**: one route `GalleryPeople?mine={mine}` with a per-entry `PeopleViewModel` (like `TrashViewModel`):
inputs `GalleryRepository.localState` (tree built on the default dispatcher) and `ObserveOwnMemberIdUseCase`; state
`PeopleUiState`. Filter mode: search field, selected people as chips, the matching people list while nothing is
selected or a query is typed, otherwise the results grid (3 columns). "Minhas fotos" mode: title "Minhas fotos", the
grid for `{ownMemberId}` only; it closes if the link disappears after being seen. The selection lives in the
ViewModel, so it survives going to the viewer and back and is cleared when the screen is left (decision 13).

**Rationale**: The filter is read-only and independent of the graph ViewModel's management state; a per-entry
ViewModel keeps `GalleryViewModel` (already ~720 lines) from growing further.

**Alternatives**: two screens (duplicated grid); state in the graph ViewModel (outlives the screen).

## R9 — Root entries

**Decision**: `GalleryRootUiState` gains `showPeople` (not organizing, index present) and `showMyPhotos`
(own member id known, not organizing, index present). "Pessoas" is a top-bar icon (`Icons.Outlined.People`), "Minhas
fotos" a full-width entry above the album grid.

## R10 — Texts

**Decision**: `TagTexts` (pure, tested): `SAVED` "Marcações salvas", `NOT_FOUND` "Algumas fotos ou pessoas não existem
mais. Confira e tente de novo.", `updated(result)` "Marcações atualizadas em {n} fotos" / "1 foto" / "Marcações
atualizadas em {n} de {total} fotos: {motivo}", `photoCount(n)`, `failure(error)` (not found → `NOT_FOUND`, else
`toGalleryWriteError().message`).
