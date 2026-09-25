# Feature Specification: Protected Media Downloads

**Feature Branch**: `004-protected-media-downloads`

**Created**: 2026-09-25

**Status**: Draft

**Input**: User description: "Adapt media downloads to the backend's protected media (backend feature
009-protected-media-access). /ipbcb/media/<path> now requires the JWT. Unauthenticated -> 401, no permission -> 403,
unknown or missing file -> 404, over the media rate limit -> 429. Responses carry ETag + `Cache-Control: private,
no-cache`; a matching If-None-Match gets 304. Rules: gallery/ and profiles/ need a member (the owner can always see
their own profile photo); members/ needs a leader (is_admin)."

## Overview

The church server is closing its media files to the public. Until now anyone holding a photo's address could fetch
it; after the backend change every media file requires a signed-in session, and the server decides per file who may
see it:

| Media area  | Who may fetch it                                          |
|-------------|-----------------------------------------------------------|
| `gallery/`  | Members                                                   |
| `profiles/` | Members; the owner can always fetch their own photo       |
| `members/`  | Leaders (administrators)                                  |

The server now answers media requests with outcomes the app never had to tell apart before:

| Outcome              | Meaning for the app                                                  |
|----------------------|----------------------------------------------------------------------|
| Success              | File delivered, with a version tag                                   |
| Not modified (304)   | The copy the app already has is current                              |
| Unauthenticated (401)| Session missing or expired                                           |
| Forbidden (403)      | Signed in, but this person may not see this file                     |
| Not found (404)      | The file does not exist                                              |
| Rate limited (429)   | Too many media requests in a short time; try again later             |

The app already sends the session with every media download and already renews an expired session transparently, and
every image on screen is shown from the device's own storage, never straight from the network. So nothing breaks
outright. What breaks is **how the app reacts to the new refusals**: today the gallery download treats a refused or
failed photo as done, which leaves permanent holes in the local gallery, and the profile photo treats every refusal
other than "not found" as a generic error.

This feature makes the two media consumers — the **gallery download** and the **profile photo** — react correctly to
each outcome, and confirms no other part of the app fetches media outside the signed-in path.

### Why the release order matters

The server change ships in three steps: backend first, then this app release, then the web server switches media to
the protected flow. Until that last switch, media stays reachable the old way, so this release must work against both
the old (open) and the new (protected) media behavior.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Gallery download never loses photos silently (Priority: P1)

A member opens the gallery on a fresh install. The app downloads the church's photo collection in the background. If
the server briefly refuses some photos because of the rate limit, or a photo fails on a flaky connection, the member
ends up — after a later run — with the **complete** gallery, not with holes that never get filled.

**Why this priority**: Today a failed photo is counted as downloaded and never fetched again. With the rate limit in
place, a first full download of a large gallery is likely to hit it, so without this fix most members would end up
with an incomplete gallery forever. This is the change that protects the gallery's core value.

**Independent Test**: Simulate a full download in which the server rate-limits partway through; confirm the run
stops, the photos already saved stay saved, the remaining ones are not reported as downloaded, and the next run
downloads exactly the missing ones.

**Acceptance Scenarios**:

1. **Given** a member with an empty gallery and 100 photos available, **When** the server rate-limits the request for
   photo 41, **Then** the download stops, photos 1–40 are on the device, progress shows 40 of 100, no error message
   is shown, and a later attempt is scheduled automatically.
2. **Given** the run in scenario 1 stopped at photo 41, **When** the next run happens, **Then** it fetches only
   photos 41–100 and never re-downloads photos 1–40.
3. **Given** a full download in progress, **When** one photo fails (not found, or a network error on that single
   photo), **Then** that photo is not saved, it does not count as downloaded, the remaining photos continue to
   download, and the failed photo is tried again on the next run.
4. **Given** a full download where every photo succeeded, **When** the run ends, **Then** progress shows all photos as
   downloaded and the gallery shows every album.

---

### User Story 2 - Gallery clearly says "no access" when the person is not a member (Priority: P1)

