# Implementation Plan: Sunday School Lessons

**Branch**: `020-sunday-school` | **Date**: 2026-10-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/020-sunday-school/spec.md`

## Summary

Everything lives in `features/studies` (research R-01), next to the doctrine reader whose models and chips it
reuses:

- **Reading**: `SundaySchoolRepositoryImpl` keeps one snapshot for the default index, one per other year and one
  per lesson (`SnapshotCacheFactory`, ETag, preload → refresh), and clears a snapshot on its 404. The content JSON
  is decoded by `LessonContentCodec` into a sealed `LessonBlock` (ten types + `Unknown`). `LessonContentView`
  renders it on a cream/dark-brown page with Cormorant Garamond + Literata, parchment brush on the header and key
  verses, Phosphor duotone drawables, `ProofTextChips`, and the shared "Aa" doctrine size.
- **Panel**: `SundaySchoolAdminRepositoryImpl` over an authed API, results in memory only. Three screens: lesson
  list per year (drafts marked, drag reorder, delete), lesson form (metadata + editor of sections/paragraph/list/
  key verse/chips, non-editable blocks movable and deletable, preview tab with the same `LessonContentView`) and
  "Anos da EBD". The admin panel gets a card that navigates to `AppRoutes.SUNDAY_SCHOOL_MANAGE`.
- **Core**: `Scope.SUNDAY_SCHOOL`, `AppRoutes.SUNDAY_SCHOOL_MANAGE`, and the `BibleBookCatalog` port (implemented by
  `features/bible`) for the reference picker (R-11).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, Retrofit + kotlinx.serialization, `ui-text-google-fonts`, `sh.calvin.reorderable`
(all present; no new library)

**Storage**: `SnapshotStorage` (`filesDir/snapshots`) — `sunday_school_index`, `sunday_school_index_<year>`,
`sunday_school_lesson_<id>` (+ ETag); `doctrine_font_size` in the Settings DataStore (shared, R-04). Panel data is
never stored.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred

**Target Platform**: Android (minSdk 24)

**Project Type**: mobile app, single `:app` module

**Performance Goals**: a lesson of 8 000 characters renders in a `LazyColumn` (one item per block) and scrolls
smoothly; the saved copy appears before the network check.

**Constraints**:
- `features/studies` must not import `features/bible` or `features/admin` (catalog via `core/domain/bible`, panel
  via `AppRoutes`);
- `domain/` stays Android-free: codec lives in `data/`, models/markup/error mapping/validation in `domain/`;
- global `Json` has `explicitNulls = false`: write bodies use `buildJsonObject` (`date: null` must be sent);
- 120-char lines; Portuguese UI strings; no magic strings.

**Scale/Scope**: ~20 lessons per year, a few years; up to 30 sections × 60 blocks per lesson.

## Constitution Check

| Rule (`specs/constitution.md`, `CLAUDE.md`) | Status |
|---|---|
| Features never import each other | Pass: book catalog through `core/domain/bible`; panel card navigates by `AppRoutes` |
| `domain/` has no Android knowledge | Pass: models, markup parser, error paths, validation in pure Kotlin |
| Errors as `AppError`; parsing only in `ResponseExt` | Pass: repository reads `AppError.Server.extras["errors"]` (already extracted) |
| Screen/content split; `StateFlow`; one-shot `SharedFlow` | Pass |
| Graph-scoped VM | Pass: each screen has its own args; per-entry `hiltViewModel()`, as the doctrine screens |
| `safePopBackStack()` | Pass |
| Public content not encrypted; panel data not on disk | Pass |
| Spec and code together | Pass: `specs/studies/spec.md` and `specs/admin/spec.md` updated with the code |

No violations.

## Project Structure

### Documentation (this feature)

```text
specs/020-sunday-school/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/api.md, contracts/ui.md
├── checklists/requirements.md
└── tasks.md
specs/studies/spec.md, specs/admin/spec.md             # domain specs updated with the feature
```

### Source Code

```text
app/src/main/
├── res/drawable/ic_ph_*.xml                            # 16 Phosphor duotone icons (R-06)
└── java/com/ipb/castelobranco/
    ├── core/domain/access/Scope.kt                     # + SUNDAY_SCHOOL("sunday_school")
    ├── core/domain/bible/BibleBookCatalog.kt           # port: books (abbrev, name, verseCounts)
    ├── core/presentation/navigation/AppRoutes.kt       # + SUNDAY_SCHOOL_MANAGE
    ├── features/bible/data/catalog/BibleBookCatalogImpl.kt (+ binding in bible DI)
    ├── features/admin/panel/...                        # PanelCard.SUNDAY_SCHOOL, AdminNav.sundaySchool, card action
    └── features/studies/
        ├── data/api/SundaySchoolApi.kt, SundaySchoolAdminApi.kt, SundaySchoolEndpoints.kt
        ├── data/dto/SundaySchoolDtos.kt
        ├── data/mapper/LessonContentCodec.kt, SundaySchoolMapper.kt
        ├── data/repository/SundaySchoolRepositoryImpl.kt, SundaySchoolSnapshotSources.kt,
        │   SundaySchoolAdminRepositoryImpl.kt
        ├── di/StudiesModule.kt                         # + bindings and APIs
        ├── domain/model/SundaySchoolModels.kt, LessonContent.kt, InlineMarkup.kt, StandardSections.kt,
        │   ContentErrors.kt, VerseRangesInput.kt
        ├── domain/repository/SundaySchoolRepository.kt, SundaySchoolAdminRepository.kt
        ├── domain/usecase/ValidateLessonDraftUseCase.kt
        └── presentation/
            ├── components/sundayschool/SundaySchoolTheme.kt (colors, fonts, parchment brush),
            │   SundaySchoolIcons.kt, LessonContentView.kt (renderer), LessonBlocks.kt,
            │   LessonEditor.kt, ProofTextEditorDialog.kt
            ├── screens/SundaySchoolScreen.kt, SundaySchoolLessonScreen.kt,
            │   SundaySchoolManageScreen.kt, SundaySchoolLessonFormScreen.kt, SundaySchoolYearsScreen.kt
            ├── viewmodel/SundaySchoolViewModel.kt, SundaySchoolLessonViewModel.kt,
            │   SundaySchoolManageViewModel.kt, SundaySchoolLessonFormViewModel.kt, SundaySchoolYearsViewModel.kt
            ├── screens/StudiesScreen.kt                # EBD card clickable
            └── navigation/StudiesNavGraph.kt           # + 5 routes

