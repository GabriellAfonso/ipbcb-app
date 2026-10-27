# Hinário — Spec

O Hinário é a coleção de hinos da igreja disponível no app. Funciona totalmente offline após a
primeira carga e não exige autenticação.

Desde `002-hymnal-view-history`, o domínio também **coleta de forma invisível** quais hinos a
congregação abre, para que a igreja saiba o que de fato se canta durante a semana e aos domingos.

---

## 1. Telas

| Tela               | Rota                    | Origem                        |
|--------------------|-------------------------|-------------------------------|
| Lista de hinos     | `hymnal_list`           | `CoreScreen → HymnalScreen`   |
| Detalhe do hino    | `hymn_detail/{hymnId}`  | Clique em um item da lista    |

Ambas vivem em `hymnalGraph` (`HymnalNavGraph.kt`), com `HymnalViewModel` escopado ao grafo via
`hiltViewModel(graphEntry)`.

`hymnId` na rota é o **número** do hino (`Hymn.number`), não a chave primária do servidor.

### 1.1 Lista

Lista completa ordenada por número crescente (numérico quando possível, alfabético como
desempate). Busca por número, título ou trecho de letra, *accent-insensitive* via `normalize()`.
Pull-to-refresh chama `refreshHymnal()`.

O `debounce` de 200 ms vale **somente para query não vazia**: a busca varre a letra inteira de
todos os hinos e não deve rodar a cada tecla, mas a query vazia precisa passar direto. Um
`debounce` uniforme atrasaria também a emissão inicial do `MutableStateFlow("")`, deixando a tela
em `isLoading` por 200 ms toda vez que se entra no hinário, mesmo com os hinos já em memória.

`HymnalUiState`: `hymns`, `filteredHymns`, `groups`, `isLoading`, `error`.

**Layout** (mesma linguagem das Tabelas e da Escala: o mais escuro em cima): o campo de busca fica no bloco
verde da TopBar (`topBarExtension`), translucido, com a dica "Número, título ou trecho". Com texto digitado,
mostra "N hinos" e um botao de limpar. Abaixo, a folha de conteudo com cantos superiores arredondados
(`surfaceDim`).

**Grupos por centena:** `filteredHymns` agrupados de 100 em 100 pelo numero (`groupHymnsByHundred`, calculado
no ViewModel): titulo do grupo "1 – 100", "101 – 200"… centralizado entre duas linhas finas, e um cartao
(`surfaceContainer`) com os hinos do grupo. Numero sem parte numerica vai para o grupo "Outros", no fim.

**Linha:** numero grande em verde (`BrandColors.Green`, 22 sp, negrito) a esquerda; titulo em cima e o
comeco da primeira estrofe numa linha embaixo. Linha inteira clicavel. Busca sem resultado: "Nenhum hino
encontrado para “{busca}”."

### 1.2 Detalhe

**Cabecalho** no bloco verde da TopBar: "Hino {número}" pequeno (mono, verde-menta) e o titulo grande em
branco. O botao de fonte da TopBar abre o ajuste no mesmo bloco verde, acima do cabecalho. Abaixo, a folha de
conteudo arredondada (`surfaceDim`).

**Estrofes:** um cartao por estrofe (`surfaceContainer`, cantos de 12 dp), com um rotulo em cima dizendo o
que e (`stanzaLabels`): "1ª estrofe", "2ª estrofe"… so contando as `VERSE`; "Coro" para `CHORUS`; sem rotulo
para `OTHER`. O coro tem fundo verde claro e texto verde escuro em italico (no escuro, fundo verde escuro e
texto verde-menta). Sem faixa colorida.

Texto selecionável (`SelectionContainer`). Ajuste de fonte de 16 sp a 32 sp por slider na
extensão da top bar, persistido em `ThemePreferences.hymnalFontSize` (padrão 22 sp).

Hino não encontrado exibe "Hino não encontrado" — não é erro.

---

## 2. Dados

### 2.1 `GET api/hymnal/`

```json
[{
  "id": 42,
  "number": "42",
  "title": "Firme nas Promessas",
  "lyrics": [{ "type": "verse" | "chorus", "text": "..." }]
}]
```

Público, sem autenticação (`@AuthLessRetrofit`). Suporta `If-None-Match` / ETag.

**`id`** é a chave primária do servidor, usada apenas pela coleta de histórico (§3). É
`Int? = null` no `HymnDto` **de propósito**: snapshots gravados antes de o campo existir não o
têm, e um campo não-nulo sem default lançaria `MissingFieldException` ao decodificar o cache,
quebrando o hinário offline de todos os usuários já instalados.

