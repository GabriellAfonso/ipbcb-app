# Core — Spec

O Core e o modulo fundacional do app. Fornece infraestrutura compartilhada por todas as features: injecao de dependencias, rede, cache offline, navegacao, tema, componentes UI reutilizaveis, e orquestracao de startup. Nenhuma feature importa outra feature — tudo passa pelo core.

---

## 1. Arquitetura

Single Activity (`CoreActivity`) com UI 100% Compose. Fluxo padrao:

```
UI -> ViewModel -> UseCase -> Repository (interface) -> Repository (impl)
```

### 1.1 Pacotes

```
core/
  data/           -- DataStore, snapshot storage, preferences, connectivity
  di/             -- Hilt modules, qualifiers
  domain/         -- Interfaces, use cases, erros, AuthEventBus
  network/        -- AuthInterceptor, TokenAuthenticator, constantes
  presentation/
    CoreActivity.kt
    viewmodel/CoreViewModel.kt
    screens/CoreScreen.kt
    base/BaseScreen.kt
    navigation/    -- AppRoutes, AppNavHost, LocalAppNavigator, AppNavExtensions
    theme/         -- Color, Theme, Type
    components/    -- TopBar, CustomButton, Highlight, etc.
    modifiers/     -- PagerModifiers
```

### 1.2 Regras

- Features nunca importam umas as outras — compartilham via `core/`.
- `domain/` nao tem conhecimento Android (sem `Context`, `ViewModel`, Compose).
- `presentation/` so conhece ViewModel — nunca acessa repo/use case.
- Erros sao sealed classes ou `Result<T>` — excecoes HTTP cruas nunca chegam ao ViewModel.

---

## 2. Injecao de Dependencias (DI)

### 2.1 Qualifiers

| Qualifier | Tipo | Uso |
|-----------|------|-----|
| `@AuthedRetrofit` | Retrofit | APIs protegidas (com auth) |
| `@AuthLessRetrofit` | Retrofit | APIs publicas (sem auth) |
| `@Client` | OkHttpClient | Client autenticado (interceptor + authenticator) |
| `@AuthLessClient` | OkHttpClient | Client sem auth |
| `@ApiBaseUrl` | String | URL base da API |
| `@AuthPrefs` | DataStore | Sessao (tokens), gravada criptografada — secao 5.5.1 |
| `@SessionCipher` | AeadCipher | Chave do Keystore que sela `@AuthPrefs` |
| `@SettingsPrefs` | DataStore | Preferencias de tema/fonte/scroll |
| `@SetlistPrefs` | DataStore | Pins de musicas do dia |

Qualifier errado em API protegida resulta em 401 silencioso.

### 2.2 Modules

| Module | Scope | Responsabilidade |
|--------|-------|-----------------|
| `HttpClientModule` | Singleton | OkHttpClient (auth e authless), logging interceptor |
| `RetrofitModule` | Singleton | Retrofit instances, base URL |
| `SerializationModule` | Singleton | kotlinx.serialization.Json configurado |
| `DataStoreModule` | Singleton | DataStores (auth criptografado, settings, setlist, push) + `@SessionCipher` |
| `SnapshotCoreModule` | Singleton | Logger, SnapshotStorage, SnapshotCacheFactory |
| `AuthCoreModule` | Singleton | Bind AuthEventBus -> AuthEventBusImpl |
| `StartupBindingsModule` | Singleton | Multibinding sets de Preloadable e Refreshable |

### 2.3 Configuracao HTTP

- Timeout: 30s (connect, read, write)
- Logging: BASIC em debug, NONE em release
- JSON: `ignoreUnknownKeys`, `encodeDefaults`, `isLenient`, `explicitNulls = false`

---

## 3. Rede (Network)

### 3.1 AuthInterceptor

Adiciona header `Authorization: Bearer {access_token}` em todas as requests do `@Client`.

- Respeita headers Authorization pre-existentes (nao sobrescreve).
- Se token nulo ou vazio, nao adiciona header.
- Dependencia: `TokenStorage` (feature auth).

### 3.2 TokenAuthenticator

Trata respostas 401 tentando refresh de token.

- Usa `Mutex` para thread-safety (apenas 1 refresh simultaneo).
- Maximo 2 tentativas por cadeia de responses (previne loop infinito).
- Fluxo:
  1. Recebe 401
  2. Lock mutex, chama `authApi.refresh(RefreshRequest(refresh))`
  3. Sucesso: salva novos tokens, retenta request original
  4. Falha (401/400 no refresh): limpa tokens (logout implicito)
- Dependencias: `AuthApi`, `TokenStorage` (feature auth).

### 3.3 Constantes

- `ApiConstants.BASE_PATH = "api/"`

---

## 4. Domain

### 4.1 Erros — AppError

Hierarquia sealed para erros de dominio:

| Tipo | Quando |
|------|--------|
| `AppError.Network` | Sem rede, DNS, timeout |
| `AppError.Auth` | HTTP 401/403 |
| `AppError.Server(code)` | 4xx/5xx exceto 401/403 |
| `AppError.Unknown` | Erros inesperados |

Extensions:
- `Throwable.toAppError()` — converte qualquer excecao para AppError
- `Result<T>.mapError()` — mapeia failure dentro de Result para AppError

### 4.2 AuthEventBus

Barramento de eventos de autenticacao (SharedFlow).

```kotlin
interface AuthEventBus {
    val events: SharedFlow<Event>
    fun emit(event: Event)

    sealed class Event {
        object LoginSuccess : Event()
        object PermissionDenied : Event()  // um endpoint com escopo recusou o usuario (403)
    }
}
```

Implementacao (`AuthEventBusImpl`): `MutableSharedFlow(extraBufferCapacity = 1)`, usa `tryEmit()` — eventos
repetidos em rajada se fundem.

### 4.2.1 Acesso — papeis e niveis por escopo

O backend (feature 012) substituiu `is_admin` por **papeis** que dao um **nivel por escopo**. O app so esconde o
que a pessoa nao pode usar; quem autoriza e sempre o backend. Spec da adequacao:
[`specs/006-role-scoped-permissions/`](../006-role-scoped-permissions/spec.md).

Vocabulario em `core/domain/access/` (sem Android):