A signed-in person who is not (or is no longer) a member of the church starts a gallery download. The server refuses
the photos. The app stops asking, and tells them plainly that the gallery is only available to members — without
offering a login button, because signing in again would not change anything.

**Why this priority**: Without it, a non-member's download would request every photo in the collection, collect a
refusal for each, and report the run as finished — wasting their data, loading the server, and leaving them with an
empty gallery and no explanation.

**Independent Test**: Simulate a download where the server forbids the first photo; confirm the download stops after
that single refusal, no further photos are requested, and the gallery shows the "members only" message without a
login button.

**Acceptance Scenarios**:

1. **Given** a signed-in non-member starts the gallery download, **When** the server forbids a photo, **Then** no
   further photos are requested in that run and the gallery shows a message that the gallery is available only to
   members.
2. **Given** the "members only" message is shown, **When** the person looks at the screen, **Then** there is no
   "connect to your account" button.
3. **Given** a member whose membership was revoked, with albums already on the device, **When** a new download is
   forbidden, **Then** the "members only" message is visible even though albums exist locally, and the already
   downloaded photos stay on the device and remain visible.
4. **Given** a forbidden download, **When** the run ends, **Then** it is not retried automatically.

---

### User Story 3 - Profile photo behaves sensibly under the new rules (Priority: P2)

A member sees their own profile photo in the top bar and on their profile screen. If the server forbids the photo,
the app shows the neutral placeholder and forgets the stored copy — quietly, with no error message. If the server is
temporarily rate-limiting, or the network is down, the app keeps showing the last photo it already had.

**Why this priority**: The owner can always see their own photo, so a refusal here is rare — but when it happens the
app must not keep showing a photo the server says this person may no longer see, nor show a scary error for a
cosmetic element. Temporary failures must not blank a photo that was fine a moment ago.

**Independent Test**: For each outcome (forbidden, rate limited, network error, not modified, not found, success),
start from a device that already holds a profile photo and check what the top bar shows afterwards and whether any
error is reported.

**Acceptance Scenarios**:

1. **Given** a stored profile photo, **When** the server forbids the photo, **Then** the stored copy and its version
   tag are removed, the placeholder is shown, and no error message is displayed.
2. **Given** a stored profile photo, **When** the server rate-limits the request, **Then** the stored photo stays on
   screen and no error message is displayed.
3. **Given** a stored profile photo, **When** the device has no network, **Then** the stored photo stays on screen
   and no error message is displayed.
4. **Given** a stored profile photo, **When** the server answers "not modified", **Then** the stored photo stays on
   screen without being downloaded again.
5. **Given** a stored profile photo, **When** the server answers "not found", **Then** the stored copy is removed and
   the placeholder is shown (unchanged current behavior).

---

### User Story 4 - Expired sessions end cleanly (Priority: P2)

A person whose session has expired and can no longer be renewed triggers a media download (gallery or profile photo).
The app signs them out through the same flow it already uses for any other expired session.

**Why this priority**: Media downloads are now one more place where an unrenewable session surfaces. The person must
see the same, familiar outcome as everywhere else rather than a media-specific error.

**Independent Test**: Simulate a media request that is refused as unauthenticated and whose session renewal also
fails; confirm the app ends up in the standard signed-out state.

**Acceptance Scenarios**:

1. **Given** an expired session that cannot be renewed, **When** a gallery photo download is refused as
   unauthenticated, **Then** the download stops, the app follows the existing sign-out flow, and the gallery shows
   the existing "sign in to access the gallery" state.
2. **Given** an expired session that cannot be renewed, **When** the profile photo download is refused as
   unauthenticated, **Then** the app follows the existing sign-out flow.
3. **Given** an expired session that **can** be renewed, **When** a media download is refused as unauthenticated,
   **Then** the session is renewed and the download continues without the person noticing.

---

### User Story 5 - No media fetched outside the signed-in path (Priority: P3)

Every image the app shows comes either from the device's storage or from a download that carries the person's
session. No screen loads a media address directly.

**Why this priority**: After the web server switch, any direct, session-less media load would show a broken image.
The audit already found none, so this story mostly guards against regressions.

