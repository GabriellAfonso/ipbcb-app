# Data Model: Trim a Video Before Uploading It to the Gallery

Nothing here is persisted except a cut file and the existing queue item it becomes.

## PickedMedia (extended)

| Field | Meaning |
|-------|---------|
| `source`, `mimeType`, `sizeBytes` | As today |
| `trimmable: TrimmableVideo?` | Set when the video may open the editor |

## TrimmableVideo

`displayName`, `durationMs` (> 0), `dateTaken` (`yyyy-MM-dd` or null), `videoMime`.

Rule (`VideoUploadCheck.canTrim`): MIME `video/mp4` or `video/quicktime`, video H.264 or HEVC, audio AAC or none, known
positive duration.

## UploadBatch

`albumId` and an ordered list of `BatchItem(media, decision)`.

`VideoDecision`: `Undecided`, `Whole`, `Cut(entry, sizeBytes)`. Only a video with `trimmable` starts `Undecided`. Photos
and non-trimmable videos start `Whole`.

| Derived | Rule |
|---------|------|
| `editable` | Items with `trimmable`, the "Vídeo N de M" count |
| `current` | First `Undecided` |
| `isComplete` | No `Undecided` left |
| `entries` | `UploadEntry` per item, picked order |
| `mobileDataVideos` | Cuts with real size, whole videos of at most 100 MB |

## UploadEntry

`Picked(source)` or `Cut(fileName, displayName, dateTaken)`.

## VideoTrimUiState

| Field | Meaning |
|-------|---------|
| `position`, `total`, `undecided` | Batch progress |
| `source`, `displayName`, `durationMs`, `sizeBytes` | Current video |
| `startMs`, `endMs` | Selection, at least 1000 ms apart |
| `previewMs` | Where the player should be; the last handle moved |
| `isCutting`, `progress`, `error` | Cut state |
| `askingDiscard` | Back confirmation open |

Derived: `selectedMs`, `isWhole`, `estimatedBytes`, `isOverLimit`, `canUseWhole`, `canCut`, `showSendAllWhole`,
`tooBigMessage`.

## Transitions

```text
Undecided --Usar inteiro / Enviar todos--> Whole
Undecided --Cortar e continuar--> cutting --done, fits--> Cut
                                  cutting --too big / failed--> Undecided, error shown
                                  cutting --Cancelar--> Undecided
Any --Descartar--> batch gone, cut files deleted
```

## Files

`gallery/uploads/trim-{uuid}.mp4` until the batch is enqueued (it becomes the item's `fileName`) or discarded. A cut
file no queue item references at app start is an orphan and is deleted.
