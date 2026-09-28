# Administração — Spec

Área restrita usada pela liderança da igreja. Reúne, em um único painel, os atalhos para as
tarefas administrativas. O painel é a porta de entrada; cada tarefa é uma tela própria dentro
de `adminGraph`.

Este documento descreve o **estado atual** do domínio, incluindo as áreas que ainda não têm
implementação — elas já aparecem no painel como cards inativos.

---

## 1. Telas

| Tela                     | Rota                        | Origem                          |
|--------------------------|-----------------------------|---------------------------------|
| Painel                   | `AdminMain`                 | `CoreScreen → Painel Admin`     |
| Registro de louvor       | `AdminRegister`             | Card "Gestão do Louvor"         |
| Geração de escala        | `AdminSchedule`             | Card "Gerar Escala"             |
| Hub de relatórios        | `ReportsHub`                | Card "Relatórios"               |
| Histórico do hinário     | `ReportsHymnalReport`       | Item do hub                     |
| Ficha do hino            | `ReportsHymnCard`           | Qualquer lista do relatório     |
| Janelas de culto         | `ReportsServiceWindows`     | Menu da TopBar do relatório     |
| Parâmetros de coleta     | `ReportsCollectionSettings` | Menu da TopBar do relatório     |
| Lista de membros         | `MembersList`               | Card "Membros"                  |
| Perfil do membro         | `MembersProfile/{memberId}` | Cartão da lista                 |
| Formulário do membro     | `MembersForm?memberId=`     | "Novo membro" / editar no perfil|
| Histórico do membro      | `MembersHistory/{memberId}` | Card de histórico no perfil     |

As três primeiras vivem em `adminGraph` (`AdminNavGraph.kt`). A navegação chega às telas pelo
`data class` `AdminNav` (`back`, `register`, `schedule`, `reports`, `members`), montado no grafo e
repassado como parâmetro — o painel não conhece o `NavController`.

As cinco de relatório vivem em `reportsGraph` (`ReportsNavGraph.kt`), **aninhado dentro de**
`adminGraph`, como `worshipHubGraph` aninha seus sub-grafos. Ver §6.

As quatro de membros vivem em `membersGraph` (`MembersNavGraph.kt`, rota `graph/admin/members`),
também aninhado em `adminGraph`. Ver §7.

## 2. Painel

`AdminScreen` é uma tela sem ViewModel: monta a lista de `AdminAction` e delega cada clique.
Não há estado assíncrono, portanto não há loading/erro.

Estrutura visual: o título "Painel Admin" fica na própria TopBar (`tabName`), e o subtítulo vem
numa faixa colada logo abaixo, injetada pelo `topBarExtension` do `BaseScreen`. O gradiente da
faixa parte do `ipbGreen` — a mesma cor de fundo da TopBar — em direção ao teal da marca, para
que TopBar e faixa leiam como um único bloco de cabeçalho. **Não** existe banner separado no
corpo da tela: título grande dentro do conteúdo, logo abaixo da TopBar verde, produzia dois
cabeçalhos empilhados.

Abaixo do cabeçalho vêm o título de seção "Funcionalidades" e uma grade de duas colunas de
cards. Cada card tem barra de destaque vertical à esquerda, fundo em gradiente sutil da cor de
destaque até `surfaceContainer`, borda fina na mesma cor, ícone em quadrado tonal, rótulo e
descrição de uma linha.

As cores de destaque dos cards ficam privadas em `AdminScreen.kt`. Só verde, laranja e teal vêm
de `BrandColors` — as demais existem apenas para diferenciar áreas administrativas entre si e
não fazem parte da identidade visual da igreja.

Ação sem implementação tem `AdminAction.enabled = false`: o card inteiro (barra, borda, fundo,
ícone) é pintado com `DisabledGray` e rótulo e descrição perdem metade da opacidade. O
`accentColor` definitivo continua declarado na ação mesmo assim — quando a tela existir, basta
remover o `enabled = false` que a cor certa volta a valer sem consulta a histórico.

### 2.1 Ações

