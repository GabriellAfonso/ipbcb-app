# Galeria — Spec

A Galeria é o acervo de fotos dos eventos da igreja, organizado numa **árvore de álbuns** com capas e ordem
definidas pela igreja. É **conteúdo restrito a membros** (`IsMemberUser` no servidor). Para quem não tem nível no
escopo `gallery` ela é **somente leitura**; quem tem `manage` ou `owner` monta a galeria **nas mesmas telas** (seção
8). O app mantém uma **cópia local** que segue o servidor: novos, alterados e apagados — álbuns e fotos — chegam ao
aparelho pelo feed de mudanças, e o que o próprio usuário grava é aplicado localmente na hora.

Fonte da verdade no backend: `backend/specs/gallery/spec.md`, `013-gallery-write-api/contracts/gallery-api.md`,
`014-gallery-trash-sync/contracts/gallery-trash-api.md`, `015-gallery-member-tags/contracts/gallery-tags-api.md`,
`016-photo-upload-idempotency/contracts/photo-upload-api.md`.

Ainda não existem no app (features seguintes): lixeira e restauração, e marcação de membros ("Minhas fotos",
filtros). O campo `members` das fotos já é guardado, mas não é exibido. O card "Galeria" do painel de gestão abre a
galeria (seção 8.1).

---

## 1. Telas

| Tela    | Rota                          | Origem                      |
|---------|-------------------------------|-----------------------------|
| Galeria | `GalleryMain`                 | `CoreScreen → botão Galeria`; painel de gestão → card "Galeria" |
| Álbum   | `Album/{albumId}`             | Álbum na grid (raiz ou sub-álbum) |
| Foto    | `Photo/{albumId}/{photoId}`   | Foto na grid do álbum       |

As três vivem em `galleryGraph` (`GalleryNavGraph.kt`), com um `GalleryViewModel` escopado ao grafo via
`hiltViewModel(graphEntry)`. Cada tela coleta um `StateFlow` de UiState (`GalleryRootUiState`, `AlbumUiState`,
`PhotoViewerUiState`) e passa dados puros ao composable de conteúdo. O estado de cada álbum e de cada visualizador é
memoizado por chave no ViewModel.

`isLoggedIn` é **parâmetro de `galleryGraph`** (`StateFlow<Boolean>`), vindo do `CoreViewModel` que o `AppNavHost`
resolve. Não pode ser buscado por `getBackStackEntry(AppRoutes.CORE)`: o `CoreViewModel` vive no escopo da Activity,
e aquele lookup devolveria uma segunda instância, sem `initialize()`, presa em "deslogado".

Toda lista é ordenada por `position`, depois `id` — nunca por nome de arquivo.

### 1.1 Galeria (raiz)

Grid de 2 colunas com os álbuns raiz (`parent_id` nulo): capa quadrada (preta quando não há) e nome. Acima da grid,
um banner não-bloqueante (seção 5). Abrir a galeria dispara um sync.

### 1.2 Álbum

Uma rolagem: sub-álbuns primeiro (2 por linha, com capa), depois as fotos (3 por linha). Título = nome do álbum;
subtítulo = nome do álbum pai (raiz não tem). Data do evento (`dd/MM/yyyy`) e descrição quando existem. Sem fotos e
sem sub-álbuns: "Nenhuma foto neste álbum.". Cada álbum aberto é uma entrada do back stack — voltar sobe um nível.

### 1.3 Foto

Pager horizontal pelas fotos **do álbum de onde foi aberto** (o `albumId` da rota), na ordem da grid, abrindo na foto
tocada. Top bar = nome da foto sem extensão. Zoom por pinça, duplo toque volta ao normal, "Baixar" (salva em
`Pictures/ipb_castelobranco` com o nome da foto e o MIME pela extensão) e "Compartilhar". Foto ainda sem original no
aparelho aparece pelo preview (ou cinza), com "Baixar" e "Compartilhar" desabilitados.

### 1.4 Item removido com a tela aberta

| Caso | Efeito | Aviso |
|------|--------|-------|
| Foto na tela apagada | pager vai para a próxima (a anterior, se era a última) | "Esta foto foi removida" |
| Foto na tela movida para outro álbum | idem | "Esta foto foi movida para outro álbum" |
| Última foto do álbum apagada | o visualizador fecha | "Esta foto foi removida" |
| Álbum aberto apagado | a tela sobe um nível; se o de baixo também saiu (subárvore apagada), ele faz o mesmo | "Este álbum foi removido" |

