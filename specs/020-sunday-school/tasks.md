# Tasks: Sunday School Lessons

**Input**: Design documents from `specs/020-sunday-school/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: required by `CLAUDE.md` (happy path + one error per use case); fakes preferred.

Paths are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and `app/src/test/java/com/ipb/castelobranco/`
(test) unless stated.

## Phase 1: Setup

- [X] T001 Add `SUNDAY_SCHOOL("sunday_school")` to `core/domain/access/Scope.kt`
- [X] T002 Add `SUNDAY_SCHOOL_MANAGE` to `core/presentation/navigation/AppRoutes.kt`
- [X] T003 Paths in `features/studies/data/api/SundaySchoolEndpoints.kt`
- [X] T004 [P] 16 Phosphor duotone drawables `app/src/main/res/drawable/ic_ph_*.xml` (R-06)

## Phase 2: Foundational

- [X] T005 [P] Domain models (year, index, summary, lesson, status, book facts, content, blocks, icons) in `features/studies/domain/model/SundaySchoolModels.kt` and `LessonContent.kt`
- [X] T006 [P] `InlineMarkup` in `features/studies/domain/model/InlineMarkup.kt` + `InlineMarkupTest`
- [X] T007 [P] `StandardSections` in `features/studies/domain/model/StandardSections.kt`
- [X] T008 DTOs in `features/studies/data/dto/SundaySchoolDtos.kt`; `LessonContentCodec` (decode/encode, `Unknown`, lossless) in `features/studies/data/mapper/LessonContentCodec.kt`; mappers in `SundaySchoolMapper.kt` + `LessonContentCodecTest`
- [X] T009 [P] Retrofit `SundaySchoolApi` (public, ETag) and `SundaySchoolAdminApi` (authed) in `features/studies/data/api/`
- [X] T010 Repository interfaces `SundaySchoolRepository`, `SundaySchoolAdminRepository` in `features/studies/domain/repository/`
- [X] T011 Hilt bindings and APIs in `features/studies/di/StudiesModule.kt`

## Phase 3: User Story 1 + 2 — read a lesson, offline (P1)

**Goal**: EBD card → year tabs → lesson rendered with every block, chips to the Bible, "Aa", offline copies.
**Independent test**: 2026 lessons; open "Panorama de Daniel"; airplane mode reopen.

- [X] T012 [US1] `SundaySchoolSnapshotSources` + `SundaySchoolRepositoryImpl` (default index, per-year, per-lesson, 404 clears) in `features/studies/data/repository/` + `SundaySchoolRepositoryImplTest`
- [X] T013 [P] [US1] `SundaySchoolTheme.kt` (palette light/dark, fonts, parchment brush) and `SundaySchoolIcons.kt` in `features/studies/presentation/components/sundayschool/`
- [X] T014 [US1] Renderer `LessonContentView` + block composables in `features/studies/presentation/components/sundayschool/LessonContentView.kt` and `LessonBlocks.kt`
- [X] T015 [US1] `SundaySchoolViewModel` + `SundaySchoolScreen` (tabs, header, list, empty, error) + `SundaySchoolViewModelTest`
- [X] T016 [US1] `SundaySchoolLessonViewModel` + `SundaySchoolLessonScreen` (header, renderer, "Aa", error/gone)
- [X] T017 [US1] EBD card clickable in `features/studies/presentation/screens/StudiesScreen.kt`; routes in `features/studies/presentation/navigation/StudiesNavGraph.kt`

## Phase 4: User Story 3 — write and publish (P2)

**Goal**: panel card, lesson list, form with basic editor, chips, preview, server errors on fields.
**Independent test**: as Liderança create a draft, preview, publish.

- [X] T018 [US3] `BibleBookCatalog` port in `core/domain/bible/BibleBookCatalog.kt`; implementation in `features/bible/data/catalog/BibleBookCatalogImpl.kt` + binding
- [X] T019 [P] [US3] `ContentErrors` (path → location) and `VerseRangesInput` in `features/studies/domain/model/` + tests
- [X] T020 [P] [US3] `ValidateLessonDraftUseCase` in `features/studies/domain/usecase/` + test
- [X] T021 [US3] `SundaySchoolAdminRepositoryImpl` (list, get, create, update, delete, order, years, theme; content errors) in `features/studies/data/repository/`
- [X] T022 [US3] `PanelCard.SUNDAY_SCHOOL`, `AdminNav.sundaySchool`, card action in `features/admin/panel/`
- [X] T023 [US3] `SundaySchoolManageViewModel` + `SundaySchoolManageScreen` (year selector, drafts badge, FAB)
- [X] T024 [US3] `SundaySchoolLessonFormViewModel` (draft, editor operations, save, errors, discard) + test
- [X] T025 [US3] `LessonEditor` + `ProofTextEditorDialog` + `SundaySchoolLessonFormScreen` (Editar / Pré-visualizar)

## Phase 5: User Story 4 — organize (P3)

- [X] T026 [US4] Drag reorder (save on drop, mismatch reload) and delete dialog in the manage screen/VM
- [X] T027 [US4] `SundaySchoolYearsViewModel` + `SundaySchoolYearsScreen` (theme dialog)

## Phase 6: Polish

- [X] T028 Update `specs/studies/spec.md` and `specs/admin/spec.md`
- [X] T029 `./gradlew :app:compileDebugKotlin` and `:app:testDebugUnitTest --tests "com.ipb.castelobranco.features.studies.*"`

## Dependencies

Phase 1–2 before everything; US1 (Phase 3) before US3 (preview reuses the renderer); US4 after the manage screen
(T023).