| Tipo | Conteudo |
|------|----------|
| `AccessLevel` | `VIEW` < `MANAGE` < `OWNER` (ordem do enum = hierarquia) |
| `Scope` | `MEMBERS`, `SCHEDULE`, `SONGS`, `GALLERY`, `EVENTS`, `NOTICES`, `HYMNAL_HISTORY_REPORT` (`reports.hymnal_history`), `PROFILES`, `BIBLE_VIDEOS` (`bible_videos`, gestão dos vídeos da Bíblia — `specs/bible/spec.md` §6), `SUNDAY_SCHOOL` (`sunday_school`, gestão das aulas da EBD — `specs/studies/spec.md` §5) |
| `Role` | `ADMIN`, `LEADER`, `MEDIA` |
| `Access` | `roles`, `hasAnyRole`, `levels`; `allows(scope, minimo)`, `holds(role)`; `Access.NONE` |
| `AccessRepository` | `access: Flow<Access>`, `refresh()` |

- **Negar na duvida:** nivel, escopo ou papel desconhecido vale como sem acesso. `hasAnyRole` conta todo papel da
  lista, conhecido ou nao — e o que decide a entrada do painel.
- `Access.NONE` enquanto o perfil carrega, falha, ou depois do logout.
- A implementacao (`ProfileAccessRepository`) vive em `features/profile`: o acesso sai do mesmo snapshot do `/me`,
  entao ja vale desde o boot. Painel, admin e worshiphub usam so `ObserveAccessUseCase` / `RefreshAccessUseCase`.
- **Perfil desatualizado:** `PermissionDeniedInterceptor` (no `@Client`) ve todo 403 com
  `error_code: "PERMISSION_DENIED"` e emite `AuthEventBus.Event.PermissionDenied`; o `CoreViewModel` chama
  `RefreshAccessUseCase`, que refaz o `/me` com uma unica requisicao em voo por vez. As telas recalculam o que
  mostram.

### 4.2.1.1 Acesso de membro e pedidos de acesso

`core/domain/access/MemberAccess.kt` (spec 016): `MemberAccess(hasAccess, status, requestsBlocked,
correctionPending)`, com `AccessStatus` `GRANTED` / `PENDING` / `NONE` / `UNKNOWN` (`access_status` do `/me`; ausente
= `UNKNOWN`) e `AccessRequestType` `ACCESS` / `DATA_CORRECTION`. `hasAccess` e o `member_access` do `/me` — o acesso
efetivo (marcacao **ou** papel Admin/Lideranca), com fallback para `is_member` de servidor antigo.

- `MemberAccessRepository` (`memberAccess: Flow<MemberAccess>`, `request(type, message)`) e implementado em
  `features/profile` (`ProfileMemberAccessRepository`) pelo snapshot do `/me`. `request` faz
  `POST api/me/access-requests/` e, com sucesso ou recusa, relê o `/me` (uma recusa quase sempre quer dizer que o
  estado mudou: bloqueado, ja pendente, ja liberado).
- `accessPromptFor(MemberAccess)`: bloqueado → `Blocked`; `PENDING` → `Pending`; `NONE` → `CanRequest`;
  `GRANTED`/`UNKNOWN` → `None`. `UNKNOWN` nunca oferece pedido (servidor antigo nao aceitaria).
- **Telas so para membros:** galeria, escala e aniversariantes trocam o ramo 403 (logado) por
  `MembersOnlyPlaceholder` (`core/presentation/components/access/`): a mensagem da tela e, conforme o prompt,
  "Pedir acesso" (dialogo com mensagem opcional de ate 1000 caracteres), "Pedido enviado. Aguardando a lideranca."
  ou "Pedidos de acesso indisponiveis. Fale com a lideranca.". Recusa do servidor aparece no dialogo com o `detail`;
  nenhuma desloga. 401/deslogado continua com "Conectar a sua conta".

### 4.2.2 Membro do usuario

`core/domain/member/`: `CurrentMemberRepository` (`memberId: Flow<Long?>`) e `ObserveOwnMemberIdUseCase` — o registro
de membro que um admin ligou ao usuario (`member_id` do `/me`), `null` sem vinculo ou sem perfil. Implementado em
`features/profile` (`ProfileCurrentMemberRepository`) pelo mesmo snapshot do `/me`, entao segue toda releitura. Fica
fora do `Access`: nao e permissao. Hoje so a galeria usa ("Minhas fotos", `specs/gallery/spec.md` §10).

### 4.2.3 Ministerio de Louvor

`core/domain/worship/`: `WorshipAccess(isWorshipMember, canSaveSetlist)` (`NONE` sem perfil), `WorshipAccessRepository`
e `ObserveWorshipAccessUseCase` — as flags `is_worship_member` e `can_save_setlist` do `/me` (spec 011). Implementado
em `features/profile` (`ProfileWorshipAccessRepository`) pelo mesmo snapshot do `/me`. Nao e nivel de escopo (vem do
ministerio do membro), por isso fica fora do `Access`. `current()` carrega o perfil do disco quando ainda nao esta em
memoria: um processo aberto por um push ou por um worker nunca passou pelo preload do boot.

### 4.2.4 Repertorio de domingo

`core/domain/setlist/` (spec 011): `SundaySetlist`/`SetlistItem`, `SundaySetlistRepository` (copia do repertorio
atual no aparelho), `setlistDateFor(today)` (hoje se domingo, senao o proximo domingo),
`ObserveSundaySetlistUseCase` (o guardado ate o fim do domingo dele, `null` depois) e `SyncSundaySetlistUseCase`
(sem sessao ou fora do Louvor limpa; senao `GET api/setlists/current/`). A implementacao
(`core/data/setlist/SundaySetlistRepositoryImpl`) guarda o JSON do servidor em `JsonSnapshotStorage` (chave
`sunday_setlist`), sem ETag (o endpoint e `no-store`); e `Preloadable` (offline desde o boot) e
`SessionScopedCache` (some no logout). `{"setlist": null}` e 403 limpam; outra falha mantem o guardado. Escrita
tambem pelo salvar do repertorio (worshiphub §2.4). `SetlistDto` e o mapper sao compartilhados com worshiphub e
admin.

`SetlistConfirmationRepository` (`core/data/setlist/SetlistConfirmationRepositoryImpl` + `SetlistAdminApi`, nunca
guardado no aparelho) e o lado de quem registra as musicas tocadas (`songs` ≥ `manage`): `GET api/setlists/{date}/`
(404 = sem repertorio), `GET api/setlists/pending-confirmation/` e `DELETE api/setlists/{date}/`. Use cases:
`GetSetlistForDateUseCase` (pre-preenche "Registrar domingo", worshiphub §2.6), `GetPendingConfirmationsUseCase`
(datas distintas, mais recente primeiro) e `DeletePendingSetlistUseCase` (404 conta como removido) — os dois
ultimos para o card de pendentes do painel (admin §2.1). Fica no core porque admin e worshiphub usam.

