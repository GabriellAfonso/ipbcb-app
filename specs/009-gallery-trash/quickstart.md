# Quickstart: Gallery Trash

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
./gradlew :app:assembleDebug
```

| Behaviour (request's test list) | Test |
|---------------------------------|------|
| Entry point only with `owner`, live | `GalleryViewModelManageTest` — `showTrash` follows `canDelete` |
| List mapping: null names, counts, purge date, empty | `TrashTextsTest`, `TrashViewModelTest` (empty) |
| Restore success applies locally without moving the cursor and syncs | `GalleryManageRepositoryImplTest` (restore), `TrashViewModelTest` (row leaves, message) |
| 404 reloads the list | `TrashViewModelTest` |
| Trashed parent highlights when listed, detail only when not | `TrashViewModelTest` |
| Name conflict exposes the conflicting album | `RestoreTrashItemUseCaseTest` (sync when missing), `TrashViewModelTest` (event with album id) |
| One restore at a time | `TrashViewModelTest` |
| Undo after a single delete, none after a batch | `GalleryViewModelManageTest` |

## Manual (device, test server)

Prerequisites: an account with `owner` on `gallery` flagged as member; a second account with `manage` only.

1. `manage` account: gallery root has no trash icon. `owner` account: icon present; hidden during "Organizar".
2. Delete one photo from the viewer → snackbar "Foto enviada para a lixeira" + "Desfazer" → tap → "Foto restaurada",
   photo back in place.
3. Delete an album with sub-albums and photos → open the trash → row shows counts, deleted by/at, "Some em" → restore →
   album back, then its content appears after the sync.
4. Delete a sub-album, then its parent; restore the sub-album → parent row highlighted and scrolled to.
5. Delete album "X", create a new "X" at the same place, restore the old one → message + "Abrir álbum" → rename →
   back → "Restaurar" works.
6. Restore the same item from two devices → second sees "Este item não está mais na lixeira." and the list reloads.
7. Airplane mode → open the trash → "Sem conexão. A lixeira precisa de internet." + "Tentar novamente".