Quando quem removeu foi o próprio usuário (seção 8), o efeito é o mesmo e o aviso é o da ação: "Álbum enviado para a
lixeira", "Foto enviada para a lixeira", "Foto movida para '{álbum}'".

O aviso é um `StateFlow<GalleryMessage?>` consumível no ViewModel, mostrado (Toast) pela tela que está no topo e
consumido uma vez — não um `SharedFlow`, porque durante o desempilhamento pode não haver tela coletando.

---

## 2. Dados

### 2.1 `GET api/gallery/changes/?since={cursor}` — `IsMemberUser`

Única leitura de dados da galeria. Sem `since`: todos os álbuns e fotos vivos. Com `since`: o que mudou desde pouco
antes do cursor (pode repetir itens).

```json
{
  "albums": [{ "id": 7, "name": "Retiro 2026", "parent_id": 2, "description": "", "event_date": "2026-03-14",
               "cover_url": "https://…/media/gallery/covers/9/3f2a.jpg", "cover_source_album_id": 9, "position": 3 }],
  "photos": [{ "id": 301, "name": "IMG_0042.jpg", "description": "", "album_id": 7, "album_name": "Retiro 2026",
               "image_url": "https://…/media/gallery/7/9b1e.jpg", "thumbnail_url": "https://…/thumbs/7/c4d0.jpg",
               "date_taken": "2026-03-14", "uploaded_at": "2026-03-15T10:00:00Z", "position": 0,
               "members": [{ "id": 40, "name": "João Lima" }] }],
  "deleted_album_ids": [12],
  "deleted_photo_ids": [302],
  "cursor": "v1.AAYh3k9x2QA",
  "full_sync_required": false
}
```

- O **cursor é opaco**: guardado e devolvido, nunca interpretado.
- Álbum apagado lista seus sub-álbuns e todas as fotos deles. Item restaurado volta como alterado.
- `full_sync_required: true` (cursor velho, ilegível ou futuro) vem com listas vazias.
- `cover_url` e `cover_source_album_id` nulos = nenhuma capa abaixo: o app mostra preto.
- `thumbnail_url` é nulo até o servidor gerar a miniatura.
- A extensão do original salvo vem de `image_url` (`png` / `webp`, senão `jpg`).

| Código | Quando                        |
|--------|-------------------------------|
| `401`  | Sem token / token inválido    |
| `403`  | Autenticado, mas não é membro |

As leituras `api/photos/`, `api/albums/` e `api/albums/{id}/photos/` não são usadas pelo app. As escritas estão na
seção 8.2.

### 2.2 Mídia (`/ipbcb/media/gallery/...`)

Originais, capas e previews exigem o JWT do membro. O arquivo de um item na lixeira responde `404` a membros. O arquivo
de uma foto nunca muda de lugar e seu `image_url` nunca muda; uma capa trocada ganha URL nova.

### 2.3 Índice local

Um snapshot JSON (`SnapshotCacheFactory`, chave `gallery_index`, `filesDir/snapshots/gallery_index.json`) com
`albums`, `photos` (com `members`) e `cursor`. O cursor é gravado **junto** com os dados, na mesma escrita: um crash
nunca deixa o cursor à frente do que descreve. Um arquivo corrompido é lido como "sem índice", e o próximo sync faz
leitura completa. Árvore, ordem e buscas são calculadas do índice em memória (`GalleryIndex`, `GalleryTree`).

Um álbum cujo pai não está no índice, ou uma foto cujo álbum não está, não aparece.

### 2.4 Disco

```
filesDir/gallery/photos/{photoId}.{ext}       originais, planos por id
filesDir/gallery/covers/{sha1(cover_url)}.jpg capas, por hash da URL
filesDir/gallery/uploads/{uploadId}[-src].{ext} fila de envio (cópia do que foi escolhido, depois o preparado)
cacheDir/gallery-*.part                       temporários da escrita atômica
cacheDir/gallery-*.{ext}                      capa sendo enviada (apagada depois do pedido)
cacheDir/gallery_thumbs/                      cache de previews (Coil, até 100 MB)
```

