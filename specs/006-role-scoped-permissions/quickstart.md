# Quickstart: validating role-scoped permissions

## Prerequisites

- Backend with feature 012 deployed (profile returns `roles` and `permissions`, no `is_admin`).
- Django admin access to assign roles.
- Four accounts: **Admin**, **Liderança**, **Mídia**, and a **member without role**. Ideally one Liderança who is
  also a church member (Gerar Escala needs `is_member`, see limitation).

## Automated checks

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.domain.access.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.profile.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.*"
./gradlew :app:testDebugUnitTest
```

Expected: role × surface matrix (`VisiblePanelCardsUseCaseTest`, screen-flag tests), DTO decoding of the old and new
shapes, 403 text, single-flight refresh — all green.

## Manual walkthrough

| # | Scenario | Expected |
|---|----------|----------|
| 1 | Install the **previous** app version, sign in as Admin, open the panel; then install this version over it and open the app **offline** | No crash; home loads with name/photo; no "Painel de Gestão" in the menu |
| 2 | Go online, wait for the profile refresh | "Painel de Gestão" appears; title reads "Painel de Gestão" |
| 3 | Admin → panel | All nine cards, same colours and grey states as before |
| 4 | Liderança → panel | Louvor, Escala, Membros, Avisos (grey), Relatórios, Galeria (grey), Eventos (grey); no Presença, no Notificações |
| 5 | Mídia → panel | Avisos, Relatórios, Galeria, Eventos only |
| 6 | Member without role → menu | No panel entry; member content as today |
| 7 | Liderança → Membros → a member | Edit and change photo present; no "Excluir membro", no "Remover foto"; "+" on the list present |
| 8 | Admin → same member | All actions present |
| 9 | Liderança → Relatórios → hinário | Report loads; menu shows "Janelas de culto" only, no "Parâmetros de coleta" |
| 10 | Liderança → Janelas de culto | List shown; no add, edit, delete or toggle |
| 11 | Liderança → Cifras / Letras | Create and edit buttons present; Mídia: absent |
| 12 | Admin on a member's profile; in Django admin swap the account to Liderança; tap "Excluir membro" and confirm | "Você não tem permissão para esta ação."; stays on the profile; the delete button disappears within a moment |
| 13 | Liderança on the members list; remove every role in Django admin; pull to refresh | Message; back to the panel; panel shows empty state or the entry disappears from the menu |
| 14 | Logout, sign in as Mídia | No Liderança cards left over |
| 15 | Liderança **not** a member → Gerar Escala | Permission message (known limitation, backend) |
