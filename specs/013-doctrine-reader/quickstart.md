# Quickstart: Doctrine Reader

## Prerequisites

- Backend with `019-doctrine-documents` deployed (or local: `docker compose up -d` in `backend/`, then point the debug
  build at it) and the three JSON files in `features/doctrine/data/`.
- Check the API: `curl -s <base>/api/doctrine/` lists 3 documents (107 / 196 / 33).

## Automated

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.studies.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.bible.*"
```

Covers: DTO decode of real-shaped JSON (both kinds, null reference, 1.2 lists), mappers (unknown kind dropped, sentence
case, short titles, item labels), search (accents, all words, numeric first, no result), repository (preload then
revalidate, 304 keeps copy, failure keeps copy, no copy → error, per-slug isolation), ViewModels (go-to validation,
font size steps and limits), Bible `openPassage` pending scroll.

## Manual (device)

1. Estudos shows Doutrina + three "Em breve" cards; only Doutrina opens.
2. Doutrina lists Breve Catecismo (107 perguntas e respostas), Catecismo Maior (196), Confissão de Fé (33 capítulos).
3. Breve Catecismo: question 1 shows 8 chips; tap "Rm 11.36" → Romanos 11 with verse 36 at the top; back → still on
   question 1.
4. Catecismo Maior: "Ir para nº…" 99 → rules on separate lines; 0 or 197 → "Digite um número de 1 a 196".
5. Search "adocao" in Catecismo Maior → question 74 among results; tap → question 74 shown.
6. Confissão: titles in sentence case ("Da ceia do Senhor"); chapter 1 paragraph 2 shows both book lists and the
   closing sentence; next/previous chapter works.
7. "Aa": raise size twice, kill app, open Confissão → larger size kept; at 32 the "+" is disabled.
8. Airplane mode after opening all three, kill and reopen → everything readable, including search.
9. Fresh install, airplane mode, open Doutrina → error with "Tentar novamente"; turn network on, retry → list loads.
