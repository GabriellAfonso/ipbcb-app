# Tasks: Protected Media Downloads

**Input**: Design documents from `specs/004-protected-media-downloads/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/media-download.md, quickstart.md

**Tests**: Required — spec FR-019/FR-020 ask for a test per status-code path, using reusable fake API classes (no
inline stubs). Tests are written first and must fail before the implementation task that follows them.

**Organization**: Tasks are grouped by user story (spec.md) so each story can be implemented, tested and committed
on its own. Each story's commit carries its domain-spec update (CLAUDE.md §7).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US5 from spec.md

## Path Conventions

- Production: `app/src/main/java/com/ipb/castelobranco/`  → abbreviated `main/`
- Tests: `app/src/test/java/com/ipb/castelobranco/` → abbreviated `test/`
- Test commands: `./gradlew :app:testDebugUnitTest --tests "<pattern>"` — never `clean`, `--rerun-tasks`,
  `--no-daemon`

---

## Phase 1: Setup (Shared Test Infrastructure)

**Purpose**: The two fake APIs every test in this feature uses (research R9, contracts/media-download.md).

- [X] T001 [P] Create `FakeGalleryApi : GalleryApi` in `test/features/gallery/data/api/FakeGalleryApi.kt`:
  `var photosResponse: Response<List<GalleryPhotoDto>>` for `getAllPhotos()`; `albumPhotos: Map<Long,
  List<GalleryPhotoDto>>` for `getAlbumPhotos()`; per-URL script for `downloadFile(url)` built with helpers
  `respondBytes(url, bytes)`, `respondError(url, code, body = "{\"detail\":\"...\"}")`,
  `respondIOException(url)`, `respondTruncated(url, bytes)` (a `ResponseBody` whose stream throws `IOException`
  after the first bytes); unscripted URL → `Response.success(bytes)` with a default payload; records every requested
  URL in `requestedUrls: List<String>`
- [X] T002 [P] Create `FakeProfileApi : ProfileApi` in `test/features/profile/data/api/FakeProfileApi.kt`: scripted
  `downloadFile` result (`Response<ResponseBody>` with optional `ETag` header, or thrown `IOException`), records
  `lastIfNoneMatch` and `downloadCalls`; `uploadProfilePhoto`, `deleteProfilePhoto`, `getMeProfile` throw
  `UnsupportedOperationException`
- [X] T003 [P] Create test helpers: `galleryPhoto(id, albumId, url)` / `photoUrl(id)` returning a `GalleryPhotoDto`
  in `test/features/gallery/data/GalleryTestFixtures.kt`, and `tempDirContext(folder: TemporaryFolder): Context`
  (MockK `Context` whose `filesDir` and `cacheDir` are subfolders of `folder`) in
  `test/core/testing/TempDirContext.kt` — in `core/` because gallery and profile tests both use it

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Result types and the atomic save that every gallery story builds on.

**⚠️ CRITICAL**: US1, US2, US4 (gallery part) cannot start until this phase is complete. US3 (profile) and US5 do not
depend on it.

- [X] T004 Create result types in `main/features/gallery/data/download/GalleryDownloadRun.kt` per data-model.md:
  `enum class StopReason { UNAUTHENTICATED, FORBIDDEN, RATE_LIMITED }`; `sealed interface GalleryDownloadRun` with
  `Completed(downloaded, total, failed, networkFailures)` and `Stopped(reason, error: AppError, downloaded, total)`
- [X] T005 Create `sealed interface WorkDecision { Success; Retry; Fail(message: String, code: Int) }` and an empty
  `fun GalleryDownloadRun.toWorkDecision(runAttemptCount: Int, maxRetries: Int): WorkDecision` (TODO body) in
  `main/features/gallery/data/download/GalleryWorkDecision.kt`
- [X] T006 Write `GalleryPhotoStorageTest` in `test/features/gallery/data/local/GalleryPhotoStorageTest.kt` (real
  storage over `tempDirContext`): full stream → file present and `exists()` true; stream throwing mid-copy → exception
  propagates, no `{photoId}.*` file in the album dir, `exists()` false, nothing left in `cacheDir`
- [X] T007 Make `GalleryPhotoStorage.save` atomic in `main/features/gallery/data/local/GalleryPhotoStorage.kt`: copy
  into `File.createTempFile("gallery-$photoId-", ".part", context.cacheDir)`, then `renameTo` the final
  `{albumDir}/{photoId}.{ext}`; on rename failure copy + delete; delete the temp file in `finally`. T006 passes

**Checkpoint**: Types compile, atomic save green.

---

## Phase 3: User Story 1 - Gallery download never loses photos silently (Priority: P1) 🎯 MVP

**Goal**: Only photos actually on disk count; per-photo failures are retried next run; 429 pauses the run and resumes
automatically without an error.

**Independent Test**: `FakeGalleryApi` answers 429 for photo 41 of 100 → run returns `Stopped(RATE_LIMITED,
downloaded = 40)`, 40 files on disk, `toWorkDecision` = `Retry` at any attempt; second run requests only photos
41–100.

### Tests for User Story 1 ⚠️ (write first, must fail)

- [X] T008 [P] [US1] Write `GalleryPhotoDownloaderTest` (US1 cases) in
  `test/features/gallery/data/download/GalleryPhotoDownloaderTest.kt`, real `GalleryPhotoStorage` over
  `tempDirContext` + `FakeGalleryApi`: (a) all 200 → `Completed(total, total, 0, 0)`, files + `.json` on disk;
  (b) photo already on disk → no request for it, counted; (c) 404 on one photo → not on disk, not counted,
  following photos still requested, `Completed(failed = 1)`; (d) `IOException` on one photo → same, `networkFailures
  = 1`; (e) truncated body → not on disk, not counted; (f) 429 on photo 41 of 100 → `Stopped(RATE_LIMITED, downloaded
  = 40, total = 100)`, `requestedUrls` ends at photo 41; (g) rerun after (f) → requests exactly photos 41–100;
  (h) `onProgress` never reports more than the files on disk and is not called for failed photos; (i)
  `onAlbumDone` called once per album on a complete run
- [X] T009 [P] [US1] Write `GalleryDownloadWorkDecisionTest` (US1 cases) in
  `test/features/gallery/data/download/GalleryDownloadWorkDecisionTest.kt`: `Completed` clean → `Success`;
  `Completed` with `networkFailures > 0` and `runAttemptCount < maxRetries` → `Retry`, `>= maxRetries` → `Success`;
  `Completed` with only 404 failures → `Success`; `Stopped(RATE_LIMITED)` → `Retry` at attempt 0, 3 and 10
- [X] T010 [P] [US1] Extend `WorkManagerGalleryDownloadSchedulerTest` in
  `test/features/gallery/data/work/WorkManagerGalleryDownloadSchedulerTest.kt`: both `enqueueWifiOnly` and
  `enqueueAnyNetwork` requests have `workSpec.backoffPolicy == EXPONENTIAL` and `backoffDelayDuration == 60_000`
- [X] T011 [P] [US1] Extend `GalleryViewModelTest` in
  `test/features/gallery/presentation/viewmodel/GalleryViewModelTest.kt`: `ENQUEUED` with `runAttemptCount == 0` →
  `isPending = true, isResuming = false`; `ENQUEUED` with `runAttemptCount = 2` → `isResuming = true, isPending =
  false`, `error == null`
- [X] T012 [P] [US1] Add `GalleryRepositoryImplDownloadTest` in
  `test/features/gallery/data/repository/GalleryRepositoryImplDownloadTest.kt` (`FakeGalleryApi` + real storage;
  the existing `GalleryRepositoryImplTest` keeps its MockK storage for the non-download cases): `downloadAllPhotos` emits progress only for saved photos; 404 photo not counted;
  429 mid-run → flow throws `AppError.Server(code = 429)` and the photos before it stay on disk
  (progress before the error is not asserted: `flowOn` drops buffered items when upstream fails)

### Implementation for User Story 1

- [X] T013 [US1] Update `specs/gallery/spec.md`: §2.3 atomic save via `cacheDir`; §3 per-photo outcome table
  (research R4), worker result table (research R5), explicit exponential backoff 60 s, 429 uncapped; §4 new
  `isResuming` state and banner text "Download pausado. Continua automaticamente em instantes."
- [X] T014 [US1] Implement `GalleryPhotoDownloader` in
  `main/features/gallery/data/download/GalleryPhotoDownloader.kt` (`@Inject constructor(api: GalleryApi, storage:
  GalleryPhotoStorage)`, `suspend fun download(photos, onProgress, onAlbumDone = {}): GalleryDownloadRun`): iterate
  `photos.groupBy { it.albumId }`; `storage.exists` → counted; 2xx with body → `storage.save` +
  `savePhotoMetadata` → counted; 2xx without body, 404, other non-2xx not in {401, 403, 429} → failed; `IOException`
  (including from `save`) → network failure; rethrow `CancellationException`; 429 → return
  `Stopped(RATE_LIMITED, response.toAppError(), downloaded, total)` immediately. Named constants for status codes.
  401/403 temporarily fall into "failed" (US2/US4 add their stops). T008 (a)–(i) pass
- [X] T015 [US1] Implement `toWorkDecision` for `Completed` and `Stopped(RATE_LIMITED)` in
  `main/features/gallery/data/download/GalleryWorkDecision.kt` per research R5. T009 passes
- [X] T016 [US1] Rewire `GalleryDownloadWorker.doWork` in `main/features/gallery/data/work/GalleryDownloadWorker.kt`:
  inject `GalleryPhotoDownloader`; on `getAllPhotos` failure replace the hand-rolled `errorBody()`/`JSONObject`
  parsing with `response.toAppError()` (message = `userMessage ?: message`), keep the 401/403 fail / others retry
  branch; call `downloader.download(photos, onProgress = setProgress, onAlbumDone = repository::preload)`; call
  `repository.preload()` before returning; map `toWorkDecision(runAttemptCount, MAX_RETRIES)` 1:1 to
  `Result.success()` / `retry()` / `failure(workDataOf(KEY_ERROR, KEY_ERROR_CODE))`; drop the `parseApiError` and
  `JSONObject` imports
- [X] T017 [US1] Add `.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS)` (constant
  `BACKOFF_SECONDS`) to both requests in `main/features/gallery/data/work/WorkManagerGalleryDownloadScheduler.kt`.
  T010 passes
- [X] T018 [US1] Make `GalleryRepositoryImpl.processDownload` delegate to `GalleryPhotoDownloader` in
  `main/features/gallery/data/repository/GalleryRepositoryImpl.kt` (inject it; emit `DownloadProgress` from
  `onProgress`; on `Stopped` throw `run.error`). The repository is `@Binds`-bound with an `@Inject` constructor, so
  adding the parameter needs no module change; update the manual construction in `GalleryRepositoryImplTest`.
  T012 passes
- [X] T019 [US1] Add `isResuming: Boolean = false` to `GalleryDownloadState` and split `WorkInfo.State.ENQUEUED` by
  `info.runAttemptCount` in `main/features/gallery/presentation/viewmodel/GalleryViewModel.kt`. T011 passes
- [X] T020 [US1] Add stateless `MessageBanner(text)` (replaces `PendingWifiBanner`, reused for pending, resuming
  and no-access) with "Download pausado. Continua automaticamente em instantes." (no button) and a `downloadState.isResuming -> ResumingBanner()` branch in the banner `when`, right after
  `isDownloading`, in `main/features/gallery/presentation/screens/GalleryScreen.kt`

**Checkpoint**: `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"` green. US1
shippable: `fix(gallery): never count failed photos and pause on media rate limit`.

