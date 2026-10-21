# Data Model: Church Events

The wire shapes are defined in `backend/specs/020-church-events/contracts/events-api.md`. This file holds only the
app's own types.

## Public side — `features/events/`

### `EventPublicDto` (data, `@Serializable`)

`id: Int`, `title: String`, `start_date: String`, `end_date: String?`, `start_time: String?`,
`location: String?`, `description: String?`, `image_url: String`, `visibility: String`.

Snapshot body: `List<EventPublicDto>`, the bare array returned by `GET api/events/`.

### `Event` (domain)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Int` | |
| `title` | `String` | |
| `startDate` | `LocalDate` | |
| `endDate` | `LocalDate?` | `null` or equal to `startDate` = one-day event |
| `startTime` | `LocalTime?` | parsed from `"HH:MM"` |
| `location` | `String?` | blank → `null` |
| `description` | `String?` | blank → `null` |
| `imageUrl` | `String` | |
| `isMembersOnly` | `Boolean` | `visibility == "members"` |

- `lastDay = endDate ?: startDate`.
- An event is **active on** a day `d` when `lastDay >= d`.
- Mapping drops an item whose `start_date` cannot be parsed and logs it, without failing the list. An unparsable
  `start_time` maps to `null`.

### `EventHighlightUi` (presentation)

`id`, `title`, `dateLabel: String` (from `EventDateLabel`), `imageUrl`.

### `EventDetailUiState`

`isLoading`, `event: EventDetailUi?` (title, dateLabel, timeLabel?, location?, description?, imageUrl), and
`isGone: Boolean` (the id is not in the active list).

## Management side — `features/admin/events/`

### `EventRecordDto`

`EventPublicDto` plus `created_by: CreatedByDto?` (`id: String`, `name: String`), `created_at: String`,
`updated_at: String`. The list body is `EventListDto(events: List<EventRecordDto>)`.

### `EventRecord` (domain)

The `Event` fields plus `createdByName: String?` (blank → `null`), `createdAt: Instant` and `updatedAt: Instant`.

### `EventStatusFilter`

`ACTIVE("active")`, `PAST("past")`, `ALL("all")`. The default is `ALL`.

### `EventVisibility`

`PUBLIC("public")`, `MEMBERS("members")`.

### `EventDraft` (domain, form content)

| Field | Type | Local rule |
|-------|------|------------|
| `title` | `String` | required after trim; at most 120 characters |
| `startDate` | `LocalDate?` | required |
| `endDate` | `LocalDate?` | `>= startDate` when both are set |
| `startTime` | `LocalTime?` | — |
| `location` | `String` | at most 200 characters after trim; blank = none |
| `description` | `String` | blank = none |
| `visibility` | `EventVisibility` | default `PUBLIC` |

The image is held apart from the draft as `pickedImage: ByteArray?`:
- create: required;
- edit: optional, `null` keeps the current image;
- local rule: decoded bounds at least 640×360.

### `EventField`

`TITLE("title")`, `START_DATE("start_date")`, `END_DATE("end_date")`, `START_TIME("start_time")`,
`LOCATION("location")`, `DESCRIPTION("description")`, `VISIBILITY("visibility")`, `IMAGE("image")`.
`fromApiKey(key)` returns the field for a server `field_errors` key, or `null`.

### `EventChanges`

`Map<EventField, String>`, the multipart text values to send:
- **create**: every field with a value (dates `YYYY-MM-DD`, time `HH:MM`, visibility wire value);
- **edit**: only the fields that differ from the original draft; a cleared optional field becomes `""`. `TITLE`
  and `START_DATE` are never cleared, because the local rules block it.

## State transitions

- **Highlight list**:
  - starts as `Loading`;
  - becomes `Data(saved list)` after the disk preload;
  - then `Data(fresh list)` after the network, or keeps the saved list on a network failure;
  - logout empties it, and a new fetch follows.
  - An `Error` with nothing saved, or an empty filtered list, shows the "Nenhum evento esse mês" page.
- **Form**:
  - `isLoading` (edit only) → editing;
  - saving (`isSaving`, button disabled) → `Saved` event → back to the list;
  - a failure goes back to editing with `fieldErrors` / `generalError`, or to a navigation event (403 read, 404).
- **Delete**: confirmation dialog → `isDeleting` → `Deleted` event → back to the list. A 404 is treated as already
  deleted and goes back to the list with the "não existe mais" message.
