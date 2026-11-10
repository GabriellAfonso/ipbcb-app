# Feature Specification: Trim a Video Before Uploading It to the Gallery

**Feature Branch**: `022-gallery-video-trim`

**Created**: 2026-11-10

**Status**: Draft

**Input**: User description: "Let the person who uploads to the gallery cut a picked video to the part they want before
it enters the upload queue, like WhatsApp does." (full request — current state, what to build, decisions, out of scope
— in `../TODO/specify_gallery_video_trim_app.md`; decision conversation: session
`de1b19f9-6d25-4b59-a72a-ce818789f84c`, 2026-11-10)

## Context

Today a video picked for the gallery always goes whole, and a video over 100 MB is refused outright. Media team members
often have a long recording of which they want only one part. This feature adds an editor between the picker and the
upload queue where each picked video can be cut to the wanted part.

It is an app-only feature. The server keeps receiving a small MP4/MOV through the same upload, with the duration and
the poster of what is actually sent, and never converts anything.

## Resolved Decisions

Taken in the decision conversation (session above), each with the reason.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | Batch handling | One video at a time, in picked order, with "Usar inteiro" and "Enviar todos sem cortar" | A WhatsApp-style carousel of the batch adds about one more day of work |
| 2 | Holding the batch | Nothing is enqueued until the last video is decided | The album keeps the picked order, and one back press can discard the batch without leaving part of it in the queue |
| 3 | Cut quality | Exact and nearly lossless where the phone allows it: only the part up to the first keyframe is re-encoded | Keeps quality without compressing, and the start of the cut is where the person chose |
| 4 | When the near-lossless path does not apply | Accept a normal re-encode of the selected part | The person keeps a working trim, and refusing would block MOV sources |
| 5 | Limits | Stay as they are: 100 MB, MP4/MOV, H.264/HEVC, AAC or silent, no duration limit | Trimming changes length, not the accepted formats |
| 6 | Videos over 100 MB | An MP4/MOV over 100 MB opens the editor instead of being refused | Trimming is the way forward inside the app |
| 7 | Formats that cannot be sent | Skip the editor and go to the existing refused list | Trimming cannot fix a format the phones cannot play, so the person should not spend effort on it |
| 8 | Whole videos | Never processed, sent byte for byte | The gallery's rule of not compressing holds; only a video the person chose to cut is processed |
| 9 | Where the cut runs | In the foreground on the editor, before the queue | The person is looking at it, and the output is ready to enqueue; the upload queue does not change |
| 10 | Editor details | Minimum 1 second, sound on, no autoplay, back confirms and discards the whole batch | Confirmed by the user on 2026-11-10 |
| 11 | Mobile-data question | Moves to after the editor and counts the final sizes | The size shown today would overstate what is really sent |
| 12 | Recording date | Read from the original before cutting and kept | The date of a video must not change because it was trimmed |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cut a video and send only that part (Priority: P1)

A media team member picks a long video in an album. An editor opens showing the video with a timeline and two handles.
They drag the handles to the part they want, watch it, tap "Cortar e continuar", see the progress, and the trimmed
video enters the upload queue with its own duration and poster and the original recording date.

**Why this priority**: It is the feature. Without it nothing else here has a purpose.

**Independent Test**: Pick one MP4, move both handles, confirm; check that the item sent has the selected duration, a
poster from the trimmed part, the original recording date, and that the original file on the device is untouched.

**Acceptance Scenarios**:

1. **Given** a batch with at least one video that can be sent, **When** the picker returns, **Then** the editor opens
   on the first such video.
2. **Given** the editor is open, **Then** it shows the video paused with sound on, a play/pause control, a timeline of
   frames with a start handle and an end handle, and the start time, end time, selected duration and estimated size.
3. **When** a handle is dragged, **Then** the preview seeks to that position and the labels update.
4. **Given** playback, **Then** it stays inside the selected range.
5. **Given** the handles, **Then** they cannot be brought closer than 1 second.
6. **Given** both handles are at the ends, **Then** the primary button reads "Usar inteiro"; **given** a handle moved,
   **then** it reads "Cortar e continuar".
