# Data Model: Doctrine Reader

Wire format: `backend/specs/019-doctrine-documents/data-model.md` and `contracts/doctrine-api.md` (backend repo). The
app mirrors it in DTOs (`features/studies/data/dto`) and maps to the domain below (`features/studies/domain/model`).

## Wire DTOs (also the on-device snapshot format)

| DTO | Fields |
|-----|--------|
| `DoctrineListDto` | `documents: List<DoctrineSummaryDto>` |
| `DoctrineSummaryDto` | `slug`, `title`, `kind`, `item_count` |
| `DoctrineDocumentDto` | `slug`, `title`, `kind`, `questions: List<CatechismQuestionDto>?`, `chapters: List<ConfessionChapterDto>?` |
| `CatechismQuestionDto` | `number`, `question`, `answer`, `proof_texts: List<ProofTextDto>` (default empty) |
| `ProofTextDto` | `printed`, `reference: BibleReferenceDto?` |
| `BibleReferenceDto` | `book`, `chapter`, `verses: List<VerseSpanDto>` |
| `VerseSpanDto` | `start`, `end` |
| `ConfessionChapterDto` | `number`, `title`, `paragraphs: List<ConfessionParagraphDto>` |
| `ConfessionParagraphDto` | `number`, `text`, `lists: List<ParagraphListDto>?`, `text_after: String?` |
| `ParagraphListDto` | `heading`, `items: List<String>` |

## Domain

```text
DoctrineKind            CATECHISM | CONFESSION         (wire "catechism" | "confession"; anything else → dropped)

DoctrineSummary         slug, title, shortTitle, kind, itemCount
DoctrineDocument        sealed
├── Catechism           slug, title, questions: List<CatechismQuestion>
└── Confession          slug, title, chapters: List<ConfessionChapter>

CatechismQuestion       number, question, answer, proofTexts: List<ProofText>
ProofText               printed, reference: BibleReference?      (link only when reference != null)
BibleReference          book, chapter, verses: List<VerseSpan>   firstVerse = verses.firstOrNull()?.start ?: 1
VerseSpan               start, end
ConfessionChapter       number, title (sentence case), paragraphs: List<ConfessionParagraph>
ConfessionParagraph     number, text, lists: List<ParagraphList>, textAfter: String?
ParagraphList           heading, items: List<String>

DoctrineSearchResult    sealed
├── Question            number, question
└── Paragraph           chapter, paragraph, preview
```

## Rules

- **Kind filter** (FR-003): the list mapper drops summaries whose `kind` is unknown; the document mapper returns `null`
  for an unknown kind, which the repository reports as an error state (never reached from the UI because the list
  hides it).
- **Order**: list in server order; questions, chapters and paragraphs sorted by `number` (the server already sends them
  sorted; sorting is a cheap guard).
- **`shortTitle`** (FR-002): known slugs map to "Breve Catecismo", "Catecismo Maior", "Confissão de Fé"; any other
  slug uses `title` as is.
- **Item label**: catechism → "N perguntas e respostas"; confession → "N capítulos" (`N` = `itemCount`, singular when
  1).
- **Chapter title**: `toSentenceCase()` (research R8) at mapping time.
- **Answer line breaks** are kept verbatim (FR-004).

## Preferences

| Key (`@SettingsPrefs`) | Type | Default | Range |
|------------------------|------|---------|-------|
| `doctrine_font_size` | Float (sp) | 18 | 14–32, step 2 |

## State

- Repository per snapshot: `SnapshotState.Loading` → `Data` (from disk or network) | `Error` (no copy, load failed).
  `load()` = preload then revalidate; `retry` = `refresh()`.
- ViewModels expose `data class` UI states with `isLoading`/`error` flags plus content, matching the project's pattern.