`type` desconhecido vira `HymnLyricType.OTHER`.

### 2.2 Snapshot offline

Segue o padrão `JsonSnapshotStorage` (`HymnalSnapshotModule`), chave `hymnal`:

- `preload()` e `refreshHymnal()` registrados como `Preloadable` e `Refreshable` no startup.
- Cache em `filesDir/snapshots/hymnal.json` + `hymnal_etag.txt`.
- `SnapshotState`: `Loading` | `Data` | `Error`.

---

## 3. Coleta de histórico de visualização

Invisível ao membro: **nenhuma tela, nenhum controle, nenhuma mensagem**. Uma falha de coleta ou
de envio nunca aparece para o usuário.

Spec completa da feature: [`specs/002-hymnal-view-history/`](../002-hymnal-view-history/spec.md).

### 3.1 Quando um hino conta

`HymnViewTrackingViewModel` — escopado ao **destino** `hymn_detail` (`hiltViewModel()` puro, não
`hiltViewModel(graphEntry)`), para que sair e voltar reinicie a contagem enquanto uma mudança de
configuração a preserve.

- `LifecycleResumeEffect` no `HymnDetailScreen` sinaliza `onHymnVisible` / `onHymnHidden`.
- Conta apenas em **`RESUMED`**: pausa ao ir para background ou desligar a tela, e retoma
  somando ao acumulado.
- Ao cruzar `minSecondsToCount`, registra **um** evento e trava — por mais que o membro
  permaneça, não há segundo evento naquela visita.
- Sair e voltar gera legitimamente um segundo evento. O servidor colapsa mesmo hino + mesmo
  dispositivo dentro da janela dele; **o app não implementa colapso próprio**.
- Hino sem `id` (snapshot antigo): não registra nada, silenciosamente.

Medição com `MonotonicClock` (`SystemClock.elapsedRealtime`), imune a ajuste de relógio. O
disparo é um único `delay(restante)`, sem loop de tick.

### 3.2 Fila local

`HymnViewQueueStore` sobre o mesmo `SnapshotStorage` dos demais snapshots, chave
`hymn_view_queue`. **Sem Room.**

- Fila mutável (append + remove), não cache de blob — todo *read-modify-write* ocorre sob um
  único `Mutex`, senão dois eventos simultâneos se perdem.
- Ordem de inserção, mais antigo primeiro. Limite de **2000**; ao estourar, descarta o mais
  antigo (o servidor recusa eventos antigos de qualquer forma).
- Sobrevive a reinício e atualização do app. Arquivo corrompido é lido como vazio.

### 3.3 Identificador de dispositivo

`DeviceIdProvider` em `core/` (não é específico do hinário). UUID aleatório gerado na primeira
leitura, dentro de `dataStore.edit {}` para que leituras concorrentes não gerem dois valores.

Sem identificador de hardware, sem `ANDROID_ID`, sem advertising id, **sem permissão**.
Sobrevive a atualização; é regenerado na reinstalação — o único efeito é o servidor não
reconhecer mais o dispositivo para colapso.

### 3.4 Envio

`POST api/hymnal-history/events/` — público, throttle de 600 req/h.

`HymnViewSyncWorker` (WorkManager) com constraint de rede, `enqueueUniqueWork` + `KEEP` e backoff
exponencial de 30 s. Enfileirado ao registrar um evento e também em `MyApp.onCreate()`, que fecha
a janela em que o `KEEP` absorveria um enfileiramento durante uma execução em andamento.

**Escolha do cliente HTTP:** `@AuthedRetrofit` **somente** quando
`AuthStatusProvider.hasValidAccessToken()` for verdadeiro; caso contrário `@AuthLessRetrofit`.
Não basta "estar logado": enviar token expirado faz o servidor responder 401, o que leva o
`TokenAuthenticator` a tentar refresh e **limpar os tokens** ao falhar — deslogando o membro a
partir de um job em background. `AuthStatusProvider` mora em `core/domain/auth/` porque
`features/hymnal` não pode importar `features/auth`.

O corpo enviado tem **exatamente sete campos** (`client_event_id`, `hymn_id`, `device_id`,
`viewed_at`, `duration_seconds`, `app_version`, `platform`). O servidor proíbe campos
desconhecidos: qualquer chave extra faz o evento voltar como `invalid_event`. Por isso o DTO de
rede é um tipo separado de `QueuedHymnViewEvent` — um campo local novo não pode vazar para a
requisição.

`viewed_at` sempre carrega offset UTC.

### 3.5 Reconciliação

Resposta `201` traz `accepted` e `rejected` (`unknown_hymn`, `viewed_at_in_future`,
`viewed_at_too_old`, `invalid_event`).

