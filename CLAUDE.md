# CLAUDE.md — IPB Castelo Branco

## Project

- **Package:** `com.ipb.castelobranco`
- **API base:** `https://gabrielafonso.com.br/ipbcb/`
- **Single module:** `:app` — all code under `app/src/main/java/`

## Architecture — Feature-Based MVVM + Clean

**Single Activity:** `CoreActivity` is the ONLY `@AndroidEntryPoint`. UI 100% Compose.

Flow: `UI → ViewModel → UseCase → Repository (interface) → Repository (impl)`

```
core/
├── data/          — DataStore, repositories, snapshot cache
├── di/            — Hilt modules
├── domain/        — Interfaces, use cases, AuthEventBus
├── network/       — AuthInterceptor, TokenAuthenticator
└── presentation/
    ├── CoreActivity.kt
    ├── viewmodel/CoreViewModel.kt
    ├── base/BaseScreen.kt  — shared Scaffold+TopBar wrapper for all feature screens
    ├── screens/CoreScreen.kt
    └── navigation/  — AppRoutes, AppNavHost, LocalAppNavigator, AppNavExtensions
features/
├── auth/ | profile/ | schedule/ | settings/
└── gallery/ | hymnal/ | worshiphub/ | admin/ | bible/ | studies/
```

**Rules:**
- Features never import each other — share via `core/`.
- `domain/` has no Android knowledge (no `Context`, `ViewModel`, Compose).
- `presentation/` only knows the ViewModel — never accesses repo/use case directly.
- Errors are sealed classes or `Result<T>` — raw HTTP exceptions never reach the ViewModel.

## UI — Jetpack Compose

- Every screen must handle the three states — loading, success, error. The default representation is a
  `data class` UI state with flags (`isLoading: Boolean`, `error: String?`), which pairs with the
  `SnapshotState` returned by `BaseSnapshotRepository` — that is the project's real sealed hierarchy.
  Use `sealed` for one-shot events (`MusicRegistrationEvent`, `AdminScheduleEvent`, `SaveResult`) and
  where a screen's states are genuinely exclusive (`ScheduleViewModel`).
- ViewModel exposes `StateFlow<UiState>`; Screen collects with `collectAsStateWithLifecycle()`.
- Composables are **dumb**: receive state, emit events via lambdas. Zero logic.
- Separate Screen (VM collector) from content Composable (pure data) — mandatory, and what makes a
  preview possible whenever someone wants one.
- `@Preview` with explicit fake data is required on shared components in
  `core/presentation/components/`, optional on feature screens.
- Naming: PascalCase, no prefix (`ScheduleScreen`).

## ViewModel

