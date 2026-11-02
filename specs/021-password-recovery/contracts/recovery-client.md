# Contract: how the app uses the recovery API

The server contract is `backend/specs/028-password-recovery/contracts/recovery-api.md` and is not repeated here. This file says
which client calls which endpoint and how each answer reaches the screen.

| Endpoint | Retrofit client | Answer | On the screen |
|----------|-----------------|--------|---------------|
| `POST api/auth/register/` | `@AuthLessRetrofit` | 400 `field_errors.recovery_email` | Under the "E-mail" field |
| `POST api/auth/password-recovery/request/` | `@AuthLessRetrofit` | 200 | The app's own generic text, moves to the code step |
| | | 400 `field_errors.email` | Under the field |
| | | Network | Banner, stays on the step |
| `POST api/auth/password-recovery/confirm/` | `@AuthLessRetrofit` | 204 | Back to login with "Senha alterada" |
| | | 400 `INVALID_EMAIL_CODE` | Banner with the server's `detail` |
| | | 400 `field_errors.new_password*` | Under each field |
| | | Network, 429 | Banner; what was typed stays |
| `GET api/me/profile/` | `@AuthedRetrofit` | `recovery_email`, `recovery_email_confirmed` | Section and notice |
| `POST api/me/recovery-email/` | `@AuthedRetrofit` | 200 | Dialog closes, "E-mail salvo", profile refreshed |
| | | 400 `INVALID_PASSWORD` | Under the password field; session kept |
| | | 400 `field_errors.*` | Under each field |
| | | 429 `ACCOUNT_LOCKED`, 409 `PASSWORD_NOT_SET` | Dialog error; profile refreshed on 409 |
| `POST api/me/recovery-email/send-code/` | `@AuthedRetrofit` | 204 | Dialog open, countdown of 60 s |
| | | 429 `EMAIL_CODE_RATE_LIMITED` | Dialog open, countdown from `retry_after_seconds` |
| | | 409 `RECOVERY_EMAIL_ALREADY_CONFIRMED`, `RECOVERY_EMAIL_NOT_SET` | Dialog closes, profile refreshed |
| `POST api/me/recovery-email/confirm/` | `@AuthedRetrofit` | 200 | Dialog closes, "E-mail confirmado", profile refreshed |
| | | 400 `INVALID_EMAIL_CODE` | Under the code field; dialog stays |

The public endpoints must use `@AuthLessRetrofit` because they read no token. `AuthApi` is already built with it.