7. **When** "Cortar e continuar" is tapped, **Then** the cut runs on the screen with "Cortando… N%" and "Cancelar".
8. **When** "Cancelar" is tapped, **Then** the cut stops, the partial output is deleted and the handles come back as
   they were.
9. **Given** the cut fails, **Then** the partial output is deleted, "Não foi possível cortar este vídeo." is shown, and
   the person stays in the editor and may try another range or, when allowed, use the whole video.
10. **Given** a finished cut, **Then** the item in the queue has an `.mp4` name, the duration and poster of the trimmed
    part, and the recording date of the original.
11. **Given** the source was a MOV, **Then** the output is still an MP4 and its name ends in `.mp4`.

---

### User Story 2 - Trim a video that is too big to send (Priority: P1)

A member picks a 400 MB recording. Instead of a refusal, the editor opens and says the video is too big, and they
select a part that fits.

**Why this priority**: It turns today's dead end into a way forward, which is the main reason to trim at all.

**Independent Test**: Pick an MP4 over 100 MB; check that "Usar inteiro" is disabled, that "Cortar e continuar" enables
only once the estimate fits, and that an output over 100 MB is refused and deleted.

**Acceptance Scenarios**:

1. **Given** an MP4/MOV over 100 MB, **Then** the editor opens, "Usar inteiro" is disabled, and the screen says "Vídeo
   muito grande (máx. 100 MB). Selecione um trecho menor.".
2. **Given** the selection, **Then** the estimated size is the file size times the selected duration divided by the
   total duration, and "Cortar e continuar" is disabled while it is over 100 MB.
3. **Given** a finished cut whose real size is over 100 MB, **Then** the output is deleted and the person stays in the
   editor with "O trecho ficou com X MB. Selecione um trecho menor.".

---

### User Story 3 - Decide a mixed batch (Priority: P2)

A member picks three videos and some photos. The editor shows "Vídeo 1 de 3", they cut the first and accept the others
whole with one tap. The whole batch then enters the queue in the order they picked it.

**Why this priority**: Batches are how the media team works, but a single-video flow already delivers value.

**Independent Test**: Pick photos and three videos in mixed order, cut one, accept the rest with "Enviar todos sem
cortar"; check the queue order matches the picked order and nothing was enqueued before the last decision.

**Acceptance Scenarios**:

1. **Given** a batch with no video, **Then** everything behaves as today and no editor opens.
2. **Given** more than one video in the editor, **Then** the screen shows the position ("Vídeo 2 de 3").
3. **Given** photos in the batch, **Then** they are not edited and wait with the batch.
4. **Given** this video and at least one more are undecided, **Then** "Enviar todos sem cortar" is shown; **given** it
   is the last undecided video, **then** it is not shown.
5. **When** "Enviar todos sem cortar" is tapped, **Then** all undecided videos are accepted whole, and any over 100 MB
   lands in the existing refused list with its reason.
6. **When** the last video is decided, **Then** the whole batch is enqueued in picked order.
7. **When** back is pressed, **Then** "Descartar a seleção?" asks for confirmation; confirming discards the whole
   batch, deletes the cut files already made for it, and returns to the album with nothing enqueued.
8. **Given** a picked file with another container or a refused codec, **Then** it skips the editor and goes to the
   existing refused list with the existing reason.
9. **Given** every video in the batch skips the editor, **Then** no editor opens and the batch goes on as today.

---

### User Story 4 - Mobile-data question reflects what is really sent (Priority: P2)

A member off WiFi cuts a 400 MB video down to 40 MB. The question asks about 40 MB, not 400 MB.

**Why this priority**: It keeps an existing protection honest, but only matters off WiFi.

**Independent Test**: Off WiFi, cut a video and accept another whole; check the count and size in the question use the
real output size and the whole file's size.

