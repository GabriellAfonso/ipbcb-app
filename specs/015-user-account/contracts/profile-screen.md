# UI Contract: Profile screen

Backend endpoints are in `backend/specs/021-user-account/contracts/account-api.md` and are not repeated here.

## Layout (top to bottom)

Top bar: a three-dot menu with "Editar nome" (→ name dialog) and "Trocar senha" (only when
`hasPassword == true`).

1. **Foto** — 140 dp circle. Tap → menu: "Trocar foto" (crop + upload, unchanged) and, when a local photo exists,
   "Remover foto". Progress over the circle while uploading or removing.
2. **Nome** — `titleLarge` centered below the photo; edited from the top bar menu. Not repeated elsewhere;
   the login (`username`) is not shown.
3. Screen-level error line ("Erro: …") as today.
4. **Footer** (pinned to the bottom of the screen)
   - "Excluir conta" — error color; disabled while `hasPassword` is unknown.
   - "No app desde {mês} de {ano}" (hidden when unknown) — last line.

"Membro: Ativo/Desativado" is gone.

## Dialogs

| Dialog | Fields | Confirm enabled when | Success |
|---|---|---|---|
| Remover foto | — ("Remover sua foto de perfil?") | always | toast "Foto removida" |
| Editar nome | Nome (max 100) | trimmed not blank, differs from current | toast "Nome atualizado" |
| Trocar senha | Senha atual, Nova senha, Confirmar nova senha (visibility toggles) | all non-blank | toast "Senha alterada" |
| Excluir conta | text explaining it is permanent; with password: Senha; always: "Digite EXCLUIR para confirmar" | `EXCLUIR` typed (trim, case-insensitive) and, with password, password non-blank | pop to home + toast "Conta excluída" |

The "Excluir conta" text: "Sua conta, sua foto e seus dados de acesso serão apagados agora e não podem ser
recuperados. O que você publicou na igreja continua, sem autor." Without a password the confirm button reads
"Confirmar com o Google".

While a request runs: buttons disabled, progress in the confirm button, dismiss blocked.

## Errors

- Field errors from the server go under their field (`name`, `current_password`, `new_password`,
  `new_password_confirm`, `password`); `INVALID_PASSWORD` goes under the password field of the open dialog.
- Anything else shows at the bottom of the dialog (server `detail` or the generic category text).
- Google picker cancelled → no message.

## Navigation

`ProfileScreen(onBackClick, onAccountDeleted)`. `AppNavHost`: `onAccountDeleted` → `navigate(CORE) { popUpTo(CORE)
{ inclusive = true } }` and `Toast "Conta excluída"`. The session cleanup is not done by the screen
(`AuthEventBus.Event.AccountDeleted` → `CoreViewModel`).
