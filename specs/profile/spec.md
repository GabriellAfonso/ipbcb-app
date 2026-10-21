# Perfil e conta — Spec

A tela **Perfil** é a conta de quem está logado: foto, nome, login, desde quando usa o app, troca de senha e
exclusão da conta. O perfil (`/me`) também alimenta o resto do app: os papéis e níveis de acesso (painel de gestão),
o vínculo com o rol (`member_id`) e as flags do Louvor. A tela só existe com sessão; sem sessão, o ícone do topo leva
ao login.

Fonte da verdade no backend: `backend/specs/accounts/spec.md` e
`backend/specs/021-user-account/contracts/account-api.md`. Endpoints, códigos de erro e a ordem das verificações não
são redefinidos aqui.

---

## 1. Modelo (`MeProfile`)

| Campo | Origem | Observação |
|-------|--------|------------|
| Nome | `name` | editável |
| Membro | `is_member` | não aparece mais na tela (o próximo passo é um card de acesso) |
| Foto | `photo_url` | mídia protegida (JWT) |
| Acesso | `roles`, `permissions` | ver `specs/006-role-scoped-permissions/` |
| Membro vinculado | `member_id` | `null` sem vínculo |
| Louvor | `is_worship_member`, `can_save_setlist` | ver `specs/011-sunday-setlist-push/` |
| Login | `username` | só leitura; `null` = desconhecido |
| Tem senha | `has_password` | só leitura; `null` = desconhecido |
| No app desde | `date_joined` (`AAAA-MM-DD`) | só leitura; data inválida ou ausente = desconhecida |

Todo campo acrescentado ao DTO tem default. Um perfil guardado em disco por uma versão anterior precisa continuar
abrindo: uma falha de decodificação apaga o arquivo e perde o perfil inteiro. Os três campos da conta são anuláveis
de propósito. Um `false` padrão em `has_password` mandaria uma conta com senha para a confirmação pelo Google.

## 2. Cache

- Snapshot `JsonSnapshotStorage` com a chave `me_profile`, carregado no boot (`Preloadable`), porque o acesso decide
  se a home mostra a gestão. É atualizado no boot, no login, num 403 `PERMISSION_DENIED` e depois de toda alteração
  feita nesta tela. Usa ETag.
- Foto: arquivo `profile_photo.*` em `filesDir/profile`, com ETag e a última URL. 404/403 limpam a cópia local.
- O logout e a exclusão da conta apagam os dois.

## 3. Tela

Três seções, de cima para baixo.

### 3.1 Foto

- Círculo de 140 dp. A foto local, ou o placeholder.
- Tocar abre um menu com **"Trocar foto"** (recorte quadrado + envio) e, só quando há foto, **"Remover foto"**.
- "Remover foto" pede confirmação. Depois apaga no servidor, apaga a cópia local, mostra o placeholder e atualiza o
  perfil. Aviso: "Foto removida".
- Enquanto envia ou remove, há um progresso sobre o círculo e o toque fica desligado.
- O nome aparece logo abaixo.

### 3.2 Conta

- **Nome**, com ação de editar. Diálogo com um campo de até 100 caracteres. O nome é aparado (trim); vazio não
  salva; o erro do servidor aparece no campo. Aviso: "Nome atualizado".
- **Login**: o `username`. Escondido enquanto desconhecido.
- **"No app desde {mês} de {ano}"**, com o mês por extenso em minúsculas ("março de 2025"). Escondido enquanto
  desconhecido.
- O e-mail não aparece e não é editável.

### 3.3 Ações

- **"Trocar senha"** — só quando a conta tem senha. Pede a senha atual, a nova e a confirmação. Se der certo, o app
  grava o novo par de tokens da resposta antes de qualquer outra chamada e continua logado; os outros aparelhos caem
  para deslogado. Aviso: "Senha alterada".
- **"Excluir conta"** — confirmação forte:
  - texto explicando que é permanente e o que some;
  - conta com senha: senha atual + digitar **EXCLUIR**;
  - conta sem senha: digitar **EXCLUIR** e confirmar com um login Google novo, no mesmo seletor do login. Cancelar
    o seletor só fecha o seletor, sem mensagem;
  - fica desligado enquanto não se sabe se a conta tem senha;
  - depois do sucesso: a sessão e todos os caches da sessão são limpos exatamente como no logout, mas sem chamar o
    servidor (logout e cancelamento do registro do push já foram feitos lá). O app volta para a home deslogada e
    mostra "Conta excluída".

### 3.4 Erros

- Erro de campo do servidor aparece no campo correspondente. "Senha incorreta." aparece no campo de senha do
  diálogo aberto.
- Os demais erros aparecem no rodapé do diálogo, com o `detail` do servidor ou o texto genérico da categoria:
  conta Google diferente, exclusão bloqueada ("Fale com a liderança da igreja"), bloqueio por tentativas, limite de
  requisições, rede.
- Nenhum desses erros desloga: senha errada é erro de formulário (400) e a conta Google diferente é 403 de
  confirmação, não de sessão.
- Enquanto uma requisição roda, os botões do diálogo ficam desligados e ele não fecha por toque fora.

## 4. Sessão

- **Troca de senha e renovação concorrente**: a troca mata o access usado e todos os refresh. A renovação do token
  (`TokenAuthenticator`) só apaga ou grava o par guardado se o refresh guardado ainda for o que ela usou (atômico).
  Se outro par foi gravado no meio tempo, ela refaz a requisição com ele. Assim uma requisição que falhou com o
  token antigo não desloga quem acabou de trocar a senha, e um par recebido de uma renovação já revogada não
  sobrescreve o par bom.
- **Exclusão**: o use case emite `AuthEventBus.Event.AccountDeleted`. O `CoreViewModel`, no escopo da Activity,
  roda o mesmo corpo de limpeza do logout: foto, perfil, escala, aniversariantes, galeria, `SessionScopedCache`,
  tokens. A única diferença é que não chama o servidor para cancelar o registro do aparelho; só esquece o registro
  local.
