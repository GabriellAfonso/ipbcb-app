# Contract: Protected Media Download

**Feature**: `004-protected-media-downloads` | **Backend**: `009-protected-media-access`

The app consumes this contract; it does not define it. Kept here so the fakes in `src/test` script exactly these
responses.

## Request

```http
GET /ipbcb/media/<path>
Authorization: Bearer <access>          # added by AuthInterceptor (@AuthedRetrofit)
If-None-Match: "<etag>"                 # profile photo only, when an ETag is stored
```

`<path>` is the absolute URL the API returns in `image_url` (gallery) or `photo_url` (profile), passed through
`@Url`. Relative URLs are resolved against `@ApiBaseUrl` (profile only, existing `toAbsoluteUrl`).

## Access rules (server-side)

| Prefix | Allowed |
|--------|---------|
| `gallery/` | member |
| `profiles/` | member; the owner always for their own photo |
| `members/` | `view` on the `members` scope (Admin, Liderança; backend 012) — consumed only by the members area (`@MemberPhotoLoader`) |

## Responses

| Status | Headers / body | App handling |
|--------|----------------|--------------|
| `200` | file bytes, `ETag`, `Cache-Control: private, no-cache` | save; profile stores `ETag` |
| `304` | empty | profile: keep local copy. Gallery never sends `If-None-Match` (skips photos already on disk) |
| `401` | JSON `detail` | `TokenAuthenticator` refreshes and retries once; a final 401 stops (gallery) / fails (profile) |
| `403` | JSON `detail` | gallery: stop run, "members only"; profile: clear local copy, no error |
| `404` | — | gallery: skip photo, retry next run; profile: clear local copy |
| `429` | optional `Retry-After` | gallery: stop, `retry()` with exponential backoff (60 s start); profile: keep local copy |

Error bodies are read only through `core/network/error/ResponseExt.kt` (`Response.toAppError()`).

## Photo list endpoints (unchanged)

`GET api/photos/` and `GET api/albums/{id}/photos/` keep their contract from `specs/gallery/spec.md` §2.