A coluna "Destaque" é a cor definitiva da ação; enquanto o estado for "Sem implementação" ela
fica guardada no código mas o card aparece cinza.

| Ação               | Destaque | Ícone           | Estado                       |
|--------------------|----------|-----------------|------------------------------|
| Gestão do Louvor   | Laranja  | `MusicNote`     | Navega para `AdminRegister`  |
| Marcar Presença    | Teal     | `Person`        | Sem implementação            |
| Gerar Escala       | Verde    | `DateRange`     | Navega para `AdminSchedule`  |
| Membros            | Azul     | `People`        | Navega para a lista de membros |
| Avisos             | Rosa     | `Send`          | Sem implementação            |
| Relatórios         | Índigo   | `BarChart`      | Navega para `ReportsHub`     |
| Galeria            | Âmbar    | `PhotoLibrary`  | Sem implementação            |
| Eventos            | Ciano    | `Event`         | Sem implementação            |
| Notificações       | Pink     | `Notifications` | Sem implementação            |

Cards sem implementação não navegam e não exibem aviso — o clique é inerte, e o cinza é o que
comunica isso ao usuário.

Nenhum card mostra badge com contador. Contadores só entram quando houver dado real por trás;
número fixo no código seria informação falsa para a liderança.

### 2.2 Nomenclatura

"Gestão do Louvor" cobre os três usos da tela de registro — cadastrar música no hinário,
registrar as músicas tocadas no domingo e, futuramente, cadastrar cifra e letra. O nome
"Ministério de Louvor" **não** pode ser usado aqui: `WorshipHubScreen` já se apresenta como
"Min. Louvor" no lado de consumo (cifras, letras, tabelas). O par é intencional — o hub consome,
a administração gerencia.

## 3. Registro de louvor

`MusicRegistrationScreen` + `MusicRegistrationViewModel`. Um seletor (`RegistrationTypeSelect`)
escolhe entre os dois tipos de `RegistrationType`:

- `SUNDAY` — "Registrar domingo": data do culto e as músicas tocadas, em linhas ordenadas.
- `MUSIC` — "Registrar música": cadastro de uma música nova no hinário.

Cadastro de cifra e de letra ainda não existe: falta o endpoint no backend.

Eventos de uma vez (sucesso, erro de envio) saem por `MusicRegistrationEvent`.

## 4. Geração de escala

`AdminScheduleScreen` + `AdminScheduleViewModel`. Monta a escala mensal por tipo
(`ScheduleType`), com seleção de membros por função. O compartilhamento é feito por
`Intent.ACTION_SEND` disparado no grafo, não na tela.

Eventos de uma vez saem por `AdminScheduleEvent`.

## 5. Regras

- O acesso ao painel é oferecido pelo menu do `CoreScreen` apenas quando
  `authState.isLoggedIn && authState.isAdmin` (`isAdmin` vem do perfil). É um filtro de UI: quem
  de fato autoriza cada operação é o backend, nas chamadas das telas internas.
- `isAdmin` sai do snapshot do perfil carregado do disco no boot, então o item aparece junto com a
  home, sem esperar o `/me`. Se o papel mudou no servidor, o item se ajusta quando o refresh do
  perfil chega.
- O painel não faz chamada de rede — não há o que autorizar nele. As telas internas fazem, e todas
  usam `@AuthedRetrofit`.
- Ação sem implementação nunca navega para uma tela vazia.

---

## 6. Relatórios

Spec completa da feature: [`specs/003-hymnal-history-reports/`](../003-hymnal-history-reports/spec.md).

### 6.1 Hub

`ReportsHubScreen` — sem ViewModel e sem camada de dados, montada como `AdminScreen`: uma
`List<ReportArea>` e um composable de item. Hoje há uma única área, o histórico do hinário, e
**nada no hub sabe disso** — acrescentar escala, presença, membros ou galeria é mais uma entrada na
lista. Nenhuma infraestrutura genérica de relatórios foi construída.

### 6.2 Histórico do hinário

