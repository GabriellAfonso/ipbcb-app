# API contract used by the app (planned backend contract)

Source: `../TODO/specify_bible_videos_backend.md`. Re-check against the backend's spec once implemented.

| Call | Client | Body / params | Success | Errors the app reads |
|---|---|---|---|---|
| `GET api/bible/videos/` | authless | `If-None-Match` | `200` list / `304` | — |
| `POST api/bible/videos/` | authed | `{"url", "places": [{"book", "chapter"}]}` | `201` video | `409 BIBLE_VIDEO_EXISTS` (+ `video`), `400 BIBLE_VIDEO_INVALID_URL`, `400 BIBLE_VIDEO_UNAVAILABLE`, `502/503 BIBLE_VIDEO_LOOKUP_FAILED` |
| `PUT api/bible/videos/{id}/places/` | authed | `[{"book", "chapter"}]` (full list; empty = delete) | `2xx` | `404`, `400` |
| `DELETE api/bible/videos/{id}/places/?book=gn&chapter=1` | authed | no `chapter` = whole book | `2xx` | `404` |
| `DELETE api/bible/videos/{id}/` | authed | — | `204` | `403`, `404` |
| `PUT api/bible/videos/order/` | authed | `{"book", "chapter", "video_ids"}` | `2xx` | `400` (list mismatch) |
| `GET api/bible/progress/` | authed | — | `{"chapters": {"gn": [1]}, "videos": [42]}` | `401` |
| `POST api/bible/progress/sync/` | authed | `{"changes": [{"type": "chapter", "book", "chapter", "done", "changed_at"} \| {"type": "video", "video_id", "done", "changed_at"}]}` (≤ 500) | `{"accepted": [i], "rejected": [{"index", "reason"}]}` | `400` whole batch |

`chapter: null` must be sent explicitly in write bodies (whole book).

Video object:

```json
{"id": 42, "kind": "video", "youtube_id": "dQw4w9WgXcQ", "url": "https://www.youtube.com/watch?v=...",
 "title": "...", "thumbnail_url": "https://i.ytimg.com/vi/.../hqdefault.jpg",
 "places": [{"book": "gn", "chapter": 1, "position": 0}, {"book": "ex", "chapter": null, "position": 3}]}
```