Sincroniza: no boot (depois do refresh do perfil, sem segurar o fim do boot), em `onAppForeground()` e no push
`setlist_saved` (secao 4.2.5). Quando `isWorshipMember` passa de `true` para `false`, limpa.

### 4.2.5 Push (FCM)

`firebase-messaging` (BOM do Firebase). Mensagens so de dados (`type` + `date`,
`backend/specs/017-sunday-setlist-push/contracts/push-messages.md`).

- **Token:** `RegisterDeviceUseCase` (`POST api/me/devices/`) roda em `PushTokenSyncWorker` (unico
  `push_token_sync`, `REPLACE`, com rede, backoff exponencial), agendado por `PushRegistrationScheduler` em todo
  boot logado (pega quem logou antes desta versao), no `LoginSuccess` e no `onNewToken`. Sem sessao ou sem token
  nao faz nada. O token aceito fica em `@PushPrefs` (`registered_push_token`).
- **Logout:** `UnregisterDeviceUseCase` e a primeira coisa do `logout()`: cancela o registro pendente, chama
  `POST api/me/devices/unregister/` com o token guardado (limite de 5 s) e apaga o token guardado. Qualquer falha
  e engolida — nunca impede o logout.
- **Recebimento:** `IpbMessagingService` (`@AndroidEntryPoint`, a unica excecao alem do `CoreActivity`) so repassa:
  `onNewToken` agenda o registro, `onMessageReceived` chama `PushMessageHandler`. O handler descarta tipo
  desconhecido ou data invalida (`PushMessage.parse`) e tudo quando nao ha sessao (o token de uma sessao que caiu
  por refresh falho nunca foi removido). `setlist_saved` (so membro do Louvor): agenda `SetlistRefreshWorker` e,
  fora do primeiro plano (`AppForegroundState`), notifica "Repertorio de domingo dd/MM disponivel".
  `confirm_plays`: notifica "Confirmar musicas de domingo" sempre; o acesso e decidido no toque.
  `access_request_created` / `_approved` / `_denied` (spec 016, `request_type` `access` | `data_correction`,
  `reason` opcional): notifica sempre; aprovado e negado tambem releem o `/me` (`RefreshAccessUseCase`).
- **Notificacoes:** `SetlistNotifications`, canal `sunday_setlist` ("Repertorio"), id fixo por tipo (a nova
  substitui a anterior), so com permissao (Android 13+ ja pedida no launch). O toque abre o `CoreActivity` com
  `NotificationTarget` nos extras (secao 6.1). Pedidos de acesso: `AccessRequestNotifications`, canal
  `access_requests` ("Pedidos de acesso"), um id por tipo; o toque do lider abre Gestao > Perfis (Pendentes)
  quando ele pode ver `profiles` (senao um aviso), e o de quem pediu abre o Perfil. Textos em
  `specs/016-access-requests/contracts/ui.md`.

### 4.3 Sistema de Snapshot Cache

Pattern para features offline. Tres camadas:

```
SnapshotFetcher<Dto>       -- busca dados da rede
SnapshotCache<Dto>         -- persiste/recupera do disco
BaseSnapshotRepository     -- orquestra cache + fetch + estado
```

#### SnapshotFetcher

```kotlin
interface SnapshotFetcher<Dto> {
    suspend fun fetch(etag: String?): NetworkResult<Dto>
}
```

#### NetworkResult

| Tipo | Descricao |
|------|-----------|
| `Success<T>(body, etag?)` | Dados novos recebidos |
| `NotModified` | HTTP 304, cache ainda valido |
| `Failure(throwable)` | Erro de rede/servidor |

#### SnapshotCache

```kotlin
interface SnapshotCache<Dto> {
    suspend fun load(): Dto?
    suspend fun exists(): Boolean   // padrao: load() != null; LocalSnapshotCache so olha o arquivo
    suspend fun save(dto: Dto, etag: String?)
    suspend fun loadETag(): String?
    suspend fun clear()
}
```

#### BaseSnapshotRepository

Classe abstrata que orquestra cache e fetch. Generics: `<Dto, Domain>`.

- Hot state: `MutableStateFlow<SnapshotState<Domain>>` (dados vivos enquanto app aberto).
- `observe()`: retorna StateFlow do estado atual.
- `preload()`: carrega cache do disco para `_state`.
- `refresh()`: busca da rede com suporte a ETag/304. Retorna `RefreshResult`.
- Fallback: se rede falha e cache existe, usa cache.
- HTTP 401/403: limpa cache (dados privilegiados revogados).

#### SnapshotState

| Tipo | Descricao |
|------|-----------|
| `Loading` | Carregando (estado inicial) |
| `Data<T>(value)` | Dados disponiveis |
| `Error(throwable)` | Erro |

#### RefreshResult

| Tipo | Descricao |
|------|-----------|
| `Updated` | Dados novos salvos |
| `NotModified` | Servidor retornou 304 |
| `CacheUsed` | Rede falhou, cache antigo usado |
| `Error(throwable)` | Falha sem cache fallback |

### 4.4 Startup — Preloadable / Refreshable

```kotlin
fun interface Preloadable { suspend fun preload() }
fun interface Refreshable { suspend fun refresh() }
```

Features registram implementacoes via Hilt multibinding (`StartupBindingsModule`).

#### PreloadDataUseCase

Orquestra inicializacao do app em 2 fases:
1. `preloadCachesFromDisk()` — roda todos Preloadables em paralelo (SupervisorScope)
2. `refreshDataFromNetwork()` — roda todos Refreshables em paralelo (SupervisorScope)

#### Onde o boot e disparado

`CoreViewModel.initialize()` e chamado no `AppNavHost`, **fora** do `NavHost`, com o ViewModel no
escopo da Activity. Isso e obrigatorio: o hot state de cada `BaseSnapshotRepository` nasce em
`Loading` a cada processo novo, e o Android pode recriar o processo restaurando a back stack
direto numa rota interna (ex.: lista de cifras). Nesse cenario a `CoreView` nunca e composta —
se o trigger morasse nela, nenhum `preload()` rodaria e todas as listas ficariam vazias ate o
usuario voltar para a home.

`initialize()` e idempotente (guarda `initialized`), entao a chamada redundante da `CoreView` e
inofensiva.

