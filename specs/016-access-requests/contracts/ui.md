# UI Contract: Access Requests

## Locked screens (gallery, schedule, birthdays)

On a 403 (signed in), instead of `PermissionErrorPlaceholder(showLoginButton = false)`:

```
Disponível apenas para membros.          ← the screen's own message (server detail)
[ Pedir acesso ]                          ← CanRequest
Pedido enviado. Aguardando a liderança.   ← Pending (no button)
Pedidos de acesso indisponíveis. Fale com a liderança.   ← Blocked (no button)
```

401 / signed out: unchanged ("Conectar à sua conta").

## Request dialogs

| Dialog | Title | Field | Confirm enabled | Success |
|---|---|---|---|---|
| Pedir acesso | "Pedir acesso" | "Mensagem para a liderança (opcional)", max 1000 | always | toast "Pedido enviado" |
| Solicitar correção | "Solicitar correção" | "O que precisa ser corrigido?", max 1000 | trimmed not blank | toast "Correção solicitada" |

While sending: buttons disabled, dismiss blocked. A refusal shows the message at the bottom of the dialog.

## Profile

Under the name:

- Card "Acesso": "Liberado" / "Aguardando" / "Não liberado". With `CanRequest`: "Pedir acesso". Blocked: blocked
  text. `UNKNOWN`: card hidden.
- "Meus dados na igreja" (only when linked and loaded): Cargo, Ministérios (comma list), Aniversário ("14 de março"),
  Batismo ("20/05/2001"), Gênero. Empty lines hidden. Button "Solicitar correção" / disabled "Correção solicitada" /
  nothing when blocked. Load error: "Não foi possível carregar seus dados." + "Tentar novamente".

## Gestão → Perfis

- Panel card "Perfis" (icon: account/manage), after "Membros".
- List screen "Perfis": tabs "Pendentes" / "Todos"; rows: avatar, name, "Pedido de acesso" / "Pedido de correção"
  chip when pending, "Bloqueado" chip when blocked. Pull to refresh. Empty: "Nenhum pedido pendente." / "Nenhuma
  conta.". Tap → detail.
- Detail screen (title = name): avatar, name, "@login", "No app desde março de 2025".
  - Acesso: "Liberado" / "Não liberado"; "Acesso pelo papel" note when `accessViaRole`.
  - Rol: member name or "Sem vínculo".
  - Pendentes: per request — type, message, date; buttons "Aprovar" / "Negar" (manage).
  - Histórico: type, status ("Aprovado" / "Resolvida" / "Negado"), by whom, when, reason.
  - Manage actions: "Revogar acesso" (only when stored flag on; confirm "Revogar o acesso de {nome}?"; with
    `accessViaRole` the confirm text adds "O papel continua liberando o acesso."), switch "Bloquear pedidos",
    "Vincular ao rol" / "Trocar vínculo" (picker dialog with search over Membros), "Remover vínculo" (confirm).
  - Deny dialog: "Negar pedido", field "Motivo (opcional)", max 500.
- Errors: server message as toast; the detail reloads after a 409.

## Notifications (channel "Pedidos de acesso")

| Push | Title | Body | Tap |
|---|---|---|---|
| created, access | Novo pedido de acesso | Toque para ver os pedidos pendentes. | Perfis › Pendentes |
| created, correction | Novo pedido de correção | Toque para ver os pedidos pendentes. | Perfis › Pendentes |
| approved, access | Acesso liberado | Agora você pode ver o conteúdo dos membros. | Perfil |
| approved, correction | Correção resolvida | A liderança resolveu seu pedido de correção. | Perfil |
| denied, access | Pedido de acesso negado | reason or "Fale com a liderança para saber mais." | Perfil |
| denied, correction | Pedido de correção negado | reason or "Fale com a liderança para saber mais." | Perfil |
