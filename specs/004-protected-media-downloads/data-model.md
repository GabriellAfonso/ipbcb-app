# Data Model: Protected Media Downloads

**Feature**: `004-protected-media-downloads` | **Date**: 2026-09-25

No new persisted data. The on-disk layout (`filesDir/gallery/{albumId}/{photoId}.{ext}` + `.json`,
`filesDir/profile/profile_photo.{ext}` + ETag/last URL) is unchanged. The types below are in-memory results that
carry each download outcome from the data layer to its caller.

---

## Gallery — `features/gallery/data/download/`

### `PhotoOutcome` (sealed, internal to the downloader)

Result of one photo in a run. See research R4 for the full status table.

| Variant | Counted as downloaded | Run continues |
|---------|-----------------------|---------------|
| `Present` | yes | yes |
| `Saved` | yes | yes |
| `Failed(isNetwork: Boolean)` | no | yes |
| `Stop(reason: StopReason, error: AppError)` | no | **no** |

### `StopReason` (enum)

`UNAUTHENTICATED` (401), `FORBIDDEN` (403), `RATE_LIMITED` (429).

### `GalleryDownloadRun` (sealed) — returned by `GalleryPhotoDownloader.download(photos, onProgress)`

| Variant | Fields | Meaning |
|---------|--------|---------|
| `Completed` | `downloaded`, `total`, `failed`, `networkFailures` | Every photo was tried |
| `Stopped` | `reason: StopReason`, `error: AppError`, `downloaded`, `total` | A requester-level refusal ended the run |

Invariants:
- `downloaded` = number of photos in the list that are on disk when the run ends (`Present` + `Saved`).
- `downloaded + failed == total` for `Completed`; `downloaded <= total` for `Stopped`.
- After `Stopped`, no request was made for any photo after the one that caused the stop.

`onProgress(downloaded, total)` is invoked after each `Present`/`Saved` photo only — never after a `Failed` one.

### `WorkDecision` (sealed) — `GalleryDownloadRun.toWorkDecision(runAttemptCount)`

| Variant | Maps to |
|---------|---------|
| `Success` | `Result.success()` |
| `Retry` | `Result.retry()` |
| `Fail(message: String, code: Int)` | `Result.failure(KEY_ERROR to message, KEY_ERROR_CODE to code)` |

Rules in research R5.

### `GalleryDownloadState` (presentation, existing — one new field)

| Field | Change |
|-------|--------|
| `isResuming: Boolean` | **new** — `ENQUEUED` with `runAttemptCount > 0` (rate-limit or network backoff) |
| `isPending` | now only `ENQUEUED` with `runAttemptCount == 0` |
| others | unchanged |

State transitions of one gallery download:

```text
ENQUEUED(attempt 0) ─► RUNNING ─┬─► SUCCEEDED                      (Completed, or network retries exhausted)
                                ├─► ENQUEUED(attempt n>0) ─► RUNNING  (RateLimited: always;
                                │                                     Completed with network failures: n < 3)
                                ├─► FAILED(403)                     (Forbidden — "members only")
                                └─► FAILED(401)                     (Unauthenticated)
```

---

## Profile — `ProfilePhotoDataSource.downloadAndPersist` (existing signature `Result<File?>`)

| Server outcome | Return | Local photo + ETag + last URL |
|----------------|--------|-------------------------------|
| 200 | `success(newFile)` | replaced, new ETag saved |
| 304 | `success(lastLocal)` | kept |
| 404 | `success(null)` | cleared |
| 403 | `success(null)` | cleared |
| 429 | `success(lastLocal)` | kept |
| network error | `success(lastLocal)` | kept |
| 401, other | `failure(AppError)` | kept |

`ProfilePhotoBus.bump()` fires whenever the local photo changes (200, 404, 403) so the top bar re-reads it.
