# Bíblia — Spec

A Bíblia completa em duas traduções (NAA e ARA), lida 100% offline depois do primeiro download. Não exige
autenticação. Vídeos ligados a capítulos e livros (seção 6) e, com login, progresso de leitura (seção 7).

---

## 1. Telas

| Tela    | Rota                      | Conteúdo                                                        |
|---------|---------------------------|-----------------------------------------------------------------|
| Leitor  | `BibleReader`             | Capítulo da posição atual, fonte ajustável, seleção de versículos |
| Índice  | `BibleIndex?tab={tab}`    | Livros (com busca por nome), capítulos e versículos             |
| Passagem | `AppRoutes.BIBLE_PASSAGE` | O mesmo leitor, aberto num livro/capítulo/versículo vindo de outra feature |
| Locais do vídeo | `BibleVideoPlaces/{videoId}` | Onde um vídeo aparece (gestores; seção 6.3) |

Todas vivem em `bibleGraph`, com `BibleViewModel` escopado ao grafo.

**Lista de livros.** O círculo da abreviação de cada livro tem a cor do seu testamento: `secondaryContainer` no
Antigo (os 39 primeiros livros do índice), `tertiaryContainer` no Novo (os 27 seguintes). Não há faixa nem título
separando os dois — só a cor. A busca por nome abre pelo ícone de lupa na top bar (só na aba Livro): um campo de
fundo preto e texto branco logo abaixo da barra, já com o teclado aberto. Fechar (X ou voltar) ou escolher um livro
apaga o termo.

**Passagem** (`bible_passage?book={book}&chapter={chapter}&verse={verse}&verses={verses}`, montada por
`AppRoutes.biblePassage`): é como outras features (as referências da Doutrina, `specs/studies/spec.md`) abrem a Bíblia
sem importá-la. Na primeira composição da entrada, `openPassage` salva a posição (conta como leitura normal) e deixa o
versículo pendente; o leitor rola até ele quando a tradução está decodificada e a posição atual bate com o pendente, e
então o descarta. Voltar retorna para quem abriu. `book` é o `abbrev` da Bíblia, comparado sem diferenciar maiúsculas.

**Destaque da passagem.** `verses` (opcional) são os intervalos citados, no formato `3-5,7` (`AppRoutes` monta e lê;
trecho malformado é ignorado). Esses versículos do capítulo aberto ficam destacados com um fundo próprio
(`secondaryContainer`), diferente da seleção. O destaque não é seleção: não abre a barra de copiar/compartilhar e não
segura o "voltar". Tocar num versículo destacado o seleciona normalmente (a seleção pinta por cima). Mudar de capítulo
ou de livro (setas, índice) apaga o destaque.

O leitor permite trocar a tradução, avançar/voltar capítulo, ajustar a fonte (14–32, padrão 18) e
copiar/compartilhar os versículos selecionados. A toolbar do leitor é quase preta (`#141414`) nos dois
temas, com ícones brancos e os botões de livro, capítulo e tradução em cinza escuro (`#2B2B2B`, cantos arredondados)
com texto branco em negrito. No tema escuro a página do texto é cinza-escuro (`#1E1E20`), um tom acima da toolbar;
no tema claro segue a cor do tema (`surface`).
Sem a tradução ativa no aparelho, mostra o banner de download (tentar de novo no WiFi ou nos dados móveis).

**Títulos.** Os títulos de seção da tradução (seção 2) aparecem antes do versículo que abrem, sempre (não há opção de
esconder). Três níveis, todos na cor primária e proporcionais à fonte do leitor: nível 1 em negrito e um pouco maior
que o texto, nível 2 em negrito no tamanho do texto, nível 3 em itálico. Vários títulos antes do mesmo versículo saem
na ordem em que vieram (Mt 5:1: "O sermão do monte", depois "As bem-aventuranças"). Título não é versículo: não é
clicável, não entra na seleção nem no copiar/compartilhar. Rolar até um versículo (passagem, índice) mostra também os
títulos que o abrem. Tradução baixada antes dos títulos existirem aparece sem títulos até a atualização (5.2).

