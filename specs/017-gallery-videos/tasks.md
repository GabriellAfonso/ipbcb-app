# Tasks: Gallery Videos

**Input**: `specs/017-gallery-videos/` (spec, plan, research, data-model, contracts/ui.md, quickstart)

Paths: `G/` = `app/src/main/java/com/ipb/castelobranco/features/gallery/`, `S/` =
`app/src/main/java/com/ipb/castelobranco/features/settings/`, `C/` = `app/src/main/java/com/ipb/castelobranco/core/`,
`T/` = `app/src/test/java/com/ipb/castelobranco/`. Tests are required by CLAUDE.md (happy path + 1 error per use case).

## Phase 1: Setup

- [X] T001 Add `media3` 1.11.1 (`media3-exoplayer`, `media3-ui`) and `coil-video` 2.6.0 to `gradle/libs.versions.toml`
  and `app/build.gradle.kts`.
- [X] T002 Update `specs/gallery/spec.md` (items with media type, videos dir, visibility, video download, player,
  upload, filter, card) and `specs/settings/spec.md` (settings graph, Galeria screen, dialogs).

## Phase 2: Foundational

- [X] T003 [P] `MediaType`, `mediaType`/`durationMs`/`fileSize`, `isVideo`, video `fileExtension()` in
  `G/domain/model/GalleryPhoto.kt`; DTO fields with defaults in `G/data/dto/GalleryPhotoDto.kt`; mappers in
  `G/data/dto/GalleryMappers.kt`; tests in `T/features/gallery/domain/model/GalleryPhotoTest.kt` (old JSON decodes
  as photo).
- [X] T004 `GalleryMediaStore`: `videos/` dir, `videoFiles()`, `partialVideo()`, `completeVideo()`, `adoptVideo()`,
  `photoBytes()`/`videoBytes()`, `clearPhotos()`/`clearVideos()` in `G/data/local/GalleryMediaStore.kt`; tests in
  `T/features/gallery/data/local/GalleryMediaStoreTest.kt`.
- [X] T005 `GalleryLocalState.videos`; `GallerySyncer` lists videos, prunes videos and partials, `clearPhotos()`,
  `clearVideos()`, `usage()`; `Synced` carries missing photos and missing videos; tests in `GallerySyncerTest.kt`.
- [X] T006 [P] `GalleryVisibility.visibleIndex` in `G/domain/model/GalleryVisibility.kt` + tests.
- [X] T007 [P] `GalleryPreferences`: `autoDownloadVideos` (flow + set), `videoCardDismissed`,
  `photoDownloadsPaused`.

## Phase 3: US2 — Hidden until downloaded (P1)

- [X] T008 [US2] `GalleryViewModel`: visible `tree` + `fullTree`; Organizar and move pickers on `fullTree`;
  delete-album counts on `fullTree`.
- [X] T009 [US2] `TreeTarget.onDevice`, tile `onDevice` flags; `GalleryUiMapper` builds tiles for video
  (`VideoFrame` / preview, duration) and marks missing items in organize mode.
- [X] T010 [P] [US2] `PeopleViewModel` and viewer work on the visible tree.
- [X] T011 [US2] UI: dimmed tiles + "not on device" icon in `GalleryManageComponents.kt` / grids.
- [X] T012 [US2] Tests: visibility in root/album/viewer/organize in `GalleryViewModelVideoTest.kt`.

## Phase 4: US1 — Watch videos offline (P1)

- [X] T013 [US1] `GalleryApi.downloadFile(url, range)`; `GalleryVideoDownloader` (Range resume, 206/200/416, free
  space floor) in `G/data/download/GalleryVideoDownloader.kt` + tests.
- [X] T014 [US1] `GalleryVideoDownloadJob`, `GalleryVideoDownloadWorker`, `WorkManagerGalleryVideoDownloadScheduler`,
  `GalleryVideoDownloadScheduler` + DI; photo downloader/job skip videos and honor `photoDownloadsPaused`; tests.
- [X] T015 [US1] `SyncGalleryUseCase` queues videos when the switch is on; `GalleryAutoDownloadUseCase` cancels
  video work on logout; `GalleryVideoSettingsUseCase` (switch on/off, card dismiss); tests.