---

## Phase 4: User Story 2 - Gallery clearly says "no access" (Priority: P1)

**Goal**: 403 on a photo stops the run, fails with 403 (no retry), and shows "members only" with or without local
albums; local photos are kept.

**Independent Test**: `FakeGalleryApi` answers 403 for the first photo → exactly one media request, run
`Stopped(FORBIDDEN)`, decision `Fail(_, 403)`; screen with albums shows the banner, without albums the existing
placeholder without login button.

### Tests for User Story 2 ⚠️

- [X] T021 [P] [US2] Add to `test/features/gallery/data/download/GalleryPhotoDownloaderTest.kt`: 403 on the first
  photo → `Stopped(FORBIDDEN)`, `requestedUrls.size == 1`; 403 on photo 5 → photos 1–4 remain on disk,
  `downloaded == 4`; error is `AppError.Auth(code = 403)` carrying the structured `detail` as `userMessage`
- [X] T022 [P] [US2] Add to `test/features/gallery/data/download/GalleryDownloadWorkDecisionTest.kt`:
  `Stopped(FORBIDDEN)` → `Fail(message, 403)` at attempt 0 and 3; message = `userMessage` when present, else
  `"Disponível apenas para membros."`
- [X] T023 [P] [US2] Add to `test/features/gallery/presentation/viewmodel/GalleryViewModelTest.kt`: `FAILED` with
  `error_code = 403` and non-empty albums → `errorCode == 403`, albums still exposed

