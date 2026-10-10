# Worship Hub — Tasks

Tarefas para implementacao da feature "Musicas". Tabelas, Cifras e Letras ja existem.

---

## 1. DTOs e Modelos

- [x] `AllSongDto`: adicionar `youtubeLink` (`@SerialName("youtube_link")`, default `null`)
- [x] `Song` (domain): adicionar `youtubeLink: String?`
- [x] `AllSongsMapper`: mapear `youtubeLink`
- [x] `TopSongDto`: adicionar `songId` (`@SerialName("song_id")`)
- [x] `TopSong` (domain): adicionar `songId: Int`
- [x] `TopSongsMapper`: mapear `songId`
- [x] `SundaySongDto`: adicionar `songId` (`@SerialName("song_id")`)
- [x] `SundaySetItem` (domain): adicionar `songId: Int`
- [x] `SongsBySundayMapper`: mapear `songId`

## 2. Lista de Musicas

- [x] `SongListItem` — data class (id, title, artist)
- [x] `SongsListUiState` — data class (songs, filteredSongs, query, isLoading, error)
- [x] `SongsListViewModel` — injeta SongsRepository, combina observe + query, filtra com `normalize()`
- [x] `SongsListScreen` — BaseScreen + busca no topo + LazyColumn + pull-to-refresh

## 3. Detalhe da Musica

- [x] `ChordChartOption` — data class (id, tone, instrument)
- [x] `SongDetailUiState` — data class (songName, artist, playCount, tones, lastSundays, chordCharts, hasLyrics, lyricsId, youtubeLink, isLoading, error)
- [x] `GetSongDetailUseCase` — combina SongsRepository + ChordChartRepository + LyricsRepository, calcula stats client-side
- [x] `SongDetailViewModel` — recebe songId via SavedStateHandle, expoe StateFlow
- [x] `SongDetailScreen` — header + stats + botoes (cifra/letra/youtube)
- [x] Dialog de escolha de cifra — AlertDialog quando multiplas cifras existem

## 4. Navegacao

- [x] `SongsNavGraph` — sub-graph com rotas List e Detail
- [x] `WorshipHubNavGraph` — substituir InDevelopmentScreen por `songsGraph(navController)`
- [x] `WorshipHubScreen` — reordenar botoes (Tabelas, Musicas, Cifras, Letras) + `visible = true`
- [x] Navegacao cruzada — SongDetail → ChordChartDetail / LyricsDetail via rotas existentes

## 5. Testes

- [x] `GetSongDetailUseCaseTest` — happy path (musica com tudo), musica sem cifra/letra/youtube, musica nunca tocada
- [x] `SongsListViewModelTest` — lista carrega, busca filtra, busca accent-insensitive
- [x] `SongDetailViewModelTest` — carrega detalhe, estados loading/error

## 6. Navegacao Tables → SongDetail

- [x] `WorshipSongsTableScreen`: adicionar parametro `onSongClick: (Int) -> Unit`
- [x] `WorshipSongsTableContent`: propagar `onSongClick`
- [x] `LastSundaysTab`: receber `onSongClick`, tornar titulo clicavel com ripple
- [x] `TopSongsTab`: receber `onSongClick`, tornar titulo clicavel com ripple
- [x] `RepertoireTab`: receber `onSongInfoClick`, icone info overlay no select (AnimatedVisibility, aparece com musica selecionada)
- [x] `WorshipHubNavGraph`: conectar `onSongClick` → `navController.navigate(SongsRoutes.detail(songId))`
- [x] Previews: atualizar previews das tabs com novo parametro

## 7. Edicao e exclusao de musica (backend 018)

- [x] `SongsEditApi` (`@AuthedRetrofit`): `PATCH api/songs/{id}/` e `DELETE api/songs/{id}/`
- [x] `SongEditValidator` — regras de nome/artista/link no dominio, com mensagens por campo
- [x] `SongEditRepository` + impl — mapeia respostas em `SongWriteResult`; atualiza snapshots no sucesso
- [x] `UpdateSongUseCase` (valida, pula envio sem mudanca) e `DeleteSongUseCase`
- [x] `SongDetailViewModel` — `canEdit`/`canDelete`, modo edicao, salvar, excluir, evento `Deleted`
- [x] `SongDetailScreen` — menu ⋮, formulario de edicao, dialog de confirmacao e de erro da exclusao
- [x] Testes: validator, repository (sucesso, 409, 400, rede), use cases, view model
