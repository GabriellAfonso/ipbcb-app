# Eventos — Spec

Os eventos da igreja (um almoço de domingo, um congresso, um acampamento) aparecem no **carrossel de destaques** da
tela inicial, um card por evento com o flyer, e numa **tela de detalhe**. Eventos são **públicos por padrão**: quem
não está logado também os vê. Um evento pode ser marcado **"Só membros"**; nesse caso só um membro logado o recebe.
Quem tem nível no escopo `events` gerencia os eventos numa área própria do **Painel de Gestão** (seção 4).

Fonte da verdade no backend: `backend/specs/020-church-events/contracts/events-api.md`. Endpoints, campos, ordem,
erros e as regras de acesso às imagens não são redefinidos aqui.

---

## 1. Modelo

| Campo | Obrigatório | Observação |
|-------|-------------|------------|
| Título | sim | até 120 caracteres |
| Data de início | sim | |
| Data de fim | não | sem ela, o evento é de um dia; quando existe, é ≥ início |
| Horário | não | `HH:MM`, horário de Brasília |
| Local | não | até 200 caracteres |
| Descrição | não | texto livre |
| Imagem | sim | sempre 3:2 (o app recorta antes de enviar) |
| Visibilidade | — | `public` (padrão) ou `members` ("Só membros") |

**Ativo**: do momento em que é criado até o fim do seu último dia (data de fim, ou de início quando não há fim). Não
existe rascunho nem publicação agendada. Evento passado continua no servidor para sempre (só um `owner` o apaga) e
aparece apenas na gestão.

---

## 2. Destaque (tela inicial)

- Páginas do carrossel, nesta ordem: escala, aniversariantes e, em seguida, **uma página por evento ativo**, no
  máximo **3** (os primeiros da lista pública, que vem ordenada por data e horário de início).
- Sem evento para mostrar, ou nada carregado ainda, o carrossel não tem página de evento: evento é raro, e uma página
  vazia ocuparia o destaque quase sempre (`specs/core/spec.md` §6.3). `ObserveActiveEventsUseCase` devolve `null`
  enquanto o snapshot está em `Loading`; erro continua sendo lista vazia.
- Página de evento (`HighlightEvent`): a imagem preenche o card, também 3:2, então aparece inteira; título (até 2
  linhas) e data sobre um gradiente na base. Sem imagem (falha, 404, offline sem cache), fica um fundo neutro com
  título e data. Imagem antiga em 16:9 (enviada antes da troca para 3:2) perde um pouco das laterais no card.
- Formatos de data (`EventDateLabel`):
  - um dia: "14 nov" ou "14 nov · 19:30";
  - vários dias: "14 a 16 nov"; entre meses, "30 nov a 2 dez"; entre anos, "30 dez 2026 a 2 jan 2027".
- Tocar na página abre o detalhe.

### 2.1 Quando a lista é buscada

- No boot: primeiro o disco (`Preloadable`), depois a rede (`Refreshable`).
- Toda vez que a tela inicial volta ao primeiro plano (`ON_RESUME`). Assim, o que foi criado ou editado na gestão
  aparece ao voltar para a home.
- Quando o estado de login muda, entrando ou saindo.
- O pedido leva o token quando há sessão (`@AuthedRetrofit`); sem sessão, sai sem token. Com ETag, um refresh sem
  novidade custa um 304.

### 2.2 Offline e expiração

- A última lista recebida fica em disco (`JsonSnapshotStorage`, chave `events`), e as imagens no cache de disco do
  `@EventImageLoader` (50 MB).
- Antes de mostrar, o app descarta da lista guardada os eventos cujo último dia já passou, então um evento vencido
  nunca aparece, nem offline.
- Falha de rede mantém a lista guardada; sem nada guardado, aparece a página vazia, nunca uma página de erro.

### 2.3 Só membros e logout

- Quem não está logado, e o logado que não é membro, nunca recebem eventos `members`. O servidor nem os envia.
- No logout, ou quando a sessão cai, um `SessionScopedCache` apaga a lista (memória e disco) e os caches de imagem.
  Depois disso a lista pública é buscada de novo, já sem token. Nenhum evento `members` nem a imagem dele fica no
  aparelho.

---

## 3. Detalhe

