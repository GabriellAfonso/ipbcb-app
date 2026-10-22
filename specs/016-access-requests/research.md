# Research: Access Requests

Decisions taken while planning. Backend facts are in `backend/specs/022-access-requests/contracts/access-api.md`.

## R-01 — Where the locked screens learn the access status

- **Decision**: a core port `MemberAccessRepository` (`core/domain/access/`) exposes `memberAccess: Flow<MemberAccess>`
  built from the profile snapshot, and `requestAccess(type, message): Result<Unit>`. It is implemented in
  `features/profile/data` (like `ProfileAccessRepository` implements `AccessRepository`). A shared stateful
  composable `MembersOnlyPlaceholder` in `core/presentation/components/access/` collects a small core ViewModel and
  renders the prompt. Gallery, schedule and birthdays swap their 403 branch (`showLoginButton = false`) for it.
- **Rationale**: the three screens already know "this is a 403", and features never import each other. Reading the
  status from the profile (not from the 403) gives the right text on every screen at once after a request.
- **Alternatives**: carry status in each screen's ViewModel (three copies of the same logic); decide by the 403
  `error_code` (the server's 403 does not say pending/blocked).

## R-02 — Rename `isMember` → `memberAccess`

- **Decision**: `MeProfileDto` gets `@SerialName("member_access") val memberAccess: Boolean? = null` and the old
  `@SerialName("is_member") val isMember: Boolean = false` (default added). The model exposes only
  `memberAccess = dto.memberAccess ?: dto.isMember`. New fields: `access_status: String? = null`,
  `access_requests_blocked: Boolean? = null`, `data_correction_pending: Boolean? = null`.
- **Rationale**: an old snapshot only has `is_member` and must decode (a failed decode deletes the snapshot). An old
  server sends no status: `null` → `AccessStatus.UNKNOWN`, which hides request buttons (spec FR-002).

## R-03 — Request errors

- **Decision**: no change to `ResponseExt`. `403 ACCESS_REQUESTS_BLOCKED` arrives as `AppError.Auth(403,
  userMessage = detail)`; `409`/`429` as `AppError.Server` with `detail`. The dialog shows `toUserMessage()` and the
  repository refreshes the profile after any refusal, so the screen moves to the real state (blocked / pending /
  granted).
- **Rationale**: the server's `detail` already says what to do (including when to retry); the constitution keeps a
  single parse point. `PermissionDeniedInterceptor` only reacts to `PERMISSION_DENIED`, so a blocked request does not
  trigger an access refresh loop, and no 403 signs out.

## R-04 — Roll data in memory only

- **Decision**: `GetOwnMemberUseCase` → `OwnMemberRepository` (profile feature) calls `GET api/me/member/` with the
  authed client, no cache, no snapshot. The result lives in `ProfileUiState` only. `404 MEMBER_NOT_LINKED` →
  `null` (section hidden). Loaded when the profile opens and `memberId != null`. Never logged.
- **Rationale**: spec FR-009; the server sends `no-store`. OkHttp has no disk cache on the authed client (verified:
  `@Client` builds no `Cache`).

## R-05 — "Perfis" lives in the admin feature

- **Decision**: new sub-feature `features/admin/profiles/` (api, repository, use cases, list + detail screens) with a
  nested `profilesGraph` inside `adminGraph`, a `PanelCard.PROFILES(AtLeast(Scope.PROFILES, VIEW))` and
  `Scope.PROFILES("profiles")`. The member picker reuses `ObserveMembersUseCase` / `RefreshMembersUseCase` from
  `features/admin/members` (same feature, so no cross-feature import).
- **Rationale**: same pattern as Membros; panel gating comes for free.

## R-06 — Account photos in Perfis

- **Decision**: a `@ProfilePhotoLoader` Coil `ImageLoader` over the `@Client` OkHttp client with a memory cache only
  (no disk cache), provided in `features/admin/profiles/di/`.
- **Rationale**: profile photos are protected media and personal data; members' photos already never touch the disk
  unencrypted. A memory-only cache is enough for a short list.

## R-07 — Pushes

- **Decision**: `PushMessage` gains `AccessRequestCreated(requestType)`, `AccessRequestApproved(requestType)`,
  `AccessRequestDenied(requestType, reason?)`. The handler (signed-in only) notifies on all three and, for approved
  and denied, calls `RefreshAccessUseCase` (one profile refresh). A new `AccessRequestNotifier` uses a new channel
  "Pedidos de acesso" (same `POST_NOTIFICATIONS` permission as the setlist; no new permission). Taps:
  `NotificationTarget.ProfilesPending` → `AppNavHost` opens Gestão → Perfis (Pendentes) when the user can view
  profiles, else a toast; `NotificationTarget.Profile` → profile screen.
- **Rationale**: reuses the 011 path end to end; the leader check on tap mirrors `ConfirmPlays`.

## R-08 — Refresh after actions

- **Decision**: creating a request refreshes the profile (`RefreshAccessUseCase`). Every Perfis action returns the
  account detail; the detail VM replaces its state with it and the list repository updates (or drops, for Pendentes)
  that row in memory. Perfis lists are memory-only (`MutableStateFlow`, ETag kept in memory) like Membros.

## R-09 — Texts

- Status: "Liberado" / "Aguardando" / "Não liberado". Pending on locked screens: "Pedido enviado. Aguardando a
  liderança." Blocked: "Pedidos de acesso indisponíveis. Fale com a liderança."
- Request status in Perfis: "Pendente" / "Aprovado" / "Negado"; an approved correction reads "Resolvida".
- Gender `F` → "Feminino", `M` → "Masculino", otherwise hidden. Birthday "14 de março". Baptism "20/05/2001".
