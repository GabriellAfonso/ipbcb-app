# Implementation Plan: Encrypted Session, Encrypted Member Photo Cache and Member Photo Download

**Branch**: `012-encrypted-session-photo-cache` | **Date**: 2026-10-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-encrypted-session-photo-cache/spec.md`

## Summary

`@AuthPrefs` stays a Preferences DataStore, but its file becomes `auth_prefs.enc.preferences_pb`, written through an
encrypting serializer (AES-256-GCM, non-exportable Android Keystore key). A DataStore migration moves the plain-text
file into it on first read and deletes it; a corruption handler turns any unreadable session into a quiet sign-out
that also wipes the member photo cache. Member photos get their own encrypted LRU cache (50 MB, separate key) that
Coil's `@MemberPhotoLoader` reads through a custom fetcher, revalidating with `If-None-Match` in the background. The
full-screen viewer moves "Apagar" to the bottom next to a new "Baixar", which saves the photo to
`Pictures/IPB Castelo Branco` via MediaStore (API 29+) or the public Pictures directory with a runtime permission
(API 24–28).

Key technical choices (details in [research.md](research.md)):

- **Keystore AES-GCM behind an `AeadCipher` port**, no new library (R1).
- **`EncryptedSerializer` wrapping `PreferencesSerializer`** + `OkioStorage`; callers of `@AuthPrefs` untouched (R2).
- **`PlaintextAuthPrefsMigration`** — DataStore-native, interrupt-safe, never overwrites a newer session (R3).
- **`SessionRecovery.pendingWipe`** consumed by `CoreViewModel` — avoids a DI cycle, covers cold start (R4).
- **Own `EncryptedMemberPhotoCache`** in `noBackupFilesDir`, hashed names, mtime LRU (R6) fed to Coil by
  **`MemberPhotoFetcher`** with stale-while-revalidate and a `revisions` flow (R7).
- **Prune/wipe from `MembersAdminRepositoryImpl`** through a `MemberPhotoStore` domain port (R8).
- **`MemberPhotoGallerySaver`**: MediaStore + `IS_PENDING` on 29+, temp-file + rename + media scan on 24–28 (R9).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (JVM 17 target), Android, `minSdk 24`, `targetSdk 36`

**Primary Dependencies**: existing only — Jetpack Compose (Material 3), Hilt, OkHttp 5 / Retrofit 3, DataStore 1.2.0
(+ its okio storage), Coil 2.6.0, `javax.crypto` + `AndroidKeyStore`. **No new library.**

**Storage**: `files/datastore/auth_prefs.enc.preferences_pb` (sealed); `no_backup/member_photos/*` (sealed, ≤ 50 MB);
shared `Pictures/IPB Castelo Branco` for downloads (plain, outside app control). Formats in
[contracts/on-device-storage.md](contracts/on-device-storage.md).

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; `FakeAeadCipher`, temp directories, fake HTTP for the
photo source. Keystore and MediaStore verified on device ([quickstart.md](quickstart.md)).

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: cached photo on screen < 300 ms after the list data (SC-004); session decrypt adds < 20 ms to
first token read; at most one revalidation in flight per photo.

**Constraints**: no plaintext session or photo on disk; never crash on key/cipher failure; `domain/` without Android;
only `ResponseExt` parses error bodies; Portuguese strings hardcoded; 120-char lines; no manual `CoroutineScope` except
the existing singleton-scope pattern already used by `TokenStorage` (see Complexity Tracking).

**Scale/Scope**: ~12 new production files, ~12 touched; ~9 test classes new or extended; `specs/core/spec.md`,
`specs/admin/spec.md` and `specs/005-admin-members-management/spec.md` updated.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ cipher/IO failures stay in data (`UndecryptableException` → `CorruptionException` or cache miss); download returns `Result<Unit>` with `AppError` |
| Conversion in the data layer | ✅ `MemberPhotoGallerySaver`, `MemberPhotoSource` convert to `AppError` |
| `message` technical, `userMessage` for screen | ✅ write failure carries authored `userMessage`; nothing technical shown on session loss |
| One HTTP error parsing point | ✅ photo fetch only reads status codes; no body parsing |
| 403 is permission, not login | ✅ 403 wipes the cache and follows 005 `LeaveArea` (permission text) |
| Screen text via `toUserMessage()` | ✅ download errors via `toUserMessage()`; permission texts are presentation-only (no `AppError`) |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| Single Activity | ✅ unchanged |
| UI → ViewModel → UseCase → Repository | ✅ `DownloadMemberPhotoUseCase`; cache pruning inside the repository |
| Features don't import each other | ✅ session crypto in `core/data/security`; photo cache inside `features/admin/members` |
| `domain/` without Android | ✅ ports `MemberPhotoStore`, `MemberPhotoExporter` use `String`/`ByteArray`/`LocalDate` |
| Screen/Content split, dumb composables | ✅ viewer gets lambdas/flags; permission launcher lives in the Screen |
| `StateFlow` + `SharedFlow` events, no `Context` in VM | ✅ messages via `MembersEvent.ShowMessage`; SDK/permission checks in the Screen |
| Graph-scoped ViewModel | ✅ unchanged |
| Snapshot cache pattern for offline features | n/a — members stay online-only; the photo cache is a dedicated encrypted store by spec |
| Security: tokens to encrypted storage (pending item in CLAUDE.md) | ✅ done by this feature — update CLAUDE.md's Security bullet |
| No PII in logs | ✅ logs carry member id / cache key prefix only |
| Tests: happy path + 1 error per use case, fakes | ✅ see quickstart |
| Spec and code in the same commit | ✅ 005, `specs/core`, `specs/admin` updated with the code |

**Gate: PASS**. Post-design re-check: PASS — one application-lifetime scope in `MemberPhotoSource` for background
revalidation (justified below); no new library; no feature-to-feature import.

## Project Structure

### Documentation (this feature)

```text
specs/012-encrypted-session-photo-cache/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R11
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── on-device-storage.md
│   └── photo-viewer-ui.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
app/src/main/AndroidManifest.xml                 # + WRITE_EXTERNAL_STORAGE maxSdkVersion=28
app/src/main/res/xml/backup_rules.xml            # exclude auth_prefs.enc.preferences_pb (+ keep old path)
app/src/main/res/xml/data_extraction_rules.xml   # same, cloud-backup and device-transfer

app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── data/security/ (new)
│   │   ├── AeadCipher.kt                 # port + UndecryptableException
│   │   ├── KeystoreAeadCipher.kt         # AndroidKeyStore AES-256-GCM, sealed format v1
│   │   ├── EncryptedSerializer.kt        # OkioSerializer<T> wrapper
│   │   ├── PlaintextAuthPrefsMigration.kt
│   │   └── SessionRecovery.kt            # pendingWipe flag, destroys session key
│   ├── di/DataStoreModule.kt             # @AuthPrefs via OkioStorage + EncryptedSerializer + migration + handler;
│   │                                     # @SessionCipher qualifier
│   └── presentation/viewmodel/CoreViewModel.kt   # collect SessionRecovery.pendingWipe → clearSessionScopedCaches
└── features/admin/members/
    ├── domain/
    │   ├── repository/MemberPhotoStore.kt (new)      # remove(url), retainOnly(urls), wipe()
    │   ├── repository/MemberPhotoExporter.kt (new)   # save(id, name, url, date): Result<Unit>
    │   └── usecase/MemberPhotoUseCases.kt            # + DownloadMemberPhotoUseCase, MemberPhotoFileName
    ├── data/
    │   ├── photo/ (new)
    │   │   ├── EncryptedMemberPhotoCache.kt          # files, LRU 50 MB, wipe (implements MemberPhotoStore)
    │   │   ├── MemberPhotoSource.kt                  # load/bytesFor/revalidate, revisions StateFlow
    │   │   ├── MemberPhotoFetcher.kt                 # Coil Fetcher.Factory<Uri>
    │   │   └── MemberPhotoGallerySaver.kt            # implements MemberPhotoExporter (API 29+ / 24–28)
    │   └── repository/MembersAdminRepositoryImpl.kt  # remove on upload/remove/delete; retainOnly on list 200;
    │                                                 # wipe on AppError.Auth
    ├── di/
    │   ├── MemberPhotoLoaderModule.kt                # + fetcher component, @MemberPhotoCipher, cache/source/saver
    │   └── MembersAdminModule.kt                     # SessionScopedCache also wipes the photo cache
    └── presentation/
        ├── components/MemberAvatar.kt                # + photoRevision in the request key
        ├── components/MemberPhotoViewer.kt           # bottom "Apagar" + "Baixar"; onDownload; previews
        ├── components/MemberCard.kt                  # pass revision
        ├── state/MemberProfileUiState.kt             # canDownloadPhoto, isDownloadingPhoto, photoRevision
        ├── state/MembersListUiState.kt               # photoRevisions
        ├── state/MemberFormUiState.kt                # photoRevision
        ├── viewmodel/MemberProfileViewModel.kt       # onDownload(), revisions
        ├── viewmodel/MembersListViewModel.kt         # revisions
        ├── viewmodel/MemberFormViewModel.kt          # revisions
        └── screens/MemberProfileScreen.kt            # permission launcher (API ≤ 28), wires onDownload

app/src/test/java/com/ipb/castelobranco/
├── core/testing/FakeAeadCipher.kt (new)
├── core/data/security/ EncryptedSerializerTest, PlaintextAuthPrefsMigrationTest, SessionRecoveryTest (new)
├── core/presentation/viewmodel/CoreViewModel…Test   # + pendingWipe case
└── features/admin/members/
    ├── data/photo/ EncryptedMemberPhotoCacheTest, MemberPhotoSourceTest (new), FakeMemberPhotoStore (new)
    ├── data/repository/MembersAdminRepositoryImplTest  # + prune/wipe
    ├── domain/usecase/ MemberPhotoFileNameTest, DownloadMemberPhotoUseCaseTest (new)
    └── presentation/viewmodel/MemberProfileViewModelTest  # + download
```

**Structure Decision**: single `:app` module, existing feature-based layout. Generic crypto and the session store go
in `core/data/security` (core already owns `@AuthPrefs`); everything about member photos stays inside
`features/admin/members`, since no other feature uses it.

## Implementation Order

1. **Crypto core** — `AeadCipher`, `KeystoreAeadCipher`, `EncryptedSerializer`, `FakeAeadCipher` + tests.
2. **Session store** — migration, recovery, `DataStoreModule` wiring, backup rules, `CoreViewModel` wipe + tests.
   Ship-safe on its own (US1, US2).
3. **Photo cache** — `MemberPhotoStore`, `EncryptedMemberPhotoCache`, `MemberPhotoSource`, `MemberPhotoFetcher`,
   loader wiring, repository prune/wipe, `SessionScopedCache`, avatar revisions + tests (US3, US4).
4. **Viewer layout** — bottom "Apagar"; "Baixar" hidden until step 5 is wired (US5).
5. **Download** — `MemberPhotoExporter`, `MemberPhotoGallerySaver`, use case, file name, ViewModel, permission flow,
   manifest + tests (US6).
6. **Specs and docs** — `specs/core/spec.md` (session store, `SessionRecovery`, DataStore table), `specs/admin/spec.md`
   (photo cache, viewer, download), 005 already updated by `/speckit-specify`, CLAUDE.md Security bullet.
7. **Device validation** — [quickstart.md](quickstart.md), API 28 and API 34+.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Application-lifetime `CoroutineScope` in `MemberPhotoSource` (CLAUDE.md: no manual scope) | Revalidation is started by a Coil fetcher, not a ViewModel, and must outlive the screen that triggered it so the cache is updated | `viewModelScope` is unavailable inside a Coil fetcher; blocking the fetcher on revalidation would cancel the "instant" display. Same pattern as `TokenStorage`'s singleton scope |
| Own encrypted cache instead of Coil's disk cache | FR-008 requires encryption at rest; Coil 2's disk cache writes plain files | An encrypting okio `FileSystem` under Coil's journaled cache would need faked sizes/random access — fragile (R6) |
