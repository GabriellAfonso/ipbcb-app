# Implementation Plan: Doctrine Reader

**Branch**: `013-doctrine-reader` | **Date**: 2026-10-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/013-doctrine-reader/spec.md`

## Summary

`features/studies` gains a doctrine reader over the public backend API (`GET api/doctrine/`, `GET api/doctrine/{slug}/`).
A singleton `DoctrineRepositoryImpl` keeps one snapshot (disk + ETag) for the list and one per document, created on
demand; screens show the saved copy first and revalidate behind it. The Estudos hub becomes a card list (Doutrina +
"Em breve" placeholders), followed by the Doutrina list, a catechism reader shared by both catechisms (cards with
reference chips, search, "Ir para nº…", text size) and a confession chapter list + chapter reader. Reference chips
navigate to a new shared route `AppRoutes.BIBLE_PASSAGE`, registered in `bibleGraph`, which opens the Bible reader at
the verse.

Key technical choices (details in [research.md](research.md)):

- **Snapshot per slug** built from `SnapshotCacheFactory` + `RetrofitSnapshotFetcher` (R1); **preload, then
  revalidate** (R2).
- **One nullable-field document DTO**, unknown kinds filtered (R3).
- **Core route string** to open the Bible, no feature import (R4); **state-matched pending scroll** in the reader (R5).
- **Own text-size preference** `doctrine_font_size` (R6); **on-device accent-insensitive search** (R7); **sentence
  case** chapter titles (R8).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (JVM 17 target), Android, `minSdk 24`, `targetSdk 36`

**Primary Dependencies**: existing only — Jetpack Compose (Material 3), Navigation Compose, Hilt, Retrofit 3 +
kotlinx.serialization, DataStore. **No new library.**

**Storage**: `filesDir/snapshots/doctrine_list.json`, `doctrine_<slug>.json` (+ ETag) via `JsonSnapshotStorage`;
`doctrine_font_size` in the Settings DataStore ([contracts/on-device-storage.md](contracts/on-device-storage.md)).

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fake `SnapshotCache`/`SnapshotFetcher`, in-memory
preferences fake.

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: saved document on screen < 2 s offline (SC-004); search < 1 s on the Catecismo Maior (SC-006);
the document is decoded once per process and shared by all doctrine screens.

**Constraints**: public endpoints → `@AuthLessRetrofit`; no feature-to-feature import; `domain/` without Android;
Portuguese strings hardcoded; 120-char lines.

**Scale/Scope**: ~25 new production files in `features/studies`, 3 touched in `features/bible` and `core`; ~7 test
classes; domain spec `specs/studies/spec.md` created, `specs/bible/spec.md` updated.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ `SnapshotState.Error(AppError)` from `BaseSnapshotRepository`; unknown kind → `AppError.Unknown` built in data |
| Conversion in the data layer | ✅ `RetrofitSnapshotFetcher` / `toAppError()` |
| Screen text via `toUserMessage()` | ✅ error states use `toUserMessage()`; validation text ("Digite um número de 1 a N") is presentation copy, not an `AppError` |
| One HTTP error parsing point | ✅ untouched (`ResponseExt`) |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| UI → ViewModel → UseCase → Repository | ✅ `ObserveDoctrineListUseCase`, `ObserveDoctrineDocumentUseCase`, `SearchDoctrineUseCase` |
| Features don't import each other | ✅ Bible opened through `AppRoutes.biblePassage` (core) |
| `domain/` without Android | ✅ models + `java.text.Normalizer` only |
| Screen/Content split, dumb composables, 3 states | ✅ each screen = VM collector + content composable; loading/success/error |
| `StateFlow` + one-shot events | ✅ scroll requests as `SharedFlow` events |
| Graph-scoped VM | ✅ `BibleViewModel` from the Bible graph entry on the new route; studies VMs are per-screen (shared state lives in the repository) |
| Snapshot cache pattern | ✅ `SnapshotCacheFactory` |
| `safePopBackStack()` | ✅ |
| Tests: happy path + 1 error per use case, fakes | ✅ see quickstart |
| Spec and code together | ✅ `specs/studies/spec.md` + `specs/bible/spec.md` with the code |

**Gate: PASS**. Post-design re-check: PASS — no new library, no feature import, no manual scope.

## Project Structure

### Documentation (this feature)

```text
specs/013-doctrine-reader/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── navigation.md
│   └── on-device-storage.md
└── tasks.md
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/presentation/navigation/AppRoutes.kt            # + BIBLE_PASSAGE, biblePassage()
├── features/bible/
│   ├── presentation/navigation/BibleNavGraph.kt         # + passage destination
│   ├── presentation/viewmodel/BibleViewModel.kt         # + openPassage, pendingScroll
│   └── presentation/screens/BibleReaderScreen.kt        # + pending-scroll effect
└── features/studies/
    ├── data/
    │   ├── api/DoctrineApi.kt, DoctrineEndpoints.kt
    │   ├── dto/DoctrineDtos.kt
    │   ├── local/DoctrinePreferences.kt
    │   ├── mapper/DoctrineMapper.kt
    │   └── repository/DoctrineRepositoryImpl.kt
    ├── di/StudiesModule.kt
    ├── domain/
    │   ├── model/DoctrineModels.kt, SentenceCase.kt
    │   ├── repository/DoctrineRepository.kt, DoctrinePreferencesRepository.kt
    │   └── usecase/ObserveDoctrineListUseCase.kt, ObserveDoctrineDocumentUseCase.kt, SearchDoctrineUseCase.kt
    └── presentation/
        ├── components/ProofTextChips.kt, DoctrineFontSizeBar.kt, DoctrineSearchField.kt, DoctrineStateViews.kt
        ├── navigation/StudiesNavGraph.kt
        ├── screens/StudiesScreen.kt, DoctrineListScreen.kt, CatechismScreen.kt, ConfessionScreen.kt,
        │           ConfessionChapterScreen.kt
        └── viewmodel/DoctrineListViewModel.kt, CatechismViewModel.kt, ConfessionViewModel.kt,
                      ConfessionChapterViewModel.kt

app/src/test/java/com/ipb/castelobranco/features/studies/
├── data/dto/DoctrineDtoDecodingTest.kt
├── data/mapper/DoctrineMapperTest.kt
├── data/repository/DoctrineRepositoryImplTest.kt
├── domain/model/SentenceCaseTest.kt
├── domain/usecase/SearchDoctrineUseCaseTest.kt
└── presentation/viewmodel/CatechismViewModelTest.kt, DoctrineListViewModelTest.kt
```

**Structure Decision**: everything new lives in `features/studies` (the Estudos tab); the only shared surface is the
Bible passage route in `core`, consumed by `features/bible`.

## Complexity Tracking

No violations.
