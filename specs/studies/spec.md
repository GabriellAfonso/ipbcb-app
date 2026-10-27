# Estudos — Spec

Aba de estudos do app. Tem duas seções, **Doutrina** (os Símbolos de Westminster) e **Escola Bíblica Dominical**
(EBD, §5), e duas anunciadas como "Em breve" (Personagens Bíblicos, História da Igreja). A leitura não exige
autenticação; a gestão da EBD exige o escopo `sunday_school`. Origem: `specs/013-doctrine-reader` e
`specs/020-sunday-school`.

---

## 1. Telas

Todas vivem em `studiesGraph` (`AppRoutes.STUDIES_GRAPH`). Cada tela tem seu ViewModel (`hiltViewModel()` por rota);
o estado compartilhado fica no repositório singleton.

| Tela | Rota | Conteúdo |
|------|------|----------|
| Hub | `StudiesHub` | Cards "Doutrina" e "Escola Bíblica Dominical" + dois cards "Em breve" (não clicáveis) |
| Doutrina | `DoctrineList` | Documentos na ordem do servidor, com contagem ("107 perguntas e respostas", "33 capítulos") |
| Catecismo | `DoctrineCatechism/{slug}` | Cards de pergunta (número, pergunta, resposta com quebras de linha, chips de referência); busca, "Ir para nº…", tamanho do texto |
| Confissão | `DoctrineConfession/{slug}` | Lista de capítulos (número + título em caixa de frase); busca |
| Capítulo | `DoctrineConfessionChapter/{slug}/{chapter}?paragraph={n}` | Parágrafos numerados, listas do 1.2 e texto final; anterior/próximo; busca; tamanho do texto |

- **Referências**: cada chip mostra o texto impresso. Com `reference`, abre a Bíblia em `AppRoutes.biblePassage(livro,
  capítulo, primeiro versículo, intervalos)` com os versículos citados destacados (ver `specs/bible/spec.md`); sem `reference`, é texto com contorno e não faz nada. O app
  nunca interpreta o texto impresso.
- **Busca** (no aparelho): ignora maiúsculas e acentos, exige todas as palavras em qualquer ordem. No catecismo, um
  número puro põe a pergunta daquele número primeiro. Sem resultado: "Nenhum resultado".
- **"Ir para nº…"**: número fora do intervalo ou não numérico mostra "Digite um número de 1 a N".
- **Tamanho do texto**: "Aa" abre a barra A− / A+, de 14 a 32 sp em passos de 2 (padrão 18), compartilhado pelos leitores
  e separado da Bíblia.
- **Títulos da Confissão** e cabeçalhos das listas do 1.2 vêm em caixa alta e são mostrados em caixa de frase,
  mantendo maiúsculas em Deus, Cristo, Mediador, Espírito Santo, Sagradas Escrituras, Santíssima Trindade, Lei, Igreja,
  Senhor e Antigo/Novo Testamento.
- Documento de `kind` desconhecido não aparece na lista.

## 2. Endpoints

Públicos (`@AuthLessRetrofit`), contrato no backend (`specs/019-doctrine-documents/contracts/doctrine-api.md`):

- `GET /api/doctrine/` → `{"documents": [{"slug", "title", "kind", "item_count"}]}`
- `GET /api/doctrine/{slug}/` → catecismo (`questions[{number, question, answer, proof_texts[{printed, reference}]}]`)
  ou confissão (`chapters[{number, title, paragraphs[{number, text, lists?, text_after?}]}]`). `reference` =
  `{book, chapter, verses[{start, end}]}`, `verses` vazio = capítulo inteiro.

Os dois respondem com `ETag` e `304` para `If-None-Match` igual.

## 3. Armazenamento

- Snapshots via `SnapshotCacheFactory` (ver `core` 4.3): `filesDir/snapshots/doctrine_list.json` e
  `doctrine_<slug>.json`, cada um com seu ETag. Conteúdo público: sem criptografia, não é apagado no login/logout.
- `DoctrinePreferences` (Settings DataStore): `doctrine_font_size`.

## 4. Repositório (`DoctrineRepository`, `@Singleton`)

- Um snapshot para a lista e um por slug, criado na primeira vez que o slug é pedido.
- `load*` = `preload()` (cópia do disco na tela) e depois `refresh()` (checagem por ETag). Falha na checagem com cópia
  salva mantém a cópia sem erro; sem cópia, `SnapshotState.Error` e a tela oferece "Tentar novamente" (`refresh*`).
- Arquivo corrompido: o `load()` apaga e conta como ausente.
- Documento cujo `kind` o app não conhece vira `SnapshotState.Error`.

## 5. Escola Bíblica Dominical

Aulas da EBD, públicas para leitura e escritas pela liderança no painel. Backend:
`backend/specs/sunday-school/spec.md` (contrato em `backend/specs/026-sunday-school/contracts/api.md`). Decisões e
motivos: `specs/020-sunday-school/research.md`.

### 5.1 Telas

