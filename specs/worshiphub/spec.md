# Worship Hub — Spec

O Worship Hub (Louvor) e o hub central do ministerio de louvor da igreja. Concentra todas as ferramentas relacionadas a musicas: tabelas de historico, cifras, letras, e catalogo de musicas.

---

## 1. Estrutura do Hub

Layout "domingo em destaque". Título "Louvor" na barra do `BaseScreen` (logo `ic_worshiphub`, seta de voltar).

**Faixa verde** (`ipbGreen`, igual nos dois temas, cantos inferiores de 28dp), só para membros do Louvor
(`is_worship_member`): o repertório de domingo é só deles, como a seção de Cifras e Letras (seção 4.1). Quem não é
membro (ou está sem login) vê a mesma faixa, sem repertório: "Ministério de Louvor" (pequeno) e "Músicas, cifras e
letras", com os cartões subindo sobre ela como sempre.

- "Repertório de domingo" (pequeno) e "Domingo, dd/MM" — a data do repertório guardado; sem repertório, o domingo
  de `setlistDateFor(hoje)`.
- **Com repertório** (`ObserveSundaySetlistUseCase`, core: vale até o fim do domingo dele, funciona offline): um
  cartão translúcido com uma linha por música, na ordem das posições — posição, "título · artista" (reticências)
  e o selo do tom. Tocar numa música abre a cifra dela (a mesma cifra que a seção do domingo em Cifras escolhe
  para a música); sem cifra, abre o detalhe da música. Embaixo, "Cifras" (pílula branca) e "Letras" (pílula de
  contorno) abrem as listas, que já começam pela seção do domingo. Rodapé "Salvo por {nome} · dd/MM às HH:mm",
  no fuso do aparelho; sem nome (conta apagada), "Salvo em dd/MM às HH:mm"; data ilegível, sem rodapé.
- **Sem repertório:** "Ainda não foi salvo.". Com `can_save_setlist`, o botão "Montar repertório" abre Tabelas
  direto na aba Repertório (seção 2.4).
- **Remover:** com repertório na faixa e `can_save_setlist`, um botão "Remover" (lixeira) ao lado da data. Pede
  confirmação ("Remover repertório de dd/MM?" / "As músicas planejadas para esse domingo serão apagadas.") e apaga
  no servidor (`DELETE api/setlists/{date}/`, o mesmo da gestão — admin spec §2), em qualquer dia: a gestão só
  mostra o domingo depois que ele passa. `404` conta como removido. No sucesso o repertório sai do aparelho na hora,
  o snackbar diz "Repertório de domingo dd/MM removido." e o app relê o repertório atual (pode vir o de um domingo
  seguinte). Falha mantém tudo e mostra "Não foi possível remover o repertório de dd/MM.". Enquanto apaga, o botão
  fica desabilitado com um spinner.
- Abrir o hub relê o repertório em silêncio (`SyncSundaySetlistUseCase`), como o pull-to-refresh das listas.

**Cartões 2x2** (104dp de altura, cantos de 18dp, fundo e sombra do card da home): ícone branco num círculo
`primaryContainer` de 44dp e o rótulo. Sobem 24dp sobre a faixa.

| Posicao | Botao     | Icone            | Desenho | Destino                |
|---------|-----------|------------------|---------|------------------------|
| 1       | Tabelas   | `ic_table`       | tabela (o de sempre) | Tela de tabelas (tabs) |
| 2       | Musicas   | `ic_songs`       | duas notas ligadas, em traço | Lista de musicas |
| 3       | Cifras    | `ic_chord_chart` | diagrama de acorde (trastes, cordas, dedos), em traço | Lista de cifras |
| 4       | Letras    | `ic_lyrics`      | linhas de texto com uma nota, em traço | Lista de letras |

O primeiro quadro já mostra a faixa certa: repertório e permissões estão na memória e o estado começa deles
(`ObserveSundaySetlistUseCase.current`, `ObserveWorshipAccessUseCase.current`), não de um estado vazio. A tela rola
quando não cabe (celular deitado, repertório longo). Estado em `WorshipHubViewModel`
(`hub/presentation`), faixa montada por `buildHubBand` (função pura).

