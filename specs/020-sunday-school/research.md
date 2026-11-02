# Research: Sunday School Lessons

Decisions taken while planning, each with its reason. The product decisions (the app's own look, Phosphor duotone,
basic editor, no PDF, chips like the catechism) were taken in session `2041feef-c82e-463c-b6c8-917c81b7498b` and
are not repeated here.

## R-01 — Where the code lives: `features/studies`, management included

**Decision**: reader, editor, preview, year themes and the reorder all live in `features/studies`. The panel card
(`features/admin/panel`) navigates to `AppRoutes.SUNDAY_SCHOOL_MANAGE`, a destination registered by
`studiesGraph`.

**Rationale**: the preview must use the exact renderer of the reader (FR-020), and the editor reuses the domain
models (`ProofText`, `BibleReference`) and `ProofTextChips` that already live in `features/studies`. Features never
import each other; putting the editor in `features/admin` would force the renderer and models into `core/`. The
panel already opens destinations owned by other features through `AppRoutes` (`sundayRegister`, worship hub), and
bible videos are managed inside `features/bible`.

**Alternatives**: `features/admin/sundayschool` with the renderer in `core/presentation` — more code in core for a
single domain, rejected.

## R-02 — Content model: typed blocks plus `Unknown(rawJson)`

**Decision**: the content JSON is decoded in the data layer block by block (`JsonObject` per block, dispatched on
`type`) into a sealed `LessonBlock` with one subtype per known type (all ten, every field) and
`LessonBlock.Unknown(type, rawJson: String)`. An unknown icon name becomes `null` for drawing but the original
name is kept (`LessonIcon` + `iconName`) so a save sends it back. Encoding writes every known block from its typed
model and `Unknown` from its raw JSON.

**Rationale**: the reader must skip unknown types without failing (FR-006), and the editor must send the
non-editable and unknown blocks back unchanged (FR-019). Every field of the ten types is modelled, so re-encoding
is lossless; the raw JSON text keeps `domain/` free of serialization types. A block of a known type that fails to
decode (newer server added a required field shape) is also turned into `Unknown`, never a crash.

**Alternatives**: polymorphic `@Serializable` sealed class with a default — the global `Json` would need a
`SerializersModule` and loses unknown fields; rejected.

## R-03 — Snapshots

**Decision**: `SnapshotCacheFactory` keys `sunday_school_index` (request without `year` — the source of the year
tabs and the default year), `sunday_school_index_<year>` (any other year), `sunday_school_lesson_<id>`; each with
its ETag, preload then refresh like doctrine. The lesson and year snapshots are built on demand per id/year (same
`DoctrineSnapshotSources` shape).

