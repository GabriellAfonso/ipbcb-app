# Implementation Plan: Church Events

**Branch**: `014-church-events` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/014-church-events/spec.md`

## Summary

The carousel's placeholder "Eventos" page becomes up to 3 real event pages (16:9 flyer, title, date), with a detail
screen. A management area in the admin panel lists, creates, edits and deletes events. The public list is a snapshot
repository on the existing `BaseSnapshotRepository` pattern, read with the session when there is one. A
`SessionScopedCache` clears it on logout, and a refresh follows the login state change. Writes are multipart against
the backend 020 contract. Technical decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, Retrofit + OkHttp, kotlinx.serialization, Coil 2, uCrop, Navigation Compose

**Storage**: `JsonSnapshotStorage` (public list snapshot); Coil disk cache `cacheDir/event_images` (50 MB)

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred

**Target Platform**: Android (minSdk unchanged)

**Project Type**: mobile app, single `:app` module

**Performance Goals**:
- carousel event pages render from the disk snapshot before the network answers;
- a refresh with no change costs one 304.

**Constraints**:
- offline-readable;
- no members-only event or image on the device after logout;
- multipart-only writes;
- 120-char lines;
- user-visible strings in Portuguese.

**Scale/Scope**: tens of events per year; 3 new screens (detail, admin list, admin form) and one carousel page type.

## Constitution Check

Project constitution: `specs/constitution.md` (error handling) and the rules in `CLAUDE.md`.

| Gate | Status |
|------|--------|
| A single error type: repositories convert to `AppError`; no raw `Throwable` reaches a ViewModel | Pass — the repositories use `Result`/`toAppError()`; the snapshot uses `SnapshotState.Error(AppError)` |
| `userMessage` vs `message`: texts via `toUserMessage()`; `detail`/`field_errors` only from structured errors | Pass — field errors come from `AppError.Server.fieldErrors` |
| A single HTTP error parsing point (`ResponseExt.kt`) | Pass — no new parsing |
| Features do not import each other | Pass — shared pieces in `core/` (R1) |
| `domain/` has no Android imports | Pass — `java.time` only; image bounds decoding stays in `data/` behind an interface |
| Screen/content split, `StateFlow` UI state, `SharedFlow` one-shot events | Pass |
| `@Preview` on shared components in `core/presentation/components` | Required for `HighlightEvent` |
| `safePopBackStack()` only | Pass |
| Wrong Retrofit qualifier = silent 401 | `@AuthedRetrofit` for both interfaces (contract) |

Post-design re-check: no violations, so the Complexity Tracking table is empty.

## Project Structure

### Documentation (this feature)

```text
specs/014-church-events/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/app-events-api.md
├── checklists/requirements.md
└── tasks.md            # /speckit-tasks
```

Domain specs updated with the code:
- `specs/core/spec.md` (carousel, image loader, routes);
- `specs/admin/spec.md` §2.1 (Eventos card: ≥ `view`, navigates);
- new `specs/events/spec.md` (domain spec).

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── data/image/ImageMemoryBudget.kt                  # + EVENT_IMAGES
│   ├── di/EventImageLoaderModule.kt                     # @EventImageLoader (on @Client, disk cache)
│   ├── presentation/components/Highlight.kt             # + HighlightEvent(page) with @Preview
│   ├── presentation/components/SquarePhotoPicker.kt     # generalized: rememberCroppedPhotoPicker
│   ├── presentation/format/EventDateLabel.kt            # day / range / time labels (pt-BR)
│   ├── presentation/navigation/AppRoutes.kt             # + EVENT_DETAIL
│   ├── presentation/navigation/AppNavHost.kt            # + inline detail composable
│   └── presentation/screens/CoreScreen.kt               # events pages, resume + login refresh
├── features/events/
│   ├── data/api/{EventsApi,EventsEndpoints}.kt
│   ├── data/dto/EventPublicDto.kt
│   ├── data/mapper/EventMapper.kt
│   ├── data/repository/EventsRepositoryImpl.kt          # BaseSnapshotRepository
│   ├── di/{EventsModule,EventsSnapshotModule}.kt        # Preloadable, Refreshable, SessionScopedCache
│   ├── domain/model/Event.kt
│   ├── domain/repository/EventsRepository.kt
│   ├── domain/usecase/{ObserveHighlightEventsUseCase,GetEventUseCase,RefreshEventsUseCase}.kt
│   └── presentation/
│       ├── viewmodel/{EventsHighlightViewModel,EventDetailViewModel}.kt
│       ├── state/EventDetailUiState.kt
│       └── screens/EventDetailScreen.kt
└── features/admin/
    ├── panel/domain/PanelCard.kt                        # EVENTS ≥ VIEW
    ├── panel/presentation/screens/AdminScreen.kt        # card enabled → nav.events
    ├── panel/presentation/navigation/AdminNavGraph.kt   # + events, eventsAdminGraph()
    └── events/
        ├── data/api/{EventsAdminApi,EventsAdminEndpoints}.kt
        ├── data/dto/EventsAdminDtos.kt
        ├── data/mapper/EventRecordMapper.kt
        ├── data/image/AndroidImageBoundsReader.kt
        ├── data/repository/EventsAdminRepositoryImpl.kt
        ├── di/EventsAdminModule.kt
        ├── domain/model/{EventRecord,EventDraft,EventField,EventChanges,EventStatusFilter,EventVisibility}.kt
        ├── domain/repository/{EventsAdminRepository,ImageBoundsReader}.kt
        ├── domain/usecase/{ListEventsUseCase,GetEventRecordUseCase,SaveEventUseCase,DeleteEventUseCase,
        │                   ValidateEventDraftUseCase,ValidateEventImageUseCase}.kt
        └── presentation/
            ├── navigation/EventsAdminNavGraph.kt
            ├── state/{EventsAdminListUiState,EventFormUiState,EventsAdminEvent}.kt
            ├── util/EventsAdminFailure.kt
            ├── viewmodel/{EventsAdminListViewModel,EventFormViewModel}.kt
            ├── components/{EventAdminCard,EventFormFields,DeleteEventDialog}.kt
            └── screens/{EventsAdminListScreen,EventFormScreen}.kt

app/src/test/java/com/ipb/castelobranco/
├── core/presentation/format/EventDateLabelTest.kt
├── features/events/...                                  # mapper, use cases, repository session clear, VM
└── features/admin/events/...                            # validation, changes, repository parts, form VM
```

**Structure Decision**: single module, feature-based (R1).

## Implementation Order

1. Core shared pieces: date label, image loader, generalized crop picker, `HighlightEvent` page with previews.
2. Public side: DTO, mapper, snapshot repository, DI (preload, refresh, session clear), use cases, home ViewModel.
3. Home wiring: `CoreView`/`CoreScreen` pages, resume refresh, login-change refresh; detail route and screen.
4. Admin data and domain: API, DTOs, mapper, repository (multipart), validation, changes.
5. Admin presentation: list and form screens, navigation graph, panel card enabled at `view`.
6. Specs: `specs/events/spec.md`, `specs/core/spec.md`, `specs/admin/spec.md`.
7. Tests alongside each step; full `:app:testDebugUnitTest` at the end.

## Complexity Tracking

No violations.
