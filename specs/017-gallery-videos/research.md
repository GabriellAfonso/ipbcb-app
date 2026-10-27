# Research: Gallery Videos

Product decisions are in [spec.md](spec.md) (Resolved Decisions). This file records the technical ones.

## R-01 Player library

- **Decision**: Media3 ExoPlayer + `media3-ui` `PlayerView` inside `AndroidView`, version 1.11.1.
- **Rationale**: the user asked for Media3 and its built-in repeat toggle (`setRepeatToggleModes`). 1.11.1 needs
  compileSdk 36 (project: 36), minSdk 23 (project: 24) and Kotlin stdlib 2.2.10 (project: 2.3.10). `PlayerView`
  brings play/pause, seek bar and the repeat toggle without custom UI.
- **Alternatives**: `media3-ui-compose` (only surface + a few buttons, no full controller yet); `VideoView`
  (no repeat toggle, weaker codec support).

## R-02 One player per viewer

- **Decision**: one `ExoPlayer` remembered by `PhotoContent`, released on dispose; the page that is settled and is
  a video attaches it to its `PlayerView` and sets the media item (`file://`). Swiping away pauses (the player is
  detached from the old page). Mute is applied as `volume = 0f/1f`; a mute icon over the player toggles it and
  reports to `GalleryViewModel.setVideoMuted`, whose `StateFlow` (default `true`) lives as long as the graph.
- **Rationale**: one decoder at a time; the graph scope gives exactly "while they stay in the gallery".
- **Alternatives**: a player per page (several decoders alive inside the pager); mute in `rememberSaveable`
  (lost between viewer openings).

## R-03 Landscape

- **Decision**: in landscape, when the current page is a video, `PhotoContent` shows only that video's player full
  screen on black (no top bar, no buttons, no pager). Portrait brings the pager back on the same page.
- **Rationale**: simplest full-screen behaviour, matches "rotating the phone to landscape shows the video full
  screen".

## R-04 Playback failure

- **Decision**: `Player.Listener.onPlayerError` sets a page flag; the page shows "Não foi possível reproduzir este
  vídeo neste aparelho" over the poster.

## R-05 Visibility

- **Decision**: `GalleryVisibility.visibleIndex(index, downloadedVideoIds)` drops videos not in the set, then drops
  every album whose subtree has items but no visible item (computed bottom-up). An album with no item at all in its
  subtree stays. The result is a `GalleryIndex` (same cursor), so `GalleryTree`, `GalleryPeople` and the viewer work
  unchanged on it.
- **Rationale**: one rule in one pure function; dropping a hidden album never orphans a visible one (a visible child
  makes its parent visible).
- **Alternatives**: filtering inside `GalleryTree` (spreads the rule over every query).

## R-06 Management modes see everything

- **Decision**: `GalleryViewModel` keeps two trees per local state: `tree` (visible) and `fullTree`. Organize
  sessions are built from `fullTree`; while organizing, the album/root state is built from `fullTree` with tiles
  flagged `onDevice = false` for undownloaded videos and hidden albums (dimmed + `CloudOff` icon). Move pickers use
  `fullTree.moveTargetsFor…` with `TreeTarget.onDevice`. Delete-album counts use `fullTree` (what goes to the trash).
- **Rationale**: the server checks that an order lists every id (`missing`).

## R-07 Video files and partials

- **Decision**: `gallery/videos/{id}.{ext}` complete; `gallery/videos/{id}.{ext}.part` partial (inside the gallery
  root so sizes and clears include it). `videoFiles()` lists only complete files whose name is `{id}.{mp4|mov}`;
  prune deletes completes and partials whose id left the index. Extension from `image_url` (`.mov` → `mov`, else
  `mp4`).
- **Rationale**: a partial with a different suffix can never be counted; kept between runs for resume.

## R-08 Resume

