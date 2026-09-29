# Contract: What the App Consumes and Exposes

Backend contracts are the source of truth (`backend/specs/013…`, `014…`, `015…`). This file lists only what the app
uses and how it reacts.

## Endpoints used (`@AuthedRetrofit`, base `https://gabrielafonso.com.br/ipbcb/`)

| Call | Use | Replaces |
|------|-----|----------|
| `GET api/gallery/changes/` | full read: first sync, after migration, after `full_sync_required` | `GET api/photos/` |
| `GET api/gallery/changes/?since={cursor}` | every later sync | — |
| `GET {image_url}` (streaming) | original download (unchanged) | — |
| `GET {cover_url}` (streaming) | cover download during sync | — |
| `GET {thumbnail_url}` | preview, via `@GalleryThumbnailLoader` (`@Client`) | — |

Removed from `GalleryApi`: `getAllPhotos()` (`api/photos/`), `getAlbumPhotos()` (`api/albums/{id}/photos/`).
`api/albums/` is not used (the feed carries albums).

## Feed handling

| Answer | App action |
|--------|-----------|
| `200`, `full_sync_required: false` | apply delta, save index + cursor, reconcile disk, enqueue missing originals (WiFi, `KEEP`) |
| `200`, `full_sync_required: true` | read again without `since`; replace index; save; reconcile |
| `401` (after refresh failed) | `Failed(Auth 401)`; local copy kept; existing session path |
| `403` | `Failed(Auth 403)`; local copy kept; notice above grid or 403 placeholder |
| other / `IOException` | `Failed(...)`; local copy kept; next trigger retries |

## Media responses

| Resource | `404` | `401`/`403`/`429` |
|----------|-------|-------------------|
| original | not counted, retried next round (unchanged) | stops the download round (unchanged) |
| cover | skipped, tile black, retried next sync | skipped, retried next sync |
| preview | grey placeholder | grey placeholder |

## Navigation (inside `galleryGraph`)

| Route | Args | Screen |
|-------|------|--------|
| `GalleryMain` | — | root albums |
| `Album/{albumId}` | `Long` | album (sub-albums + photos) |
| `Photo/{albumId}/{photoId}` | `Long`, `Long` | viewer, paging the photos of `albumId` |

`GalleryNav`: `back()`, `toAlbum(albumId)`, `toPhoto(albumId, photoId)`.

## Triggers

| Trigger | Where | Action |
|---------|-------|--------|
| Activity `ON_START` (app start, foreground) with session | `AppNavHost` → `CoreViewModel.onAppForeground()` | `SyncGalleryUseCase()` |
| Login success | `CoreViewModel` (auth event) | `SyncGalleryUseCase()` + schedule periodic |
| Gallery graph opened | `GalleryViewModel` init | `SyncGalleryUseCase()` |
| Every 6 h, `CONNECTED`, with session | `GallerySyncWorker` | `SyncGalleryUseCase()` |
| "Tentar novamente" (no index) | root screen | `SyncGalleryUseCase()` |
| Download buttons | root screen | unchanged |
| Logout | `CoreViewModel.logout()` | `GalleryAutoDownloadUseCase.clearOnLogout()` — cancels both works, `syncer.clear()`, preview cache cleared |