**Acceptance Scenarios**:

1. **Given** a batch with videos off WiFi, **When** the last video is decided, **Then** the question is asked with the
   count and the final sizes: the cut output's real size, or the whole file's size.
2. **Given** a whole video over 100 MB, **Then** it is not counted, because it will be refused.
3. **Given** a photo-only batch, **Then** the question is never asked.
4. **Given** "Esperar WiFi" or "Enviar", **Then** the choice behaves as today.

---

### Edge Cases

- The batch is gone when the editor route is shown (process death): the screen returns to the album.
- A video's duration cannot be read from its URI: it skips the editor and takes the existing path, where the existing
  check gives the reason.
- A video shorter than 1 second, or so short the handles cannot move: only the whole-video action is useful; the
  handles stay at the ends and "Cortar e continuar" never appears.
- The timeline frames fail to load: the timeline shows empty cells and the handles still work.
- The picker URI becomes unreadable while the editor is open: the cut fails with the failure message, and a whole-video
  decision reaches the existing refused list.
- A cut file made for a batch when the process is killed mid-cut or before the batch is enqueued: an orphan in the
  uploads folder that nothing in the queue points to; it is removed the next time the queue is cleaned at startup.
- Rotation while the editor is open: the batch, the handles and a running cut are kept.
- The person leaves the app during a cut: the cut goes on while the screen's state lives; if the process dies the batch
  is gone, as in the first edge case.
- An output that fails the existing checks after a cut (duration, poster, format): it follows the existing refused path
  with the existing reason.
- The same video picked twice: two separate decisions and two queue items, as today.

## Requirements *(mandatory)*

### Functional Requirements

**Flow**

- **FR-001**: A batch with no video MUST behave exactly as before this feature.
- **FR-002**: A batch with at least one video eligible for the editor MUST open a full-screen editor, one video at a
  time, in picked order, showing the position when there is more than one.
- **FR-003**: Photos MUST NOT be edited and MUST wait with the batch.
- **FR-004**: Nothing from the batch MUST enter the queue until the last video is decided; then the whole batch MUST
  be enqueued in picked order.
- **FR-005**: The editor MUST only be reachable with a batch held in memory, and MUST return to the album when the
  batch is gone.
- **FR-006**: Back MUST ask "Descartar a seleção?"; confirming MUST discard the batch, delete its cut files and return
  to the album with nothing enqueued.

**Eligibility**

- **FR-007**: A video MUST open the editor when its type is MP4 or QuickTime and its tracks pass the codec check, read
  without copying the whole file.
- **FR-008**: Any other picked video MUST skip the editor and take the existing path to the refused list with the
  existing reason.

**Editor**

- **FR-009**: The editor MUST show the video paused, with sound on, a play/pause control, and playback confined to the
  selected range.
- **FR-010**: The editor MUST show a timeline of frames with start and end handles; dragging a handle MUST seek the
  preview to that position.
- **FR-011**: The editor MUST show the start time, end time, selected duration and estimated size, the estimate being
  the file size times the selected duration divided by the total duration.
- **FR-011a**: Tapping the start or end time MUST open a field to type that time (`1:23`, `83` or `83,5` seconds);
  a value outside the video, or less than 1 second from the other handle, MUST be refused with the reason and leave
  the handles as they were. An accepted value MUST move that handle and seek the preview to it.
- **FR-011b**: Each handle MUST be a thin line across the frames with a round knob in its middle, joined by a thin
  frame along the selection. Dragging near a handle moves it; dragging inside the selection slides the whole
  selection, keeping its length, and stops at the video's ends (the preview follows its start); dragging outside the
  selection moves the nearer handle.
- **FR-012**: The selection MUST be at least 1 second.
- **FR-013**: The primary button MUST read "Usar inteiro" while both handles are at the ends and "Cortar e continuar"
  once a handle moved.
