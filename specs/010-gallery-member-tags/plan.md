# Implementation Plan: Gallery Member Tags

**Branch**: `010-gallery-member-tags` | **Date**: 2026-09-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/010-gallery-member-tags/spec.md`

## Summary

The tags already live in the gallery index; this feature shows them, filters by them and writes them. The viewer gets
a details sheet ("ⓘ") and, for `manage`, "Marcar pessoas"; the album selection bar gets "Pessoas" (add / remove in
chunks of 200). Tag writes follow the 008 write path: the returned photos are applied to the index without moving the
cursor, then a sync runs; a `404` syncs and reloads the picker. The gallery root gets a people filter (AND, derived
from the index, offline) and "Minhas fotos", fed by a new core port that exposes the user's member id from the
profile. The viewer learns a second source (the filter result) through a new route.

Key technical choices (details in [research.md](research.md)):

- **`CurrentMemberRepository` core port** implemented by the profile feature from the `/me` snapshot (R1).
- **Tag calls on `GalleryManageRepository`**, `UpsertPhotos` local change (R2); chunked bulk use case (R3).
- **Pure people functions** (`GalleryPeople`, `NameSearch` with `java.text.Normalizer`) (R4).
- **`ViewerSource`** + route `PeoplePhoto/{memberIds}/{photoId}` (R5).
- **Picker in `GalleryDialogHost`** (`GalleryDialogState.PeoplePicker`) (R6); details sheet as view state (R7).
- **Per-entry `PeopleViewModel`** for the filter and "Minhas fotos" (R8); root entries (R9); texts in `TagTexts` (R10).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (JVM 17 target), Android, `minSdk 24`, `targetSdk 36`

**Primary Dependencies**: existing only — Jetpack Compose (Material 3, material-icons-extended), Hilt, Retrofit 3 +
OkHttp 5, kotlinx.serialization, Coil 2.6. No new library.

**Storage**: none new; the index changes only through `applyLocal`; `MeProfileDto` gains an optional field

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fakes preferred (`FakeGalleryApi`,
`FakeGalleryManageRepository`, `FakeAccessRepository`, new `FakeCurrentMemberRepository`)

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: filter results recomputed off the main thread on every selection or index change (index of a
few thousand photos); picker search over a few hundred names in memory

**Constraints**: filter offline; picker online and never on disk; `domain/` without Android types; only `ResponseExt`
reads error bodies; Portuguese strings hardcoded; 120-char lines

**Scale/Scope**: ~10 new production files, ~18 touched; ~8 test classes new or extended; `specs/gallery/spec.md`
updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ repository returns `Result<T>`; `TagSaveResult`/`TagBatchResult` carry `AppError` |
| Conversion in the data layer | ✅ `GalleryManageRepositoryImpl.request` |
| `message` technical, `userMessage` for screen | ✅ server `detail` only through `toUserMessage()` |
| One HTTP error parsing point | ✅ no extras parsed; `404` read from the status |
| 403 is permission, not login | ✅ global handling; controls and picker go away |
| Screen text via `toUserMessage()` | ✅ via `toGalleryWriteError()` |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| UI → ViewModel → UseCase → Repository | ✅ tag use cases; `ObserveOwnMemberIdUseCase` |
| Features don't import each other | ✅ member id through `core/domain/member`; profile implements it |
| `domain/` without Android | ✅ `java.text.Normalizer` is JDK |
| Screen/Content split, dumb composables, three states | ✅ `PeopleScreen`/`PeopleContent`; picker loading/error/empty/list |
| `StateFlow` + events | ✅ gallery keeps its consumable `StateFlow<GalleryMessage?>` (007/008 justification) |
| `viewModelScope`, no manual scopes | ✅ |
| Graph-scoped ViewModel | ✅ viewer and picker in the graph VM; `PeopleViewModel` per-entry on purpose (R8) |
| `safePopBackStack` | ✅ via `GalleryNav.back` |
| Tests: happy path + 1 error per use case, fakes | ✅ see quickstart |
| Spec and code in the same commit | ✅ `specs/gallery/spec.md` updated with the code |

**Gate: PASS.** Post-design re-check: PASS — the only core change is a new port, mirroring `AccessRepository`.

## Project Structure

### Documentation (this feature)

```text
specs/010-gallery-member-tags/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R10
├── data-model.md
├── quickstart.md
├── contracts/
│   └── gallery-tags-client.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/domain/member/ (new) CurrentMemberRepository.kt, ObserveOwnMemberIdUseCase.kt
├── features/profile/
│   ├── data/dto/MeProfileDto.kt                           # + member_id
│   ├── domain/model/MeProfile.kt                          # + memberId
│   ├── data/snapshot/ProfileSnapshotRepository.kt         # maps memberId
│   ├── data/access/ProfileCurrentMemberRepository.kt (new)
│   └── di/ProfileModule.kt                                # binds the port
└── features/gallery/
    ├── data/
    │   ├── api/GalleryApi.kt, GalleryEndpoints.kt         # + taggable members, put/change members
    │   ├── dto/GalleryWriteBodies.kt                      # + photoMembersBody, changeMembersBody
    │   └── manage/GalleryManageRepositoryImpl.kt          # + taggableMembers, setPhotoMembers, changePhotoMembers
    ├── domain/
    │   ├── model/GalleryLocalChange.kt, GalleryIndex.kt   # + UpsertPhotos
    │   ├── model/GalleryPhoto.kt                          # members doc
    │   ├── manage/GalleryManageRepository.kt, GalleryManageUseCases.kt
    │   └── tags/ (new) GalleryPeople.kt, NameSearch.kt, TagModels.kt, TagUseCases.kt
    └── presentation/
        ├── navigation/GalleryNavGraph.kt                  # + People, PeoplePhoto routes; nav lambdas
        ├── state/GalleryUiState.kt                        # ViewerSource, ViewerPhoto fields, root/album flags, message
        ├── state/GalleryDialogState.kt                    # PeoplePicker
        ├── state/PeopleUiState.kt (new)
        ├── viewmodel/GalleryViewModel.kt                  # viewer sources, picker, bulk tags, own member id
        ├── viewmodel/GalleryUiMapper.kt                   # viewerPhoto fields
        ├── viewmodel/TagTexts.kt (new)
        ├── viewmodel/PeopleViewModel.kt (new)
        ├── components/GalleryManageComponents.kt          # picker sheet, selection "Pessoas"
        ├── components/GalleryTagComponents.kt (new)       # details sheet, person rows, photo grid tile
        └── screens/PhotoScreen.kt, AlbumScreen.kt, GalleryScreen.kt, PeopleScreen.kt (new)