Navegacao: `CoreScreen → WorshipHubScreen → [Tabelas | Musicas | Cifras | Letras]`

---

## 2. Tabelas

Tela com 4 tabs horizontais e busca global via FAB. Quem tem `songs` ≥ `manage` ve menu overflow (⋮) na TopBar
com "Registrar domingo" (§2.6).

**Tab selecionada:** abre em Ultimos Domingos. Ao voltar de `SongDetailScreen` (ou apos rotacao / morte do
processo), a tela reabre na tab de onde o usuario saiu.

**Layout:** do topo para baixo, o mais escuro em cima. As tabs ficam dentro do bloco verde da TopBar (`ipbGreen`,
via `topBarExtension` do `BaseScreen`): texto branco, tab ativa com fundo branco translucido e sublinhado
verde-menta. Abaixo, o conteudo e uma folha com cantos superiores arredondados (`surfaceDim`), e as listas ficam
em cartoes arredondados (`surfaceContainer`). Sem cabecalho de colunas: cada linha tem posicao, titulo em cima e
artista embaixo (quando houver), e o tom num selo (`secondaryContainer`, fonte mono).

### 2.1 Ultimos Domingos

Lista agrupada por data (`dd/MM/yyyy`). Cada domingo tem a data inteira centralizada entre duas linhas finas e,
abaixo, um cartao com as musicas tocadas: posicao, titulo, artista e tom.

**Click na musica:** a linha inteira e clicavel (ripple padrao Material). Navega para `SongDetailScreen` usando
`songId`.

**Busca:** filtra por data, titulo, artista ou tom (accent-insensitive e sem pontuacao via `normalize()` — "07/04" casa com "07/04/2024"). Mostra o domingo inteiro quando qualquer campo casa. O resultado atualiza apos uma pausa curta na digitacao (~150 ms); limpar a busca volta a lista completa na hora. A digitacao e o scroll nunca travam, mesmo com o historico inteiro carregado.

**Dados:** `GET songs-by-sunday/`
```
[{
  "date": "dd/MM/yyyy",
  "songs": [{ "position": int, "song": string, "song_id": int, "artist": string, "tone": string }]
}]
```

### 2.2 Mais Tocadas

Ranking de musicas por numero de vezes tocadas aos domingos. Lista ordenada por `play_count` decrescente, num
cartao so: posicao, titulo e "N vezes". As 3 primeiras posicoes mostram o numero num circulo verde.

**Click na musica:** mesmo comportamento de Ultimos Domingos — linha inteira clicavel, navega para `SongDetailScreen` usando `songId`.

**Dados:** `GET top-songs/`
```
[{ "song__title": string, "song_id": int, "play_count": int }]
```

### 2.3 Top Tons

Ranking global de tons mais utilizados. Lista ordenada por `tone_count` decrescente, num cartao so: posicao (top 3
em circulo verde), tom num selo, barra proporcional ao mais usado e "N vezes".

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
musicas, repertorio, cifras, letras e o registro de domingo leem a mesma instancia em memoria.

**Layout:** titulo "Repertorio" com os icones "Limpar repertorio" e "Compartilhar" a direita; abaixo, um cartao
com as 4 posicoes no mesmo formato de Ultimos Domingos (posicao, titulo/artista, selo de tom) mais um alfinete no
fim da linha. Posicao vazia mostra "Escolher musica" e o selo "Tom" tracejado. Uma dica embaixo do cartao: "Toque
na musica para trocar e no tom para mudar." No rodape, preso: "Gerar" (tonal) e "Salvar" (verde). O FAB de busca
nao aparece nesta tab.

**Escolher musica:** tocar no titulo abre, logo abaixo da linha, a busca (titulo ou artista, accent-insensitive)
e a lista do catalogo (titulo em cima, artista embaixo; a atual destacada). Escolher fecha a busca. Tocar de novo
no titulo fecha sem trocar.

