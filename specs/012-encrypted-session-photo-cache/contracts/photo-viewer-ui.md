# Contract: Member Photo Viewer (full screen)

`MemberPhotoViewer` in `features/admin/members/presentation/components/`.

## Parameters

| Parameter | Type | Null / false means |
|---|---|---|
| `initials` | `String` | — |
| `photoUrl` | `String?` | member has no photo |
| `photoRevision` | `Int` | — (part of the image request key) |
| `imageLoader` | `ImageLoader?` | preview |
| `isBusy` | `Boolean` | upload, removal or download running |
| `onPickPhoto` | `(() -> Unit)?` | no `manage` on `members` |
| `onRemovePhoto` | `(() -> Unit)?` | no `owner` on `members` |
| `onDownload` | `(() -> Unit)?` | no `manage` on `members` |
| `onDismiss` | `() -> Unit` | — |

## Layout

```text
┌──────────────────────────────────────┐
│ [✎ Trocar foto]                  [✕] │  top, safe-drawing padding
│                                      │
│            ┌──────────────┐          │
│            │    photo     │          │  square, full width, centred
│            └──────────────┘          │
│                                      │
│        [🗑 Apagar]  [⤓ Baixar]        │  bottom centre, above system bars
└──────────────────────────────────────┘
```

| Element | Visible when | Label / icon | content description |
|---|---|---|---|
| Trocar foto | `onPickPhoto != null` | pencil + "Trocar foto" | — |
| Fechar | always | X | "Fechar" |
| Apagar | `onRemovePhoto != null && photoUrl != null` | trash + "Apagar" | — |
| Baixar | `onDownload != null && photoUrl != null` | download + "Baixar" | — |

- One bottom button alone is centred; none → no bottom row.
- `isBusy` disables every button except "Fechar" and shows a centred progress indicator.
- "Apagar" → existing confirmation dialog (005 FR-025). "Baixar" → permission flow (API ≤ 28) then download.

## Messages (via `MembersEvent.ShowMessage`)

| Case | Text |
|---|---|
| saved | "Foto salva em Imagens/IPB Castelo Branco" |
| permission denied | "Para salvar a foto, permita o acesso ao armazenamento." |
| permission denied permanently | "Permita o acesso ao armazenamento nas configurações do aparelho para salvar a foto." |
| write failed / storage full | "Não foi possível salvar a foto." |
| download refused / network | `AppError.toUserMessage()` (constitution) |