### Implementation for User Story 2

- [X] T024 [US2] Update `specs/gallery/spec.md` §3 (403 on media stops the run, no retry, local photos kept) and §4
  (banner slot: "members only" banner when `errorCode == 403` and albums exist)
- [X] T025 [US2] Add the 403 stop to `GalleryPhotoDownloader` in
  `main/features/gallery/data/download/GalleryPhotoDownloader.kt`. T021 passes
- [X] T026 [US2] Add the `Stopped(FORBIDDEN)` branch to `toWorkDecision` with constant
  `NO_ACCESS_MESSAGE = "Disponível apenas para membros."` in
  `main/features/gallery/data/download/GalleryWorkDecision.kt`. T022 passes
- [X] T027 [US2] Reuse `MessageBanner` for the no-access notice (no button) and a banner branch `albums.isNotEmpty()
  && downloadState.errorCode == HTTP_FORBIDDEN -> NoAccessBanner(downloadState.error ?: ...)` in
  `main/features/gallery/presentation/screens/GalleryScreen.kt`; empty-albums branch unchanged

**Checkpoint**: gallery tests green. `fix(gallery): stop and show members-only when media is forbidden`.

---

## Phase 5: User Story 3 - Profile photo under the new rules (Priority: P2)

**Goal**: 403 clears the photo silently; 429 and network errors keep the last photo; 304/404 unchanged.

