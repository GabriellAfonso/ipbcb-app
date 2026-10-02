# Quickstart: Sunday Setlist — Draft, Save, Push and Play Confirmation

## Automated

```bash
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest -q
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.worshiphub.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.*"
```

| Behaviour | Test |
|-----------|------|
| Draft restored within 1 h, discarded after; every change renews; restore does not renew | `DraftExpiryTest`, `SongsTableViewModelDraftTest` |
| Draft row with a song missing from the catalog comes back empty; waits for the catalog | `SongsTableViewModelDraftTest` |
| "Limpar repertório" empties rows and storage | `SongsTableViewModelDraftTest`, `RepertoireDraftStorageTest` |
| Save date: Sunday → today, any other day → next Sunday | `SetlistDateTest` |
| "Salvar" visible only with `can_save_setlist`; disabled without filled rows / with missing or long key | `RepertoireValidationTest`, `SongsTableViewModelSaveTest` |
| Only filled rows sent, positions kept | `SaveSundaySetlistUseCaseTest` |
| Save success stores the returned setlist; each failure maps to its text (403, 400, 404 + count, offline) | `SetlistSaveRepositoryTest`, `SaveSundaySetlistUseCaseTest`, `RepertoireTextsTest` |
| Push payload parsing: two types, bad/missing date, unknown type | `PushMessageTest` |
| Handler gates: logged out, not worship member, foreground → no notification | `PushMessageHandlerTest` |
| Token registered on start/login/rotation; worker no-ops when logged out; retry on network error | `PushTokenSyncWorkerTest` (or use-case test with fake scheduler), `RegisterDeviceUseCaseTest` |
| Unregister before token clear; failure and timeout never block logout | `UnregisterDeviceUseCaseTest`, `CoreViewModelLogoutTest` |
| Current setlist: stored, `null` clears, 403 clears, network error keeps | `SundaySetlistRepositoryImplTest`, `SyncSundaySetlistUseCaseTest` |
| Section hidden after its Sunday; clear on logout and on losing worship membership | `ObserveSundaySetlistUseCaseTest`, `SyncSundaySetlistUseCaseTest` |
| `is_worship_member` / `can_save_setlist` absent → false | `MeProfileDtoBackwardCompatibilityTest`, `ProfileWorshipAccessRepositoryTest` |
| Section in Letras/Cifras: order, keys, songs without content dropped, no duplicates with pins | `SundaySectionTest`, `LyricsViewModelTest`, `ChordChartsViewModelTest` |
| Notification extras → target; invalid extras ignored | `NotificationTargetTest` |
| Prefill: loaded, 404 → empty + message, failure → retry | `MusicRegistrationViewModelPrefillTest` |
| Pending card: hidden without access / empty list, dates newest first, refresh on resume | `AdminPanelViewModelPendingTest` |

Fakes: `FakeSundaySetlistRepository`, `FakeWorshipAccessRepository` (core/testing), `FakeSetlistConfirmationRepository`,
`FakeRepertoireDraftRepository`, `FakePushTokenSource`, `FakeDevicesApi`, `FakeWallClock`.

## Manual (device, test server)

Prerequisites: test server with backend 017 and push credentials; an account with `manage` on `songs` linked to a
"Louvor" member (leader); a "Louvor" member without `manage` (band); a member outside "Louvor"; notification
permission granted on the devices.

1. **Draft** — leader: Repertório → pick 2 songs, set keys, pin one → kill the app → reopen: rows identical. Wait > 1 h
   (or move the device clock) → reopen: empty. "Limpar repertório" empties at once.
2. **Save** — leader on a Wednesday: "Salvar" → dialog names next Sunday dd/MM → confirm → "Repertório de domingo dd/MM
   salvo."; Letras shows the section immediately. Clear the key of a filled row → "Salvar" disabled. Band account: no
   "Salvar", "Compartilhar" works.
3. **Push** — band device in background; leader saves → within a minute "Repertório de domingo dd/MM disponível";
   tap → Letras with the section on top, keys shown, songs without lyrics absent. Cifras shows its own subset.
   Airplane mode → restart app → section still there.
4. **Fallback** — band device: disable notifications, save from leader, bring the band app to the foreground → section
   updated without any push.
5. **Logout** — log out on the band device (also once with airplane mode on: logout completes) → save again from leader
   → nothing arrives on that device.
6. **Old login** — install the previous version, log in, update to this version without logging out → save from
   leader → the device receives the push.
7. **Confirm plays** — trigger `confirm_plays` (backend reminder or the backend quickstart's manual send) for a date with
   a setlist → "Confirmar músicas de domingo" → tap → register screen, Sunday mode, date fixed, rows = setlist with keys
   → send → admin panel no longer lists that Sunday. Tap a `confirm_plays` notification on the band account → home +
   "Você não tem mais acesso ao registro de músicas.".
8. **Pending card** — leader with two past setlists without plays → panel shows the card, newest first → tap one →
   prefilled screen → register → back → only the other remains. None pending → no card.
9. **End of Sunday** — move the device clock to Monday → Letras without the section.