### 4.4.1 Caches da sessao — SessionScopedCache

```kotlin
fun interface SessionScopedCache { suspend fun clear() }
```

Dado que so vale para a sessao atual, em memoria ou em disco (ex.: fichas de membros vistas por um
lider; o repertorio de domingo guardado, secao 4.2.4). Features registram implementacoes via `@IntoSet`; o set vazio e declarado em
`core/di/SessionModule.kt` (`@Multibinds`).

`CoreViewModel` chama `clear()` de todos:
- em `logout()`, antes de `logoutUseCase()`;
- quando `isLoggedInFlow` passa de `true` para `false` (sessao encerrada pelo `TokenAuthenticator`
  depois de um refresh que falhou — nesse caminho `logout()` nao e chamado);
- quando `SessionRecovery.pendingWipe` fica `true` (sessao criptografada ilegivel, secao 5.5.1) — mesmo num boot
  que ja comeca deslogado, onde a transicao acima nao acontece.

Existe para que `core/` limpe dado de uma feature sem importa-la, do mesmo jeito que
`Preloadable`/`Refreshable` disparam o boot.

### 4.5 SnapshotRepository (interface)

```kotlin
interface SnapshotRepository<T> {
    fun observe(): Flow<T?>
    suspend fun refresh(): Boolean
}
```

### 4.6 Utilidades

- `String.normalize()` — remove acentos e pontuacao (NFD + regex). Usado em buscas accent-insensitive.
- `DownloadProgress(downloaded, total)` — data class com `percentage` calculado.

---

## 5. Data

### 5.1 Snapshot Storage

`JsonSnapshotStorage` — persistencia em arquivos JSON no filesDir.

```kotlin
interface SnapshotStorage {
    suspend fun save(key: String, json: String)
    suspend fun loadOrNull(key: String): String?
    suspend fun exists(key: String): Boolean
    suspend fun clear(key: String)
    suspend fun clearAll()
    suspend fun loadETagOrNull(key: String): String?
    suspend fun saveETag(key: String, etag: String)
}
```

- Diretorio: `context.filesDir / "snapshots"`
- Nomes: `{safe_key}.json` e `{safe_key}_etag.txt`
- Sanitizacao: lowercase, apenas alfanumerico + underscore/hyphen
- I/O em `Dispatchers.IO`
- `exists(key)` checa so o arquivo `.json`: para saber se um snapshot grande existe (ex.: Biblia), nunca
  ler e decodificar o conteudo.

### 5.2 Codecs

```kotlin
interface SnapshotCodec<T> {
    fun encode(value: T): String
    fun decode(raw: String): T
}
```

`JsonSnapshotCodec<T>` — usa kotlinx.serialization. Detecta snapshots corrompidos (ex: OOM durante escrita), deleta e retorna null.

### 5.3 SnapshotCacheFactory

```kotlin
class SnapshotCacheFactory(storage, json) {
    fun <T> create(key: String, serializer: KSerializer<T>): SnapshotCache<T>
}
```

Retorna `LocalSnapshotCache` configurado.

### 5.4 RetrofitSnapshotFetcher

Implementacao base de `SnapshotFetcher` para Retrofit:

| HTTP Status | Resultado |
|-------------|-----------|
| 304 | `NetworkResult.NotModified` |
| 200-299 | `NetworkResult.Success(body, eTag)` |
| 401/403 | `HttpPermissionException` |
| Outros 4xx/5xx | `IllegalStateException` |
| Erro de rede | `NetworkResult.Failure` |

### 5.5 Preferences

#### ThemePreferences (`@SettingsPrefs`)

| Chave | Tipo | Default | Descricao |
|-------|------|---------|-----------|
| `theme_mode` | Int | 0 (FollowSystem) | 0=Sistema, 1=Light, 2=Dark |
| `hymnal_font_size` | Float | 22f | Tamanho da fonte do hinario |
| `song_scroll_mode` | Int | 0 (Horizontal) | 0=Horizontal, 1=Vertical |

Flows: `themeModeFlow`, `hymnalFontSizeFlow`, `songScrollModeFlow`.

#### SetlistPreferences (`@SetlistPrefs`)

Pins de musicas para o dia atual. Reseta automaticamente quando a data muda.

| Chave | Tipo | Descricao |
|-------|------|-----------|
| `setlist_date` | String | Data atual (YYYY-MM-DD) |
| `setlist_song_ids_v1` | String | IDs separados por virgula |

Flow: `pinnedSongIds: Flow<List<Int>>`.
Metodo: `toggleSong(songId)` — adiciona/remove do set.

#### SongScrollMode

```kotlin
enum class SongScrollMode { HORIZONTAL, VERTICAL }
```

### 5.5.1 Sessao criptografada (`core/data/security`)

A sessao (`@AuthPrefs`) e um Preferences DataStore cujo arquivo so contem um blob selado:
`files/datastore/auth_prefs.enc.preferences_pb`. Nada da sessao fica em texto puro no disco.

| Peca | Papel |
|------|-------|
| `AeadCipher` | Porta: `encrypt`, `decrypt` (falha = `UndecryptableException`), `destroyKey` |
| `KeystoreAeadCipher(alias)` | AES-256-GCM com chave nao exportavel no `AndroidKeyStore`; formato `[versao][IV 12][cifra+tag]` |
| `EncryptedSerializer` | Envolve qualquer `OkioSerializer`; blob que nao abre vira `CorruptionException` |
| `PlaintextAuthPrefsMigration` | Primeira leitura apos a atualizacao: copia `auth_prefs.preferences_pb` para o arquivo criptografado e apaga o antigo. Se o criptografado ja tem tokens, eles ficam (sao mais novos) |
| `SessionRecovery` | Handler de corrupcao: destroi a chave e liga `pendingWipe`; a sessao vira vazia (app deslogado, sem mensagem) |
| `EncryptedAuthPrefs.create` | Monta o store (storage okio + serializer + migracao + handler) |

Alias: `ipbcb_session_v1`. Arquivos novo e antigo ficam fora de backup e transferencia
(`backup_rules.xml`, `data_extraction_rules.xml`). Formato e regras:
`specs/012-encrypted-session-photo-cache/contracts/on-device-storage.md`.

### 5.6 NetworkConnectivityObserver

Monitora conectividade WiFi via `ConnectivityManager` + `callbackFlow`.

- `isOnWifi: Flow<Boolean>` — emite estado atual + mudancas.

### 5.6.1 Imagens — orcamento de memoria do Coil

