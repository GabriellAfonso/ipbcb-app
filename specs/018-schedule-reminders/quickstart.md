# Quickstart: Rota Reminders in the App

## Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.domain.push.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.data.push.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.schedule.*"
```

Expected: parsing accepts the three contract payloads and rejects malformed ones; the handler shows nothing signed
out or with the switch off and shows in the foreground; texts match `contracts/ui.md`; a date picks the tab.

## Manual check on a device (debug build, signed in)

FCM data messages cannot be sent with `adb` directly; until the backend sends them, use the Firebase console
("Cloud Messaging" → test message, custom data) or the backend's push sender from a Django shell with the payloads
in `contracts/ui.md`.

1. Send `schedule_eve` for a date in the current month → "Amanhã: …" appears; tap → Escala on that service's tab.
2. Send `schedule_week` with two slots → "2 cultos nesta semana"; expand → two lines.
3. Send `schedule_published` with `month` next month → "Escala de {mês} publicada"; tap → Escala, default tab.
4. Configurações › Notificações › Escala off; send any → nothing. On again → permission flow if notifications are
   blocked.
5. System settings › app notifications → a separate "Escala" channel exists.
