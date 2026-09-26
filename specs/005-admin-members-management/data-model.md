# Data Model: Members Management for Church Leaders

**Feature**: `005-admin-members-management` | **Date**: 2026-09-26

Wire shapes are in [contracts/admin-members-api.md](contracts/admin-members-api.md). This file covers the app's
own types.

---

## Domain (`features/admin/members/domain/model/`)

No Android types. Dates are `java.time.LocalDate` / `Instant` (desugaring is on, `minSdk 24`).

### `NamedRef`

| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | |
| `name` | `String` | Shown verbatim — status, role and ministry names are never hardcoded |

### `Gender`

`enum class Gender(val apiCode: String, val label: String) { MALE("M", "Masculino"), FEMALE("F", "Feminino") }`
— `null` means not informed.

### `BirthDate.kt` (year-unknown convention)

`const val UNKNOWN_BIRTH_YEAR = 1` and `fun LocalDate.hasUnknownYear(): Boolean = year == UNKNOWN_BIRTH_YEAR` —
the only place that knows the church stores "birthday known, year unknown" as `0001-MM-DD` (research R10). Age,
display, validation and the form all ask this predicate.

### `MemberSummary` (list card)

| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | |
| `name` | `String` | |
| `photoUrl` | `String?` | Absolute URL; `null` → initials |
| `status` | `NamedRef?` | `null` → "Sem situação" |
| `isValid` | `Boolean` | wire `is_active` |

### `MemberRecord` (profile)

| Field | Type | Notes |
|---|---|---|
| `id` | `Int` | |
| `name` | `String` | display name |
| `firstName`, `lastName` | `String` | `""` = not informed |
| `birthDate` | `LocalDate?` | |
| `gender` | `Gender?` | |
| `status`, `role` | `NamedRef?` | |
| `ministries` | `List<NamedRef>` | server order (by name) |
| `baptismDate` | `LocalDate?` | |
| `isValid` | `Boolean` | |
| `photoUrl` | `String?` | |
| `createdAt` | `Instant` | |

`fun MemberRecord.toSummary(): MemberSummary` — used to patch the in-memory list after a write.

### `MemberOptions`

`statuses`, `roles`, `ministries`: `List<NamedRef>`.

### `MemberDraft` (form working copy)

| Field | Type | Default (create) |
|---|---|---|
| `id` | `Int?` | `null` = create |
| `name` | `String` | `""` |
| `firstName`, `lastName` | `String` | `""` |
| `birthDate`, `baptismDate` | `LocalDate?` | `null` — a year-unknown birth date is `LocalDate.of(1, m, d)` |
| `gender` | `Gender?` | `null` |
| `statusId`, `roleId` | `Int?` | `null` |
| `ministryIds` | `Set<Int>` | empty |
| `isValid` | `Boolean` | `true` |

`fun MemberRecord.toDraft(): MemberDraft`.

### `MemberField`

`enum` of the editable fields — `NAME, FIRST_NAME, LAST_NAME, BIRTH_DATE, GENDER, STATUS, ROLE, MINISTRIES,
BAPTISM_DATE, IS_VALID` — each with its API key (`name`, `first_name`, `last_name`, `birth_date`, `gender`,
`status_id`, `role_id`, `ministry_ids`, `baptism_date`, `is_active`). `fromApiKey(key)` maps a server
`field_errors` key back to a field; unknown → `null` (general message).

### `MemberChanges`

`data class MemberChanges(val values: Map<MemberField, Any?>)` — only the fields to send; a `null` value means
"clear". Built by `MemberDraft.changesFrom(original: MemberDraft?)`:

- create (`original == null`): every field with a value (non-blank strings, non-null, non-empty set), plus
  `IS_VALID` only when `false`;
- edit: exactly the fields whose value differs; `isEmpty` → nothing is sent (FR-019).

Strings are trimmed before comparing and sending.

### `HistoryEntry` / `HistoryLine`

| `HistoryEntry` | Type |
|---|---|
| `id` | `Int` |
| `editor` | `HistoryEditor?` (`id: String`, `name: String`) |
| `field` | `String` |
| `oldValue`, `newValue` | `String?` |
| `changedAt` | `Instant` |

`HistoryLine(editorName: String, text: String, changedAt: Instant)` — built by `BuildHistorySentenceUseCase`
(research R9).

### `DeleteConfirmation`

Pure rule, `fun matches(typed: String, memberName: String): Boolean =
typed.trim().equals(memberName.trim(), ignoreCase = true)`.