**Tom:** tocar no selo abre um menu com os 12 tons. Desabilitado em posicao vazia.

**Icone de detalhe:** com a busca aberta e uma musica escolhida, um icone `(i)` ao lado do campo de busca navega
para `SongDetailScreen` usando `songId`.

**Alfinete:** fixa/desafixa a posicao (regras em "Fixar", abaixo). Desabilitado em posicao vazia. Linha fixada
fica com fundo verde leve e alfinete verde.

**Rascunho local (spec 011):** as 4 linhas (musica, tom, fixada, execucao sugerida) ficam salvas no aparelho a cada mudanca
(escolha a mao, tom, fixar/desafixar, "Gerar") e voltam ao reabrir a tela ou o app. Expiram 1h depois da
ultima mudanca (TTL deslizante); a expiracao so e checada ao restaurar, nunca esvazia uma tela aberta, e
restaurar nao conta como mudanca. Musica que saiu do catalogo volta como linha vazia; a restauracao espera o
catalogo carregar. "Limpar repertorio" zera as linhas e o rascunho. O rascunho e so do aparelho: nao vai ao
servidor, nao e preenchido a partir do repertorio salvo e continua depois do logout. Guardado no DataStore
`@SetlistPrefs` (chaves `repertoire_draft_v1` e `repertoire_draft_updated_at`) por `RepertoireDraftStorage`;
regras em `DraftExpiry` e `RestoreRepertoireDraftUseCase`, tempo via `WallClock` (core).

**Botoes:** "Gerar" | "Salvar" | "Compartilhar". "Compartilhar" (texto via Intent) e sempre disponivel
quando ha musica escolhida, com ou sem salvar. Sem "Salvar", "Gerar" ocupa o rodape inteiro. "Salvar" so aparece com `can_save_setlist` do perfil (`ObserveWorshipAccessUseCase`, core) e
fica desabilitado sem linha preenchida, com linha preenchida sem tom ou com tom acima de 3 caracteres
(`RepertoireValidation`), ou durante o envio. Ao tocar, um dialogo confirma "Salvar repertorio de domingo
dd/MM?" — a data e hoje se for domingo, senao o proximo domingo (`setlistDateFor`, core). Envia so as linhas
preenchidas, cada uma na sua posicao. Salvar de novo na mesma data substitui. Resultado em snackbar
(`RepertoireEvent`): "Repertorio de domingo dd/MM salvo." ou o texto do erro (`RepertoireTexts`: sem
permissao, `detail` do servidor no 400, N musicas nao encontradas no 404, sem conexao, generico). No sucesso,
o repertorio devolvido vira na hora o "Repertorio de domingo" do aparelho (secao 4.1), sem esperar o push.
Falha nunca altera o rascunho.

**Sem repetir ao gerar de novo:** o app guarda, por posicao livre, as musicas (`song.id`) que "Gerar" ja
mostrou ali e manda essa lista em `exclude`, para o backend nao sortear de novo. So vale para posicoes
livres: fixar uma posicao descarta a lista dela; desafixar comeca a lista so com a musica atual. Quando a
resposta traz para uma posicao uma musica que ja estava na lista dela, as opcoes daquela posicao acabaram (o
backend ignora a exclusao e sorteia normalmente): a lista da posicao recomeca so com essa musica. A lista vive
so enquanto a tela esta aberta — sair da tela ou "Limpar repertorio" zera; nao vai ao rascunho nem ao disco.
Regras em `ShownSuggestions` (domain). Backend sem o param apenas o ignora.

**Fixar:** cada linha que veio do "Gerar" guarda o `id` do item sugerido (a execucao, Played); escolher
musica a mao apaga esse id. Linha fixada com id vai em `fixed` — o backend a mantem e nao sorteia a mesma
musica nas outras posicoes. Linha fixada sem id (escolhida a mao) nao tem execucao para mandar: o `song.id`
dela vai em `exclude` de todas as posicoes livres. Se o backend tiver de ignorar essa exclusao por falta de
opcoes, a musica pode repetir no repertorio.

