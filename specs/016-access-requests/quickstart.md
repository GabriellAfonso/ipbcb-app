# Quickstart: Access Requests

Needs backend 022 deployed (dev) and three accounts: A (no access), L (Liderança), M (Mídia).

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.profile.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.profiles.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.*"
```

## Device checks

1. A opens Galeria → "Disponível apenas para membros." + "Pedir acesso". Sends with a message → toast; Galeria,
   Escala and Aniversariantes show "Pedido enviado. Aguardando a liderança.".
2. A sends again from the profile within 24 h → the server's limit message in the dialog; still signed in.
3. L gets "Novo pedido de acesso"; tap → Perfis › Pendentes lists A.
4. L opens A, taps "Aprovar" → A leaves Pendentes; A gets "Acesso liberado"; A's Galeria loads.
5. L revokes A (confirm) → A's next refresh shows locked screens with "Pedir acesso".
6. L blocks A → A sees the blocked text, no buttons, on locked screens and the profile.
7. L links A to a roll member → A's profile shows "Meus dados na igreja"; "Solicitar correção" sends; L denies with a
   reason → A gets "Pedido de correção negado" with the reason.
8. M opens Gestão → no "Perfis".
9. Old app snapshot: install the previous build, sign in, update to this build offline → profile opens.
10. Roll data: after step 7, `adb shell run-as com.ipb.castelobranco ls -R files cache` and grep the snapshot for the
    member's role/ministry → nothing found.
11. No e-mail anywhere in Perfis or the profile.
