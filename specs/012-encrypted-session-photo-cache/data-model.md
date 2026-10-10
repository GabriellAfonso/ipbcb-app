# Data Model: Encrypted Session, Encrypted Member Photo Cache and Member Photo Download

**Feature**: [spec.md](spec.md) | **Research**: [research.md](research.md)

Nothing here reaches the server; no DTO changes. On-disk formats are in
[contracts/on-device-storage.md](contracts/on-device-storage.md).

## Session store (`@AuthPrefs`)

| Item | Before | After |
|---|---|---|
| File | `filesDir/datastore/auth_prefs.preferences_pb` (plain protobuf) | `filesDir/datastore/auth_prefs.enc.preferences_pb` (sealed protobuf) |
| Type seen by callers | `DataStore<Preferences>` | unchanged |
| Keys | `auth_tokens` (JSON `AuthTokens`) | unchanged; any future key is encrypted too |
| Key material | — | Keystore alias `ipbcb_session_v1` |

**States** (per app start):

```text
             old file?   encrypted readable?   result
fresh        no          (empty)               signed out
migrating    yes         empty / no tokens     old prefs → encrypted, old file deleted, still signed in
leftover     yes         has tokens            keep encrypted, delete old file
normal       no          yes                   as stored
lost         any         no (key/corrupt)      empty prefs, key destroyed, pendingWipe = true → signed out
```

## `SessionRecovery` (in memory)

| Field | Type | Meaning |
|---|---|---|
| `pendingWipe` | `StateFlow<Boolean>` | the session was found unreadable in this process; session-scoped caches must be wiped |

Transitions: `false → true` in the corruption handler; `true → false` after `CoreViewModel` wipes.

## Member photo cache entry

| Field | Type | Rules |
|---|---|---|
| cache key | `String` | `sha256(url path)` hex; also the file name and the AEAD associated data |
| `etag` | `String?` | from the last `200`; sent as `If-None-Match` |
| `mimeType` | `String` | from `Content-Type`, else sniffed (JPEG/PNG/WEBP/GIF as `ValidateMemberPhotoUseCase`) |
| `bytes` | `ByteArray` | image as served |
| last use | file mtime | touched on read; drives LRU |

Cache-wide: directory `noBackupFilesDir/member_photos/`, limit **50 MB** (sum of file sizes), Keystore alias
`ipbcb_member_photos_v1`.

**Entry lifecycle**:

```text
absent ──fetch 200──▶ cached ──revalidate 304──▶ cached (touched)
                       │  ├─revalidate 200──▶ cached (new bytes, revision+1)
                       │  ├─revalidate 404/other refusal──▶ absent (revision+1)
                       │  ├─undecryptable──▶ absent (re-fetched)
                       │  ├─LRU over 50 MB──▶ absent
                       │  └─photo replaced/removed/member deleted in app, or not in refreshed list──▶ absent
any ──401/403 on list/record/photo, logout, session lost──▶ wiped (all files + key)
```

## `MemberPhotoSource.revisions`

`StateFlow<Map<String, Int>>` keyed by URL; incremented whenever the cached bytes for a URL change or disappear
(wipe increments all known). Exposed by member ViewModels as `photoRevisions` and passed to `MemberAvatar` as
`photoRevision: Int`.

## Profile UI state additions (`MemberProfileUiState`)

| Field | Type | Rule |
|---|---|---|
| `canDownloadPhoto` | `Boolean` | `allows(MEMBERS, MANAGE)` |
| `isDownloadingPhoto` | `Boolean` | true while a download runs; blocks a second one (FR-026) and all viewer buttons |
| `photoRevision` | `Int` | from `revisions[photoUrl] ?: 0` |

`MembersListUiState` gains `photoRevisions: Map<String, Int>`; `MemberFormUiState` gains `photoRevision`.

## Download request / result

| Item | Value |
|---|---|
| Input | member id, display name, photo URL, today's date |
| File name | `"<name> <yyyy-MM-dd>.<ext>"`, sanitised (R9), fallback `"Membro <id>"` |
| Folder | `Pictures/IPB Castelo Branco` |
| Success | `Result.success(Unit)` → message "Foto salva em Imagens/IPB Castelo Branco" |
| Failure | `Result.failure(AppError)` → `toUserMessage()`; storage write failures carry `userMessage = "Não foi possível salvar a foto."` |
| Permission denied | presentation only: "Para salvar a foto, permita o acesso ao armazenamento." / permanent: "Permita o acesso ao armazenamento nas configurações do aparelho para salvar a foto." |
