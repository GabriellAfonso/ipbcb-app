# Worship Hub — Spec

O Worship Hub (Min. Louvor) e o hub central do ministerio de louvor da igreja. Concentra todas as ferramentas relacionadas a musicas: tabelas de historico, cifras, letras, e catalogo de musicas.

---

## 1. Estrutura do Hub

Tela principal com grid de botoes 2x2 (expansivel):

| Posicao | Botao     | Icone           | Destino                |
|---------|-----------|-----------------|------------------------|
| 1       | Tabelas   | `ic_table`      | Tela de tabelas (tabs) |
| 2       | Musicas   | `ic_songs`      | Lista de musicas       |
| 3       | Cifras    | `ic_chord_chart` | Lista de cifras       |
| 4       | Letras    | `ic_lyrics`     | Lista de letras        |

Navegacao: `CoreScreen → WorshipHubScreen → [Tabelas | Musicas | Cifras | Letras]`

---

## 2. Tabelas

Tela com 4 tabs horizontais e busca global via FAB.

### 2.1 Ultimos Domingos

Lista agrupada por data (`dd/MM/yyyy`). Cada domingo mostra as musicas tocadas com posicao, titulo, artista e tom.

**Click no titulo:** titulo da musica e clicavel (ripple padrao Material). Navega para `SongDetailScreen` usando `songId`. Apenas o texto do titulo e clicavel, nao a row inteira.

**Busca:** filtra por data, titulo, artista ou tom (accent-insensitive via `normalize()`).

**Dados:** `GET songs-by-sunday/`
```
[{
  "date": "dd/MM/yyyy",
  "songs": [{ "position": int, "song": string, "song_id": int, "artist": string, "tone": string }]
}]
```

### 2.2 Mais Tocadas

Ranking de musicas por numero de vezes tocadas aos domingos. Lista ordenada por `play_count` decrescente.

**Click no titulo:** mesmo comportamento de Ultimos Domingos — titulo clicavel com ripple, navega para `SongDetailScreen` usando `songId`.

**Dados:** `GET top-songs/`
```
[{ "song__title": string, "song_id": int, "play_count": int }]
```

### 2.3 Top Tons

Ranking global de tons mais utilizados. Lista ordenada por `tone_count` decrescente.

**Dados:** `GET top-tones/`
```
[{ "tone": string, "tone_count": int }]
```

### 2.4 Repertorio (Sugestoes)

Gera sugestao de 4 musicas para o proximo domingo, priorizando musicas nao tocadas nos ultimos 90 dias. Permite fixar musicas em posicoes especificas e re-gerar as demais.