> Remove-se **todo id enviado no chunk**, não apenas os que o servidor respondeu.

`accepted` significa gravado, deduplicado ou colapsado — os três são "pode esquecer". `rejected`
também é removido, com o motivo logado: o código de motivo existe justamente para que nenhum
evento tente para sempre. Um evento com `client_event_id` inválido é descartado pelo servidor
**sem aparecer em nenhuma das listas** — reconciliar pelo conjunto enviado torna o vazamento
estruturalmente impossível.

| Resposta        | Fila                | Worker            |
|-----------------|---------------------|-------------------|
| `201`           | remove o chunk todo | segue             |
| `400`           | remove o chunk todo | segue (insolúvel) |
| `429`           | mantém              | `Result.retry()`  |
| `5xx` / rede    | mantém              | `Result.retry()`  |

### 3.6 Configuração

`GET api/hymnal-history/settings/` — leitura pública, buscada no startup pelo multibinding
`Refreshable`. O app consome apenas `min_seconds_to_count` e `max_batch_size`, cacheados no
DataStore `@SettingsPrefs`.

Fallback: valores cacheados → padrões embutidos (**30 s** e lote de **50**). Falha na busca não
produz nenhum efeito visível.

O `PATCH` é admin-only e agora **tem superfície no app**, em
[`features/admin/reports/`](../admin/spec.md#6-relatórios) — não aqui. A tela administrativa lê e
grava os parâmetros pelo cliente autenticado; a coleta continua lendo pelo cliente público. As duas
não compartilham interface Retrofit, de propósito.

Salvar pelo painel **não** atualiza o cache deste aparelho: `HymnViewSettingsStore` vive nesta
feature e `features/admin` não pode importá-la. Os valores novos passam a valer no próximo startup,
pelo `Refreshable` já existente.

### 3.7 Catálogo compartilhado

`HymnalRepository` implementa também `core/domain/repository/HymnCatalogRepository`, que expõe
apenas número e título (`HymnCatalogEntry`). É como o relatório administrativo descobre quais hinos
**nunca** foram cantados, sem que `features/admin` importe `features/hymnal`.

Mesmo arranjo que `SongsRepository` tem com `AllSongsRepository`: o binding fica em `HymnalModule`,
não há segundo cache nem segunda busca — o snapshot já pré-carregado é mapeado.

---

## 4. Fora de escopo

- Telas administrativas de histórico (ocorrências, hinos mais cantados, edição de settings, CRUD de
  janelas de culto). Existem desde `003-hymnal-history-reports`, mas **não neste domínio**: são
  superfície administrativa e vivem em `features/admin/reports/`
  ([spec](../admin/spec.md#6-relatórios)). O que este domínio expõe para elas é o catálogo, por
  `core/` (§3.7) — nada mais.
- Coleta em qualquer outra tela — letras, cifras, estudos e Bíblia não são instrumentados.
- Qualquer controle para o membro ativar, desativar ou inspecionar a coleta.
- Colapso local de visualizações próximas.

---

## 5. Estrutura

```
features/hymnal/
├── data/
│   ├── api/          HymnalApi, HymnalEndpoints, HymnalHistoryApi, HymnalHistoryEndpoints
│   ├── dto/          HymnalDtos, HymnalHistoryDtos
│   ├── local/        HymnViewQueueStore, QueuedHymnViewEvent, HymnViewSettingsStore
│   ├── mapper/       HymnMapper, HymnViewHistoryMapper, HymnViewHistoryWireMapper
│   ├── repository/   HymnalRepositoryImpl, HymnViewHistoryRepositoryImpl
│   ├── snapshot/     HymnalSnapshotFetcher
│   └── work/         HymnViewSyncWorker, WorkManagerHymnViewSyncScheduler
├── di/               HymnalModule, HymnalSnapshotModule, HymnalHistoryModule
├── domain/
│   ├── model/        Hymnal, HymnViewHistory
│   ├── repository/   HymnalRepository, HymnViewHistoryRepository
│   ├── sync/         HymnViewSyncScheduler
│   ├── timer/        HymnViewTimer
│   └── usecase/      ObserveHymns, SearchHymns, RecordHymnView, SyncHymnViews,
│                     GetHymnViewSettings
└── presentation/
    ├── navigation/   HymnalNavGraph
    ├── screens/      HymnalScreen, HymnDetailScreen
    └── viewmodel/    HymnalViewModel, HymnViewTrackingViewModel
```

Em `core/`: `DeviceIdProvider`, `AuthStatusProvider` (+ `AuthSessionStatusProvider`),
`MonotonicClock`, todos ligados em `AppInfoModule`.