O app tem quatro `ImageLoader`: o padrao (`MyApp` como `ImageLoaderFactory`: originais e capas da galeria, avatar
da conta), `@GalleryThumbnailLoader`, `@MemberPhotoLoader` e `@EventImageLoader`. Cada um tem seu cache de memoria,
porque o logout esvazia os autenticados separadamente. O Coil daria 25% da memoria do app a cada um (100% so em
bitmaps); `ImageMemoryBudget` (`core/data/image`) limita: padrao 15%, previews da galeria 10%, fotos de membros 5%,
flyers de eventos 5%.

`@EventImageLoader` (`core/di/EventImageLoaderModule.kt`) vive em `core/` porque a home (`features/events`) e a
gestao (`features/admin/events`) usam o mesmo: client `@Client` (token so com sessao), `respectCacheHeaders(false)`
(imagem trocada ganha URL nova) e cache de disco `event_images` de 50 MB. `EventImageCache` o esvazia no fim da
sessao (`specs/events/spec.md` §2.3).

### 5.7 Constantes de Storage

```kotlin
object StorageDirConstants {
    const val PROFILE = "profile"
    const val GALLERY = "gallery"
}
```

---

## 6. Presentation

### 6.1 CoreActivity

Unica Activity do app (`@AndroidEntryPoint`), `launchMode="singleTop"`.

**Orientacao:** livre (em pe e deitado). A Activity declara `configChanges` de orientacao e tamanho
(`orientation|screenSize|screenLayout|smallestScreenSize|keyboardHidden`): girar o celular nao recria a Activity, o
Compose so recompoe com a nova configuracao, entao o estado em `remember` sobrevive. O recorte de foto
(`UCropActivity`) continua travado em retrato.

- `enableEdgeToEdge()`
- Aplica `IPBCasteloBrancoTheme`
- Cria `NavController` + `AppNavHost`
- In-app updates: enforces Play Store immediate update no launch. Cancelamento = `finish()`.
- Extension: `Activity.restartApp(message?)` — reinicia app via Intent para CoreActivity.
- Primeiro frame: um `OnPreDrawListener` segura o desenho ate `CoreViewModel.isBootReady` (caches do disco em
  memoria), no maximo 1 s. Desenhada antes, a home mostraria "Nenhum aniversariante" / "Escala indisponivel" e
  trocaria logo depois. No Android 12+ a splash do sistema fica na tela enquanto isso; antes, o fundo da janela. A
  Activity pega o mesmo `CoreViewModel` (`by viewModels()`) e chama `initialize()` (idempotente).
- Toque em notificacao: os extras viram `NotificationTarget` (`SundaySetlist` | `ConfirmPlays(date)`) em
  `onCreate` (sem estado salvo) e `onNewIntent` (app ja aberto, gracas ao `singleTop`). O `AppNavHost` navega quando
  `isBootReady` (caches do disco carregados): `SundaySetlist` -> lista de Letras (`navigateToLyrics()`);
  `ConfirmPlays` -> registro pre-preenchido (`navigateToSundayConfirmation(date)`, admin §3) se ha sessao e `songs`
  >= `manage`, senao fica na home com "Voce nao tem mais acesso ao registro de musicas.". Depois limpa o alvo.

### 6.2 CoreViewModel

Orquestra inicializacao e estado global.

**Injecoes:** `PreloadDataUseCase`, `AuthSession`, `FetchProfileUseCase`, `AuthEventBus`, `LogoutUseCase`, `GalleryAutoDownload`, `BibleAutoDownload`, `BibleRepository`, `ScheduleRepository`, `SyncSundaySetlistUseCase`, `ObserveWorshipAccessUseCase`, `PushRegistrationScheduler`, `UnregisterDeviceUseCase`, `AppForegroundState`, `SessionRecovery`.

**State:**
- `isPreloading: StateFlow<Boolean>` — app inicializando
- `isLoggedIn: StateFlow<Boolean>` — estado de autenticacao
- `isBootReady: StateFlow<Boolean>` — caches do disco ja em memoria (o `PreloadDataUseCase` avisa entre a fase de
  disco e a de rede); libera a navegacao de um toque em notificacao
- `canManageSongs: StateFlow<Boolean>` — `songs` >= `manage`, para o toque em "Confirmar musicas de domingo"
- `birthdays: StateFlow<List<Birthday>?>` — aniversariantes do mes; `null` sem nada para mostrar (snapshot em
  `Loading` ou `Error`)
- `isSyncing: StateFlow<Boolean>` — a fase de rede do boot, ou a que o `LoginSuccess` roda de novo, esta em
  andamento (contador: login durante o boot sobrepoe as duas). Pagina do carousel sem dado vira "carregando" so
  enquanto isso

**Events (SharedFlow):**
- `LogoutSuccess`

**Fluxo de inicializacao:**
1. Observa mudancas de login state e `SessionRecovery.pendingWipe` (sessao ilegivel → limpa os
   `SessionScopedCache`, secao 4.4.1)
2. Reage a `LoginSuccess` do AuthEventBus
3. Cascata: preload disco -> refresh rede -> auto-download gallery/bible -> fetch profile

O snapshot do perfil (`ProfileSnapshotRepository`) e um `Preloadable` como os demais: o `/me` salvo
em disco entra na fase de preload, entao o acesso (secao 4.2.1) e `isMember` ja valem desde o boot, sem esperar a
rede. O refresh de rede do perfil continua no fim da cascata (depende de login) e corrige o valor
se ele mudou no servidor.

**Metodos:**
- `initialize()` — setup de observables e trigger startup. Idempotente; chamado pelo `AppNavHost`
  (escopo da Activity) para garantir o boot em qualquer rota restaurada — ver secao 4.4.
- `logout()` — remove o token de push no servidor (secao 4.2.5, nunca bloqueia), limpa todos os caches (incluindo
  todo `SessionScopedCache`, secao 4.4.1) + tokens
- `onAppForeground()` / `onAppBackground()` — `ON_START`/`ON_STOP` do `AppNavHost`: marcam `AppForegroundState`;
  o primeiro tambem sincroniza a galeria e o repertorio de domingo
- No boot logado e no `LoginSuccess`, agenda o registro do token de push
- No `LoginSuccess`, roda de novo a fase de rede da cascata (todos os `Refreshable`): o boot sem sessao
  guardou erro (401) nos snapshots restritos a membros (escala, aniversariantes...) e nada mais os
  buscaria ate o proximo boot

