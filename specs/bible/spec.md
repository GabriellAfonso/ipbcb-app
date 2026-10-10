# Bíblia — Spec

A Bíblia completa em duas traduções (NAA e ARA), lida 100% offline depois do primeiro download. Não exige
autenticação.

---

## 1. Telas

| Tela    | Rota                      | Conteúdo                                                        |
|---------|---------------------------|-----------------------------------------------------------------|
| Leitor  | `BibleReader`             | Capítulo da posição atual, fonte ajustável, seleção de versículos |
| Índice  | `BibleIndex?tab={tab}`    | Livros (com busca por nome), capítulos e versículos             |

Ambas vivem em `bibleGraph`, com `BibleViewModel` escopado ao grafo. O leitor permite trocar a tradução,
avançar/voltar capítulo, ajustar a fonte (14–32, padrão 18) e copiar/compartilhar os versículos selecionados.
Sem a tradução ativa no aparelho, mostra o banner de download (tentar de novo no WiFi ou nos dados móveis).

## 2. Endpoint

`GET /api/bible/{translation}/` — `translation` = `NAA` | `ARA`. Lista de 66 livros:

```json
[{ "abbrev": "gn", "name": "Gênesis", "chapters": [["No princípio...", "..."]] }]
```

`chapters[i]` são os versículos do capítulo `i + 1`. Um versículo pode vir como array de partes; o app junta com
espaço. ~3,9 MB por tradução.

## 3. Armazenamento

- Cada tradução é um snapshot em `filesDir/snapshots/bible_naa.json` / `bible_ara.json` (+ ETag), via
  `SnapshotCache` (ver `core` 4.3).
- `BiblePreferences` (DataStore): tradução ativa, última posição (livro, capítulo, versículo) e tamanho da fonte.

## 4. Repositório (`BibleRepository`, `@Singleton`)

Expõe `booksFlow` (livros da tradução ativa), `activeTranslationFlow`, `cachedTranslationsFlow` (traduções
presentes no disco), `positionFlow` e `fontSizeFlow`.

- **Traduções no disco sem decodificar.** `cachedTranslationsFlow` olha só a existência dos arquivos
  (`SnapshotCache.exists()`). O repositório checa ao nascer, e `preload()` (boot, fim do download) checa de novo.
- **Livros só em memória enquanto a Bíblia está aberta.** Uma tradução inteira pesa vários MB no heap, então
  `booksFlow` não carrega nada no boot: decodifica a tradução ativa quando alguém coleta (o `BibleViewModel` do
  `bibleGraph`) e solta 5 s depois do último coletor sair — o valor volta a `null`, nada fica retido pelo resto
  da sessão. Os 5 s cobrem rotação de tela sem decodificar de novo.
- **Só a tradução ativa é decodificada, uma vez.** Decodifica de novo apenas quando a tradução ativa muda ou chega
  ao disco (download com a Bíblia aberta).
- `booksFlow`: `null` = decodificando (tela mostra carregando); vazio = tradução não está no aparelho (tela
  oferece o download); lista = pronto.
- `clearAll()`: apaga os snapshots das duas traduções, reseta as preferências e esvazia `cachedTranslationsFlow`
  (e, com isso, `booksFlow`).
- Arquivo corrompido: o `load()` apaga e devolve vazio; a tradução passa a contar como ausente.

## 5. Download

`BibleDownloadWorker` (WorkManager, trabalho único `bible_auto_download`) baixa cada tradução ausente
(`exists()`; com `force`, todas) como JSON cru, sem parse, e grava direto no snapshot. No fim chama `preload()`
para a UI ver os livros sem reabrir o app. Retry até 3 tentativas; 401/403 falha na hora.

- **Automático:** no boot, depois do `preload()`, se falta alguma tradução — só WiFi (`UNMETERED`).
- **Manual:** pelo banner (WiFi ou qualquer rede) e por `DeleteAndRedownloadBibleUseCase` (`clearAll()` + download
  forçado em qualquer rede).
