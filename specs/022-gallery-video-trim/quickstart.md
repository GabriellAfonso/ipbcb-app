# Quickstart: Validating the Video Trim

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
```

## On real phones (2 or 3 devices)

Sign in with a `manage` account and open an album. For each case, confirm the result in the album and on the server.

| Case | Expected |
|------|----------|
| H.264 MP4, both handles moved | Exact cut, audio in sync, right duration and poster |
| HEVC MP4 | Same, output still HEVC or an accepted fallback |
| MOV | Output is MP4, name ends in `.mp4` |
| Portrait video | Rotation kept |
| Only the end handle moved | Quality unchanged, fast |
| MP4 over 100 MB | "Usar inteiro" disabled, trim to a part that fits |
| Mixed batch | Picked order kept, "Enviar todos sem cortar" |
| Back, then "Descartar" | Nothing queued, no `trim-*` file left |
| Off WiFi | The question shows the final sizes |
| Kill the app during a cut, reopen | `trim-*` file removed |

To see which path ran, filter logcat for `VideoTrim`: it logs the optimization result of each export.

Also record the release APK size before and after the `media3-transformer` dependency.