### 6.3 CoreScreen (Tela Principal)

Tela home com drawer de navegacao e, de cima para baixo: faixa verde, cartoes de atalho e carousel de destaques.
Tudo cabe numa tela, sem rolagem, em qualquer altura de tela (`HomeLayout`). A faixa tem a altura do conteudo. A
grade de atalhos fica centralizada na vertical da tela inteira (da barra de status a barra de navegacao), separada
da faixa: nao sobe por cima dela. O carousel fica centralizado no espaco que sobra embaixo. Quando nao da para
centralizar, a grade se move o minimo: fica pelo menos 16dp abaixo da faixa e deixa o carousel em largura total.

#### Drawer

| Item | Condicao | Acao |
|------|----------|------|
| Login | nao logado | navega para auth |
| Painel de Gestão | logado + algum papel no perfil (`canOpenPanel`) | navega para admin |
| Configuracoes | sempre | navega para settings |
| Sair | logado | executa logout |

Largura: 80% da tela em pe e 40% deitado (altura menor que 480dp), no maximo 360dp. Girar o celular fecha o menu.

#### Celular deitado

Com altura de tela menor que 480dp (altura compacta do Material: celular deitado), a home troca de layout
(`HomeLandscapeLayout`), com as mesmas pecas e sem rolagem:

- **Faixa fina em cima:** a faixa verde vira uma linha (cantos inferiores de 22dp). Data e saudacao a esquerda, numa
  linha cada, com reticencias; o cartao da escala (300dp de largura) ou o botao de entrar a direita.
- **Embaixo, lado a lado:** os 6 cartoes de atalho em 3x2 a esquerda (80dp de altura, circulo de 34dp, icone de 19dp)
  e o carousel de destaques a direita, centralizado, em 3:2 pela altura que sobra.

Tablet em pe (altura >= 480dp) continua com o layout em pe.

#### Faixa verde

Continua a top bar (`ipbGreen`, igual nos dois temas), com cantos inferiores arredondados (28dp). Texto branco.

- Linha de data: dia da semana e data por extenso ("Quarta, 28 de outubro").
- Saudacao pela hora do aparelho: "Bom dia" (05h-11h59), "Boa tarde" (12h-17h59), "Boa noite" (18h-04h59). Logado
  e com nome no perfil, segue o primeiro nome ("Boa noite, Gabriel"); visitante ve "Bem-vindo à IPB Castelo Branco".
- Abaixo, um cartao translucido clicavel (abre a escala) com um bloco de data (mes abreviado + dia) e duas linhas
  (`ScheduleViewModel.band`, tipo `ScheduleBandUi`):
  - **Escalado:** "Sua próxima escala" / "Domingo, 15/11 · 18h" / "Faltam 18 dias" ("Amanhã" a 1 dia, "É hoje" no
    dia). E a primeira data do mes, de hoje em diante, em qualquer secao da escala, cuja linha e do membro ligado ao
    usuario (`MeProfile.memberId` igual ao `member.id` da escala). Bloco de data branco.
  - **Fora da escala:** "Próximo culto" / "Quinta, 29/10 · 19h30" / "Escalado: Marcos Vinícius" — a proxima data
    da secao do card da escala (`nextSection`). Bloco de data translucido.
  - Sem escala carregada, sem data futura no mes ou sem acesso: o cartao nao aparece, ficam data e saudacao.
- Visitante: no lugar do cartao, o botao branco "Entrar para ver sua escala" (abre auth).

O horario some do titulo quando a secao nao tem horario.

#### Cartoes de atalho (3x2)

Seis cartoes iguais, 90dp de altura, em 3 colunas com 10dp entre eles e 16dp de margem lateral. Fundo e sombra do card do carousel (`surfaceContainerLowest` no claro, `surfaceContainerHigh`
no escuro), cantos de 18dp. Dentro: icone branco num circulo `primaryContainer` de 38dp e o rotulo abaixo.

| Posicao | Rotulo | Icone | Destino |
|---------|--------|-------|---------|
| 1 | Louvor | `ic_worshiphub` | WorshipHubGraph |
| 2 | Escala | `ic_schedule` | ScheduleScreen |
| 3 | Galeria | `ic_galery` | GalleryGraph |
| 4 | Hinário | `ic_sarca_ipb` | HymnalGraph |
| 5 | Estudos | `ic_studies` (capelo, Material Symbols `school`) | StudiesGraph |
| 6 | Bíblia | `ic_bible` | BibleGraph |

#### Carousel de Destaques (auto-scroll 5s)

| Pagina | Conteudo |
|--------|----------|
| 1 | Escala do proximo culto (tabela dia x membro) — clicavel, abre a tela de escala (`AppRoutes.SCHEDULE`) |
| 2 | Aniversariantes do mes — clicavel, abre `BirthdaysScreen` (`AppRoutes.BIRTHDAYS`) |
| 3+ | Um card por evento ativo, no maximo 3 (`HighlightEvent`) — clicavel, abre o detalhe (`AppRoutes.EVENT_DETAIL`). Sem evento (ou ainda carregando), nenhuma pagina de evento. Ver `specs/events/spec.md` §2 |
| Fallback | "Escala indisponivel" (se dados nao carregaram) — tambem clicavel, abre a tela de escala |

**Carregando, nunca vazio antes da hora.** A home nao mostra "Nenhum aniversariante" nem "Escala indisponivel"
enquanto o dado ainda pode chegar. Com cache em disco, o primeiro frame ja espera por ele (secao 6.1). Sem cache
(primeira instalacao) ou depois de um login (o boot anonimo guardou 401), cada pagina sem dado, enquanto
`CoreViewModel.isSyncing`, mostra icone + titulo e um spinner pequeno no lugar da mensagem
(`HighlightBirthdaysLoading`, `HighlightScheduleLoading`). Terminada a sincronizacao sem dado, entra a mensagem de
vazio/indisponivel de sempre. A `CoreView` junta tudo em `HighlightsUi` (escala sem dado = `ScheduleUiState` que nao
e `Success` nem `Empty`; aniversariantes e eventos sem dado = `null`).
`ScheduleViewModel.nextSection` comeca do snapshot ja em memoria, nao de `null`.

**Paginas de evento.** Vem do `EventsHighlightViewModel` (`features/events`), coletado na `CoreView` como o
`ScheduleViewModel`. A `CoreView` pede a lista de novo em todo `ON_RESUME` e quando `isLoggedIn` muda: o logout fica
na home, entao so a troca de login avisa que os eventos "Só membros" saem e a lista publica volta.

