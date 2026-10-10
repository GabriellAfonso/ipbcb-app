# Research: Encrypted Session, Encrypted Member Photo Cache and Member Photo Download

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-09

Facts found in the code before deciding:

- `@AuthPrefs` (`core/di/DataStoreModule.kt`) is a Preferences DataStore on `datastore/auth_prefs.preferences_pb`. Its
  only reader/writer is `features/auth/data/local/TokenStorage` (one key, `auth_tokens`, JSON of `AuthTokens`).
- `TokenAuthenticator` clears the tokens when refresh answers 401/400; `CoreViewModel` reacts to
  `isLoggedInFlow` going `true → false` by clearing every `SessionScopedCache`. On a cold start that is already logged
  out (`wasLoggedIn = false`), nothing is cleared.
- Member photos load through `@MemberPhotoLoader` (Coil 2.6, `diskCache(null)`, authenticated `@Client`).
  `MemberAvatar` passes the URL (or a `ByteBuffer` for a picked photo) as the Coil model.
- Backend stores member photos as `members/<uuid4>.<ext>` (`member_photo_storage.py`): **replacing a photo always
  produces a new URL**; removing sets it to null. Media responses carry `ETag` + `Cache-Control: private, no-cache`
  and answer `304` to a matching `If-None-Match` (`specs/004-protected-media-downloads/contracts/media-download.md`).
- `MembersAdminRepositoryImpl` keeps the roll in memory, revalidates with ETag, and patches the list on writes.
  `SessionScopedCache` for members clears the repository and Coil's memory cache.
- The gallery already saves to shared pictures (`PhotoScreen.saveImageToGallery`) with `RELATIVE_PATH`, which only
  exists on API 29+; there is no pre-Q path and no `WRITE_EXTERNAL_STORAGE` in the manifest.
- `backup_rules.xml` / `data_extraction_rules.xml` exclude `datastore/auth_prefs.preferences_pb` only.
- DataStore 1.2.0 exposes `PreferenceDataStoreFactory.create(storage: Storage<Preferences>, …)` and the public
  `PreferencesSerializer : OkioSerializer<Preferences>` (checked in the sources jar).

---

## R1 — Cipher: Android Keystore AES-256-GCM, no new library

**Decision**: one small `AeadCipher` port (`encrypt(plain, associatedData): ByteArray`,
`decrypt(sealed, associatedData): ByteArray`, `destroyKey()`) with a `KeystoreAeadCipher(alias)` implementation:
AES-256 key generated in `AndroidKeyStore` (`PURPOSE_ENCRYPT | PURPOSE_DECRYPT`, `BLOCK_MODE_GCM`,
`ENCRYPTION_PADDING_NONE`, `setRandomizedEncryptionRequired(true)`, no user authentication), created lazily on first
encrypt. Sealed format: `[version=1][12-byte IV][ciphertext‖16-byte tag]`. Associated data = a fixed purpose label
(`"auth_prefs"`, `"member_photo:<cache key>"`) so a blob cannot be swapped between files.

Two aliases: `ipbcb_session_v1` and `ipbcb_member_photos_v1` (FR-008: separate keys).

Any `GeneralSecurityException`, `KeyStoreException`, `ProviderException` or a wrong version byte surfaces as one
`UndecryptableException` (data layer only).

**Rationale**: Keystore AES-GCM works on every supported API (23+; minSdk 24), keeps the key non-exportable and
hardware-backed when the device has it (FR-002). The data sizes are small (session < 4 KB; member photos are
square-cropped and capped at 10 MB by the server, typically < 500 KB), so one-shot GCM in memory is enough — no
streaming cipher.

**Alternatives considered**:
- `androidx.security:security-crypto` (`EncryptedSharedPreferences`/`EncryptedFile`): deprecated by Google in 2025,
  and would replace DataStore rather than encrypt it. Rejected.
- Tink (`tink-android`): streaming AEAD and keyset management, but a new ~1 MB dependency for two small blobs.
  Rejected (CLAUDE.md: no unnecessary abstractions/libs).
- Requiring user authentication on the key: would tie the session to the screen lock — explicitly out of scope.

