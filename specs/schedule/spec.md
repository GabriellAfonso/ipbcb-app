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

## 2. Tela da escala (`AppRoutes.SCHEDULE`, inline no `AppNavHost`)

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
culto de hoje ate as 22h, senao o proximo dia de culto da semana). Sem correspondencia, a primeira aba. A aba
escolhida sobrevive a rotacao.

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
