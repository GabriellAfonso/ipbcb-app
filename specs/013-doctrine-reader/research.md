# Research: Doctrine Reader

All Technical Context items were resolvable from the codebase; no open question remained. Each decision below lists
what was chosen, why, and what was rejected.

## R1 — One snapshot per document, created on demand

**Decision**: `DoctrineRepositoryImpl` (`@Singleton`) owns one snapshot for the list (`doctrine_list`) and one per
document slug (`doctrine_<slug>`), each a small private subclass of `BaseSnapshotRepository` built from
`SnapshotCacheFactory` + a `RetrofitSnapshotFetcher` lambda. Document snapshots live in a map keyed by slug, created the
first time the slug is asked for.

**Rationale**: Reuses the project's offline pattern (CLAUDE.md pitfall 3) with ETag/304 handled by
`RetrofitSnapshotFetcher` and the corrupt-file fallback by `LocalSnapshotCache`. Slugs come from the server list, so a
fixed Hilt map (as Bible does with `BibleTranslation`) would hardcode them.

**Alternatives**: a fixed `Map<Slug, SnapshotCache>` in a Hilt module (rejected: slugs are server data); one snapshot
holding all three documents (rejected: no such endpoint, and one document's change would refetch all).

## R2 — Show the saved copy first, then revalidate

**Decision**: Opening a screen calls `load(slug)` = `preload()` (disk → `Data`) followed by `refresh()` (ETag check).

**Rationale**: `refresh()` alone keeps the screen on `Loading` until the network answers or fails; with a slow
connection the member would wait for a check whose usual answer is 304 (SC-004). A failed check after a preload leaves
`Data` on screen (`BaseSnapshotRepository` falls back to the cache), meeting FR-023.

## R3 — One wire DTO for both document kinds

**Decision**: `DoctrineDocumentDto(slug, title, kind, questions?, chapters?)`; the mapper picks the domain type by
`kind` and returns `null` for an unknown kind.

**Rationale**: The project's `Json` has `ignoreUnknownKeys` and `explicitNulls = false`; a nullable-field DTO is
simpler than a polymorphic serializer, and the cached file is the same DTO.

**Alternatives**: `@JsonClassDiscriminator` sealed DTO (rejected: an unknown `kind` would throw on decode instead of
being filtered, FR-003).

## R4 — Opening the Bible without importing the Bible feature

**Decision**: `AppRoutes` gets `BIBLE_PASSAGE` (`bible_passage?book={book}&chapter={chapter}&verse={verse}`) and a
`biblePassage(book, chapter, verse)` builder. `bibleGraph` registers that destination; it applies the position once
(`BibleViewModel.openPassage`) and renders the same `BibleReaderScreen`. `studiesGraph` navigates with the core route.
Navigating by route into a nested graph pushes the graph entry too, so `getBackStackEntry(BIBLE_GRAPH)` keeps working.

**Rationale**: Features never import each other; the route string is the shared contract and lives in `core`. Back
pops straight to the doctrine screen, whose `LazyListState` is saved with its back-stack entry (FR-012).

**Alternatives**: a shared "pending position" holder in `core` read by the Bible on open (rejected: hidden coupling,
easy to leak a stale position into a normal Bible open); deep links (rejected: overkill inside one app).

## R5 — Scrolling to the verse after the books are decoded

**Decision**: `BibleViewModel.openPassage` saves the position and sets a `pendingScroll` (book, chapter, verse). The
reader scrolls when the books are ready **and** the current position matches the pending one, then clears it.

**Rationale**: The existing `ScrollToCurrentVerse` event fires right after `savePosition`, before DataStore emits the
new position and possibly before the translation is decoded (lazy `booksFlow`), so the scroll would hit the old or an
empty list. Matching on state removes the race.

## R6 — Text size preference

**Decision**: `DoctrinePreferences` on `@SettingsPrefs`, key `doctrine_font_size`, default 18 sp, range 14–32,
step 2.

**Rationale**: Same range and default as the Bible for familiarity, separate key (FR-020). The Bible's own font bar
lives in `features/bible` and cannot be imported, so studies gets a small `DoctrineFontSizeBar` (− value +).

## R7 — Search on the device

**Decision**: `SearchDoctrineUseCase` normalizes text (NFD, strip combining marks, lowercase) and matches when every
word of the term appears. In a catechism a purely numeric term puts that question first. The ViewModel debounces the
term (200 ms, like the hymnal) and runs on the default dispatcher.

**Rationale**: The largest document (Catecismo Maior, ~580 KB) is a few thousand short strings; a linear scan is well
under SC-006's 1 s. `java.text.Normalizer` is plain JVM, allowed in `domain/`.

## R8 — Confession titles in sentence case

**Decision**: `toSentenceCase()` in `domain`: lowercase, capitalize the first letter, and restore capitals for a fixed
word set (`deus`, `cristo`, `mediador`, `espírito`, `santo`, `sagradas`, `escrituras`, `santíssima`, `trindade`, `lei`,
`igreja`, `senhor`, `antigo`, `novo`, `testamento`). Applied in the mapper to chapter titles and to the 1.2 list
headings (printed "O ANTIGO TESTAMENTO" → "O Antigo Testamento"), so search and screens see the same text.

**Rationale**: FR-006. The 33 titles are fixed; a word set covers them without a dictionary.

## R9 — Screens and routes

**Decision**: `studiesGraph` routes: `StudiesHub`, `DoctrineList`, `DoctrineCatechism/{slug}`,
`DoctrineConfession/{slug}`, `DoctrineConfessionChapter/{slug}/{chapter}?paragraph={paragraph}`. ViewModels:
`DoctrineListViewModel`, `CatechismViewModel`, `ConfessionViewModel` (chapter list) and `ConfessionChapterViewModel`
(reader; previous/next change the chapter in state, not by pushing routes). Each reads the singleton repository, so
the document is decoded once and shared.

**Rationale**: Per-route `hiltViewModel()` with `SavedStateHandle` args is enough; nothing needs a graph-scoped VM
because the shared state is the repository's.
