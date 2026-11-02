# Quickstart: validating Password Recovery

## Prerequisites

- Backend `028-password-recovery` running, with the Brevo SMTP key configured so messages are delivered.
- A password account with an e-mail you can read, a password account without one, and a Google-only account.

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.auth.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.profile.*"
./gradlew :app:testDebugUnitTest
```

## Manual

| # | Steps | Expected |
|---|-------|----------|
| 1 | Login, "Esqueci minha senha", type an unknown address | Generic answer, code step opens, no message arrives |
| 2 | Repeat with your address | Message with username and code. Wrong code: "Código inválido ou expirado." |
| 3 | Right code, short password | Error under the field. Fix it: the same code still works |
| 4 | Finish | Back to login with "Senha alterada"; the new password signs in |
| 5 | Airplane mode on any step | Network message, typed data kept, button retries |
| 6 | "Reenviar código" | Disabled for 60 s, then sends |
| 7 | Register with an empty and a malformed e-mail | Error under the field, nothing sent. A valid one enters the app |
| 8 | Profile of the new account | Address and "Não confirmado". "Confirmar", code, then "Confirmado" |
| 9 | "Alterar" with a wrong password | "Senha incorreta." under the field, still signed in |
| 10 | "Alterar" with the right password | New address and "Não confirmado" |
| 11 | Open the app on the account with no e-mail | Card in the home, shortcuts in place. "Agora não": never returns |
| 12 | Reset the flag by clearing data, repeat with "Adicionar e-mail" | Profile opens with the form, which asks for the password |
| 13 | Largest system font on a small screen and landscape | Card scrolls inside itself, shortcuts stay reachable |
| 14 | Google-only account | No section, no card, no change in registration with Google |

## Privacy check

On a debug build, after using every flow:

```bash
adb shell run-as com.ipb.castelobranco grep -rl "@" files/ || echo none
```

Review the hits: the address must not appear in `me_profile` or any plain file, and the sealed store is unreadable. Open the
in-app log viewer and confirm no address. After logout, the profile section's data is gone from the sealed store.
