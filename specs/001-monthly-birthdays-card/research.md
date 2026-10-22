# Research: Monthly Birthdays Highlight Card

## R1: API Response Format

**Decision**: Wrap birthdays in a `BirthdaysResponseDto` with a `birthdays` list field, matching the API contract `{ "birthdays": [...] }`.

**Rationale**: API returns a wrapper object, not a raw list. Matches documented contract. The DTO must use `@Serializable` with `kotlinx.serialization` (project standard — no Gson/Moshi).

**Alternatives considered**: Raw `List<BirthdayDto>` — rejected because API wraps in `{ "birthdays": [] }`.

## R2: Snapshot Cache Key Strategy

**Decision**: Use `"birthdays_month_{month}"` as the snapshot cache key (e.g., `"birthdays_month_6"`).

**Rationale**: Monthly data changes each month. Using a month-specific key ensures stale data from a previous month is not shown. On month change, the old key's cache becomes unused but harmless (small file, overwritten next time that month is current).

**Alternatives considered**:
- Single `"birthdays"` key — rejected because switching months would show stale data until network refresh.
- Clearing cache on month change — unnecessary complexity; per-month keys are simpler.

## R3: Month Parameter Source

**Decision**: Use `java.time.LocalDate.now().monthValue` to get current month (1-12). Inject a `Clock`-like provider for testability.

**Rationale**: `java.time` is available on API 26+ (project minimum). Simple, no extra dependency.

**Alternatives considered**:
- `Calendar.getInstance()` — older API, more verbose.
- Inject month as parameter — overengineered for current scope (no month selector).

## R4: Snapshot Integration with Preload System

**Decision**: Register `MembersRepository` as both `Preloadable` and `Refreshable` in `MembersModule`, following `ScheduleModule` pattern.

**Rationale**: Birthday data should load from disk cache on app boot (preload) and refresh from network afterward. Existing `PreloadDataUseCase` handles both via `Set<Preloadable>` and `Set<Refreshable>` multibindings.

**Alternatives considered**: Manual loading in CoreViewModel — breaks the centralized preload pattern.

## R5: HighlightBirthdays Card Design

**Decision**: Visual card with title header, cake emoji, and a vertical list of name + day entries. Visually distinct from the table-style schedule card — uses softer spacing and no table headers.

**Rationale**: Spec requires "cleaner and more visually polished than the schedule highlight card". A simple vertical list with day badges and names achieves this without complex layout.

**Alternatives considered**: Grid layout — rejected because birthdays are naturally a list (name + day pairs).

## R6: Empty State Handling

**Decision**: When `SnapshotState.Data` contains an empty list, show the existing `HighlightPlaceholder` with cake emoji and "Nenhum aniversariante esse mês". When `SnapshotState.Loading` or `SnapshotState.Error`, also show the placeholder (non-critical feature, no error UI needed).

**Rationale**: Birthdays are a nice-to-have highlight. Showing an error state for this card would degrade the home screen experience. Silent fallback to placeholder is appropriate.

**Alternatives considered**: Show loading spinner — unnecessary for a non-primary feature that loads in <2s from cache.

## R7: API Endpoint Does Not Support ETag

**Decision**: The birthdays API does not support ETag/304 caching (no `If-None-Match` header needed). The `RetrofitSnapshotFetcher` handles this gracefully — if no ETag is returned, it simply never sends `If-None-Match` and always gets fresh data.

**Rationale**: The existing `RetrofitSnapshotFetcher` already handles null ETags. No special code needed.

**Alternatives considered**: N/A — the existing infrastructure handles this.