## R2 — Encrypted session store: same DataStore, encrypting serializer, new file

**Decision**: `@AuthPrefs` stays a `DataStore<Preferences>` (so `TokenStorage` and its tests do not change), built
with `PreferenceDataStoreFactory.create(storage = OkioStorage(FileSystem.SYSTEM, EncryptedSerializer(
PreferencesSerializer, sessionCipher, "auth_prefs")) { datastore/auth_prefs.enc.preferences_pb }, corruptionHandler,
migrations = [PlaintextAuthPrefsMigration])`.

`EncryptedSerializer<T>` (generic, in `core/data/security`) wraps any `OkioSerializer<T>`: `writeTo` serializes to a
buffer and writes the sealed bytes; `readFrom` decrypts then delegates; an `UndecryptableException` or a delegate
`CorruptionException` becomes `CorruptionException`.

A new file name (`auth_prefs.enc.preferences_pb`) keeps old and new formats unambiguous during migration.

**Rationale**: Encrypting at the serializer keeps "nothing in plain text on disk" (FR-001) for every key, current or
future, without touching callers. DataStore's own atomic write (temp file + rename) still applies.

**Alternatives**: encrypt only the token string inside the existing plain file — leaves future keys in plain text and
the file structure readable. Rejected.

## R3 — Silent migration from the plain-text file

**Decision**: `PlaintextAuthPrefsMigration : DataMigration<Preferences>`:

- `shouldMigrate(current)`: the old file `datastore/auth_prefs.preferences_pb` exists.
- `migrate(current)`: if `current` already holds `auth_tokens`, return `current` unchanged (the encrypted copy is
  newer — e.g. killed after the write but before cleanup, then the session was renewed). Otherwise read the old file
  with plain `PreferencesSerializer` and return its preferences; an unreadable old file returns `current` (treated as
  signed out — never a crash).
- `cleanUp()`: delete the old file.

DataStore runs migrations before the first read and only calls `cleanUp` after the migrated data is durably written,
so the encrypted copy reads back before the plain file goes (FR-003); an interrupted run repeats safely (FR-004).

**Rationale**: built-in, transactional, no custom "migration done" flag.

**Alternatives**: a one-off startup step in `CoreActivity` — races with `TokenStorage`'s eager `stateIn`. Rejected.

## R4 — Unreadable session: quiet sign-out and photo-cache wipe

**Decision**:

- `ReplaceFileCorruptionHandler { recovery.onSessionUnreadable(); emptyPreferences() }`. `SessionRecovery`
  (`core/data/security`, singleton) destroys the session key (a new one is created on the next write) and raises a
  `pendingWipe` `StateFlow<Boolean>`.
- `CoreViewModel.initialize()` collects `pendingWipe`: when true, it runs `clearSessionScopedCaches()` (which now
  includes the member photo cache, R7) and acknowledges. Empty preferences ⇒ `isLoggedInFlow` false ⇒ the app shows
  what it shows on any start without a session — no message, no crash (FR-005, US2).
- If the key itself throws on `getKey`/`init` (manufacturer fault), `EncryptedSerializer.readFrom` turns it into
  `CorruptionException` — same path. A failure on **write** after recovery is thrown to the caller as today
  (`TokenStorage.save` is already called inside `runCatching` paths); the next start retries.

**Rationale**: `SessionScopedCache` lives in features; injecting the set into the DataStore provider would create a DI
cycle (DataStore → TokenStorage → client → APIs → repositories → caches). A core flag consumed by `CoreViewModel`
breaks the cycle and also covers the cold-start case where `wasLoggedIn` is false.

**Alternatives**: emit on `AuthEventBus` — a `SharedFlow` event at DataStore init can fire before any collector.
Rejected.

## R5 — Backup and transfer exclusions

**Decision**: replace the excluded path with `datastore/auth_prefs.enc.preferences_pb`, keep the old one (it may still
exist on devices until first launch), and exclude the photo cache directory `member_photos/` (domain `file`, under
`noBackupFilesDir` it is excluded automatically — see R6). Both `backup_rules.xml` and `data_extraction_rules.xml`.

**Rationale**: FR-007/FR-014. Keystore keys never travel with a backup anyway, so a restored blob would be unreadable;
excluding avoids a guaranteed quiet sign-out after restore.

