# Constituição

> **Documento incompleto.** Por enquanto cobre apenas tratamento de erro. As demais regras globais
> (autenticação, arquitetura, segurança) ainda precisam ser escritas.

## Tratamento de erro

Regras que nenhum domínio pode quebrar.

### Um único tipo de erro

`AppError` (`core/domain/error/AppError.kt`) é a única hierarquia de erro que cruza a fronteira
data → domain → presentation. Nenhum `Throwable` cru — `IOException`, `HttpException`,
`IllegalStateException` — chega ao ViewModel.

Repositórios convertem qualquer `Throwable` em `AppError` antes de emitir, seja por
`Throwable.toAppError()`, por `Result.mapError()` ou construindo a subclasse diretamente. A
conversão acontece na camada de dados; a presentation nunca converte, apenas consome.

`SnapshotState.Error` carrega `AppError`, não `Throwable` — a garantia é verificada pelo compilador,
não por convenção.

### `message` é técnico, `userMessage` é para a tela

`AppError.message` é técnico e destina-se a log. Pode conter corpo de resposta do servidor: JSON do
Django, página HTML de erro, stack trace. **Nunca é exibido ao usuário.**

`AppError.userMessage` é texto autoral do app, escrito para ser lido na tela. `null` significa "use
o texto genérico da categoria".

Conteúdo vindo do servidor só entra em `userMessage` quando vem do campo `detail` de uma resposta de
erro estruturada — isto é, uma resposta que carrega `error_code`. Nesse caso o `detail` é copy que a
API assume como voltada ao usuário (ex.: "Já existe cifra para esta música"). O fallback de body cru
nunca vira `userMessage`.

### A presentation decide o texto exibido

O texto que aparece na tela é decidido em `core/presentation/error/AppErrorMessages.kt`, via
`AppError.toUserMessage()`: usa `userMessage` quando existe, senão devolve um genérico por
categoria (rede, autenticação, servidor, desconhecido).

Dentro de `AppError.Auth`, o genérico depende do código: 401 é sessão ("Faça login para continuar.") e 403 é
permissão ("Você não tem permissão para esta ação."). Uma recusa de permissão nunca aparece como problema de login.

Não há variação de texto genérico por tela. Uma tela que precise de texto próprio para um erro deve
fazer o repositório preencher `userMessage`, não escrever o texto no ViewModel.

Strings ficam hardcoded em português no Kotlin — sem `strings.xml`, sem `UiText`, conforme o
`CLAUDE.md`.

### Um único ponto de parsing de erro HTTP

`core/network/error/ResponseExt.kt` é o único lugar do projeto que lê `errorBody()` e transforma uma
resposta HTTP com erro em `AppError`. Nenhuma outra camada reimplementa essa lógica.

Mapeamento de status: 401 e 403 viram `AppError.Auth(code)`; os demais viram
`AppError.Server(code)`.

Única exceção de leitura: `core/network/PermissionDeniedInterceptor.kt` espia o corpo de toda resposta 403 do
client autenticado, com o mesmo `parseApiError`, só para saber se `error_code` é `PERMISSION_DENIED` e disparar a
atualização do perfil. Ele não produz `AppError` nem texto, e não altera a resposta.
