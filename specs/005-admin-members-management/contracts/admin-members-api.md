# Contract (consumed): Leader Members API

Source of truth: backend `specs/010-members-management/contracts/admin-members-api.md`. This file records only
what the app relies on and how each case is handled. Base URL: `ApiConstants` base (`/ipbcb/`). Every call goes
through `@AuthedRetrofit` (JWT + refresh); the wrong qualifier would be a silent 401.

## Endpoints used

| Call | App method (`MembersAdminApi`) | Success | App handling |
|---|---|---|---|
| `GET api/admin/members/` | `getMembers(ifNoneMatch)` | 200 `{"members": [...]}` / 304 | 304 → keep in-memory list |
| `GET api/admin/members/options/` | `getOptions()` | 200 `{"statuses","roles","ministries"}` | loaded each time the form opens |
| `GET api/admin/members/{id}/` | `getMember(id, ifNoneMatch)` | 200 record / 304 | 304 → cached record |
| `POST api/admin/members/` | `createMember(body: JsonObject)` | 201 record | insert into list |
| `PATCH api/admin/members/{id}/` | `updateMember(id, body: JsonObject)` | 200 record | replace in list |
| `DELETE api/admin/members/{id}/` | `deleteMember(id)` | 204 | remove from list |
| `PUT api/admin/members/{id}/photo/` | `uploadPhoto(id, photo: MultipartBody.Part)` | 200 `{"photo_url"}` | replace `photoUrl` |
| `DELETE api/admin/members/{id}/photo/` | `removePhoto(id)` | 204 | `photoUrl = null` |
| `GET api/admin/members/{id}/history/` | `getHistory(id)` | 200 `{"history": [...]}` | newest first, as sent |
| `GET <photo_url>` | Coil `@MemberPhotoLoader` | image bytes | memory only; any failure → initials |

## Request bodies

POST / PATCH (`JsonObject`, built from `MemberChanges`):

```json
{"name": "Ana Souza", "first_name": "Ana", "last_name": "Souza",
 "birth_date": "1990-04-02", "gender": "F", "status_id": 1, "role_id": null,
 "ministry_ids": [2, 5], "baptism_date": "2005-06-12", "is_active": true}
```

- PATCH carries only changed keys; `null` (`JsonNull`) clears `status_id`, `role_id`, `gender`, dates.
- `ministry_ids` replaces the whole list.
- Any other key → 400. The app never sends `id`, `photo`, `created_at`.

Photo: `multipart/form-data`, part name `photo`, file name `member.jpg`, content type from
`ValidateMemberPhotoUseCase`.

## Response shapes (DTOs, `@Serializable`, `ignoreUnknownKeys` as in the shared `Json`)

```kotlin
NamedRefDto(id: Int, name: String)
MemberSummaryDto(id, name, @SerialName("photo_url") photoUrl: String?, status: NamedRefDto?,
                 @SerialName("is_active") isActive: Boolean)
MemberListDto(members: List<MemberSummaryDto>)
MemberRecordDto(id, name, first_name, last_name, birth_date: String?, gender: String?, status: NamedRefDto?,
                role: NamedRefDto?, ministries: List<NamedRefDto>, baptism_date: String?, is_active: Boolean,
                photo_url: String?, created_at: String)
MemberOptionsDto(statuses, roles, ministries: List<NamedRefDto>)
PhotoUrlDto(photo_url: String)
HistoryEditorDto(id: String, name: String)
HistoryEntryDto(id: Int, editor: HistoryEditorDto?, field: String, old_value: String?, new_value: String?,
                changed_at: String)
HistoryDto(history: List<HistoryEntryDto>)
```

Date strings: `YYYY-MM-DD`; `created_at`/`changed_at`: ISO-8601 with an offset — `Z` or the server's
local offset (e.g. `2026-02-23T21:21:35.359000-03:00`); the app parses any offset.

## Errors

Body `{"error_code", "detail"}` (+ `field_errors` on serializer errors), parsed only by
`Response.toAppError()`.

| Status | `AppError` | App reaction |
|---|---|---|
| 400 `VALIDATION_ERROR` | `Server(400, fieldErrors)` | fields or general form message |
| 401 | `Auth(401)` (after refresh failed) | sign-out path |
| 403 `PERMISSION_DENIED` | `Auth(403)` | leave members area with `detail` |
| 404 `NOT_FOUND` | `Server(404)`, `userMessage` = "Este membro não existe mais" | back to list, member dropped |
| I/O | `Network` | connection message / retry |

## Caching

Every GET answers `ETag` and `Cache-Control: private, no-store`. The app keeps ETags **in memory only** (list and
opened records) and sends `If-None-Match`; nothing is persisted. Options and history are always fetched fresh.
