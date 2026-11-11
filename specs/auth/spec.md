# Autenticação — Spec

Login com usuário e senha ou com Google, cadastro de conta e renovação da sessão. A sessão vive no `@AuthPrefs`
cifrado (ver `constitution.md`). Sem sessão o app continua utilizável; o login só destrava o que o backend exige
(escala, galeria, progresso na Bíblia, gestão conforme permissão).

Fonte da verdade no backend: `backend/specs/accounts/spec.md`. Endpoints e códigos de erro não são redefinidos aqui.

---

## 1. Endpoints

| Uso | Caminho |
|-----|---------|
| Login com usuário e senha | `auth/login/` |
| Login com Google (`id_token`) | `auth/google/` |
| Cadastro | `auth/register/` |
| Renovar tokens | `auth/refresh/` |
| Pedir código de recuperação | `auth/password-recovery/request/` |
| Trocar a senha com o código | `auth/password-recovery/confirm/` |

## 2. Fluxo

- Entradas: item **Entrar** do menu lateral, botão **Entrar** da faixa da Home e o ícone de conta da barra superior
  sem sessão. Todas abrem `authGraph` (`AuthRoutes.AUTH`).
- Sucesso emite `AuthEventBus.Event.LoginSuccess` e chama `onAuthSuccess()` (ver CLAUDE.md, Pitfalls #6).
- O Google usa o Credential Manager (`requestGoogleIdToken`) e manda o `id_token` ao backend. Cancelar a escolha de
  conta não mostra erro.

## 3. Tela de login

Layout "marca centrada": sem a barra superior do `BaseScreen`.

- **Faixa verde** (`ipbGreen`, fixa nos dois temas) que vai até a status bar. Botão voltar branco no canto
  superior esquerdo. Ao centro: sarça em branco, "IPB Castelo Branco" e o subtítulo **"Entre com sua conta."**.
  Uma sarça grande e quase transparente fica como marca d'água no canto inferior direito.
- O subtítulo é neutro de propósito: muito do que o login libera ainda depende de permissão, então a tela não
  promete funcionalidades.
- **Folha** com cantos superiores de 28dp sobe sobre a faixa, no fundo do tema. Ordem:
  1. **Continuar com Google** (botão em pílula, contorno, fundo `surfaceContainerLowest`).
  2. Divisor "ou com usuário e senha".
  3. Faixa de erro (quando houver).
  4. Campo **Usuário** (ícone de pessoa, ação de teclado "próximo").
  5. Campo **Senha** (ícone de cadeado, olho para mostrar/ocultar, ação de teclado "concluir" envia).
  6. **Esqueci minha senha** (link de texto, alinhado à direita), que abre a recuperação (§5).
  7. **Entrar** (botão em pílula, `primary`).
  8. "Ainda não tem conta? **Criar conta**", que abre o cadastro.
- Campos tonais: fundo `surfaceContainerHigh`, cantos de 16dp, sem linha inferior.
- A tela rola e respeita o teclado (`imePadding`).

### 3.1 Estados

| Estado | Comportamento |
|--------|---------------|
| Entrando com senha | Entrar mostra spinner e "Entrando…"; Entrar e Google ficam desabilitados |
| Conectando ao Google | Google mostra spinner e "Conectando…"; Entrar e Google ficam desabilitados |
| Campo vazio | Não chama o backend. Erro "Preencha usuário e senha." e os campos vazios ficam marcados |
| Falha do backend | Mensagem do backend (`parseLoginError`) na faixa de erro (`errorContainer`) |

A faixa de erro fica até a pessoa editar um campo ou tentar de novo; não some sozinha.

Voltando da recuperação, uma faixa em `primaryContainer` diz **"Senha alterada"**, com a mesma regra de saída.

## 4. Cadastro

Layout "tudo numa folha", da mesma família do login: sem a barra superior do `BaseScreen`.

- **Faixa verde** igual à do login, mais baixa: botão voltar, sarça menor (58dp), título **"Criar conta"** e o
  subtítulo "IPB Castelo Branco". Mesma marca d'água.
- **Folha** sobre a faixa (cantos de 28dp, fundo do tema). Ordem:
  1. **Criar com Google** (mesmo botão do login). Chama o mesmo `auth/google/`: o backend cria a conta no primeiro
     login com Google, então não existe "cadastro com Google" separado. Sucesso entra no app como o login.
  2. Divisor "ou preencha seus dados".
  3. Faixa de erro (quando houver).
  4. **Nome** e **Sobrenome** lado a lado, sem ícone.
  5. **Usuário** (ícone de pessoa), **E-mail** (ícone de envelope, teclado de e-mail), **Senha** (cadeado, olho),
     **Confirmar senha** (cadeado). O olho da senha mostra as duas.
  6. **Criar conta** (pílula, `primary`). Ação "concluir" do teclado na confirmação também envia.
  7. "Já tem conta? **Entrar**", que volta ao login.
- Campos tonais do login. A tela rola e respeita o teclado.

### 4.1 Regras ao vivo

As regras repetem as do backend (`backend/server/features/accounts/validators.py` e `AUTH_PASSWORD_VALIDATORS`),
para a pessoa ver o problema antes de enviar. O backend continua sendo quem decide.

| Campo | Enquanto digita | Ao tocar em Criar conta |
|-------|-----------------|-------------------------|
| Nome, Sobrenome | Nada | Vazio: "Preencha este campo." |
| Usuário | Vira minúsculo e perde espaços. Ajuda: "Letras minúsculas, números, ponto, hífen ou _.". Fora da regra `^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$`: "Use só letras minúsculas, números, ponto, hífen ou _, começando e terminando com letra ou número." | Vazio: "Preencha este campo." |
| E-mail | "Usado só para recuperar sua senha." | Vazio: "Preencha este campo."; fora do formato: "Informe um e-mail válido." |
| Senha | "Mínimo de 6 caracteres."; com 6 ou mais vira "✓ Mínimo de 6 caracteres." em `primary` | Curta: "A senha precisa de pelo menos 6 caracteres." |
| Confirmar senha | Igual: "✓ As senhas coincidem." em `primary`. Diferente, quando já tem o tamanho da senha: "As senhas não coincidem." | Vazio: "Preencha este campo."; diferente: "As senhas não coincidem." |

Com algum campo errado, Criar conta não chama o backend e a faixa de erro diz "Confira os campos marcados.".

### 4.2 Estados

| Estado | Comportamento |
|--------|---------------|
| Criando conta | Criar conta mostra spinner e "Criando conta…"; Criar conta e Google ficam desabilitados; segundo toque não envia de novo |
| Conectando ao Google | Google mostra spinner e "Conectando…"; os dois botões ficam desabilitados |
| Erro do backend por campo | Mensagem (`parseRegisterError`) sob o campo, no lugar da ajuda |
| Erro do backend sem campo, ou do Google | Na faixa de erro |
| Sucesso | `onAuthSuccess()`, como o login |

Erros não somem sozinhos: o de um campo sai quando a pessoa edita aquele campo; a faixa sai quando ela edita
qualquer campo.

O estado do formulário vive no `AuthViewModel` (`RegisterFormState`); a ajuda de cada campo é calculada por funções
puras sobre esse estado.

O e-mail é digitado uma vez, não pede código e não é único: "Criar com Google" não muda, e o app nunca diz "e-mail já
em uso". Um engano se corrige no Perfil (`specs/profile/spec.md` §3.2.3).

Os erros de campo chegam por `AppError.Server.fieldErrors` (`registerErrorsOf`): o corpo da resposta já foi lido em
`ResponseExt.kt`, então `parseRegisterError` só cobre o caso sem campos.

## 5. Recuperação de senha

Rota `AuthRoutes.PASSWORD_RECOVERY`, com o layout do cadastro (faixa baixa e folha) e `PasswordRecoveryViewModel` no
escopo da própria rota: sair e voltar começa do zero. O e-mail serve apenas para recuperar, nunca para entrar, e nenhuma
tela o apresenta como login. A API é pública e usa `@AuthLessRetrofit`: sessão vencida não atrapalha.

**Passo 1, e-mail.** Só o campo E-mail e "Enviar código", com o aviso **"Se você entra pelo Google, use o botão do
Google."**. Qualquer endereço bem formado recebe a mesma resposta, **"Se houver conta com esse e-mail, enviamos um
código."**, e abre o passo 2: a tela nunca revela se a conta existe.

**Passo 2, código.** Repete a resposta e explica: **"O e-mail mostra o nome de usuário. Se houver mais de uma conta nesse
e-mail, cada uma tem seu código: digite o da conta que quer recuperar."** Campos Código (só dígitos, até 6), Nova senha e
Confirmar nova senha, com o olho e as regras do cadastro; botão "Trocar senha". "Reenviar código" fica desligado por 60 s,
o limite do servidor por endereço. Voltar leva ao passo 1 mantendo o e-mail.

| Situação | Comportamento |
|----------|---------------|
| Campo vazio ou fora do formato | Não chama o backend; erro sob o campo |
| Código errado, vencido ou já usado | Faixa com "Código inválido ou expirado."; o que foi digitado fica |
| Senha fora das regras | Erro sob o campo; o código não é gasto |
| Sem rede ou limite de requisições | Faixa com a mensagem da categoria; o que foi digitado fica; o botão tenta de novo |
| Sucesso | Volta ao login com "Senha alterada"; a pessoa entra normalmente |

## 6. Celular deitado

Regras gerais em `specs/core/spec.md` §6.9. Vale para login, cadastro e recuperação de senha (`AuthLayout`).

- A faixa verde vira uma coluna de 300dp à esquerda: voltar no canto, sarça de 56dp, título e subtítulo no centro.
- O formulário fica à direita, rolando, com no máximo 440dp de largura e centralizado. O primeiro campo aparece sem
  rolar, também com o teclado aberto.
