# Quickstart: Sunday School

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.studies.*"
./gradlew :app:compileDebugKotlin
```

Covers: content codec (every block type, unknown type/icon, lossless re-encode), inline markup, error-path
mapping, standard sections, repository snapshot behaviour (404 clears), list/lesson/form ViewModels.

## Manual (device, backend with the 2026 lessons)

1. Estudos → EBD: tab 2026, header "2026 — O Drama das Escrituras", 18 lessons in order.
2. Open "Panorama de Daniel": parchment header, section icons in gold, key verses on parchment, cards, comparison,
   table, timeline, book card; tap a chip → Bible on the verses. "Aa" changes the size. Dark mode: dark brown.
3. Airplane mode: reopen list and lesson — no error. A never-opened lesson shows "Tentar novamente".
4. As Liderança: Painel → EBD. New lesson in 2026 (six sections), write a paragraph with a chip, a list and a key
   verse, preview, save as Rascunho — absent for members; publish — last in the member list.
5. Drag it to the top; members see it first after refresh. Change the 2026 theme in "Anos da EBD".
6. Put an invalid verse in a chip via a free-text book ("Xx") — the error shows under that block.
7. As Mídia: no EBD card.
