# Implementation Plan: Access Requests

**Branch**: `016-access-requests` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/016-access-requests/spec.md`

## Summary

The profile DTO reads `member_access` (falling back to `is_member`) plus the access status, the request block and the
pending correction. A core port `MemberAccessRepository` (implemented in the profile feature over the snapshot)
feeds a shared `MembersOnlyPlaceholder` that gallery, schedule and birthdays use on a 403: "Pedir acesso", the
pending text or the blocked text. The profile gets an access card and, when linked, the person's roll data from
`GET api/me/member/`, held only in UI state. Gestão gets "Perfis" (`features/admin/profiles`, new scope `profiles`):
Pendentes / Todos lists and an account detail with approve, deny, revoke, block and link. Three new pushes reuse the
setlist push path with a new channel and two new notification targets. Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, Retrofit + OkHttp, kotlinx.serialization, Coil 2, Firebase Messaging (all present)

**Storage**: profile snapshot `JsonSnapshotStorage` `me_profile` (new fields additive); roll data and Perfis lists in
memory only

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred

**Target Platform**: Android (minSdk 24, desugaring on)

**Project Type**: mobile app, single `:app` module

**Performance Goals**: none beyond the usual; every action is one request plus one refresh.

**Constraints**:
- roll data never on disk or in logs;
- no e-mail anywhere;
- an old snapshot keeps decoding;
- 120-char lines;
- user-visible strings in Portuguese.

**Scale/Scope**: 3 locked screens touched, profile card + section + 2 dialogs, 2 new admin screens + 3 dialogs,
8 endpoints, 3 push types.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule (`specs/constitution.md`, `CLAUDE.md`) | Status |
|---|---|
| `AppError` is the only error type crossing data → presentation | Pass: repositories `mapError()`; dialogs show `toUserMessage()` |
| Only `ResponseExt.kt` parses error bodies | Pass: unchanged; `MEMBER_NOT_LINKED` is read from `AppError.Server.errorCode` |
| `userMessage` only from a structured `detail` | Pass: request refusals show the server `detail` |
| 403 is permission, not login | Pass: a blocked request (403) shows its text and never signs out |
| Features never import each other | Pass: locked screens use the core port + core placeholder; Perfis reuses Membros inside `admin` |
| `domain/` has no Android | Pass |
| Screen/content split, `StateFlow` + `SharedFlow` events | Pass: placeholder = collector + pure content; same for the new screens |
| Member data never touches the disk | Pass: roll data in UI state; account photos through a memory-only image loader |

No violations; Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/016-access-requests/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/ui.md
├── checklists/requirements.md
└── tasks.md
specs/profile/spec.md, specs/admin/…, specs/core/…   # domain specs updated with the feature
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/access/Scope.kt                         # + PROFILES
│   ├── domain/access/MemberAccess.kt                  # AccessStatus, MemberAccess, AccessRequestType, prompt
│   ├── domain/access/MemberAccessRepository.kt        # port + use cases
│   ├── domain/push/PushMessage.kt                     # + 3 access request messages
│   ├── data/push/PushMessageHandler.kt                # dispatch + profile refresh
│   ├── data/push/AccessRequestNotifications.kt        # channel "Pedidos de acesso"
│   ├── presentation/components/access/                # MembersOnlyPlaceholder, RequestDialog, VM
│   ├── presentation/navigation/NotificationTarget.kt  # + ProfilesPending, Profile
│   ├── presentation/navigation/AppNavHost.kt          # route the targets
│   ├── presentation/screens/BirthdaysScreen.kt        # 403 → placeholder
│   └── presentation/viewmodel/CoreViewModel.kt        # canOpenProfiles()
├── features/profile/
│   ├── data/dto/MeProfileDto.kt, OwnMemberDto.kt, AccessRequestDtos.kt
│   ├── data/api/ProfileApi.kt, ProfileEndpoints.kt   # POST me/access-requests/, GET me/member/
│   ├── data/access/ProfileMemberAccessRepository.kt   # implements the core port
│   ├── data/repository/OwnMemberRepositoryImpl.kt
│   ├── domain/model/MeProfile.kt, OwnMember.kt
│   └── presentation/…                                 # access card, roll section, dialogs, VM
├── features/gallery/presentation/screens/GalleryScreen.kt    # 403 → placeholder
├── features/schedule/presentation/screens/ScheduleScreen.kt  # 403 → placeholder
└── features/admin/
    ├── panel/domain/PanelCard.kt, panel/presentation/screens/AdminScreen.kt, navigation/AdminNavGraph.kt
    └── profiles/
        ├── data/api/, data/dto/, data/repository/ProfilesAdminRepositoryImpl.kt
        ├── di/ProfilesAdminModule.kt                  # repo binding + @ProfilePhotoLoader
        ├── domain/model/, domain/repository/, domain/usecase/
        └── presentation/navigation/ProfilesNavGraph.kt, screens/ (list, detail), components/, viewmodel/, state/
```

**Structure Decision**: the access state is a core concept (three features read it), so its port and placeholder are
in `core/`; the profile feature implements it from the snapshot it already owns. Perfis is an admin sub-feature next
to Membros.

## Implementation Order

1. Domain specs (profile, admin, core) updated for the destination.
2. DTO rename + new fields + mapper + backward-compatibility test; `Scope.PROFILES`.
3. Core `MemberAccess` types + port; profile implementation (request + refresh); tests.
4. `MembersOnlyPlaceholder` + VM + dialog; wire gallery, schedule, birthdays.
5. Profile: access card, own member (memory) + correction dialog; VM tests.
6. Perfis: API, DTOs, repository, use cases, tests; list + detail VMs, tests; screens; panel card + graph.
7. Push: messages, handler, notifier, targets, AppNavHost; tests.
8. Full unit test run; debug build compiles.

## Complexity Tracking

None.
