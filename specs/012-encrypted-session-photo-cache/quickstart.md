# Quickstart: Validating Feature 012

**Feature**: [spec.md](spec.md) | **Storage contract**: [contracts/on-device-storage.md](contracts/on-device-storage.md) |
**Viewer contract**: [contracts/photo-viewer-ui.md](contracts/photo-viewer-ui.md)

## Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.data.security.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.members.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.auth.*"
```

Never `clean`, `--rerun-tasks` or `--no-daemon` (CLAUDE.md).

| Test class | Proves |
|---|---|
| `EncryptedSerializerTest` | round trip; tampered byte, wrong version, lost key → `CorruptionException` |
| `PlaintextAuthPrefsMigrationTest` | no old file; old with tokens → migrated; old + encrypted tokens → encrypted kept; corrupt old → signed out; cleanUp deletes |
| `SessionRecoveryTest` | corruption sets `pendingWipe`, destroys key; acknowledge resets |
| `EncryptedMemberPhotoCacheTest` | put/get; LRU at 50 MB; `retainOnly`; `wipe` deletes dir + key; undecryptable → miss + deleted |
| `MemberPhotoSourceTest` | miss/hit; revalidation 304/200/404/401/403/429/network; revisions bump |
| `MembersAdminRepositoryImplTest` (extended) | prune on upload/remove/delete/list; wipe on `AppError.Auth` |
| `MemberPhotoFileNameTest` | sanitising, date, fallback |
| `MemberProfileViewModelTest` (extended) | `canDownloadPhoto` by level; download success/failure events; no double download |
| `CoreViewModelTest` (extended) | `pendingWipe` clears session caches on cold start |

## Device checks

Use an emulator with API 28 and one with API 34+ (Play image, so the Keystore is real).

1. **Migration (US1)** — install the current Play/`dev` build, sign in, then install this build over it
   (`./gradlew :app:installDebug`). Open: still signed in.
   `adb shell run-as com.ipb.castelobranco ls files/datastore` → only `auth_prefs.enc.preferences_pb`;
   `adb shell run-as com.ipb.castelobranco cat files/datastore/auth_prefs.enc.preferences_pb | strings` shows no
   token fragments (`eyJ`).
2. **Unreadable session (US2)** — signed in, corrupt the file
   (`adb shell run-as com.ipb.castelobranco sh -c 'echo x > files/datastore/auth_prefs.enc.preferences_pb'`), force
   stop, open: app opens signed out, no error, no crash; `no_backup/member_photos` is gone. Sign in again; restart;
   still signed in.
3. **Instant photos (US3)** — as Admin open Membros, scroll the list, open two profiles; force stop; reopen Membros:
   photos appear with the cards. `ls no_backup/member_photos` shows hashed names only; `strings` on a file shows no
   `JFIF`/`PNG` markers.
4. **Removed elsewhere (US3 #4)** — remove a photo from another device/Django admin; reopen the list: initials, and the
   entry is gone from the directory.
5. **Wipe (US4)** — fill the cache, sign out → directory gone. Fill again, change the user's role to one without
   `members` in Django, reopen the list → leaves the area, directory gone.
6. **Viewer (US5)** — Admin: "Trocar foto" top-left, X top-right, "Apagar" + "Baixar" bottom centre. Liderança: only
   "Baixar". No photo: no bottom buttons. Check with gesture and 3-button navigation.
7. **Download API 34 (US6)** — "Baixar": message shown; Google Photos/Files shows `Pictures/IPB Castelo Branco/<name>
   <date>.jpg`. Download again → second file ` (1)`. Sign out → files remain.
8. **Download API 28** — first "Baixar" asks storage permission; deny → message, nothing in the folder; deny with "don't
   ask again" → settings message; allow → saved and visible in the gallery app.
9. **Failure** — airplane mode on a photo not cached → error message, no file. Cached photo offline → saved.