**Dados:** `GET suggested-songs/?fixed=1:12,3:45&exclude=2:7,2:9,3:30`
- `fixed=posicao:id` — `id` do item (Played).
- `exclude=posicao:songId` — `song.id` do item; posicao pode repetir. Exclusao em posicao fixada e ignorada
  pelo backend; o app nem envia. Backend conta so os primeiros 200 pares.
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

- FAB circular no canto inferior direito (escondido na tab Repertorio)
- Abre barra de busca animada no topo da folha de conteudo, abaixo das tabs
- Busca accent-insensitive usando `normalize()`
- Ativa apenas na tab "Ultimos Domingos" (demais tabs nao filtram)

### 2.6 Registrar domingo (`songs` ≥ `manage`)

Registra as musicas tocadas num culto de domingo (`POST api/played/register/`). Tela `SundayRegistrationScreen` +
`SundayRegistrationViewModel` (`worshiphub/register/`), rota `AppRoutes.SUNDAY_REGISTER`
(`sunday_register?date=YYYY-MM-DD`, data opcional) registrada em `worshipHubGraph`. A rota fica em `AppRoutes`
porque a administracao tambem abre a tela (admin spec §3).

Entradas:
- Menu ⋮ das Tabelas → "Registrar domingo": sem data. Voltar cai nas Tabelas.
- Card "Confirmar músicas de domingo" do painel de gestao e notificacao "Confirmar músicas de domingo": com a data
  do domingo pendente (pre-preenchido, abaixo). Voltar cai no painel.

**Tela:** titulo "Registrar domingo", campo de data (abre o seletor; hoje por padrao) e as musicas tocadas em
linhas ordenadas (no minimo 4; "adicionar mais" cria a 5a em diante, que pode ser removida). Cada linha tem musica
(busca no catalogo; vazio mostra "Nª música", ex. "1ª música" — sem numero solto ao lado) e tom, que sai de uma lista com os 12 tons (`MUSIC_TONES`, core). O botao "Enviar ao servidor"
so habilita com data, nenhuma linha incompleta (musica sem tom ou tom sem musica) e ao menos uma linha completa.
Linha incompleta mostra "Posição N incompleta: preencha música e tom.".

- Sucesso: snackbar "Enviado com sucesso." e Ultimos Domingos, Mais tocadas e Top tons sao relidos
  (`RefreshPlayedTablesUseCase`), entao as Tabelas ja mostram o domingo ao voltar. Falha dessa releitura e
  silenciosa.
- Falha de validacao no envio: "Existem posições incompletas. Corrija antes de enviar." e as linhas marcadas.
- Outras falhas: a mensagem do erro (ou "Erro inesperado ao enviar.").

**Catalogo ao abrir:** toda vez que a tela abre, o catalogo de musicas e atualizado (`GET songs/`), para que uma
musica recem-cadastrada apareca nas linhas sem reiniciar o app. Falha mantem o catalogo ja carregado; sem catalogo
nenhum, "Falha ao carregar músicas.".

**Domingo pre-preenchido (spec 011):** com data, a tela abre com a data fixa (o seletor de data nao abre) e le o
repertorio desse domingo (`GET api/setlists/{date}/`, `SetlistConfirmationRepository`, core §4.2.4): uma linha por
item na ordem das posicoes (no minimo as 4 de sempre), com musica e tom, todas editaveis — o que e registrado e o
que a tela mostra. 404 deixa as linhas vazias com o aviso "Repertório de dd/MM não encontrado. Preencha as
músicas."; outra falha mostra o erro com "Tentar novamente" sem bloquear o preenchimento manual.

Eventos de uma vez (sucesso, erro de envio) saem por `SundayRegistrationEvent`.

---

## 3. Musicas

### 3.1 Lista de Musicas

Lista de todas as musicas cadastradas no sistema. Cada item mostra titulo e artista.

**Busca:** campo de busca no topo, filtra por titulo e artista (accent-insensitive via `normalize()`).

