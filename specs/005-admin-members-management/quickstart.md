# Quickstart: Members Management for Church Leaders

How to prove the feature works. Details live in [data-model.md](data-model.md) and
[contracts/admin-members-api.md](contracts/admin-members-api.md).

## Prerequisites

- Backend with feature `010-members-management` and the `members/` media rule (`009`) deployed.
- Two accounts: a leader (`is_admin=true`) and a regular member.
- A roll with valid and invalid members, some with status Ativo / Inativo / Visitante, some without status,
  photo, role or dates.

## 1. Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.members.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.presentation.viewmodel.*"
./gradlew :app:testDebugUnitTest
```

Expected: all green (no `clean`, no `--rerun-tasks` — `CLAUDE.md`).

## 2. Audits (grep)

```bash
# No member data in logs: every Timber call in the feature logs ids only
grep -rn "Timber\." app/src/main/java/com/ipb/castelobranco/features/admin/members/

# Nothing in the feature writes files (only the shared picker touches cacheDir, and deletes it)
grep -rnE "filesDir|cacheDir|FileOutputStream|DataStore|SnapshotStorage" \
  app/src/main/java/com/ipb/castelobranco/features/admin/members/

# Status names are never hardcoded
grep -rnE "\"(Ativo|Inativo|Visitante|Comungante)\"" \
  app/src/main/java/com/ipb/castelobranco/features/admin/members/
```

Expected: logs show only ids; no storage hits; status names appear only as fake data inside `@Preview`
functions, never in logic.

## 3. Device checks

| # | Steps | Expected |
|---|---|---|
| 1 | Sign in as a regular member | No admin panel, no members entry anywhere |
| 2 | Sign in as leader → Painel Admin | "Membros" card is blue and opens the grid |
| 3 | Grid | All members, invalid ones faded with "Perfil inválido", status chips as the server names them |
| 4 | Search "jose" | Only José/Jose matches; clearing restores all |
| 5 | Open a full profile | Age, "há N anos", ministries |
| 6 | Open an empty profile | "Não informado", "Sem cargo", "Nenhum ministério", "Sem situação" |
| 6a | Member with day+month only; clear the year of a full date | "dd/MM", age "Desconhecida"; day and month stay |
| 6b | Member with year only | "yyyy", "N anos" (current year − year) |
| 7 | Toggle "Perfil válido" off | Saved at once; card in grid shows "Perfil inválido" |
| 8 | Toggle with airplane mode | Switch reverts, connection message |
| 9 | Novo membro, blank name → Salvar | Error on Nome, nothing sent |
| 10 | Birth 2030 / baptism before birth | Errors on the date fields |
| 11 | Create full member | Profile opens; member in the grid |
| 12 | Edit only Situação | Only `status_id` in the PATCH (debug log shows one request); profile updated |
| 13 | Edit, change a field, press back | Discard confirmation |
| 14 | Add photo → replace → remove | Grid and profile follow; history gains "trocou a foto" / "removeu a foto" |
| 14a | Tap photo → tap square → camera | Square pops over dimmed screen; full screen with camera top-left |
| 14c | Member without photo: tap initials | Initials pop as a square, never the picker; camera in full screen |
| 14b | Edit form: camera → pick → Salvar | Preview before saving; photo uploaded after the fields; discard keeps old |
| 15 | Histórico | Sentences in Portuguese, dd/MM/yyyy, "Usuário removido" for a deleted editor |
| 16 | Excluir: type wrong name / right name | Button disabled / member deleted, back to grid without it |
| 17 | In Django admin, remove leader rights, then act in the app | Server message, back to admin panel |
| 18 | Delete a member in Django admin, open it in the app | "Este membro não existe mais", back to grid |
| 19 | Sign out, sign in as another leader | Grid loads fresh; nothing from the previous session |
| 20 | Device File Explorer: `files/`, `cache/` after using everything | No member JSON or member photos |
