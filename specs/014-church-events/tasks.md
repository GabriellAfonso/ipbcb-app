# Tasks: Church Events

**Input**: `specs/014-church-events/` (plan.md, spec.md, research.md, data-model.md, contracts/app-events-api.md)

**Tests**: required by `CLAUDE.md` (happy path + 1 error per use case; fakes over mocks).

Paths: `main/` = `app/src/main/java/com/ipb/castelobranco/`, `test/` = `app/src/test/java/com/ipb/castelobranco/`.

## Phase 1: Setup

- [X] T001 Write the domain spec `specs/events/spec.md`: public list, detail, members-only rules, offline/expiry, management, levels, errors, out of scope. It references the backend contract and is the destination state, not a refactor.

## Phase 2: Foundational (shared `core/` pieces)

- [X] T002 [P] Create the `EventDateLabel` object in `main/core/presentation/format/EventDateLabel.kt`:
  - `dateLabel(start: LocalDate, end: LocalDate?, time: LocalTime?)` returns:
    - one day: "14 nov · 19:30" (time optional);
    - range: "14 a 16 nov", "30 nov a 2 dez", or across years "30 dez 2026 a 2 jan 2027";
  - `end == start` counts as one day;
  - `fullDateLabel` returns the detail form ("Sábado, 14 de novembro" / "14 a 16 de novembro"), in pt-BR.
- [X] T003 [P] Write `EventDateLabelTest` in `test/core/presentation/format/EventDateLabelTest.kt`: single day with and without time, same month, across months, across years, end == start.
- [X] T004 [P] Add `EVENT_IMAGES = 0.05` to `main/core/data/image/ImageMemoryBudget.kt`. Create `main/core/di/EventImageLoaderModule.kt` with:
  - the `@EventImageLoader` qualifier;
  - a Coil `ImageLoader` on `@Client`: `respectCacheHeaders(false)`, disk cache `event_images` of 50 MB, memory budget `EVENT_IMAGES`;
  - an `EventImageCache` (`fun interface` in `main/core/domain/events/EventImageCache.kt`) that clears the memory and disk caches.
- [X] T005 [P] Generalize `main/core/presentation/components/SquarePhotoPicker.kt`:
  - add `rememberCroppedPhotoPicker(aspectX, aspectY, maxWidth, maxHeight, title, onPicked, onCancelled)`;
  - `rememberSquarePhotoPicker` delegates to it with 1:1, 512 and "Recortar Foto";
  - keep the leftover sweep.
- [X] T006 Add `HighlightEventUi(id, title, dateLabel, imageUrl)` and `HighlightEvent(page, imageLoader, onClick)` to `main/core/presentation/components/Highlight.kt`:
  - `AsyncImage` crop fill on a neutral `surfaceContainerHighest` background;
  - bottom vertical gradient with the title (2 lines max) and the date label;
  - `@Preview`s with fake data (with and without image).

  Keep `HighlightEvents()` as the empty page.

**Checkpoint**: core compiles; date label tests pass.

## Phase 3: User Story 1 — Upcoming events in the carousel (P1) 🎯 MVP

**Goal**: anyone sees up to 3 active events in the carousel.
**Independent test**: logged out with 3+ public events, the carousel shows 3 event pages in order.

- [X] T007 [P] [US1] Create `EventsEndpoints` (paths and header constants) and `EventsApi` (`getEvents(ifNoneMatch)`) in `main/features/events/data/api/`.
- [X] T008 [P] [US1] Create the `@Serializable EventPublicDto` in `main/features/events/data/dto/EventPublicDto.kt`, and the `Event` model with `lastDay` and `isActiveOn(day)` in `main/features/events/domain/model/Event.kt`.
- [X] T009 [US1] Create `List<EventPublicDto>.toDomain()` in `main/features/events/data/mapper/EventMapper.kt`:
  - blank → null;
  - "HH:MM" → `LocalTime`, null if unparsable;
  - an unparsable `start_date` drops the item with a `Timber.w`.
