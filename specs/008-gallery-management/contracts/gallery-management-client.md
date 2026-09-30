# Contract: Gallery Management (app side)

What the app sends and how it reads each answer. The server contract is not redefined: see
`backend/specs/013-gallery-write-api/contracts/gallery-api.md`,
`backend/specs/014-gallery-trash-sync/contracts/gallery-trash-api.md` and
`backend/specs/016-photo-upload-idempotency/contracts/photo-upload-api.md`.

All calls on `@AuthedRetrofit` (JWT, `PermissionDeniedInterceptor`, `TokenAuthenticator`), paths relative to
`ApiConstants.BASE_PATH`. Errors are turned into `AppError` only by `Response.toAppError()`.

## 1. HTTP calls added to `GalleryApi`

| Call | Method + path | Body | Success read |
|------|---------------|------|--------------|
| `createAlbum` | `POST api/albums/` | JSON: `name`; `parent_id` when not root; `description` when not blank; `event_date` when set | `201` Album |
| `patchAlbum` | `PATCH api/albums/{id}/` | JSON with **only changed** keys; `parent_id: null` = move to root; `event_date: null` = clear | `200` Album |
| `orderAlbums` | `PUT api/albums/order/` | JSON `{"parent_id": <id or explicit null>, "ids": [...]}` | `204` |
| `putCover` | `PUT api/albums/{id}/cover/` | multipart `image` (one file) | `200` Album |
| `deleteCover` | `DELETE api/albums/{id}/cover/` | — | `204` |
| `deleteAlbum` | `DELETE api/albums/{id}/` | — | `204` |
| `uploadPhoto` | `POST api/photos/` | multipart `album_id`, `client_upload_id`, one `image` (`filename` = display name, `Content-Type` `image/jpeg` or `image/gif`) | `201`/`207` `{accepted, rejected}` |
| `patchPhoto` | `PATCH api/photos/{id}/` | JSON with only changed keys among `name`, `description`, `date_taken` (`null` clears), `album_id` | `200` Photo |
| `orderPhotos` | `PUT api/albums/{id}/photos/order/` | JSON `{"ids": [...]}` | `204` |
| `deletePhoto` | `DELETE api/photos/{id}/` | — | `204` |

JSON bodies are built as `JsonObject` (research R5): the project `Json` has `explicitNulls = false`.

Album and Photo bodies are read with the existing `GalleryAlbumDto` / `GalleryPhotoDto`. New DTO:

```kotlin
@Serializable data class PhotoUploadResultDto(
    val accepted: List<GalleryPhotoDto> = emptyList(),
    val rejected: List<RejectedFileDto> = emptyList(),
)
@Serializable data class RejectedFileDto(val filename: String = "", val reason: String = "")
```

A `400`/`409` body is read only by `toAppError()` (constitution: one parsing point). The gallery needs the
structured extras of three errors (`rejected` on the upload, `missing`/`unexpected`/`repeated` on an order, `chain` on
a cycle), so the core parser is extended, generically:

- `ApiErrorBody` gains `extras: Map<String, String>` — every top-level key other than `error_code`, `detail` and
  `field_errors`, with its value as raw JSON text (`parseApiError` in `ApiErrorParser.kt`).
- `AppError.Server` gains `extras: Map<String, String>? = null`, filled by `Response.toAppError()`.

The gallery data layer then reads `extras["rejected"]` with the project `Json` into `List<RejectedFileDto>`, and
recognizes the order mismatch by `"missing" in extras || "unexpected" in extras || "repeated" in extras` and the cycle
by `"chain" in extras`. Nobody else reads `errorBody()`. Existing callers are unaffected (new defaulted fields).

## 2. Answer → app behaviour

