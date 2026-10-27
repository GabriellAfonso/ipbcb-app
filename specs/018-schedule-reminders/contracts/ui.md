# UI Contract: Rota Reminders

## Incoming push (from `backend/specs/024-schedule-reminders/spec.md`, Payload)

```json
{ "type": "schedule_week", "slots": "[{\"date\":\"2026-10-27\",\"service\":\"Terça de Oração\",\"time\":\"19:30\"}]" }
{ "type": "schedule_eve", "slots": "[{\"date\":\"2026-10-29\",\"service\":\"Quinta de Oração\",\"time\":\"19:30\"}]", "gender": "F" }
{ "type": "schedule_published", "month": "2026-11", "slots": "[...]" }
```

`gender` (`M`/`F`, the recipient's own) is optional on every kind. `F` → feminine wording; `M`, absent or unknown →
masculine. Never makes a message invalid.

## Notifications — channel `schedule` / "Escala"

Formats: time `19h30` (`HH` + `h` + `mm`), date `dd/MM`, weekday lowercase without "-feira" (`terça`, `quinta`,
`domingo`, `sábado`), month lowercase (`novembro`).

Slot line: `{service} · {weekday} {dd/MM} · {time}` → "Quinta de Oração · quinta 29/10 · 19h30".

| Kind | Id | Title | Collapsed text | Expanded |
|------|----|-------|----------------|----------|
| week, 1 slot | 3201 | Você está na escala desta semana | slot line | same text |
| week, n ≥ 2 | 3201 | Você está na escala desta semana | `{n} cultos nesta semana` | one slot line each |
| eve | 3202 | `Amanhã: {service}` | `Você é o/a responsável · {dd/MM} às {time}` | same text |
| published | 3203 | `Escala de {mês} publicada` | `Você está escalado/a em 1 data` / `… {n} datas` | one slot line each |

All: small icon `ic_notification`, accent `notification_accent`, auto-cancel, default priority, shown only with
notification permission, tap → `CoreActivity` with `EXTRA_TARGET = "schedule"` and `EXTRA_DATE` = first slot date.

## Tap

`AppNavHost` (after boot is ready) → `schedule?date=YYYY-MM-DD`. Escala opens on the first tab with an entry on that
date when the loaded month contains it; otherwise the next-service tab.

## Settings — section "Notificações"

| Order | Label | Control | Behavior |
|-------|-------|---------|----------|
| 1 | Aniversários | Switch | unchanged |
| 2 | Escala | Switch, "Ativado"/"Desativado", default on | turning on runs the notification-access request (settings §2.1) |
