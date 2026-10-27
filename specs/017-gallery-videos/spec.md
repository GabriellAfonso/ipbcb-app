# Feature Specification: Gallery Videos

**Feature Branch**: `017-gallery-videos`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Add videos to the gallery: show, download and play videos offline, upload them, and the
settings that control how much space they use." (full request — current state, what to build, decisions, out of
scope — in `../TODO/specify_gallery_videos_app.md`; decision conversation: session
`75f3ece2-ae36-412f-847c-4fd2bf68030b`, 2026-10-26)

## Context

The gallery today holds only photos. The backend (its feature 023) now stores videos as gallery items of type
"video" inside the same albums as photos, each with a poster, a duration and a file size. The server never
transcodes: the media team posts small, playable MP4/MOV files of at most 100 MB.

The user's guiding wishes, which explain most decisions below:

- **Videos are opt-in.** They are big, so by default nobody downloads them. Only people who want them get them.
- **A video the device does not have does not exist for the user.** No streaming, no "tap to download" placeholders
  in the normal gallery.
- **Simple and without extra problems**: no compression on the phone, no MKV, no streaming.

### Source of truth on the backend (not redefined here)

- `backend/specs/023-gallery-videos/spec.md` and `contracts/video-upload-api.md` — item resource fields, video
  upload, media types, Range.
- `backend/specs/gallery/spec.md` — everything about photos that videos reuse (feed, order, trash, tags, covers).

### Backend facts this feature relies on

- **Item resource**: every gallery item gains a media type (`photo` or `video`), a duration in milliseconds (videos
  only) and the file size in bytes (may be missing for an old photo). For a video, the item's file is the video and
  its preview is the poster.
- **Same feed, same albums, same order, same trash, same tags** as photos. An album cover taken from a video is the
  poster.
- **Video upload** (`manage`): one video per request, with its poster, its duration, an idempotency id and an
  optional date. Refused with a reason in Portuguese when the video is over 100 MB, not MP4/MOV, or the poster is
  invalid; same idempotency rules as photos (repeat of a live item answers it again; repeat of a trashed item is a
  conflict).
- **Media files**: video files are served with the video MIME type and support resuming from a byte offset.

## Resolved Decisions

Taken in the decision conversation (session above), each with the user's reason.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | Where videos live | Mixed with photos in the same albums, same viewer | One gallery; a play icon tells them apart |
| 2 | Download | Opt-in switch "Baixar vídeos automaticamente", off by default | Videos are big; only who wants them gets them |
| 3 | An undownloaded video | Not shown anywhere (grid, viewer, counts, filters) | "Pq diabos um vídeo que não foi baixado vai aparecer junto com as fotos? Não era nem pra aparecer." |
| 4 | Albums with only undownloaded videos | Hidden; empty albums stay visible | Consequence of 3; empty albums are being filled by the media team |
| 5 | Management modes | "Organizar" and the move destination picker show undownloaded videos and hidden albums, dimmed | The person must locate things correctly and the saved order must include every item |
| 6 | Playback | Only after the full download; no streaming | Simplicity; offline viewing |
| 7 | Network for video downloads | WiFi only, always; the "Usar dados móveis" option stays photos-only | "Ok vídeo só com WiFi." |
| 8 | Resuming | An interrupted video resumes from where it stopped | Files up to 100 MB on flaky WiFi |
| 9 | Low space | Stop downloading videos when finishing the next one would leave under 1 GB free; discreet notice | Do not fill the phone; photos are not affected |
| 10 | Discovery | A card on the gallery root "Há N vídeos na galeria (X MB)" with "Não, obrigado" / "Baixar"; either hides it forever | Visible without nagging; the user explicitly chose no snooze |
| 11 | Player autoplay and sound | Autoplays muted; once unmuted, following videos start with sound until the gallery is left | Quiet by default; no repeated unmuting while browsing |
| 12 | Loop | Off by default; the player's built-in repeat toggle is available | Only because it is built-in |
| 13 | Who uploads | The same people who upload photos (`manage`) | Media team |
| 14 | Upload picker | One "Adicionar fotos e vídeos" picker; photos keep today's preparation, videos are sent as they are | No load on the uploader's phone; quality must not drop |
| 15 | Upload checks | On the phone when picking: ≤ 100 MB, MP4/MOV container, H.264/HEVC video, AAC or no audio; no duration limit; each refusal listed with its reason | The person converts the file elsewhere and comes back |
| 16 | Poster and duration | Made on the uploader's phone; poster failure refuses the video | A video without a poster must never exist |
| 17 | Upload on mobile data | Allowed, but confirmed first with the size | The uploader decides whether to spend data |
| 18 | Uploader's own video | Their file becomes the local copy; shown at once even with the switch off; nothing downloaded back | The uploader just picked it |
| 19 | Settings | A "Galeria ›" row with the space per type, opening a gallery settings screen with the switch, "Baixar galeria" and "Apagar galeria" | Keeps the main screen clean; room for future gallery settings |
| 20 | "Apagar galeria" | Dialog with checkboxes Fotos / Vídeos, both pre-checked, each with its size | Delete only what is needed |
| 21 | Deleting videos | Turns the switch off; turning it on is how videos come back | One concept |
| 22 | Deleting only photos | Mirror of deleting only videos: delete photo files, pause only photo downloads, keep the local copy syncing and the downloaded videos visible | Confirmed by the user on 2026-10-26 |
| 23 | "Baixar galeria" | Dialog with checkboxes Fotos / Vídeos; checking Vídeos turns the switch on; Vídeos never pre-checked | One concept, not a one-shot download separate from the switch; pre-checking would download videos by accident, against decision 2 |
| 24 | The in-gallery "Baixar galeria" button | Stays, opens the same dialog | Who deleted and opened the gallery needs a way out right there |
| 25 | Tags, trash, move, rename, cover | Apply to videos as to photos; "Usar como capa" sends the poster | Same item for the server |
| 26 | Poster of a hidden video in management modes | Loaded as a preview through the existing authenticated preview loader and its disk cache | Same as previews of photos without original |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Watch the gallery's videos offline (Priority: P1)

