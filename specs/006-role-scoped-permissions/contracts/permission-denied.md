# Contract: how the app reacts to a 403

Backend side: `specs/012-feature-role-permissions/contracts/management-access.md` — a missing level answers

```json
{"error_code": "PERMISSION_DENIED", "detail": "Você não tem permissão para esta ação."}
```

## 1. Text on screen

| Response                                        | `AppError`                                      | Text shown                                   |
|-------------------------------------------------|-------------------------------------------------|----------------------------------------------|
| 403 with `error_code` + `detail`                | `Auth(403, userMessage = detail)`               | the `detail`                                 |
| 403 with no structured body                     | `Auth(403, userMessage = null)`                 | "Você não tem permissão para esta ação."     |
| 401 (after the token refresh failed)            | `Auth(401)`                                     | "Faça login para continuar." (unchanged)     |

## 2. Profile refresh

| Step | Who                                  | What                                                                  |
|------|--------------------------------------|-----------------------------------------------------------------------|
| 1    | `PermissionDeniedInterceptor` (`@Client`) | 403 + `error_code == "PERMISSION_DENIED"` → `AuthEventBus.Event.PermissionDenied`; response untouched |
| 2    | `CoreViewModel`                      | on the event → `RefreshAccessUseCase()`                               |
| 3    | `ProfileAccessRepository.refresh()`  | single-flight; refreshes the profile snapshot                         |
| 4    | every screen observing access        | flags recomputed; lost actions disappear                              |

A 403 without `error_code` does not trigger the refresh (it is not a scope refusal the backend vouches for).
The unauthenticated client (`@AuthLessClient`) does not carry the interceptor.

## 3. Navigation

| Area      | Refused request                                           | Kind  | Reaction                                   |
|-----------|-----------------------------------------------------------|-------|--------------------------------------------|
| Members   | list, profile, history, form load (record / options)      | READ  | message + leave `MembersRoutes.GRAPH`      |
| Members   | save, validity toggle, photo upload/removal, delete       | WRITE | message; stay; input kept                  |
| Reports   | report load, windows list                                 | READ  | message + leave `ReportsRoutes.GRAPH`      |
| Reports   | window create/edit/delete/toggle, settings save           | WRITE | message; stay (unchanged)                  |
| Louvor, Escala | any                                                  | —     | message only (unchanged flow)              |
| Worship hub | create/edit chord chart or lyrics                       | WRITE | message; stay (unchanged flow)             |

"Leave" pops the area's nested graph, landing on the panel. The collection settings read
(`GET api/hymnal-history/settings/`) is public, so it never produces a scope refusal; only its save can.
