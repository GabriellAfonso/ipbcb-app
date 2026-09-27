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
| `@AuthPrefs` | DataStore | Armazenamento de tokens |
| `@SettingsPrefs` | DataStore | Preferencias de tema/fonte/scroll |
| `@SetlistPrefs` | DataStore | Pins de musicas do dia |

Qualifier errado em API protegida resulta em 401 silencioso.

### 2.2 Modules

| Module | Scope | Responsabilidade |
|--------|-------|-----------------|
| `HttpClientModule` | Singleton | OkHttpClient (auth e authless), logging interceptor |
| `RetrofitModule` | Singleton | Retrofit instances, base URL |
| `SerializationModule` | Singleton | kotlinx.serialization.Json configurado |
| `DataStoreModule` | Singleton | 3 DataStores (auth, settings, setlist) |
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
    }
}
```

Implementacao (`AuthEventBusImpl`): `MutableSharedFlow(extraBufferCapacity = 1)`, usa `tryEmit()`.

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

Dado que vive **so em memoria** e so vale para a sessao atual (ex.: fichas de membros vistas por um
lider). Features registram implementacoes via `@IntoSet`; o set vazio e declarado em
`core/di/SessionModule.kt` (`@Multibinds`).

`CoreViewModel` chama `clear()` de todos:
- em `logout()`, antes de `logoutUseCase()`;
- quando `isLoggedInFlow` passa de `true` para `false` (sessao encerrada pelo `TokenAuthenticator`
  depois de um refresh que falhou — nesse caminho `logout()` nao e chamado).

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

### 5.6 NetworkConnectivityObserver

Monitora conectividade WiFi via `ConnectivityManager` + `callbackFlow`.

- `isOnWifi: Flow<Boolean>` — emite estado atual + mudancas.

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

Unica Activity do app (`@AndroidEntryPoint`).

- `enableEdgeToEdge()`
- Aplica `IPBCasteloBrancoTheme`
- Cria `NavController` + `AppNavHost`
- In-app updates: enforces Play Store immediate update no launch. Cancelamento = `finish()`.
- Extension: `Activity.restartApp(message?)` — reinicia app via Intent para CoreActivity.

### 6.2 CoreViewModel

Orquestra inicializacao e estado global.

**Injecoes:** `PreloadDataUseCase`, `AuthSession`, `FetchProfileUseCase`, `AuthEventBus`, `LogoutUseCase`, `GalleryAutoDownload`, `BibleAutoDownload`, `BibleRepository`, `ScheduleRepository`.

**State:**
- `isPreloading: StateFlow<Boolean>` — app inicializando
- `isLoggedIn: StateFlow<Boolean>` — estado de autenticacao

**Events (SharedFlow):**
- `LogoutSuccess`

**Fluxo de inicializacao:**
1. Observa mudancas de login state
2. Reage a `LoginSuccess` do AuthEventBus
3. Cascata: preload disco -> refresh rede -> auto-download gallery/bible -> fetch profile

O snapshot do perfil (`ProfileSnapshotRepository`) e um `Preloadable` como os demais: o `/me` salvo
em disco entra na fase de preload, entao `isAdmin`/`isMember` ja valem desde o boot, sem esperar a
rede. O refresh de rede do perfil continua no fim da cascata (depende de login) e corrige o valor
se ele mudou no servidor.

**Metodos:**
- `initialize()` — setup de observables e trigger startup. Idempotente; chamado pelo `AppNavHost`
  (escopo da Activity) para garantir o boot em qualquer rota restaurada — ver secao 4.4.
- `logout()` — limpa todos os caches (incluindo todo `SessionScopedCache`, secao 4.4.1) + tokens

### 6.3 CoreScreen (Tela Principal)

Tela home com drawer de navegacao + grid de botoes + carousel de destaques.

#### Drawer

| Item | Condicao | Acao |
|------|----------|------|
| Login | nao logado | navega para auth |
| Painel Admin | logado + admin | navega para admin |
| Configuracoes | sempre | navega para settings |
| Sair | logado | executa logout |

#### Grid de Botoes (2x3)

| Posicao | Botao | Destino |
|---------|-------|---------|
| 1 | Agenda | ScheduleScreen |
| 2 | Galeria | GalleryGraph |
| 3 | Hinario | HymnalGraph |
| 4 | Biblia | BibleGraph |
| 5 | Min. Louvor | WorshipHubGraph |
| 6 | Estudos | StudiesGraph |

#### Carousel de Destaques (auto-scroll 5s)

| Pagina | Conteudo |
|--------|----------|
| 1 | Aniversariantes |
| 2 | Escala do Domingo (tabela dia x membro) |
| 3 | Eventos |
| Fallback | "Agenda indisponivel" (se dados nao carregaram) |

HorizontalPager com auto-scroll (tween 600ms), cards com rounded corners (16dp) e shadow (6dp).

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
- `CORE`, `SCHEDULE`, `SETTINGS`, `PROFILE`

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

- Display: Poppins (Google Fonts)
- Body: Inter (Google Fonts)
- 15 estilos Material 3 mapeados (displayLarge ate labelSmall)

### 6.7 Componentes Reutilizaveis

| Componente | Descricao |
|------------|-----------|
| `TopBar` | Logo + tab name + back/menu + account image. Fundo ipbGreen, icones brancos |
| `CustomButton` | Icone + texto, rounded 16dp, som de clique, tamanho dinamico |
| `Highlight` | Carousel com auto-scroll, HorizontalPager, cards 250dp com shadow |
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
| `is_member` | `isMember` | `Boolean` | sim |
| `is_admin` | `isAdmin` | `Boolean` | sim |
| `photo_url` | `photoUrl` | `String?` | nao (default `null`) |

O campo `active` foi removido do DTO, do dominio e do `ProfileUiState`: nenhuma permission
class do backend o lia (as checagens usam `is_member` / `is_admin`) e nenhuma tela do app o
exibia. O backend ainda envia a chave; `ignoreUnknownKeys = true` no `Json` do
`SerializationModule` a descarta, e o app segue funcionando quando o backend parar de
enviar. Regressao coberta por `MeProfileDtoBackwardCompatibilityTest`.

---

## 8. Seguranca

- Tokens armazenados em DataStore plain (migracao para EncryptedSharedPreferences pendente).
- Sem `Log.d` com PII em producao.
- ProGuard/R8 ativo em release.
- Secrets via `local.properties` + `BuildConfig` (nunca hardcoded).
