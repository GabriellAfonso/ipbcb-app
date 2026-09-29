# Data Model: Role-Scoped Permissions in the App

## 1. Wire — `MeProfileDto` (`features/profile/data/dto/`)

| JSON          | Kotlin        | Type                    | Default       | Note                                        |
|---------------|---------------|-------------------------|---------------|---------------------------------------------|
| `name`        | `name`        | `String`                | —             | unchanged                                   |
| `is_member`   | `isMember`    | `Boolean`               | —             | unchanged                                   |
| `photo_url`   | `photoUrl`    | `String?`               | `null`        | unchanged                                   |
| `roles`       | `roles`       | `List<RoleDto>`         | `emptyList()` | new; default makes an old cache read as "no role" |
| `permissions` | `permissions` | `Map<String, String?>`  | `emptyMap()`  | new; raw strings, never fails on an unknown value |
| `is_admin`    | —             | —                       | —             | removed; ignored if present (old cache)     |

`RoleDto(id: String, name: String)` — `name` is not used by the app (FR-008) but is kept so the cache stays a
faithful copy of the response.

## 2. Domain — `core/domain/access/`

No Android types.

### `AccessLevel`

Enum in hierarchy order: `VIEW`, `MANAGE`, `OWNER`. `fromWire(value: String?): AccessLevel?` maps `"view"`,
`"manage"`, `"owner"`; anything else (including `null`) → `null`.

### `Scope`

Enum with its wire key: `MEMBERS("members")`, `SCHEDULE("schedule")`, `SONGS("songs")`, `GALLERY("gallery")`,
`EVENTS("events")`, `NOTICES("notices")`, `HYMNAL_HISTORY_REPORT("reports.hymnal_history")`.
`fromWire(key)` → `null` when unknown (the key is dropped).

### `Role`

Enum with its wire id: `ADMIN("admin")`, `LEADER("leader")`, `MEDIA("media")`. `fromWire(id)` → `null` when unknown.
Named `Role`, never "media" alone, to avoid confusion with `features/media` (backend 012 note).

### `Access`

| Field        | Type                        | Meaning                                                           |
|--------------|-----------------------------|-------------------------------------------------------------------|
| `roles`      | `Set<Role>`                 | known roles only                                                  |
| `hasAnyRole` | `Boolean`                   | the profile listed at least one role, known or not (panel entry)  |
| `levels`     | `Map<Scope, AccessLevel>`   | known scopes with a known level only; absence = no access         |

Rules:
- `allows(scope, minimum)` = `levels[scope]?.let { it >= minimum } == true`.
- `holds(role)` = `role in roles`.
- `Access.NONE` = no roles, `hasAnyRole = false`, empty levels. Used when the profile is loading, failed, cleared by
  logout, or absent.

### `AccessRepository` (interface)

| Member                        | Contract                                                                  |
|-------------------------------|---------------------------------------------------------------------------|
| `access: Flow<Access>`        | current access, distinct; `Access.NONE` whenever there is no profile data |
| `suspend fun refresh()`       | re-fetch the profile; single-flight — returns at once if one is running   |

### Use cases

- `ObserveAccessUseCase` → `Flow<Access>` (ViewModels `stateIn` with `Access.NONE`).
- `RefreshAccessUseCase` → `suspend () -> Unit`.

## 3. Mapping (`features/profile/data/access/`)

`MeProfileDto.toAccess()`:
- `roles` → `Role.fromWire(id)`, nulls dropped → `Access.roles`; `hasAnyRole = dto.roles.isNotEmpty()`.
- `permissions` → for each entry, `Scope.fromWire(key)` and `AccessLevel.fromWire(value)`; kept only when both are
  known.

`MeProfile` gains `access: Access`, filled by the snapshot mapper with `toAccess()`. `ProfileAccessRepository.access` = `ProfileSnapshotRepository.observe()` mapped: `SnapshotState.Data` → `value.access`,
anything else → `Access.NONE`. The mapper in `ProfileSnapshotRepository` keeps producing `MeProfile` (without
`isAdmin`); `Access` is derived from the same snapshot, so there is one source of truth.

`MeProfile` loses `isAdmin`. `ProfileUiState` loses `isAdmin`.

## 4. Panel (`features/admin/panel/`)

### `CardRequirement` (domain)

`AtLeast(scope: Scope, level: AccessLevel)` | `HasRole(role: Role)`.

### `PanelCard` (domain, enum in display order)

| Card               | Requirement                                   | Enabled |
|--------------------|-----------------------------------------------|---------|
| `WORSHIP`          | `AtLeast(SONGS, MANAGE)`                      | yes     |
| `ATTENDANCE`       | `HasRole(ADMIN)`                              | no      |
| `SCHEDULE`         | `AtLeast(SCHEDULE, MANAGE)`                   | yes     |
| `MEMBERS`          | `AtLeast(MEMBERS, VIEW)`                      | yes     |
| `NOTICES`          | `AtLeast(NOTICES, MANAGE)`                    | no      |
| `REPORTS`          | `AtLeast(HYMNAL_HISTORY_REPORT, VIEW)`        | yes     |
| `GALLERY`          | `AtLeast(GALLERY, MANAGE)`                    | no      |
| `EVENTS`           | `AtLeast(EVENTS, MANAGE)`                     | no      |
| `NOTIFICATIONS`    | `HasRole(ADMIN)`                              | no      |

"Enabled" stays a presentation concern (`AdminAction.enabled`); the domain decides only visibility.

### `AdminPanelUiState`

`cards: List<PanelCard>`, `isEmpty: Boolean`.

## 5. Screen flags

| UI state                      | Added                                                        | Removed   |
|-------------------------------|--------------------------------------------------------------|-----------|
| `MembersListUiState`          | `canAdd`                                                     | —         |
| `MemberProfileUiState`        | `canEdit`, `canChangePhoto`, `canDelete`, `canRemovePhoto`    | —         |
| `HymnalReportUiState`         | `canOpenSettings`                                            | —         |
| `ServiceWindowsUiState`       | `canManage`                                                  | —         |
| `ChordChartsUiState`, `ChordChartDetailUiState`, `LyricsUiState`, `LyricsDetailUiState` | `canEdit` | `isAdmin` |
| `UserAuthState` (CoreScreen)  | `canOpenPanel`                                               | `isAdmin` |

All flags default to `false` (deny until known).

## 6. Events

| Type                     | Change                                                                   |
|--------------------------|--------------------------------------------------------------------------|
| `AuthEventBus.Event`     | + `PermissionDenied` (a structured 403 was received)                     |
| `MembersEvent`           | unchanged; `toMembersEvent(kind: FailureKind)` decides it (research R6)  |
| Reports                  | + a `LeaveArea(message)` event for the report and windows loads (settings read is public) |
