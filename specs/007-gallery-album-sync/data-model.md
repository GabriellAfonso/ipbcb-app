# Data Model: Gallery Album Tree and Sync

## Wire (data/dto)

All `@Serializable`, snake_case via `@SerialName`, unknown keys ignored by the project's `Json`.

### `GalleryAlbumDto`

| Field | Type | Notes |
|-------|------|-------|
| `id` | `Long` | |
| `name` | `String` | |
| `parent_id` | `Long?` | `null` = root |
| `description` | `String` | default `""` |
| `event_date` | `String?` | `yyyy-MM-dd` |
| `cover_url` | `String?` | absolute; `null` with `cover_source_album_id == null` = no cover below |
| `cover_source_album_id` | `Long?` | stored, not used for display |
| `position` | `Int` | order among siblings |

### `GalleryPhotoDto` (extended)

Existing fields plus `thumbnail_url: String?` (default `null`), `position: Int` (default `0`),
`members: List<GalleryPhotoMemberDto>` (default `[]`). `fileExtension()` unchanged.

### `GalleryPhotoMemberDto`

`id: Long`, `name: String`. Stored only.

### `GalleryChangesDto`

| Field | Type |
|-------|------|
| `albums` | `List<GalleryAlbumDto>` |
| `photos` | `List<GalleryPhotoDto>` |
| `deleted_album_ids` | `List<Long>` |
| `deleted_photo_ids` | `List<Long>` |
| `cursor` | `String` — opaque |
| `full_sync_required` | `Boolean` |

## Snapshot (data/snapshot)

### `GalleryIndexSnapshot` — key `gallery_index`

| Field | Type | Notes |
|-------|------|-------|
| `albums` | `List<GalleryAlbumDto>` | |
| `photos` | `List<GalleryPhotoDto>` | |
| `cursor` | `String?` | `null` never saved in practice; present for decode safety |

Absent or undecodable snapshot = no index = next sync is a full read.

## Domain (domain/model)

### `GalleryAlbum`, `GalleryPhoto`

Mirror the DTOs (camelCase; `eventDate` kept as the server's `yyyy-MM-dd` string, formatted by the UI — `minSdk 24` has no `java.time` without desugaring). `GalleryPhoto.members` kept as `List<GalleryMember>`.

### `GalleryIndex`

`albums: Map<Long, GalleryAlbum>`, `photos: Map<Long, GalleryPhoto>`, `cursor: String?`.

Operations (pure, unit-tested):

- `applyDelta(delta)`: upsert `albums`/`photos` by id; remove `deletedAlbumIds`/`deletedPhotoIds`; set `cursor`.
  Idempotent: applying the same delta twice equals applying it once. A delete and an upsert of the same id in one
  answer cannot happen (backend lists a restored item only as changed); if it did, delete wins.
- `replaceWith(full)`: new index from a full read (albums, photos, cursor).

### `GalleryTree` (derived from `GalleryIndex`)

- `roots()`: albums with `parentId == null`.
- `children(albumId)`: albums with that parent.
- `photosOf(albumId)`: photos with that `albumId`.
- `parentOf(albumId)`.
- Every list sorted by `position`, then `id`.
- An album whose parent is not in the index is unreachable (not shown); a photo whose album is not in the index is not
  shown. Neither is deleted by the tree — the next full sync resolves it.
- `allPhotosInTreeOrder()`: pre-order walk, used as the download worker's list.

### `GalleryDelta`

Domain form of `GalleryChangesDto` minus `full_sync_required`.

### `GalleryLocalState` (repository output)

| Field | Type | Meaning |
|-------|------|---------|
| `index` | `GalleryIndex?` | `null` = never synced (or cleared) |
| `originals` | `Map<Long, File>` | photo id → original on disk |
| `covers` | `Map<String, File>` | `cover_url` → file on disk |

### `GallerySyncResult` (sealed)

`Synced(missingOriginals: Int)` · `Skipped` · `Failed(error: AppError)`

### `GallerySyncStatus` (StateFlow from the syncer)

`isRunning: Boolean`, `lastError: AppError?` (cleared by the next `Synced`), `hasAnswered: Boolean` (a sync finished,
successfully or not, in this process).

## Presentation (presentation/state)

### `PhotoImage` (sealed)

`Original(file: File)` · `Preview(url: String)` · `None`. Rule: original on disk → `Original`; else
`thumbnailUrl != null` → `Preview`; else `None` (grey). A `Preview` that fails to load renders grey.

### `AlbumTile`

`id`, `name`, `cover: File?` (black when `null`).

### `GalleryRootUiState`

| Field | Meaning |
|-------|---------|
| `isLoggedIn` | from `CoreViewModel` via graph parameter |
| `isLoading` | no index and the first sync has not answered |
| `albums: List<AlbumTile>` | root albums, ordered |
| `hasIndex` | an index exists (even if empty) |
| `download: GalleryDownloadState` | unchanged banner model |
| `isOnWifi` | unchanged |
| `error: String?`, `errorCode: Int?` | sync failure with no index (401 / 403 / network) — see screen rules |
| `forbiddenNotice` | 403 with an index → "Disponível apenas para membros." above the grid |

### `AlbumUiState`

`isLoading`, `title`, `subtitle: String?` (parent name), `eventDate: String?` (`dd/MM/yyyy`), `description: String?`,
`subAlbums: List<AlbumTile>`, `photos: List<PhotoTile(id, image: PhotoImage)>`, `isEmpty`, `isRemoved`.

### `PhotoViewerUiState`

`isLoading`, `photos: List<ViewerPhoto(id, title (name without extension), image: PhotoImage, canSaveOrShare)>`,
`initialIndex`, `isClosed` (no photo left → close to the album).

### `GalleryMessage` (sealed, one-shot)

`PhotoRemoved` ("Esta foto foi removida") · `PhotoMoved` ("Esta foto foi movida para outro álbum") · `AlbumRemoved`
("Este álbum foi removido").

## State transitions

```
[no index] --sync ok--> [index, cursor C1] --sync ok (delta)--> [index', C2]
     |                         |
     |                         +--full_sync_required--> read all --> [index'', C3]
     |                         +--401/403/network--> unchanged (status.lastError set)
     +--sync fails--> [no index] (root: loading until first answer, then error state)

any --logout--> [no index] + no files + no preview cache + no scheduled work
```

## Preferences

`GalleryPreferences`: `gallery_layout_version: Int` (`2` after R4 migration). `gallery_auto_download_triggered`
removed.
