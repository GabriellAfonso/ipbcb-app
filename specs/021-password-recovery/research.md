# Research: Password Recovery by E-mail

Every product decision was taken in session `5ea87f01-d455-45d6-a6f0-13a62f8ae10c`. The items here are the technical
choices that conversation left to the plan.

## R-01: Keeping the address out of the plain snapshot

**Decision**: `SealedProfileSnapshotCache`, a decorator over `SnapshotCache<MeProfileDto>`. `save` writes the address to
`RecoveryEmailVault` (a key in the sealed `@AuthPrefs` store) and hands the inner cache a copy without it. `load` puts it
back. `BaseSnapshotRepository` does not change.

**Rationale**: The whole DTO is serialized by the snapshot layer, so anything else would need a second model or a change
in the base class every feature shares. A decorator is one small class, tested without Android. `@AuthPrefs` is already
sealed, is wiped by `SessionRecovery` when the key is lost, and adds no second Keystore key.

**Alternatives considered**: A dedicated encrypted file (a second key and more code for one string). Not caching the
address, only reading it in memory (the user accepted an encrypted cache, and the profile section should open offline).
Marking the field `@Transient` (it would then not be read from the network either).

## R-02: Unknown versus no e-mail

**Decision**: `recovery_email_confirmed` is `Boolean?` in the DTO, default `null`, and `null` means "unknown". The server
always sends it. The vault distinguishes "none" (empty string stored) from "not stored" (key absent). The cache reports
`null` for the confirmed flag when the vault has no entry, and `loadETag` returns `null` in that state, so the next refresh
downloads the whole profile instead of getting a 304 with no address.

**Rationale**: With a plain default, an old cached profile would read as "no e-mail" and show the notice to someone who
already has one. This is the same reasoning as `has_password`. The ETag rule matters: after the vault is wiped, a 304 would
otherwise leave the address unknown forever.

**Alternatives considered**: A local-only field in the DTO (leaks a storage concern into the wire model).

## R-03: Erasing the address

**Decision**: `SealedProfileSnapshotCache.clear()` clears the vault, covering logout, account deletion and an `AppError.Auth`
on refresh. The vault is also contributed as a `SessionScopedCache`, covering a session that dies when the token refresh
fails, where `CoreViewModel` only clears that set.

**Rationale**: Those are the two distinct paths by which a session ends in `CoreViewModel`. Missing the second would leave the
address sealed on disk with no session.

## R-04: Where the notice lives on a home that does not scroll

**Decision**: While the notice is showing, it takes the **highlight slot** of `HomeLayout` and `HomeLandscapeLayout`, in place
of the carousel, with internal vertical scroll.

**Rationale**: The handoff describes the home as a `Column` with fixed spacers, but today it is a custom `Layout` that measures
band, grid and highlight, and promises no scroll on any height. A card in that flow would have to enter the measure policy
and could move the grid. The highlight slot already takes "what is left", exists in both orientations, and can never overlap
the shortcuts. Internal scroll keeps both buttons reachable at the largest font. The carousel returns the moment the person
answers, which takes one tap.

**Alternatives considered**: A card pinned over the screen (overlaps the shortcuts on a small screen). A dialog (blocks,
which the user refused). A page in the carousel (can be missed, and "Agora não" would sit on a swipeable page).

## R-05: What ends the notice

**Decision**: Only answering it ends it: "Agora não" or "Adicionar e-mail" store the flag. Seeing the card and closing the app
without answering does not.

**Rationale**: The decision text is "'Agora não' or adding the e-mail ends it for good". Marking it on display could spend the
only chance on a person who opened the app for a second during a service. Adding the address from the profile ends it too,
because the condition stops holding.

## R-06: The recovery route's ViewModel scope

**Decision**: `PasswordRecoveryViewModel` uses bare `hiltViewModel()`, scoped to its own route entry, not the graph.

**Rationale**: The graph-scoped rule (Pitfall #1) exists for screens that share state, and this route shares none. With the
graph scope, leaving and coming back would keep an old code and step. The "Senha alterada" message reaches the login through
the graph-scoped `AuthViewModel`.

## R-07: Resend countdown

**Decision**: 60 seconds in both flows, a constant matching the server's limit of 1 per minute per address. In the profile a
429 `EMAIL_CODE_RATE_LIMITED` overrides it with `retry_after_seconds` from `AppError.Server.extras`.

**Rationale**: The public recovery endpoint answers 200 past the limit and sends nothing, so a shorter countdown would let the
person "resend" with no effect and no explanation.

## R-08: E-mail format check

**Decision**: `EmailRules.isValid`: one `@`, no whitespace, a dot in the domain. Input is trimmed before sending.

**Rationale**: `android.util.Patterns` is not available to `domain/` or to unit tests. The local check only catches obvious
mistakes; the server decides, and its message shows under the field.

## R-09: Screen wording for the code error

**Decision**: Show the server's `detail` through `toUserMessage()`. The contract fixes it as "Código inválido ou expirado.".

**Rationale**: The constitution makes a structured `detail` the API's own user-facing copy. The server has a single answer for
any wrong shape of code, so the app needs no local mapping.

## R-10: Logging

**Decision**: No new log line carries the address. `HttpClientModule` logs at `BASIC` in debug (no bodies) and `NONE` in release,
and `AppError` messages from these endpoints hold only the server's `detail`, which never echoes the address.

**Rationale**: The in-app log viewer can share logs (FR-022). A failure log uses a fixed text and no form values.
