# Quickstart: Gallery Member Tags

## Automated

```bash
./gradlew.bat :app:assembleDebug :app:testDebugUnitTest -q
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
```

| Behaviour (request's test list) | Test |
|---------------------------------|------|
| AND filter with 1, 2 and 3 people, and an empty result | `GalleryPeopleTest` |
| People list from the index: counts, order, accent-insensitive search | `GalleryPeopleTest`, `NameSearchTest`, `PeopleViewModelTest` |
| "Minhas fotos" only with a member id, live | `GalleryViewModelTagsTest` (root), `PeopleViewModelTest` (mine mode) |
| Single-photo save sends the full set; nothing when unchanged | `TagUseCasesTest`, `GalleryViewModelTagsTest` |
| Bulk add/remove chunked at 200; remove list from the selection | `TagUseCasesTest`, `GalleryViewModelTagsTest` |
| 404 atomic failure syncs and shows the message | `GalleryManageRepositoryImplTest`, `GalleryViewModelTagsTest` |
| Returned photos applied locally without moving the cursor | `GalleryManageRepositoryImplTest`, `GalleryIndexApplyTest` |
| Viewer from a filter pages through the result; leaving = removed | `GalleryViewModelTagsTest` |
| `member_id` null/absent decodes | `MeProfileDtoBackwardCompatibilityTest`, `ProfileCurrentMemberRepositoryTest` |
| Texts | `TagTextsTest` |

## Manual (device, test server)

Prerequisites: an account with `manage` on `gallery` linked to a member in the Django admin; a plain member account
not linked.

1. Open a photo → "ⓘ" → sheet with description, date, "Nesta foto" / "Ninguém marcado". Swipe: the sheet follows.
2. `manage`: "Marcar pessoas" → list loads, search "joao" finds "João" → check two → "Salvar" → "Marcações salvas",
   sheet shows both names. Open again, "Salvar" without change → closes, no request.
3. Album → long press → select 3 → "Pessoas" → "Adicionar pessoas" → pick one → "Marcações atualizadas em 3 fotos".
   Again → "Remover pessoas" lists only people in those photos.
4. Root → "Pessoas" → pick A, then B → hint and AND grid → open a photo → swipe through the result only. Untag B from
   it in the Django admin, pull a sync → "Esta foto não está mais no resultado".
5. Linked account → "Minhas fotos" visible with its photos; unlink in the admin, re-read profile → entry disappears.
   Unlinked account → no entry.
6. Delete a member in the admin while the picker is open with them checked → "Salvar" → not-found message, list
   reloads.
7. Airplane mode → the filter and "Minhas fotos" work; the picker shows "Sem conexão" + "Tentar novamente".