app/src/test/java/com/ipb/castelobranco/
├── core/testing/FakeCurrentMemberRepository.kt (new)
├── features/profile/data/access/ProfileCurrentMemberRepositoryTest.kt (new)
├── features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt (+ member_id)
└── features/gallery/
    ├── data/api/FakeGalleryApi.kt, data/manage/GalleryManageRepositoryImplTest.kt (+ tags)
    ├── data/dto/GalleryWriteBodiesTest.kt (+ bodies)
    ├── domain/model/GalleryIndexApplyTest.kt (+ UpsertPhotos)
    ├── domain/manage/FakeGalleryManageRepository.kt (+ tags)
    ├── domain/tags/ (new) GalleryPeopleTest.kt, NameSearchTest.kt, TagUseCasesTest.kt
    └── presentation/viewmodel/ TagTextsTest.kt, PeopleViewModelTest.kt, GalleryViewModelTagsTest.kt (new)

specs/gallery/spec.md                                      # updated with the code
```

**Structure Decision**: core gets one port; the profile feature implements it; everything else lives in
`features/gallery` following its data/domain/presentation split. The filter is a new route of the existing graph.

## Implementation Order

1. **Profile link** — DTO field, `MeProfile.memberId`, core port + use case, profile implementation + binding; tests.
2. **Data** — endpoints, bodies, API calls, `UpsertPhotos`, repository methods; repository and body tests.
3. **Domain** — `GalleryPeople`, `NameSearch`, tag models and use cases; tests.
4. **Viewer** — `ViewerSource`, new viewer fields, details sheet, route; tests for the result viewer.
5. **Picker + bulk** — dialog state, ViewModel actions, sheet, selection bar; `TagTexts`; tests.
6. **Filter + Minhas fotos** — `PeopleViewModel`, screen, root entries; tests.
7. **Specs** — `specs/gallery/spec.md`; build and run every unit test.

## Risks

| Risk | Mitigation |
|------|-----------|
| The leave-the-result message races the "Marcações salvas" message after untagging from a result viewer | Photos retagged by this ViewModel are skipped by the leave check (like `selfRemovedPhotos`) |
| Recomputing the AND filter on every index change is slow on big galleries | Built on the default dispatcher with the tree; `O(photos × selected)` |
| `member_id` read before the profile loads emits `null` and closes "Minhas fotos" | Close only after a non-null id was seen (same rule as the trash's access loss) |
| Picker left open when the photo disappears | The viewer closes a `Photo`-mode picker for the photo that left |

## Implementation Notes

Where the code settled differently from the design above (the spec is unchanged):

- **Repository tag tests in their own class**: `GalleryManageRepositoryTagsTest` (same package) applies the local
  changes to a real `GalleryIndex`, so "cursor untouched" is asserted on the index itself; the existing
  `GalleryManageRepositoryImplTest` is left as it was.
- **`PeopleScreen` also takes the graph `GalleryViewModel`**, only to host the gallery's messages
  (`GalleryMessageHost`): the viewer opened from a result may close (last photo left the result) and its message must
  show on the screen below.
- **`specs/core/spec.md` §4.2.2** documents the new core port next to the access vocabulary.
- **Filter selection**: toggling prunes people no longer tagged anywhere (tracked from the last computed state), so a
  person who left every photo does not come back selected.
- **`GalleryNav`'s new lambdas have defaults**, so previews and tests that build a `GalleryNav` keep compiling.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Second per-entry ViewModel inside `galleryGraph` (`PeopleViewModel`) | The filter's selection and search belong to one visit and are unrelated to management state | Growing the ~720-line graph VM and keeping a stale selection across visits |
