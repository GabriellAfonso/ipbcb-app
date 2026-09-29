# Contract: `GET/PATCH api/me/profile/` as the app reads it

Source of truth: backend `specs/012-feature-role-permissions/contracts/profile-api.md`. This file fixes only how the
app consumes it. Client: `@AuthedRetrofit`, `ProfileApi`, ETag via `RetrofitSnapshotFetcher` — unchanged.

## Response the app expects

```json
{
  "name": "Ana Paula",
  "is_member": true,
  "photo_url": "https://…/ipbcb/media/profiles/ana.paula/6f1c2d.png",
  "roles": [{"id": "leader", "name": "Liderança"}],
  "permissions": {
    "members": "manage", "schedule": "manage", "songs": "manage",
    "gallery": "manage", "events": "manage", "notices": "manage",
    "reports.hymnal_history": "view"
  }
}
```

## Parsing rules

| Input                                                 | App result                                          |
|-------------------------------------------------------|-----------------------------------------------------|
| `roles` absent                                        | no role (`hasAnyRole = false`)                      |
| `roles` with an unknown `id`                          | role ignored for `holds()`, still counts for `hasAnyRole` |
| `permissions` absent                                  | no level on any scope                               |
| scope key missing                                     | no access to that scope                             |
| unknown scope key                                     | ignored                                             |
| value `null`                                          | no access                                           |
| value other than `view` / `manage` / `owner`          | no access                                           |
| `is_admin` present (old cache on disk)                | ignored                                             |
| Old cache file (`is_admin`, no `roles`/`permissions`) | decodes; no role, no levels; name, membership and photo kept |

No input of this endpoint may make deserialization throw because of `roles` or `permissions`.

## Examples → visible panel cards

| Profile                                   | Panel entry | Cards                                                               |
|-------------------------------------------|-------------|---------------------------------------------------------------------|
| `roles: []`, all `null`                   | no          | —                                                                   |
| Admin (all `owner`)                       | yes         | all nine                                                            |
| Liderança (matrix levels)                 | yes         | Louvor, Escala, Membros, Avisos, Relatórios, Galeria, Eventos       |
| Mídia (matrix levels)                     | yes         | Avisos, Relatórios, Galeria, Eventos                                |
| `roles: [{"id":"x"}]`, all `null`         | yes         | none → empty-state message                                          |