A 404 on a lesson (`SUNDAY_SCHOOL_LESSON_NOT_FOUND`) or on a year (`SUNDAY_SCHOOL_YEAR_NOT_FOUND`) clears that
snapshot and emits an error with app copy ("Esta aula não está mais disponível" / "Este ano não tem mais aulas
publicadas"); the base class would otherwise keep showing the stale copy. Implemented in the repository's snapshot
subclass by remembering the fetch failure and calling `clearCache()` + `emitError()`.

**Rationale**: matches FR-012 and the edge cases; public content, not encrypted, not cleared at logout.

## R-04 — Text size: shared with doctrine

**Decision**: the reader uses `DoctrinePreferencesRepository` (`doctrine_font_size`), the existing
`DoctrineFontSize` and `DoctrineFontSizeBar`.

**Rationale**: one "reading size" for the Estudos tab; a member who enlarged the catechism wants the lessons the
same. The Bible keeps its own size (it already does).

## R-05 — Fonts: the app's Poppins and Inter

**Decision**: titles in `displayFontFamily` (Poppins) and text in `bodyFontFamily` (Inter), both from
`core/presentation/theme/Type.kt`. The feature bundles no font of its own. Inter has no italic file, so `*italic*`
uses Compose's synthesized slant.

**Rationale**: chosen in the design round of 02/11/2026 together with R-07. The EBD reads as part of the app, and
the APK does not carry two more font families.

## R-06 — Icons: 16 Phosphor duotone vector drawables

**Decision**: `res/drawable/ic_ph_<name>.xml` for the 16 names of the closed list, converted from the Phosphor core
duotone SVGs (MIT; `viewBox 0 0 256 256`). The duotone background path keeps `fillAlpha="0.2"`; every path is
`fillColor="#FF000000"` so Compose `Icon(tint = gold)` draws gold with a 20 % gold background — the duotone look.
`LessonIcon` (domain enum, wire names) → drawable in presentation (`SundaySchoolIcons.kt`). A header comment in each
XML names the source (Phosphor Icons) and the MIT licence.

Phosphor names: `heart`, `scales`, `shield`, `plant`, `hands-praying`, `crown`, `cross`, `book-open`, `scroll`,
`flame`, `bird`, `church`, `hourglass`, `sword`, `mountains`, `lightning`.

**Rationale**: no library dependency (user decision), tint follows light/dark.

## R-07 — The app's colors

**Decision**: `SundaySchoolColors` reads the Material color scheme and holds no color of its own, so the EBD
follows the same tokens as Tabelas and Escala in light and dark:

| Role | Source |
|---|---|
| list sheet | `surfaceDim` |
| reader page | `surface` |
| text / muted text | `onSurface` / `onSurfaceVariant` |
| cards, year header, lesson header | `surfaceContainer`, no border |
| labels, icons, numbers, stat values | `primary` |
| "ANO 2026" overline | `outline`, like the "Próximo" card of Escala |
| number ring, timeline dots, step arrows, highlighted comparison border | `nextRowColors().stripe` |
| key verse and highlighted comparison background | `nextRowColors().background` |
| key verse label | `nextRowColors().text` |
| rules and table borders | `outlineVariant` |
| table header | `surfaceContainerHigh` |
| icon badge | `secondaryContainer` |

`nextRowColors()` is the upcoming-date green of Escala, shared from `core/presentation/components`. No texture is
drawn. Body text keeps a contrast of at least 7:1 in both modes (SC-006).

Chosen in the design round of 02/11/2026 (option A, "Igual a Tabelas e Escala") over strong green bands and green
with the brand's gold: the feature had drifted from the rest of the app and its reference chips, already green,
clashed with the parchment palette it used before.

## R-08 — Inline formatting

**Decision**: a pure parser in `domain/` (`InlineMarkup.parse(text): List<InlineSpan>`) recognizes `**bold**` and
`*italic*` (bold first, no nesting across the other marker); unmatched markers stay literal. Presentation builds an
`AnnotatedString`. Tested on the JVM.

## R-09 — Panel data is not persisted

**Decision**: management uses an `@AuthedRetrofit` API and in-memory `Result<T>` calls only (FR-025), like
`EventsAdminRepository`. After a write the public snapshots of that year (and of the lesson) are refreshed so the
leader sees the change in the reader.

## R-10 — Content errors to fields

**Decision**: the repository reads `AppError.Server.extras["errors"]` (JSON text already extracted by
`ResponseExt`) into `List<ContentError(path, message)>` and throws a `SundaySchoolContentInvalid` carrying them. A
pure mapper turns a path into an editor location: `sections.<s>.title|icon` → section, `sections.<s>.blocks.<b>…`
→ block (any deeper part, e.g. `items.2.proof_texts.0.reference`, stays inside that block's message list);
anything else → top of the form. `VALIDATION_ERROR` `field_errors` (`title`, `year`, `date`, `teacher`) go to the
form fields.

## R-11 — Reference editor and the Bible book list

**Decision**: a new core port `core/domain/bible/BibleBookCatalog` (`books: Flow<List<BibleBookInfo>>` with
`abbrev`, `name`, `verseCounts`) implemented in `features/bible` from `BibleRepository.booksFlow` and bound in its
DI module. The chip dialog has: printed text, book dropdown (names), chapter, verses ("1-3,5"; empty = whole
chapter), and a "Sem referência" switch for an inert chip. Chapter/verse bounds are checked on the device. The
reference `book` is the Bible display abbreviation (`gn` → `Gn`, `1tn` → `1Tn`), which is what the server's
`ScriptureIndex` matches (case-sensitive). With no catalog loaded the book becomes a free-text abbreviation field
and the server validates.

**Rationale**: avoids most server errors (spec assumption) without the feature importing `features/bible`.

## R-12 — Editing structure

**Decision**: the form is one screen with two tabs, "Editar" and "Pré-visualizar", both over the same draft in
`SundaySchoolLessonFormViewModel` (no draft passed through navigation). Sections and blocks move with up/down
buttons in their menu; lessons in the list reorder by long-press drag (`sh.calvin.reorderable`, already used),
saving the full order on drop. A stale order (`SUNDAY_SCHOOL_ORDER_MISMATCH`) reloads the list with
"A lista mudou; tente de novo".

## R-13 — Year tabs and theme

**Decision**: `GreenTabBar` (`core/presentation/components`) in `BaseScreen.topBarExtension`, with the year only
(themes are long), and the list on a `surfaceDim` sheet with rounded top corners over the green, like Tabelas and
Escala. Above the list, a year header on `surfaceContainer`: "ANO 2026", the theme, "18 aulas". Tabs newest first, as the
server sends; none when there is a single year. Chosen in the design round of 28/10/2026 (option 1, "cartões
numerados") over a timeline and a highlight-plus-compact-list layout.

## R-13a — Lesson cards

**Decision**: each lesson is a card (`surfaceContainer`, no border, 14 dp corners) with its position + 1 in a 42 dp
ringed circle, the title in the title font, the date "dd/MM/yyyy · Prof. X" below and a chevron. The number tells
the order, which matters in a year that walks through the Bible.

## R-13b — Table column widths

**Decision**: a `table` block fills the width, each column taking its share of the text (its longest cell,
clamped to 6..60 characters), but never narrower than its longest word; a column that would be narrower keeps that
minimum and the others share the rest. It scrolls sideways only when the minimums alone exceed the width. Fixed
140 dp columns made Josué's "Capítulos" (1–12) as wide as the description and forced a needless scroll.

## R-14 — Unsaved changes and new lessons

**Decision**: the form compares the draft with the loaded/initial lesson; back with changes asks "Descartar
alterações?". A new lesson takes the selected year, status Rascunho, and the six standard sections each with one
empty paragraph (the server rejects blank text, so the leader fills or deletes it).
