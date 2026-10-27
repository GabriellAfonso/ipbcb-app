# Data Model: Bible Videos and Reading Progress

## Domain (pure Kotlin, `features/bible/domain/model`)

- `BibleVideoKind { VIDEO, PLAYLIST }` — wire `video` / `playlist`; unknown → `VIDEO`.
- `BiblePlace(book: String, chapter: Int?)` — `book` lowercase abbrev; `chapter == null` = whole book. Equality
  ignores case of `book` by normalizing to lowercase on creation.
- `BibleVideoPlace(place: BiblePlace, position: Int)`.
- `BibleVideo(id: Long, kind, url, title, thumbnailUrl: String?, places: List<BibleVideoPlace>)`.
- `BibleVideoSections(chapter: List<BibleVideo>, book: List<BibleVideo>)` built by
  `List<BibleVideo>.sectionsFor(book, chapter)`: chapter section = videos with place (book, chapter) by position;
  book section = videos with place (book, null) by position, minus those in the chapter section.
  `isEmpty` when both empty.
- `BibleProgress(chapters: Map<String, Set<Int>>, videos: Set<Long>)` — `isRead(book, chapter)`,
  `readCount(book)`, `isBookRead(book, chapterCount)`, `isWatched(id)`, `applying(changes)`. `EMPTY`.
- `ProgressTarget` sealed: `Chapter(book, chapter)`, `Video(id)`.
- `ProgressChange(target, done: Boolean, changedAt: String)` — ISO-8601 with offset.

## Local (`features/bible/data/local`)

- `QueuedProgressChange` (`@Serializable`): `type` (`chapter`|`video`), `book?`, `chapter?`, `videoId?`, `done`,
  `changedAt`. Separate from the wire DTO.
- Snapshots: `bible_videos` (list of `BibleVideoDto`, + ETag), `bible_progress` (`BibleProgressDto`),
  `bible_progress_queue` (list of `QueuedProgressChange`).

## Wire (`features/bible/data/dto`) — see `contracts/api.md`

- `BibleVideoDto(id, kind, youtube_id, url, title, thumbnail_url?, places: List<BibleVideoPlaceDto>)`.
- `BibleVideoPlaceDto(book, chapter: Int?, position: Int)`.
- `BibleProgressDto(chapters: Map<String, List<Int>>, videos: List<Long>)`.
- `ProgressChangeWireDto(type, book?, chapter?, video_id?, done, changed_at)`; `ProgressSyncRequestDto(changes)`.
- `ProgressSyncResponseDto(accepted: List<Int>, rejected: List<RejectedChangeDto(index, reason)>)`.

## State transitions

- Pending change: created by a mark → replaces any pending change of the same target → removed when a chunk that
  contained it (same target and time) gets a 2xx or a 400 → otherwise stays (network/5xx/401 retry).
- Video: created (POST) → places replaced/removed/reordered → deleted (DELETE or last place removed).
