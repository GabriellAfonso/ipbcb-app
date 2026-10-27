# Data Model: Sunday School Lessons (app)

All domain types live in `features/studies/domain/model/SundaySchoolModels.kt` (pure Kotlin). Wire shapes:
[contracts/api.md](contracts/api.md).

## Reading

```text
SundaySchoolYear        year: Int, theme: String            label = "2026 — Tema" | "2026"
SundaySchoolIndex       years: List<SundaySchoolYear>, defaultYear: Int?, year: SundaySchoolYear?,
                        lessons: List<LessonSummary>
LessonSummary           id: Int, title, date: LocalDate?, teacher: String (blank = unknown), position: Int
Lesson                  id, title, date?, teacher, position, status: LessonStatus, year: SundaySchoolYear,
                        content: LessonContent, books: Map<String, BookFacts>
LessonStatus            DRAFT("draft") | PUBLISHED("published")
BookFacts               name, position, chapters, verses
```

## Content

```text
LessonContent           sections: List<LessonSection>
LessonSection           title, iconName: String?, blocks: List<LessonBlock>
                        icon: LessonIcon? = LessonIcon.fromWire(iconName)   // null = not drawn
LessonIcon              HEART, SCALES, SHIELD, PLANT, HANDS_PRAYING, CROWN, CROSS, BOOK_OPEN, SCROLL, FLAME,
                        BIRD, CHURCH, HOURGLASS, SWORD, MOUNTAINS, LIGHTNING   (wireName = snake_case)
LessonBlock (sealed)
  Paragraph             text, proofTexts: List<ProofText>
  BulletList            style: ListStyle (BULLET|NUMBERED|ROMAN), items: List<ListItem(text, proofTexts)>
  KeyVerse              label: String?, text, proof: ProofText
  Cards                 items: List<Card(iconName?, label?, title, text?, proofTexts)>
  Comparison            items: List<ComparisonItem(label, title, text?, highlight: Boolean)>
  Steps                 items: List<Step(label?, title, text?)>
  Table                 header: List<String>, rows: List<List<String>>
  Stats                 items: List<Stat(value, label)>
  Timeline              events: List<TimelineEvent(whenLabel, text)>
  BookFactsBlock        book, author?, date?, style?, readingTime?
  Unknown               type: String, rawJson: String            // skipped by the reader, kept by the editor
  isEditable            Paragraph | BulletList | KeyVerse
ProofText / BibleReference / VerseSpan                         // existing doctrine models, reused
```

`LessonBlock.BulletList` is named so to avoid clashing with `kotlin.collections.List`; its wire type is `list`.

`StandardSections.template()` → the six standard sections with default icons, each with one empty `Paragraph`.

## Inline text

```text
InlineSpan              text: String, bold: Boolean, italic: Boolean
InlineMarkup.parse(s)   "**a** e *b*" → [a(bold), " e ", b(italic)]; unmatched markers literal
```

## Panel

```text
SundaySchoolYearAdmin   year, theme, lessonCount, publishedCount
LessonDraft             title, year: Int, date: LocalDate?, teacher, status, content   // what the form edits
ContentError            path: String, message: String
EditorLocation (sealed) Section(s) | Block(s, b) | Form                   // from ContentError.path
SundaySchoolContentInvalid : AppError.Server   errors: List<ContentError>   // thrown by the repository
```

Rules (device-side, before sending):

- `title` non-blank; `year` in 2000..2100; texts of editable blocks non-blank (server: ≤ 5000);
- list items 1..50; a `KeyVerse` needs a printed proof;
- chip: printed non-blank; with a reference, chapter within the book and verses within the chapter.

## State

- Reader screens: `SundaySchoolUiState(isLoading, error, years, selectedYear, yearLabel, lessons, isEmpty)` and
  `SundaySchoolLessonUiState(isLoading, error, lesson, fontSize, showFontBar)`.
- Panel: `SundaySchoolManageUiState(years, selectedYear, lessons, isLoading, error, canManage, canDelete,
  isReordering)`, `SundaySchoolLessonFormUiState(draft, original, mode: EDIT|PREVIEW, isSaving, fieldErrors,
  locationErrors, topErrors, books, catalog)`, `SundaySchoolYearsUiState(years, isLoading, error, editing)`.
- One-shot events (`SharedFlow`): `ShowMessage`, `Saved`, `Deleted`, `NavigateBack`.
