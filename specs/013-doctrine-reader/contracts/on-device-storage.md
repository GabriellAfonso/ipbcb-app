# Contract: On-device Storage

| What | Where | Format |
|------|-------|--------|
| Document list | `filesDir/snapshots/doctrine_list.json` + ETag | `DoctrineListDto` JSON |
| Each document | `filesDir/snapshots/doctrine_<slug>.json` + ETag | `DoctrineDocumentDto` JSON |
| Text size | Settings DataStore, `doctrine_font_size` | Float |

- Written through `SnapshotCacheFactory` / `JsonSnapshotStorage` (`core` 4.3); a corrupt file is deleted on load and
  counts as absent.
- Public content: not encrypted, not removed on sign-in/sign-out (FR-026).
- Size: ≈ 0.8 MB for the three documents.
