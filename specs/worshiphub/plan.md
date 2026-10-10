# Worship Hub — Plan

Plano de implementacao da feature "Musicas" (unica parte nova; Tabelas, Cifras e Letras ja existem).

---

## 1. Mudancas no Backend (pre-requisito)

Feitas pelo dev manualmente, fora deste plano:

- `AllSongsAPI`: adicionar campo `youtube_link` na resposta
- `TopSongsAPI`: adicionar `song_id` no `.values()` da query
- `SongsBySundayAPI`: adicionar `song_id` no dict de cada musica
- `Song` model: adicionar campo `youtube_link` (CharField, blank=True, null=True)

---

## 2. Atualizacao dos DTOs e Modelos existentes

### 2.1 AllSongDto
Adicionar campo `youtubeLink` (`@SerialName("youtube_link")`), default `null`.

### 2.2 Song (domain model)
Adicionar `youtubeLink: String?`.

### 2.3 AllSongsMapper
Mapear novo campo.

### 2.4 TopSongDto
Adicionar `songId` (`@SerialName("song_id")`).

### 2.5 TopSong (domain model)
Adicionar `songId: Int`.

### 2.6 TopSongsMapper
Mapear novo campo.

### 2.7 SundaySongDto
Adicionar `songId` (`@SerialName("song_id")`).

### 2.8 SundaySetItem (domain model)
Adicionar `songId: Int`.

### 2.9 SongsBySundayMapper
Mapear novo campo.

---

## 3. Nova feature: songs (dentro de worshiphub)

Estrutura de pacotes:

```
features/worshiphub/songs/
├── domain/
│   └── usecase/
│       └── GetSongDetailUseCase.kt
├── presentation/
│   ├── navigation/
│   │   └── SongsNavGraph.kt
│   ├── screens/
│   │   ├── SongsListScreen.kt
│   │   └── SongDetailScreen.kt
│   ├── state/
│   │   ├── SongsListUiState.kt
│   │   └── SongDetailUiState.kt
│   └── viewmodel/
│       ├── SongsListViewModel.kt
│       └── SongDetailViewModel.kt
```

Nao precisa de data layer proprio — consome repositorios existentes via DI.

---

## 4. SongsListScreen

### 4.1 ViewModel (`SongsListViewModel`)

Injeta: `SongsRepository`.

Combina `observeAllSongs()` + query de busca. Filtra por `title` e `artist` usando `normalize()`. Mesmo padrao de `ChordChartsViewModel` e `LyricsViewModel`.

Sem pinned (por enquanto — item futuro na spec).

### 4.2 UiState (`SongsListUiState`)

```kotlin
data class SongsListUiState(
    val songs: List<SongListItem> = emptyList(),
    val filteredSongs: List<SongListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SongListItem(
    val id: Int,
    val title: String,
    val artist: String,
)
```

### 4.3 Tela

Mesmo padrao visual de `ChordChartsScreen` / `LyricsScreen`:
- BaseScreen com topbar
- Campo de busca no topo
- LazyColumn com items
- Pull-to-refresh
- Click no item → navega para SongDetailScreen

---

## 5. SongDetailScreen

### 5.1 UseCase (`GetSongDetailUseCase`)

Injeta: `SongsRepository`, `ChordChartRepository`, `LyricsRepository`.

Recebe `songId`. Combina os 3 observes e monta `SongDetail`:

```kotlin
data class SongDetail(
    val song: Song,
    val playCount: Int,
    val tones: List<String>,
    val lastSundays: List<String>,      // datas "dd/MM/yyyy", max 3
    val chordCharts: List<ChordChart>,   // pode ser 0, 1 ou N
    val lyrics: Lyrics?,                 // pode ser null
)
```

Logica de calculo (tudo client-side):
- `playCount`: contar itens em `songsBySunday` onde `songId` == alvo
- `tones`: distinct de `.tone` desses mesmos itens
- `lastSundays`: ultimas 3 datas distintas onde a musica apareceu
- `chordCharts`: filtrar lista de chord charts por `songId`
- `lyrics`: primeiro lyrics com `songId` correspondente (ou null)

### 5.2 ViewModel (`SongDetailViewModel`)

Recebe `songId` via `SavedStateHandle`.
Injeta `GetSongDetailUseCase`.
Expoe `StateFlow<SongDetailUiState>`.

### 5.3 UiState (`SongDetailUiState`)

