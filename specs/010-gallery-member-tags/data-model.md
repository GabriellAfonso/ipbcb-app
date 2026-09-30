# Data Model: Gallery Member Tags

Nothing new is stored on the device: the tags already live in the gallery index (`GalleryPhoto.members`, feature
007), and the profile snapshot gains one field. The picker list is memory-only.

## Wire (data)

### `MeProfileDto` (profile snapshot, cached)

| Field | Type | Notes |
|-------|------|-------|
| `member_id` | `Long?` | new; default `null`, so a profile cached before this feature still decodes |

### `GalleryPhotoMemberDto` — element of a photo's `members` and of `GET api/gallery/taggable-members/`

| Field | Type |
|-------|------|
| `id` | `Long` |
| `name` | `String` |

### Request bodies (hand-built `JsonObject`s)

| Call | Body |
|------|------|
| `PUT api/photos/{id}/members/` | `{"member_ids": [Long]}` |
| `POST api/photos/members/` | `{"photo_ids": [Long], "add_member_ids": [Long], "remove_member_ids": [Long]}` |

## Core domain

| Type | Fields / members | Notes |
|------|------------------|-------|
| `CurrentMemberRepository` | `memberId: Flow<Long?>` | implemented by the profile feature |
| `ObserveOwnMemberIdUseCase` | `invoke(): Flow<Long?>` | |
| `MeProfile` (profile feature) | `+ memberId: Long?` | default `null` |

## Gallery domain

| Type | Fields | Notes |
|------|--------|-------|
| `GalleryMember` (existing) | `id`, `name` | a photo's person and a taggable person |
| `GalleryLocalChange.UpsertPhotos` | `photos: List<GalleryPhoto>` | a bulk tag answer, applied without moving the cursor |
| `TaggedPerson` | `id`, `name`, `photoCount` | derived from the tree |
| `TagSaveResult` | `Unchanged` / `Saved` / `Failed(error: AppError)` | single-photo save |
| `TagBatchResult` | `updated: Int`, `total: Int`, `failure: AppError?` | bulk; `failure` = first failure |

Rules:

- `photosWithAll(tree, ids)`: photos whose member ids contain every id; tree pre-order, then position, then id;
  empty set → empty list.
- `taggedPeople(tree)`: members of reachable photos with counts; order by normalized name, name, id.
- `peopleIn(photos)`: distinct members of those photos, same order.
- `NameSearch.matches(name, query)`: case- and accent-insensitive `contains`; blank query matches.
- Chunk size: 200 photos per bulk request.

## Presentation

| Type | Fields | Notes |
|------|--------|-------|
| `ViewerSource` | `Album(albumId)` / `People(memberIds: Set<Long>)` | the list a viewer pages through |
| `ViewerPhoto` | `+ albumId`, `+ description: String?`, `+ dateTaken: String?` (`dd/MM/yyyy`), `+ people: List<String>` | |
| `GalleryRootUiState` | `+ ownMemberId: Long?`; `showPeople`, `showMyPhotos` derived | |
| `AlbumUiState` | `+ canRemovePeople: Boolean` | a selected photo has someone tagged |
| `PickerMode` | `Photo(photoId)` / `Add(albumId, photoIds)` / `Remove(albumId, photoIds)` | |
| `PeoplePickerState` | `mode`, `people: List<GalleryMember>`, `isLoading`, `error: String?`, `query`, `checked: Set<Long>`, `isSaving`; derived `visiblePeople`, `title`, `confirmLabel`, `canConfirm` | |
| `GalleryDialogState.PeoplePicker` | `picker` | |
| `GalleryMessage.PhotoLeftResult` | — | "Esta foto não está mais no resultado" |
| `PersonRow` | `id`, `name`, `photoCount`, `isChecked` | filter list row |
| `PeopleUiState` | `isLoading`, `isMine`, `isClosed`, `query`, `people: List<PersonRow>`, `selected: List<PersonRow>`, `results: List<PhotoTile>`, `showResults`, `hint`, `emptyText`, `noTags` | |

## State transitions — people picker

```text
closed ──open(Photo|Add)──▶ loading ──ok──▶ ready ──Salvar──▶ saving ──ok──▶ closed (+message)
                               │              ▲                  │
                               └─error──▶ error ─Tentar novamente┘ ├─404──▶ loading/ready (reload) + message
closed ──open(Remove)──▶ ready                                      ├─403──▶ closed
                                                                     └─other─▶ ready (single) / closed (bulk)
```