## R6 — Member photo cache: own small cache, not Coil's disk cache

**Decision**: `EncryptedMemberPhotoCache` (feature data layer) stores one file per photo in
`noBackupFilesDir/member_photos/`, named `sha256(path)` (hex) — the URL path never appears in a file name. Each file is
the sealed blob of `[etag length][etag][mime length][mime][image bytes]`, associated data = file name. LRU by file
`lastModified` (touched on every read); after each write, if the directory exceeds 50 MB, delete oldest first
(FR-010). All operations behind one `Mutex`.

Operations: `get(path)`, `put(path, etag, mime, bytes)`, `remove(path)`, `retainOnly(paths)`, `wipe()` (delete
directory + `cipher.destroyKey()`). An entry that fails to decrypt is deleted and reported as a miss (FR-013).

**Rationale**: Coil 2's `DiskCache` writes plain files and reads them back through its own `FileSystem`; an encrypting
okio `FileSystem` would have to fake sizes and random access for the journal — fragile for little gain. The cache
needs only get/put/evict on tens to hundreds of small files.

**Alternatives**: Coil disk cache + encrypting `ForwardingFileSystem` (rejected above); Room/SQLite index (overkill;
no index needed when names are hashes and LRU uses mtime).

## R7 — Feeding Coil and revalidating

**Decision**: `@MemberPhotoLoader` keeps its memory cache and gets one custom `Fetcher.Factory<Uri>`
(`MemberPhotoFetcher`) registered in `components {}` — user components take precedence over Coil's HTTP fetcher. For
member-photo URLs (the loader is only used for them) it:

1. Asks `MemberPhotoSource.load(url)`:
   - **hit** → returns the decrypted bytes (`DataSource.DISK`) at once and launches a background revalidation;
   - **miss** → `GET` with the authenticated client; `200` → `put` + return (`DataSource.NETWORK`); failure → Coil
     error ⇒ initials (005 FR-026).