- [X] T010 [US1] Create the `EventsRepository` interface in `main/features/events/domain/repository/EventsRepository.kt` with: `observe(): Flow<SnapshotState<List<Event>>>`, `preload()`, `refresh(): RefreshResult`, `clear()`.
- [X] T011 [US1] Implement `EventsRepositoryImpl` in `main/features/events/data/repository/EventsRepositoryImpl.kt`, extending `BaseSnapshotRepository<List<EventPublicDto>, List<Event>>`, tag "EventsSnapshot":
  - `refresh` is guarded by a `Mutex.tryLock`; a concurrent call returns `RefreshResult.NotModified`;
  - `clear()` = `clearCache()` (state back to `Loading`, which the carousel reads as no events).
- [X] T012 [US1] Wire the DI.
  - `main/features/events/di/EventsSnapshotModule.kt`: cache key "events" with `ListSerializer(EventPublicDto.serializer())`, and a `RetrofitSnapshotFetcher`.
  - `main/features/events/di/EventsModule.kt`:
    - binds the repository;
    - provides `EventsApi` from `@AuthedRetrofit`;
    - `@IntoSet` `Preloadable` and `Refreshable`;
    - `@IntoSet` `SessionScopedCache { repository.clear(); eventImageCache.clear() }`.
- [X] T013 [P] [US1] Create `ObserveActiveEventsUseCase` and `ObserveHighlightEventsUseCase` in `main/features/events/domain/usecase/`:
  - maps the snapshot: `Data` → the events active on `DateProvider.today()`, `take(HIGHLIGHT_LIMIT = 3)`; `Loading`/`Error` → empty list;
- [X] T014 [P] [US1] Create `RefreshEventsUseCase` in `main/features/events/domain/usecase/RefreshEventsUseCase.kt`.
- [X] T015 [US1] Create `EventsHighlightViewModel` in `main/features/events/presentation/viewmodel/EventsHighlightViewModel.kt`:
  - `pages: StateFlow<List<HighlightEventUi>>` mapped with `EventDateLabel`;
  - exposes `imageLoader` (`@EventImageLoader`);
  - `refresh()`;
  - `onLoginStateChanged(isLoggedIn)` refreshes only when the value differs from the last one seen; the first value is only recorded.
- [X] T016 [US1] Wire the home in `main/core/presentation/screens/CoreScreen.kt`:
  - `CoreView` gets `eventsViewModel: EventsHighlightViewModel = hiltViewModel()`, collects `pages`, uses `LifecycleResumeEffect` → `refresh()` and `LaunchedEffect(isLoggedIn)` → `onLoginStateChanged`;
  - `CoreScreen` gets `eventPages`, `eventImageLoader` and `onEventClick`;
  - `buildHighlightPages` adds one `HighlightEvent` per page, or `HighlightEvents()` when there are none;
  - `CoreView` gets the `onNavigateToEvent(id)` parameter.
- [X] T017 [P] [US1] Write `EventMapperTest` in `test/features/events/data/mapper/EventMapperTest.kt`: full item, nulls/blanks, bad time, bad date dropped.
- [X] T018 [P] [US1] Write `ObserveHighlightEventsUseCaseTest` in `test/features/events/domain/usecase/ObserveHighlightEventsUseCaseTest.kt`, with a fake repository and a fixed `DateProvider`:
  - expired event dropped;
  - multi-day event still running kept;
  - limit 3 in order;
  - Error → empty.
- [X] T019 [P] [US1] Write `EventsHighlightViewModelTest` in `test/features/events/presentation/viewmodel/EventsHighlightViewModelTest.kt`: pages mapped; the first login value does not refresh, a change does.

**Checkpoint**: the carousel shows real events (tap not wired yet).

## Phase 4: User Story 2 — Event detail (P1)

**Goal**: tapping a page opens the full detail.
**Independent test**: an event with all fields shows every row; a minimal event shows only image, title and date.

- [X] T020 [P] [US2] Create `GetEventUseCase` in `main/features/events/domain/usecase/GetEventUseCase.kt`: `invoke(id): Flow<Event?>` from the active (unlimited) list.
- [X] T021 [US2] Create `EventDetailUiState`/`EventDetailUi` in `main/features/events/presentation/state/EventDetailUiState.kt`, and `EventDetailViewModel` in `main/features/events/presentation/viewmodel/EventDetailViewModel.kt` (`SavedStateHandle` `eventId`; `isGone` when null after loading).
- [X] T022 [US2] Create `EventDetailScreen` (collector) and `EventDetailContent` (pure) in `main/features/events/presentation/screens/EventDetailScreen.kt`:
  - `BaseScreen` with back;
  - 16:9 image (`ContentScale.Fit`, neutral background);
  - title, `fullDateLabel`;
  - rows for time, location and description, each only when present;
  - gone state: "Este evento não está mais disponível.".
