# Data Model: Monthly Birthdays

## Entities

### Birthday (Domain Model)

**File**: `core/domain/model/Birthday.kt`

| Field  | Type   | Description |
|--------|--------|-------------|
| name   | String | Member's display name |
| day    | Int    | Day of birth (1-31) |
| gender | Gender | Member's gender (MALE, FEMALE, UNKNOWN) |

**Validation rules**:
- `day` must be 1-31
- `name` must be non-blank
- `gender` maps from API `"M"` → MALE, `"F"` → FEMALE, `null` → UNKNOWN
- List is always sorted by `day` ascending (enforced by mapper)

**State transitions**: None — read-only entity.

### BirthdayDto (Network DTO)

**File**: `core/data/dto/BirthdayDtos.kt`

| Field     | JSON Key   | Type    |
|-----------|------------|---------|
| name      | `name`     | String  |
| gender    | `gender`   | String? |
| birthDay  | `birth_day`| Int     |

### BirthdaysResponseDto (Network Wrapper)

**File**: `core/data/dto/BirthdayDtos.kt`

| Field     | JSON Key    | Type              |
|-----------|-------------|-------------------|
| birthdays | `birthdays` | List\<BirthdayDto\> |

## Mapping

`BirthdaysResponseDto` → `List<Birthday>`:
- Map each `BirthdayDto` to `Birthday(name = dto.name, day = dto.birthDay, gender = dto.gender.toGender())`
- `gender` mapping: `"M"` → MALE, `"F"` → FEMALE, `null`/other → UNKNOWN
- Sort by `day` ascending
- Filter out entries with blank names (defensive)

## Cache

- **Storage**: `JsonSnapshotStorage` with key `"birthdays_month_{month}"`
- **Format**: Serialized `BirthdaysResponseDto` JSON
- **Lifecycle**: Persists across app restarts; stale on month change (new key used)
