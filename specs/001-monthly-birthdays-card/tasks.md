# Tasks: Monthly Birthdays Highlight Card

**Input**: Design documents from `specs/001-monthly-birthdays-card/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/birthdays-api.md

**Tests**: Not explicitly requested in spec. Omitted.

**Organization**: Tasks grouped by user story for independent implementation.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Exact file paths included in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Domain model, DTOs, API interface, and DI wiring shared by all stories

- [x] T001 [P] Create Birthday domain model in `app/src/main/java/com/ipb/castelobranco/core/domain/model/Birthday.kt`
- [x] T002 [P] Create BirthdayDto and BirthdaysResponseDto in `app/src/main/java/com/ipb/castelobranco/core/data/dto/BirthdayDtos.kt`
- [x] T003 [P] Create MembersApi Retrofit interface in `app/src/main/java/com/ipb/castelobranco/core/data/api/MembersApi.kt` — `GET members/birthdays/` with `@Query("month")` param, using `Response<BirthdaysResponseDto>`
- [x] T004 [P] Create MembersRepository interface in `app/src/main/java/com/ipb/castelobranco/core/domain/repository/MembersRepository.kt` — `observeBirthdays(): Flow<SnapshotState<List<Birthday>>>`, `getCurrentSnapshot()`, `preload()`, `refreshBirthdays(): RefreshResult`, `clearBirthdaysCache()`
- [x] T005 Create BirthdayMapper in `app/src/main/java/com/ipb/castelobranco/core/data/mapper/BirthdayMapper.kt` — `BirthdaysResponseDto.toDomain(): List<Birthday>`, sort by day ascending, filter blank names

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Repository implementation, snapshot infrastructure, DI modules — MUST complete before user stories

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Create BirthdaySnapshotFetcher in `app/src/main/java/com/ipb/castelobranco/core/data/snapshot/BirthdaySnapshotFetcher.kt` — extend `RetrofitSnapshotFetcher<BirthdaysResponseDto>`, inject `MembersApi`, call `api.getBirthdays(currentMonth)` where `currentMonth` = `java.time.LocalDate.now().monthValue`
- [x] T007 Create MembersRepositoryImpl in `app/src/main/java/com/ipb/castelobranco/core/data/repository/MembersRepositoryImpl.kt` — extend `BaseSnapshotRepository<BirthdaysResponseDto, List<Birthday>>` with mapper `{ it.toDomain() }`, delegate interface methods to base class
- [x] T008 Create BirthdaySnapshotModule in `app/src/main/java/com/ipb/castelobranco/core/di/BirthdaySnapshotModule.kt` — provide `SnapshotCache<BirthdaysResponseDto>` via `SnapshotCacheFactory.create(key = "birthdays_month_${LocalDate.now().monthValue}", serializer = BirthdaysResponseDto.serializer())` and `SnapshotFetcher<BirthdaysResponseDto>` via `BirthdaySnapshotFetcher`
- [x] T009 Create MembersModule in `app/src/main/java/com/ipb/castelobranco/core/di/MembersModule.kt` — provide `MembersApi` from `@AuthedRetrofit`, bind `MembersRepositoryImpl` to `MembersRepository`, register as `Preloadable` and `Refreshable` via `@IntoSet`
- [x] T010 Create GetMonthlyBirthdaysUseCase in `app/src/main/java/com/ipb/castelobranco/core/domain/usecase/GetMonthlyBirthdaysUseCase.kt` — `observe(): Flow<SnapshotState<List<Birthday>>>` and `suspend refresh(): RefreshResult` delegating to `MembersRepository`

**Checkpoint**: Birthday data pipeline ready (API → DTO → Cache → Domain). Use case available for ViewModel injection.

---

## Phase 3: User Story 1 - View Current Month Birthdays (Priority: P1) MVP

**Goal**: Display birthday list in home screen highlight carousel with dynamic data from API

**Independent Test**: Open app (logged in) → highlight carousel shows birthday card with names and days sorted ascending. Empty month shows placeholder.

### Implementation for User Story 1

- [x] T011 [US1] Add birthday state to CoreViewModel in `app/src/main/java/com/ipb/castelobranco/core/presentation/viewmodel/CoreViewModel.kt` — inject `GetMonthlyBirthdaysUseCase`, add `val birthdays: StateFlow<SnapshotState<List<Birthday>>>`, collect use case flow in `initialize()`
- [x] T012 [US1] Replace `HighlightBirthdays()` with dynamic composable in `app/src/main/java/com/ipb/castelobranco/core/presentation/components/Highlight.kt` — new signature `HighlightBirthdays(birthdays: List<Birthday>)`, show title "Aniversariantes do Mes" with header, vertical list of entries (day badge + name), empty state fallback to existing placeholder with cake emoji. Design: clean vertical list with day numbers styled as badges, NOT a table layout
- [x] T013 [US1] Wire birthday state into CoreScreen in `app/src/main/java/com/ipb/castelobranco/core/presentation/screens/CoreScreen.kt` — collect `birthdays` StateFlow in `CoreView`, pass `List<Birthday>` to `CoreScreen`, update `buildHighlightPages` to pass birthdays to `HighlightBirthdays(birthdays)`. On Loading/Error states, pass empty list (shows placeholder)
- [x] T014 [US1] Add `@Preview` for HighlightBirthdays in `app/src/main/java/com/ipb/castelobranco/core/presentation/components/Highlight.kt` — preview with fake birthday data and preview with empty list

**Checkpoint**: User Story 1 fully functional — birthdays visible on home screen from API with empty state fallback.

---

## Phase 4: User Story 2 - Offline Birthday Access (Priority: P2)

**Goal**: Cached birthday data available without network

**Independent Test**: Load birthdays with network → kill app → airplane mode → relaunch → cached birthdays still visible

### Implementation for User Story 2

- [x] T015 [US2] Verify snapshot cache integration works end-to-end — `BaseSnapshotRepository.preload()` loads from `JsonSnapshotStorage`, `BirthdaySnapshotModule` provides cache with month-specific key. No new code needed if Phase 2 was implemented correctly; this task validates the offline path works by reviewing the wiring between `BirthdaySnapshotModule` (cache key), `MembersModule` (Preloadable registration), and `PreloadDataUseCase` (preload call chain)
- [x] T016 [US2] Handle stale month cache in `app/src/main/java/com/ipb/castelobranco/core/data/snapshot/BirthdaySnapshotFetcher.kt` — ensure fetcher always uses current month value at call time (not at injection time) so month transitions produce fresh API calls

**Checkpoint**: Offline access works. Cached data survives app restart. Month change triggers fresh fetch.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Cleanup and validation

- [x] T017 Add birthday cache clearing to logout flow in `app/src/main/java/com/ipb/castelobranco/core/presentation/viewmodel/CoreViewModel.kt` — call `membersRepository.clearBirthdaysCache()` in `logout()` alongside existing schedule/profile cache clears
- [x] T018 Run quickstart.md validation scenarios (all 4) to verify end-to-end behavior
- [x] T019 Update spec status from "Draft" to "Implemented" in `specs/001-monthly-birthdays-card/spec.md`

---

## Phase 6: Birthday Notifications (User Story 3)

**Purpose**: Daily notification for members whose birthday is today, using cached data

- [x] T020 [US3] Add `gender` field to `BirthdayDto` in `core/data/dto/BirthdayDtos.kt` and `Gender` enum + `gender` field to `Birthday` in `core/domain/model/Birthday.kt`
- [x] T021 [US3] Update `BirthdayMapper` in `core/data/mapper/BirthdayMapper.kt` to map `gender` string ("M"/"F"/null) to `Gender` enum
- [x] T022 [US3] Create `BirthdayNotificationWorker` in `core/data/worker/BirthdayNotificationWorker.kt` — reads cached birthdays via `MembersRepository.getCurrentSnapshot()`, filters `birth_day == today`, dispatches gender-personalized notifications with random message templates
- [x] T023 [US3] Schedule daily `PeriodicWorkRequest` in `MyApp.onCreate` with ~8AM initial delay and `ExistingPeriodicWorkPolicy.KEEP`
- [x] T024 [US3] Add `POST_NOTIFICATIONS` permission to `AndroidManifest.xml` and runtime permission request in `CoreActivity`
- [x] T025 [US3] Update spec docs (spec.md, plan.md, tasks.md, data-model.md, contracts/birthdays-api.md) with gender field and notification feature

**Checkpoint**: Daily birthday notifications working from cached data. Gender-personalized messages. Grouped for multiple birthdays.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001-T005 all parallelizable
- **Foundational (Phase 2)**: Depends on Phase 1 — T006-T010 sequential (fetcher → repo → DI → use case)
- **User Story 1 (Phase 3)**: Depends on Phase 2 — T011 → T012/T013 (T012, T013 partially parallel) → T014
- **User Story 2 (Phase 4)**: Depends on Phase 2 — T015, T016 can run after Phase 2
- **Polish (Phase 5)**: Depends on US1 completion minimum

### User Story Dependencies

- **User Story 1 (P1)**: Depends only on Foundational (Phase 2). No dependency on US2.
- **User Story 2 (P2)**: Depends only on Foundational (Phase 2). Validates existing snapshot infrastructure — no dependency on US1 UI changes.

### Parallel Opportunities

- Phase 1: T001, T002, T003, T004 all parallel (different files)
- Phase 2: T006 and T007 parallel after T005; T008 and T009 parallel after T006/T007
- Phase 3: T012 and T013 partially parallel (different files, but T013 uses types from T012)
- US1 and US2: Can proceed in parallel after Phase 2

---

## Parallel Example: Phase 1

```
# All Phase 1 tasks target different files — fully parallel:
Task T001: Birthday.kt (domain model)
Task T002: BirthdayDtos.kt (DTOs)
Task T003: MembersApi.kt (Retrofit interface)
Task T004: MembersRepository.kt (repository interface)
```

## Parallel Example: User Story 1

```
# After T011 (ViewModel), T012 and T013 target different files:
Task T012: Highlight.kt (UI component)
Task T013: CoreScreen.kt (screen wiring)
# But T013 depends on T012's new HighlightBirthdays signature
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T005)
2. Complete Phase 2: Foundational (T006-T010)
3. Complete Phase 3: User Story 1 (T011-T014)
4. **STOP and VALIDATE**: Test birthday card on home screen
5. Ready to merge as MVP

### Incremental Delivery

1. Setup + Foundational → Data pipeline ready
2. Add User Story 1 → Birthday card visible → **MVP!**
3. Add User Story 2 → Offline validated → Full feature
4. Polish → Logout cleanup, spec update → Done

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- US2 is mostly validation — snapshot infrastructure from Phase 2 handles offline automatically
- No test tasks generated (not requested in spec)
- Commit after each phase or logical group
