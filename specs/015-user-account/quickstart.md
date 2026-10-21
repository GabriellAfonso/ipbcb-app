# Quickstart: User Account

## Automated

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.profile.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.network.TokenAuthenticatorTest"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.presentation.viewmodel.CoreViewModelTest"
```

Expected: all green. Key cases: old profile JSON decodes; password pair stored before the use case returns; blacklisted
refresh plus an already stored new pair does not sign out; deletion cleanup runs every logout step except the server
unregister.

## Manual (debug build against the dev backend)

1. Password account: open Perfil → see Login and "No app desde"; no "Membro" line.
2. Edit the name with spaces around it → trimmed name shown, toast "Nome atualizado".
3. With a photo: tap → "Remover foto" → confirm → placeholder; restart → still placeholder.
4. "Trocar senha" with a wrong current password → "Senha incorreta." under the field, still signed in.
5. "Trocar senha" correctly while signed in on a second device → this device keeps working (open Galeria); the second
   device falls to signed out on its next request.
6. "Excluir conta" → type EXCLUIR + password → home signed out, toast "Conta excluída"; old credentials fail.
7. Google-only account: no "Trocar senha"; "Excluir conta" → EXCLUIR → "Confirmar com o Google" → cancel picker →
   nothing shown; repeat choosing another Google account → mismatch message; repeat with the right one → deleted.
