# Tasks: Doctrine Reader

**Input**: Design documents from `specs/013-doctrine-reader/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/navigation.md,
contracts/on-device-storage.md, quickstart.md

**Tests**: required by `CLAUDE.md` (happy path + 1 error per use case, fakes preferred); the list is in quickstart.md.

Paths below are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and
`app/src/test/java/com/ipb/castelobranco/` (test) unless they start with `specs/`.

Run tests with `./gradlew :app:testDebugUnitTest --tests "<package>.*"` — never `clean`, `--rerun-tasks` or
`--no-daemon`.

## Phase 1: Setup

No setup: no new library, no new module (plan §Technical Context).

## Phase 2: Foundational (blocking for every story)

**Purpose**: wire format, domain model, repository and DI used by every screen.

- [X] T001 [P] Create DTOs per data-model.md (`@Serializable`, `@SerialName` for snake_case; `proof_texts` default empty, `lists`/`text_after`/`questions`/`chapters`/`reference` nullable) in `features/studies/data/dto/DoctrineDtos.kt`
- [X] T002 [P] Create domain models (`DoctrineKind`, `DoctrineSummary` with `itemLabel`, sealed `DoctrineDocument` = `Catechism`/`Confession`, `CatechismQuestion`, `ProofText`, `BibleReference` with `firstVerse`, `VerseSpan`, `ConfessionChapter`, `ConfessionParagraph`, `ParagraphList`, sealed `DoctrineSearchResult`) in `features/studies/domain/model/DoctrineModels.kt`
- [X] T003 [P] Create `String.toSentenceCase()` with the capitalized word set of research R8 in `features/studies/domain/model/SentenceCase.kt`
- [X] T004 Create mappers `DoctrineListDto.toDomain()` (drops unknown kinds, short titles by slug) and `DoctrineDocumentDto.toDomain(): DoctrineDocument?` (null on unknown kind; sorts by number; sentence-case chapter titles; `lists` null → empty) in `features/studies/data/mapper/DoctrineMapper.kt`
- [X] T005 [P] Create `DoctrineEndpoints` (`doctrine/`, `doctrine/{slug}/` under `ApiConstants.BASE_PATH`) and `DoctrineApi` (`getDocuments(etag)`, `getDocument(slug, etag)` with `If-None-Match`) in `features/studies/data/api/`
- [X] T006 Create `DoctrineRepository` interface (`observeList()`, `loadList()`, `refreshList()`, `observeDocument(slug)`, `loadDocument(slug)`, `refreshDocument(slug)`) in `features/studies/domain/repository/DoctrineRepository.kt`
- [X] T007 Implement `DoctrineRepositoryImpl` (`@Singleton`): private `DoctrineSnapshot` subclass of `BaseSnapshotRepository`; list snapshot key `doctrine_list`; per-slug snapshots `doctrine_<slug>` created on demand in a synchronized map; `load*` = `preload()` then `refresh()` (research R1, R2); document mapper `null` → `SnapshotState.Error(AppError.Unknown)` in `features/studies/data/repository/DoctrineRepositoryImpl.kt`
- [X] T008 Create `StudiesModule` (Hilt: bind repository, provide `DoctrineApi` from `@AuthLessRetrofit`, bind preferences repository) in `features/studies/di/StudiesModule.kt`
- [X] T009 [P] Create use cases `ObserveDoctrineListUseCase` and `ObserveDoctrineDocumentUseCase` (observe + load + refresh) in `features/studies/domain/usecase/`
- [X] T010 [P] Test DTO decoding of real-shaped JSON (catechism with null reference, confession 1.2 with lists + text_after, unknown extra keys) in `features/studies/data/dto/DoctrineDtoDecodingTest.kt`
- [X] T011 [P] Test mappers (unknown kind dropped/null, short titles, item labels, sorting, sentence case applied) in `features/studies/data/mapper/DoctrineMapperTest.kt` and `toSentenceCase()` on the 33 shapes in `features/studies/domain/model/SentenceCaseTest.kt`
- [X] T012 Test repository with fake cache/fetcher: preload shows saved copy before network; 304 keeps copy; 200 replaces copy + ETag; failure with copy → Data; failure without copy → Error; slugs isolated in `features/studies/data/repository/DoctrineRepositoryImplTest.kt`

**Checkpoint**: data ready — screens can start.

## Phase 3: User Story 1 — Read a catechism (P1) 🎯 MVP

**Goal**: Estudos hub → Doutrina → catechism reader with question cards and reference chips.

**Independent test**: quickstart §Manual 1–3 (without tapping a chip).

- [X] T013 [P] [US1] Create shared components: `ProofTextChips` (FlowRow; linked chip vs plain non-clickable chip), `DoctrineErrorView` (message + "Tentar novamente"), `DoctrineLoadingView` in `features/studies/presentation/components/`
- [X] T014 [US1] Rewrite `StudiesScreen` as the hub (Doutrina card + three disabled "Em breve" cards, mockup copy) with Screen/Content split in `features/studies/presentation/screens/StudiesScreen.kt`
- [X] T015 [US1] Create `DoctrineListViewModel` (UI state with `isLoading`, `error`, `documents`; `retry()`) in `features/studies/presentation/viewmodel/DoctrineListViewModel.kt`
- [X] T016 [US1] Create `DoctrineListScreen` (cards with count badge, short title, item label) in `features/studies/presentation/screens/DoctrineListScreen.kt`
- [X] T017 [US1] Create `CatechismViewModel` (slug from `SavedStateHandle`; state with title, questions, loading, error; `retry()`) in `features/studies/presentation/viewmodel/CatechismViewModel.kt`
- [X] T018 [US1] Create `CatechismScreen` (LazyColumn of question cards: "PERGUNTA N", question, answer keeping `\n`, "REFERÊNCIAS" + chips) in `features/studies/presentation/screens/CatechismScreen.kt`
- [X] T019 [US1] Add routes `DoctrineList`, `DoctrineCatechism/{slug}` (and placeholders for the confession routes) to `features/studies/presentation/navigation/StudiesNavGraph.kt`
- [X] T020 [P] [US1] Test `DoctrineListViewModel` (loading → data; error → retry calls refresh) in `features/studies/presentation/viewmodel/DoctrineListViewModelTest.kt`

**Checkpoint**: both catechisms readable.

## Phase 4: User Story 2 — Open a reference in the Bible (P1)

**Goal**: tapping a linked chip opens the Bible reader at the verse; back returns to the same place.

**Independent test**: quickstart §Manual 3.

- [X] T021 [US2] Add `BIBLE_PASSAGE`, arg name constants and `biblePassage(book, chapter, verse)` (URL-encoded book) to `core/presentation/navigation/AppRoutes.kt` (contracts/navigation.md)
- [X] T022 [US2] Add `openPassage(book, chapter, verse)` and `pendingScroll: StateFlow<VerseRef?>` + `consumePendingScroll()` to `features/bible/presentation/viewmodel/BibleViewModel.kt` (research R5)
- [X] T023 [US2] In `features/bible/presentation/screens/BibleReaderScreen.kt` scroll to the pending verse when books are ready and the position matches it, then consume
- [X] T024 [US2] Register the passage destination in `features/bible/presentation/navigation/BibleNavGraph.kt` (graph-scoped VM; apply once per entry with `rememberSaveable`)
- [X] T025 [US2] Wire chip taps in `StudiesNavGraph.kt` → `navController.navigate(AppRoutes.biblePassage(...))` using `BibleReference.firstVerse`
- [X] T026 [P] [US2] Test `openPassage` saves the position and sets/consumes the pending scroll in `features/bible/presentation/viewmodel/BibleViewModelTest.kt` (region openPassage)

## Phase 5: User Story 3 — Read the Confession (P2)

**Goal**: chapter list + chapter reader with 1.2 lists and previous/next.

**Independent test**: quickstart §Manual 6.

- [X] T027 [US3] Create `ConfessionViewModel` (chapter list state) in `features/studies/presentation/viewmodel/ConfessionViewModel.kt`
- [X] T028 [US3] Create `ConfessionScreen` (header with full title and "N capítulos", rows number + title) in `features/studies/presentation/screens/ConfessionScreen.kt`
- [X] T029 [US3] Create `ConfessionChapterViewModel` (slug, chapter, paragraph from `SavedStateHandle`; current chapter in state; `next()`/`previous()`) in `features/studies/presentation/viewmodel/ConfessionChapterViewModel.kt`
- [X] T030 [US3] Create `ConfessionChapterScreen` (paragraphs with number, text, lists with heading + items, text after; previous/next bar) in `features/studies/presentation/screens/ConfessionChapterScreen.kt`
- [X] T031 [US3] Add confession routes to `StudiesNavGraph.kt`; Doutrina list routes by kind

## Phase 6: User Story 4 — Find a question or passage (P2)

**Goal**: in-document search and "Ir para nº…".

**Independent test**: quickstart §Manual 4–5.

- [X] T032 [US4] Create `SearchDoctrineUseCase` (normalize NFD + strip marks + lowercase; all words; numeric first in catechism; paragraph previews) in `features/studies/domain/usecase/SearchDoctrineUseCase.kt`
- [X] T033 [P] [US4] Test search (accents, word order, numeric first, list items in confession, no result) in `features/studies/domain/usecase/SearchDoctrineUseCaseTest.kt`
- [X] T034 [P] [US4] Create `DoctrineSearchField` and `DoctrineSearchResults` components in `features/studies/presentation/components/DoctrineSearch.kt`
- [X] T035 [US4] Add query (debounced 200 ms, default dispatcher), results, `goTo(number)` validation ("Digite um número de 1 a N") and scroll events to `CatechismViewModel`; search toggle, results, "Ir para nº…" dialog and position footer to `CatechismScreen`
- [X] T036 [US4] Add search to `ConfessionViewModel`/`ConfessionScreen` (result → chapter route with paragraph) and to `ConfessionChapterViewModel`/`ConfessionChapterScreen` (result → switch chapter + scroll)
- [X] T037 [P] [US4] Test `CatechismViewModel` go-to (valid → scroll event; out of range / not a number → message, no event) in `features/studies/presentation/viewmodel/CatechismViewModelTest.kt`

## Phase 7: User Story 5 — Read offline (P2)

**Goal**: saved copies with ETag revalidation (delivered by T007; this phase verifies screens).

- [X] T038 [US5] Make every doctrine screen call `load*` on open and keep content on screen during revalidation; error view only without content (check the four ViewModels)

## Phase 8: User Story 6 — Adjust text size (P3)

**Goal**: "Aa" control shared by all readers, persisted.

**Independent test**: quickstart §Manual 7.

- [X] T039 [P] [US6] Create `DoctrinePreferencesRepository` (domain interface: `fontSizeFlow`, `setFontSize`) and `DoctrinePreferences` (`@SettingsPrefs`, key `doctrine_font_size`, 14–32, default 18, step 2) in `features/studies/domain/repository/` and `features/studies/data/local/DoctrinePreferences.kt`
- [X] T040 [P] [US6] Create `DoctrineFontSizeBar` (− value +, ends disabled) in `features/studies/presentation/components/DoctrineFontSizeBar.kt`
- [X] T041 [US6] Expose font size + `increaseFont()`/`decreaseFont()` in the catechism and chapter ViewModels; apply size to answer/paragraph text and show the bar from the "Aa" action
- [X] T042 [P] [US6] Test font steps and limits in `CatechismViewModelTest.kt`

## Phase 9: Polish & Cross-Cutting

- [X] T043 Create domain spec `specs/studies/spec.md` (screens, endpoints, storage, rules) and add the passage route to `specs/bible/spec.md`
- [X] T044 Run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.studies.*"` and `... features.bible.*`, then `:app:compileDebugKotlin`; fix failures

## Dependencies

- Phase 2 blocks everything. US1 before US2 (chips live in the catechism screen) and US4/US6 (they extend the
  catechism screen). US3 depends only on Phase 2; US4's confession part depends on US3. US5 is verification over
  US1/US3. Polish last.

## Parallel examples

- Phase 2: T001, T002, T003, T005 together; then T010, T011 together.
- US1: T013 with T015; T020 after T015.
- US4: T033 and T034 together after T032.
- US6: T039 and T040 together.

## Implementation strategy

MVP = Phase 2 + US1 + US2 (read both catechisms and open the references). Then US3 (Confession), US4 (search/go-to),
US6 (text size), verification of US5, polish.