**Card da escala.** Mostra a secao do proximo culto (`ScheduleViewModel.nextSection`, tipo `NextScheduleUi`):
cabecalho com titulo + horario e uma linha por data do mes, sem cabecalho de colunas. Um dia da semana
pode cair 5 vezes no mes, entao o card precisa caber 5 linhas: as linhas dividem por igual a altura
abaixo do cabecalho, entao 4 ou 5 linhas ocupam o card inteiro, sem sobra no rodape. Destaque por data,
calculado no ViewModel (`nextDay`):
- **proxima data** (primeiro dia >= hoje): dia e nome em negrito, cor `primary`;
- **datas passadas** (dia < `nextDay`, ou todas quando `nextDay` e nulo): texto esmaecido;
- escala de mes futuro: nenhuma data passada, a primeira e a proxima; escala de mes anterior: todas passadas.

A linha do proprio usuario (mesmo `memberId`) ganha " · você" depois do nome, em `primary`.

**Card dos aniversariantes.** O aniversario de hoje tem o badge destacado (`BirthdayEntry.highlighted`).

HorizontalPager com auto-scroll (tween 600ms) dentro de um card elevado em 3:2 — proporcao fixa, para o card nao
achatar em telas largas; e a proporcao do flyer de evento, que aparece inteiro. Em tela baixa, onde a largura toda
nao cabe na altura que sobra, o card encolhe mantendo o 3:2. Cantos de 20dp, sombra externa suave
e fundo que se destaca da tela (`surfaceContainerLowest` no claro, `surfaceContainerHigh` no escuro); sem sombra
interna. Abaixo do card, um indicador de pagina (pontos; o da pagina atual alongado, cor `primaryContainer`),
escondido quando ha uma pagina so.

Cabecalho das paginas de escala e aniversariantes: sem faixa colorida — titulo (e o emoji, nos aniversariantes) e,
na escala, o horario num chip, separados do conteudo por um divisor fino. Na escala, a proxima data fica numa faixa
arredondada `secondaryContainer`, sem rotulo, com dia e nome alinhados as outras linhas; as linhas nao tem
divisores. Placeholders (escala indisponivel, sem aniversariante) usam `onSurface`/`onSurfaceVariant`.
Ao voltar para a home, o carousel sempre mostra uma pagina inteira: sair da home no meio da animacao cancela o
scroll e o estado salvo guarda o deslocamento parcial, entao o auto-scroll encaixa na pagina atual antes do loop.

**Fora do escopo por enquanto:** versiculo do dia (pensado como pagina do carousel, lido da Biblia offline).

#### BirthdaysScreen (aniversariantes do ano)

Tela unica (`composable` inline), aberta pela pagina de aniversariantes do carousel. Grade de 2 colunas
com um card por mes, janeiro a dezembro; abre rolada no mes atual. Cada card repete o layout do
highlight (`BirthdayEntry`: badge do dia + nome). Mes atual com cabecalho destacado; aniversario de hoje
com badge destacado. Mes vazio: "Nenhum aniversariante". Pull-to-refresh; erro sem cache mostra
`PermissionErrorPlaceholder`. `BirthdaysViewModel` le `GetYearBirthdaysUseCase` — mesmo cache do
card, sem request extra. Regras de dominio em `specs/001-monthly-birthdays-card/spec.md`.

### 6.4 BaseScreen

Wrapper Scaffold + TopBar compartilhado por todas as telas de features.

**Parametros:** `tabName`, `logoRes`, `showBackArrow`, `onMenuClick`, `onBackClick`, `onAccountClick`, `showAccountAction`, `containerColor`, `extraActions`, `topBarExtension`, `content`.

**Logica de foto de perfil:**
- Verifica `filesDir / "profile" / "profile_photo.*"`
- Usa Coil AsyncImage (ou placeholder)
- Bumpa versao quando login state muda (via `ProfilePhotoBus.version`)
- ViewModel interno: `TopBarProfileViewModel`
- So renderiza arquivo local: nenhuma tela carrega URL de midia direto. A midia (`/ipbcb/media/`) exige
  JWT e so e baixada pelo `@AuthedRetrofit`.

**Download da foto (`ProfilePhotoDataSource.downloadAndPersist`):** envia `If-None-Match` com o ETag
salvo. A midia e protegida (backend `009-protected-media-access`); cada desfecho:

| Resposta | Retorno | Foto local + ETag + ultima URL |
|----------|---------|--------------------------------|
| `200` | `success(novo arquivo)` | substituida (via temporario), ETag novo salvo |
| `304` | `success(foto local)` | mantida |
| `404` | `success(null)` | apagada, placeholder |
| `403` | `success(null)` | apagada, placeholder — sem erro na tela |
| `429` | `success(foto local)` | mantida |
| sem rede / corpo cortado | `success(foto local)` | mantida |
| `401`, outros | `failure(AppError)` | mantida |

`401` so chega aqui depois de o `TokenAuthenticator` tentar o refresh; se o refresh foi recusado, os
tokens ja foram apagados e `isLoggedInFlow` leva o app ao estado deslogado.

**Navegacao:** usa `LocalAppNavigator` para profile/auth no clique do botao de conta.

### 6.5 Navegacao

#### AppRoutes

Telas individuais:
- `CORE`, `SCHEDULE`, `SETTINGS`, `PROFILE`, `BIRTHDAYS`, `EVENT_DETAIL` (`events/{eventId}`)

Grafos de features:
- `AUTH_GRAPH`, `ADMIN_GRAPH`, `WORSHIP_HUB_GRAPH`, `GALLERY_GRAPH`, `HYMNAL_GRAPH`, `BIBLE_GRAPH`, `STUDIES_GRAPH`

#### AppNavHost

NavHost unico com todas as rotas. Transicoes desabilitadas (Enter/Exit = None).

- Auth success: navega para CORE e pop AUTH_GRAPH (inclusive)
- Logout success: `popUpTo(CORE) { inclusive = true }`
- Share (schedule): `Intent.createChooser(...)`

#### LocalAppNavigator

CompositionLocal que fornece funcoes de navegacao para BaseScreen:
- `navigateToProfile()`
- `navigateToAuth()`

Erro se nao provido (staticCompositionLocalOf com error factory).

#### AppNavExtensions

- `NavHostController.safePopBackStack()` — pop somente se nao esta na rota CORE (previne crash).

### 6.6 Tema

#### Cores