**Carregando.** Enquanto o texto do capítulo não chegou, o leitor mostra um placeholder com o mesmo layout do
capítulo: o título real ("Gênesis 1", vindo do índice) e barras pulsando no lugar dos versículos, na altura de
linha da fonte atual. A toolbar já mostra o livro e o capítulo. Quando o texto chega, ocupa o mesmo espaço — nada
pula. Mudar de capítulo rola para o versículo 1 só depois que o texto do novo capítulo está na tela.

## 2. Endpoint

`GET /api/bible/{translation}/` — `translation` = `NAA` | `ARA`. Lista de 66 livros:

```json
[{
  "abbrev": "gn", "name": "Gênesis", "chapters": [["No princípio...", "..."]],
  "headings": [[{ "verse": 1, "level": 1, "title": "A criação do Universo" }]]
}]
```

`chapters[i]` são os versículos do capítulo `i + 1`. Um versículo pode vir como array de partes; o app junta com
espaço. `headings[i]` são os títulos do capítulo `i + 1`: `verse` é o versículo que o título abre, `level` vai de 1
a 3. `headings` pode faltar (tradução sem títulos, ou backend antigo) — vale lista vazia. ~4,2 MB por tradução.

## 3. Armazenamento

Cada tradução vive dividida por livro em `filesDir/bible/{naa,ara}/`:

- `index.json`: os 66 livros na ordem canônica — `abbrev`, `name` e `verseCounts` (versículos por capítulo).
  Poucos KB.
- `00.json` … `65.json`: os capítulos de cada livro (`[["v1", "v2"], ...]`), na posição do livro no índice. Os
  versículos já vêm juntados (sem arrays de partes). O maior (Salmos) tem ~250 KB.
- `00.headings.json` … `65.headings.json`: os títulos de cada livro (`[[{verse, level, title}], ...]`), ao lado do
  arquivo de texto. Sem esse arquivo, o livro não tem títulos.
- `format`: a versão do formato da pasta (hoje `2` = com títulos). Pasta sem ele, ou com versão menor, é do formato
  antigo (só texto): continua legível e entra na atualização (5.2).

A tradução conta como presente quando `index.json` existe. `BiblePreferences` (DataStore) guarda a tradução ativa,
a última posição (livro, capítulo, versículo) e o tamanho da fonte.

**Arquivo antigo.** Versões anteriores gravavam a tradução inteira em `filesDir/snapshots/bible_{naa,ara}.json`
(+ `_etag.txt`). O repositório converte esse arquivo para o formato por livro na primeira checagem do disco e o
apaga, sem baixar de novo; se a conversão falhar, apaga do mesmo jeito e a tradução conta como ausente. A pasta
convertida fica no formato antigo (o arquivo antigo não tem títulos).

## 4. Repositório (`BibleRepository`, `@Singleton`)

Expõe `booksFlow` (índice da tradução ativa), `currentBookFlow` (texto do livro da posição atual),
`activeTranslationFlow`, `cachedTranslationsFlow` (traduções presentes no disco), `outdatedTranslationsFlow`
(presentes, mas no formato antigo), `positionFlow` e `fontSizeFlow`.

- **Traduções no disco sem decodificar.** `cachedTranslationsFlow` olha só a existência do `index.json`, e
  `outdatedTranslationsFlow` só o arquivo `format`. O repositório checa ao nascer, e `preload()` (boot, fim do
  download) checa de novo; as duas checagens convertem antes o arquivo antigo (3), se houver.
- **Índice sempre em memória.** `booksFlow` lê o `index.json` da tradução ativa assim que ela está no disco e o
  mantém pela sessão inteira: nomes, capítulos e versículos por capítulo ficam disponíveis antes de qualquer
  texto — a toolbar, o índice e a navegação entre livros não esperam nada. `null` = ainda lendo; vazio = tradução
  não está no aparelho (tela oferece o download); lista = pronto. Ao trocar de tradução, o índice anterior fica
  até o novo chegar.
- **Só um livro de texto em memória.** `currentBookFlow` lê o arquivo do livro da posição atual (o livro de
  `abbrev` igual, sem diferenciar maiúsculas; se não existir, o primeiro do índice) enquanto alguém coleta (o
  `BibleViewModel` do `bibleGraph`) e o solta 5 s depois do último coletor sair. Mudar de capítulo no mesmo livro
  não lê nada; mudar de livro ou de tradução lê o novo arquivo, e o anterior continua no flow até ele chegar. O
  texto vem com os títulos do livro. Quando a tradução ativa sai do formato antigo (fim da atualização), o livro
  aberto é lido de novo, e os títulos aparecem sem trocar de capítulo.