A member turns on "Baixar vídeos automaticamente". On WiFi, the gallery's videos download one by one; each appears in
its album as soon as its file is complete, with a play icon and its duration. Opening it plays it in the viewer,
muted, without any connection.

**Why this priority**: It is the feature: members see the church's videos.

**Independent Test**: With an index holding two videos, turn the switch on over WiFi; check each appears after its
download, plays offline, and that nothing downloads with the switch off or on mobile data.

**Acceptance Scenarios**:

1. **Given** the switch is off, **Then** no video file is downloaded and no video appears anywhere in the gallery.
2. **Given** the switch is on and the phone is on WiFi, **Then** every video in the local copy is downloaded, new
   videos too as they arrive.
3. **Given** the switch is on and the phone is on mobile data, **Then** no video downloads, even when "Usar dados
   móveis" was chosen for photos.
4. **Given** a video download is interrupted, **When** it runs again, **Then** it continues from where it stopped;
   the partial file never counts as downloaded.
5. **When** a video's file is complete, **Then** it appears in its album without reopening the screen.
6. **Given** a video in a grid, **Then** its thumbnail shows a play icon and the duration (`1:24`, `1:02:03`).
7. **When** a video is opened, **Then** the viewer plays the local file as soon as the page settles, muted, with
   play/pause and a seek bar; swiping away pauses it.
8. **Given** the person turned the sound on, **Then** the following videos start with sound until the gallery is
   left; the next time the gallery is opened, videos start muted.
9. **Given** a video ended, **Then** it does not restart unless the person turned on the player's repeat toggle.
10. **When** the phone is rotated to landscape on a video, **Then** the video fills the screen.
11. **Given** a file the device cannot play, **Then** the viewer shows "Não foi possível reproduzir este vídeo neste
    aparelho" instead of crashing.

---

### User Story 2 - Hidden until downloaded (Priority: P1)

A member without the switch sees a gallery of photos only. Albums that hold only videos they do not have are not
shown; counts count only what they can open.

**Why this priority**: The core rule; without it the gallery fills with items that do nothing.

**Independent Test**: With an index mixing photos, undownloaded videos and an album with only videos, check the root,
the album, counts, the people filter and the viewer.

**Acceptance Scenarios**:

1. **Given** a video whose file is not complete on the device, **Then** it does not appear in any grid, viewer page,
   filter result, people count or album count.
2. **Given** an album whose whole subtree has items and none is visible, **Then** the album is hidden.
3. **Given** an album whose whole subtree has no items at all, **Then** it stays visible.
4. **Given** an album with only undownloaded videos and a sub-album with photos, **Then** it is visible and shows only
   the sub-album.
5. **Given** a video cover, **Then** the album shows the poster as its cover, independent of the video file.
6. **Given** "Organizar" mode or the move destination picker, **Then** undownloaded videos and hidden albums are
   shown, dimmed, with an icon meaning "not on this device", and the saved order includes them.

