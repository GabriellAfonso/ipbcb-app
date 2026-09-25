# Quickstart: Validating Protected Media Downloads

**Feature**: `004-protected-media-downloads`

## 1. Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.profile.*"
./gradlew :app:testDebugUnitTest
```

Never `clean`, `--rerun-tasks` or `--no-daemon` (see CLAUDE.md).

Expected: every outcome in spec FR-019 has a passing test.

| Area | Test class | Outcomes |
|------|-----------|----------|
| Gallery loop | `GalleryPhotoDownloaderTest` | success, already present, 404, network error, 401, 403, 429, truncated body |
| Worker result | `GalleryDownloadWorkDecisionTest` | Completed (clean / network failures under and over cap / 404 only), RateLimited at any attempt, Forbidden, Unauthenticated |
| Repository | `GalleryRepositoryImplTest` | stop reasons surface as `AppError`, failed photos not counted |
| Screen state | `GalleryViewModelTest` | `ENQUEUED` attempt 0 → `isPending`; attempt > 0 → `isResuming`; `FAILED` 403 |
| Profile photo | `ProfilePhotoDataSourceTest` | 200, 304, 404, 403, 429, network, 401 |

All media responses come from `FakeGalleryApi` / `FakeProfileApi` (see `contracts/media-download.md`).

## 2. Audit re-check (FR-015)

From `app/src/main/java`:

```bash
grep -rn "AsyncImage\|rememberAsyncImagePainter\|ImageRequest" --include=*.kt .
grep -rn "OkHttpClient.Builder\|openConnection\|URL(" --include=*.kt .
grep -rn "media/" --include=*.kt .
```

Expected: the four `AsyncImage` sites in `research.md` R1, each with a `File` model; `OkHttpClient.Builder` only in
`core/di/HttpClientModule.kt`; no `media/` literal.

## 3. Manual checks on a device

Against a backend with `009-protected-media-access` deployed and nginx switched (staging):

1. **Member, empty gallery** — log in on WiFi. Gallery fills completely. Progress never exceeds photos on disk.
2. **Rate limit** — lower the media rate limit on staging, clear app data, log in. Download pauses with
   "Download pausado…" (no error), resumes by itself, and ends with the complete gallery.
3. **Non-member** — log in with a non-member account. Gallery shows "Disponível apenas para membros." with no login
   button. Server logs show at most one refused media request per run.
4. **Membership revoked with local albums** — revoke membership of an account whose gallery is downloaded, trigger
   "Tentar novamente". Albums stay; the "members only" banner appears above them.
5. **Profile photo** — with a profile photo on screen: toggle airplane mode and reopen → photo stays; forbid the
   file on staging → placeholder, no error.
6. **Expired session** — expire both tokens on the server, open the gallery → logged-out gallery state.

Against the **current** backend (media still public): steps 1 and 5 (airplane mode) behave exactly as before.
