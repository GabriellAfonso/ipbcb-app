# Estudos — Spec

Aba de estudos do app. Hoje tem uma seção, **Doutrina** (os Símbolos de Westminster), e três seções anunciadas como
"Em breve" (Escola Bíblica Dominical, Personagens Bíblicos, História da Igreja). Não exige autenticação. Origem:
`specs/013-doctrine-reader`.

---

## 1. Telas

Todas vivem em `studiesGraph` (`AppRoutes.STUDIES_GRAPH`). Cada tela tem seu ViewModel (`hiltViewModel()` por rota);
o estado compartilhado fica no repositório singleton.

| Tela | Rota | Conteúdo |
|------|------|----------|
| Hub | `StudiesHub` | Card "Doutrina" + três cards "Em breve" (não clicáveis) |
| Doutrina | `DoctrineList` | Documentos na ordem do servidor, com contagem ("107 perguntas e respostas", "33 capítulos") |
| Catecismo | `DoctrineCatechism/{slug}` | Cards de pergunta (número, pergunta, resposta com quebras de linha, chips de referência); busca, "Ir para nº…", tamanho do texto |
| Confissão | `DoctrineConfession/{slug}` | Lista de capítulos (número + título em caixa de frase); busca |
| Capítulo | `DoctrineConfessionChapter/{slug}/{chapter}?paragraph={n}` | Parágrafos numerados, listas do 1.2 e texto final; anterior/próximo; busca; tamanho do texto |

- **Referências**: cada chip mostra o texto impresso. Com `reference`, abre a Bíblia em `AppRoutes.biblePassage(livro,
  capítulo, primeiro versículo)` (ver `specs/bible/spec.md`); sem `reference`, é texto com contorno e não faz nada. O app
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
