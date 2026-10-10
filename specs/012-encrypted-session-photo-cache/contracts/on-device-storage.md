# Contract: On-Device Storage

What this feature writes to the device, byte for byte. Anything not listed here must not be written.

## Sealed blob (shared by session and photo cache)

```text
offset  size  field
0       1     version = 0x01
1       12    IV (random per write)
13      n+16  AES-256-GCM ciphertext ‖ tag (128-bit)
```

- Key: non-exportable AES-256 in `AndroidKeyStore`; aliases `ipbcb_session_v1`, `ipbcb_member_photos_v1`.
- Associated data: UTF-8 label — `"auth_prefs"` for the session; `"member_photo:" + cacheKey` for a photo.
- Any other version byte, a short blob, a tag mismatch or a key error = undecryptable.

## Session file

| Path | Content |
|---|---|
| `files/datastore/auth_prefs.enc.preferences_pb` | sealed blob of the DataStore Preferences protobuf |
| `files/datastore/auth_prefs.preferences_pb` | **must not exist** after the first successful read of the new store |

Excluded from cloud backup and device transfer (both rule files list the new and the old path).

## Member photo cache

| Path | Content |
|---|---|
| `no_backup/member_photos/<sha256 hex of URL path>` | sealed blob of the payload below |

Payload before sealing:

```text
u16  etag length (0 = none)   ‖ etag UTF-8
u16  mime length              ‖ mime UTF-8
...  image bytes (to end)
```

- No member id, name or URL appears in any file name or in plain text.
- Total size of the directory ≤ 50 MB after every write.
- `wipe()` deletes the directory and the key alias; it is idempotent.

## Downloaded photo (outside app control)

| API | Location | Mechanism |
|---|---|---|
| 29+ | `Pictures/IPB Castelo Branco/<name> <yyyy-MM-dd>.<ext>` | MediaStore insert, `IS_PENDING` 1 → 0; delete row on failure |
| 24–28 | same, under `getExternalStoragePublicDirectory(DIRECTORY_PICTURES)` | write `.tmp`, rename, media scan; delete temp on failure; needs `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`) |

Plain image bytes, never encrypted, never tracked.

## Manifest

```xml
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
```
