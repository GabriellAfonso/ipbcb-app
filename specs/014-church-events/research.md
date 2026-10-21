# Research: Church Events

Each entry settles a technical question the spec leaves to the plan. Everything was read from the current code; no
new library is needed.

## R1 — Where the code lives

- **Decision**: two areas.
  - `features/events/` — the public read side: snapshot repository, highlight use case, home ViewModel, detail
    screen.
  - `features/admin/events/` — management: list, form, writes. It is nested in `adminGraph` like `membersGraph`.

  The pieces both sides need go to `core/`, because features never import each other:
  - the `@EventImageLoader` Coil loader (`core/di/EventImageLoaderModule.kt`);
  - the date label (`core/presentation/format/EventDateLabel.kt`);
  - the highlight page composable (`HighlightEvent` in `core/presentation/components/Highlight.kt`).
- **Rationale**: management follows the existing admin areas (members, reports), and the home reads features through
  their ViewModels, as `CoreView` already does with `ScheduleViewModel`. The two sides speak different resources
  (`EventPublic` vs `EventRecord`) and different endpoints, so they share no repository.
- **Alternatives**:
  - Everything in `features/events/`, with the admin panel navigating there: `adminGraph` would import another
    feature, which the rules forbid.
  - Everything in `features/admin/`: the home would depend on an admin package for public content.

## R2 — Public list: snapshot repository

- **Decision**: `EventsRepositoryImpl : BaseSnapshotRepository<List<EventPublicDto>, List<Event>>`.
  - Cache key `"events"`; the fetcher is a `RetrofitSnapshotFetcher` with `If-None-Match`.
  - The API is created from `@AuthedRetrofit`. `AuthInterceptor` adds the bearer only when a token exists, so a
    logged-out request goes without one and the server answers public-only.
  - Contributes `Preloadable` (disk at boot) and `Refreshable` (network at boot and after `LoginSuccess`).
- **Rationale**: this is the project's offline pattern (CLAUDE.md pitfall 3). The ETag/304 support is already there,
  and `Vary: Authorization` on the server makes an anonymous ETag never match a member's list.
- **Alternatives**:
  - `@AuthLessRetrofit`: members would never get members-only events.
  - A plain in-memory repository: offline would break.

## R3 — Expired events offline

- **Decision**: `ObserveHighlightEventsUseCase` maps the snapshot through a filter:
  `(endDate ?: startDate) >= DateProvider.today()`, then `take(3)`. `GetEventUseCase(id)` reads from the same
  filtered list.
- **Rationale**: the server stops returning an event after its last day, but a saved list can be days old. The
  filter keeps FR-006/SC-004 true offline. `DateProvider` makes the use case testable on a fixed day.

## R4 — Logout, session drop and login

- **Decision**:
  - A `SessionScopedCache` from the events module clears the snapshot (disk and memory) and the event image
    loader's memory and disk caches. It runs on explicit logout, on a session drop, and on a session wipe.
  - The home ViewModel then fetches the public list again when `CoreViewModel.isLoggedIn` changes. `CoreView` hands
    it the value with `LaunchedEffect(isLoggedIn)`, and the ViewModel ignores the first value it sees.
  - On login, `refreshDataFromNetwork()` (the `Refreshable`) and the same login-change refresh fetch the member list.
- **Rationale**: logout stays on the home screen (`onLogoutSuccess = {}` in `AppNavHost`), so no resume happens.
  `logout()` clears the session caches *before* `logoutUseCase()` removes the tokens. Fetching inside `clear()`
  would therefore still send the member's token. `isLoggedIn` turns false only after the tokens are gone, and the
  session-drop path also clears the caches before it flips the flag.
- **Alternatives**: observing the auth session from `features/events` would import `features/auth`, which is
  forbidden.

## R5 — Refresh on returning home

- **Decision**: `CoreView` calls `eventsViewModel.refresh()` from a `LifecycleResumeEffect`. The repository ignores
  a refresh while another one is in flight (a `Mutex.tryLock`).
- **Rationale**: this covers FR-008 after a management write without core and admin knowing each other. Thanks to
  the ETag, a refresh with nothing new costs one 304.

## R6 — Event images

- **Decision**: `@EventImageLoader` in `core/di`.
  - Coil `ImageLoader` on `@Client`, with `respectCacheHeaders(false)`.
  - Disk cache `cacheDir/event_images`, 50 MB.
  - Memory cache `ImageMemoryBudget.EVENT_IMAGES = 0.05`.
- **Rationale**:
  - The `@Client` client sends the token only with a session, which members-only images need.
  - The server gives each image a new URL whenever it is replaced, so caching by URL forever is safe. The server
    sends `private, no-cache`; respecting it would revalidate every carousel turn.
  - This is the same shape as `GalleryThumbnailLoaderModule`.
- **Failure display**: a 404 (members-only or replaced image) or no network shows the card's neutral background
  under the title and date.

## R7 — 16:9 crop

- **Decision**: generalize `rememberSquarePhotoPicker` into
  `rememberCroppedPhotoPicker(aspectX, aspectY, maxWidth, maxHeight, title)`.
  - `rememberSquarePhotoPicker` stays as a one-line wrapper (1:1, 512).
  - Events use 16:9 with a 1920×1080 maximum and the title "Recortar imagem".
- **Rationale**: uCrop is already wired, and the picker already deletes its temp file. The server re-encodes to
  1920×1080 at most anyway, so a larger upload is wasted bytes.
- **Small source**: uCrop does not upscale. An image under 640×360 reaches the server, which refuses it, and the
  message lands under the image field. A local check (decode bounds) also catches it before sending, with the same
  copy style.

## R8 — Multipart writes

- **Decision**:
  - `@Multipart @POST` and `@Multipart @PATCH` with `@PartMap fields: Map<String, RequestBody>` (text/plain) and an
    optional `@Part image: MultipartBody.Part?` named `image`.
  - Create sends every filled field.
  - Edit sends only the fields that changed. A cleared optional field is sent as `""`.
- **Rationale**: the contract accepts only multipart and answers 415 to JSON. Sending only the changes mirrors
  `MemberChanges`.

## R9 — Errors

- **Decision**:
  - `field_errors` keys map to `EventField` through `EventField.fromApiKey`. Unknown keys go to `generalError`, as
    `MemberFormViewModel` does.
  - A management read refused with 403 leaves the area (`EventsAdminEvent.LeaveArea`).
  - A 404 on open, save or delete returns to the list with "Este evento não existe mais." and refreshes the list.
  - A refused write stays on the form with a message.
- **Rationale**: this is the same reaction table as `MembersFailure.kt`. The permission-denied interceptor already
  refreshes the profile, and the texts come from `toUserMessage()` (constitution).

## R10 — Management "detail"

- **Decision**:
  - Tapping an event opens the form screen. For a `view`-only user the fields are read-only and no save is shown.
  - Delete is an action in the form's top bar, shown only for `owner`.
- **Rationale**: this meets Story 4.8 and Story 5 with two screens instead of three, and the form already shows
  every field.

## R11 — Date and time pickers

- **Decision**:
  - Dates use `DateFieldWithPicker` (core) plus Material3 `DatePickerDialog`, read as UTC midnight.
  - Time uses Material3 `TimePicker` in a dialog (24 h).
  - End date and time each get a "remover" action, since they are optional.
- **Rationale**: these are the components the admin screens already use, with no new dependency.
