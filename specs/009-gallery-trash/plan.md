# Implementation Plan: Gallery Trash

**Branch**: `009-gallery-trash` | **Date**: 2026-09-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/009-gallery-trash/spec.md`

## Summary

Owners get a trash screen inside `galleryGraph`, opened from a trash icon on the gallery root. The screen reads
`GET api/gallery/trash/` on every open (no cache), lists entries with their texts, and restores one entry at a time.
A restore is a gallery write: the returned album or photo is applied to the local index under the syncer's lock
without moving the cursor, then a never-skipped sync brings the rest of the batch. Refusals are classified once in
the domain (`RestoreResult`) and worded once in presentation (`TrashTexts`), shared by the trash screen and the new
"Desfazer" snackbar after deleting one album or photo.

Key technical choices (details in [research.md](research.md)):

- **Trash calls on `GalleryManageRepository`**, reusing its request/apply/sync helpers (R1).
- **`RestoreTrashItemUseCase` → `RestoreResult`**, extras read from `AppError.Server.extras` (R2); conflicting album
  synced in when missing (R3).
- **`TrashViewModel`** scoped to the trash entry; `StateFlow<TrashUiState>` + `SharedFlow<TrashEvent>` (R4, R5).
- **Local-time dates** with desugared `java.time` (R6); previews via `@GalleryThumbnailLoader` (R7).
- **Gallery messages become snackbars** with an optional action ("Desfazer" / "Abrir álbum"), shown by
  `GalleryMessageHost` (R8).
- **Access lost closes the trash** (R9); leaving mid-restore cancels waiting (R10).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (JVM 17 target), Android, `minSdk 24`, `targetSdk 36`

**Primary Dependencies**: existing only — Jetpack Compose (Material 3, `PullToRefreshBox` via
`ElasticPullToRefresh`), Hilt, Retrofit 3 + OkHttp 5, kotlinx.serialization, Coil 2.6, core library desugaring
(`java.time`). No new library.

**Storage**: none (the trash list is not cached); the gallery index is changed only through the existing
`applyLocal`

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fakes preferred (`FakeGalleryApi` and
`FakeGalleryManageRepository` extended with the trash calls)

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: restored item visible in the gallery as soon as the server answers (SC-002); trash of tens of
entries rendered in one lazy list

**Constraints**: online only; one restore at a time; `domain/` without Android types; only `ResponseExt` reads error
bodies; Portuguese strings hardcoded; 120-char lines

**Scale/Scope**: ~8 new production files, ~12 touched; ~5 test classes; `specs/gallery/spec.md` updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ repository returns `Result<T>` with `AppError`; `RestoreResult` carries `AppError` |
| Conversion in the data layer | ✅ `GalleryManageRepositoryImpl.request` |
| `message` technical, `userMessage` for screen | ✅ server `detail` only through `toUserMessage()` |
| One HTTP error parsing point | ✅ `trashed_parent_id` / `conflicting_album_id` read from `AppError.Server.extras` |
| 403 is permission, not login | ✅ shown as the server message; no login button; access loss closes the screen |
| Screen text via `toUserMessage()` | ✅ "Sem conexão" comes from the repository's `userMessage` |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| UI → ViewModel → UseCase → Repository | ✅ `LoadTrashUseCase`, `RestoreTrashItemUseCase` |
| Features don't import each other | ✅ everything in `features/gallery`; no core change |
| `domain/` without Android | ✅ dates kept as `String` in domain |
| Screen/Content split, dumb composables, three states | ✅ `TrashScreen` collects; `TrashContent` receives state + lambdas; loading / error / empty / list |
| `StateFlow` + `SharedFlow` for events | ✅ `TrashEvent` is a `SharedFlow`; gallery keeps its consumable `StateFlow<GalleryMessage?>` (007/008 justification) |
| `viewModelScope`, no manual scopes | ✅ |
| Graph-scoped ViewModel | ✅ gallery screens unchanged; `TrashViewModel` is per-entry on purpose (R4) |
| `safePopBackStack` | ✅ via `GalleryNav.back` |
| Tests: happy path + 1 error per use case, fakes | ✅ see quickstart |
| Spec and code in the same commit | ✅ `specs/gallery/spec.md` updated with the code |

**Gate: PASS.** Post-design re-check: PASS — no core change.

## Project Structure

### Documentation (this feature)

```text
specs/009-gallery-trash/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R10
├── data-model.md
├── quickstart.md
├── contracts/
│   └── gallery-trash-client.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
└── features/gallery/
    ├── data/
    │   ├── api/GalleryApi.kt, GalleryEndpoints.kt        # + trash, restoreAlbum, restorePhoto
    │   ├── dto/GalleryTrashEntryDto.kt (new), GalleryMappers.kt (+ toDomain)
    │   └── manage/GalleryManageRepositoryImpl.kt         # + trash(), restoreAlbum(), restorePhoto()
    ├── domain/
    │   ├── manage/GalleryManageRepository.kt             # + trash calls
    │   ├── manage/GalleryWriteErrorKinds.kt              # + trashedParentId(), conflictingAlbumId()
    │   ├── manage/GalleryManageUseCases.kt               # + restore
    │   └── trash/ (new) TrashEntry.kt (TrashKind, TrashKey, TrashEntry, RestoreResult),
    │       TrashUseCases.kt (LoadTrashUseCase, RestoreTrashItemUseCase)
    └── presentation/
        ├── navigation/GalleryNavGraph.kt                 # + Trash route, GalleryNav.toTrash
        ├── state/GalleryUiState.kt                       # MessageAction, AlbumTrashed/PhotoTrashed with undo, showTrash
        ├── state/TrashUiState.kt (new)                   # TrashRow, TrashUiState, TrashEvent
        ├── viewmodel/TrashTexts.kt (new)                 # row texts + restore result texts
        ├── viewmodel/TrashViewModel.kt (new)
        ├── viewmodel/GalleryViewModel.kt                 # undo, single-photo PhotoTrashed
        ├── screens/TrashScreen.kt (new)
        └── screens/GalleryScreen.kt, AlbumScreen.kt, PhotoScreen.kt  # trash icon, snackbar host, message effect

