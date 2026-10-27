# Quickstart: Gallery Videos

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.settings.*"
./gradlew :app:assembleDebug
```

## Manual (needs the backend of `backend/specs/023-gallery-videos` deployed)

1. Upload an MP4 (< 100 MB) in an album as a `manage` user on WiFi: it shows at once with ▶ and duration.
2. Another device with the switch off: the video does not show; the card "Há 1 vídeo na galeria" is on the root.
3. Tap "Baixar" on WiFi: the video downloads and appears; open it: muted autoplay; unmute, swipe to another video:
   sound on; leave the gallery and come back: muted again.
4. Turn airplane mode on mid-download, then back: the download resumes (server log shows `206`).
5. Rotate on a video: full screen. Repeat toggle in the controls.
6. Pick a 150 MB file, an MKV and a video with AC3 audio: three entries in "Não enviadas" with their reasons.
7. On mobile data, pick photos + a video: the confirmation shows; "Esperar WiFi" keeps the video queued until WiFi
   while the photos go.
8. Configurações › Galeria: sizes per type; "Apagar galeria" with only Vídeos: videos gone, switch off, photos stay.
   Only Fotos: photos gone, downloaded videos stay. "Baixar galeria" with Vídeos: switch on.
