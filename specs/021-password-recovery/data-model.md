# Data Model: Password Recovery by E-mail

## RecoveryEmailStatus (domain, profile)

| State | Meaning | Section | Notice |
|-------|---------|---------|--------|
| `Unknown` | Cached before this feature, vault empty, or no profile yet | Hidden | No |
| `None` | The server says the account has no address | "Adicionar e-mail" | If it has a password and the flag is unset |
| `Set(address, confirmed)` | Address known | Address with "Confirmado" / "Não confirmado" | No |

Mapped from the DTO: `recovery_email_confirmed == null` is `Unknown`; a null or blank address is `None`; otherwise `Set`.

## MeProfileDto (additive)

| Field | Type | Default | Note |
|-------|------|---------|------|
| `recovery_email` | `String?` | `null` | Never written to the plain snapshot |
| `recovery_email_confirmed` | `Boolean?` | `null` | `null` is unknown (cached before this feature) |

## RecoveryEmailVault (sealed `@AuthPrefs`)

One key, `recovery_email`.

| Stored value | Meaning |
|--------------|---------|
| Absent | Not known: the snapshot cannot be trusted for the address |
| Empty string | The account has no address |
| Address | The account's address |

Cleared with the snapshot and when the session ends.

## Notice flag (`@SettingsPrefs`)

`recovery_email_notice_answered: Boolean`, default `false`. Per device. Survives logout on purpose.

## RegisterFormState (additive)

`email: String`, help line under the field, `RegisterErrors.email` for the server's `field_errors.recovery_email`.

| Situation | Help |
|-----------|------|
| Default | "Usado só para recuperar sua senha." |
| Submitted and empty | "Preencha este campo." |
| Submitted and malformed | "Informe um e-mail válido." |
| Server error | The server's message |

## PasswordRecoveryState

| Field | Type | Note |
|-------|------|------|
| `step` | `Email` / `Code` | Back from `Code` returns to `Email` keeping the address |
| `email`, `code`, `newPassword`, `newPasswordConfirm` | `String` | Memory only. Code keeps digits only, at most 6 |
| `submitted` | `Boolean` | Empty fields become errors after the first submit |
| `emailError`, `newPasswordError`, `newPasswordConfirmError`, `banner` | `String?` | Server errors, cleared on edit |
| `isSending`, `isResetting` | `Boolean` | Requests in flight |
| `resendSecondsLeft` | `Int` | 0 enables "Reenviar código" |

Transitions: `Email` to `Code` on any successful request answer. `Code` ends with the `Done` one-shot event, which returns to
the login.

## Profile dialogs

| State | Fields |
|-------|--------|
| `RecoveryEmailFormState` | `isSaving`, `emailError`, `passwordError`, `error` |
| `RecoveryCodeDialogState` | `isSending`, `isConfirming`, `codeError`, `error`, `resendSecondsLeft` |

Passwords and typed codes stay in the composition, never in the screen state, as in the existing dialogs.