- [X] T023 [US2] Add the event detail route and its tap from the home:
  - `AppRoutes.EVENT_DETAIL = "events/{eventId}"`, `ARG_EVENT_ID` and `eventDetail(id)` in `main/core/presentation/navigation/AppRoutes.kt`;
  - an inline `composable` in `main/core/presentation/navigation/AppNavHost.kt`;
  - pass `onNavigateToEvent` to `CoreView`.
- [X] T024 [P] [US2] Write `EventDetailViewModelTest` in `test/features/events/presentation/viewmodel/EventDetailViewModelTest.kt`: found → mapped rows; missing → `isGone`.

## Phase 5: User Story 3 — Members-only events (P2)

**Goal**: members see members-only events; they vanish on logout.
**Independent test**: logged out 1 page, member 2 pages, after logout 1 page offline.

- [X] T025 [US3] Write `EventsSessionCacheTest` in `test/features/events/di/EventsSessionCacheTest.kt`: the session cache clears the repository (disk + memory, state `Loading`) and calls `EventImageCache.clear()`. Extract the lambda into a testable `eventsSessionCache(repository, imageCache)` function in `EventsModule.kt`.
- [X] T026 [US3] Write `EventsRepositoryImplTest` in `test/features/events/data/repository/EventsRepositoryImplTest.kt`, with a fake cache and fetcher: a refresh saves and emits; `clear` empties the disk and emits empty; a concurrent refresh is skipped.

(The members-only behaviour itself comes from the server via the token; T012/T015/T016 already carry the session handling.)

## Phase 6: User Story 4 — Create and edit events (P1)

**Goal**: managers create and edit events from the panel.
**Independent test**: create an event as Mídia, see it in the list and the carousel; edit and clear fields.

- [X] T027 [P] [US4] Create `EventsAdminEndpoints` and `EventsAdminApi` (per the contract) in `main/features/admin/events/data/api/`.
- [X] T028 [P] [US4] Create `EventRecordDto`, `CreatedByDto` and `EventListDto` in `main/features/admin/events/data/dto/EventsAdminDtos.kt`.
- [X] T029 [P] [US4] Create the domain models in `main/features/admin/events/domain/model/`:
  - `EventRecord`;
  - `EventVisibility` with `wireValue`;
  - `EventStatusFilter` with a pt label;
  - `EventField` with `apiKey` and `fromApiKey`;
  - `EventDraft` with `EventRecord.toDraft()`;
  - `EventChanges` with `forCreate(draft)` and `changesFrom(original, draft)` (cleared optional → "").
- [X] T030 [US4] Create `EventRecordDto.toDomain()` in `main/features/admin/events/data/mapper/EventRecordMapper.kt`.
- [X] T031 [P] [US4] Create `ValidateEventDraftUseCase` in `main/features/admin/events/domain/usecase/ValidateEventDraftUseCase.kt`, returning `Map<EventField, String>`:
  - "Informe o título.";
  - "O título pode ter até 120 caracteres.";
  - "Informe a data de início.";
  - "A data de fim não pode ser antes da data de início.";
  - "O local pode ter até 200 caracteres.";
  - "Escolha uma imagem." (create, no image picked).
- [X] T032 [P] [US4] Create the `ImageBoundsReader` interface in `main/features/admin/events/domain/repository/ImageBoundsReader.kt`, and `AndroidImageBoundsReader` (`BitmapFactory` `inJustDecodeBounds`) in `main/features/admin/events/data/image/AndroidImageBoundsReader.kt`. Then create `ValidateEventImageUseCase` in `main/features/admin/events/domain/usecase/ValidateEventImageUseCase.kt` (min 640×360 → "A imagem precisa ter pelo menos 640×360 pixels.").
- [X] T033 [US4] Create `EventsAdminRepository` in `main/features/admin/events/domain/repository/EventsAdminRepository.kt` (`list(status)`, `get(id)`, `create(changes, image)`, `update(id, changes, image?)`, `delete(id)`, all `Result`). Implement `EventsAdminRepositoryImpl` in `main/features/admin/events/data/repository/EventsAdminRepositoryImpl.kt`:
  - text parts are `text/plain` `RequestBody`s;
  - the image part is named `image`, file `event.jpg`, `image/jpeg`;
  - failures go through `toAppError()`.
