# Data Model: Gallery Management

Types added or changed by this feature. Existing 007 types (`GalleryAlbum`, `GalleryPhoto`, `GalleryIndex`,
`GalleryTree`, `GalleryLocalState`) keep their fields. Package root: `com.ipb.castelobranco.features.gallery`.

## Domain (pure Kotlin, no Android)

### `domain/model/GalleryLocalChange` — sealed interface (R1)

| Variant | Fields |
|---------|--------|
| `UpsertAlbum` | `album: GalleryAlbum` |
| `UpsertPhoto` | `photo: GalleryPhoto` |
| `RemoveAlbumTree` | `albumId: Long` |
| `RemovePhotos` | `ids: Set<Long>` |
| `ReorderAlbums` | `parentId: Long?`, `ids: List<Long>` |
| `ReorderPhotos` | `albumId: Long`, `ids: List<Long>` |

`GalleryIndex.apply(change): GalleryIndex` — cursor untouched. Rules:
- `RemoveAlbumTree` removes the album, every album whose ancestor chain reaches it, and every photo whose `albumId` is
  in that set. Unknown id → index unchanged.
- `Reorder*` sets `position = index in ids` for listed items present in the index; unlisted items untouched.
- Applying the same change twice equals applying it once.

### `domain/model/GalleryTree` — additions (R11)

- `moveTargetsForAlbum(albumId: Long): List<TreeTarget>`
- `moveTargetsForPhotos(currentAlbumId: Long): List<TreeTarget>`
- `subtreeCounts(albumId: Long): SubtreeCounts`

```text
TreeTarget(albumId: Long?  /* null = "Raiz" */, name: String, depth: Int, selectable: Boolean)
SubtreeCounts(subAlbums: Int, photos: Int)   // all levels, excluding the album itself
```

### `domain/manage/` — drafts and results

```text
AlbumDraft(name: String, description: String, eventDate: String? /* yyyy-MM-dd */, parentId: Long?)
AlbumEdit(albumId, name: Field<String>, description: Field<String>, eventDate: Field<String?>, parentId: Field<Long?>)
PhotoEdit(photoId, name: Field<String>, description: Field<String>, dateTaken: Field<String?>, albumId: Field<Long>)
Field<T> = Unchanged | Set(value: T)          // Set(null) = clear / move to root
BatchResult(succeeded: Int, failures: Map<String /* user message */, Int>)
```

`GalleryNames.validate(raw: String): NameCheck` — `Valid(trimmed)` | `Empty` | `TooLong` (1–100 after trim).

### `domain/manage/GalleryManageRepository` — interface

Every method returns `Result<…>` whose failure is an `AppError`; each success is already applied locally (R1) and has
requested a post-write sync (R2), except the batch helpers, which defer the sync to the use case.

| Method | Server call | Local change |
|--------|-------------|--------------|
| `createAlbum(draft): Result<GalleryAlbum>` | `POST albums/` | `UpsertAlbum` |
| `editAlbum(edit): Result<GalleryAlbum>` | `PATCH albums/{id}/` | `UpsertAlbum` |
| `reorderAlbums(parentId, ids): Result<Unit>` | `PUT albums/order/` | `ReorderAlbums` |
| `setCover(albumId, file): Result<GalleryAlbum>` | `PUT albums/{id}/cover/` | `UpsertAlbum` |
| `removeCover(albumId): Result<Unit>` | `DELETE albums/{id}/cover/` | none (sync) |
| `deleteAlbum(albumId): Result<Unit>` | `DELETE albums/{id}/` | `RemoveAlbumTree` (also on 404) |
| `editPhoto(edit): Result<GalleryPhoto>` | `PATCH photos/{id}/` | `UpsertPhoto` |
| `reorderPhotos(albumId, ids): Result<Unit>` | `PUT albums/{id}/photos/order/` | `ReorderPhotos` |
| `deletePhoto(photoId): Result<Unit>` | `DELETE photos/{id}/` | `RemovePhotos` (also on 404) |
| `originalForCover(photo): Result<File>` | `GET image_url` when not on disk | none |

Error classification exposed to presentation through `GalleryWriteError` (derived from `AppError`, in domain):
`Offline`, `Forbidden(message)`, `NotFound`, `DuplicateName(message)`, `Cycle(message)`, `OrderMismatch`,
`Rejected(message)`, `Other(message)`.

### `domain/manage/` — use cases

`CreateAlbumUseCase`, `EditAlbumUseCase` (edit + move), `ReorderUseCase` (sends only changed groups; returns
`OrderChanged` on mismatch after the sync), `SetCoverUseCase` (picked file or photo), `RemoveCoverUseCase`,
`DeleteAlbumUseCase`, `EditPhotoUseCase`, `MovePhotosUseCase`, `DeletePhotosUseCase` (R13).

### `domain/upload/UploadItem` (R6)

| Field | Type | Notes |
|-------|------|-------|
| `uploadId` | `String` | UUID v4, generated once at enqueue; the `client_upload_id` of every attempt |
| `albumId` | `Long` | the album open when picked |
| `displayName` | `String` | picked name with extension set to what is sent; `foto_yyyyMMdd_HHmmss.jpg` when unknown |
| `fileName` | `String` | file under `gallery/uploads/` |
| `state` | `UploadState` | see below |
| `failure` | `String?` | user-facing reason when `Failed` |
| `enqueuedAt` | `Long` | epoch ms; queue order |