**Independent Test**: `ProfilePhotoDataSourceTest` with `FakeProfileApi`, starting each case from a stored
`profile_photo.jpg` + ETag.

**Depends on**: Phase 1 only (T002, T003's `tempDirContext`).

### Tests for User Story 3 ⚠️

- [X] T028 [P] [US3] Write `ProfilePhotoDataSourceTest` in
  `test/features/profile/data/photo/ProfilePhotoDataSourceTest.kt` (real `ProfilePhotoCacheStorage` over
  `tempDirContext`, `StandardTestDispatcher`): 200 → new file, ETag saved; stored ETag sent as `If-None-Match`;
  304 → `success(existing file)`, file unchanged; 404 → `success(null)`, file + ETag gone; **403 → `success(null)`,
  file + ETag + last URL gone**; **429 → `success(existing file)`, file + ETag kept**; **`IOException` →
  `success(existing file)`, kept**; 500 → `failure(AppError.Server(500))`, file kept

### Implementation for User Story 3

- [X] T029 [US3] Update the profile photo section of `specs/core/spec.md` with the outcome table from data-model.md
  (Profile)
- [X] T030 [US3] In `main/features/profile/data/photo/ProfilePhotoDataSource.kt` `downloadAndPersist`: add
  `HTTP_FORBIDDEN` branch identical to 404 (clear local, `photoCache.clearAll()`, bump, `null`); add
  `HTTP_TOO_MANY_REQUESTS` branch → `findLastLocalPhotoOrNull()`; wrap the `api.downloadFile` call so an
  `IOException` returns `findLastLocalPhotoOrNull()` (rethrow `CancellationException`). Status codes as private
  constants. T028 passes

**Checkpoint**: `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.profile.*"` green.
`fix(profile): keep or clear the profile photo per protected media outcome`.

---

## Phase 6: User Story 4 - Expired sessions end cleanly (Priority: P2)

**Goal**: A final 401 on media (after `TokenAuthenticator` gave up) stops the gallery run with 401 and leaves the
profile photo untouched; the logged-out UI comes from `isLoggedInFlow` (research R2).

**Independent Test**: `FakeGalleryApi` answers 401 → `Stopped(UNAUTHENTICATED)`, decision `Fail(_, 401)`;
`FakeProfileApi` answers 401 → `failure(AppError.Auth(401))`, local photo kept.

**Depends on**: Phase 3 (downloader and decision exist); T030 for the profile case.

### Tests for User Story 4 ⚠️

- [X] T031 [P] [US4] Add to `test/features/gallery/data/download/GalleryPhotoDownloaderTest.kt`: 401 on photo 3 →
  `Stopped(UNAUTHENTICATED)`, no request after photo 3, photos 1–2 on disk
- [X] T032 [P] [US4] Add to `test/features/gallery/data/download/GalleryDownloadWorkDecisionTest.kt`:
  `Stopped(UNAUTHENTICATED)` → `Fail(message, 401)`, never `Retry`
- [X] T033 [P] [US4] Add to `test/features/profile/data/photo/ProfilePhotoDataSourceTest.kt`: 401 →
  `failure(AppError.Auth(code = 401))`, file + ETag kept

### Implementation for User Story 4

- [X] T034 [US4] Add the 401 stop to `GalleryPhotoDownloader`
  (`main/features/gallery/data/download/GalleryPhotoDownloader.kt`) and the `Stopped(UNAUTHENTICATED)` branch to
  `toWorkDecision` (`main/features/gallery/data/download/GalleryWorkDecision.kt`); make the `when` over
  `StopReason` exhaustive. T031, T032 pass; T033 passes without production change (confirms existing path)
- [X] T035 [US4] Add to `specs/gallery/spec.md` §3 that a final 401 on media stops the run like 401 on the photo list,
  and note in §3.1 that a rejected refresh only clears tokens (`isLoggedInFlow` → logged-out branch), no full logout

**Checkpoint**: all gallery + profile tests green. `fix(gallery): stop the download on an expired session`.

---

## Phase 7: User Story 5 - No media fetched outside the signed-in path (Priority: P3)

**Goal**: Confirm and record FR-015/FR-016.

**Independent Test**: quickstart.md §2 greps return only the expected sites.

- [X] T036 [US5] Run the three greps from `specs/004-protected-media-downloads/quickstart.md` §2 in
  `app/src/main/java`; compare with the table in `research.md` R1; if any site loads a remote media URL or builds a
  non-authenticated client for media, move it to the `@AuthedRetrofit` download path and add a row to R1
- [X] T037 [US5] Add to `specs/gallery/spec.md` §1 (and the profile photo section of `specs/core/spec.md`) the rule
  "images are rendered only from local files; media is fetched only through `@AuthedRetrofit`"

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T038 [P] Remove dead imports/constants left in `main/features/gallery/data/work/GalleryDownloadWorker.kt` and
  `main/features/gallery/data/repository/GalleryRepositoryImpl.kt`; verify 120-char limit on all touched files
- [X] T039 Run the full suite `./gradlew :app:testDebugUnitTest` and fix any regression
- [ ] T040 **Pending — manual, needs staging.** Walk through `specs/004-protected-media-downloads/quickstart.md` §3 device checks on staging (manual; record
  results in the PR description)
- [ ] T041 **Pending — at release time.** Add a release note line to the changelog for the next version: the app must be published after backend
  `009-protected-media-access` and before nginx switches `/ipbcb/media/` (spec FR-018)

---

## Dependencies & Execution Order

### Phase dependencies

- **Phase 1 (Setup)**: none
- **Phase 2 (Foundational)**: after Phase 1 (T006 uses T003) — blocks US1, US2, US4-gallery
- **US1 (Phase 3)**: after Phase 2
- **US2 (Phase 4)**: after US1 (extends `GalleryPhotoDownloader`, `toWorkDecision`, `GalleryScreen`)
- **US3 (Phase 5)**: after Phase 1 only — can run in parallel with Phases 2–4
- **US4 (Phase 6)**: after US1 (gallery part) and T030 (profile part)
- **US5 (Phase 7)**: independent — any time
- **Polish (Phase 8)**: after all stories

### Story graph

```text
Setup ─┬─► Foundational ─► US1 ─┬─► US2
       │                        └─► US4 ◄─ US3
       ├─► US3
       └─► US5 (anytime)
```

### Within each story

Tests first (must fail) → spec update → implementation → tests green → commit (spec + code together).

---

## Parallel Opportunities

- T001, T002, T003 — different files
- US3 (T028–T030) alongside Phase 2 + US1 — no shared files
- US5 (T036–T037) at any time
- Within US1: T008, T009, T010, T011, T012 — five different test files
- Within US2: T021, T022, T023; within US4: T031, T032, T033

### Example: US1 test batch

```text
T008 GalleryPhotoDownloaderTest.kt
T009 GalleryDownloadWorkDecisionTest.kt
T010 WorkManagerGalleryDownloadSchedulerTest.kt
T011 GalleryViewModelTest.kt
T012 GalleryRepositoryImplTest.kt
```

---

## Implementation Strategy

### MVP (US1)

1. Phase 1 → Phase 2 → Phase 3.
2. Validate: gallery tests green; a rate-limited run resumes and completes (quickstart §3 step 2).
3. This alone fixes the permanent-hole bug, which is the highest-impact change once media is rate-limited.

### Incremental delivery

1. MVP (US1) → commit.
2. US2 → commit. Both P1 stories done: gallery is safe for the nginx switch.
3. US3 (can be done earlier, in parallel) → commit.
4. US4 → commit.
5. US5 audit + Polish → release, published before the nginx switch (FR-018).

---

## Notes

- Every commit that changes behavior also updates `specs/gallery/spec.md` or `specs/core/spec.md` (CLAUDE.md §7).
- MockK is used only for `Context` (`tempDirContext`) and the existing `WorkManager`/`WorkInfo` stubs in
  `GalleryViewModelTest`; all media responses come from `FakeGalleryApi` / `FakeProfileApi`.
- No `clean` / `--rerun-tasks` / `--no-daemon` when running tests.