**Tom automatico:** ao selecionar uma musica, o tom mais usado historicamente para aquela musica e preenchido automaticamente (calculado client-side a partir de `songsBySunday`).
O seletor de tom oferece os 12 tons (naturais e sustenidos: C, C#, D, D#, E, F, F#, G, G#, A, A#, B — `MUSIC_TONES`, core).

**Catalogo ao abrir:** entrar na aba Repertorio atualiza o catalogo de musicas (`GET songs/`), para que
uma musica recem-cadastrada apareca no select sem reiniciar o app. Falha e silenciosa: o select continua com o
catalogo que ja estava carregado. O catalogo e um so no app (`SongsRepositoryImpl` e `@Singleton`): lista de
musicas, repertorio, cifras, letras e o registro de domingo (admin) leem a mesma instancia em memoria.

**Icone de detalhe:** quando uma musica esta selecionada no select, aparece icone `(i)` flutuante sobrepondo o canto direito do select (overlay). Tap no icone navega para `SongDetailScreen` usando `songId`. Icone aparece com `AnimatedVisibility` (fade+scale) e some quando select esta vazio. Nao conflita com tap (selecionar) nem long press (fixar).

**Rascunho local (spec 011):** as 4 linhas (musica, tom, fixada) ficam salvas no aparelho a cada mudanca
(escolha a mao, tom, fixar/desafixar, "Gerar") e voltam ao reabrir a tela ou o app. Expiram 1h depois da
ultima mudanca (TTL deslizante); a expiracao so e checada ao restaurar, nunca esvazia uma tela aberta, e
restaurar nao conta como mudanca. Musica que saiu do catalogo volta como linha vazia; a restauracao espera o
catalogo carregar. "Limpar repertorio" zera as linhas e o rascunho. O rascunho e so do aparelho: nao vai ao
servidor, nao e preenchido a partir do repertorio salvo e continua depois do logout. Guardado no DataStore
`@SetlistPrefs` (chaves `repertoire_draft_v1` e `repertoire_draft_updated_at`) por `RepertoireDraftStorage`;
regras em `DraftExpiry` e `RestoreRepertoireDraftUseCase`, tempo via `WallClock` (core).

**Botoes:** "Gerar" | "Salvar" | "Compartilhar". "Compartilhar" (texto via Intent) e sempre disponivel,
com ou sem salvar. "Salvar" so aparece com `can_save_setlist` do perfil (`ObserveWorshipAccessUseCase`, core) e
fica desabilitado sem linha preenchida, com linha preenchida sem tom ou com tom acima de 3 caracteres
(`RepertoireValidation`), ou durante o envio. Ao tocar, um dialogo confirma "Salvar repertorio de domingo
dd/MM?" — a data e hoje se for domingo, senao o proximo domingo (`setlistDateFor`, core). Envia so as linhas
preenchidas, cada uma na sua posicao. Salvar de novo na mesma data substitui. Resultado em snackbar
(`RepertoireEvent`): "Repertorio de domingo dd/MM salvo." ou o texto do erro (`RepertoireTexts`: sem
permissao, `detail` do servidor no 400, N musicas nao encontradas no 404, sem conexao, generico). No sucesso,
o repertorio devolvido vira na hora o "Repertorio de domingo" do aparelho (secao 4.1), sem esperar o push.
Falha nunca altera o rascunho.

**Dados:** `GET suggested-songs/?fixed=1:12,3:45`
```
[{
  "id": int,
  "song": { "id": int, "title": string, "artist": string },
  "date": "dd/MM/yyyy",
  "tone": string,
  "position": int
}]
```

### 2.5 Busca Global (Tabelas)

- FAB circular no canto inferior direito
- Abre barra de busca animada acima das tabs
- Busca accent-insensitive usando `normalize()`
- Ativa apenas na tab "Ultimos Domingos" (demais tabs nao filtram)

---

## 3. Musicas

### 3.1 Lista de Musicas

Lista de todas as musicas cadastradas no sistema. Cada item mostra titulo e artista.

**Busca:** campo de busca no topo, filtra por titulo e artista (accent-insensitive via `normalize()`).

**Tela:** mesma `SongContentListScreen` de Cifras e Letras — nao e "mesmo padrao visual", e o
mesmo componente. Musicas passa `onTogglePin = null` (sem pin) e nao passa `canEdit`, entao nao
tem nem o marcador de fixado nem o menu de adicionar. O artista vai como chip unico; artista em
branco nao vira chip. Estados de loading, erro e vazio: identicos aos da secao 4.1.

**Dados:** reutiliza `GET songs/` (snapshot ja existente em `AllSongsSnapshotRepository`).

```
[{
  "id": int,
  "title": string,
  "artist": string,
  "category": string,
  "youtube_link": string | null
}]
```

> `youtube_link` e campo novo adicionado ao endpoint `songs/`. Pode ser `null` ou string vazia quando a musica nao tem link.

**Sem snapshot proprio** — consome dados dos snapshots ja existentes (AllSongs, SongsBySunday, ChordCharts, Lyrics).

### 3.2 Tela de Detalhe da Musica

Tela com todas as informacoes consolidadas de uma musica. Dados vem de multiplas fontes cruzadas por `songId`.

#### Layout

**Header:**
- Nome da musica (titulo principal)
- Artista (subtitulo)

**Estatisticas:**
- Vezes tocada aos domingos (contagem calculada client-side a partir de `songsBySunday`)
- Tons utilizados (lista de tons distintos extraida client-side de `songsBySunday`, filtrada por `songId`)
- Ultimo(s) domingo(s) em que foi tocada (ate 3, depende do espaco na tela)

**Acoes (botoes):**
- **Cifra** — navega para tela de cifra existente:
  - Se 1 cifra registrada: navega direto para `ChordChartDetailScreen`
  - Se multiplas cifras: abre dialog/bottom sheet para escolher (mostra tom + instrumento de cada)
  - Se nenhuma cifra: botao desabilitado (visualmente claro que esta indisponivel)
- **Letra** — navega para `LyricsDetailScreen`:
  - Se tem letra: navega direto
  - Se nao tem: botao desabilitado
- **YouTube** — abre link externo no navegador/app do YouTube:
  - Se `youtube_link` presente: abre via `Intent(ACTION_VIEW, uri)`
  - Se `youtube_link` null/vazio: botao desabilitado

#### Fontes de dados (todas client-side)

| Dado | Fonte | Relacao |
|------|-------|---------|
| nome, artista, categoria, youtube_link | `AllSongs` snapshot | direto por `songId` |
| vezes tocada | `SongsBySunday` snapshot | filtrar itens por `songId`, contar |
| tons utilizados | `SongsBySunday` snapshot | filtrar por `songId`, coletar tons distintos |
| ultimos domingos | `SongsBySunday` snapshot | filtrar por `songId`, pegar ultimas datas |
| tem cifra(s)? | `ChordCharts` snapshot | filtrar por `songId` |
| tem letra? | `Lyrics` snapshot | filtrar por `songId` |

---

## 4. Cifras

### 4.1 Lista de Cifras

Lista de todas as cifras cadastradas. Cada item mostra nome da musica, tom e instrumento.

**Estados (compartilhados com Musicas e Letras via `SongContentListScreen`):**

| Estado | Renderizacao |
|--------|--------------|
| `isLoading` e lista vazia | Spinner + "Carregando..." |
| `error != null` e lista vazia | Mensagem do erro + botao "Tentar novamente" (dispara `onRefresh`) |
| Lista vazia sem loading/erro | "Nenhum resultado" |
| Lista com itens | `LazyColumn` de cards |

Os tres estados sem itens sao renderizados dentro de um container com `verticalScroll`, porque
o `PullToRefreshBox` so recebe o gesto de puxar se o filho despachar nested scroll — sem isso a
tela vazia ficaria sem nenhuma forma de recarregar.

**Busca:** campo no topo, filtra por nome da musica (accent-insensitive).

**Pinned:** musicas podem ser fixadas no topo da lista via `SetlistPreferences`. Ordem de exibicao: pinned primeiro (na ordem de pin), depois o resto.

**Repertorio de domingo (spec 011):** so para membros do Louvor (`is_worship_member`). Acima da lista vem a
secao "Repertorio de domingo dd/MM" com as musicas do repertorio guardado no aparelho (core
`SundaySetlistRepository`), na ordem das posicoes e com o tom do repertorio como chip. Musicas sem cifra
(ou, em Letras, sem letra) nao aparecem. Uma musica da secao nao se repete nos fixados nem no resto da lista.
Com busca ativa a secao some e as musicas dela voltam para a lista filtrada. A secao aparece desde que o
repertorio chega ate o fim do domingo dele (`ObserveSundaySetlistUseCase`), e funciona offline. Tap abre o
detalhe como qualquer item. Montada por `buildSundaySection` (`worshiphub/shared/domain`) e desenhada por
`SongContentListScreen(sundaySection = ...)`. Os fixados manuais continuam como antes, separados. O
pull-to-refresh (de Cifras e de Letras) tambem rele o repertorio (`SyncSundaySetlistUseCase`), junto com o
catalogo: e o gesto que se tenta quando a secao nao apareceu. Falha nessa leitura e silenciosa, como na
abertura do app.

**Dados:** `GET chord-charts/`
```
[{
  "id": int,
  "song_id": int,
  "content": string,
  "tone": string,
  "instrument": string,
  "updated_at": string
}]
```

Nome da musica vem do cruzamento com `AllSongs` por `song_id`.

### 4.2 Criacao de Cifra (`songs` ≥ `manage`)

Na tela de lista de cifras, quem tem `manage` ou `owner` no escopo `songs` (Admin, Lideranca) ve menu overflow (⋮) na TopBar com item "Nova Cifra". Tap navega para tela `ChordChartCreateScreen`.

**Tela de criacao:**
- Campo de busca de musica (filtra `AllSongs` em tempo real, accent-insensitive)
- Selecionar musica da lista preenche o campo e fecha a lista
- Campos de tom e instrumento (singleLine)
- Campo multiline para digitar a cifra (fonte monospace)
- Com o teclado aberto, a tela encolhe para caber acima dele: o campo da cifra e o botao "Salvar" ficam
  visiveis, e o trecho sendo digitado nunca fica atras do teclado
- Botao "Salvar" (desabilitado se musica, tom, instrumento ou cifra vazios)
- Apos salvar com sucesso, volta automaticamente para a lista

**Envio:** `POST api/chord-charts/` com `{"song_id": int, "content": string, "tone": string, "instrument": string}` via API autenticada (escopo `songs`, nivel `manage`).

### 4.3 Detalhe da Cifra

Exibe cifra em formato ChordPro parseado. Conteudo dividido em blocos (Intro, Verso, Coro, etc.) com acordes posicionados acima das letras correspondentes.

**Parser:** `ChordProParser` converte string ChordPro em `List<ChordBlock>`, cada bloco com titulo e linhas de `ChordLine` contendo `LineToken.Chord` e `LineToken.Lyrics`.

**Paginacao:** `BlockPaginator` divide blocos em paginas que cabem na tela, com navegacao por swipe/botoes.

**Edicao (`songs` ≥ `manage`):** quem pode editar ve menu overflow (⋮) na TopBar. Menu normal: "Editar". Em modo edicao: "Salvar" e "Cancelar". Conteudo vira `TextField` editavel com fonte monospace. Apenas `content` e editavel (nao tom/instrumento). Salvar envia `PATCH api/chord-charts/{id}/` com `{"content": "..."}` via API autenticada. Com o teclado aberto, o campo encolhe para caber acima dele — o trecho sendo editado nunca fica atras do teclado, mesmo no fim do texto.

---

## 5. Letras

### 5.1 Lista de Letras

Lista de todas as letras cadastradas. Cada item mostra nome da musica.

**Busca:** campo no topo, filtra por nome da musica (accent-insensitive).

**Pinned:** mesmo mecanismo de `SetlistPreferences` das cifras.

**Repertorio de domingo:** mesma secao das cifras (4.1), com as musicas que tem letra.

**Estados:** identicos aos da lista de cifras (secao 4.1) — mesma `SongContentListScreen`.

**Dados:** `GET lyrics/`
```
[{
  "id": int,
  "song_id": int,
  "content": string,
  "updated_at": string
}]
```

Nome da musica vem do cruzamento com `AllSongs` por `song_id`.

### 5.2 Criacao de Letra (`songs` ≥ `manage`)

Na tela de lista de letras, quem tem `manage` ou `owner` no escopo `songs` ve menu overflow (⋮) na TopBar com item "Nova Letra". Tap navega para tela `LyricsCreateScreen`.

**Tela de criacao:**
- Campo de busca de musica (filtra `AllSongs` em tempo real, accent-insensitive)
- Selecionar musica da lista preenche o campo e fecha a lista
- Campo multiline para digitar a letra (fonte monospace)
- Com o teclado aberto, a tela encolhe para caber acima dele (mesmo comportamento da criacao de cifra)
- Botao "Salvar" (desabilitado se musica ou letra nao selecionada)
- Apos salvar com sucesso, volta automaticamente para a lista

**Envio:** `POST api/lyrics/` com `{"song_id": int, "content": string}` via API autenticada (escopo `songs`, nivel `manage`).

### 5.3 Detalhe da Letra

Exibe letra dividida em estrofes. `LyricsParser` separa o texto em `List<LyricsStanza>`, cada estrofe com suas linhas.

**Edicao (`songs` ≥ `manage`):** mesmo mecanismo de edicao das cifras. Menu overflow (⋮) com "Editar"/"Salvar"/"Cancelar". Apenas `content` editavel, e o campo fica acima do teclado como na cifra. Salvar envia `PATCH api/lyrics/{id}/` com `{"content": "..."}` via API autenticada.

---

## 6. Modelos de Dominio

### Song
```
id: Int
title: String
artist: String
categoryName: String
youtubeLink: String?        ← novo
```

### SundaySet
```
date: String                // "dd/MM/yyyy"
songs: List<SundaySetItem>
```

### SundaySetItem
```
position: Int
title: String
artist: String
tone: String
songId: Int                 ← novo (antes relacionava por title)
```

### TopSong
```
title: String
playCount: Int
songId: Int                 ← novo
```

### TopTone
```
tone: String
count: Int
```

### SuggestedSong
```
id: Int
songId: Int
title: String
artist: String
date: String
tone: String
position: Int
```

### ChordChart
```
id: Int
songId: Int
content: String             // formato ChordPro
tone: String
instrument: String
```

### Lyrics
```
id: Int
songId: Int
content: String             // texto puro
```

---

## 7. Navegacao

```
worshipHubGraph (AppRoutes.WORSHIP_HUB_GRAPH)
├── WorshipHubScreen (hub com botoes)
├── Tables (tela unica, inline)
├── songsGraph (sub-graph novo)
│   ├── SongsListScreen (lista com busca)
│   └── SongDetailScreen (detalhe da musica)
├── chordChartsGraph (sub-graph existente)
│   ├── ChordChartsScreen (lista com busca)
│   ├── ChordChartCreateScreen (criacao de cifra — `songs` ≥ `manage`)
│   └── ChordChartDetailScreen (detalhe da cifra)
└── lyricsGraph (sub-graph existente)
    ├── LyricsScreen (lista com busca)
    ├── LyricsCreateScreen (criacao de letra — `songs` ≥ `manage`)
    └── LyricsDetailScreen (detalhe da letra)
```

Navegacao entre features a partir de SongDetailScreen:
- Botao Cifra → navega para `ChordChartDetailScreen` (rota existente dentro de `chordChartsGraph`)
- Botao Letra → navega para `LyricsDetailScreen` (rota existente dentro de `lyricsGraph`)
- Botao YouTube → `Intent(ACTION_VIEW)` para URL externa

---

## 8. Endpoints (API)

Publicos (`AllowAny`), sem autenticacao:

| Metodo | Path | Descricao |
|--------|------|-----------|
| GET | `songs/` | Todas as musicas cadastradas |
| GET | `songs-by-sunday/` | Historico de domingos com musicas tocadas |
| GET | `top-songs/` | Ranking de musicas mais tocadas |
| GET | `top-tones/` | Ranking de tons mais usados |
| GET | `suggested-songs/` | Sugestao de repertorio |
| GET | `chord-charts/` | Todas as cifras |
| GET | `lyrics/` | Todas as letras |

Todos suportam `If-None-Match` / ETag para cache (exceto `suggested-songs`).

Autenticados, escopo `songs` com nivel `manage` (backend 012). O app so mostra criar/editar quando
`canEdit` = `access.allows(Scope.SONGS, AccessLevel.MANAGE)`, lido de `ObserveAccessUseCase` (core §4.2.1) — a
feature nao importa `features/profile`:

| Metodo | Path | Descricao |
|--------|------|-----------|
| POST | `chord-charts/` | Cria nova cifra |
| PATCH | `chord-charts/{id}/` | Atualiza `content` de uma cifra |
| POST | `lyrics/` | Cria nova letra |
| PATCH | `lyrics/{id}/` | Atualiza `content` de uma letra existente |
| PUT | `api/setlists/{date}/` | Salva o repertorio do domingo (`manage` em `songs` + membro do Louvor; spec 011) |

O contrato do repertorio e do backend: `backend/specs/017-sunday-setlist-push/contracts/setlist-api.md`.

`PATCH` body: `{"content": "..."}`. `POST chord-charts/` body: `{"song_id": int, "content": string, "tone": string, "instrument": string}`. `POST lyrics/` body: `{"song_id": int, "content": string}`. Retorna o objeto criado/atualizado. 401 se nao autenticado, 403 `PERMISSION_DENIED` sem `manage` em `songs` (a tela mostra a mensagem e fica), 404 se nao encontrado.

---

## 9. Cache / Offline

Cada fonte de dados usa `JsonSnapshotStorage` com snapshot proprio:

| Snapshot | Repositorio |
|----------|-------------|
| AllSongs | `AllSongsSnapshotRepository` |
| SongsBySunday | `SongsBySundaySnapshotRepository` |
| TopSongs | `TopSongsSnapshotRepository` |
| TopTones | `TopTonesSnapshotRepository` |
| ChordCharts | `ChordChartsSnapshotModule` |
| Lyrics | `LyricsSnapshotModule` |

Feature "Musicas" **nao cria snapshot proprio** — consome dados dos snapshots existentes acima.

---

## 10. Busca

Todas as listas usam busca accent-insensitive via `String.normalize()` (em `core/domain/util/`).

| Tela | Campos buscaveis |
|------|-----------------|
| Ultimos Domingos | data, titulo, artista, tom |
| Musicas (lista) | titulo, artista |
| Cifras | nome da musica |
| Letras | nome da musica |

---

## 11. Itens Futuros

- **Categorias:** campo `categoryName` existe no modelo mas nunca foi implementado como filtro na UI. Potencial filtro por categoria na lista de musicas.
- **Pinned em Musicas:** avaliar se faz sentido ter o mesmo mecanismo de pin de Cifras/Letras na lista de Musicas. Como a lista ja usa a `SongContentListScreen`, basta passar `onTogglePin` e preencher `isPinned`.