Uma busca, várias leituras. O endpoint de ocorrências devolve o período inteiro sem paginação, e o
app **pivota localmente**: período, recorte e visualização são três escolhas independentes e só a
troca de período vai à rede.

- **Período** — esta semana, este mês, este ano, personalizado (máx. 366 dias, validado localmente).
- **Recorte** — todas, uma janela de culto, fora do culto, ou um dia da semana.
- **Visualização** — Destaques, Ranking, Evolução, Cultos, Calendário, Culto × fora, Cobertura.

**Métricas:** contagem de ocorrências é a principal — ordena o ranking e define o comprimento da
barra. `device_count` é rótulo secundário ("alcance"), nunca ordena nem vira comprimento.

**Fontes, e qual alimenta o quê:** tudo limitado por período vem de `occurrences`; "todo o
histórico" vem de `top-hymns`. Nenhuma leitura soma as duas num mesmo número. Cobertura e ficha do
hino cruzam as duas para fatos **diferentes** — o conjunto dos nunca cantados vem do ranking, as
datas vêm da janela de 366 dias.

**Limite honesto:** `top-hymns` não devolve datas e `occurrences` não passa de 366 dias, então um
hino cantado antes disso aparece como "não é cantado há mais de um ano", sem data.

**Gráficos à mão**, com primitivas do Compose e cores do tema — barras horizontais por layout
(rótulos continuam texto de verdade), barras verticais em `Canvas`. Nenhuma biblioteca de gráficos
foi adicionada. O domínio entrega `BarPoint` já agregado.

**Vazios**, produzidos no domínio como `ReportEmptyReason` selado: nunca houve coleta, nada no
período, nada no recorte, culto inativo/ausente no período, culto sem registro, hinário
indisponível. O caso "culto sem registro" nomeia as duas possibilidades e **não afirma nenhuma** —
o app coleta visualização de hino e não tem como saber se o culto aconteceu.

### 6.3 Janelas de culto e parâmetros de coleta

Alcançadas pelo menu da TopBar do relatório, não pelo hub — o hub lista áreas com relatório, não
administração.

- **Janelas de culto:** listar, criar, editar, ativar/desativar, apagar. O dia da semana vem do
  backend na convenção `0 = segunda … 6 = domingo`; a conversão acontece uma única vez, em
  `ServiceWindowMapper`. Apagar **nunca apaga histórico** — a confirmação diz isso e apresenta
  desativar como a opção mais branda.
- **Parâmetros de coleta:** os seis inteiros, cada um com sua faixa visível e o efeito explicado.
  Validação local antes de qualquer requisição; `field_errors` do servidor caem no campo
  correspondente, via `AppError.Server.fieldErrors`. Chave desconhecida cai no texto genérico.

### 6.4 Camadas

- A feature vive em `features/admin/reports/` e **não importa** `features/hymnal`. O catálogo do
  hinário chega por `core/domain/repository/HymnCatalogRepository`.
- Filtragem e agregação ficam em casos de uso puros no domínio, testáveis sem Android. Toda frase,
  proporção e afirmação temporal é montada no domínio e chega pronta à tela.
- `HymnalReportViewModel` é escopado ao grafo (`hiltViewModel(graphEntry)`) para que a ficha do
  hino reaproveite o período já carregado. As ViewModels de janelas e de parâmetros são de tela
  única e usam `hiltViewModel()` puro, como as demais telas do `adminGraph`.

---

## 7. Membros

Spec completa da feature: [`specs/005-admin-members-management/`](../005-admin-members-management/spec.md).
Contrato consumido: `specs/010-members-management` do backend.

Área em que a liderança mantém o rol de membros: lista, perfil, cadastro, edição, foto (visível só
para líderes), histórico de alterações e exclusão. Vive em `features/admin/members/`.

### 7.1 Situação × validade

Dois atributos diferentes que a tela nunca mistura:

- **Situação** (`status`) — cadastro do servidor (hoje Ativo, Inativo, Visitante). Aparece exatamente
  com o nome que o servidor manda; vazia = "Sem situação". **Nenhum nome de situação é fixado no
  código** e não há filtro por situação.