- [X] T034 [US4] Create the use cases in `main/features/admin/events/domain/usecase/`:
  - `ListEventsUseCase`;
  - `GetEventRecordUseCase`;
  - `SaveEventUseCase`: create when there is no original, otherwise patch with the changes; a patch with no changes and no image returns the original.
- [X] T035 [US4] Wire `EventsAdminModule` in `main/features/admin/events/di/EventsAdminModule.kt`: the API from `@AuthedRetrofit`, plus the repository and bounds reader bindings.
- [X] T036 [P] [US4] Create the shared admin-events failure handling:
  - `EventsAdminEvent` in `main/features/admin/events/presentation/state/EventsAdminEvent.kt`: `ShowMessage`, `LeaveArea`, `EventGone` ("Este evento não existe mais."), `Saved`, `Deleted`;
  - `Throwable.toEventsAdminEvent(kind)` in `main/features/admin/events/presentation/util/EventsAdminFailure.kt`, mirroring `MembersFailure.kt`.
- [X] T037 [US4] Create `EventsAdminListUiState` in `main/features/admin/events/presentation/state/` and `EventsAdminListViewModel` in `main/features/admin/events/presentation/viewmodel/`:
  - filter (default ALL), loading/error/items, `canCreate` (`events` ≥ MANAGE from `ObserveAccessUseCase`);
  - reload on filter change and on `onResume`.
- [X] T038 [US4] Create `EventFormUiState` in `main/features/admin/events/presentation/state/` and `EventFormViewModel` in `main/features/admin/events/presentation/viewmodel/`:
  - state: `eventId` from the nav argument, `isEditing`, `isReadOnly` (< MANAGE), `canDelete` (OWNER), `fieldErrors`, `generalError`, `isSaving`, `isDeleting`, `hasUnsavedChanges`, `imageUrl`, `pickedImage`;
  - `onImagePicked` validates the bounds;
  - `onSave` runs the local validation first, then save; server `field_errors` go to their fields through `EventField.fromApiKey`, and unknown keys go to `generalError`;
  - `onDelete`.
- [X] T039 [P] [US4] Create the list components in `main/features/admin/events/presentation/components/`:
  - `EventAdminCard` (16:9 thumbnail via `@EventImageLoader`, title, `EventDateLabel`, a "Só membros" chip, faded when past);
  - `EventFormFields`:
    - title, location and description text fields;
    - start/end dates with `DateFieldWithPicker` + `DatePickerDialog`;
    - time in a `TimePicker` dialog;
    - "remover" on the end date and the time;
    - a Público / Só membros segmented button;
    - an image box with "Escolher imagem" through `rememberCroppedPhotoPicker(16, 9, 1920, 1080, "Recortar imagem")`;
    - errors under each field;
    - every input disabled when read-only.
- [X] T040 [US4] Create `EventsAdminListScreen` in `main/features/admin/events/presentation/screens/EventsAdminListScreen.kt` (collector + content):
  - filter chips Ativos / Passados / Todos;
  - empty state "Nenhum evento.";
  - error with "Tentar novamente";
  - FAB "Novo evento" when `canCreate`.
- [X] T041 [US4] Create `EventFormScreen` in `main/features/admin/events/presentation/screens/EventFormScreen.kt` (collector + content):
  - title "Novo evento" / "Editar evento" / "Evento";
  - Salvar disabled while saving;
  - discard dialog on back with unsaved changes;
  - general error text.
- [X] T042 [US4] Add the `EventsAdminRoutes` object (GRAPH, LIST, FORM with an optional `eventId`) and `eventsAdminGraph(navController)` in `main/features/admin/events/presentation/navigation/EventsAdminNavGraph.kt`:
  - `LeaveArea` pops the graph with a toast;
  - `EventGone` and `Deleted` go back to LIST (with a toast for `EventGone`);
  - `Saved` goes back.
