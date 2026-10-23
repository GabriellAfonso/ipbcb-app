# Data Model: Access Requests

Wire shapes are in `backend/specs/022-access-requests/contracts/access-api.md`; this file lists the app types.

## Core (`core/domain/access/`)

```kotlin
enum class AccessStatus { GRANTED, PENDING, NONE, UNKNOWN }          // wire granted|pending|none; null → UNKNOWN

data class MemberAccess(
    val hasAccess: Boolean,
    val status: AccessStatus,
    val requestsBlocked: Boolean,      // null on the wire → false, but UNKNOWN status already hides buttons
    val correctionPending: Boolean,
) { companion object { val NONE = MemberAccess(false, AccessStatus.UNKNOWN, false, false) } }

enum class AccessRequestType(val wire: String) { ACCESS("access"), DATA_CORRECTION("data_correction") }

interface MemberAccessRepository {
    val memberAccess: Flow<MemberAccess>                               // from the profile snapshot; NONE signed out
    suspend fun request(type: AccessRequestType, message: String?): Result<Unit>   // refreshes the profile after
}

// Scope.PROFILES("profiles") added to Scope.
```

Prompt shown by `MembersOnlyPlaceholder` (derived, pure function `accessPromptFor(MemberAccess)`):

| Condition | Prompt |
|---|---|
| `requestsBlocked` | `Blocked` — blocked text, no button |
| `status == PENDING` | `Pending` — pending text, no button |
| `status == NONE` | `CanRequest` — "Pedir acesso" |
| `GRANTED` / `UNKNOWN` | `None` — only the screen's own message |

## Profile (`features/profile/`)

- `MeProfileDto`: `isMember: Boolean = false` (`is_member`), `memberAccess: Boolean? = null`, `accessStatus: String?
  = null`, `accessRequestsBlocked: Boolean? = null`, `dataCorrectionPending: Boolean? = null`.
- `MeProfile`: `isMember` → `memberAccess: Boolean`, + `memberAccessInfo: MemberAccess` (mapped).
- `OwnMember` (memory only): `role: String?`, `ministries: List<String>`, `birthDay: Int?`, `birthMonth: Int?`,
  `baptismDate: LocalDate?`, `gender: String?`.
- `OwnMemberRepository.get(): Result<OwnMember?>` (`null` on 404 `MEMBER_NOT_LINKED`).

## Perfis (`features/admin/profiles/`)

```kotlin
enum class RequestStatus { PENDING, APPROVED, DENIED }
data class AccessRequest(
    val id: Long, val type: AccessRequestType, val message: String?, val status: RequestStatus,
    val createdAt: OffsetDateTime?, val decidedBy: String?, val decidedAt: OffsetDateTime?, val denialReason: String?,
)
data class ManagedAccount(
    val id: String, val name: String, val username: String?, val photoUrl: String?,
    val memberAccess: Boolean,          // stored flag
    val accessViaRole: Boolean,
    val accessGrantedBy: String?,       // grant record: leader's name; null when unknown
    val accessGrantedAt: OffsetDateTime?,
    val requestsBlocked: Boolean,
    val member: LinkedMember?,          // LinkedMember(id: Int, name: String)
    val dateJoined: LocalDate?,
    val pendingRequests: List<AccessRequest>,
    val requests: List<AccessRequest>,  // detail only; empty in lists
)
enum class ProfilesTab(val wire: String) { PENDING("pending"), ALL("all") }
```

`ProfilesAdminRepository`:
- `observe(tab): Flow<List<ManagedAccount>?>`, `refresh(tab): Result<Unit>`
- `detail(id): Result<ManagedAccount>`
- `approve(requestId)`, `deny(requestId, reason?)`, `grantAccess(userId)`, `revokeAccess(userId)`, `setBlocked(userId, Boolean)`,
  `link(userId, memberId: Int?)` — each `Result<ManagedAccount>` and updates the cached lists.

No e-mail field exists in any of these types.

## Push (`core/domain/push/PushMessage.kt`)

`AccessRequestCreated(type)`, `AccessRequestApproved(type)`, `AccessRequestDenied(type, reason: String?)`;
unknown `request_type` → message ignored.