`UploadState`: `Waiting` (raw copy) → `Prepared` (ready to send) → removed on success; any → `Failed` (terminal until
dismissed). No `Sending` state is persisted: a process death mid-request resends with the same id (R9).

```text
Waiting ──prepare ok──▶ Prepared ──201/207 accepted──▶ (removed; file becomes the original)
   │                        │ ──IOException/5xx──▶ Prepared (retry later, same uploadId)
   │                        └──rejected/409/404/403/malformed──▶ Failed(reason)
   └──unreadable / too large──▶ Failed(reason)
Failed ──dismiss──▶ (removed, file deleted)
```

### `domain/upload/` — interfaces and use cases

- `GalleryUploadRepository`: `items: StateFlow<List<UploadItem>>`, `enqueue(albumId, sources: List<String>)`,
  `dismiss(uploadId)`, `clear()`.
- `GalleryUploadScheduler`: `enqueue()`, `cancel()`.
- `EnqueueUploadsUseCase` (copy + schedule), `DismissUploadUseCase`.
- `UploadOutcome` (sealed, R9 table): `Sent(photo)`, `Failed(reason)`, `AlbumGone`, `AccessLost(reason)`, `Stop`,
  `Retry`.

## Data

### Snapshot `gallery_upload_queue` (R6)

```json
{"items": [{"upload_id": "3f2a9c1e-…", "album_id": 7, "display_name": "IMG_0042.jpg",
            "file_name": "3f2a9c1e-….heic", "state": "waiting", "failure": null, "enqueued_at": 1790000000000}]}
```

Unknown `state` on read → item dropped with its file (forward compatibility is not needed; app and queue ship together).

### Files

| Path | Content | Removed when |
|------|---------|--------------|
| `filesDir/gallery/uploads/{uploadId}.{ext}` | raw copy, then the prepared JPEG (same id, `.jpg`, or `.gif` sent as is) | sent (moved to `photos/`), dismissed, logout |
| `filesDir/gallery/photos/{photoId}.{ext}` | adopted prepared file for photos sent from this device | as in 007 |
| `cacheDir/gallery-cover-*` | temp cover (prepared pick or downloaded original) | after the request |

### `UploadPreparationPlanner` (R8) — pure

Inputs: `width`, `height`, `exifOrientation`, `mimeType`, `byteSize`. Constants: `MAX_LONG_SIDE = 4000`,
`MAX_BYTES = 10 * 1024 * 1024`, `MAX_PIXELS = 50_000_000`, `QUALITY_LADDER = [90, 85, 80, 75, 70, 60]`.

Output: `SendAsIs` (GIF within `MAX_BYTES` and `MAX_PIXELS`) | `Reencode(sampleSize, targetWidth, targetHeight,
rotationDegrees, flipHorizontal)` — target long side `min(4000, source long side)` after rotation, aspect kept,
`sampleSize` the largest power of two keeping the decoded long side ≥ target.

## Presentation

### `GalleryPermissions` (R4)

`canManage: Boolean`, `canDelete: Boolean`; `NONE` for members without a level. Added to `GalleryRootUiState`,
`AlbumUiState`, `PhotoViewerUiState`.

### UiState additions

| State | New fields |
|-------|------------|
| `GalleryRootUiState` | `permissions`, `organize: OrganizeUiState?` |
| `AlbumUiState` | `permissions`, `canRemoveCover`, `organize: OrganizeUiState?`, `selection: Set<Long>`, `uploads: AlbumUploadsUiState`, `deleteCounts: SubtreeCounts` |
| `PhotoViewerUiState` | `permissions` |
| `OrganizeUiState` | `albums: List<AlbumTile>`, `photos: List<PhotoTile>`, `isSaving` |
| `AlbumUploadsUiState` | `isCopying`, `pending: Int`, `total: Int`, `failed: List<FailedUpload(uploadId, name, reason)>` |
| `AlbumFormState` | `mode (Create/Edit)`, `name`, `description`, `eventDate`, `nameError`, `isSaving` |
| `PhotoFormState` | `name`, `description`, `dateTaken`, `nameError`, `isSaving` |
| `MovePickerState` | `targets: List<TreeTarget>`, `isSaving` |
| `ConfirmState` | `text`, `action` (delete album, delete photos, remove cover) |

Dialog/sheet states live in one `GalleryDialogState?` `StateFlow` on the ViewModel (only one open at a time).

### `GalleryMessage` — sealed interface (R12)

Existing: `PhotoRemoved`, `PhotoMoved`, `AlbumRemoved`. New: `AlbumCreated`, `Saved` ("Alterações salvas"),
`AlbumTrashed` ("Álbum enviado para a lixeira"), `PhotoTrashed` ("Foto enviada para a lixeira"),
`PhotoMovedTo(albumName)` ("Foto movida para '…'"), `Batch(text)`, `OrderChanged`, `CoverUpdated` ("Capa atualizada"),
`CoverRemoved` ("Capa removida"), `Failure(text)`.
