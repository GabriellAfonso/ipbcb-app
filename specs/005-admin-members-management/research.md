# Research: Members Management for Church Leaders

**Feature**: `005-admin-members-management` | **Date**: 2026-09-26

Every decision below was checked against the code on branch `005-admin-members-management` (base `dev` at
`10bde13`).

---

## R1. Where the feature lives and how it is reached

**Decision**: A new sub-area `features/admin/members/` (data / di / domain / presentation), with its own nested
graph `membersGraph` inside `adminGraph`, like `reportsGraph`. `AdminNav` gains `members`; the "Membros"
`AdminAction` drops `enabled = false` and navigates to `MembersRoutes.GRAPH`.

**Rationale**: Four screens (list, profile, form, history) → a graph (`CLAUDE.md`: multiple screens →
`XNavGraph.kt`). Nesting lets the list and the profile share one graph-scoped state holder. `features/admin/*`
already groups `register`, `schedule`, `reports` this way.

**Leader gate**: the admin panel is only offered when `authState.isAdmin` (`specs/admin/spec.md` §5), so the
members area inherits the gate — no new check. The server stays the authority (403 handling in R8).

**Alternatives considered**: a top-level `features/members/` — rejected, the area only exists for administrators
and `core/` already owns the regular member list (`MembersRepository`, birthdays); a second top-level "members"
would be confusing.

---

## R2. Sending `null` in a PATCH

**Finding**: The shared `Json` is built with `explicitNulls = false` (see `CollectionSettingsMapper.diff`), so a
`null` property in a `@Serializable` DTO is **dropped**, not sent. The members contract needs both "absent" (not
changed) and `null` (clear status, role, dates, gender).

**Decision**: The PATCH body is a `JsonObject` built with `buildJsonObject { }` from a `MemberChanges` domain
value (see `data-model.md`), where each changed field is present and a cleared one is `JsonNull`. The Retrofit
method takes `@Body body: JsonObject`. Create (POST) uses the same builder with every non-empty field, so both
calls share one mapping and one test.

**Alternatives considered**: a second `Json` instance with `explicitNulls = true` — rejected, it would send
`null` for every unchanged field and wipe them; `JsonElement` wrappers per field in a DTO — more types for the
same result.

---

## R3. Member photos without touching disk

**Finding** (from `specs/004-protected-media-downloads/research.md` R1): today no screen loads a remote image;
every `AsyncImage` shows a local `File`. Member photos must not be written to disk (spec FR-033), and
`photo_url` only opens with a leader's token.

**Decision**: A dedicated Coil `ImageLoader`, provided in `features/admin/members/di/` under a local qualifier
`@MemberPhotoLoader`, built with:

- `okHttpClient(@Client client)` — the authenticated client (`AuthInterceptor` + `TokenAuthenticator`), so a
  401 is refreshed transparently;
- `diskCache(null)` and `respectCacheHeaders(false)` — nothing written to disk;
- the default memory cache — photos shown again within the session are not re-downloaded.

Replacing a photo yields a new URL (the server stores `members/<random>.<ext>`), so a memory cache keyed by URL
never shows a stale photo. `If-None-Match` for images is dropped: without a disk cache Coil has nothing to
revalidate, and the memory cache already avoids repeated downloads (spec FR-034 is a "MAY").

Screens receive the loader through the ViewModel (`val imageLoader: ImageLoader`), keeping presentation free of
DI lookups. Error/fallback (401 after failed refresh, 403, 404, 429, I/O) → `AsyncImage`'s `error` slot shows
the initials (spec FR-026); nothing is surfaced as an error message.

**Alternatives considered**: download bytes through Retrofit into a `ByteArray` held in state — rejected,
reinvents Coil's memory cache, decoding and lifecycle; the app's default `ImageLoader` — rejected, it has a disk
cache and the default unauthenticated client.

---

## R4. Picking and cropping a photo — reuse without importing `features/profile`

**Finding**: The pick → UCrop → bytes flow is written inline in `ProfileScreen.kt` (`pickLauncher` +
`cropLauncher`, square 1:1, max 512×512, destination `cacheDir/cropped_<ts>.jpg`). Features never import each
other.

**Decision**: Move the flow to `core/presentation/components/SquarePhotoPicker.kt` as
`rememberSquarePhotoPicker(onPicked: (ByteArray) -> Unit, onCancelled: () -> Unit): () -> Unit`, and use it from
both `ProfileScreen` and the member profile. Behavior for the profile photo is unchanged. The picker **deletes the
cropped file right after reading its bytes** — for the profile it is dead weight in `cacheDir`, for a member photo
it would be a copy of sensitive data on disk.

