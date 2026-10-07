# Data Model: Sunday Setlist — Draft, Save, Push and Play Confirmation

App-side models only. The server's Setlist object, error table and push payload are defined in
`backend/specs/017-sunday-setlist-push/contracts/` and not repeated.

## Core — `core/domain/setlist`

### `SundaySetlist`

| Field | Type | Notes |
|-------|------|-------|
| `date` | `LocalDate` | Always a Sunday (server rule) |
| `items` | `List<SetlistItem>` | Ordered by `position` (mapper sorts defensively) |
| `savedByName` | `String?` | `null` when the author account was deleted |
| `savedAt` | `String` | ISO 8601 as received; shown nowhere today, kept for logs |

### `SetlistItem`

| Field | Type | Notes |
|-------|------|-------|
| `position` | `Int` | 1–10 |
| `songId` | `Int` | |
| `title` | `String` | |
| `artist` | `String` | |
| `tone` | `String` | 1–3 chars |

**Mapping** (`core/data/setlist/SetlistMapper.kt`): `SetlistDto` → `SundaySetlist`. An unparsable `date` makes the
whole object invalid → treated as a server failure (`AppError.Unknown`), never stored.

**Stored copy**: `JsonSnapshotStorage` key `sunday_setlist`, the `SetlistDto` JSON as received (so the mapper is the
only reader). Lifecycle:

```text
(none) ──current read: setlist──▶ stored ──current read: null──▶ (none)
   ▲                                │  ▲
   │                                │  └── save success / current read: setlist (replace)
   └── logout · session lost · not a worship member · ◀──┘
```

Visibility (not a state of the file): `ObserveSundaySetlistUseCase` emits the stored setlist while
`today <= setlist.date`, `null` afterwards.

### Pure rules

- `setlistDateFor(today: LocalDate): LocalDate` — today if Sunday, else next Sunday.

## Core — `core/domain/worship`

### `WorshipAccess`

| Field | Type | Source |
|-------|------|--------|
| `isWorshipMember` | `Boolean` | `/me` `is_worship_member` (default `false`) |
| `canSaveSetlist` | `Boolean` | `/me` `can_save_setlist` (default `false`) |

`WorshipAccess.NONE` (both false) while the profile is loading, failed, or after logout.

## Core — `core/domain/push`

### `PushMessage` (sealed)

| Variant | Fields | From payload |
|---------|--------|--------------|
| `SetlistSaved` | `date: LocalDate` | `type = "setlist_saved"`, `date = YYYY-MM-DD` |
| `ConfirmPlays` | `date: LocalDate` | `type = "confirm_plays"`, `date = YYYY-MM-DD` |

Anything else (unknown type, missing/invalid date) → `null`, ignored.

### Device registration (stored)

`@PushPrefs` DataStore, key `registered_push_token`: the last token sent with success to `POST api/me/devices/`.
Written by `PushTokenSyncWorker`, read and cleared by `UnregisterDeviceUseCase` at logout.

## Core — `core/presentation/navigation`

### `NotificationTarget` (sealed)

| Variant | Extras | Destination |
|---------|--------|-------------|
| `SundaySetlist` | `target = "sunday_setlist"` | Letras list |
| `ConfirmPlays(date)` | `target = "confirm_plays"`, `date = YYYY-MM-DD` | Register screen, prefilled — or home + message without access |

## Profile — `features/profile`

- `MeProfileDto` + `@SerialName("is_worship_member") isWorshipMember: Boolean = false`,
  `@SerialName("can_save_setlist") canSaveSetlist: Boolean = false`.
- `MeProfile` + `isWorshipMember`, `canSaveSetlist` (default `false`).

## Worship hub tables — `features/worshiphub/tables`

### `RepertoireDraft` (domain)

| Field | Type | Notes |
|-------|------|-------|
| `rows` | `List<DraftRow>` | Always the four positions |
| `updatedAtMillis` | `Long` | Wall clock of the last user change |

`DraftRow(position: Int, songId: Int?, tone: String, isFixed: Boolean)`.

Rules: `DraftExpiry.isExpired(updatedAt, now) = now - updatedAt >= DRAFT_TTL_MS` (1 h); a negative difference (clock
moved back) counts as not expired. On restore, a `songId` absent from the catalog → row empty (`songId = null`,
`tone = ""`, `isFixed = false`).

Storage: `@SetlistPrefs` DataStore keys `repertoire_draft_v1` (JSON of `RepertoireDraftDto`) and
`repertoire_draft_updated_at`. Unreadable JSON → treated as no draft and cleared.

### `RepertoireRowState` (presentation, existing)

Unchanged fields: `position`, `selectedSong`, `tone`, `isFixed`.

### Repertoire UI state additions (`SongsTableViewModel`)

| Field | Type | Meaning |
|-------|------|---------|
| `canSave` | `Boolean` | `WorshipAccess.canSaveSetlist` — shows "Salvar" |
| `isSaveEnabled` | `Boolean` | `RepertoireValidation.canSave(rows) && !isSaving` |
| `isSaving` | `Boolean` | Request in flight |
| `pendingSaveDate` | `LocalDate?` | Non-null while the confirmation dialog is open |

### `RepertoireEvent` (one-shot, `SharedFlow`)

`Saved(date)`, `SaveFailed(message: String)`.

### `SaveSetlistResult`

`Saved(setlist: SundaySetlist)` | `Failed(failure: SaveSetlistFailure)`;
`SaveSetlistFailure` = `NoPermission` | `Invalid(error: AppError)` | `MissingSongs(count: Int)` | `NoConnection` |
`Other(error: AppError)`.

## Worship hub shared — `features/worshiphub/shared/domain`

### `SundaySection`

| Field | Type | Notes |
|-------|------|-------|
| `date` | `LocalDate` | For the title "Repertório de domingo dd/MM" |
| `entries` | `List<SundaySectionEntry>` | Position order; only songs with content in this list |

`SundaySectionEntry(contentId: Int, songId: Int, title: String, tone: String)` — `contentId` is the lyrics or chord
chart id opened on tap.

## Admin — `features/admin/register`

### `SetlistConfirmationRepository` (domain port)

- `suspend fun byDate(date: LocalDate): Result<SundaySetlist>` — `404` surfaces as `AppError.Server(404)`.
- `suspend fun pending(): Result<List<SundaySetlist>>` — newest first, as received.

### Register prefill (presentation)

`MusicRegistrationUiState` + `prefillDate: LocalDate?` (route argument; locks the date picker) and
`prefill: PrefillState` = `None` | `Loading` | `Loaded` | `NotFound` | `Failed(message)`.

## Admin panel — `features/admin/panel`

`AdminPanelUiState` + `pending: PendingConfirmationsUi` = `Hidden` | `Failed(message)` |
`Dates(dates: List<LocalDate>)` (never empty — empty maps to `Hidden`). No `Loading`: the card stays hidden until
the first answer.
