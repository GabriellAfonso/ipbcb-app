# Contract: Gallery Trash (app side)

What the app sends and how it reads each answer. The server contract is not redefined: see
`backend/specs/014-gallery-trash-sync/contracts/gallery-trash-api.md`.

All calls on `@AuthedRetrofit` (JWT, `PermissionDeniedInterceptor`, `TokenAuthenticator`), paths relative to
`ApiConstants.BASE_PATH`. Errors are turned into `AppError` only by `Response.toAppError()`.

## 1. HTTP calls added to `GalleryApi`

| Call | Method + path | Success read |
|------|---------------|--------------|
| `getTrash` | `GET api/gallery/trash/` | `200` `List<GalleryTrashEntryDto>` (`[]` when empty) |
| `restoreAlbum` | `POST api/gallery/trash/albums/{id}/restore/` (no body) | `200` `GalleryAlbumDto` |
| `restorePhoto` | `POST api/gallery/trash/photos/{id}/restore/` (no body) | `200` `GalleryPhotoDto` |

Entries with an unknown `kind` are dropped when mapping.

## 2. Reading a restore answer

| Answer | `AppError` | `RestoreResult` | Local effect |
|--------|------------|-----------------|--------------|
| `200` | — | `Restored` | `applyLocal(UpsertAlbum / UpsertPhoto)`, then `syncAfterWrite` |
| `404` | `Server(404)` | `NotInTrash` | `syncAfterWrite` (as any 008 not-found) |
| `400` + `trashed_parent_id` | `Server(400, extras)` | `TrashedParent(parentAlbumId)` | none |
| `400` + `conflicting_album_id` | `Server(400, extras)` | `NameConflict(id, isOnDevice)` | sync when the album is missing locally |
| no network | `Network(userMessage = "Sem conexão")` | `Failed` | none |
| `403` | `Auth(403)` | `Failed` | profile re-read by `PermissionDeniedInterceptor` |
| other | any | `Failed` | none |

Extras are strings (`Map<String, String>`); ids are parsed with `toLongOrNull()`. A 400 whose id does not parse is
`Failed`.

## 3. Texts (Portuguese, user-visible)

| Where | Text |
|-------|------|
| Trash title | "Lixeira" |
| Explanation | "Os itens ficam 30 dias na lixeira e depois são apagados para sempre. Restaurar um álbum traz de volta tudo o que foi apagado com ele." |
| Empty | "A lixeira está vazia." |
| Offline read | "Sem conexão. A lixeira precisa de internet." |
| Retry | "Tentar novamente" |
| Kind | "Álbum" / "Foto" |
| Deleted | "Apagado por {nome} em {dd/MM/yyyy HH:mm}" / "Apagado por usuário desconhecido em …" |
| Counts | "{n} subálbuns · {m} fotos" ("1 subálbum", "1 foto"; zero parts omitted) |
| Uploader | "Enviada por {nome}" |
| Purge | "Some em {dd/MM/yyyy}" |
| Button | "Restaurar" |
| Restored | "Álbum restaurado" / "Foto restaurada" |
| Not in trash | "Este item não está mais na lixeira." |
| Parent / conflict / other | server `detail` via `toUserMessage()` |
| Conflict action | "Abrir álbum" |
| Undo action | "Desfazer" |
| Access lost | "Você não tem mais acesso à lixeira." |
| Trash icon description | "Lixeira" |
