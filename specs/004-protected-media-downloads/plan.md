# Implementation Plan: Protected Media Downloads

**Branch**: `004-protected-media-downloads` | **Date**: 2026-09-25 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-protected-media-downloads/spec.md`

## Summary

The backend now protects `/ipbcb/media/` (401/403/404/429, ETag + 304). The app already downloads media over
`@AuthedRetrofit` and renders only local files, so the access path needs no change (audit in
[research.md](research.md) R1). The work is in **how each outcome is handled**:

- **Gallery**: extract the duplicated per-photo loop from `GalleryDownloadWorker` and
  `GalleryRepositoryImpl.processDownload` into one `GalleryPhotoDownloader` that counts only photos actually on disk,
  continues past per-photo failures (404, I/O), and stops the run on 401/403/429. The worker maps the run result to
  `success` / `retry` (429 uncapped, exponential backoff from 60 s) / `failure(403|401)`. The screen gains a
  "paused" banner for backoff and a "members only" banner when albums exist. Photo writes become atomic so a cut
  stream never leaves a file that looks downloaded.
- **Profile photo**: `ProfilePhotoDataSource.downloadAndPersist` handles 403 (clear, `success(null)`), 429 and I/O
  errors (keep last local, `success`).
- **Tests**: `FakeGalleryApi`, `FakeProfileApi`, real storage over a temp folder.

## Technical Context

**Language/Version**: Kotlin (JVM 17 target), Android

**Primary Dependencies**: Retrofit + OkHttp (`@AuthedRetrofit`, `AuthInterceptor`, `TokenAuthenticator`),
WorkManager 2.10 + Hilt-Work, Coil (local files only), Jetpack Compose, Hilt

**Storage**: app-private files — `filesDir/gallery/{albumId}/`, `filesDir/profile/`; `cacheDir` for in-flight
gallery downloads (new)

**Testing**: JUnit4 + kotlinx-coroutines-test + Turbine; fakes for APIs; MockK only for `Context.filesDir`/`cacheDir`

**Target Platform**: Android (single `:app` module)

**Project Type**: mobile-app

**Performance Goals**: no extra requests — a stopped run makes zero requests after the stopping photo; photos on
disk never requested again

**Constraints**: must work with media both public and protected (release lands before the nginx switch); no new
libraries

**Scale/Scope**: ~9 production files touched, 2 fakes + 5 test classes; 2 domain specs updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing data → presentation | ✅ Downloader returns `AppError` inside `Stopped`; repository throws `AppError`; profile returns `Result` mapped by `mapError()` |
| `message` technical, `userMessage` for screen | ✅ 403 text = `toAppError().userMessage` (structured `detail`) else app text; raw body never shown |
| One single point of HTTP error parsing (`ResponseExt.kt`) | ✅ **Fixes a current violation**: `GalleryDownloadWorker` parses `errorBody()` itself today; it switches to `toAppError()` |
| 401/403 → `AppError.Auth(code)`, others → `AppError.Server(code)` | ✅ Unchanged; 429 arrives as `AppError.Server(429)` |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| Features never import each other | ✅ Gallery and profile changes stay in their own features |
| Presentation only knows the ViewModel | ✅ `GalleryScreen` reads `GalleryDownloadState` only |
| Composables dumb; Screen/content split | ✅ New banner is one stateless composable (`MessageBanner`) in `GalleryScreen.kt` |
| No `Context` in ViewModel | ✅ `isResuming` derived from `WorkInfo.runAttemptCount` |
| Strings in Portuguese, hardcoded; no magic strings | ✅ Status codes as named constants next to the existing `HTTP_UNAUTHORIZED`/`HTTP_FORBIDDEN` |
| Tests: happy path + 1 error per use case; prefer fakes | ✅ All status paths covered; fakes per user request |
| Spec and code in the same commit; domain specs updated | ✅ `specs/gallery/spec.md` §2.3/§3/§4 and `specs/core/spec.md` profile photo section updated with the code |
| Line limit 120, Conventional Commits | ✅ |

**Gate result**: PASS. No violations; Complexity Tracking empty.

**Post-design re-check**: PASS — the one new class (`GalleryPhotoDownloader`) replaces two copies of the same loop
rather than adding a layer.

## Project Structure

### Documentation (this feature)

```text
specs/004-protected-media-downloads/
├── spec.md
├── plan.md                  # this file
├── research.md              # R1–R10
├── data-model.md            # run results, worker decision, profile outcomes
├── quickstart.md            # tests, audit grep, device checks
├── contracts/
│   └── media-download.md    # consumed backend contract
├── checklists/
│   └── requirements.md
└── tasks.md                 # /speckit-tasks
```

### Source code

```text
app/src/main/java/com/ipb/castelobranco/
├── features/gallery/
│   ├── data/
│   │   ├── download/
│   │   │   ├── GalleryPhotoDownloader.kt     # NEW — per-photo loop, PhotoOutcome, GalleryDownloadRun, StopReason
│   │   │   └── GalleryWorkDecision.kt        # NEW — WorkDecision + GalleryDownloadRun.toWorkDecision()
│   │   ├── local/GalleryPhotoStorage.kt      # save() writes via cacheDir, then moves (atomic)
│   │   ├── repository/GalleryRepositoryImpl.kt  # processDownload delegates to downloader; Stopped → throw AppError
│   │   └── work/
│   │       ├── GalleryDownloadWorker.kt      # thin: list → downloader → toWorkDecision → Result; toAppError()
│   │       └── WorkManagerGalleryDownloadScheduler.kt  # explicit EXPONENTIAL backoff, 60 s
│   └── presentation/
│       ├── viewmodel/GalleryViewModel.kt     # GalleryDownloadState.isResuming; ENQUEUED split by runAttemptCount
│       └── screens/GalleryScreen.kt          # MessageBanner: resuming, no-access (403 with albums)
└── features/profile/data/photo/
    └── ProfilePhotoDataSource.kt             # 403 / 429 / IOException branches