- Cor base: `ipbGreen = #045A48`
- Paleta Material 3 completa: light, dark, medium contrast, high contrast
- 8 cores base + 12 tonais por variante

#### Theme

- `IPBCasteloBrancoTheme()` — aplica colorScheme + AppTypography
- Suporte a Dynamic Color (Android 12+, se habilitado)
- Dark mode: `AppCompatDelegate.getDefaultNightMode()` com fallback para `isSystemInDarkTheme()`

#### Tipografia

- Display: Poppins; Body: Inter — embutidas em `res/font` (subset latino, pesos 400/500/600/700), nao baixadas do
  Google Fonts: a fonte baixavel chega depois do primeiro frame, entao o texto saia em Roboto e trocava. Peso real
  por arquivo, sem negrito sintetizado. Sem dependencia de `ui-text-google-fonts`
- 15 estilos Material 3 mapeados (displayLarge ate labelSmall)

### 6.7 Componentes Reutilizaveis

| Componente | Descricao |
|------------|-----------|
| `TopBar` | Logo + tab name + back/menu + account image. Fundo ipbGreen, icones brancos |
| `CustomButton` | Icone + texto, rounded 16dp, som de clique, tamanho dinamico |
| `Highlight` | Carousel com auto-scroll, HorizontalPager, card elevado em 3:2 (largura da tela) + indicador de pagina. O auto-scroll le `pagerState.pageCount`, pois as paginas de evento entram e saem |
| `HighlightEvent` | Pagina de evento do carousel: flyer preenchendo o card, titulo e data sobre gradiente; sem imagem, fundo neutro |
| `BirthdayEntry` | Linha de aniversario: badge com o dia (`primaryContainer`, ou `primary` quando destacado) + nome com ellipsis. Usada pelo highlight e pela `BirthdaysScreen` |
| `ThemeToggle` | Switch "Modo Escuro" / "Modo Claro" |
| `DateFieldWithPicker` | Input estilizado 56dp, botao verde "Calendario" |
| `ElasticPullToRefresh` | Wrapper para Material 3 PullToRefreshBox |
| `InDevelopmentScreen` | Placeholder "Pagina em construcao" |
| `PermissionErrorPlaceholder` | Erro de permissao com botao opcional "Conectar a sua conta" |
| `SquarePhotoPicker` | `rememberSquarePhotoPicker(onPicked, onCancelled)`: escolhe imagem da galeria, recorta com UCrop (1:1, max 512x512, titulo "Recortar Foto") e entrega os bytes JPEG. Apaga o arquivo recortado logo depois de ler e varre `cacheDir/cropped_*` a cada abertura — nenhuma copia fica no aparelho. Usado pela foto de perfil e pela foto de membro |

### 6.8 Modifiers

- `Modifier.tapToPaginate(pagerState, scope, zoneWidthFraction)` — tap nas bordas (20% default) navega entre paginas com animacao.

---

## 7. Dependencias Cross-Feature

O core depende de interfaces/classes de features auth para funcionar:

| Classe | Feature | Uso no Core |
|--------|---------|-------------|
| `TokenStorage` | auth | AuthInterceptor, TokenAuthenticator |
| `AuthApi` | auth | TokenAuthenticator (refresh) |
| `AuthSession` | auth | CoreViewModel, BaseScreen |
| `FetchProfileUseCase` | profile | CoreViewModel |
| `LogoutUseCase` | auth | CoreViewModel |
| `ProfilePhotoBus` | profile | BaseScreen (TopBarProfileViewModel) |
| `GalleryAutoDownload` | gallery | CoreViewModel |
| `BibleAutoDownload` | bible | CoreViewModel |
| `BibleRepository` | bible | CoreViewModel |
| `ScheduleRepository` | schedule | CoreViewModel |

> Nota: estas dependencias sao injetadas via Hilt. O core nao importa pacotes de features diretamente — as interfaces vivem em `core/domain/` ou sao providas via modulos Hilt das features.

### Contrato de `GET /api/me/profile/`

`FetchProfileUseCase` entrega `MeProfile`, mapeado de `MeProfileDto`:

| Campo JSON | Campo Kotlin | Tipo | Obrigatorio |
|------------|--------------|------|-------------|
| `name` | `name` | `String` | sim |
| `member_access` | `memberAccess` | `Boolean` | nao (fallback `is_member`) |
| `is_member` | — | `Boolean` | nao (default `false`; so fallback, removido quando o servidor parar de enviar) |
| `access_status` | `memberAccessInfo.status` | `String?` | nao (`null` = desconhecido) |
| `access_requests_blocked` | `memberAccessInfo.requestsBlocked` | `Boolean?` | nao |
| `data_correction_pending` | `memberAccessInfo.correctionPending` | `Boolean?` | nao |
| `roles` | `roles` | `List<RoleDto>` (`id`, `name`) | nao (default vazio) |
| `permissions` | `permissions` | `Map<String, String?>` | nao (default vazio) |
| `photo_url` | `photoUrl` | `String?` | nao (default `null`) |
| `member_id` | `memberId` | `Long?` | nao (default `null`) |
| `is_worship_member` | `isWorshipMember` | `Boolean` | nao (default `false`) |
| `can_save_setlist` | `canSaveSetlist` | `Boolean` | nao (default `false`) |

O campo `active` foi removido do DTO, do dominio e do `ProfileUiState`: nenhuma permission
class do backend o lia (as checagens usam `is_member` e os papeis) e nenhuma tela do app o
exibia. O backend ainda envia a chave; `ignoreUnknownKeys = true` no `Json` do
`SerializationModule` a descarta, e o app segue funcionando quando o backend parar de
enviar. Regressao coberta por `MeProfileDtoBackwardCompatibilityTest`.

`is_admin` saiu do contrato (backend 012) e do app. `roles` e `permissions` sao lidos como texto cru e so viram
`Access` no mapper (`AccessMapper`): um valor desconhecido nunca quebra a desserializacao, vira "sem acesso". Um
`/me` salvo em disco no formato antigo decodifica normalmente — `is_admin` e descartado e os dois campos novos ficam
vazios — e vale como "sem papel" ate o proximo fetch. `MeProfile.access` carrega o `Access` ja mapeado.

---

## 8. Seguranca

- Tokens armazenados em DataStore plain (migracao para EncryptedSharedPreferences pendente).
- Sem `Log.d` com PII em producao.
- ProGuard/R8 ativo em release.
- Secrets via `local.properties` + `BuildConfig` (nunca hardcoded).