**Independent Test**: Review every place the app displays an image or requests a media address and confirm each one
either reads a local file or goes through the signed-in download path.

**Acceptance Scenarios**:

1. **Given** the app's current code, **When** every image display and every media request is reviewed, **Then** none
   loads a media address without the session.
2. **Given** a future change that displays a remote media address directly, **When** it is reviewed against this
   spec, **Then** it is rejected in favor of the signed-in path.

---

### Edge Cases

- **Rate limit on the very first photo**: the run stops with 0 downloaded, no error shown, and a later attempt is
  scheduled.
- **Rate limit repeats on every run**: each run makes progress until the limit; the gallery fills in over several
  runs. The app never tight-loops against the limit — each new attempt waits before starting.
- **Photo already on the device**: counts as downloaded without any request, so it never consumes rate limit.
- **Forbidden on a photo mid-run, after others succeeded**: the photos saved earlier in that run stay on the device
  (FR-008); the run stops and reports "members only".
- **Not found for one photo**: the photo is skipped for this run, not counted, and retried on the next run. It does
  not stop the run.
- **Network drops mid-run**: the photo in flight is not counted; the whole run is retried later under the existing
  retry policy, which resumes from the first missing photo.
- **Profile photo forbidden while offline copy exists**: the copy is removed; the placeholder is shown until a later
  success.
- **Profile photo address changes**: the stored version tag is discarded before the request (current behavior).
- **Media still public (before the web server switch)**: everything keeps working; the new outcomes simply never
  occur.
- **Leader-only media (`members/`)**: the app does not download from this area today; no behavior is defined for it
  here.

## Requirements *(mandatory)*

### Functional Requirements

#### Gallery download

- **FR-001**: A photo MUST count as downloaded only when it is saved on the device during the run, or was already on
  the device before the run.
- **FR-002**: A photo whose download fails for any reason MUST NOT be saved, MUST NOT count as downloaded, and MUST be
  requested again on the next run.
- **FR-003**: When the server rate-limits a photo request, the run MUST stop immediately without requesting further
  photos, keep every photo already saved, report progress equal to the photos actually on the device, show no error
  message, and schedule a later attempt that waits before starting.
- **FR-004**: When the server forbids a photo request, the run MUST stop immediately without requesting further photos
  and MUST end in the "no access" state. The "no access" state MUST NOT be retried automatically.
- **FR-005**: The "no access" state MUST show a message that the gallery is available only to members, MUST NOT offer
  a sign-in button, and MUST be visible whether or not albums already exist on the device.
- **FR-006**: When a single photo is not found, or fails on a network error, the run MUST continue with the remaining
  photos (subject to the existing whole-run retry policy for network failures).
- **FR-007**: The same outcome rules (FR-001 to FR-006) MUST apply to both the full-gallery download and the
  single-album download.
- **FR-008**: When the download ends in the "no access" state, photos already on the device MUST be kept and remain
  visible, with the "members only" message shown alongside the local albums. This matches today's behavior when the
  photo list itself is forbidden, and a mistaken refusal from the server never wipes the local gallery. Local photos
  are still removed on sign-out, as today.

#### Profile photo

- **FR-009**: When the server forbids the profile photo, the app MUST remove the stored photo and its version tag, show
  the placeholder, and report no error.
- **FR-010**: When the server rate-limits the profile photo, or the request fails on a network error, the app MUST keep
  and show the last stored photo and report no error.
- **FR-011**: "Not modified" and "not found" MUST keep their current behavior: keep the stored photo, and remove it and
  show the placeholder, respectively.
- **FR-012**: Any other failure MUST keep the last stored photo; it MAY be reported through the app's existing generic
  error handling.

#### Session

- **FR-013**: An unauthenticated refusal on any media request MUST first attempt the existing transparent session
  renewal; if renewal fails, the app MUST follow the existing sign-out flow and MUST NOT show a media-specific error.
- **FR-014**: An unauthenticated refusal during the gallery download MUST stop the run and end in the existing
  "sign in to access the gallery" state.

