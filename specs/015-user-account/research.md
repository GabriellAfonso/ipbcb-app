# Research: User Account

## R1. New profile fields are nullable, not defaulted to a value

- **Decision**: `username: String? = null`, `has_password: Boolean? = null`, `date_joined: String? = null` in
  `MeProfileDto`; the domain keeps `hasPassword: Boolean?` and `dateJoined: LocalDate?` (unparseable → `null`).
- **Rationale**: a profile cached before this feature has none of them. A `false` default for `has_password` would
  hide "Trocar senha" and send a password account to the Google confirmation while offline. `null` means "unknown":
  the login/date lines are hidden and "Excluir conta" waits for the server's answer.
- **Alternatives**: `Boolean = false` (wrong confirmation path offline), `= true` (offers a password change to
  Google-only accounts).

## R2. Writing the new token pair from the profile feature

- **Decision**: port `core/domain/auth/SessionTokensWriter.replace(access, refresh)`, implemented in `core/data/auth`
  over `TokenStorage.save`. `AccountRepositoryImpl.changePassword` writes the pair as soon as the 200 body is read,
  before returning, so no other request of this flow can run first.
- **Rationale**: features never import each other; `core/` already wraps `features/auth` storage for read-only ports
  (`AuthStatusProvider`, `SessionPresenceProvider`).
- **Alternatives**: return the pair to the use case and let it save (one more hop where a refactor could put a
  request in between); move `TokenStorage` to core (out of scope).

## R3. Race between a password change and a token renewal

- **Problem**: after the server changes the password, the old access and every refresh token are dead. A concurrent
  request that fails with the old access makes `TokenAuthenticator` renew with the old refresh. The renewal fails
  (400/401) and today clears the store, signing the person out. Worse, a renewal that *succeeded* just before the
  change returns a pair that is already blacklisted. If it is saved after the password pair, it overwrites the good
  pair.
- **Decision**: `TokenStorage.swapIfRefresh(expectedRefresh, replacement)` runs one atomic DataStore edit. It writes
  `replacement` (or removes the pair when `null`) only if the stored refresh is still `expectedRefresh`, and returns
  what is stored afterwards. The authenticator uses it both on failure (`replacement = null`) and on success. If the
  stored pair changed in the meantime, it retries the request with the stored access instead of clearing.
- **Residual window**: a renewal that fails *before* the password pair is saved still clears; the save right after
  restores the session. `CoreViewModel` would see true→false→true and wipe the session-scoped memory caches. That is
  harmless (they reload) and needs a request to fail inside the few milliseconds between the answer and the save.
- **Alternatives**: hold the authenticator mutex around the password call. Rejected: it deadlocks when the call
  itself gets a 401 (expired access) and the authenticator needs the same non-reentrant mutex.

## R4. Ending the session after a deletion

- **Decision**: `DeleteAccountUseCase` emits `AuthEventBus.Event.AccountDeleted` on 204. `CoreViewModel` handles it
  with `endSession(notifyServer = false)`, the private body that `logout()` also uses (`notifyServer = true`). The
  only difference is that `UnregisterDeviceUseCase(notifyServer = false)` cancels the scheduler and clears the local
  registered-token store without calling the server. The profile screen receives a one-shot event, pops back to the
  home and shows the "Conta excluída" toast.
- **Rationale**: the cleanup must not depend on the profile screen staying composed. The Activity-scoped
  `CoreViewModel` always collects the bus (same pattern as `LoginSuccess`). A single body means a cache added later
  cannot be forgotten in one of the two paths.
- **Alternatives**: a lambda from the screen to `coreViewModel.onAccountDeleted()` (approved first). It was replaced
  because an event lost when the screen leaves would skip the cleanup. Clearing tokens in the repository was also
  rejected: it would leave the profile photo, schedule and gallery behind.

## R5. Fresh Google ID token

- **Decision**: extract the Credential Manager request from `AuthScreen` to
  `core/presentation/auth/GoogleIdToken.kt` (`suspend fun requestGoogleIdToken(context): String?`, `null` on
  cancel, failure or unknown credential type, with the same logs). `AuthScreen` calls it and keeps its behavior. The
  profile screen calls it when the VM asks (one-shot event). A `null` token goes back to the VM, which only clears the
  busy flag and shows no message.
- **Rationale**: needs an Activity context (presentation), shared by two features → `core/presentation`.

## R6. Errors in dialogs

- **Decision**: a dialog shows `AppError.Server.fieldErrors[field]` under its field (`name`, `current_password`,
  `new_password`, `new_password_confirm`, `password`). Any other error (`INVALID_GOOGLE_TOKEN`,
  `GOOGLE_ACCOUNT_MISMATCH` (403), `ACCOUNT_DELETION_BLOCKED`, `ACCOUNT_LOCKED`, throttle, network) shows
  `toUserMessage()` at the bottom of the dialog. `INVALID_PASSWORD` has no `field_errors`, so the VM puts its message
  on the password field (current password in "Trocar senha", password in "Excluir conta").
- **Rationale**: the server's `detail` is user copy (constitution); a 400 never reaches the authenticator, and a 403
  is not a session error, so no path signs the person out.

## R7. Name and photo refresh

- **Decision**: after a successful `PATCH` or photo `DELETE`, call the snapshot `refresh()`. The ETag changed, so
  the screen gets the new data through the usual flow. The `PATCH` body is not written into the cache directly.
- **Rationale**: keeps a single writer of the snapshot; one extra GET is cheap.

## R8. "No app desde"

- **Decision**: `DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale("pt", "BR"))` →
  "No app desde março de 2025" (formatted in presentation).