- **FR-014**: "Enviar todos sem cortar" MUST appear only when this video and at least one more are undecided, and MUST
  accept all undecided videos whole.

**Over the size limit**

- **FR-015**: An MP4/MOV over 100 MB MUST open the editor with "Usar inteiro" disabled and the message "Vídeo muito
  grande (máx. 100 MB). Selecione um trecho menor.".
- **FR-016**: "Cortar e continuar" MUST be disabled while the estimate is over 100 MB.
- **FR-017**: After a cut, the real size MUST be checked; over 100 MB the output MUST be deleted and the person MUST
  stay in the editor with "O trecho ficou com X MB. Selecione um trecho menor.".

**The cut**

- **FR-018**: The cut MUST be exact at both ends, re-encoding only what is needed where the phone allows it, and MUST
  fall back to a normal re-encode of the selected part otherwise.
- **FR-019**: The output MUST keep the source's video and audio formats so it passes the existing upload checks.
- **FR-020**: The cut MUST read from the picked item directly, without first copying the whole source.
- **FR-021**: The output MUST be an MP4 with an `.mp4` display name, also for a MOV source.
- **FR-022**: A video accepted whole MUST NOT be processed and MUST be sent byte for byte.
- **FR-023**: The cut MUST run in the foreground on the editor with "Cortando… N%" and "Cancelar"; cancelling MUST stop
  it, delete the partial output and return to the handles.
- **FR-024**: A failed cut MUST delete the partial output and show "Não foi possível cortar este vídeo.", leaving the
  person in the editor.

**After the cut**

- **FR-025**: The duration, poster and final checks of a cut video MUST describe the output.
- **FR-026**: The recording date of a cut video MUST be read from the original before cutting and carried to the item.
- **FR-027**: The queue MUST accept both a picked item and an already-cut file with its display name and date; a cut
  file MUST NOT be copied a second time.
- **FR-028**: Cut files left behind by a killed process MUST be removed.

**Mobile-data question**

- **FR-029**: The question MUST run after the last video is decided and count the final sizes, excluding videos over
  100 MB sent whole; photo-only batches MUST never be asked.

**Backend**

- **FR-030**: The upload contract MUST NOT change; the server MUST keep receiving one video with its poster, duration
  and date.

### Key Entities

- **Picked batch**: the ordered list of picked items held in memory while the editor is open, with the decision taken
  for each video.
- **Video decision**: undecided, whole, or cut (with its output file, display name and recording date).
- **Trim selection**: the start and end position chosen in the editor, at least 1 second apart.
- **Cut output**: a file in the uploads folder that exists only for this batch until the batch is enqueued or discarded.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A person can cut a video and put it in the queue in under 1 minute, not counting the time of the cut.
- **SC-002**: Every video over 100 MB in MP4/MOV with an accepted format can reach the queue as long as the person
  selects a part that fits.
- **SC-003**: Discarding a batch leaves zero items in the queue and zero cut files on the device.
- **SC-004**: A trimmed video has the same recording date as its original in 100% of uploads.
- **SC-005**: A video accepted whole arrives on the server byte-identical to the picked file.
- **SC-006**: The size in the mobile-data question matches what is really sent.
- **SC-007**: Every rule outside the UI is covered by an automated unit test (happy path and at least one error).

## Assumptions

- The person uploading already has the `manage` permission; this feature does not change who can upload.
- The near-lossless cut may not apply on some phones or formats; the fallback is accepted (decision 4).
- Trimming does not reduce bitrate, so a long 4K selection can still exceed 100 MB; the size check handles that and no
  compression option is added.
- A held batch living only in memory is acceptable: after process death the person picks again.
- Device matrix validation (H.264 MP4, HEVC MP4, MOV, portrait) is done manually on real phones.
- Out of scope: compression or quality options; crop, rotate, mute, filters or any other edit; joining clips or
  several ranges from one video; trimming a video already in the gallery; choosing the poster frame; a carousel of the
  whole batch; backend changes.