| Tela | Rota | Conteúdo |
|------|------|----------|
| EBD | `SundaySchool` | Abas por ano (as do servidor, mais recente primeiro; abre o ano padrão do servidor), "2026 — Tema" acima da lista, aulas na ordem do servidor com data e professor quando conhecidos. Sem aula publicada: "Nenhuma aula publicada ainda" |
| Aula | `SundaySchoolLesson/{lessonId}` | Cabeçalho em pergaminho (título, ano e tema, data · professor), seções com ícone e blocos; "Aa" |
| Gestão | `SundaySchoolManage` (`AppRoutes.SUNDAY_SCHOOL_MANAGE`, aberta pelo card "EBD" do painel) | Seletor de ano, aulas do ano com "Rascunho" nas não publicadas, arrastar pela alça (pressão longa) para reordenar, apagar, "Nova aula", menu "Anos da EBD" |
| Formulário | `SundaySchoolLessonForm?lessonId={id}&year={ano}` (`-1` = nova) | Título, ano, data, professor, status (Rascunho/Publicado) e o conteúdo; abas "Editar" e "Pré-visualizar" |
| Anos | `SundaySchoolYears` | Cada ano com tema e "N aulas · M publicadas"; tocar abre o diálogo do tema ("Remover tema" limpa) |

- **Blocos**: `paragraph`, `list` (marcadores, números ou romanos), `key_verse` (cartão de pergaminho), `cards`,
  `comparison` (o lado destacado tem borda dourada), `steps` (setas entre as etapas), `table` (rolagem horizontal),
  `stats` (número grande + rótulo), `timeline`, `book_facts` (nome, nº do livro na Bíblia, capítulos e versículos
  vindos de `books`, mais autor, data, estilo e tempo de leitura quando houver). Texto aceita só `**negrito**` e
  `*itálico*`; o resto aparece literal. Tipo de bloco desconhecido é pulado e ícone desconhecido não é desenhado.
- **Referências**: os mesmos chips da Doutrina (§1).
- **Visual**: fundo creme liso atrás do texto, textura de pergaminho só no cabeçalho e nos versículos-chave, títulos
  em Cormorant Garamond e texto em Literata (Google Fonts baixadas; serifa do sistema até lá), marrom muito escuro
  no modo escuro. Ícones Phosphor duotone (MIT) como vetores do app (`res/drawable/ic_ph_*.xml`), dourados.
- **Tamanho do texto**: o mesmo "Aa" e a mesma preferência dos leitores de Doutrina.
- **Editor** (só o básico nesta versão): cria, edita, move e apaga seções (título e ícone da lista fechada ou
  nenhum), parágrafos, listas e versículos-chave, e os chips de referência (texto do chip + livro, capítulo e
  versículos "1-3,5", ou sem referência). Os outros tipos aparecem como na leitura, podem ser movidos ou apagados e
  voltam intactos ao salvar. Uma aula nova começa com as seis seções padrão e seus ícones, cada uma com um
  parágrafo vazio. O livro vem da lista da Bíblia do aparelho (`core/domain/bible/BibleBookCatalog`), com a
  abreviação que o servidor espera (`Gn`, `1Tm`); sem a lista, a abreviação é digitada.
- **Erros**: o aparelho confere título, ano (2000–2100) e textos vazios antes de enviar; os erros do servidor
  (`SUNDAY_SCHOOL_CONTENT_INVALID`, `errors[{path, message}]`) aparecem no bloco ou na seção do `path`, e o que
  não tem lugar fica no topo do formulário. Ordem desatualizada (`SUNDAY_SCHOOL_ORDER_MISMATCH`): a lista recarrega
  com "A lista mudou; tente de novo.". Sair com alterações pergunta "Descartar alterações?".
- **Permissões**: o card aparece com `sunday_school` ≥ `view`; criar, editar, reordenar e tema pedem `manage`;
  apagar pede `owner`. Admin e Liderança são `owner`; Mídia não tem nada.

### 5.2 Endpoints

- Públicos (`@AuthLessRetrofit`, ETag/304): `GET /api/sunday-school/[?year=N]` (anos, ano padrão, ano pedido e suas
  aulas publicadas) e `GET /api/sunday-school/lessons/{id}/` (aula publicada com `content` e `books`).
- Gestão (`@AuthedRetrofit`): `manage/lessons/` (GET por ano, POST), `manage/lessons/{id}/` (GET, PATCH, DELETE),
  `manage/lessons/order/` (PUT `{year, lesson_ids}`), `manage/years/` (GET), `manage/years/{year}/` (PATCH
  `{theme}`). Os corpos são montados à mão para mandar `date: null`.

### 5.3 Armazenamento

- Snapshots (`SnapshotCacheFactory`): `sunday_school_index` (pedido sem ano), `sunday_school_index_<ano>` e
  `sunday_school_lesson_<id>`, cada um com seu ETag; conteúdo público, sem criptografia, não apagado no
  login/logout. Um 404 de ano ou aula não mais publicados apaga a cópia e mostra "Este ano não tem mais aulas
  publicadas." / "Esta aula não está mais disponível.".
- Nada da gestão vai para o disco (rascunhos não são públicos). Depois de salvar, reordenar, apagar ou mudar tema, o
  app revalida os snapshots públicos do ano para a leitura no aparelho refletir a mudança.