| Answer | Non-upload write | Upload item |
|--------|------------------|-------------|
| 2xx | apply locally (data-model table), `syncAfterWrite`, success message | `Sent` (R9) |
| `IOException` | "Sem conexão", nothing changes | retry later, same `client_upload_id` |
| 5xx / 429 | server `detail` or generic server text | retry later |
| 400 duplicate name | form stays open, `detail` under the name | — |
| 400 cycle (`chain`) | picker closes, `detail`, sync | — |
| 400 order mismatch | sync, keep "Organizar", "A ordem mudou enquanto você editava. Confira e salve de novo." | — |
| 400 with `rejected` | cover: `detail`, old cover stays | `Failed(rejected[0].reason)` |
| 400 other | `detail` or generic | `Failed("Não foi possível enviar esta foto.")` |
| 401 | existing session path | stop the run; items stay |
| 403 | `detail`; access refreshed by the interceptor; controls follow | `AccessLost` — this and every remaining item fail |
| 404 | delete: counts as done, removed locally; other writes: "Este item não existe mais.", sync | `AlbumGone` — "O álbum foi apagado" for every item of the album |
| 409 (upload) | — | `Failed(detail)` ("Esta foto já foi enviada e depois apagada; ela está na lixeira.") |

## 3. UI contract — controls per level

| Screen | Control | Level | Shown when |
|--------|---------|-------|-----------|
| Root | "Novo álbum" (FAB) | `manage` | always |
| Root | "Organizar" (top bar) | `manage` | ≥ 2 root albums |
| Album | "Adicionar fotos" / "Novo álbum" (FAB menu) | `manage` | always |
| Album | overflow: "Editar álbum", "Mover álbum", "Trocar capa", "Organizar" | `manage` | "Organizar" only with ≥ 2 sub-albums or ≥ 2 photos |
| Album | overflow: "Remover capa" | `owner` | `coverSourceAlbumId == id` |
| Album | overflow: "Apagar álbum" | `owner` | always |
| Album | long press on a photo → selection bar "Mover" | `manage` | — |
| Album | selection bar "Apagar" | `owner` | — |
| Album | upload strip "Enviando X de N" / "Preparando fotos…" and failed list | `manage` | queue has items of this album |
| Viewer | overflow: "Editar foto", "Mover", "Usar como capa" | `manage` | — |
| Viewer | overflow: "Apagar" | `owner` | — |
| Admin panel | card "Galeria" enabled → gallery root | `manage` | card already filtered by `PanelCard` |

Without a level, none of these nodes are composed.

## 4. Texts (Portuguese, hardcoded)

| Where | Text |
|-------|------|
| Offline write | "Sem conexão" |
| Name empty / too long | "Informe um nome" / "Máximo de 100 caracteres" |
| Delete album | "Apagar '{nome}' com {n} subálbuns e {m} fotos? Fica 30 dias na lixeira." (omit zero parts; singular forms "1 subálbum", "1 foto"; nothing inside → "Apagar '{nome}'? Fica 30 dias na lixeira.") |
| Delete photos | "Apagar 1 foto? Fica 30 dias na lixeira." / "Apagar {n} fotos? Ficam 30 dias na lixeira." |
| Remove cover | "Remover a capa de '{nome}'?" |
| After album delete | "Álbum enviado para a lixeira" |
| After photo delete (viewer) | "Foto enviada para a lixeira" |
| Batch delete | "{n} fotos apagadas" / "{ok} de {total} fotos apagadas. {k}: {motivo}" (one clause per reason) |
| Batch move | "{n} fotos movidas para '{álbum}'" / "{ok} de {total} fotos movidas. {k}: {motivo}" |
| Move from viewer | "Foto movida para '{álbum}'" |
| Order mismatch | "A ordem mudou enquanto você editava. Confira e salve de novo." |
| Upload progress | "Enviando {x} de {n}" (notification and album), "Preparando fotos…" |
| Upload failures | "O álbum foi apagado", "Não foi possível ler esta imagem.", "Imagem grande demais para enviar.", "Não foi possível enviar esta foto.", server reasons as sent |
| Upload summary notification | "{n} fotos não foram enviadas" |
| Other writes, 404 | "Este item não existe mais." |
| Cover | "Capa atualizada", "Capa removida" |