app/src/test/java/com/ipb/castelobranco/features/gallery/
├── data/api/FakeGalleryApi.kt, data/manage/GalleryManageRepositoryImplTest.kt (+ trash/restore)
├── domain/manage/FakeGalleryManageRepository.kt (+ trash), domain/trash/RestoreTrashItemUseCaseTest.kt (new)
└── presentation/viewmodel/TrashTextsTest.kt (new), TrashViewModelTest.kt (new),
    GalleryViewModelManageTest.kt (+ undo, showTrash)

specs/gallery/spec.md                                     # updated with the code
```

**Structure Decision**: all work in `features/gallery` following its data/domain/presentation split; the trash is a
new route of the existing graph.

## Implementation Order

1. **Data** — endpoints, DTO + mapper, API calls, repository methods; repository tests.
2. **Domain** — trash models, extras readers, use cases; use case tests.
3. **Texts + TrashViewModel** — row mapping and result texts; ViewModel with load, refresh, restore, highlight,
   events, access loss; tests.
4. **Screen + navigation** — `TrashScreen`/`TrashContent`, route, trash icon on the root.
5. **Undo + snackbars** — `GalleryMessage` actions, `GalleryMessageEffect` as snackbar on the three
   screens, `GalleryViewModel.undo`; tests.
6. **Specs** — `specs/gallery/spec.md`; run the unit tests and build.

## Risks

| Risk | Mitigation |
|------|-----------|
| Snackbar lost when the screen that shows it leaves | Leaving screens (`isRemoved` / `isClosed`) do not consume the message; the one below does |
| `LaunchedEffect` restart cancels `showSnackbar` when the message is consumed | Snackbar launched on `rememberCoroutineScope` (R8) |
| `deleted_at` format variations (microseconds, `+00:00`) | `OffsetDateTime.parse` accepts both; fallback shows the raw value |
| Access emits `NONE` before the profile loads | Close only after `owner` was seen (R9) |

## Implementation Notes

Where the code settled differently from the design above (the spec is unchanged):

- **No `BaseScreen` change**: the snackbar lives in `GalleryMessageHost` (in `GalleryScreen.kt`), which wraps each
  gallery screen's content with a `SnackbarHost` overlay — the pattern the members screens already use. Core is not
  touched.
- **Trash events on a `Channel`** (`receiveAsFlow`), as `ServiceWindowsViewModel` does: an event raised while the
  album opened by "Abrir álbum" is on top (e.g. access lost) waits for the trash screen instead of being dropped.
- **Restore on the repository** is one `restore(TrashKey)` instead of `restoreAlbum`/`restorePhoto`.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Second ViewModel inside `galleryGraph` (per-entry) | The trash list is online-only, read on each open, and unrelated to the gallery's local state | Growing the 683-line graph VM and keeping a stale list across visits |
