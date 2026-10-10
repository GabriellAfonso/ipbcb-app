# Tasks: Encrypted Session, Encrypted Member Photo Cache and Member Photo Download

**Input**: Design documents from `specs/012-encrypted-session-photo-cache/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/on-device-storage.md,
contracts/photo-viewer-ui.md, quickstart.md

**Tests**: required by `CLAUDE.md` (happy path + 1 error per use case, fakes preferred); the list is in quickstart.md.

Paths below are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and
`app/src/test/java/com/ipb/castelobranco/` (test) unless they start with `specs/`, `app/` or `CLAUDE.md`.

Run tests with `./gradlew :app:testDebugUnitTest --tests "<package>.*"` — never `clean`, `--rerun-tasks` or
`--no-daemon`.

## Phase 1: Setup

No setup: no new library, no new module (plan §Technical Context).

## Phase 2: Foundational (blocking for every story)

**Purpose**: the AES-GCM cipher used by both the session store (US1, US2) and the photo cache (US3, US4).

- [X] T001 [P] Create `AeadCipher` interface (`fun encrypt(plain: ByteArray, associatedData: ByteArray): ByteArray`, `fun decrypt(sealed: ByteArray, associatedData: ByteArray): ByteArray`, `fun destroyKey()`) and `class UndecryptableException(cause: Throwable? = null) : Exception(cause)` in `core/data/security/AeadCipher.kt` (research R1)
- [X] T002 Implement `KeystoreAeadCipher(alias: String) : AeadCipher` in `core/data/security/KeystoreAeadCipher.kt`: AES-256 key in `AndroidKeyStore` created lazily on first `encrypt` (`PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`, `ENCRYPTION_PADDING_NONE`, `setRandomizedEncryptionRequired(true)`, no user auth); `AES/GCM/NoPadding`, 128-bit tag, `updateAAD(associatedData)`; sealed format `[0x01][12-byte IV][ciphertext‖tag]` (contracts/on-device-storage.md §Sealed blob); `decrypt` with missing key, wrong version, short blob or any `GeneralSecurityException`/`KeyStoreException`/`ProviderException` throws `UndecryptableException` and never creates a key; `destroyKey()` deletes the alias, ignoring errors; alias constants `SESSION_KEY_ALIAS = "ipbcb_session_v1"`, `MEMBER_PHOTOS_KEY_ALIAS = "ipbcb_member_photos_v1"` in the companion
- [X] T003 [P] Create `FakeAeadCipher` in `core/testing/FakeAeadCipher.kt`: reversible transform (XOR with a key byte) prefixed with version byte and a checksum of plain + associated data; flags `loseKey()` (next `decrypt` throws `UndecryptableException`), `failNextEncrypt`, counter `destroyed`; `destroyKey()` changes the key byte so old blobs fail
- [X] T004 Create `EncryptedSerializer<T>(delegate: OkioSerializer<T>, cipher: AeadCipher, label: String) : OkioSerializer<T>` in `core/data/security/EncryptedSerializer.kt`: `defaultValue` = delegate's; `writeTo` serializes into an `okio.Buffer`, writes `cipher.encrypt(bytes, label.encodeToByteArray())`; `readFrom` reads all bytes, empty → `defaultValue`, decrypts and delegates; `UndecryptableException` or delegate failure → `androidx.datastore.core.CorruptionException` (research R2)
- [X] T005 [P] Test `EncryptedSerializer` with `PreferencesSerializer` + `FakeAeadCipher`: round trip of a `auth_tokens` string; empty file → empty prefs; tampered byte, lost key, wrong label → `CorruptionException` in `core/data/security/EncryptedSerializerTest.kt`

**Checkpoint**: cipher and serializer ready — stories can start.

## Phase 3: User Story 1 — Session survives the update, now encrypted (P1) 🎯 MVP

**Goal**: `@AuthPrefs` is written only encrypted to `auth_prefs.enc.preferences_pb`; signed-in users stay signed in;
the plain-text file is deleted.

**Independent test**: install over the previous build while signed in → still signed in, only the encrypted file in
`files/datastore`, no token fragments readable (quickstart §Device checks 1).

- [X] T006 [US1] Create `PlaintextAuthPrefsMigration(oldFile: File) : DataMigration<Preferences>` in `core/data/security/PlaintextAuthPrefsMigration.kt`: `shouldMigrate` = `oldFile.exists()`; `migrate(current)` returns `current` when it already has key `auth_tokens`, else reads `oldFile` with `PreferencesSerializer.readFrom(oldFile.source().buffer())` and returns it (any read failure → `current`); `cleanUp()` deletes `oldFile` (research R3). Keep the key name in a constant shared with `TokenStorage` (`AUTH_TOKENS_KEY = "auth_tokens"`) and use it in `features/auth/data/local/TokenStorage.kt` — no behaviour change there
- [X] T007 [US1] Add `@SessionCipher` qualifier and provide `KeystoreAeadCipher(SESSION_KEY_ALIAS)` in `core/di/DataStoreModule.kt`; rebuild `@AuthPrefs` with `PreferenceDataStoreFactory.create(storage = OkioStorage(FileSystem.SYSTEM, EncryptedSerializer(PreferencesSerializer, sessionCipher, "auth_prefs")) { File(context.filesDir, "datastore/auth_prefs.enc.preferences_pb").absolutePath.toPath() }, migrations = listOf(PlaintextAuthPrefsMigration(context.preferencesDataStoreFile("auth_prefs"))))`; file-name constants, no magic strings
- [X] T008 [P] [US1] Exclude `datastore/auth_prefs.enc.preferences_pb` (keep the old path too) in `app/src/main/res/xml/backup_rules.xml` and in both `<cloud-backup>` and `<device-transfer>` of `app/src/main/res/xml/data_extraction_rules.xml` (research R5)
- [X] T009 [P] [US1] Test `PlaintextAuthPrefsMigration` on a temp dir: no old file → `shouldMigrate` false; old file with tokens + empty current → tokens returned and `cleanUp` deletes it; old file + current with tokens → current kept; corrupt old file → current (signed out), no throw in `core/data/security/PlaintextAuthPrefsMigrationTest.kt`
- [X] T010 [US1] End-to-end store test: build the `@AuthPrefs` store as in T007 but with `FakeAeadCipher` and a temp dir; write tokens via `TokenStorage`; assert the file bytes do not contain the token string and a new store instance reads them back; with a plain old file present, the first read returns its tokens and the old file is gone in `core/data/security/EncryptedAuthPrefsStoreTest.kt`

**Checkpoint**: US1 deliverable on its own (ship-safe: callers unchanged).

## Phase 4: User Story 2 — Session cannot be read: quiet sign-out (P1)

**Goal**: an unreadable session becomes empty, destroys the key, wipes session-scoped caches and shows the app
signed out — no message, no crash.

**Independent test**: corrupt the encrypted file and reopen → signed out, no error, sign in again works
(quickstart §Device checks 2).

- [X] T011 [US2] Create `SessionRecovery` (`@Singleton`, `@Inject` with `@SessionCipher cipher`) in `core/data/security/SessionRecovery.kt`: `val pendingWipe: StateFlow<Boolean>`; `fun onSessionUnreadable()` calls `cipher.destroyKey()` and sets `pendingWipe = true`; `fun acknowledgeWipe()` sets it back to `false`; logs `Timber.w("Session unreadable; signed out")` without content (research R4)
- [X] T012 [US2] Add `corruptionHandler = ReplaceFileCorruptionHandler { recovery.onSessionUnreadable(); emptyPreferences() }` to the `@AuthPrefs` factory in `core/di/DataStoreModule.kt` (inject `SessionRecovery`)
- [X] T013 [US2] In `core/presentation/viewmodel/CoreViewModel.kt` `initialize()`: inject `SessionRecovery`, launch a collector on `pendingWipe` that, when `true`, runs `clearSessionScopedCaches()` then `acknowledgeWipe()`; no event, no message (the empty session already makes `isLoggedIn` false)
- [X] T014 [P] [US2] Test `SessionRecovery`: `onSessionUnreadable` destroys the key and raises the flag, `acknowledgeWipe` lowers it in `core/data/security/SessionRecoveryTest.kt`
- [X] T015 [US2] Extend `EncryptedAuthPrefsStoreTest.kt` (T010): with the handler wired, a lost key / corrupted file reads as empty prefs, `SessionRecovery.pendingWipe` is `true`, and a subsequent write + read with the new key succeeds in `core/data/security/EncryptedAuthPrefsStoreTest.kt`
- [X] T016 [US2] Extend `core/presentation/viewmodel/CoreViewModelTest.kt`: `pendingWipe = true` at `initialize()` clears every `SessionScopedCache` (fake set) and acknowledges, even when the user was never logged in this process

**Checkpoint**: session part (US1 + US2) complete.

## Phase 5: User Story 3 — Member photos open instantly (P2)

**Goal**: photos come from an encrypted 50 MB LRU cache and are revalidated in the background; changed photos are
replaced on screen.

**Independent test**: open list/profiles, force-stop, reopen → photos appear with the cards; directory holds only
hashed sealed files (quickstart §Device checks 3–4).

- [X] T017 [P] [US3] Create `MemberPhotoStore` domain port (`suspend fun remove(url: String)`, `suspend fun retainOnly(urls: Set<String>)`, `suspend fun wipe()`) in `features/admin/members/domain/repository/MemberPhotoStore.kt` (research R8)
- [X] T018 [US3] Implement `EncryptedMemberPhotoCache(dir: File, cipher: AeadCipher, maxBytes: Long = 50 MB) : MemberPhotoStore` in `features/admin/members/data/photo/EncryptedMemberPhotoCache.kt`: `data class CachedPhoto(etag: String?, mimeType: String, bytes: ByteArray)`; key = SHA-256 hex of the URL's path (`Uri`-free: `java.net.URI(url).path`); payload `[u16 etag len][etag][u16 mime len][mime][bytes]` sealed with associated data `"member_photo:" + key` (contracts/on-device-storage.md §Member photo cache); `get(url)` decrypts, touches `setLastModified(now)`, `UndecryptableException`/bad payload → delete file + `null`; `put(url, photo)` writes to `<key>.tmp` then renames, then evicts oldest by `lastModified` while total > `maxBytes`; `remove`, `retainOnly` (delete files whose key is not among the hashed urls), `wipe` (delete dir recursively + `cipher.destroyKey()`); all under one `Mutex`, IO on `Dispatchers.IO`; logs only the first 8 chars of the key (research R6)
- [X] T019 [US3] Create `MemberPhotoSource` (`@Singleton`) in `features/admin/members/data/photo/MemberPhotoSource.kt`: constructor `cache: EncryptedMemberPhotoCache`, `@Client client: OkHttpClient`, application scope `CoroutineScope(SupervisorJob() + Dispatchers.IO)` (justified in plan §Complexity Tracking); `val revisions: StateFlow<Map<String, Int>>`; `suspend fun load(url): CachedPhoto?` — hit → return and `launchRevalidation(url, etag)` (skip if one is in flight for that url); miss → `GET`, `200` → `put` + return (mime from `Content-Type`, else sniff with `ValidateMemberPhotoUseCase` signatures), `401`/`403` → `cache.wipe()` + bump all + `null`, else `null`; revalidation sends `If-None-Match`: `304` → nothing, `200` → `put` + bump, `404` or other non-auth 4xx → `remove` + bump, `401`/`403` → wipe + bump all, `429`/IO error → keep; `suspend fun bytesFor(url): Result<CachedPhoto>` (cache or network, failures as `AppError` via `toAppError()`) for US6 (research R7)
- [X] T020 [US3] Create `MemberPhotoFetcher` (`Fetcher.Factory<Uri>`) in `features/admin/members/data/photo/MemberPhotoFetcher.kt`: for `http`/`https` URIs calls `source.load(uri.toString())`; returns `SourceResult(ImageSource(Buffer().write(bytes), options.context), mimeType, DataSource.DISK or NETWORK)`; `null` → throw `IOException` so Coil shows the error slot (initials)
- [X] T021 [US3] Wire in `features/admin/members/di/MemberPhotoLoaderModule.kt`: `@MemberPhotoCipher` qualifier providing `KeystoreAeadCipher(MEMBER_PHOTOS_KEY_ALIAS)`; provide `EncryptedMemberPhotoCache(File(context.noBackupFilesDir, "member_photos"), cipher)` as singleton and bind it as `MemberPhotoStore`; add `.components { add(MemberPhotoFetcher.Factory(source)) }` to the loader (keep `diskCache(null)`); update the KDoc (photos now on disk, encrypted)
- [X] T022 [US3] Add `photoRevision: Int = 0` to `MemberAvatar` in `features/admin/members/presentation/components/MemberAvatar.kt`: build an `ImageRequest` with `data(photoUrl)`, `memoryCacheKey("$photoUrl#$photoRevision")` (picked `ByteBuffer` path unchanged); pass it from `MemberCard.kt` and `MemberPhotoViewer.kt` (new `photoRevision` param, contracts/photo-viewer-ui.md)
- [X] T023 [US3] Expose revisions: `photoRevisions: Map<String, Int>` in `presentation/state/MembersListUiState.kt` (combined from `MemberPhotoSource.revisions` in `MembersListViewModel.kt`), `photoRevision` in `MemberProfileUiState.kt` / `MemberFormUiState.kt` (from `revisions[photoUrl] ?: 0` in their ViewModels); pass them in `MembersListScreen.kt`, `MemberProfileScreen.kt`, `MemberFormScreen.kt` — inject `MemberPhotoSource` through a small `ObserveMemberPhotoRevisionsUseCase` in `features/admin/members/domain/usecase/MemberPhotoUseCases.kt` backed by a `MemberPhotoRevisions` domain port (`val revisions: Flow<Map<String, Int>>`) implemented by `MemberPhotoSource`
- [X] T024 [US3] In `features/admin/members/data/repository/MembersAdminRepositoryImpl.kt` inject `MemberPhotoStore`: after `uploadPhoto`/`removePhoto` success `remove(oldUrl)` (read from `records`/`members` before `updatePhoto`); after delete success `remove(url of the deleted member)`; after `refreshMembers` `200` `retainOnly(urls of the new list)` (not on `304`)
- [X] T025 [P] [US3] Create `FakeMemberPhotoStore` (records `removed`, `retained`, `wipes`) in `features/admin/members/data/photo/FakeMemberPhotoStore.kt` and update the repository construction in existing tests (`MembersAdminRepositoryImplTest.kt`)
- [X] T026 [P] [US3] Test `EncryptedMemberPhotoCache` on a temp dir with `FakeAeadCipher`: put/get round trip with etag and mime; file name is a 64-char hex and contains no URL text; plain bytes not in the file; LRU evicts oldest past the limit (small `maxBytes`); `retainOnly`; `remove`; `wipe` deletes dir and destroys key; lost key → `get` is `null` and the file is gone in `features/admin/members/data/photo/EncryptedMemberPhotoCacheTest.kt`
- [X] T027 [P] [US3] Test `MemberPhotoSource` with an `OkHttpClient` whose fake application `Interceptor` returns scripted responses and records request headers (no mockwebserver — not a dependency): miss 200 → cached; hit → returned without waiting + revalidation sends `If-None-Match`; 304 keeps; 200 replaces + revision bump; 404 removes + bump; 401/403 wipe; 429 keeps in `features/admin/members/data/photo/MemberPhotoSourceTest.kt` (use `StandardTestDispatcher` injected through a constructor-param scope)
- [X] T028 [US3] Extend `features/admin/members/data/repository/MembersAdminRepositoryImplTest.kt`: upload/remove/delete call `remove(oldUrl)`; list 200 calls `retainOnly`, 304 does not

**Checkpoint**: US3 deliverable — photos cached and instant.

## Phase 6: User Story 4 — Losing access wipes the photo cache (P2)

**Goal**: sign-out, failed renewal, unreadable session or a 401/403 on the list/record/photo erase the cache and its key.

**Independent test**: fill cache, then sign out / remove `members` access / expire session → directory gone
(quickstart §Device checks 5).

- [X] T029 [US4] In `features/admin/members/di/MembersAdminModule.kt` make the members `SessionScopedCache` also call `photoStore.wipe()` (covers logout, refresh failure via `isLoggedInFlow`, and US2's `pendingWipe`)
- [X] T030 [US4] In `features/admin/members/data/repository/MembersAdminRepositoryImpl.kt`: when `refreshMembers` or `getMember` fail with `AppError.Auth` (401 after renewal or 403), call `photoStore.wipe()` before returning the failure (the screen keeps leaving the area per 005 FR-003)
- [X] T031 [US4] Extend `MembersAdminRepositoryImplTest.kt`: list 403 and record 401 → one wipe; 500 → no wipe
- [X] T032 [P] [US4] Test the members `SessionScopedCache` clears repository, memory cache and photo store (fakes) in `features/admin/members/di/MembersSessionCacheTest.kt`

**Checkpoint**: photo cache lifecycle complete (US3 + US4).

## Phase 7: User Story 5 — Full-screen viewer with "Apagar" and "Baixar" at the bottom (P2)

**Goal**: new viewer layout per contracts/photo-viewer-ui.md; "Baixar" visible to `manage` with a photo.

**Independent test**: open the viewer as Admin, Liderança, view-only, and on a member without photo; check buttons
and positions (quickstart §Device checks 6).

- [X] T033 [US5] Rework `MemberPhotoViewer` in `features/admin/members/presentation/components/MemberPhotoViewer.kt`: top-left only "Trocar foto"; remove the trash `IconButton`; bottom `Row` aligned `BottomCenter` with `safeDrawingPadding()` + 16 dp, `Arrangement.spacedBy(16.dp)`: "Apagar" (`Icons.Filled.Delete` + text) when `onRemovePhoto != null && photoUrl != null`, "Baixar" (`Icons.Filled.Download` + text) when `onDownload != null && photoUrl != null`, same translucent `TextButton` style as "Trocar foto"; new params `onDownload: (() -> Unit)?`, `photoRevision`; `isBusy` disables all but close; update KDoc; add `@Preview`s for owner, manage, view-only and no-photo with `imageLoader = null`
- [X] T034 [US5] Add `canDownloadPhoto` (= `canManage`) and `isDownloadingPhoto` to `features/admin/members/presentation/state/MemberProfileUiState.kt`, set `canDownloadPhoto` where `canChangePhoto` is set in `MemberProfileViewModel.kt`; in `MemberProfileScreen.kt` pass `onDownload = actions.onDownloadPhoto.takeIf { canDownloadPhoto }` and `isBusy = isBusy || isDownloadingPhoto` (the action is a no-op until T041)
- [X] T035 [US5] Extend `features/admin/members/presentation/viewmodel/MemberProfileFlagsTest.kt`: owner → download + remove; manage → download only; view → none

**Checkpoint**: US5 visible; "Baixar" wired in US6.

## Phase 8: User Story 6 — Leader saves a member photo to the device gallery (P3)

**Goal**: "Baixar" saves the shown photo, unencrypted, to `Pictures/IPB Castelo Branco` on API 24–36, with the storage
permission on API ≤ 28 and no partial files.

**Independent test**: download on API 34 and API 28 (allow, deny, deny permanently), offline with and without cache
(quickstart §Device checks 7–9).

- [X] T036 [P] [US6] Create `MemberPhotoExporter` domain port (`suspend fun save(fileBaseName: String, url: String): Result<Unit>`) in `features/admin/members/domain/repository/MemberPhotoExporter.kt`
- [X] T037 [P] [US6] Create `MemberPhotoFileName` (pure: `fun build(name: String, memberId: Int, date: LocalDate): String` — remove `\ / : * ? " < > |` and control chars, collapse spaces, trim, max 100 chars, empty → `"Membro $memberId"`, append `" yyyy-MM-dd"`) in `features/admin/members/domain/usecase/MemberPhotoUseCases.kt`, and `DownloadMemberPhotoUseCase(exporter, dateProvider)` (`invoke(id, name, url): Result<Unit>`, today from the existing `DateProvider`) in the same file (research R9)
- [X] T038 [US6] Implement `MemberPhotoGallerySaver(@ApplicationContext context, source: MemberPhotoSource) : MemberPhotoExporter` in `features/admin/members/data/photo/MemberPhotoGallerySaver.kt`: bytes + mime from `source.bytesFor(url)`; extension from mime (`jpg`/`png`/`webp`/`gif`); API ≥ 29: `MediaStore.Images.Media` insert with `DISPLAY_NAME`, `MIME_TYPE`, `RELATIVE_PATH = "Pictures/IPB Castelo Branco"`, `IS_PENDING = 1`, write, set `IS_PENDING = 0`; any exception → `resolver.delete(uri)`; API 24–28: `Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)/IPB Castelo Branco`, `mkdirs`, first free name (`name.ext`, `name (1).ext`, …), write `.<name>.tmp`, `renameTo`, `MediaScannerConnection.scanFile`; failure deletes the temp; storage failures → `AppError.Unknown(message = technical, userMessage = "Não foi possível salvar a foto.")`; folder name constant `"IPB Castelo Branco"`; bind as `MemberPhotoExporter` in `features/admin/members/di/MemberPhotoLoaderModule.kt`
- [X] T039 [P] [US6] Add `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` to `app/src/main/AndroidManifest.xml` (contracts/on-device-storage.md §Manifest)
- [X] T040 [US6] In `features/admin/members/presentation/viewmodel/MemberProfileViewModel.kt` add `fun onDownloadPhoto()`: ignore if `!canDownloadPhoto`, no photo, or `isDownloadingPhoto`; set `isDownloadingPhoto`; call `DownloadMemberPhotoUseCase(id, name, photoUrl)`; success → `MembersEvent.ShowMessage("Foto salva em Imagens/IPB Castelo Branco")`; failure → `ShowMessage(error.toAppError().toUserMessage())` (a 403 here is a write-like action: message only, no `LeaveArea`); always reset the flag; texts as constants
- [X] T041 [US6] In `features/admin/members/presentation/screens/MemberProfileScreen.kt` add the permission flow: `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` for `WRITE_EXTERNAL_STORAGE`; on "Baixar": `Build.VERSION.SDK_INT >= Q` or already granted → `viewModel.onDownloadPhoto()`; else launch; granted → download; denied → if `activity.shouldShowRequestPermissionRationale(...)` is false → snackbar "Permita o acesso ao armazenamento nas configurações do aparelho para salvar a foto.", else "Para salvar a foto, permita o acesso ao armazenamento." (use the existing `findActivity()`; texts as constants; messages through the screen's existing snackbar host)
- [X] T042 [P] [US6] Test `MemberPhotoFileName` (forbidden chars, long names, blank name fallback, date format) and `DownloadMemberPhotoUseCase` (success, exporter failure passed through) with a fake exporter in `features/admin/members/domain/usecase/MemberPhotoFileNameTest.kt` and `features/admin/members/domain/usecase/DownloadMemberPhotoUseCaseTest.kt`
- [X] T043 [US6] Extend `features/admin/members/presentation/viewmodel/MemberProfileViewModelTest.kt`: download success emits the saved message; failure emits `toUserMessage()` and no `LeaveArea`; second call while running is ignored; no photo / no `manage` → nothing

**Checkpoint**: all six stories complete.

## Phase 9: Polish & Cross-Cutting

- [X] T044 [P] Update `specs/core/spec.md`: `@AuthPrefs` row (encrypted, file name, key alias), `DataStoreModule` description, new `core/data/security` section (`AeadCipher`, `EncryptedSerializer`, migration, `SessionRecovery`), `CoreViewModel` wipe on `pendingWipe`
- [X] T045 [P] Update `specs/admin/spec.md` members section: photo loader now with encrypted disk cache (`no_backup/member_photos`, 50 MB LRU, revalidation, prune/wipe rules), viewer layout ("Apagar"/"Baixar" at the bottom), download rules
- [X] T046 [P] Update `CLAUDE.md` §Security: replace "Auth tokens: pending migration to `EncryptedSharedPreferences` (currently plain DataStore)" with "Auth tokens: `@AuthPrefs` DataStore encrypted with an Android Keystore AES-GCM key (`core/data/security`)"; add `@SessionCipher`/`@MemberPhotoCipher` to the qualifiers block
- [X] T047 Check spec 005 still matches the code (FR-023a, FR-026a, FR-033, FR-035, SC-004) in `specs/005-admin-members-management/spec.md`; adjust wording if implementation details changed
- [X] T048 Grep the new code for `Timber`/`Log` calls and confirm no token, URL path, member name or photo bytes are logged (005 FR-036, spec FR-015)
- [X] T049 Run `./gradlew :app:testDebugUnitTest` and fix failures
- [ ] T050 Device validation per `specs/012-encrypted-session-photo-cache/quickstart.md` §Device checks 1–9 on API 28 and API 34+; also build `assembleRelease` once to confirm R8 keeps `KeystoreAeadCipher` and the Coil fetcher (add keep rules in `app/proguard-rules.pro` only if the release build fails at runtime)

## Dependencies

```text
Phase 2 (T001–T005)
 ├─▶ US1 (T006–T010) ─▶ US2 (T011–T016)          session
 └─▶ US3 (T017–T028) ─▶ US4 (T029–T032)          photo cache
                     └─▶ US6 (T036–T043, needs MemberPhotoSource.bytesFor)
US5 (T033–T035) independent of crypto; T034 feeds T040/T041
Polish (T044–T050) after all stories
```

- US2 needs US1's store wiring (T007) for the corruption handler.
- US4 needs `MemberPhotoStore` (T017, T021) and the repository injection (T024).
- US6 needs `MemberPhotoSource` (T019) and US5's viewer/flags (T033–T034).

## Parallel Examples

- **Phase 2**: T001, T003 together; then T002 and T004; T005 after T003/T004.
- **US1**: T008 and T009 in parallel with T007.
- **US3**: T017, T025 first; T026 and T027 in parallel once T018/T019 exist; T022 and T024 in parallel.
- **US5** can run at any time alongside US1–US4 (UI-only files).
- **US6**: T036, T037, T039 in parallel; T042 after T037.
- **Polish**: T044, T045, T046 in parallel.

## Implementation Strategy

1. **MVP = Phase 2 + US1 + US2**: tokens encrypted with a safe migration and a crash-free fallback. Shippable alone —
   closes the CLAUDE.md security pending item.
2. **Increment 2 = US3 + US4**: encrypted photo cache with its full lifecycle (never ship US3 without US4).
3. **Increment 3 = US5 + US6**: viewer layout and download.
4. Polish and device validation before the user's release.

Each increment keeps spec and code in the same commit (CLAUDE.md §Specs Driven Development).