- **Validade** (`is_active`) — se a ficha vale. Ficha inválida some da lista comum de membros e dos
  aniversários. Na tela é sempre "Perfil válido" / "Perfil inválido", nunca "ativo/inativo".

### 7.2 Ano de nascimento desconhecido

A igreja registra "sei o aniversário, não sei o ano" como nascimento no ano **0001**
(`0001-MM-DD`). O app trata esse ano como "ano não informado": mostra só "dd/MM",
não calcula idade ("Desconhecida") e o formulário oferece "Não sei o ano" (dia e mês só).
Datas com ano 0001 nunca contam como futuras e pulam a regra "batismo antes do nascimento". 29/02
não cabe nessa convenção (0001 não é bissexto). A convenção mora em um único lugar,
`domain/model/BirthDate.kt`.

### 7.3 Telas

- **Lista** — grade de 2 colunas (foto ou iniciais, nome, chip de situação, tag "Perfil inválido" com
  cartão esmaecido), busca por nome no aparelho (sem acento/maiúscula), sem filtros, botão
  "Novo membro". Estados: carregando, erro com "Tentar novamente", rol vazio, busca sem resultado.
- **Perfil** — faixa verde, foto grande sem botão próprio (toque traz a foto para frente da tela em quadrado,
  segundo toque abre em tela cheia com câmera no canto superior esquerdo para escolher ou remover; sem foto, as
  iniciais fazem o mesmo caminho), nome, idade · sexo, chips de situação e
  cargo, seções "Dados pessoais" e "Vida na igreja" (batismo
  com "há N anos", ministérios), switch "Perfil válido" que salva na hora e volta se falhar, card da
  última alteração, "Cadastrado em" e "Excluir membro".
- **Formulário** — um só para criar e editar; opções de situação, cargo e ministérios vêm do
  servidor; validação local (nome obrigatório, ≤ 255, datas não futuras, batismo não antes do
  nascimento) e `field_errors` do servidor no campo certo; edição envia só o que mudou; sair com
  alteração pendente pede confirmação. Na edição, a foto aparece no topo com câmera: a escolhida fica em
  pré-visualização e só sobe ao salvar, depois dos campos. A foto de um membro novo é adicionada depois da
  criação.
- **Histórico** — do mais novo ao mais antigo, frases em português ("X alterou Situação de A para B",
  "X cadastrou o membro", "X trocou/removeu a foto"); editor apagado = "Usuário removido".
- **Excluir** — diálogo avisa que ficha, histórico e foto somem para sempre; o botão só libera quando o
  nome digitado bate (sem diferenciar maiúsculas nem espaços nas pontas).

### 7.4 Regras

- **Só online, só memória** (LGPD art. 11): nada de membro vai para o disco — sem snapshot, sem cache
  de imagem em disco. O repositório (`@Singleton`) guarda lista, fichas abertas e ETags em memória,
  e cada escrita atualiza a lista na hora. A foto carrega por um `ImageLoader` próprio
  (`@MemberPhotoLoader`) no client autenticado, sem cache em disco; falha = iniciais. O único rastro
  em disco é o recorte do UCrop, apagado logo depois de lido.
- Logout e queda de sessão limpam tudo via `SessionScopedCache` (core §4.4.1).
- Logs levam só o id do membro.
- 403 em qualquer chamada: mostra o `detail` e volta ao painel. 404 numa ficha: "Este membro não
  existe mais" e volta à lista.
- PATCH é montado com `buildJsonObject` para que `null` (limpar campo) chegue ao servidor — o `Json`
  compartilhado usa `explicitNulls = false`.

### 7.5 Camadas

`data/` (API em `@AuthedRetrofit`, DTOs, mapper, repositório em memória) → `domain/` (modelos, casos de
uso puros: idade, validação, frases do histórico, checagem da foto, confirmação de exclusão) →
`presentation/` (uma ViewModel por tela com `hiltViewModel()`; o estado compartilhado entre telas é o
do repositório, não de uma ViewModel escopada ao grafo).
