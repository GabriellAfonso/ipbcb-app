# UI Contract: Gallery Videos

Server contract: `backend/specs/023-gallery-videos/contracts/video-upload-api.md` (not repeated).

## Video upload request (app → server)

`POST api/photos/` multipart: `album_id`, `client_upload_id`, `duration_ms`, `date_taken` (only when known),
`video` (file, `video/mp4` or `video/quicktime`, picked name), `poster` (JPEG). Answers classified as photo uploads.

## Texts

| Where | Text |
|-------|------|
| Tile | play icon + `m:ss` / `h:mm:ss` |
| Organizar / move picker | dimmed tile + "Não está no aparelho" (icon description) |
| Card | "Há {n} vídeo(s) na galeria ({size})" / "Eles não são baixados automaticamente." / [Não, obrigado] [Baixar] |
| Card, Não obrigado | snackbar "Você pode baixar os vídeos depois em Configurações." |
| Card, Baixar off WiFi | snackbar "Os vídeos vão baixar quando você estiver no WiFi" |
| Low space banner | "Pouco espaço no aparelho. Os vídeos pararam de baixar." |
| Player failure | "Não foi possível reproduzir este vídeo neste aparelho" |
| Upload entry | "Adicionar fotos e vídeos" |
| Metered dialog | "Você está nos dados móveis. Enviar {n} vídeo(s) ({size}) agora?" [Esperar WiFi] [Enviar] |
| Filter chips | "Tudo" / "Fotos" / "Vídeos" |
| Settings row | "Galeria" / "Fotos {x} · Vídeos {y}" |
| Switch | "Baixar vídeos automaticamente" / off: "Somente no WiFi · {n} vídeos ({size})" / on, downloading: "{k} de {n} baixados" / on, done: "Somente no WiFi · {n} de {n} baixados" |
| Apagar dialog | "Apagar galeria" / "Escolha o que apagar deste aparelho." / [x] "Fotos {size}" [x] "Vídeos {size}" / note "Apagar os vídeos desliga o download automático de vídeos." / [Cancelar] [Apagar] |
| Baixar dialog | "Baixar galeria" / [ ] "Fotos" ("Já baixadas" when complete) / [ ] "Vídeos" ("Já baixados") / note "Vídeos novos também serão baixados. Somente no WiFi." / [ ] "Usar dados móveis para as fotos" / [Cancelar] [Baixar] |
| After delete | "Galeria apagada" / "Fotos apagadas" / "Vídeos apagados" |
| Refusals | see research R-13 |