- Rota `AppRoutes.EVENT_DETAIL` (`events/{eventId}`): uma única tela, `composable` inline no `AppNavHost`.
- Layout "cartaz em tela cheia": barra verde "Evento" com o botão de compartilhar à direita; o cartaz inteiro (3:2,
  sem nada por cima) logo abaixo; depois o título, as etiquetas da data curta ("14 a 16 nov"), do horário e "Só
  membros" (as duas últimas só quando existem/valem); um card com a data por extenso ("Sábado, 14 de novembro" /
  "14 a 16 de novembro", com "{n} dias" embaixo quando dura mais de um dia) e, quando existe, o local com "IPB Castelo
  Branco" embaixo e o botão "Mapa"; e "Sobre" (título com o traço verde) com a descrição, quando existe.
- **Compartilhar** abre o compartilhar do Android com um texto: título, data por extenso (e " · {hora}"), local e
  descrição, cada um só quando existe. O cartaz não vai junto (só texto).
- **Mapa** abre o app de mapas buscando o local (`geo:0,0?q={local}`); sem app que abra, nada acontece.
- Lê o evento da lista ativa em memória, por isso funciona offline. Se o evento não está mais na lista (venceu ou
  foi apagado), mostra "Este evento não está mais disponível.".

---

## 4. Gestão (Painel de Gestão → Eventos)

Área `eventsAdminGraph` dentro de `adminGraph`, com as rotas em `EventsAdminRoutes`: lista e formulário.

| Nível em `events` | O que pode |
|-------------------|------------|
| `view` | ver a lista e abrir um evento (formulário somente leitura) |
| `manage` | também criar e editar |
| `owner` | também excluir |

O card "Eventos" aparece para `events` ≥ `view`. Hoje Admin, Liderança e Mídia têm `owner`. O app segue os níveis,
nunca o nome do papel.

### 4.1 Lista

- Filtro Ativos / Passados / Todos, padrão Todos. Os mais recentes primeiro, pela data de início.
- Cada item mostra: miniatura 3:2, título, data, quem anunciou ("por Ana Paula") e o selo "Só membros" quando é
  o caso. Sem nome (conta apagada ou perfil sem nome), a linha do autor não aparece. Evento passado aparece
  esmaecido.
- O botão "Novo evento" aparece para `manage` ou mais.
- Recarrega ao voltar para a tela e ao trocar o filtro.

### 4.2 Formulário (novo, editar, somente leitura)

- Num evento que já existe, uma linha só de leitura no topo diz quem criou e quem editou por último, com as datas
  em horário de Brasília: "Criado por Ana Paula em 21/10/2026 · editado por Gabriel em 22/10/2026".
  - Sem nome, a parte fica sem ele: "Criado em 21/10/2026", "editado em 22/10/2026".
  - A parte "editado" só aparece quando o servidor informa um editor (`updated_by`); o servidor guarda só o último.
  - Num evento novo a linha não existe.
- Campos do modelo (seção 1).
  - Data de fim e horário têm a ação "remover".
  - A visibilidade é um seletor Público / Só membros.
  - A imagem passa pelo recorte travado em 3:2 (até 1800×1200).
- Regras locais, conferidas antes de enviar:
  - título obrigatório, até 120 caracteres;
  - data de início obrigatória;
  - fim ≥ início;
  - local até 200 caracteres;
  - imagem obrigatória ao criar, com pelo menos 600×400.
- Envio só em multipart. Na criação vão todos os campos preenchidos. Na edição vão só os campos alterados: um campo
  opcional limpo vai como string vazia, e a imagem só vai quando foi trocada.
- Erros do servidor (`field_errors`) aparecem embaixo do campo correspondente, e os valores digitados são mantidos.
  Chave desconhecida vira mensagem geral.
- Enquanto salva, o botão fica desligado, então dois toques não criam dois eventos. Sair com alterações não salvas
  pede confirmação.

### 4.3 Exclusão

- Só para `owner`: ação na barra do formulário. A confirmação diz "A exclusão é permanente e não pode ser
  desfeita.".
- Apaga o evento e a imagem para sempre; não há lixeira.

### 4.4 Erros

| Situação | Reação |
|----------|--------|
| Leitura recusada (403) | sai da área com a mensagem de permissão (o perfil é atualizado pelo interceptor) |
| Escrita recusada (403) | mensagem; continua no formulário |
| Evento não existe mais (404) | volta para a lista com "Este evento não existe mais." |
| Rede / servidor | lista: estado de erro com "Tentar novamente"; formulário: mensagem, valores mantidos |

---

## 5. Fora de escopo (futuro)

- Push quando um evento é publicado.
- "Adicionar à agenda".
- Compartilhar o cartaz junto com o texto.
- Link de inscrição.
- Tela "Todos os eventos".
- Tela de "Eventos passados" para membros.