- **Decision**: `GalleryApi.downloadFile(url, range: String?)` with `@Header("Range")`. With a partial of `n` bytes:
  `Range: bytes=n-`. `206` appends; `200` rewrites from zero; `416` with `n == fileSize` completes, otherwise deletes
  the partial (retried next run). When the partial reaches `fileSize` (or the body ends and `fileSize` is unknown) it
  is renamed to the final name.
- **Rationale**: nginx answers `206` after the media check (backend contract).

## R-09 Free-space floor

- **Decision**: before each video, `needed = (fileSize ?: 100 MB) - partialBytes`; if
  `filesDir.usableSpace - needed < 1 GB` the run stops with `LowSpace`. The worker returns `failure` with
  `KEY_LOW_SPACE`; the gallery reads it from the work's `WorkInfo` and shows "Pouco espaço no aparelho. Os vídeos
  pararam de baixar.". The next sync enqueues with `KEEP`, which replaces a finished (failed) work, so the check runs
  again.
- **Alternatives**: a persisted flag (needs clearing logic).

## R-10 Video download work

- **Decision**: separate unique work `gallery_video_download`, always `UNMETERED`, backoff 60 s, `KEEP` from sync /
  switch on / "Baixar galeria" with Vídeos, cancelled when the switch turns off, on logout and when videos are
  deleted. The job runs only while the switch is on, walks videos in tree order (full index — hidden ones are what it
  downloads) and calls `refreshLocalFiles()` after each completed video so it appears at once. Same stop rules as
  photos (401/403/429). Photos' download keeps its own work and skips videos.
- **Rationale**: the photo work may run on `CONNECTED` ("Usar dados móveis"); videos never do.

## R-11 Switch off keeps files

- **Decision**: turning the switch off stops (cancels) video downloads and keeps the complete videos already on the
  device (still visible); partial files stay too and resume if it is turned on again. Deleting videos is
  "Apagar galeria › Vídeos", which also turns the switch off.
- **Rationale**: no destructive side effect on a toggle; deletion has its own confirmed dialog.

## R-12 Photos-only deletion

- **Decision**: `GalleryPreferences.photoDownloadsPaused`. "Apagar galeria" with only Fotos: cancel the photo
  download work, set the pause, delete `gallery/photos/` and covers are kept (they are album art, re-fetched by
  sync anyway), clear the preview cache. Sync keeps running; `SyncGalleryUseCase` does not queue photo downloads
  while paused; the photo job ends at once while paused. Photos without original keep showing their preview (as any
  photo waiting for WiFi). "Baixar galeria" with Fotos lifts it and enqueues. Both types checked = today's full
  pause (`gallery_sync_paused`) + switch off.

## R-13 Upload checks

- **Decision**: at pick time (`PickedMediaCopier`), a picked URI whose MIME starts with `video/` is copied to
  `gallery/uploads/{uploadId}-src.{mp4|mov|bin}` and probed (`AndroidVideoProbe`): container from the
  `MediaExtractor` track formats and the file signature (`ftyp` box: `qt  ` = MOV, other brands = MP4); video track
  MIME `video/avc` or `video/hevc`; audio absent or `audio/mp4a-latm`; size ≤ 100 MiB. Poster: `getFrameAtTime(
  500_000, OPTION_CLOSEST_SYNC)` scaled to ≤ 1280 px, JPEG 85, `gallery/uploads/{uploadId}-poster.jpg`; duration
  `METADATA_KEY_DURATION`. The pure `VideoUploadCheck.check(probe)` returns the refusal text. Refused items enter the
  queue as `Failed` with the reason (listed in "Não enviadas"), so the person sees each one.
- Refusal texts: "Vídeo muito grande (máx. 100 MB). Reduza o arquivo e tente de novo.", "Formato não suportado.
  Converta o vídeo para MP4.", "O formato de áudio ou vídeo não é compatível. Converta o vídeo para MP4.", "Não foi
  possível gerar a capa deste vídeo.", "Não foi possível ler este vídeo."
- **Alternatives**: checking in the worker (the person would learn later).

## R-14 Metered confirmation and "Esperar WiFi"