- Exposes only `StateFlow`/`SharedFlow`. Never `LiveData`.
- Use `viewModelScope`. Never `GlobalScope` or manual `CoroutineScope`.
- One-time events (snackbar, dialog, nav) via `SharedFlow<UiEvent>`.
- No `Context` in ViewModel — use parameter or `UiText`.
- Multi-screen graphs: use graph-scoped `hiltViewModel(graphEntry)` (see Pitfalls #1).

## Navigation

- Multiple screens → `NavGraphBuilder.xGraph()` in `XNavGraph.kt`.
- Single screen → inline `composable {}` in `AppNavHost`.
- Existing graphs: `authGraph`, `adminGraph`, `worshipHubGraph`, `hymnalGraph`, `galleryGraph`, `bibleGraph`, `studiesGraph`.
- Top-level routes go in `AppRoutes`. Feature-internal routes use a local `XRoutes` object inside the nav graph file (e.g. `AdminRoutes`, `WorshipHubRoutes`).
- Always use `navController.safePopBackStack()` (from `AppNavExtensions.kt`) — never raw `popBackStack()` (crashes if already at root).

## DI — Custom Qualifiers

```kotlin
@AuthedRetrofit / @AuthLessRetrofit   // protected / public APIs
@Client / @AuthLessClient             // authenticated / unauthenticated OkHttpClient
@AuthPrefs / @SettingsPrefs           // DataStores
@ApiBaseUrl                           // base URL string
```

Wrong qualifier on protected API → silent 401.

## Security

- Never hardcode keys/secrets — use `local.properties` + `BuildConfig`.
- Auth tokens: pending migration to `EncryptedSharedPreferences` (currently plain DataStore).
- No `Log.d` with PII in production. ProGuard/R8 active in release.

## Tests

Stack: `JUnit4` + `MockK` + `kotlinx-coroutines-test` + `turbine`. Minimum: happy path + 1 error per use case. Prefer fakes over mocks.

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.*"
```

**Gradle cache — never use** `clean`, `--rerun-tasks`, or `--no-daemon` before running tests (destroys KSP cache, turns seconds into 15+ min). Use `clean` only for unexplainable build errors.

**Cross-platform cache (Windows host + Linux container):** Both `build/` and `.gradle/` are redirected to container-local paths so Windows and Linux never share compiled artifacts:
- `build/` → `/home/node/.gradle-builds/IPB Castelo Branco/app/` (configured in `build.gradle.kts`)
- `.gradle/` → `/home/node/.gradle-builds/project-cache` (configured in `~/.gradle/gradle.properties` via `gradle.projectCacheDirOverride`)

Both environments can build/test simultaneously without cleaning. First run on a fresh container is slow (cold cache); subsequent runs are fast (~6s for tests).

`sdk.dir` in `local.properties` → `/home/node/.local/android-sdk`

## Code

- All code in English. User-visible strings in Portuguese (hardcoded, no `strings.xml`).
- Line limit: 120 chars. Kotlin code style: `official`. No magic strings — use constants.
- No unnecessary abstractions. Check compatibility before adding libs.

## Git

- Conventional Commits: `feat`, `fix`, `refactor`, `chore`, `test`, `docs`.
- Version bump in separate commit: `chore(release): bump version to X.Y.Z`.

## Pitfalls

1. **Graph-scoped VM:** In existing graphs (hymnal, gallery, worshiphub, admin), use `hiltViewModel(graphEntry)` — not bare `hiltViewModel()`. **`CoreViewModel` is the exception:** `AppNavHost` resolves it outside the `NavHost` (Activity scope), so `hiltViewModel(getBackStackEntry(AppRoutes.CORE))` returns a *second*, never-initialized instance. Pass what you need down as a parameter to the graph.
2. **Single screen vs. graph:** Check `AppNavHost.kt` first — single screen = inline, multiple = graph.
3. **Snapshot cache:** Offline features follow the `JsonSnapshotStorage` pattern (`HymnalSnapshotModule`, `ScheduleSnapshotModule`...).
4. **UCropActivity** in Manifest — do not remove (profile photo upload).
5. **Theme change:** `context.findActivity()?.recreate()` — called from Screen, never from ViewModel.
6. **Auth/Logout:** Auth success via lambda `onAuthSuccess()` in `AppNavHost → authGraph`. Logout via `popUpTo(MAIN) { inclusive = true }`.
7. **Share intents:** `navController.context.startActivity(Intent.createChooser(...))`.
8. **worshiphub sub-graphs:** `worshipHubGraph` contains nested `chordChartsGraph` and `lyricsGraph` — add new screens inside the appropriate sub-graph, not directly in `worshipHubGraph`.
9. **In-app updates:** `CoreActivity` enforces immediate Play Store updates on launch — if user cancels, app calls `finish()`. Do not remove `AppUpdateManager` logic.
10. **`restartApp()`:** Use `activity.restartApp()` (extension in `CoreActivity.kt`) to fully restart the app — replaces manual Intent construction.

## 7. Specs Driven Development

The project follows **Specs Driven Development**: the spec is the source of truth — code reflects the spec, not the other way around.

### 7.1 Structure

```
specs/                          # project root, outside server/
├── constitution.md             # rules no domain can break
├── flows/                      # complex flows crossing domains
│   └── {flow}.md
└── {domain}/                   # e.g., songs, accounts, schedule
    ├── spec.md                 # what the domain is — complete, including unimplemented parts
    ├── plan.md                 # how it will be implemented — technical decisions
    └── tasks.md                # what still needs to be implemented
```

### 7.2 Workflow

**Before coding:**
- Always read the domain spec before implementing anything
- If no spec exists, create one before coding
- If the spec is outdated, update it before coding

**When making a change:**
- Update the spec first, then the code
- Spec and code go together in the same commit
- Never update only the code without updating the spec

**When creating something new:**
- Write the complete domain spec — including what doesn't exist yet
- Create `tasks.md` only with what still needs to be implemented
- Use `plan.md` for technical decisions and implementation order

**When encountering code without a spec:**
- Create the spec for the current state before making any changes
- Only then apply the change to both spec and code

### 7.3 What each file answers

- **`spec.md`** — What does this domain do? Which endpoints? Which data models? Which business rules? Which errors?
- **`plan.md`** — How will it be implemented? Which technical decisions? In what order?
- **`tasks.md`** — What still needs to be implemented?
- **`constitution.md`** — Global rules no domain can break (auth, security, architecture)
- **`flows/{flow}.md`** — Flows crossing multiple domains

### 7.4 Never do

- Don't code without reading the domain spec
- Don't make changes without updating the spec
- Don't create refactoring specs — specs describe the destination, not the path
- Don't repeat in the spec what's already in `constitution.md`

---

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at `specs/004-protected-media-downloads/plan.md`
<!-- SPECKIT END -->