- [X] T043 [US4] Enable the panel entry:
  - `main/features/admin/panel/domain/PanelCard.kt`: EVENTS → `AtLeast(Scope.EVENTS, AccessLevel.VIEW)`;
  - `main/features/admin/panel/presentation/screens/AdminScreen.kt`: the card is enabled and goes to `nav.events`;
  - `main/features/admin/panel/presentation/navigation/AdminNavGraph.kt`: add `events` to `AdminNav` (and to every `AdminNav(...)` construction, previews included), and call `eventsAdminGraph(navController)`.
- [X] T044 [P] [US4] Write `ValidateEventDraftUseCaseTest` and `EventChangesTest` in `test/features/admin/events/domain/`. The changes cases: create carries every filled field; edit carries only the changed ones; a cleared location/time/end date gives ""; a visibility change carries its wire value.
- [X] T045 [P] [US4] Write `SaveEventUseCaseTest` in `test/features/admin/events/domain/usecase/SaveEventUseCaseTest.kt`, with a fake repository: create, patch, no-op, and a failure passed through.
- [X] T046 [P] [US4] Write `EventFormViewModelTest` in `test/features/admin/events/presentation/viewmodel/EventFormViewModelTest.kt`:
  - local errors block the save;
  - server `field_errors` land on their fields and an unknown key goes to `generalError`;
  - 404 → `EventGone`;
  - read-only below MANAGE;
  - a double save is ignored.
- [X] T047 [P] [US4] Write `EventsAdminListViewModelTest` in `test/features/admin/events/presentation/viewmodel/EventsAdminListViewModelTest.kt`: the filter reloads with the right status; a 403 read → `LeaveArea`.
- [X] T048 [P] [US4] Update `test/features/admin/panel/...` (the existing `VisiblePanelCardsUseCase` tests) for EVENTS at `view`.

## Phase 7: User Story 5 — Delete events (P2)

**Goal**: owners delete after a confirmation.
**Independent test**: Admin deletes; `manage` sees no delete.

- [X] T049 [US5] Create `DeleteEventUseCase` in `main/features/admin/events/domain/usecase/DeleteEventUseCase.kt` (a 404 counts as already deleted and maps to `EventGone`), and `DeleteEventDialog` in `main/features/admin/events/presentation/components/DeleteEventDialog.kt` ("Excluir evento?" / "A exclusão é permanente e não pode ser desfeita." / Excluir / Cancelar).
- [X] T050 [US5] Wire the delete into `EventFormViewModel` (`onDelete`, `isDeleting`, `Deleted` event) and `EventFormScreen` (a top bar action only when `canDelete`, then the dialog).
- [X] T051 [P] [US5] Write `DeleteEventUseCaseTest`, and add the delete cases (success → `Deleted`; no delete when not OWNER) to `EventFormViewModelTest`.

## Phase 8: Polish & Cross-Cutting

- [X] T052 [P] Update `specs/core/spec.md`: carousel page 3 → event pages (max 3) / empty page, `HighlightEvent` in the components table, `EventImageLoaderModule`, the `EVENT_DETAIL` route, the resume/login refresh.
- [X] T053 [P] Update `specs/admin/spec.md` §2.1: Eventos navigates to the events area, requirement `events` ≥ `view`; and the role summary paragraph.
- [X] T054 Run `./gradlew :app:testDebugUnitTest` (no `clean`), fix failures, then `./gradlew :app:compileDebugKotlin` for lint-free compilation (120-char lines).
- [X] T055 Mark the completed tasks here and in `checklists/requirements.md` notes; commit spec and code together (Conventional Commits, `feat(events): ...`).

## Dependencies

- Phase 2 blocks every story.
- US1 → US2 (the detail uses the repository and the tap), and US1 → US3 (the session cache lives in the US1 DI).
- US4 is independent of US1–US3; it needs only Phase 2 (the date label, the image loader, the picker).
- US5 depends on US4 (form and screen).

## Parallel Examples

- Phase 2: T002/T003, T004 and T005 run in parallel; T006 comes after T002.
- US1: T007 and T008 run in parallel; T013 and T014 run in parallel after T010; the tests T017–T019 run in parallel.
- US4: T027, T028, T029, T031 and T032 run in parallel; T039 runs in parallel with the ViewModels; the tests T044–T048 run in parallel.

## Implementation Strategy

MVP = Phase 2 + US1 + US2 (real events in the carousel, with their detail). Then US4 + US5 so events can be created
from the app (until then, through the Django admin), then US3's tests. Each phase ends compiling and with its tests
green.