- `clearAll()`: apaga as duas traduções (e o arquivo antigo), reseta as preferências e esvazia
  `cachedTranslationsFlow` (e, com isso, `booksFlow` e `currentBookFlow`).
- Arquivo corrompido (índice ou livro): apaga a pasta da tradução, que passa a contar como ausente.

## 5. Download

`BibleDownloadWorker` (WorkManager, trabalho único `bible_auto_download`) baixa cada tradução ausente ou no formato
antigo (`isCurrent()` falso; com `force`, todas). A resposta é lida em fluxo, um livro por vez, e dividida no formato da seção 3
numa pasta temporária que só substitui a da tradução quando o último livro e o índice estão gravados — nunca há a
tradução inteira na memória nem uma tradução pela metade no lugar. Antes de começar chama `preload()` (converte o
arquivo antigo em vez de baixar de novo) e no fim chama de novo, para a UI ver os livros sem reabrir o app. Retry
até 3 tentativas; 401/403 falha na hora.

- **Automático:** no boot, depois do `preload()`, se falta alguma tradução e o download automático não está
  pausado — só WiFi (`UNMETERED`).
- **Manual:** pelos botões do leitor ("Baixar via WiFi", "Baixar com dados móveis", "Tentar novamente", "Usar dados
  móveis"). Todo download manual tira a pausa.

### 5.1 Apagar

"Apagar Bíblia" nas configurações (`DeleteBibleUseCase`) cancela o trabalho `bible_auto_download`, **pausa** o
download automático e chama `clearAll()`. Nada é baixado de novo: o leitor mostra "Bíblia ainda não baixada." com os
botões de download, e o boot não enfileira mais nada enquanto a pausa existir.

A pausa é a preferência `bible_auto_download_paused` (`@SettingsPrefs`), fora do que `clearAll()` reseta. Sobrevive a
fechar o app; só um download manual a tira.

### 5.2 Atualização de formato

No boot, se nenhuma tradução falta mas alguma está no formato antigo (`outdatedTranslationsFlow`), e o download
automático não está pausado, o app baixa de novo **em silêncio**: o mesmo trabalho `bible_auto_download`, só WiFi,
política `KEEP` (não substitui um download em andamento), marcado com a tag `bible_format_upgrade`. Enquanto o
trabalho com essa tag espera ou roda, o leitor não mostra banner nem progresso — a pessoa segue lendo a versão
antiga, e a troca é atômica como qualquer download (5). Se falta alguma tradução, vale o download automático normal
(com banners), que também atualiza as antigas. Um download manual substitui a atualização.

## 6. Vídeos

Vídeos e playlists do YouTube ligados a capítulos e a livros inteiros (`specs/019-bible-videos/`). Valem para as
duas traduções (só `abbrev` e número do capítulo). Gerência só com `bible_videos` ≥ `manage` (Admin e Liderança;
Mídia não); "Excluir vídeo" com `owner`.

### 6.1 Dados

`GET api/bible/videos/` é público (cliente sem token): todos os vídeos com seus locais numa resposta. Guardado como
snapshot `bible_videos` (+ ETag) pelo `BibleVideoRepository`; carregado do disco no boot (`Preloadable`) e
atualizado quando o grafo da Bíblia abre (`BibleVideosViewModel`) e depois de cada alteração. Falha de rede mantém a
lista guardada. Vídeo: `id`, `kind` (`video` | `playlist`), `url`, `title`, `thumbnail_url`, `places` (`book`,
`chapter` ou `null` = livro inteiro, `position`). Miniaturas pelo Coil (cache em disco, aparecem offline).

### 6.2 Leitor

- Ícone ▶ (`SmartDisplay`) na toolbar, antes do de fonte: para quem não gerencia, só quando o capítulo ou o livro
  atual tem vídeo; para quem gerencia, sempre (é por ele que se adiciona).
- Abre um bottom sheet com duas seções: "Gênesis 1" (vídeos do capítulo) e "Gênesis" (do livro inteiro), cada uma
  na ordem do seu local. Vídeo nos dois aparece só no capítulo. Seção vazia some; gestor sem nada vê "Nenhum vídeo
  aqui ainda.".
- Card: miniatura 16:9, título em até 2 linhas, selo "Playlist". Tocar abre a `url` com `ACTION_VIEW` (app do
  YouTube, senão navegador; o `t=` do link vale).

### 6.3 Gerência

- "Adicionar link" no sheet: diálogo com o link → `POST` com o capítulo atual como primeiro local → abre "Locais do
  vídeo". `409 BIBLE_VIDEO_EXISTS`: aviso "Esse vídeo já está cadastrado" e abre "Locais do vídeo" do existente
  (lido de `video` no corpo do erro) com o capítulo atual já na lista, sem salvar. Link inválido, vídeo indisponível
  ou falha na consulta: mensagem do servidor no diálogo.
- **"Locais do vídeo"** (`BibleVideoPlaces/{videoId}?presetBook=&presetChapter=`, VM própria da entrada): miniatura e
  título; lista de locais ("Gênesis 1", "Êxodo (livro inteiro)") em ordem canônica, cada um com ✕; "+ Local" abre o
  seletor: livro (mesma lista e busca do índice), depois grade de capítulos com seleção múltipla e "Livro inteiro";
  locais já na lista ficam marcados e travados. "Salvar" envia a lista inteira (`PUT .../places/`); lista vazia pede
  confirmação e exclui o vídeo. Sair sem salvar descarta.
- Menu ⋮ do card: "Editar locais", "Remover deste capítulo" / "Remover deste livro" (conforme a seção), "Excluir
  vídeo" (com confirmação, de todos os lugares).
- "Reordenar" na seção com 2+ vídeos: mostra só aquela seção com alças de arrastar; soltar salva a ordem daquele
  local (`PUT .../order/`); "Concluir" sai. Reordenar Gn 1 não mexe em Gn 2. Vídeo novo entra no fim.
- Toda alteração precisa de rede; falha mostra a mensagem e a lista volta à do servidor.

## 7. Progresso de leitura

Só com login; sem login não aparece nenhum controle. Marcação sempre manual.

- **Fim do capítulo:** depois do último versículo, "Marcar como lido"; lido vira "Lido ✓", e tocar desmarca. Rolar
  até o fim não marca.
- **Índice:** na aba Capítulo, capítulo lido tem fundo `primaryContainer` e um check; na lista de livros, "12/50" no
  lugar de "50 cap.", com check quando completo.
- **Livro inteiro:** toque longo no livro → diálogo sempre ("Marcar os 50 capítulos de Gênesis como lidos?" /
  "Desmarcar os 50 capítulos de Gênesis?"); só os capítulos que mudam entram na fila. Livro lido é derivado (todos
  os capítulos), nunca guardado.
- **Vídeo visto:** check no card; visto mostra o selo "Visto" e a miniatura esmaecida. Abrir o vídeo não marca.

### 7.1 Estado e fila

`BibleProgressRepository`: estado da conta (`GET api/bible/progress/`, snapshot `bible_progress`) com as mudanças
pendentes aplicadas por cima — a marca aparece na hora, inclusive offline. Buscado no boot e depois do login
(`Refreshable`), quando a Bíblia abre e depois de cada envio; sem sessão não busca.

Fila `BibleProgressQueueStore` (snapshot `bible_progress_queue`, um `Mutex`, arquivo ilegível = vazio, máximo 5000):
cada item é um estado (alvo capítulo ou vídeo, `done`, `changedAt` ISO-8601 com fuso), **um por alvo** — marcar e
desmarcar offline vira um item só. `BibleProgressSyncWorker` (rede, trabalho único `bible_progress_sync` + `KEEP`,
backoff exponencial; enfileirado a cada marca e no `MyApp.onCreate()`) envia em lotes de 500 para
`POST api/bible/progress/sync/` (DTO de envio separado do item guardado). 2xx ou 400: os itens enviados saem da fila
(só os mesmos alvo e horário — uma mudança feita durante o envio fica); erro de rede, 5xx ou 401: tudo fica. O
servidor resolve conflitos pela hora da ação (a mais recente vence).

**Logout** (`SessionScopedCache`): tenta enviar a fila por até 5 s e então apaga fila e estado guardado — outra conta
pode entrar no aparelho. Marca feita offline por quem sai antes de reconectar se perde (aceito).