**Transient file note**: UCrop can only write its output to a file, so a cropped member photo exists in
`cacheDir` for the moment between the crop result and the read. That is the only on-disk trace and it is removed
immediately; recorded here as the accepted exception to FR-033.

**Format and size (FR-024)**: UCrop output is always JPEG ≤ 512×512, so the 10 MB / format guard never fires for
cropped images. The guard stays in the domain (`ValidateMemberPhotoUseCase`: non-empty, ≤ 10 MB, JPEG/PNG/WEBP/GIF
magic bytes) so the rule is enforced regardless of the picker.

**Alternatives considered**: copying the flow into the members screen — rejected, two copies of the UCrop options
would drift.

---

## R5. In-memory state and "the list reflects changes" (FR-010, FR-034)

**Decision**: `MembersAdminRepositoryImpl` is a `@Singleton` holding:

- `members: MutableStateFlow<List<MemberSummary>?>` — `null` = not loaded this session;
- the list `ETag`;
- a map `id → (ETag, MemberRecord)` for records opened this session.

`observeMembers()` exposes the flow; `refreshMembers()` fetches with `If-None-Match` and keeps the cached list on
304. Every successful write patches the in-memory list directly (create → insert sorted by name; edit/validity →
replace the summary built from the returned record; photo → replace `photoUrl`; delete → remove) and drops that
record's ETag. The list screen only observes the flow, so returning to it shows the change with no extra request
and no event plumbing between screens.

**Rationale**: The roll is small (hundreds); one list in memory is cheap. Patching locally avoids a full refetch
after each edit while the next `refreshMembers()` still reconciles with the server.

**Alternatives considered**: navigation result / event from profile to list — more wiring for the same outcome;
refetch on every return — extra traffic and a loading flash.

---

## R6. Clearing member data on sign-out (FR-035)

**Finding**: `CoreViewModel.logout()` clears each cache by name (profile photo, schedule, birthdays, gallery).
`core/` must not import `features/admin`. A session can also end without `logout()`: when the token refresh
fails, `TokenAuthenticator` clears the token store and `isLoggedInFlow` emits `false`.

**Decision**: A new core contract `core/domain/session/SessionScopedCache` (`fun interface { suspend fun
clear() }`), multibound `@IntoSet` like `Preloadable`. The members module contributes one that clears the
repository state **and** the `@MemberPhotoLoader` memory cache. `CoreViewModel` calls every `SessionScopedCache`
in `logout()` and whenever `isLoggedInFlow` goes from `true` to `false`.

**Rationale**: Same multibinding pattern the app already uses for startup (`Preloadable`/`Refreshable`); covers
both sign-out paths; future session-only caches plug in without touching `CoreViewModel`.

**Alternatives considered**: repository observing `AuthSession` — `AuthSession` lives in `features/auth`; adding
a login flow to `AuthStatusProvider` widens a deliberately narrow interface.

---

## R7. Validation — local rules and server `field_errors`

**Decision**: `ValidateMemberDraftUseCase` (pure, `DateProvider` injected) returns `Map<MemberField, String>`:

| Rule | Field | Message |
|------|-------|---------|
| name blank | `NAME` | "Informe o nome." |
| name > 255 | `NAME` | "Use no máximo 255 caracteres." |
| first/last name > 255 | `FIRST_NAME`/`LAST_NAME` | same |
| year < 1 or > current year | `BIRTH_YEAR` | "Ano de nascimento inválido." / "A data não pode estar no futuro." |
| day/month not a real date for the year (29/02, non-leap) | `BIRTH_DAY` | "Esse dia não existe nesse mês e ano." |
| full birth date > today | `BIRTH_YEAR` | "A data não pode estar no futuro." |
| baptism date > today | `BAPTISM_DATE` | same |
| baptism < full birth date, or baptism year < birth year (year only) | `BAPTISM_DATE` | "O batismo não pode ser antes do nascimento." |

Server `field_errors` arrive on `AppError.Server.fieldErrors` keyed by API name (`name`, `birth_year`,
`status_id`, `ministry_ids`, …); a single mapper `MemberField.fromApiKey()` puts them on the same field. Unknown
keys and `non_field_errors` go to the form's general message. Service-level 400s without `field_errors` (unknown
status id, date rules re-checked by the server) show `detail` as the general message, and the form reloads the
options (edge case "option removed while the form is open").

**Day and month only**: never "future", and the baptism rule is not checked (R10).

**Stored data that breaks a rule**: validation runs on the *draft*; a draft is only saved when changed, so an
untouched legacy value never blocks anything (spec edge case).

---

## R8. Error handling per status

