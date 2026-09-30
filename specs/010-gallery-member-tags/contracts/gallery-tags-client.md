# Contract: Gallery Member Tags (app side)

What the app sends and how it reads each answer. The server contract is not redefined: see
`backend/specs/015-gallery-member-tags/contracts/gallery-tags-api.md`.

All calls on `@AuthedRetrofit` (JWT, `PermissionDeniedInterceptor`, `TokenAuthenticator`), paths relative to
`ApiConstants.BASE_PATH`. Errors are turned into `AppError` only by `Response.toAppError()`.

## 1. HTTP calls added to `GalleryApi`

| Call | Method + path | Body | Success read |
|------|---------------|------|--------------|
| `getTaggableMembers` | `GET api/gallery/taggable-members/` | — | `200` `List<GalleryPhotoMemberDto>` |
| `putPhotoMembers` | `PUT api/photos/{id}/members/` | `{"member_ids": [...]}` | `200` `GalleryPhotoDto` |
| `changePhotoMembers` | `POST api/photos/members/` | `{"photo_ids", "add_member_ids", "remove_member_ids"}` | `200` `List<GalleryPhotoDto>` |

Not used: `GET api/gallery/tagged-members/`, `?member_id=` on photo lists.

## 2. Reading a tag write

| Answer | `AppError` | Result | Local effect |
|--------|------------|--------|--------------|
| `200` single | — | `Saved` | `applyLocal(UpsertPhoto)`, then `syncAfterWrite` |
| `200` bulk chunk | — | counted in `updated` | `applyLocal(UpsertPhotos)`; one `syncAfterWrite` at the end |
| `404` (any list extras) | `Server(404)` | `Failed` / chunk failure | `syncAfterWrite` (repository, as any 008 not-found); picker reloads |
| `400` | `Server(400)` | `Failed` | none; server `detail` shown |
| no network | `Network(userMessage = "Sem conexão")` | `Failed` | none |
| `403` | `Auth(403)` | `Failed` (bulk stops) | profile re-read by `PermissionDeniedInterceptor`; controls go away |

`missing_photo_ids` / `missing_member_ids` are never parsed: the sync plus the message are enough.

## 3. Profile

`GET accounts/api/me/profile/` gains `member_id` (`Long?`), read into `MeProfile.memberId` and exposed through
`CurrentMemberRepository.memberId`.

## 4. UI contract (texts)

| Where | Text |
|-------|------|
| Viewer top bar | "ⓘ" (`contentDescription` "Detalhes") |
| Details sheet | description; "Tirada em {dd/MM/yyyy}"; "Nesta foto"; names or "Ninguém marcado"; button "Marcar pessoas" (`manage`) |
| Viewer menu | "Marcar pessoas" (`manage`) |
| Picker | titles "Marcar pessoas" / "Adicionar pessoas" / "Remover pessoas"; search "Buscar pessoa"; "Salvar" / "Adicionar" / "Remover"; "Cancelar"; "Nenhuma pessoa encontrada."; "Nenhuma pessoa cadastrada."; error + "Tentar novamente" |
| Selection bar | "Pessoas" → "Adicionar pessoas", "Remover pessoas" (disabled when nobody tagged) |
| Messages | "Marcações salvas"; "Marcações atualizadas em {n} fotos" ("1 foto"); "Marcações atualizadas em {n} de {total} fotos: {motivo}"; "Algumas fotos ou pessoas não existem mais. Confira e tente de novo."; "Esta foto não está mais no resultado" |
| Gallery root | top-bar "Pessoas"; entry "Minhas fotos" |
| Filter screen | title "Pessoas"; search "Buscar pessoa"; rows "{nome}" + "{n} fotos"/"1 foto"; hint "Fotos com todas as pessoas selecionadas"; "Nenhuma foto com todas essas pessoas."; "Ninguém foi marcado nas fotos ainda."; "Nenhuma pessoa encontrada." |
| Minhas fotos | title "Minhas fotos"; "Você ainda não foi marcado em nenhuma foto." |
