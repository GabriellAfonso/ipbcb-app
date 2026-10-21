# UI Contract: Profile screen

Backend endpoints are in `backend/specs/021-user-account/contracts/account-api.md` and are not repeated here.

## Layout (top to bottom)

1. **Foto** — 140 dp circle. Tap → menu: "Trocar foto" (crop + upload, unchanged) and, when a local photo exists,
   "Remover foto". Progress over the circle while uploading or removing. Below it: the name (`titleLarge`).
2. **Conta** (section title)
   - "Nome" — value + pencil action → name dialog.
   - "Login" — `username` (hidden when unknown).
   - "No app desde {mês} de {ano}" (hidden when unknown).
3. **Ações** (section title)
   - "Trocar senha" — only when `hasPassword == true`.
   - "Excluir conta" — error color; disabled while `hasPassword` is unknown.
4. Screen-level error line ("Erro: …") as today.

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
