# Research: Protected Media Downloads

**Feature**: `004-protected-media-downloads` | **Date**: 2026-09-25

Every decision below was checked against the code on branch `004-protected-media-downloads` (base `dev` at
`4b1f003`).

---

## R1. Audit — where does the app fetch or show media?

**Decision**: No code change needed for the access path. FR-015/FR-016 are satisfied by the current code; the audit
is recorded here and re-run as a grep in `quickstart.md`.

| Site | What it shows | Source |
|------|---------------|--------|
| `core/presentation/components/TopBar.kt:108` (`AsyncImage`) | Account photo | `File` from `filesDir/profile/profile_photo.*`, resolved in `BaseScreen.kt:122-139` |
| `features/gallery/presentation/components/GalleryComponents.kt:48` | Album thumbnail | `File` from `thumbnailsFlow` |
| `features/gallery/presentation/screens/AlbumScreen.kt:88` | Album grid | `File` from `getLocalPhotos` |
| `features/gallery/presentation/screens/PhotoScreen.kt:187` | Photo viewer | `File` from `photos[index]` |
| `GalleryApi.downloadFile(@Url)` | Gallery photo bytes | `@AuthedRetrofit` |
| `ProfileApi.downloadFile(@Url, If-None-Match)` | Profile photo bytes | `@AuthedRetrofit` |

Nothing else builds an `OkHttpClient` (only `core/di/HttpClientModule.kt`), opens a `URL`, or references `media/`.
No screen passes a `String`/`Uri` model to Coil.

**Alternatives considered**: adding an authenticated Coil `ImageLoader` — rejected, nothing loads remote images.

---

## R2. How does "401 after a failed refresh" reach the existing logout flow?

**Finding**: `TokenAuthenticator` retries once with a refreshed token. When the refresh endpoint answers 401 or 400
it calls `tokenStorage.clear()`. `AuthSession.isLoggedInFlow` maps `tokenStorage.tokensFlow`, so it emits `false`;
`CoreViewModel` collects it into `isLoggedIn`, which the gallery graph already receives as a parameter. The media
request itself finishes as a 401 response.

**Decision**: No change to the auth stack. Media code only has to treat a final 401 as "stop, end in the
unauthenticated state" — the gallery screen then shows its existing "Faça login para acessar a galeria." branch
because `isLoggedIn` is `false`.

**Note**: When the refresh fails for another reason (network, 5xx), tokens are kept and the final 401 surfaces as
the gallery's existing 401 placeholder with the login button. That is today's behavior for `api/photos/` too; not
changed here.

**Alternatives considered**: emitting a `Logout` event on `AuthEventBus` from the media path — rejected, it would
be a second, media-only logout trigger; the spec asks for the existing flow.

---

## R3. One download loop for the worker and the repository

**Finding**: `GalleryDownloadWorker.doWork` and `GalleryRepositoryImpl.processDownload` each hold their own copy of
the per-photo loop, and both have the bug (a failed photo is counted). The repository copy is only reachable through
`downloadAlbum` / `downloadAllPhotos` → `DownloadAllPhotosUseCase`, which has no caller in `main/` today, but the
spec (FR-007) requires both paths to follow the same rules.

**Decision**: Extract the loop into one class, `GalleryPhotoDownloader` (`features/gallery/data/download/`), that
takes the photo list and returns a `GalleryDownloadRun` result (see `data-model.md`). The worker and the repository
both call it and only translate the result: the worker into `ListenableWorker.Result`, the repository into
`DownloadProgress` emissions plus a thrown `AppError` on a stop.

**Rationale**: One place to fix, one place to test. The worker needs an Android `Context` and `WorkerParameters`
and the project has no Robolectric or `work-testing`, so logic inside `doWork` is not unit-testable today; logic in
a plain class is.

**Alternatives considered**: fixing both loops in place — rejected, two copies of the status rules would drift again
(they already differ: the repository throws on an empty body, the worker skips).

---

## R4. Per-photo outcome classification

**Decision**: Each photo request is classified once, from `Response.code()` or the thrown exception:

| Input | Outcome | Effect on the run |
|-------|---------|-------------------|
| Already on disk | `Present` | counted, no request |
| 2xx with body, saved | `Saved` | counted |
| 2xx without body | `Failed` | not counted, continue |
| 404 | `Failed` | not counted, continue |
| other 4xx/5xx (not 401/403/429) | `Failed` | not counted, continue |
| `IOException` | `Failed(network)` | not counted, continue |
| 401 | stop → `Unauthenticated` | end run |
| 403 | stop → `Forbidden` | end run |
| 429 | stop → `RateLimited` | end run |

`GalleryPhotoStorage.save` writes straight to `{photoId}.{ext}`, and `exists()` matches any file named
`{photoId}.*` — so today a stream cut mid-body leaves a truncated file that every later run treats as present, i.e.
a permanent hole. `save` changes to write into `context.cacheDir` first and move the file into the album directory
only after the body is fully copied (rename, with copy+delete fallback). The temporary file lives outside the
gallery tree on purpose: every subdirectory there is read as an album, and any `{photoId}.`-prefixed name there is
read as a saved photo. Metadata JSON is written after the image, as today, so an image without metadata stays
possible only if the process dies between the two writes — existing behavior, unchanged.

**Rationale**: 401/403/429 describe the requester, not the photo — every following request would get the same
answer. 404 and I/O errors describe one photo or one moment.

**Alternatives considered**: stopping on the first `IOException` — rejected by FR-006; the run-level retry below
already covers a connection that is fully down.

---

## R5. How the worker ends each run

**Decision**:

| Run result | `ListenableWorker.Result` | Screen |
|------------|---------------------------|--------|
| Complete, no network failures | `success()` | resolved, no error |
| Complete, some `Failed(network)` | `retry()` while `runAttemptCount < MAX_RETRIES`, else `success()` | pending, then resolved |
| Complete, only non-network failures (404…) | `success()` — retried on the next trigger | resolved, no error |
| `RateLimited` | `retry()`, **not** capped by `MAX_RETRIES` | "paused" banner, no error |
| `Forbidden` | `failure(error, 403)` | "members only" |
| `Unauthenticated` | `failure(error, 401)` | existing 401 / logged-out branch |
| Photo list request fails | unchanged (401/403 fail, others retry up to `MAX_RETRIES`) | unchanged |

The worker calls `repository.preload()` after each album *and* before returning on any stop, so photos saved before
a stop show up immediately.

The error text for 403/401 comes from `Response.toAppError()` (`userMessage` when the body is structured), falling
back to app text `"Disponível apenas para membros."` for 403. This also removes the worker's hand-rolled
`errorBody()` parsing, which today breaks the constitution rule "one single point of HTTP error parsing".

**Rationale for uncapped 429**: every rate-limited run saves at least the photos before the limit, so the run always
makes progress and ends. Capping would turn a rate limit into a visible failure, which FR-003 forbids.

**Backoff**: set explicitly on both requests in `WorkManagerGalleryDownloadScheduler`:
`BackoffPolicy.EXPONENTIAL`, 60 s initial delay (WorkManager default is 30 s). `Retry-After` is not honored —
`CoroutineWorker` cannot set a per-attempt delay, and re-enqueueing with `setInitialDelay` would need `REPLACE`,
which races with the running worker. Exponential backoff from 60 s is well above any sane media rate-limit window.

**Alternatives considered**: `success()` + re-enqueue with `initialDelay = Retry-After` — rejected, see above.

---

## R6. What the screen shows during a rate-limit pause

**Finding**: A worker in backoff is `WorkInfo.State.ENQUEUED`. Today every `ENQUEUED` maps to `isPending`, which
shows "Aguardando WiFi…" — wrong when the device is on WiFi and the run is waiting on the server.

**Decision**: `GalleryViewModel` maps `ENQUEUED` with `runAttemptCount > 0` to a new flag `isResuming`, shown as a
neutral banner: "Download pausado. Continua automaticamente em instantes." It is not an error and has no button.
`ENQUEUED` with `runAttemptCount == 0` keeps today's WiFi banners.

**Alternatives considered**: no UI change — rejected, a member on WiFi reading "Aguardando WiFi" would reasonably
think the app is broken.

---

## R7. "Members only" visible with albums on the device

**Finding**: `GalleryScreen` only reads `errorCode` inside the `albums.isEmpty()` branch, so a 403 with local albums
is silent.

**Decision**: Show the notice through the shared `MessageBanner` in the existing banner slot (above the grid) when
`downloadState.errorCode == 403` and albums exist. No button. When albums are empty the existing
`PermissionErrorPlaceholder(showLoginButton = false)` stays. Local photos are kept (spec FR-008, option A).

---

## R8. Profile photo outcomes

**Decision**: Extend the `when` in `ProfilePhotoDataSource.downloadAndPersist`:

| Input | Result | Local copy |
|-------|--------|------------|
| 304 | `success(lastLocal)` | kept (unchanged) |
| 404 | `success(null)` | removed + bump (unchanged) |
| **403** | `success(null)` | **removed + cache cleared + bump** |
| **429** | `success(lastLocal)` | kept |
| **`IOException`** | `success(lastLocal)` | kept |
| 401 / other | `failure(AppError)` | kept |

403 returns `success(null)` — same shape as 404 — so no caller treats it as an error (FR-009). 429 and network
return the last local file so the top bar is untouched (FR-010). Callers (`ProfileViewModel.observeProfile`,
`CoreViewModel.refreshProfileOnAppOpen`) already ignore the returned `Result`; no presentation change.

**Alternatives considered**: returning `failure` for 429/network and relying on callers ignoring it — rejected, the
data source would then depend on every caller's discipline to honor "no error shown".

---

## R9. Test doubles

**Decision**: Two reusable fakes in `src/test`, implementing the Retrofit interfaces:

- `features/gallery/data/api/FakeGalleryApi` — photo list response plus a per-URL script of responses
  (`Response.success(body)`, `Response.error(code, body)`, or a thrown `IOException`); records requested URLs so a
  test can assert "no request after the 403".
- `features/profile/data/api/FakeProfileApi` — scripted `downloadFile` responses (with headers for `ETag`) and the
  recorded `If-None-Match`; the other methods fail loudly if a test calls them unexpectedly.

Storage stays real (`GalleryPhotoStorage`, `ProfilePhotoCacheStorage`) over a JUnit `TemporaryFolder`; the only
MockK use is `Context.filesDir` → the temp folder, since both storages read `context.filesDir`.

**Rationale**: CLAUDE.md prefers fakes; the user asked for fake API classes, no inline stubs. A real storage over a
temp dir tests the "not saved / not counted" rules against actual files.

**Worker coverage**: the worker keeps only the result → `ListenableWorker.Result` translation, through a pure
function `GalleryDownloadRun.toWorkDecision(runAttemptCount)` returning a small sealed `WorkDecision`
(`Success`, `Retry`, `Fail(message, code)`). That function is unit-tested; `doWork` maps it 1:1.

---

## R10. Release compatibility

**Decision**: Nothing branches on "is media protected yet". Before the nginx switch the server never answers
401/403/429 for media and the new branches are simply unused; `If-None-Match` is already sent for the profile photo
and a public server that ignores it answers 200, which is today's behavior.