---

### User Story 3 - Discover that videos exist (Priority: P2)

A member opens the gallery root and sees a card: "Há 12 vídeos na galeria (480 MB). Eles não são baixados
automaticamente." with "Não, obrigado" and "Baixar".

**Why this priority**: Without it the switch stays hidden in settings and nobody finds the videos.

**Independent Test**: With undownloaded videos and the switch off, open the root; tap each button on fresh installs;
check the card never comes back.

**Acceptance Scenarios**:

1. **Given** the switch is off, the local copy has at least one undownloaded video and the card was never dismissed,
   **Then** the card shows on the gallery root, above the albums, with the count and the total size.
2. **When** "Não, obrigado" is tapped, **Then** the card hides forever and "Você pode baixar os vídeos depois em
   Configurações." shows once.
3. **When** "Baixar" is tapped, **Then** the card hides forever and the switch turns on; when not on WiFi, "Os vídeos
   vão baixar quando você estiver no WiFi" shows.
4. **Given** the card was dismissed, **Then** it never shows again on this device — not when new videos arrive, not
   after turning the switch off, not after deleting videos, not after signing out and in.

---

### User Story 4 - Upload videos (Priority: P2)

A media team member taps "Adicionar fotos e vídeos" in an album and picks photos and videos together. Photos go as
today; each video is checked on the phone and sent as it is, with a poster and duration made on the phone.

**Why this priority**: Without it videos only arrive through the Django admin.

**Independent Test**: Pick a valid MP4, a 150 MB file, an MKV and a video with AC3 audio; check the three refusals
with their reasons and that the MP4 shows at once in the album.

**Acceptance Scenarios**:

1. **Given** a user with `manage`, **Then** the album's "+" offers "Adicionar fotos e vídeos", one picker for both.
2. **Given** a picked video over 100 MB, **Then** it is refused with "Vídeo muito grande (máx. 100 MB)…".
3. **Given** a picked file whose container is not MP4/MOV, **Then** it is refused, saying to convert it to MP4.
4. **Given** a video whose video is not H.264/HEVC or whose audio is not AAC (or absent), **Then** it is refused,
   saying the audio/video format is not compatible and to convert it to MP4.
5. **Given** the poster cannot be made, **Then** the video is refused with a reason.
6. **Given** valid videos, **Then** each goes through the existing upload queue, one per request, with the same
   idempotency id on every attempt, the notification and the "Não enviadas" list with the server's reason.
7. **Given** the batch has videos and the phone is on mobile data, **Then** the app first asks "Você está nos dados
   móveis. Enviar 2 vídeos (140 MB) agora?" with "Esperar WiFi" and "Enviar"; photo-only batches behave as today.
8. **When** a video is accepted, **Then** the uploader's file becomes the local copy and the video shows in the album
   at once, even with the switch off; nothing is downloaded back.

---

### User Story 5 - Gallery settings (Priority: P2)

A member opens Configurações and sees "Galeria ›" with "Fotos 320 MB · Vídeos 1,2 GB". Inside: the video switch with
what is available or the progress, "Baixar galeria" and "Apagar galeria", each with checkboxes Fotos / Vídeos.

**Why this priority**: Controls the space videos use and is the only place to turn videos off.

**Independent Test**: Delete only videos, check photos stay and the switch turns off; delete only photos, check
videos stay; "Baixar galeria" with Vídeos checked turns the switch on.

**Acceptance Scenarios**:

1. **Given** Configurações, **Then** the "Dados" section holds only "Apagar Bíblia", and a row "Galeria" with "Fotos
   {x} · Vídeos {y}" opens the gallery settings screen.
2. **Given** the switch is off, **Then** its description reads "Somente no WiFi · {n} vídeos ({size})"; while
   downloading, "{k} de {n} baixados".
3. **When** "Apagar galeria" is tapped, **Then** a dialog asks for confirmation with "Fotos {size}" and "Vídeos
   {size}" both checked; "Apagar" is disabled when none is checked.
4. **Given** both checked, **Then** today's behaviour: every sync pauses, the local copy and every file are deleted,
   and the switch turns off.
5. **Given** only Vídeos, **Then** the video files are deleted, the switch turns off (the dialog says so), photos and
   the local copy keep syncing.
6. **Given** only Fotos, **Then** the photo files are deleted and only photo downloads pause; the local copy keeps
   syncing and downloaded videos stay visible.