| Status | Where | Behavior |
|--------|-------|----------|
| 401 | any | `TokenAuthenticator` refreshes; on final failure tokens are cleared → sign-out path (R6), app returns to login as today |
| 403 | any members call | `AppError.Auth(403)` → one-time event `LeaveMembersArea(message)`; the graph pops back to `AdminRoutes.ADMIN` and shows the message (FR-003) |
| 404 | record, history, write on a member | event `MemberGone`: "Este membro não existe mais", the member is removed from the in-memory list, back to the list (FR-038) |
| 400 | writes | field errors (R7) or general message |
| I/O | any | `AppError.Network` → generic connection text; list shows retry |
| photo errors | image loads | initials fallback, silent (R3) |

All texts go through `AppError.toUserMessage()`; the `detail` of a structured error is already `userMessage`
(constitution). "Este membro não existe mais" is app text set as `userMessage` in the repository when the status
is 404 on a member route, so the ViewModel writes no copy.

---

## R9. History sentences

**Decision**: `BuildHistorySentenceUseCase` (pure) turns a `HistoryEntry` into a `HistoryLine(editor, text,
changedAt)`:

| `field` | Text |
|---------|------|
| `created` | "cadastrou o membro" |
| `photo` + `photo changed` | "trocou a foto" |
| `photo` + `photo removed` | "removeu a foto" |
| known field | "alterou <Rótulo> de <A> para <B>" |
| unknown field | "alterou <field> de <A> para <B>" |

Labels: name → Nome, first_name → Primeiro nome, last_name → Sobrenome, birth_day → Dia de nascimento,
birth_month → Mês de nascimento, birth_year → Ano de nascimento, birth_date (entries before the split) →
Nascimento, gender → Sexo,
status → Situação, role → Cargo, ministries → Ministérios, baptism_date → Batismo, is_active → Perfil.
Values: `YYYY-MM-DD` → dd/MM/yyyy (old `birth_date` in year 0001 → "dd/MM"); `birth_month` → month name; `birth_day`/`birth_year` verbatim; `M`/`F` → Masculino/Feminino (gender field only); `true`/`false` →
Válido/Inválido (is_active only); `null` → "vazio"; anything else verbatim (status/role/ministry names). Editor
`null` → "Usuário removido". `changed_at` (UTC ISO) → device local time, dd/MM/yyyy HH:mm, formatted in
presentation.

The profile's history card shows the first line of the same list (fetched when the profile opens).

---

## R10. Age and "há N anos"

**Decision**: `MemberAge` computed in the domain with `DateProvider.today()`: `Period.between(birth, today).years`,
"1 ano" / "N anos"; baptism the same with "há 1 ano" / "há N anos" / "este ano" when 0. Future or missing dates →
no value (the UI shows "Não informado").

**Partial birth date** (spec FR-011, FR-016a): the wire carries `birth_day`, `birth_month`, `birth_year`, mapped
to `BirthDate(day, month, year)` in `members/domain/model/BirthDate.kt`. Consequences:

| Where | Rule |
|---|---|
| Age | full date → exact; year only → "N anos" (current year − year); day+month only → "Desconhecida" |
| Profile display | "dd/MM/yyyy", "dd/MM", "yyyy", or "Não informado" |
| Form | birthday (day + month dropdowns, both or neither, "Limpar") and year (number field) are separate; clearing one keeps the other; February offers 29 with no year or a leap year |
| Validation (R7) | day+month only is never "future" and skips the baptism rule |
| PATCH | each part is its own `MemberField`, so only changed parts are sent; `null` clears one part |

---

## R11. Search

**Decision**: Filtering in the ViewModel over the in-memory list: `Normalizer.NFD`, strip combining marks,
lowercase, `contains`. Recomputed with `combine(members, query)`. No debounce needed for hundreds of items.

---

## R12. Logging (FR-036)

**Finding**: `HttpLoggingInterceptor` is `BASIC` in debug (method + URL + status, no bodies) and `NONE` in release.
URLs carry only member ids.

**Decision**: No change. Members code logs with `Timber` using the member id only; no `Timber` call receives a
DTO, domain model or name. Checked by a grep in `quickstart.md`.

---

## R13. Tests

**Decision** (fakes, per `CLAUDE.md`):

- `FakeMembersAdminApi` (src/test) — scripted responses per call, records request bodies and `If-None-Match`.
- Repository: list 200/304, write patches the in-memory list, 404 → member removed, clear().
- Mapper: `MemberChanges` → `JsonObject` (absent vs `JsonNull`, ministries replace), DTO → domain.
- Use cases: validation table, history sentences table, age/baptism, photo guard.
- ViewModels: list (search, loading/error), profile (validity switch revert on failure, 403 → leave, 404 → gone),
  form (only changed fields, field errors mapping, unsaved-changes flag), delete (name match rule).
- `CoreViewModel`: `SessionScopedCache.clear()` on logout and on logged-in → logged-out.