**Tela:** mesma `SongContentListScreen` de Cifras e Letras — nao e "mesmo padrao visual", e o
mesmo componente. Musicas passa `onTogglePin = null` (sem pin), entao nao tem o marcador de fixado. Quem
tem `songs` ≥ `manage` (`canEdit`) ve o menu overflow (⋮) com "Adicionar Música" (§3.3). O artista vai como chip unico; artista em
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

**Texto copiavel:** header e estatisticas (nome, artista, contagem, tons e datas) ficam dentro de um
`SelectionContainer`: long press seleciona e abre o menu de copiar do sistema. Os botoes de acao nao entram.

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

#### Edicao (`songs` ≥ `manage`)

Quem tem `manage` ou `owner` no escopo `songs` ve menu overflow (⋮) na TopBar. Menu normal: "Editar" (e
"Excluir", so para `owner`). Em modo edicao: "Salvar" e "Cancelar" — mesmo mecanismo de cifras e letras.

**Modo edicao:** a tela troca header, estatisticas e botoes por um formulario com tres campos, ja preenchidos com
os valores atuais: **Nome**, **Artista** e **Link do YouTube** (vazio quando a musica nao tem link). Categoria nao
e editavel (o backend nao aceita). Com o teclado aberto o formulario rola e o campo em foco fica acima dele.

**Validacao no app** (antes de enviar; mesmas regras do backend). Valores sao comparados e enviados sem espacos nas
pontas:
- Nome e artista: obrigatorios, ate 100 caracteres — "Informe o nome." / "Informe o artista." /
  "Use até 100 caracteres."
- Link: vazio (remove o link) ou URL `http(s)` com ate 200 caracteres — "Link inválido. Use um endereço http(s)."
  / "Use até 200 caracteres."

O erro aparece embaixo do proprio campo e some quando o campo e editado. Com erro, nada e enviado.

**Salvar:** se nada mudou, so sai do modo edicao, sem chamada. Senao envia `PATCH api/songs/{id}/` com
`{"title", "artist", "youtube_link"}` (link vazio = `""`) via API autenticada. Durante o envio, spinner no topo do
formulario e "Salvar"/"Cancelar" desabilitados. No sucesso sai do modo edicao e atualiza `songs/`,
`songs-by-sunday/` e `top-songs/`, que repetem o titulo da musica. Falhas ficam no modo edicao, com a mensagem
acima dos campos:

| Resposta | Mensagem |
|----------|----------|
| `409` | "Já existe uma música com esse nome e artista." |
| `400` com `field_errors` | erro do app no campo correspondente (`title`, `artist`, `youtube_link`) |
| `403` | "Você não tem permissão para editar músicas." |
| `404` | "Essa música não existe mais." |
| rede / outros | mensagem generica (`toUserMessage()`) |

#### Exclusao (`songs` = `owner`)

"Excluir" abre dialog de confirmacao: titulo "Excluir música?", texto
"“{nome}” será excluída junto com as cifras e a letra. Essa ação não pode ser desfeita.", botoes "Excluir" e
"Cancelar". Confirmar envia `DELETE api/songs/{id}/` via API autenticada; durante o envio a tela mostra spinner.

- **`204`:** atualiza `songs/`, `chord-charts/` e `lyrics/` (o backend apaga cifras e letra junto) e volta para a
  tela anterior.