app/src/test/java/com/ipb/castelobranco/
├── features/gallery/
│   ├── data/api/FakeGalleryApi.kt            # NEW
│   ├── data/download/GalleryPhotoDownloaderTest.kt       # NEW
│   ├── data/download/GalleryDownloadWorkDecisionTest.kt  # NEW
│   ├── data/repository/GalleryRepositoryImplTest.kt      # extend download cases
│   ├── data/work/WorkManagerGalleryDownloadSchedulerTest.kt  # assert backoff
│   └── presentation/viewmodel/GalleryViewModelTest.kt    # isPending vs isResuming, 403
└── features/profile/
    ├── data/api/FakeProfileApi.kt            # NEW
    └── data/photo/ProfilePhotoDataSourceTest.kt           # NEW

specs/gallery/spec.md                          # §2.3 atomic save, §3 outcome rules + backoff, §4 new states
specs/core/spec.md                             # profile photo outcomes table
```

**Structure Decision**: Single `:app` module, feature-based. New code lives in `features/gallery/data/download/`
because the loop is data-layer logic shared by the worker (`data/work`) and the repository (`data/repository`).

## Design Notes

### `GalleryPhotoDownloader`

```kotlin
class GalleryPhotoDownloader @Inject constructor(
    private val api: GalleryApi,
    private val storage: GalleryPhotoStorage,
) {
    suspend fun download(
        photos: List<GalleryPhotoDto>,
        onProgress: suspend (downloaded: Int, total: Int) -> Unit,
        onAlbumDone: suspend () -> Unit = {},
    ): GalleryDownloadRun
}
```

- Iterates `photos.groupBy { it.albumId }` in order; `onAlbumDone` lets the worker keep its per-album `preload()`.
- Classifies each photo per research R4; `IOException` caught per photo; `CancellationException` rethrown.
- On `Stop` returns immediately — the caller runs `preload()` so photos saved before the stop appear.

### Worker

```text
getAllPhotos() fails     → unchanged branch, error text via toAppError()
downloader.download(...) → run
repository.preload()     → always, before returning
run.toWorkDecision(runAttemptCount) → Result
```

### Screen order (updated `specs/gallery/spec.md` §4)

1. Logged out → unchanged.
2. Banner slot: downloading → progress; `isResuming` → "Download pausado. Continua automaticamente em instantes.";
   pending → WiFi banners; **`errorCode == 403` and albums not empty → "members only" banner**.
3. Albums → grid.
4. No albums → unchanged branches (403 placeholder without login button, 401 with login button, …).

## Implementation Order

1. **Specs** — update `specs/gallery/spec.md` and `specs/core/spec.md` first (CLAUDE.md §7: spec before code,
   committed together with the code of each slice).
2. **Fakes** — `FakeGalleryApi`, `FakeProfileApi`.
3. **Profile photo (US3)** — `ProfilePhotoDataSourceTest` red → `downloadAndPersist` branches → green. Independent of
   the gallery; smallest slice, ships value alone.
4. **Atomic save** — `GalleryPhotoStorage.save` via `cacheDir`; test truncated body leaves nothing behind.
5. **Downloader (US1, US2, US4 core)** — `GalleryPhotoDownloaderTest` red → `GalleryPhotoDownloader` → green.
6. **Worker decision** — `GalleryDownloadWorkDecisionTest` → `toWorkDecision()`; worker rewired; `toAppError()`
   replaces hand-rolled parsing; scheduler backoff + test.
7. **Repository** — `processDownload` delegates; extend `GalleryRepositoryImplTest`.
8. **Presentation** — `isResuming`, banners; extend `GalleryViewModelTest`.
9. **Audit re-check + full test run** — quickstart §1–2.

Suggested commits: `docs(specs)` folded into each `feat`/`fix` per slice (spec + code together):
`fix(profile): handle protected media outcomes for the profile photo`,
`fix(gallery): write photos atomically`,
`fix(gallery): stop on refused media and never count failed photos`,
`feat(gallery): show paused and members-only banners`.

## Risks

| Risk | Mitigation |
|------|------------|
| Uncapped 429 retries on a server that always answers 429 | Exponential backoff (60 s → WorkManager max 5 h); each run needs at least one request; logout/`REPLACE` cancels |
| `cacheDir` → `filesDir` rename fails on some device | Fallback to copy + delete; failure is a per-photo `Failed`, never a counted photo |
| Existing truncated files from the old bug stay "present" forever | Out of scope — cannot be told apart from valid files cheaply; noted for a later cleanup if reported |
| Release ordering missed (nginx switched before app update) | Old app keeps downloading (it already sends the JWT); only miscounts on refusal. Release checklist in spec FR-018 |
| `GalleryViewModelTest` mocks `WorkInfo`; `runAttemptCount` needs stubbing | Existing test style; add the property to the stubs |

## Complexity Tracking

No violations.
