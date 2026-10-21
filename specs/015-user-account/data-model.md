# Data Model: User Account

## MeProfileDto (additive)

| JSON | Kotlin | Default | Notes |
|------|--------|---------|-------|
| `username` | `username: String?` | `null` | read-only |
| `has_password` | `hasPassword: Boolean?` | `null` | `null` = unknown (profile cached before this feature) |
| `date_joined` | `dateJoined: String?` | `null` | `"YYYY-MM-DD"` |

## MeProfile (domain)

Adds `username: String? = null`, `hasPassword: Boolean? = null`, `dateJoined: LocalDate? = null` (parsed with
`LocalDate.parse`, failure → `null`).

## Requests / responses (`features/profile/data/dto/AccountDtos.kt`)

| Type | Body |
|------|------|
| `UpdateProfileRequest` | `{"name"}` |
| `ChangePasswordRequest` | `{"current_password","new_password","new_password_confirm"}` |
| `SessionTokensDto` | `{"access","refresh"}` (response of the password change) |
| `DeleteAccountWithPasswordRequest` | `{"password"}` |
| `DeleteAccountWithGoogleRequest` | `{"id_token"}` |

## AccountDeletionProof (domain)

```text
sealed interface AccountDeletionProof
  Password(password: String)
  Google(idToken: String)
```

## Ports

- `AccountRepository` (profile domain): `updateName(name): Result<Unit>`,
  `changePassword(current, new, confirm): Result<Unit>` (pair already stored on success),
  `deleteAccount(proof): Result<Unit>`.
- `SessionTokensWriter` (core domain): `suspend fun replace(access: String, refresh: String)`.
- `TokenStorage.swapIfRefresh(expectedRefresh: String, replacement: AuthTokens?): AuthTokens?` — atomic; returns
  the stored pair after the edit.

## UI state

```text
ProfileUiState
  isUploading, isRemovingPhoto, photoUrl, localPhotoPath, localPhotoVersion, error
  userName, username?, joinedSince? ("março de 2025"), hasPassword?
  nameDialog: NameDialogState?        (isSaving, nameError?, error?)
  passwordDialog: PasswordDialogState? (isSaving, currentError?, newError?, confirmError?, error?)
  deleteDialog: DeleteDialogState?     (requiresPassword, isDeleting, passwordError?, error?)
  showRemovePhotoConfirm: Boolean
```

`isMember` leaves the UI state (stays in `MeProfile`).

## UI events (one-shot)

`ShowMessage(text)` ("Nome atualizado", "Foto removida", "Senha alterada"), `RequestGoogleIdToken`,
`AccountDeleted`.

## State transitions — deletion

```text
closed → open(requiresPassword = hasPassword)
open → deleting                       (password path: confirm; Google path: token received)
open → requestingGoogle → open        (picker cancelled: no message)
deleting → open + error               (any failure; session untouched)
deleting → AccountDeleted event       (204; AuthEventBus.AccountDeleted emitted)
```
