# Escala — Spec

A escala do mes mostra quem dirige ou e responsavel por cada culto do mes corrente: Terca de Oracao, Quinta de
Oracao e Domingo Liturgia de Adoracao. Conteudo so para membros. A geracao da escala e do painel de gestao
(`specs/admin/spec.md` §4); o card da home que mostra o proximo culto esta em `specs/core/spec.md` (Carousel de
Destaques).

## 1. Dados

`GET api/schedule/current/` (`@AuthedRetrofit`, `If-None-Match`/ETag). Snapshot offline via `JsonSnapshotStorage`
(`ScheduleSnapshotModule`).

```
{
  "year": int, "month": int,
  "schedule": {
    "<titulo do culto>": {
      "time": string,            // ex. "19h30"; pode vir vazio
      "items": [{ "day": int, "member": { "id": int, "name": string }, "date": string?, "schedule_type": {...}? }]
    }
  }
}
```

**Modelos de dominio:** `MonthSchedule(year, month, schedule: Map<String, ScheduleEntry>)`,
`ScheduleEntry(time, items)`, `ScheduleItem(day, member)` (so o nome do membro).

**Ordem dos cultos:** terca, quinta, domingo (pelo inicio do titulo, sem diferenciar maiusculas); titulos fora
disso vao para o fim, em ordem alfabetica. Datas de cada culto em ordem crescente.

## 2. Tela da escala (`AppRoutes.SCHEDULE_ROUTE`, inline no `AppNavHost`)

Rota `schedule?date={date}`: da home abre sem data (`AppRoutes.SCHEDULE`); o toque num lembrete da escala (secao 4)
passa a data `YYYY-MM-DD` do primeiro culto da mensagem.

**Estados:** carregando (spinner); vazia ("Nenhuma escala encontrada."); erro 403 (`MembersOnlyPlaceholder`);
outros erros (`PermissionErrorPlaceholder` com botao de login); sucesso (abaixo). Pull-to-refresh rele a escala.

**Layout (sucesso):** do topo para baixo, o mais escuro em cima, na mesma linguagem das Tabelas
(`specs/worshiphub/spec.md` §2):
- TopBar "Escala" com icone de compartilhar.
- Abas no bloco verde (`ipbGreen`, via `topBarExtension`), uma por culto: nome curto (primeira palavra do
  titulo: "Terça", "Quinta", "Domingo") e o horario embaixo. Aba ativa com fundo branco translucido e sublinhado
  verde-menta.
- Abaixo, a folha de conteudo com cantos superiores arredondados (`surfaceDim`).

**Aba inicial:** a do proximo culto, pela mesma regra do card da home (`ScheduleViewModel.findNextSection`: o
culto de hoje ate as 22h, senao o proximo dia de culto da semana). Sem correspondencia, a primeira aba. Aberta por
um lembrete com data: se o mes carregado tem essa data, a primeira aba (na ordem das abas) com uma linha nesse dia;
senao a regra acima. A data so escolhe a aba: o cartao "Próximo" e a linha destacada seguem as regras de sempre.
A aba escolhida sobrevive a rotacao.

**Conteudo de uma aba:**
- Cartao "Próximo" (`surfaceContainer`): rotulo "PRÓXIMO · {dia curto em minusculas} dd/MM · {horario}" (ex.
  "PRÓXIMO · terça 27/10 · 19h30"), o nome do responsavel grande e o titulo completo do culto embaixo. Sem data
  futura no mes: "Todos os {dia curto}s de {mes} já passaram." (ex. "Todos os domingos de outubro já passaram.").
- Titulo "Todas as datas" centralizado entre duas linhas finas.
- Cartao com uma linha por data: dia com dois digitos e o dia da semana abreviado embaixo ("27" / "TER"), e o
  nome do responsavel.
  - **Proxima data:** fundo verde claro, faixa verde de 3dp a esquerda, dia e nome em verde escuro e negrito.
    Sem etiqueta "Próximo" na linha: o cartao de cima ja diz qual e. Precisa ser bem visivel nos dois temas.
  - **Datas passadas:** esmaecidas.
- Rodape: "*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo." e "*Aberto à participação de qualquer irmão."

**Proxima data de cada culto:** `resolveNextDay` — primeira data >= hoje no mes corrente; mes futuro, a primeira;
mes passado, nenhuma (todas passadas). Calculada no ViewModel, nunca no Composable.

**Compartilhar:** abre o seletor do sistema com o texto de `MonthScheduleWhatsappFormatter.format` (cabecalho
"ESCALA DE {MÊS} {ANO} - DIRIGENTES E RESPONSÁVEIS", cada culto com horario e linhas "dd- Nome", os avisos do
cafezinho e "DEUS ABENÇOE").

## 3. Fora do escopo

- Navegar para outros meses: o endpoint so devolve o mes corrente.
- Agendar lembretes no aparelho: quem decide quando lembrar e o servidor (secao 4).

## 4. Lembretes da escala (push)

O servidor (`backend/specs/024-schedule-reminders/spec.md`) avisa por push quem esta na escala; o app so mostra,
deixa desligar e abre a Escala no toque. Detalhes em `specs/018-schedule-reminders/`.

| `type` | Campos | Quando |
|--------|--------|--------|
| `schedule_week` | `slots` | Segunda 08h: as datas da pessoa de segunda a domingo |
| `schedule_eve` | `slots` (exatamente um) | Vespera de cada data, 19h |
| `schedule_published` | `month` (`YYYY-MM`), `slots` | Escala do mes ficou definitiva (30 min apos o primeiro save) |

`slots` e uma string JSON: lista de `{date: "YYYY-MM-DD", service: nome do culto, time: "HH:MM"}`, em ordem.
Mensagem malformada (JSON invalido, lista vazia, campo invalido, vespera com mais de um slot, mes invalido) e
ignorada. `gender` (`M`/`F`, o da propria pessoa) e opcional nos tres: `F` usa "a responsável" / "escalada"; `M`,
ausente ou outro valor usa "o responsável" / "escalado", sem invalidar a mensagem.

- Ignorada deslogado e com o switch "Escala" desligado (`specs/settings/spec.md`). Mostrada na hora a partir da
  mensagem, sem rede, inclusive com o app aberto. Nao atualiza o snapshot da escala.
- Canal proprio "Escala"; um id fixo por tipo (o mais novo substitui o anterior).
- Formatos: horario "19h30", data dd/MM, dia da semana em minusculas sem "-feira" ("quinta").

| Tipo | Titulo | Texto | Expandida |
|------|--------|-------|-----------|
| semana, 1 data | Você está na escala desta semana | "Quinta de Oração · quinta 29/10 · 19h30" | — |
| semana, 2+ | Você está na escala desta semana | "{n} cultos nesta semana" | uma linha por data |
| vespera | Amanhã: {culto} | "Você é o/a responsável · 29/10 às 19h30" | — |
| publicada | Escala de {mês} publicada | "Você está escalado/a em {n} data(s)" | uma linha por data |

Toque: abre a Escala com a data do primeiro slot (secao 2). Deslogado ou sem acesso de membro, a tela mostra os
proprios placeholders. Uma escala publicada de um mes que ainda nao comecou abre o mes corrente na aba padrao.
