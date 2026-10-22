# Bíblia — Spec

A Bíblia completa em duas traduções (NAA e ARA), lida 100% offline depois do primeiro download. Não exige
autenticação.

---

## 1. Telas

| Tela    | Rota                      | Conteúdo                                                        |
|---------|---------------------------|-----------------------------------------------------------------|
| Leitor  | `BibleReader`             | Capítulo da posição atual, fonte ajustável, seleção de versículos |
| Índice  | `BibleIndex?tab={tab}`    | Livros (com busca por nome), capítulos e versículos             |
| Passagem | `AppRoutes.BIBLE_PASSAGE` | O mesmo leitor, aberto num livro/capítulo/versículo vindo de outra feature |

Todas vivem em `bibleGraph`, com `BibleViewModel` escopado ao grafo.

**Passagem** (`bible_passage?book={book}&chapter={chapter}&verse={verse}`, montada por `AppRoutes.biblePassage`): é como
outras features (as referências da Doutrina, `specs/studies/spec.md`) abrem a Bíblia sem importá-la. Na primeira
composição da entrada, `openPassage` salva a posição (conta como leitura normal) e deixa o versículo pendente; o leitor
rola até ele quando a tradução está decodificada e a posição atual bate com o pendente, e então o descarta. Voltar
retorna para quem abriu. `book` é o `abbrev` da Bíblia, comparado sem diferenciar maiúsculas.

O leitor permite trocar a tradução, avançar/voltar capítulo, ajustar a fonte (14–32, padrão 18) e
copiar/compartilhar os versículos selecionados.
Sem a tradução ativa no aparelho, mostra o banner de download (tentar de novo no WiFi ou nos dados móveis).

**Carregando.** Enquanto o texto do capítulo não chegou, o leitor mostra um placeholder com o mesmo layout do
capítulo: o título real ("Gênesis 1", vindo do índice) e barras pulsando no lugar dos versículos, na altura de
linha da fonte atual. A toolbar já mostra o livro e o capítulo. Quando o texto chega, ocupa o mesmo espaço — nada
pula. Mudar de capítulo rola para o versículo 1 só depois que o texto do novo capítulo está na tela.

## 2. Endpoint

`GET /api/bible/{translation}/` — `translation` = `NAA` | `ARA`. Lista de 66 livros:

```json
[{ "abbrev": "gn", "name": "Gênesis", "chapters": [["No princípio...", "..."]] }]
```

`chapters[i]` são os versículos do capítulo `i + 1`. Um versículo pode vir como array de partes; o app junta com
espaço. ~3,9 MB por tradução.

## 3. Armazenamento

Cada tradução vive dividida por livro em `filesDir/bible/{naa,ara}/`:

- `index.json`: os 66 livros na ordem canônica — `abbrev`, `name` e `verseCounts` (versículos por capítulo).
  Poucos KB.
- `00.json` … `65.json`: os capítulos de cada livro (`[["v1", "v2"], ...]`), na posição do livro no índice. Os
  versículos já vêm juntados (sem arrays de partes). O maior (Salmos) tem ~250 KB.

A tradução conta como presente quando `index.json` existe. `BiblePreferences` (DataStore) guarda a tradução ativa,
a última posição (livro, capítulo, versículo) e o tamanho da fonte.

**Arquivo antigo.** Versões anteriores gravavam a tradução inteira em `filesDir/snapshots/bible_{naa,ara}.json`
(+ `_etag.txt`). O repositório converte esse arquivo para o formato por livro na primeira checagem do disco e o
apaga, sem baixar de novo; se a conversão falhar, apaga do mesmo jeito e a tradução conta como ausente.

## 4. Repositório (`BibleRepository`, `@Singleton`)

Expõe `booksFlow` (índice da tradução ativa), `currentBookFlow` (texto do livro da posição atual),
`activeTranslationFlow`, `cachedTranslationsFlow` (traduções presentes no disco), `positionFlow` e `fontSizeFlow`.

- **Traduções no disco sem decodificar.** `cachedTranslationsFlow` olha só a existência do `index.json`. O
  repositório checa ao nascer, e `preload()` (boot, fim do download) checa de novo; as duas checagens convertem
  antes o arquivo antigo (3), se houver.
- **Índice sempre em memória.** `booksFlow` lê o `index.json` da tradução ativa assim que ela está no disco e o
  mantém pela sessão inteira: nomes, capítulos e versículos por capítulo ficam disponíveis antes de qualquer
  texto — a toolbar, o índice e a navegação entre livros não esperam nada. `null` = ainda lendo; vazio = tradução
  não está no aparelho (tela oferece o download); lista = pronto. Ao trocar de tradução, o índice anterior fica
  até o novo chegar.
- **Só um livro de texto em memória.** `currentBookFlow` lê o arquivo do livro da posição atual (o livro de
  `abbrev` igual, sem diferenciar maiúsculas; se não existir, o primeiro do índice) enquanto alguém coleta (o
  `BibleViewModel` do `bibleGraph`) e o solta 5 s depois do último coletor sair. Mudar de capítulo no mesmo livro
  não lê nada; mudar de livro ou de tradução lê o novo arquivo, e o anterior continua no flow até ele chegar.
- `clearAll()`: apaga as duas traduções (e o arquivo antigo), reseta as preferências e esvazia
  `cachedTranslationsFlow` (e, com isso, `booksFlow` e `currentBookFlow`).
- Arquivo corrompido (índice ou livro): apaga a pasta da tradução, que passa a contar como ausente.

## 5. Download

`BibleDownloadWorker` (WorkManager, trabalho único `bible_auto_download`) baixa cada tradução ausente
(`exists()`; com `force`, todas). A resposta é lida em fluxo, um livro por vez, e dividida no formato da seção 3
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
