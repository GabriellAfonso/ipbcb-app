# Contract: Navigation

## Studies graph (`studiesGraph`, `features/studies/presentation/navigation/StudiesNavGraph.kt`)

| Route | Screen | Args |
|-------|--------|------|
| `StudiesHub` (start) | Estudos hub | — |
| `DoctrineList` | Doutrina (documents) | — |
| `DoctrineCatechism/{slug}` | Catechism reader | `slug` |
| `DoctrineConfession/{slug}` | Confession chapter list | `slug` |
| `DoctrineConfessionChapter/{slug}/{chapter}?paragraph={paragraph}` | Confession chapter reader | `slug`, `chapter` (Int), `paragraph` (Int, default 0 = top) |

Back on every screen uses `safePopBackStack()`.

## Bible passage (shared, `core/presentation/navigation/AppRoutes.kt`)

```kotlin
AppRoutes.BIBLE_PASSAGE          // "bible_passage?book={book}&chapter={chapter}&verse={verse}"
AppRoutes.biblePassage(book, chapter, verse)   // builds the concrete route, book URL-encoded
```

- Registered inside `bibleGraph` (`BibleNavGraph.kt`), rendering `BibleReaderScreen` with the graph-scoped
  `BibleViewModel`.
- On first composition (once per back-stack entry, survives rotation) it calls
  `BibleViewModel.openPassage(book, chapter, verse)`: saves the position (normal reading, FR-013) and asks the reader
  to scroll to `verse` once the chapter is on screen.
- `book` is the Bible `abbrev` from the doctrine reference (case-insensitive match, as the reader already does).
- `verse` = first verse of the first span, or `1` for a whole-chapter reference.
- Back pops to the caller; the Bible index opened from this reader works as from `BibleReader`.