- **Decision**: before copying, `GalleryViewModel.addMedia` asks the upload repository for each URI's MIME and size
  (`ContentResolver` query). With at least one video ≤ 100 MB and not on WiFi, it opens the dialog "Você está nos
  dados móveis. Enviar {n} vídeo(s) ({size}) agora?" [Esperar WiFi] [Enviar]. "Enviar" enqueues as today; "Esperar
  WiFi" enqueues with the batch's videos `wifiOnly = true`. The queue skips `wifiOnly` items while the network is
  metered (checked per item through `NetworkStatus.isUnmetered()`), and the scheduler also enqueues a second unique
  work `gallery_upload_wifi` (`UNMETERED`, `KEEP`). Both workers run the same `GalleryUploadRun`, serialized by a
  process-wide mutex.
- **Rationale**: photos never wait behind a WiFi-only work in the same chain (`APPEND_OR_REPLACE` would block them).

## R-15 Video upload request

- **Decision**: `GalleryApi.uploadVideo(album_id, client_upload_id, duration_ms, date_taken?, video, poster)`; video
  part MIME `video/mp4` / `video/quicktime`, file name = picked name; `date_taken` from `METADATA_KEY_DATE` when it
  parses as a date. Same `UploadOutcomeClassifier` (generic text "Não foi possível enviar este vídeo." for videos).
  On `Sent`, `adoptVideo(id, ext, file)` moves the file into `gallery/videos/` under the index lock; the poster file is
  deleted.

## R-16 Offline video thumbnails

- **Decision**: a downloaded video's tile and page poster come from the local file through Coil's
  `VideoFrameDecoder` (`coil-video` 2.6.0) registered on `@GalleryThumbnailLoader`; `PhotoImage.VideoFrame(file)`.
  An undownloaded video (only in management modes) uses `thumbnail_url` like a photo preview.
- **Rationale**: works offline without storing posters; frames are memory-cached by Coil.

## R-17 "Usar como capa" on a video

- **Decision**: `originalForCover` downloads `thumbnail_url` to a temp JPEG for videos (never sends the video file).

## R-18 Share and save

- **Decision**: share with the item's MIME (`video/mp4`, `video/quicktime`); save videos to
  `Movies/ipb_castelobranco` through `MediaStore.Video`.

## R-19 Media-type filter

- **Decision**: `GalleryPeople.matching(tree, memberIds, type)`; the people screen gets chips Tudo / Fotos / Vídeos
  (shown only when the visible tree has a video). With people chosen, AND; with no people and a type other than Tudo,
  every visible item of that type in tree order. `ViewerSource.People(memberIds, type)`; route
  `PeoplePhoto/{memberIds}/{photoId}?type={type}` (empty member list allowed).

## R-20 Settings ↔ gallery

- **Decision**: core port `GalleryStoragePort` (`core/domain/gallery/`): `usage(): GalleryUsage(photoBytes,
  videoBytes)`, `videoSummary: Flow<VideoSummary(total, downloaded, totalBytes)>`, `autoDownloadVideos: Flow<Boolean>`,
  `setAutoDownloadVideos(Boolean)`, `missing(): MissingMedia(photos, videos)`, `delete(photos, videos)`,
  `download(photos, videos, useMobileData)`. Implemented by `GalleryStoragePortImpl` in the gallery feature, bound in
  `GalleryModule`. `SettingsViewModel` drops its gallery imports; the gallery sub-screen has its own
  `GallerySettingsViewModel`. The in-gallery "Baixar galeria" opens the same dialog, rendered by a shared composable
  in `core/presentation/components/MediaTypesDialog.kt` (with `@Preview`), each feature wiring its own VM.

## R-21 Snapshot compatibility

- **Decision**: DTO defaults `media_type = "photo"`, `duration_ms = null`, `file_size = null`; unknown media types
  map to photo. Queue DTO defaults `kind = "photo"`, `wifi_only = false`, `poster_file = null`, `duration_ms = null`.