- **`409`** (musica tocada em algum domingo ou presente numa setlist): dialog "Não foi possível excluir" com o
  `detail` do backend, que ja explica o motivo em pt-BR (ex.: "A música "Oceans" não pode ser excluída: foi tocada
  em 7 domingos…"). Sem `detail`: "Essa música já foi usada em domingos ou setlists e não pode ser excluída."
- **`403`:** "Você não tem permissão para excluir músicas." · **`404`:** "Essa música não existe mais." ·
  rede / outros: mensagem generica — todos no mesmo dialog. A musica continua na tela.

### 3.3 Criacao de Musica (`songs` ≥ `manage`)

"Adicionar Música" no menu da lista navega para `SongCreateScreen` ("Nova Música", `SongCreateViewModel`).

**Tela:** campos "Título" e "Artista", os dois obrigatorios (o botao "Salvar" so habilita com os dois
preenchidos). Salvar faz `POST api/songs/` com `{ title, artist }` ja sem espacos nas pontas
(`RegisterSongUseCase`, `WorshipRegisterRepository`). Categoria e link do YouTube nao entram pelo app (o link se
edita depois no detalhe, §3.2).

- Sucesso: o catalogo e relido e a tela volta para a lista, que ja mostra a musica (assim como o registro de
  domingo e o Repertorio).
- `409` (mesmo titulo e artista, ignorando maiusculas): "Essa música já está cadastrada." O form fica como esta.
- `400` (campo vazio, ou com mais de 100 caracteres): "Título ou artista inválido. Use até 100 caracteres."
- Outras falhas (rede, `403` sem `songs` `manage`, 5xx): o texto generico da categoria do erro.
- O erro aparece acima do botao e some ao editar um campo. Enquanto salva, os campos travam e o botao vira spinner.

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

**Busca:** campo no topo, filtra por nome da musica (accent-insensitive). Ver **Busca na letra** (secao 10.1).

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

**Layout "ajuste fino com faixa"** (o mesmo na letra, 5.3):

- Barra verde do `BaseScreen` com o nome da musica e os tres pontos (painel de acoes, abaixo).
- Logo abaixo, uma **faixa verde fina** (`ipbGreen`, cantos inferiores de 20dp): o artista numa linha; embaixo, a
  etiqueta branca "Tom {tom}" e, a direita, o seletor "Paginas | Rolagem" (o selecionado com fundo branco
  translucido).
- Secoes (Verso, Coro...) em etiquetas pequenas em caixa alta (`primary` sobre `secondaryContainer`). Acordes em
  laranja escuro (`#A85F00`) no tema claro e no laranja claro (`#F7B733`) no escuro: o laranja da marca sobre fundo
  claro tinha contraste de 2:1.
- No modo paginas, embaixo, uma barra de progresso fina (`primary`) com "{pagina} de {total}". Toque na metade
  direita vira a pagina, na esquerda volta.

**Modo de leitura:** horizontal (paginas) ou vertical (rolagem), escolhido no seletor da faixa e salvo no aparelho
(`song_scroll_mode`), o mesmo para cifras e letras. A tela ja abre no modo salvo: enquanto a preferencia nao foi
lida, mostra o carregando — nunca um modo provisorio que depois troca.

**Texto copiavel:** o conteudo (acordes e letra), nos modos horizontal e vertical, fica num `SelectionContainer`:
long press seleciona e abre o menu de copiar do sistema; tap nas bordas continua virando a pagina. O cabecalho
(tom, paginas) nao entra.

**Edicao (`songs` ≥ `manage`):** quem pode editar ve os tres pontos na barra; eles abrem o painel de acoes que desce
da barra (o mesmo da galeria, `core/presentation/components/TopActionPanel`), com o nome da musica e "artista · Tom
{tom}" no topo e, no grupo "Gerenciar", "Editar cifra" ("Muda o texto e os acordes"). Sem permissao, os tres pontos
somem. Em modo edicao a barra mostra "Cancelar" e "Salvar". Conteudo vira `TextField` editavel com fonte monospace. Apenas `content` e editavel (nao tom/instrumento). Salvar envia `PATCH api/chord-charts/{id}/` com `{"content": "..."}` via API autenticada. Com o teclado aberto, o campo encolhe para caber acima dele — o trecho sendo editado nunca fica atras do teclado, mesmo no fim do texto.

---

## 5. Letras

### 5.1 Lista de Letras

Lista de todas as letras cadastradas. Cada item mostra nome da musica.

**Busca:** campo no topo, filtra por nome da musica (accent-insensitive). Ver **Busca na letra** (secao 10.1).

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

**Texto copiavel:** a letra e copiavel como na cifra (4.3), nos dois modos de rolagem.

**Layout:** o da cifra (4.3), com o nome da musica na barra (nao mais "Letra" na barra e o nome repetido no corpo) e,
na faixa fina, "A−" / "A+" no lugar do tom: mudam o tamanho do texto de 2 em 2sp, entre 16 e 34sp (comeca em 22). O
tamanho vale enquanto a letra esta aberta; as paginas sao recalculadas com ele.

**Modo de leitura:** o mesmo da cifra (4.3) — compartilhado, salvo no aparelho e ja aplicado ao abrir.

**Edicao (`songs` ≥ `manage`):** mesmo mecanismo de edicao das cifras: painel com "Editar letra" ("Muda o texto");
"Cancelar" e "Salvar" na barra durante a edicao. Apenas `content` editavel, e o campo fica acima do teclado como na cifra. Salvar envia `PATCH api/lyrics/{id}/` com `{"content": "..."}` via API autenticada.

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
├── WorshipHubScreen (hub: faixa do domingo + cartoes)
├── Tables (tela unica, inline; `tab` opcional abre numa aba — o hub abre Repertorio)
├── SundayRegistrationScreen (`AppRoutes.SUNDAY_REGISTER` — `songs` ≥ `manage`)
├── songsGraph (sub-graph novo)
│   ├── SongsListScreen (lista com busca)
│   ├── SongCreateScreen (criacao de musica — `songs` ≥ `manage`)
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
| POST | `api/songs/` | Cadastra musica (`title`, `artist`) |
| PATCH | `api/songs/{id}/` | Edita `title`, `artist`, `youtube_link` de uma musica (backend 018) |
| DELETE | `api/songs/{id}/` | Exclui musica sem uso em domingos/setlists — nivel `owner`; `409` com `detail` se em uso |
| PUT | `api/setlists/{date}/` | Salva o repertorio do domingo (`manage` em `songs` + membro do Louvor; spec 011) |
| POST | `api/played/register/` | Registra as musicas tocadas num domingo (§2.6) |
| GET | `api/setlists/{date}/` | Repertorio que pre-preenche o registro de domingo (spec 011) |

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
| Cifras | nome da musica; com "Buscar na letra", tambem o texto da cifra (sem acordes) |
| Letras | nome da musica; com "Buscar na letra", tambem o texto da letra |

### 10.1 Busca na letra (Cifras e Letras)

Para achar uma musica lembrando so de um trecho. Desligada por padrao: a busca normal continua so pelo nome.

- Com texto no campo de busca aparece, logo abaixo dele, o chip "Buscar na letra". Sem texto o chip nao
  aparece e a tela fica como sempre.
- Ligado, a lista mostra primeiro as musicas cujo nome casa (como antes) e depois as que so casam pelo
  conteudo. Cada uma dessas ultimas mostra, embaixo do nome, a linha da letra onde o trecho foi achado.
- O conteudo casa quando **todas** as palavras digitadas aparecem nele, em qualquer ordem e em qualquer linha
  (accent-insensitive, sem pontuacao, sem diferenciar maiusculas). Assim quem lembra a frase meio errada ainda
  acha. A linha mostrada e a que contem mais palavras da busca (a primeira, no empate).
- Cifras: acordes (`[G]`) e diretivas (`{start_of_chorus}`, `{comment: ...}`) sao ignorados, entao "grande"
  casa com `gran[D]de`.
- O chip e por visita a tela: sair e voltar volta ao padrao (desligado). Limpar o campo esconde o chip mas
  mantem a escolha enquanto a tela estiver aberta.
- Funciona offline: usa o conteudo que ja esta no snapshot local, sem request nova.
- Com a busca ativa a secao "Repertorio de domingo" some, como em qualquer busca.

---

## 11. Itens Futuros

- **Categorias:** campo `categoryName` existe no modelo mas nunca foi implementado como filtro na UI. Potencial filtro por categoria na lista de musicas.
- **Pinned em Musicas:** avaliar se faz sentido ter o mesmo mecanismo de pin de Cifras/Letras na lista de Musicas. Como a lista ja usa a `SongContentListScreen`, basta passar `onTogglePin` e preencher `isPinned`.
