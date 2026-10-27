# Quickstart: validating Bible videos and progress

## Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.bible.*"
```

Covers sections (no repetition, order), progress merge, queue collapse and removal of sent items, sync drain,
logout flush/clear, 409 handling.

## Manual (needs the backend feature deployed)

1. Signed out, open the Bible on a chapter with a video: ▶ shows, sheet lists it, tap opens YouTube. A chapter
   with no video in it nor in its book: no ▶.
2. Liderança account: ▶ always; add a link → places screen opens; add Gn 2–3 + Êxodo (livro inteiro), save; check
   the sheet in each place. Add the same link again → "Esse vídeo já está cadastrado".
3. Reorder a section; reopen; order kept; other places unchanged. Remove from chapter; delete video.
4. Member account: mark Rm 5 offline (airplane mode) → "Lido ✓", index check and "1/16". Back online → reinstall /
   another device shows it.
5. Long-press Gênesis → confirm → "50/50 ✓"; long-press again → "Desmarcar".
6. Logout → login with another account: no previous marks.