Mover uma foto de álbum nunca toca no arquivo. Capa nova = URL nova = arquivo novo; dois álbuns com a mesma capa
herdada compartilham um arquivo. Toda escrita vai para um temporário em `cacheDir` e só é movida para o nome final
quando o corpo chegou inteiro — um arquivo truncado com o nome final contaria como baixado para sempre.

---

## 3. Sincronização

`GallerySyncer` (`@Singleton`) aplica o feed ao índice.

1. Lê o feed com o cursor guardado (sem cursor: leitura completa).
2. `full_sync_required`: lê de novo sem `since` e **substitui** o índice pelo resultado.
3. Senão: upsert de álbuns e fotos por id, remoção dos ids apagados (idempotente; se um id vier alterado e apagado,
   apagar vence).
4. Grava índice + novo cursor.
5. **Reconcilia o disco com o índice**: apaga todo original cuja foto não está no índice e toda capa cuja URL nenhum
   álbum usa; baixa as capas que faltam (qualquer rede; falha é pulada e a capa fica preta até o próximo sync).

O passo 5 é a única regra de limpeza: cobre fotos e álbuns apagados, `full_sync_required`, capas trocadas ou
removidas e os órfãos da migração.

- **Um sync por vez**: pedido enquanto outro roda é pulado (`Skipped`) — o que está rodando atende. A exceção é o
  sync depois de uma escrita (`syncAfterWrite`, seção 8.3): se já há um rodando, ele roda **mais uma vez** antes de
  soltar o lock, porque pode ter lido o feed antes da escrita.
- **Sessão**: sem sessão nada é pedido; a sessão é conferida de novo antes de gravar. Toda gravação acontece sob o
  lock, então um sync que termina depois do logout não grava nada.
- **Falha** (`401`, `403`, rede, servidor): a cópia local fica intacta; o próximo gatilho tenta de novo.

### 3.1 Gatilhos

| Gatilho | Onde |
|---------|------|
| Abertura do app e volta do background (`ON_START` da Activity), com sessão | `AppNavHost` → `CoreViewModel.onAppForeground()` |
| Login bem-sucedido | `CoreViewModel` (`AuthEventBus.LoginSuccess`) |
| Abertura da galeria | `GalleryViewModel` (init) |
| A cada 6 h, qualquer rede, com sessão | `GallerySyncWorker` (periódico único `gallery_sync_periodic`, `KEEP`) |
| "Tentar novamente" (sem índice) | tela da galeria |

O periódico é agendado no login e em cada `onAppForeground` (idempotente). Voltar à tela da galeria dentro do app não
dispara sync — a volta do background já cobre.

Depois de cada sync bem-sucedido, se há fotos sem original no aparelho, o download é enfileirado **só no WiFi**
(`KEEP`: não mexe num download já na fila ou rodando; um trabalho já terminado é substituído).

### 3.2 Migração do layout antigo

Na primeira execução desta versão (`gallery_layout_version < 2` em `GalleryPreferences`), antes do primeiro sync: cada
`gallery/{albumId}/{photoId}.{ext}` é movido para `gallery/photos/`, os `.json` e as pastas de álbum são apagados, e o
marcador vira `2`. Nada é baixado de novo. Sem índice, o primeiro sync é leitura completa, e o passo 5 apaga os
originais de fotos que o servidor não tem mais. Cada passo é idempotente: uma migração interrompida termina na próxima.

Limitação aceita: quem atualiza o app sem conexão vê "sem conexão" até o primeiro sync, embora as fotos estejam no
aparelho.

---

## 4. Download dos originais

Roda em `GalleryDownloadWorker` (WorkManager, trabalho único `gallery_auto_download`), nunca no `viewModelScope`. A
lógica fica em `GalleryDownloadJob` (sem tipos do WorkManager, testável).

| Gatilho                         | Política  | Rede        |
|---------------------------------|-----------|-------------|
| Sync com originais faltando     | `KEEP`    | `UNMETERED` |
| Login bem-sucedido              | `REPLACE` | `UNMETERED` |
| Botão "Tentar novamente"        | `REPLACE` | `UNMETERED` |
| Botão "Usar dados móveis"       | `REPLACE` | `CONNECTED` |
| Logout                          | cancela   | —           |