```kotlin
data class SongDetailUiState(
    val songName: String = "",
    val artist: String = "",
    val playCount: Int = 0,
    val tones: List<String> = emptyList(),
    val lastSundays: List<String> = emptyList(),
    val chordCharts: List<ChordChartOption> = emptyList(),  // id + tone + instrument
    val hasLyrics: Boolean = false,
    val lyricsId: Int? = null,
    val youtubeLink: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class ChordChartOption(
    val id: Int,
    val tone: String,
    val instrument: String,
)
```

### 5.4 Tela

Layout vertical scrollavel:

1. **Header** — titulo grande + artista abaixo
2. **Stats** — card ou secao com:
   - "Tocada X vezes aos domingos"
   - Tons: chips ou texto inline (ex: "C, D, G")
   - Ultimos domingos: lista compacta de datas
3. **Botoes de acao** — row ou column:
   - **Cifra** — enabled se `chordCharts.isNotEmpty()`
     - 1 cifra → navega direto para `chord_chart_detail/{id}`
     - N cifras → abre AlertDialog/BottomSheet com opcoes (tom + instrumento)
   - **Letra** — enabled se `hasLyrics`, navega para `lyrics_detail/{lyricsId}`
   - **YouTube** — enabled se `youtubeLink != null && youtubeLink.isNotBlank()`
     - Abre `Intent(ACTION_VIEW, Uri.parse(youtubeLink))`

Botoes desabilitados: alpha reduzido + cor muted, sem onClick.

---

## 6. Navegacao

### 6.1 SongsNavGraph

Sub-graph novo `songsGraph` dentro de `worshipHubGraph`, mesmo padrao de `chordChartsGraph` e `lyricsGraph`.

```kotlin
private object SongsRoutes {
    const val List   = "songs_list"
    const val Detail = "song_detail/{songId}"
    fun detail(id: Int) = "song_detail/$id"
}

fun NavGraphBuilder.songsGraph(navController: NavHostController) {
    navigation(
        route            = WorshipHubRoutes.Songs,
        startDestination = SongsRoutes.List,
    ) {
        composable(SongsRoutes.List) { ... }
        composable(SongsRoutes.Detail, arguments = [...]) { ... }
    }
}
```

### 6.2 WorshipHubNavGraph

- Remover `composable(WorshipHubRoutes.Songs) { InDevelopmentScreen(...) }`
- Adicionar `songsGraph(navController)`

### 6.3 WorshipHubScreen

- Reordenar lista de botoes: Tabelas, Musicas, Cifras, Letras
- Setar `visible = true` no botao Musicas

### 6.4 Navegacao cruzada (SongDetail → Cifra/Letra)

SongDetailScreen precisa navegar para rotas de outros sub-graphs. Possivel porque todos estao dentro do mesmo `worshipHubGraph` — `navController.navigate("chord_chart_detail/$id")` funciona direto.

---

## 7. Ordem de implementacao

1. **DTOs e modelos** — atualizar campos novos (songId, youtubeLink)
2. **SongsListScreen** — lista + busca (funcional rapido, usa snapshot existente)
3. **GetSongDetailUseCase** — logica de cruzamento de dados
4. **SongDetailViewModel + UiState** — expor dados
5. **SongDetailScreen** — layout completo
6. **SongsNavGraph** — wiring de navegacao
7. **Ativar botao no hub** — reordenar + visible = true
8. **Testes** — use case (happy path + sem dados) + viewmodel

---

## 9. Navegacao das Tabelas para SongDetail

### 9.1 Contexto

Tabs "Ultimos Domingos" e "Mais Tocadas" ja possuem `songId` nos modelos (`SundaySetItem.songId`, `TopSong.songId`). Tab "Repertorio" tem `Song.id` no select. Objetivo: tornar titulos clicaveis para navegar ao `SongDetailScreen`.

### 9.2 Callback chain

`navController` nao e passado para tabs. Propagacao via lambda:

```
WorshipHubNavGraph (navController)
  → WorshipSongsTableScreen (onSongClick: (Int) -> Unit)
    → WorshipSongsTableContent (onSongClick)
      → LastSundaysTab (onSongClick)
      → TopSongsTab (onSongClick)
      → RepertoireTab (onSongInfoClick)
```

No NavGraph: `onSongClick = { songId -> navController.navigate(SongsRoutes.detail(songId)) }`.

Funciona porque Tables e SongsGraph estao dentro do mesmo `worshipHubGraph`.

### 9.3 Ultimos Domingos

`SundaySongRow`: titulo (coluna "Nome") vira `Text` com `Modifier.clickable` + ripple padrao. Recebe `onSongClick(songId)`.

### 9.4 Mais Tocadas

`TopSongsRow`: titulo (coluna "Nome") vira `Text` com `Modifier.clickable` + ripple padrao. Recebe `onSongClick(songId)`.