7. **When** "Baixar galeria" is tapped, **Then** a dialog shows Fotos and Vídeos; missing photos come checked, videos
   never come checked (they stay opt-in); a type already complete is disabled ("Já baixadas"), and Vídeos is disabled
   while the switch is on.
8. **Given** Vídeos is checked in "Baixar galeria", **Then** the dialog says "Vídeos novos também serão baixados" and
   confirming turns the switch on.
9. **Given** the gallery was deleted, **When** "Baixar galeria" is tapped inside the gallery, **Then** the same dialog
   opens.

---

### User Story 6 - Videos behave as photos (Priority: P3)

Members and managers do with a video everything they do with a photo: share, save to the device, see details and
tags, move, rename, trash, restore, use as cover, filter by people and by type.

**Why this priority**: Expected parity; each action already exists.

**Independent Test**: On a downloaded video, run each action and check the result.

**Acceptance Scenarios**:

1. **When** "Compartilhar" or "Baixar" is used on a video, **Then** it works as for photos with the video MIME type.
2. **Given** a video, **Then** "ⓘ", "Marcar pessoas", "Editar", "Mover", "Apagar" and the selection bar work as for
   photos.
3. **When** "Usar como capa" is used on a video, **Then** the poster is sent as the cover.
4. **Given** at least one video is visible, **Then** a "Tudo / Fotos / Vídeos" filter shows, combined (AND) with the
   people filter, as one grid in tree order; with no visible video it does not show.

---

### Edge Cases

- A video arrives in the feed while the switch is off: it stays hidden; the card's count and size grow.
- A video is moved to the trash on the server: the next sync removes it from the local copy and deletes its file, as
  for photos.
- A video without a known size (should not happen for videos): counted in the number but not in the size.
- Free space drops below the threshold while downloading: the current video finishes only if it fits above the
  threshold; otherwise video downloads stop, the partial file is kept, and "Pouco espaço no aparelho. Os vídeos
  pararam de baixar." shows in the gallery. Photos keep downloading. The next run checks again.
- The switch is turned off during a video download: the download stops; complete videos stay on the device (and
  visible), the partial stays to resume if the switch is turned on again. Removing videos from the device is
  "Apagar galeria" with Vídeos.
- A video is replaced on the server (same item, new file is impossible: files never change); a partial file whose
  item left the local copy is deleted by the disk reconciliation.
- Sign-out: the local copy and every file are deleted as today; the switch and the dismissed card survive (device
  settings).
- The uploader picks the same video twice: two queue items, two uploads (same as photos).
- The uploader's own video and "Apagar galeria" with Vídeos: their copy is deleted with the others and hidden until
  the switch is on.
- A video's poster fails to load in "Organizar" offline: grey cell with the icon, same as photo previews.
- The viewer is on a video when it is removed (trash, move): same handling as a removed photo.
- The person leaves the viewer during playback: the player stops and is released.

## Requirements *(mandatory)*

### Functional Requirements

**Model**

- **FR-001**: Each gallery item MUST carry its media type (photo or video), its duration (videos) and its file size,
  read from the server's item resource; an item stored before this feature MUST read as a photo with no size.
- **FR-002**: Video files MUST be stored apart from photo files so the space per type can be measured and videos
  deleted alone.

**Visibility**

- **FR-003**: A video MUST be visible only when its file is complete on the device.
- **FR-004**: An album MUST be visible when it has a visible item, a visible child album, or no item at all in its
  subtree; otherwise hidden.
- **FR-005**: Album counts, people counts, filter results and viewer pages MUST include only visible items.
- **FR-006**: "Organizar" mode and the move destination picker MUST show every item and album, marking undownloaded
  videos and hidden albums as not on the device; saved orders MUST include every id.

**Grid and filter**

- **FR-007**: Video cells MUST show a play icon and the duration (`m:ss`, `h:mm:ss` from one hour).
- **FR-008**: A "Tudo / Fotos / Vídeos" filter MUST show only when at least one video is visible, combined (AND) with
  the people filter, as one grid in tree order.

**Download**

- **FR-009**: The switch "Baixar vídeos automaticamente" MUST be off by default and stored on the device, surviving
  sign-out.
- **FR-010**: With the switch on, every video in the local copy MUST be downloaded, on WiFi only, never following the
  photos' mobile-data option.