- [X] T016 [US1] `VideoDuration.format` + tests; tiles show ▶ + duration (`AlbumScreen`, `PeopleScreen`).
- [X] T017 [US1] Player: `GalleryVideoPlayer.kt` (ExoPlayer, `PlayerView`, mute button, repeat toggle, error
  text), `PhotoScreen` integration (autoplay muted on settle, pause off page, landscape full screen), mute state in
  `GalleryViewModel`.
- [X] T018 [US1] Low-space banner from the video work's `WorkInfo` in `GalleryScreen`.

## Phase 5: US6 — Parity (P3, small, done with US1)

- [X] T019 [US6] Share/save with video MIME (`PhotoScreen`); `originalForCover` sends the poster for videos;
  tests in `GalleryManageRepositoryImplTest.kt`.
- [X] T020 [US6] Media filter `Tudo/Fotos/Vídeos` in `GalleryPeople`, `PeopleViewModel`, `PeopleScreen`,
  `ViewerSource.People(memberIds, filter)`, route `type` arg; tests in `GalleryPeopleTest.kt`/`PeopleViewModelTest`.

## Phase 6: US3 — Discovery card (P2)

- [X] T021 [US3] Card state in `GalleryRootUiState` (count, size, visible), actions in `GalleryViewModel`
  (`dismissVideoCard`, `acceptVideoCard`), composable in `GalleryScreen`; tests.

## Phase 7: US4 — Upload videos (P2)

- [X] T022 [P] [US4] `VideoUploadCheck` (pure) in `G/domain/upload/VideoUploadCheck.kt` + tests.
- [X] T023 [US4] `VideoProbe` + `AndroidVideoProbe` (MediaExtractor, signature, poster, duration, date).
- [X] T024 [US4] Queue: `UploadItem` kind/poster/duration/date/wifiOnly + DTO; `PickedMediaCopier` (images and
  videos, probe at copy); `GalleryUploadRepository.inspect()` / `enqueue(..., videosWifiOnly)`; tests.
- [X] T025 [US4] `GalleryApi.uploadVideo`; `GalleryUploadRun` sends videos, skips wifi-only on metered, mutex,
  adopts video; classifier video text; tests in `GalleryUploadRunTest.kt`.
- [X] T026 [US4] `GalleryUploadScheduler.enqueueWifiOnly()` (`gallery_upload_wifi`, UNMETERED); worker progress
  from both works; test.
- [X] T027 [US4] UI: "Adicionar fotos e vídeos" (ImageAndVideo), metered confirmation dialog, VM `addMedia`; tests.

## Phase 8: US5 — Settings (P2)

- [X] T028 [US5] Core port `C/domain/gallery/GalleryStoragePort.kt`; `G/data/GalleryStoragePortImpl.kt`
  (usage, video summary, switch, missing, delete by type, download by type) + binding; tests.
- [X] T029 [US5] `DeleteGalleryUseCase(photos, videos)` mirror rules; `downloadAfterDelete(photos, videos, mobile)`;
  tests.
- [X] T030 [US5] Shared `C/presentation/components/MediaTypesDialog.kt` (checkbox dialog with `@Preview`).
- [X] T031 [US5] `S/presentation/navigation/SettingsNavGraph.kt` (main, gallery, logs) wired in `AppNavHost`;
  `AppRoutes.SETTINGS_GRAPH`.
- [X] T032 [US5] `SettingsViewModel`/`SettingsScreen`: "Galeria" row, Dados = only Bíblia, gallery imports removed;
  `GallerySettingsViewModel` + `GallerySettingsScreen`; tests.
- [X] T033 [US5] In-gallery "Baixar galeria" opens the same dialog (`GalleryViewModel`, `GalleryScreen`).

## Phase 9: Polish

- [X] T034 Run gallery + settings unit tests, then the full suite and `assembleDebug`.
- [X] T035 Review specs vs. code; mark tasks done.

## Dependencies

Setup → Foundational → US2 → US1 (+US6) → US3 → US4 → US5 → Polish. US4 and US5 only need Foundational and can
run in either order; US3 needs the switch from US1.

## Parallel examples

- T003, T006, T007 in parallel (different files).
- T022 alongside T013.

## Implementation strategy

MVP = Foundational + US2 + US1 (videos sent from the Django admin are downloaded and watched). Then the card, upload
and settings.