2. Revalidation (`If-None-Match: <etag>`, application-scoped coroutine in the source, one in flight per path):
   `304` → touch; `200` → `put` new bytes, bump the path's revision; `404` or any refusal other than 401/403 → `remove`,
   bump revision; `401`/`403` (after the authenticator's renewal) → `wipe()`, bump all; `429`/network → keep.
3. `MemberPhotoSource.revisions: StateFlow<Map<String, Int>>`. Member ViewModels expose it in their UI state; screens
   pass `photoRevision` to `MemberAvatar`, which uses it as an `ImageRequest` parameter that is part of the memory
   cache key, so a bumped photo is reloaded from the (updated) cache without flicker of other photos.

Revalidation happens on each fetcher hit, i.e. once per photo per memory-cache miss — "at most once per display" as
FR-009 asks. Since a replaced photo always gets a new URL (backend fact), the usual "photo changed" case arrives via
the list's new URL and loads immediately; revalidation mainly catches removed files and server-side swaps.

**Rationale**: keeps `MemberAvatar` a dumb Coil composable, reuses Coil's decoding, sizing and memory cache.

**Alternatives**: ViewModels load bytes and pass `ByteArray` to the avatar — moves image I/O into presentation state
and loses Coil's memory cache sizing. Rejected.

## R8 — When the cache is pruned or wiped

**Decision**:

| Trigger | Action | Where |
|---|---|---|
| `uploadPhoto`/`removePhoto`/`deleteMember` success | `remove(oldPath)` | `MembersAdminRepositoryImpl` (knows the old URL in its records/list) |
| `refreshMembers` success (200, not 304) | `retainOnly(paths in list)` | `MembersAdminRepositoryImpl` |
| `refreshMembers`/`getMember` → `AppError.Auth` (401 after renewal, or 403) | `wipe()` | `MembersAdminRepositoryImpl` |
| photo request 401/403 | `wipe()` | `MemberPhotoSource` (R7) |
| logout, refresh failure (`isLoggedInFlow` true→false), unreadable session (R4) | `wipe()` | members `SessionScopedCache` |

The repository depends on a `MemberPhotoStore` domain port (`remove`, `retainOnly`, `wipe`) — no Android types.

**Rationale**: FR-011/FR-012; pruning against the list also drops photos removed by other leaders (US3 #4).

## R9 — Saving to the device gallery on API 24–36

**Decision**: `MemberPhotoGallerySaver` (feature data, behind a domain port `MemberPhotoExporter`):

- **API 29+**: `MediaStore.Images` insert with `RELATIVE_PATH = Pictures/IPB Castelo Branco`, `IS_PENDING = 1`, write,
  then `IS_PENDING = 0`. On any exception, `delete(uri)` (FR-024). MediaStore appends ` (1)` on a name collision, so
  nothing is overwritten (FR-022). No permission.
- **API 24–28**: `Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)/IPB Castelo Branco/`, `mkdirs`,
  pick a free name (`name.jpg`, `name (1).jpg`, …), write to `name.tmp` and rename; on failure delete the temp. Then
  `MediaScannerConnection.scanFile` so the gallery app sees it. Needs `WRITE_EXTERNAL_STORAGE` declared with
  `android:maxSdkVersion="28"`.
- Bytes come from `MemberPhotoSource.bytesFor(url)` — the cached copy if present (works offline), else the network
  (FR-020 "the photo being shown"). The extension follows the stored MIME type.
- File name: `"<member name> <yyyy-MM-dd>"`, characters `\ / : * ? " < > |` and control characters removed, trimmed,
  max 100 chars, fallback `"Membro <id>"` when empty (FR-022).

**Permission flow (presentation)**: `MemberProfileScreen` holds a `RequestPermission` launcher. On "Baixar":
SDK ≤ 28 and not granted → launch; granted → `viewModel.onDownload()`; denied → if
`shouldShowRequestPermissionRationale` is false after a denial → permanent message, else the "permita o acesso" message
(FR-025). SDK ≥ 29 → `onDownload()` directly. The ViewModel stays free of `Context`; the Screen reads the SDK and the
Activity (via the existing `findActivity()`).

**Rationale**: the only way to write shared pictures on both sides of scoped storage; no new library.

**Alternatives**: `ACTION_CREATE_DOCUMENT` (user picks the folder) — breaks "saved in Pictures/IPB Castelo Branco".
Reusing the gallery's `saveImageToGallery` — it lives in a screen file of another feature and is API 29+ only. Not
reused; the gallery is out of scope.

## R10 — Viewer layout and states

**Decision**: `MemberPhotoViewer` gains `onDownload: (() -> Unit)?` and `isDownloading`. Bottom `Row`
(`Alignment.BottomCenter`, `navigationBarsPadding`/`safeDrawingPadding`, 16 dp gap) with `FilledTonalButton`-style
translucent buttons: "Apagar" (`Icons.Filled.Delete`) when `onRemovePhoto != null && photoUrl != null`; "Baixar"
(`Icons.Filled.Download`) when `onDownload != null && photoUrl != null`. All buttons disabled while `isBusy`; progress
indicator shown for downloads as for uploads (FR-019). `MemberProfileUiState` gains `canDownloadPhoto = canManage`
and `isDownloadingPhoto`; result messages go through the existing `MembersEvent.ShowMessage`. A `@Preview` set covers
owner / manage / view / no photo (optional for feature components, cheap here).

## R11 — Testing without the Keystore

**Decision**: unit tests use `FakeAeadCipher` (XOR + tag check, can be told to fail or "lose" its key). Covered with
JVM tests: `EncryptedSerializer` round trip and corruption; `PlaintextAuthPrefsMigration` (no old file, old file with
tokens, old file + newer encrypted, corrupt old file); `SessionRecovery`; `EncryptedMemberPhotoCache` (put/get,
LRU eviction at the limit, retainOnly, wipe, undecryptable entry) on a temp dir; `MemberPhotoSource` revalidation
table with a fake HTTP layer; file-name sanitising; `MemberProfileViewModel` download flags/events;
`MembersAdminRepositoryImpl` prune/wipe calls via a `FakeMemberPhotoStore`. `KeystoreAeadCipher` and the MediaStore
writer are verified manually (see [quickstart.md](quickstart.md)) — they need a device.