- **FR-011**: Video downloads MUST resume from the partial file; a partial file MUST NOT count as downloaded.
- **FR-012**: Before each video, the app MUST check that finishing it leaves at least 1 GB free; otherwise video
  downloads stop and the gallery shows "Pouco espaço no aparelho. Os vídeos pararam de baixar.".
- **FR-013**: A completed video MUST appear in the gallery without reopening the screen.

**Discovery card**

- **FR-014**: The gallery root MUST show the card only when the switch is off, at least one video is undownloaded and
  the card was never dismissed; either button MUST dismiss it forever on this device (surviving sign-out).
- **FR-015**: "Não, obrigado" MUST show "Você pode baixar os vídeos depois em Configurações." once; "Baixar" MUST turn
  the switch on and, off WiFi, show "Os vídeos vão baixar quando você estiver no WiFi".

**Player**

- **FR-016**: Videos MUST play inside the existing viewer pager, from the local file only, autoplaying muted when the
  page settles and pausing when swiped away.
- **FR-017**: The sound choice MUST last while the gallery stays open and reset to muted on the next visit.
- **FR-018**: The player MUST offer play/pause, seek, and the built-in repeat toggle (off by default); landscape MUST
  show the video full screen.
- **FR-019**: A playback failure MUST show "Não foi possível reproduzir este vídeo neste aparelho".
- **FR-020**: Share and save-to-device MUST work for videos with the video MIME type.

**Upload**

- **FR-021**: The album's upload entry MUST read "Adicionar fotos e vídeos" and pick images and videos in one picker.
- **FR-022**: Each picked video MUST be checked on the phone: ≤ 100 MB, MP4/MOV container, H.264 or HEVC video, AAC or
  no audio; each refusal MUST be listed with its reason; there is no duration limit.
- **FR-023**: The poster (a frame near the start) and the duration MUST be made on the phone; failure MUST refuse the
  video.
- **FR-024**: Videos MUST be sent unchanged through the existing queue, one per request, with the existing
  idempotency, retries, notification and rejected list.
- **FR-025**: When a batch with videos is picked on a metered network, the app MUST ask first with the count and size;
  "Esperar WiFi" defers the videos until WiFi; photo-only batches MUST behave as today.
- **FR-026**: An accepted video MUST become the local copy from the uploaded file and be visible at once regardless of
  the switch.

**Settings**

- **FR-027**: Configurações MUST show a "Galeria" row with the space used by photos and by videos, opening the gallery
  settings screen; "Apagar galeria" MUST move there.
- **FR-028**: The gallery settings screen MUST hold the switch (with its description) and "Baixar galeria" / "Apagar
  galeria" dialogs with Fotos / Vídeos checkboxes, as described in User Story 5.
- **FR-029**: Deleting videos MUST turn the switch off; deleting only photos MUST keep the local copy and videos;
  deleting both MUST behave as today and turn the switch off.
- **FR-030**: Checking Vídeos in "Baixar galeria" MUST turn the switch on; the mobile-data option MUST apply to photos
  only.

### Key Entities

- **Gallery item**: a photo or a video — id, album, name, media type, duration (videos), file size, file and preview
  addresses, tags.
- **Video file on device**: complete (visible) or partial (resumable, never visible).
- **Video download preference**: the switch; device setting.
- **Discovery card dismissal**: a device flag; once set, never cleared.
- **Photo download pause**: the "only photos deleted" state; photos stop downloading while the local copy keeps
  syncing.
- **Media type filter**: Tudo / Fotos / Vídeos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the switch off, a member's device downloads zero bytes of video.
- **SC-002**: A member who wants the videos gets them in at most 1 tap from the gallery root (card "Baixar").
- **SC-003**: Every video shown in the gallery plays without a connection.
- **SC-004**: A video interrupted at any point resumes without downloading again the bytes already on the device.
- **SC-005**: A refused upload always tells the uploader the reason, before anything is sent.
- **SC-006**: Video downloads never leave the phone with less than 1 GB free.
- **SC-007**: Every behaviour in the acceptance scenarios is covered by an automated unit test where it lives outside
  the UI.

## Assumptions

- The backend contract `backend/specs/023-gallery-videos/contracts/video-upload-api.md` is final; the app is built
  against it while the backend finishes.
- The existing gallery sync, trash, tags, order and cover flows work for videos without server changes beyond that
  contract.
- The media team prepares small playable files; the app never compresses or transcodes.
- Out of scope: streaming or playing before the download completes; a per-video download button; compression,
  trimming or editing on the phone; MKV and other containers; push notifications about new videos; a video-specific
  storage quota.