A lista de fotos é o **índice, em ordem de árvore** (pré-ordem: fotos do álbum, depois cada sub-álbum). Sem índice
(login no WiFi antes de qualquer sync responder), o trabalho sincroniza primeiro. O worker atualiza a lista de
arquivos locais a cada álbum concluído e ao terminar, então a grid troca preview por original durante o download.

### 4.1 Uma foto só conta quando está no disco

O laço por foto vive em `GalleryPhotoDownloader`. `downloaded` é sempre o número de fotos da lista que estão no disco
ao fim da rodada.

| Resposta da foto                        | Efeito                                              |
|-----------------------------------------|-----------------------------------------------------|
| já no disco                             | conta, sem requisição                               |
| `2xx` com corpo, salvo                  | conta                                               |
| `2xx` sem corpo, `404`, outro erro      | não conta, segue; tentada de novo na próxima rodada |
| `IOException` (inclusive corpo cortado) | não conta, segue; conta como falha de rede          |
| `401` (após o refresh falhar)           | **para a rodada** — sessão expirada                 |
| `403`                                   | **para a rodada** — "sem acesso"                    |
| `429`                                   | **para a rodada**                                   |

`401`, `403` e `429` descrevem quem pede, não a foto: toda requisição seguinte teria a mesma resposta. Um `404` de uma
foto já na lixeira no servidor só se resolve no próximo sync, que a tira do índice e do disco.

### 4.2 Como a rodada termina

| Resultado                                        | `Result`                                              |
|--------------------------------------------------|-------------------------------------------------------|
| sync inicial falhou (`401`/`403`)                | `failure` com `errorCode`                             |
| sync inicial falhou (outros)                     | `retry` até 3 tentativas, depois `failure`            |
| completa, sem falha de rede                      | `success` (falhas `404` ficam para o próximo gatilho) |
| completa, com falha de rede                      | `retry` até 3 tentativas, depois `success`            |
| parada por `429`                                 | `retry`, **sem limite de tentativas**                 |
| parada por `403`                                 | `failure(403)`, sem nova tentativa                    |
| parada por `401`                                 | `failure(401)`, sem nova tentativa                    |

Em `403` as fotos já baixadas **ficam** no aparelho e continuam visíveis — uma recusa do servidor nunca apaga a
galeria local. `429` nunca vira erro na tela. Backoff `EXPONENTIAL` a partir de 60 s; `Retry-After` não é honrado.

Um `401` só chega aqui depois de o `TokenAuthenticator` tentar renovar o token. Se o refresh foi recusado, os tokens
são apagados, `isLoggedInFlow` vira `false` e a tela cai em "Faça login para acessar a galeria.". Esse caminho não é
logout completo: o trabalho não é cancelado e a galeria local não é apagada.

---

## 5. Previews

Uma foto **sem original no aparelho** (nova esperando WiFi, primeiro download em andamento) aparece pelo seu
`thumbnail_url` (lado maior 1000 px), carregado pela rede — em qualquer rede, só as células visíveis — pelo loader
`@GalleryThumbnailLoader` (Coil sobre o `@Client` autenticado, cache em disco de 100 MB). Com o original no disco,
usa o original. Sem original e sem preview (nulo, offline sem cache, erro): cinza.

Esta é a **única** exceção à regra "nenhuma tela carrega URL de mídia direto": originais e capas só entram no aparelho
pelo sync e pelo download.

---

## 6. Estados da tela da galeria

`GalleryDownloadState` (`isDownloading`, `isPending`, `isResuming`, `downloaded`, `total`, `error`, `errorCode`,
`isResolved`) é mapeado do `WorkInfo` do download:

| `WorkInfo.State`                   | Estado                                    |
|------------------------------------|-------------------------------------------|
| `RUNNING`                          | `isDownloading` + progresso               |
| `ENQUEUED`, `runAttemptCount == 0` | `isPending`                               |
| `ENQUEUED`, `runAttemptCount > 0`  | `isResuming` (backoff após `429` ou rede) |
| `FAILED`                           | `error` + `errorCode`                     |
| demais / ausente                   | resolvido, sem erro                       |

Ordem de decisão:

1. **Deslogado** → "Faça login para acessar a galeria." + botão de login.
2. **Banner** (independe da grid): feed respondeu `403` com a galeria no aparelho → "Disponível apenas para membros."
   (sem botão) / download `403` com álbuns → mesmo aviso / baixando (progresso) / pausado ("Download pausado. Continua
   automaticamente em instantes.") / aguardando WiFi (com "Usar dados móveis") / aguardando WiFi já no WiFi.
3. **Com álbuns** → grid.
4. **Sem índice e o primeiro sync ainda não respondeu** → `CircularProgressIndicator` (nunca o estado vazio).
5. **Sem índice e o sync falhou**:
   - `401` → "Sua sessão expirou. Entre novamente para acessar a galeria." + "Conectar à sua conta"
   - `403` → "Disponível apenas para membros.", **sem** botão de login
   - rede → "Não foi possível carregar a galeria. Verifique sua conexão." + "Tentar novamente" (novo sync)
   - outro → texto genérico da categoria + "Tentar novamente"
6. **Download falhou sem álbuns** → mesmas regras de `401`/`403`, "Tentar novamente" re-enfileira o download.
7. **Índice sem álbuns** → "Nenhum álbum disponível."

O botão de login aparece **somente** em `401`. Falha de rede não é problema de sessão, e `403` não se resolve logando
de novo.

---

## 7. Logout

Cancela a fila de envio (antes de tudo: nada pode ser enviado como o próximo usuário), cancela o download e o sync
periódico, cancela um sync em andamento e apaga a fila, índice, cursor, originais, capas e o cache de previews
(`GalleryAutoDownloadUseCase.clearOnLogout()`). O acervo é restrito a membros e não sobrevive ao fim da
sessão. "Resetar galeria" nas configurações apaga índice e arquivos; o próximo sync reconstrói tudo.

---

## 8. Gestão

Quem tem nível no escopo `gallery` monta a galeria nas mesmas telas que os membros usam. O app segue **os níveis,
nunca os papéis** (hoje Admin, Liderança e Mídia têm `owner`). A leitura continua exigindo `is_member`: um gestor que
não é membro vê "Disponível apenas para membros." (regra operacional: todo gestor é marcado como membro).

### 8.1 Controles por nível

`GalleryPermissions(canManage, canDelete)` vem de `ObserveAccessUseCase` (`Access.allows(GALLERY, MANAGE / OWNER)`) e
está em todo UiState. Sem nível, nenhum controle é composto — as telas são as de leitura.

| Tela | Controle | Nível |
|------|----------|-------|
| Raiz | FAB "Novo álbum"; menu "Organizar" (com 2+ álbuns) | `manage` |
| Álbum | FAB "+" → "Adicionar fotos" / "Novo álbum" (sub-álbum) | `manage` |
| Álbum | menu: "Editar álbum", "Mover álbum", "Trocar capa", "Organizar" (com 2+ sub-álbuns ou 2+ fotos) | `manage` |
| Álbum | menu: "Remover capa" (só se a capa é própria: `cover_source_album_id == id`), "Apagar álbum" | `owner` |
| Álbum | toque longo numa foto → seleção; barra: "Mover" (`manage`), "Apagar" (`owner`) | `manage` |
| Álbum | faixa de envio ("Enviando X de N", "Preparando fotos…") e lista "Não enviadas" | — (itens da fila) |
| Foto | menu: "Editar foto", "Mover", "Usar como capa" | `manage` |
| Foto | menu: "Apagar" | `owner` |
| Painel de gestão | card "Galeria" → `AppRoutes.GALLERY_GRAPH` | `manage` (filtro do `PanelCard`) |

O nível muda ao vivo (a tela não precisa ser reaberta). Um `403` numa escrita não precisa de refresh pela galeria: o
`PermissionDeniedInterceptor` avisa o `AuthEventBus` e o `CoreViewModel` relê o perfil; os controles somem quando o
acesso novo chega. A galeria só mostra a mensagem do servidor.

### 8.2 Escritas (`@AuthedRetrofit`, `GalleryApi`)

| Ação | Pedido | Aplicado no índice |
|------|--------|--------------------|
| Novo álbum | `POST api/albums/` (`name`, `parent_id` se sub-álbum, `description`/`event_date` se preenchidos) | upsert do álbum devolvido |
| Editar / mover álbum | `PATCH api/albums/{id}/` só com os campos alterados; `parent_id: null` = raiz, `event_date: null` limpa | upsert |
| Organizar álbuns | `PUT api/albums/order/` `{parent_id, ids}` (`parent_id` sempre presente) | `position` = índice na lista |
| Trocar capa | `PUT api/albums/{id}/cover/` multipart `image` | upsert do álbum devolvido |
| Remover capa | `DELETE api/albums/{id}/cover/` | nada (a capa resolvida vem do sync) |
| Apagar álbum | `DELETE api/albums/{id}/` | remove o álbum, a subárvore e as fotos |
| Enviar foto | `POST api/photos/` multipart `album_id`, `client_upload_id`, um `image` | upsert da foto; o arquivo enviado vira o original |
| Editar / mover foto | `PATCH api/photos/{id}/` só com os campos alterados (`date_taken: null` limpa) | upsert |
| Organizar fotos | `PUT api/albums/{id}/photos/order/` `{ids}` | `position` = índice na lista |
| Apagar foto | `DELETE api/photos/{id}/` | remove a foto |

Os corpos JSON são `JsonObject` montados à mão: o `Json` do projeto tem `explicitNulls = false`, e um data class
perderia o `null` de "mover para a raiz" ou "limpar a data".

Erros (lidos só por `ResponseExt`; os campos extras do corpo ficam em `AppError.Server.extras`):

| Resposta | Efeito |
|----------|--------|
| sem rede | "Sem conexão"; nada muda no aparelho |
| `400` nome repetido entre irmãos | o formulário fica aberto com a mensagem do servidor sob o nome |
| `400` com `chain` (ciclo) | mensagem do servidor + sync |
| `400` com `missing`/`unexpected`/`repeated` (ordem) | sync, "Organizar" continua aberto com a lista atualizada, "A ordem mudou enquanto você editava. Confira e salve de novo." |
| `403` | mensagem do servidor; nada muda |
| `404` num apagar | conta como feito (já estava apagado) e sai do aparelho |
| `404` em outra escrita | "Este item não existe mais." + sync |

### 8.3 Depois de uma escrita

A resposta é aplicada ao índice **na hora**, sem mexer no cursor (`GallerySyncer.applyLocal`, sob o mesmo lock do
sync: espera um sync em andamento em vez de disputar com ele), o disco é podado (originais e capas que o índice não
usa mais), e um `syncAfterWrite` traz o que o servidor derivou (capas dos ancestrais, `album_name` das fotos,
`position` dos outros). O feed seguinte pode repetir o item; aplicar de novo não muda nada.

### 8.4 Álbuns

- Nome obrigatório, sem espaços nas pontas, 1–100 caracteres (validado antes de enviar); descrição e data opcionais.
- Mover: escolha numa árvore (bottom sheet) com "Raiz" e todos os álbuns **menos o próprio e os descendentes**; o pai
  atual aparece desabilitado. Vai para o fim dos novos irmãos.
- Apagar (`owner`): "Apagar '{nome}' com {n} subálbuns e {m} fotos? Fica 30 dias na lixeira." — contagens de toda a
  subárvore a partir do índice; partes vazias omitidas; singular "1 subálbum", "1 foto".
- Capa: "Trocar capa" (imagem do aparelho, preparada como um envio) ou "Usar como capa" no visualizador (manda o
  original do aparelho; sem ele, baixa antes). "Remover capa" (`owner`) só com capa própria; depois a capa resolvida
  pode vir de um sub-álbum ou ficar preta.

### 8.5 Organizar

Modo explícito na raiz (álbuns raiz) e no álbum (sub-álbuns entre si, fotos entre si — nunca de um grupo para o
outro), com `sh.calvin.reorderable` sobre a grid existente: toque longo e arrasta. "Salvar" manda um pedido por grupo
que mudou (sub-álbuns primeiro); nada mudou = nenhum pedido. "Cancelar" (ou voltar) descarta. Seleção e Organizar são
exclusivos.

### 8.6 Fotos

- Editar (visualizador): nome (editado sem a extensão, que é recolocada ao salvar), descrição, data da foto.
- Seleção: toque longo numa foto da grid; toques alternam; voltar sai. Uma foto removida por um sync sai da seleção.
- Mover: árvore de álbuns, o atual desabilitado, sem "Raiz". Apagar: "Apagar 1 foto? Fica 30 dias na lixeira." /
  "Apagar {n} fotos? Ficam 30 dias na lixeira.".
- Em lote: um pedido por foto, em sequência, **todas tentadas**; um sync no fim. Resultado: "{n} fotos movidas para
  '{álbum}'" / "{n} fotos apagadas", ou "{ok} de {total} fotos movidas. {k}: {motivo}" com uma parte por motivo. Um
  `403` para o lote (as seguintes teriam a mesma resposta) e as conta como falhas com a mesma mensagem.

### 8.7 Envio de fotos

"Adicionar fotos" abre o Photo Picker do sistema (`PickMultipleVisualMedia`, só imagens, sem permissão). O destino é
sempre o álbum aberto.

1. **Cópia imediata** (`PickedImageCopier`, no `viewModelScope`): cada imagem é copiada para
   `gallery/uploads/{uploadId}-src.{ext}` (a permissão da URI do picker não sobrevive ao processo). Cada item recebe
   um `uploadId` UUID v4, gerado **uma vez**, e entra na fila como `Waiting`. Uma URI ilegível entra como falha.
2. **Fila persistente** (`GalleryUploadQueueStore`, snapshot `gallery_upload_queue`, chaves `upload_id`, `album_id`,
   `display_name`, `file_name`, `state` = `waiting` / `prepared` / `failed`, `failure`, `enqueued_at`). Sobrevive a
   sair da tela e à morte do processo.
3. **Worker** `GalleryUploadWorker` (trabalho único `gallery_upload`, `APPEND_OR_REPLACE`, qualquer rede, backoff
   exponencial a partir de 30 s), em primeiro plano (`dataSync`) quando o sistema permite; senão roda como worker
   comum. Um item por vez, do mais antigo, relendo a fila a cada item (o que chega durante a rodada é atendido nela).
   Sessão conferida antes de cada pedido.
4. **Preparo** (`GalleryImagePreparer`, uma vez por item): decodifica (HEIF inclusive, API 28+), reduz para lado
   maior ≤ 4000 px, gira os pixels para a posição certa e grava orientação "normal", JPEG qualidade 90 (descendo até
   60 se passar de 10 MB), fundo branco onde havia transparência, e copia as tags de data da captura
   (`DateTimeOriginal`, `OffsetTimeOriginal`, `SubSecTimeOriginal`, `DateTimeDigitized`, `DateTime`) — nada de
   localização. GIF dentro de 10 MB e 50 MP vai como está. O nome no servidor é o do arquivo escolhido com a extensão
   do que é enviado (`IMG_0042.HEIC` → `IMG_0042.jpg`; sem nome, `foto_{yyyyMMdd_HHmmss}.jpg`).
5. **Envio**: `album_id`, `client_upload_id` (o mesmo em toda tentativa) e um `image`.

| Resposta | Efeito |
|----------|--------|
| `201`/`207` com a foto em `accepted` (nova ou repetição de uma viva, talvez noutro álbum) | aplicada ao índice; o arquivo preparado vira o original (não é baixado de volta); sai da fila |
| `207`/`400` com `rejected` | falha com o motivo em português do servidor |
| `409` (original na lixeira) | falha com "Esta foto já foi enviada e depois apagada; ela está na lixeira." |
| `404` (álbum apagado) | falha "O álbum foi apagado" para todos os itens desse álbum |
| `403` | falha com a mensagem do servidor para todos os itens restantes |
| `401` | a rodada para; os itens ficam (o caminho de sessão decide) |
| rede, `429`, `5xx` | nova tentativa depois, com o mesmo id |
| `400` sem `rejected` | falha "Não foi possível enviar esta foto." |
| imagem ilegível / grande demais | falha "Não foi possível ler esta imagem." / "Imagem grande demais para enviar." |

Um sync no fim de cada rodada que enviou algo. Notificação (canal `gallery_upload`) "Enviando X de N" durante a
rodada e, se algo falhou, "{n} fotos não foram enviadas". Sem permissão de notificação (o app já pede na abertura) o
envio segue e o progresso aparece no álbum. As falhas ficam listadas no álbum e cada uma pode ser dispensada (o
arquivo é apagado).
