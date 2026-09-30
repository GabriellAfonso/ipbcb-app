# Quickstart: Validating Gallery Management

How to prove the feature works. Rules and texts live in [spec.md](spec.md) and
[contracts/gallery-management-client.md](contracts/gallery-management-client.md); types in
[data-model.md](data-model.md).

## Prerequisites

- Backend on branch `dev` with features 013, 014 and 016 (`C:\Users\gabri\Projetos\Ipb_castelo_branco\backend`).
- Three test users, all flagged as members: **M** (no level on `gallery`), **G** (`manage`), **O** (`owner`).
- A second device (or emulator) signed in as M, to see changes arrive by the feed.
- On the test phone: a HEIC photo, a PNG with transparency, an animated GIF under 10 MB, a portrait JPEG with EXIF
  orientation 6 and a capture date, and a JPEG above 40 MP.

## Automated tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.network.*"
./gradlew :app:testDebugUnitTest
```

Never run `clean`, `--rerun-tasks` or `--no-daemon` before tests (CLAUDE.md).

Expected coverage (SC-010), each a happy path plus at least one failure:

| Area | Test class |
|------|-----------|
| Local apply of each change, cursor kept, idempotent; album delete removes its subtree | `GalleryIndexApplyTest` |
| Move targets hide self and descendants; current parent/album not selectable; subtree counts | `GalleryTreeManageTest` |
| Name validation (trim, 1–100) | `GalleryNamesTest` |
| PATCH bodies: only changed keys, explicit nulls | `GalleryWriteBodiesTest` |
| `applyLocal` waits for a running sync; `syncAfterWrite` re-runs once | `GallerySyncerTest` (extended) |
| Each repository write applies its result and requests a sync; errors mapped (offline, 403, 404, duplicate, cycle, mismatch) | `GalleryManageRepositoryImplTest` |
| Order: only changed groups sent; mismatch → sync + `OrderChanged` | `ReorderUseCaseTest` |
| Batch move/delete: all attempted, 404 on delete counts as done, 403 stops, result grouped | `MovePhotosUseCaseTest`, `DeletePhotosUseCaseTest` |
| Upload outcome classification (201 new, 201 dedup, 207, 400 rejected, 400 malformed, 409, 404, 403, 401, 5xx, IOException) | `UploadOutcomeClassifierTest` |
| Queue run: id kept across retries, sent item applied and file adopted, 404 fails the album's items, 403 fails the rest, retry leaves item `Prepared`, session ended writes nothing | `GalleryUploadRunTest` |
| Preparation plan: long side ≤ 4000, rotation/flip per EXIF orientation, GIF as is within limits, quality ladder until ≤ 10 MB | `UploadPreparationPlannerTest` |
| Preparer with a fake codec: output JPEG, EXIF date copied, orientation written NORMAL | `GalleryImagePreparerTest` |
| Queue store: persists, dismiss deletes file, clear empties | `GalleryUploadQueueStoreTest` |
| Logout cancels upload work and clears the queue | `GalleryAutoDownloadUseCaseTest` (extended) |
| Controls per level (none / manage / owner), remove cover only for own cover, 403 message shown and controls follow access | `GalleryViewModelManageTest` |
| `extras` parsed from structured errors | `ApiErrorParserTest`, `ResponseExtTest` (extended) |

## Build check for the new libraries

```bash
./gradlew :app:assembleDebug
./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -E "reorderable|exifinterface|compose.foundation:foundation"
```

Expected: build succeeds; `androidx.compose.foundation:foundation` resolves to the BOM version (1.10.x), not 1.7.0.

## Manual scenarios

1. **Levels** — Sign in as M: root, album and viewer look exactly as before (no FAB, no overflow items, long press does
   nothing). As G: every `manage` control of contract §3, no "Apagar"/"Remover capa". As O: all controls. Admin panel
   as G: "Galeria" card enabled, opens the gallery root; back returns to the panel.
2. **Create/edit** — As G on the root, "Novo álbum" "Teste 008" with today's date: appears last at once. Inside it,
   create "Sub". Try a second "Sub": the form stays open with the server message. Edit "Sub" → "Sub 2", clear the
   date. Second device after a sync: all visible.
3. **Upload** — In "Sub 2", "Adicionar fotos", pick the five test images: the album shows "Enviando 0 de 5", the
   notification "Enviando x de 5". Turn the screen off until done. Check on the second device: every photo upright
   (portrait one included), the HEIC and the 40 MP JPEG present with long side 4000 px, the PNG with white where it was
   transparent, the GIF animated, each capture date equal to the source's. On this device, the gallery's download
   banner never counts them (`filesDir/gallery/photos/` already holds them).
4. **Retry with the same id** — Queue 10 photos, enable airplane mode after 3 are sent, disable after a minute: the
   queue resumes; the server has exactly 10 photos (no duplicates).
5. **Upload failures** — Queue photos into an album and have O delete that album from another device before they are
   sent: the items fail with "O álbum foi apagado". Rename a `.txt` to `.jpg` and pick it through the Files app
   fallback: "Não foi possível ler esta imagem.". Dismiss each: they leave the list.
6. **Trashed original** — Upload a photo, delete it (O), then force a retry of the same item (debug: re-enqueue the
   stored item): failed list shows "Esta foto já foi enviada e depois apagada; ela está na lixeira.".
7. **Organize** — In an album with 3 sub-albums and 6 photos, "Organizar", move the last sub-album first and the fifth
   photo first, "Salvar": new order at once and on the second device. Repeat, but before saving add a photo to that
   album from the second session: "A ordem mudou enquanto você editava…", the list refreshed, mode still open.
8. **Move** — "Mover álbum" on "Sub 2": the picker omits "Sub 2" and its sub-albums, shows "Teste 008" disabled, offers
   "Raiz". Move to root. Select 3 photos (long press), "Mover" to another album: "3 fotos movidas para '…'"; no
   original downloaded again.
9. **Covers** — In the viewer, "Usar como capa": the album tile and its parent's tile change. "Trocar capa" from the
   phone. As O, "Remover capa" (only offered now): the tile shows a sub-album's cover or black.
10. **Delete** — As O, "Apagar álbum" on "Teste 008": the confirmation shows the counts of contract §4; after it, the
    screen goes up with "Álbum enviado para a lixeira", and the files of its photos are gone from the device. Select
    2 photos, "Apagar": "2 fotos apagadas".
11. **403** — As G, remove G's level on the server, then try "Editar álbum": the server message appears and every
    management control disappears without leaving the screen.
12. **Offline** — Airplane mode, "Novo álbum" → "Sem conexão", nothing created locally.
13. **Logout** — Queue 20 photos, sign out mid-upload: no notification remains, `filesDir/gallery/` is gone,
    `adb shell dumpsys jobscheduler | grep gallery_upload` shows nothing; signing in again as G sends nothing.