### 9.5 Repertorio — Icone Info

Dentro de `RepertoireRow`, quando `selectedSong != null`:
- Icone `Icons.Outlined.Info` (ou `Icons.Default.Info`) posicionado com `Box` overlay no canto direito do select
- `AnimatedVisibility(visible = selectedSong != null, enter = fadeIn + scaleIn, exit = fadeOut + scaleOut)`
- `IconButton(onClick = { onSongInfoClick(selectedSong.id) })`
- Tamanho pequeno (20-24dp) para nao cobrir demais o select

### 9.6 Top Tons

Sem alteracao — nao possui `songId`.

### 9.7 Ordem de implementacao

1. Adicionar `onSongClick` callback em `WorshipSongsTableScreen` e `WorshipSongsTableContent`
2. Propagar para `LastSundaysTab` e `TopSongsTab` — tornar titulo clicavel
3. Adicionar `onSongInfoClick` em `RepertoireTab` — icone info overlay
4. Conectar no `WorshipHubNavGraph` com navegacao para `SongsRoutes.detail(songId)`
5. Importar `SongsRoutes` (do `SongsNavGraph`) no NavGraph

---

## 8. Decisoes tecnicas

- **Sem repositorio novo:** use case combina 3 repos existentes. Evita duplicacao.
- **Sem snapshot novo:** lista de Musicas consome `AllSongsSnapshotRepository`. Detalhe cruza multiplos snapshots em memoria.
- **Navegacao cruzada flat:** como tudo esta dentro de `worshipHubGraph`, rotas de cifra/letra sao acessiveis diretamente sem deep link complexo.
- **Dialog para multiplas cifras:** AlertDialog simples, nao BottomSheet (menor complexidade, dados sao poucos — tom + instrumento por cifra).
- **Calculo client-side:** `playCount`, `tones` e `lastSundays` sao derivados de `SongsBySunday` ja em cache. Sem roundtrip extra.
- **Novos campos com default:** `songId` e `youtubeLink` adicionados no final das data classes com default (0 e null) para manter compatibilidade com construtores existentes nos testes.
- **Icone YouTube:** usa `Icons.Filled.PlayArrow` (Material Icons) pois `ic_youtube` nao existe no projeto. Pode ser substituido por drawable custom futuramente.
- **Ultimos domingos:** `take(3)` direto (API ja retorna cronologicamente, mais recente primeiro). Sem reversed.
- **Navegacao Tables→SongDetail:** callback lambda chain, nao CompositionLocal. Simples, explicito, testavel. Todas tabs dentro de `worshipHubGraph`, rota acessivel diretamente.
- **Icone info Repertorio:** overlay com `AnimatedVisibility` sobre select. Nao usa gesto novo — e tap separado em area distinta. Sem conflito com select (tap) e fixar (long press).
- **Busca de Ultimos Domingos no ViewModel:** `SongsTableViewModel` guarda a query (`searchQuery`, imediata, alimenta o campo) e expoe `filteredSundays`. `SearchSundaysUseCase.index()` normaliza data/titulo/artista/tom uma vez por emissao do snapshot; a cada busca so roda `contains` nos campos ja normalizados. Query com `debounce` de 150 ms (0 quando vazia, padrao do `HymnalViewModel`), `mapLatest` cancela filtro obsoleto, tudo em `@DefaultDispatcher`. `lastSundays` segue sem filtro (usado no tom sugerido do Repertorio). `LastSundaysTab` so desenha, com `key` por data.
- **Busca na letra (Cifras/Letras):** `ContentSearch` (`worshiphub/shared/domain`, Kotlin puro) indexa o conteudo uma vez por emissao do snapshot (`map` antes do `combine` no ViewModel): tira acordes `[..]` e linhas de diretiva `{..}`, guarda cada linha original junto da versao normalizada (`normalize()` + lowercase) e o texto inteiro normalizado. A cada tecla so roda `contains` por palavra em strings ja preparadas, por isso nao ha debounce (o custo e de poucos ms com o catalogo inteiro). O chip vive no ViewModel (`_searchLyrics`, `MutableStateFlow`), unido a query e ao acesso num unico flow para caber no `combine` de 5. O item ganha `lyricsSnippet: String?`; `SongContentListScreen` recebe `searchLyrics`/`onSearchLyricsChange` (nulo = sem chip, como na lista de Musicas) e desenha o trecho no card.
- **`normalize()` com Regex constantes:** os padroes ficam em `private val` no arquivo; antes eram compilados a cada chamada, o que travava qualquer busca que normaliza listas grandes.