#### Media access path

- **FR-015**: Every media request MUST go through the signed-in download path. No screen may display an image by
  loading a remote media address directly.
- **FR-016**: The audit outcome MUST be recorded in the plan: every image display site and every media request, with
  its source (local file or signed-in download). If any direct load is found, it MUST be moved to the signed-in path.

#### Compatibility and release

- **FR-017**: The release MUST behave correctly both while media is still public and after the web server switches to
  protected media.
- **FR-018**: The release MUST be published after the backend change (009-protected-media-access) and before the web
  server switches media to the protected flow.

#### Verification

- **FR-019**: Each outcome path above — gallery: success, rate limited, forbidden, not found, network error,
  unauthenticated; profile photo: success, not modified, not found, forbidden, rate limited, network error — MUST be
  covered by an automated test.
- **FR-020**: Those tests MUST simulate the server with reusable fake API classes, not with per-test inline stubs.

### Key Entities

- **Media file**: a file served from one of the protected areas (`gallery/`, `profiles/`, `members/`), identified by
  its address and carrying a version tag used to skip unchanged downloads.
- **Gallery photo**: a member-only photo belonging to an album; stored on the device together with its description.
  Its state within a run is *already present*, *saved*, or *failed* — only the first two count as downloaded.
- **Gallery download run**: one background pass over the photo list. Ends as *complete*, *paused by rate limit*
  (to be resumed later), *no access*, *signed out*, or *failed after retries*.
- **Profile photo**: the signed-in person's own photo, stored on the device with its address and version tag, shown in
  the top bar and on the profile screen, with a placeholder when absent.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After enough runs to get past the rate limit, 100% of the gallery's available photos are on the device —
  no photo is ever permanently skipped because of a refusal or a transient failure.
- **SC-002**: The progress reported to the member never exceeds the number of photos actually on the device.
- **SC-003**: A non-member's gallery download makes at most one refused photo request per run and shows the
  "members only" message within the same run.
- **SC-004**: A profile photo refusal due to rate limiting or a network outage never replaces a previously shown photo
  with the placeholder.
- **SC-005**: Zero error messages are shown to the person for profile photo refusals (forbidden, rate limited,
  network).
- **SC-006**: After the web server switch, zero broken images appear on any screen for a signed-in member.
- **SC-007**: Every outcome listed in FR-019 has a passing automated test.

## Assumptions

- **Current state (verified in code on 2026-09-25)**: gallery photos and the profile photo are already downloaded
  through the signed-in client, so the session and its transparent renewal already apply to media. Every image on
  screen (gallery thumbnails, album grid, photo viewer, top bar account photo) is loaded from a local file. No
  session-less request to the media area exists. The image display library needs no change.
- **Current gallery bug (verified)**: both the full-gallery background download and the single-album download count a
  failed photo as downloaded and move on.
- **Current profile photo behavior (verified)**: "not modified" and "not found" are handled; any other refusal is
  raised as a generic error.
- **Forbidden stops the whole run, not just one album**: the user description says a refusal stops "the download for
  that album". Because the server's rule for the gallery is gallery-wide (members only), a refusal on one album means
  every other album would be refused too. The run therefore stops entirely; continuing would only spend requests on
  guaranteed refusals.
- **Not found is per-photo**: a missing file is a server-side data problem for that one photo, not a permission
  decision, so it neither stops the run nor shows a message.
- **Rate limit resumption uses the background scheduler's own delayed retry**: no custom wait is added; if the server
  sends a suggested wait time, honoring it is a plan-level decision.
- **The "members only" text reuses the server's user-facing message when present** (per the constitution's rule on
  structured error details), falling back to the app's own text.
- **Sign-out on unrenewable session is unchanged**: the existing session renewal already clears the session when
  renewal fails; this feature only makes sure media downloads end in the right state afterwards.
- **Leader-only media (`members/`) is out of scope**: the app downloads nothing from that area today.
- **Domain specs are updated alongside the code**: the gallery domain spec (download and screen states) and the
  profile photo section of the core domain spec change in the same commits as the implementation.