app/src/test/java/com/ipb/castelobranco/features/studies/
├── data/mapper/LessonContentCodecTest.kt
├── data/repository/SundaySchoolRepositoryImplTest.kt
├── domain/model/InlineMarkupTest.kt, ContentErrorsTest.kt, VerseRangesInputTest.kt
├── domain/usecase/ValidateLessonDraftUseCaseTest.kt
└── presentation/viewmodel/SundaySchoolViewModelTest.kt, SundaySchoolLessonFormViewModelTest.kt
```

**Structure Decision**: one feature (`features/studies`); core gains a scope, a route and a small port.

## Implementation Order

1. Domain models, inline markup, standard sections, content errors, verse input (+ tests).
2. DTOs, `LessonContentCodec` (+ tests), APIs, endpoints.
3. Public repository + snapshot sources (+ tests); admin repository; DI.
4. Core: `Scope`, `AppRoutes`, `BibleBookCatalog` + bible implementation.
5. Theme, icons (drawables), `LessonContentView`.
6. Reader: hub card, list screen + VM, lesson screen + VM, routes.
7. Panel: card, manage list + reorder + delete, form + editor + preview, years screen.
8. `specs/studies/spec.md`, `specs/admin/spec.md`, build, unit tests.

## Complexity Tracking

None.
