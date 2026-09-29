# Quickstart: Validating Gallery Album Tree and Sync

## Prerequisites

- Backend on `dev` with 013, 014 and 015 applied, reachable at the app's base URL.
- A member account and an account with `owner` on `gallery` (for creating test data through the API or Django admin).
- For the migration check: a device with the **previous** app version and the gallery fully downloaded.

## Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
```

Expected green, covering (see [data-model.md](data-model.md) and [research.md](research.md)):

- `GalleryIndexTest` — delta upsert, delete, duplicates, delete wins, `replaceWith`.
- `GalleryTreeTest` — roots/children/photos, order by `position` then `id`, unreachable items hidden.
- `GallerySyncerTest` — first full read, delta with cursor, `full_sync_required`, 403 / 401 / network keep local data,
  skip when busy, nothing written after `clear()`.
- `GalleryMediaReconcileTest` — originals not in index deleted, moved photo keeps its file, cover replaced / removed /
  shared.
- `GalleryLegacyMigrationTest` — files moved, `.json` and folders deleted, rerun idempotent, marker set, no download.
- `GalleryDownloadWorker` path — reads the index, syncs first when there is none.
- `GalleryAutoDownloadUseCase.clearOnLogout` — both works cancelled, index, files and preview cache cleared.
- `GalleryViewModel` — root / album / viewer states, removed photo and removed album messages.

## Manual scenarios

| # | Steps | Expected |
|---|-------|----------|
| 1 | Server: roots A (pos 1), B (pos 0); A has sub-album A1 and 3 photos. Open gallery | B, A with covers; A shows A1 then photos in order; back from A1 → A → root |
| 2 | Album with no cover anywhere below | black tile with name |
| 3 | App synced; on server add photo, rename album, move photo, reorder albums, delete photo; bring app to foreground | all changes visible; moved photo not re-downloaded (no `image_url` request in logs); deleted photo file gone from `filesDir/gallery/photos/` |
| 4 | Delete album with sub-albums on server; foreground | subtree, photos, files and cover gone |
| 5 | Restore it from trash; foreground | reappears; missing originals queued on WiFi |
| 6 | On mobile data, add photo on server; open its album | preview shown; "aguardando WiFi" banner; on WiFi, tile switches to original |
| 7 | Open a photo in the viewer; delete it on server; foreground | viewer on next photo, "Esta foto foi removida" |
| 8 | Open a sub-album; delete it on server; foreground | back to parent, "Este álbum foi removido" |
| 9 | Replace an album's cover on server; foreground | new cover shown; old file gone from `covers/` |
| 10 | Revoke membership (403); foreground; open gallery | local albums still shown, "Disponível apenas para membros." above grid |
| 11 | Signed in; delete `files/snapshots/gallery_index.json` via `adb`; airplane mode; open gallery | "Não foi possível carregar a galeria. Verifique sua conexão." + "Tentar novamente" |
| 12 | Logout | `filesDir/gallery/`, `snapshots/gallery_index.json`, `cacheDir/gallery_thumbs/` gone; `adb shell dumpsys jobscheduler` shows no gallery work |
| 13 | Migration: previous version with full gallery, delete 1 photo on server, install new version, open app online | 0 original downloads; old folders and `.json` gone; deleted photo's file gone |
| 14 | Leave app closed 6 h with session (or `adb shell cmd jobscheduler run` on the periodic job) | a deleted-on-server photo leaves the device |

## Inspecting the device

```bash
adb shell run-as com.ipb.castelobranco ls -R files/gallery
adb shell run-as com.ipb.castelobranco cat files/snapshots/gallery_index.json
adb shell run-as com.ipb.castelobranco ls cache/gallery_thumbs
```