---

## Repository (`domain/repository/MembersAdminRepository.kt`)

```kotlin
interface MembersAdminRepository {
    fun observeMembers(): Flow<List<MemberSummary>?>      // null = not loaded yet
    suspend fun refreshMembers(): Result<Unit>
    suspend fun getMember(id: Int): Result<MemberRecord>
    suspend fun getOptions(): Result<MemberOptions>
    suspend fun createMember(changes: MemberChanges): Result<MemberRecord>
    suspend fun updateMember(id: Int, changes: MemberChanges): Result<MemberRecord>
    suspend fun deleteMember(id: Int): Result<Unit>
    suspend fun uploadPhoto(id: Int, bytes: ByteArray, mimeType: String): Result<String>   // new photo URL
    suspend fun removePhoto(id: Int): Result<Unit>
    suspend fun getHistory(id: Int): Result<List<HistoryEntry>>
    suspend fun clear()
}
```

Failures are always `AppError` (constitution). 404 on a member route carries
`userMessage = "Este membro não existe mais"` and the member is dropped from the in-memory list.

---

## Use cases (`domain/usecase/`)

| Use case | Does |
|---|---|
| `ObserveMembersUseCase` | `observeMembers()` |
| `RefreshMembersUseCase` | `refreshMembers()` |
| `GetMemberUseCase` | record by id |
| `GetMemberOptionsUseCase` | picker options |
| `ValidateMemberDraftUseCase` | local rules → `Map<MemberField, String>` (research R7) |
| `SaveMemberUseCase` | `changesFrom` → create or update; empty changes → returns the current record, no call |
| `SetMemberValidityUseCase` | update with `IS_VALID` only |
| `DeleteMemberUseCase` | delete |
| `ValidateMemberPhotoUseCase` | non-empty, ≤ 10 MB, JPEG/PNG/WEBP/GIF by magic bytes → mime type or error |
| `UploadMemberPhotoUseCase` / `RemoveMemberPhotoUseCase` | photo writes |
| `GetMemberHistoryUseCase` | entries → `List<HistoryLine>` via `BuildHistorySentenceUseCase` |
| `BuildHistorySentenceUseCase` | research R9 |
| `ComputeMemberAgeUseCase` | age and "há N anos" (research R10) |

---

## Presentation state (`presentation/state/`)

### `MembersListUiState`

| Field | Type |
|---|---|
| `isLoading` | `Boolean` |
| `error` | `String?` |
| `query` | `String` |
| `members` | `List<MemberCardUi>` (filtered) |
| `totalCount` | `Int` (to tell "empty roll" from "no search result") |

`MemberCardUi(id, name, initials, photoUrl, statusLabel, isValid)`.

### `MemberProfileUiState`

`isLoading`, `error`, `profile: MemberProfileUi?`, `lastChange: HistoryLine?`, `isSavingValidity`,
`isPhotoBusy`, `showRemovePhotoDialog`, `showDeleteDialog`, `deleteTyped`, `canConfirmDelete`, `isDeleting`.
`MemberProfileUi` carries preformatted strings: name, initials, `ageLabel`, `genderLabel`, `statusLabel`,
`roleLabel`, `birthDateLabel`, `baptismLabel` ("12/06/2005 · há 21 anos"), `ministries: List<String>`,
`createdAtLabel`, `isValid`, `photoUrl`.

### `MemberFormUiState`

`isEditing`, `isLoading`, `loadError`, `draft: MemberDraft`, `options: MemberOptions?`,
`fieldErrors: Map<MemberField, String>`, `generalError: String?`, `isSaving`, `hasUnsavedChanges`.

### `MemberHistoryUiState`

`isLoading`, `error`, `lines: List<HistoryLineUi>` (editor, text, `dd/MM/yyyy HH:mm`).

### Events (sealed, one-shot)

```kotlin
sealed interface MembersEvent {
    data class ShowMessage(val message: String) : MembersEvent
    data class LeaveArea(val message: String) : MembersEvent     // 403
    data class MemberGone(val message: String) : MembersEvent    // 404
    data class Saved(val memberId: Int) : MembersEvent           // form → profile
    data object Deleted : MembersEvent                           // profile → list
}
```

---

## State transitions

- Record: create → (edit | validity | photo)* → delete. No other states.
- Photo: none ↔ present; replace keeps present with a new URL.
- In-memory list: `null` (not loaded) → loaded → patched by writes → `null` again on `clear()` (sign-out).
