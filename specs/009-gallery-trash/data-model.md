# Data Model: Gallery Trash

Nothing is stored on the device. Every type below lives in memory while the trash screen or a "Desfazer" is in use.

## Wire (data)

### `GalleryTrashEntryDto` — one element of `GET api/gallery/trash/`

| Field | Type | Notes |
|-------|------|-------|
| `kind` | `String` | `"album"` / `"photo"`; anything else is dropped when mapping |
| `id` | `Long` | |
| `name` | `String` | |
| `deleted_at` | `String` | ISO-8601 instant (UTC) |
| `deleted_by` | `String?` | display name, `null` when the user was removed |
| `uploaded_by` | `String?` | photos only; `null` for albums and old photos |
| `purge_on` | `String` | `yyyy-MM-dd` |
| `sub_album_count` | `Int` | default 0 |
| `photo_count` | `Int` | default 0 |
| `thumbnail_url` | `String?` | |

Restore answers reuse `GalleryAlbumDto` / `GalleryPhotoDto`.

## Domain (`features/gallery/domain/trash`)

### `TrashKind`
`ALBUM`, `PHOTO`.

### `TrashEntry`
`kind`, `id`, `name`, `deletedAt: String`, `deletedBy: String?`, `uploadedBy: String?`, `purgeOn: String`,
`subAlbumCount: Int`, `photoCount: Int`, `thumbnailUrl: String?`. `key = TrashKey(kind, id)`.

### `TrashKey`
`(kind, id)` — identifies a row and a restore target (an album and a photo may share an id).

### `RestoreResult` (sealed)

| Case | From | Carries |
|------|------|---------|
| `Restored` | 200 (resource applied locally, sync run) | — |
| `NotInTrash` | 404 | — |
| `TrashedParent` | 400 with `trashed_parent_id` | `parentAlbumId: Long`, `error: AppError` (for the detail) |
| `NameConflict` | 400 with `conflicting_album_id` | `conflictingAlbumId: Long`, `isOnDevice: Boolean`, `error` |
| `Failed` | anything else (offline, 403, 5xx, other 400) | `error: AppError` |

## Presentation (`features/gallery/presentation`)

### `TrashRow`
`key`, `title` (name), `kindLabel` ("Álbum" / "Foto"), `deletedLine` ("Apagado por {nome|usuário desconhecido} em
{dd/MM/yyyy HH:mm}"), `countsLine: String?` (albums: "{n} subálbuns · {m} fotos", singular forms, zero parts omitted,
`null` when both zero or a photo), `uploadedLine: String?` ("Enviada por {nome}", photos with a name), `purgeLine`
("Some em {dd/MM/yyyy}"), `thumbnailUrl: String?`.

### `TrashUiState`

| Field | Meaning |
|-------|---------|
| `isLoading` | first read not answered yet |
| `isRefreshing` | a reload (pull or after 404) with rows already shown |
| `error: String?` | the last read failed and there are no rows to show |
| `rows: List<TrashRow>` | server order |
| `restoring: TrashKey?` | the row whose restore is running (one at a time) |
| `highlighted: TrashKey?` | the trashed parent to restore first |

Derived: `isEmpty` (loaded, no error, no rows), `canRefresh` (`restoring == null`).

State transitions of a row: listed → restoring → (removed | listed again with message | list reloaded).

### `TrashEvent` (one-shot, `SharedFlow`)
`Message(text, openAlbumId: Long?)`, `Close(text)`.

### `GalleryMessage` additions
`action: MessageAction?` on every message (default `null`); `AlbumTrashed(undo: MessageAction.Undo?)`,
`PhotoTrashed(undo: MessageAction.Undo?)`. `MessageAction`: `Undo(key: TrashKey)` label "Desfazer",
`OpenAlbum(albumId)` label "Abrir álbum".
