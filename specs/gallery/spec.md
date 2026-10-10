# Galeria — Spec

A Galeria é o acervo de fotos dos eventos da igreja, organizado numa **árvore de álbuns** com capas e ordem
definidas pela igreja. É **conteúdo restrito a membros** (`IsMemberUser` no servidor). Para quem não tem nível no
escopo `gallery` ela é **somente leitura**; quem tem `manage` ou `owner` monta a galeria **nas mesmas telas** (seção
8). O app mantém uma **cópia local** que segue o servidor: novos, alterados e apagados — álbuns e fotos — chegam ao
aparelho pelo feed de mudanças, e o que o próprio usuário grava é aplicado localmente na hora.

Fonte da verdade no backend: `backend/specs/gallery/spec.md`, `013-gallery-write-api/contracts/gallery-api.md`,
`014-gallery-trash-sync/contracts/gallery-trash-api.md`, `015-gallery-member-tags/contracts/gallery-tags-api.md`,
`016-photo-upload-idempotency/contracts/photo-upload-api.md`.

Cada foto diz quem está nela (membros marcados); todo membro vê as pessoas de uma foto e filtra a galeria por pessoas
(achando a si mesmo no topo da lista), e quem tem `manage` marca pessoas numa foto ou em várias (seção 10). A
gestão vive nas próprias telas da galeria — o painel de gestão não tem card dela (seção 8.1).

---

## 1. Telas

| Tela    | Rota                          | Origem                      |
|---------|-------------------------------|-----------------------------|
| Galeria | `GalleryMain`                 | `CoreScreen → botão Galeria` |
| Álbum   | `Album/{albumId}`             | Álbum na grid (raiz ou sub-álbum) |
| Foto    | `Photo/{albumId}/{photoId}`   | Foto na grid do álbum       |
| Foto (resultado) | `PeoplePhoto/{memberIds}/{photoId}` | Foto no resultado do filtro (seção 10.6) |
| Pessoas | `GalleryPeople`               | "Pessoas" no menu ⋮ da raiz (seção 10.6) |
| Lixeira | `GalleryTrash`                | "Lixeira" no menu ⋮ da raiz (`owner`, seção 9) |

Todas vivem em `galleryGraph` (`GalleryNavGraph.kt`). Galeria, Álbum e Foto usam um `GalleryViewModel` escopado
ao grafo via `hiltViewModel(graphEntry)`; a Lixeira tem o próprio `TrashViewModel` e Pessoas o próprio
`PeopleViewModel`, presos às suas entradas (a lista da lixeira é lida de novo a cada visita; a seleção de pessoas dura
a visita). Cada tela coleta um `StateFlow` de UiState (`GalleryRootUiState`, `AlbumUiState`,
`PhotoViewerUiState`) e passa dados puros ao composable de conteúdo. O estado de cada álbum é memoizado pelo id no
ViewModel; o de cada visualizador, pela entrada do back stack que o abriu (`NavBackStackEntry.id`), e é descartado
quando essa entrada sai da pilha — não numa rotação, em que a entrada continua.

`isLoggedIn` é **parâmetro de `galleryGraph`** (`StateFlow<Boolean>`), vindo do `CoreViewModel` que o `AppNavHost`
resolve. Não pode ser buscado por `getBackStackEntry(AppRoutes.CORE)`: o `CoreViewModel` vive no escopo da Activity,
e aquele lookup devolveria uma segunda instância, sem `initialize()`, presa em "deslogado".

Toda lista é ordenada por `position`, depois `id` — nunca por nome de arquivo.

### 1.1 Galeria (raiz)

Grid de 2 colunas com os álbuns raiz (`parent_id` nulo): capa quadrada (preta quando não há) e nome. Acima da grid,
um banner não-bloqueante (seção 5); nada mais fica entre a barra e os álbuns. Na
barra, um único menu ⋮ reúne "Pessoas" (abre o filtro, seção 10.6), "Lixeira" (`owner`) e "Organizar" (`manage`, com
2+ álbuns) — sem ícones soltos, para não poluir a barra; o menu some durante o "Organizar". Abrir a galeria dispara um
sync.

### 1.2 Álbum

Uma rolagem: sub-álbuns primeiro (2 por linha, com capa), depois as fotos (3 por linha). Com os dois, cada grupo
abre com um título de linha inteira — "Álbuns · {n}" e "Fotos · {n}" —, e as fotos sempre começam numa linha nova,
nunca ao lado de um sub-álbum; com um só dos dois, não há título. Título = nome do álbum;
subtítulo = nome do álbum pai (raiz não tem). Data do evento (`dd/MM/yyyy`) e descrição quando existem. Sem fotos e
sem sub-álbuns: "Nenhuma foto neste álbum.". Cada álbum aberto é uma entrada do back stack — voltar sobe um nível.

### 1.3 Foto

Pager horizontal pelas fotos **de onde foi aberto** (`ViewerSource`): o álbum (o `albumId` da rota), na ordem da
grid, ou o resultado de um filtro de pessoas (os `memberIds` da rota, seção 10.6); abre sempre na foto tocada, mesmo
que uma visita anterior a partir dela tenha deslizado para outra; uma rotação mantém a página atual. Top bar = nome
da foto sem extensão, "ⓘ" (detalhes, seção 10.2) e o menu de gestão. Zoom por pinça, duplo toque volta ao normal, "Baixar" (salva em
`Pictures/ipb_castelobranco` com o nome da foto e o MIME pela extensão) e "Compartilhar". Foto ainda sem original no
aparelho aparece pelo preview (ou cinza), com "Baixar" e "Compartilhar" desabilitados.

### 1.4 Item removido com a tela aberta

| Caso | Efeito | Aviso |
|------|--------|-------|
| Foto na tela apagada | pager vai para a próxima (a anterior, se era a última) | "Esta foto foi removida" |
| Foto na tela movida para outro álbum | idem | "Esta foto foi movida para outro álbum" |
| Última foto do álbum apagada | o visualizador fecha | "Esta foto foi removida" |
| Álbum aberto apagado | a tela sobe um nível; se o de baixo também saiu (subárvore apagada), ele faz o mesmo | "Este álbum foi removido" |
| Foto na tela sai do resultado do filtro (alguém desmarcou uma das pessoas) | idem à foto apagada | "Esta foto não está mais no resultado" |

Quando quem removeu foi o próprio usuário (seção 8), o efeito é o mesmo e o aviso é o da ação: "Álbum enviado para a
lixeira", "Foto enviada para a lixeira", "Foto movida para '{álbum}'", "Marcações salvas".

O aviso é um `StateFlow<GalleryMessage?>` consumível no ViewModel, mostrado (snackbar, `GalleryMessageHost`) pela
tela que está no topo e consumido uma vez — não um `SharedFlow`, porque durante o desempilhamento pode não haver tela
coletando. Uma tela que está saindo (álbum removido, visualizador fechado) não consome: a de baixo mostra. O aviso pode
ter um botão (`MessageAction`): "Desfazer" (seção 9.4) ou "Abrir álbum" (seção 9.3).

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
`@GalleryThumbnailLoader` (Coil sobre o `@Client` autenticado, cache em disco de 100 MB, memória limitada a 10% —
`core` 5.6.1). Com o original no disco, usa o original. Sem original e sem preview (nulo, offline sem cache, erro): cinza.

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
sessão. "Resetar galeria" nas configurações apaga índice e arquivos; o próximo sync reconstrói tudo. As marcações
moram no índice (apagado junto); a lista do seletor de pessoas só existe em memória.

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
| Raiz | FAB "Novo álbum"; menu ⋮ "Organizar" (com 2+ álbuns) | `manage` |
| Raiz | menu ⋮ "Lixeira" (fora do "Organizar") | `owner` |
| Álbum | FAB "+" → "Adicionar fotos" / "Novo álbum" (sub-álbum) | `manage` |
| Álbum | menu: "Editar álbum", "Mover álbum", "Trocar capa", "Organizar" (com 2+ sub-álbuns ou 2+ fotos) | `manage` |
| Álbum | menu: "Remover capa" (só se a capa é própria: `cover_source_album_id == id`), "Apagar álbum" | `owner` |
| Álbum | toque longo numa foto → seleção; barra: "Pessoas" e "Mover" (`manage`), "Apagar" (`owner`) | `manage` |
| Álbum | faixa de envio ("Enviando X de N", "Preparando fotos…") e lista "Não enviadas" | — (itens da fila) |
| Foto | menu: "Editar foto", "Marcar pessoas", "Mover", "Usar como capa"; "Marcar pessoas" também nos detalhes | `manage` |
| Foto | menu: "Apagar" | `owner` |

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
| Marcar pessoas numa foto | `PUT api/photos/{id}/members/` `{member_ids}` (o conjunto inteiro; `[]` limpa) | upsert da foto |
| Pessoas em várias fotos | `POST api/photos/members/` `{photo_ids, add_member_ids, remove_member_ids}` (até 200 fotos) | upsert de cada foto devolvida |

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

---

## 9. Lixeira

Quem tem `owner` em `gallery` vê o que foi apagado nos últimos 30 dias e restaura. Só online: nada é guardado no
aparelho. Não há "apagar para sempre" nem "esvaziar": o purge diário do servidor é o único apagamento definitivo.

### 9.1 Chamadas (`@AuthedRetrofit`, `GalleryApi`)

| Ação | Pedido | Resposta |
|------|--------|----------|
| Listar | `GET api/gallery/trash/` | uma entrada por ação de apagar, mais recente primeiro; `[]` vazia |
| Restaurar álbum | `POST api/gallery/trash/albums/{id}/restore/` | `200` Album (volta com tudo o que foi apagado junto) |
| Restaurar foto | `POST api/gallery/trash/photos/{id}/restore/` | `200` Photo |

Entrada: `kind` (`album`/`photo`; outro valor é ignorado), `id`, `name`, `deleted_at`, `deleted_by`, `uploaded_by`,
`purge_on`, `sub_album_count`, `photo_count`, `thumbnail_url`. O que foi apagado junto com um álbum não aparece nem
se restaura sozinho.

### 9.2 Tela

Abre lendo a lista (carregando / erro com "Tentar novamente" — sem rede: "Sem conexão. A lixeira precisa de
internet." / vazia: "A lixeira está vazia." / lista) e tem pull-to-refresh. No topo: "Os itens ficam 30 dias na
lixeira e depois são apagados para sempre. Restaurar um álbum traz de volta tudo o que foi apagado com ele."

Cada linha, na ordem do servidor: preview pelo `@GalleryThumbnailLoader` (cinza sem preview ou em erro; sem tela
cheia), nome, "Álbum"/"Foto", "Apagado por {nome} em {dd/MM/yyyy HH:mm}" (hora local; "usuário desconhecido" sem
nome), para álbuns "{n} subálbuns · {m} fotos" (singular "1 subálbum", "1 foto"; partes zero omitidas), para fotos
"Enviada por {nome}" quando conhecido, e "Some em {dd/MM/yyyy}". Botão "Restaurar", sem confirmação.

Um restauro por vez: a linha mostra progresso, os outros botões e o pull-to-refresh ficam desativados. Perder
`owner` com a tela aberta fecha a tela: "Você não tem mais acesso à lixeira.". Sair da tela durante um restauro para
de esperar a resposta; o que o servidor fez chega no próximo sync.

### 9.3 Depois de restaurar

| Resposta | Efeito |
|----------|--------|
| `200` | o item volta ao índice na hora (`applyLocal`, cursor intacto), `syncAfterWrite` traz o resto do lote e enfileira originais faltando no WiFi; a linha sai; "Álbum restaurado" / "Foto restaurada" |
| `404` | a lista é relida; "Este item não está mais na lixeira." |
| `400` com `trashed_parent_id` | mensagem do servidor; se o pai é uma linha, a lista rola até ele e o destaca (até o próximo restauro, recarga ou saída) |
| `400` com `conflicting_album_id` | mensagem do servidor com "Abrir álbum" (abre `Album/{id}`); se o álbum não está no índice, sincroniza antes; se continua faltando, só a mensagem |
| sem rede / `403` / outro | "Sem conexão" / mensagem do servidor; a lista não muda |

A classificação (`RestoreTrashItemUseCase` → `RestoreResult`) lê os extras de `AppError.Server.extras`; os textos
ficam em `TrashTexts`, os mesmos para a lixeira e para "Desfazer".

### 9.4 Desfazer

Depois de apagar **um** álbum (menu do álbum) ou **uma** foto (visualizador, ou seleção de uma só), o aviso "Álbum
enviado para a lixeira" / "Foto enviada para a lixeira" vem com "Desfazer", se o usuário tem `owner` naquele momento.
Tocar restaura com o mesmo tratamento da seção 9.3 ("Álbum restaurado", "Este item não está mais na lixeira.", "Abrir
álbum" num conflito de nome); é ignorado se o `owner` foi perdido. Apagar duas ou mais fotos não oferece "Desfazer":
cada foto é uma entrada da lixeira.

---

## 10. Pessoas nas fotos

Cada foto traz `members` (`[{id, name}]`, por nome, depois id; `[]` sem marcação), guardado no índice. Membros
inativos aparecem como qualquer outro. O feed devolve a foto quando as marcações mudam ou quando um membro marcado nela
é renomeado ou apagado (também pelo Django admin), então tudo abaixo segue o índice sem nada extra. O app **não** usa
`GET api/gallery/tagged-members/` nem o filtro `member_id` do servidor: o índice já tem todas as marcações, e o filtro
funciona offline.

### 10.1 Membro do perfil

`GET accounts/api/me/profile/` traz `member_id` (inteiro ou `null`; só leitura; ligado pelo Django admin).
`MeProfileDto.memberId` tem default `null` (um perfil em cache antigo decodifica). A galeria lê o valor pela porta de
core `CurrentMemberRepository` / `ObserveOwnMemberIdUseCase` (`core/domain/member`), implementada pelo perfil
(`ProfileCurrentMemberRepository`) a partir do mesmo snapshot do `/me`: segue toda releitura do perfil. Fica fora do
`Access` — não é permissão.

### 10.2 Detalhes da foto (todo membro)

"ⓘ" na barra do visualizador abre um bottom sheet da página atual: descrição (se houver), "Tirada em {dd/MM/yyyy}"
(se houver) e "Nesta foto" com os nomes na ordem da foto, ou "Ninguém marcado". Os nomes são texto; tocar não faz
nada. O sheet segue o pager e o índice (um sync que muda as marcações atualiza na hora). Com `manage`, tem o botão
"Marcar pessoas"; o sheet se recolhe enquanto o seletor está aberto.

### 10.3 Marcar pessoas numa foto (`manage`)

"Marcar pessoas" (menu do visualizador ou detalhes) abre o seletor: lê `GET api/gallery/taggable-members/` a cada
abertura (online; carregando, erro com "Tentar novamente", "Nenhuma pessoa cadastrada."; nunca em disco — o servidor
manda `no-store`). Busca local por nome sem diferenciar maiúsculas nem acentos (`NameSearch`, `java.text.Normalizer`);
sem resultado: "Nenhuma pessoa encontrada.". Seleção múltipla; as pessoas atuais da foto vêm marcadas e primeiro, as
outras por nome. "Salvar" manda um `PUT` com o conjunto inteiro; conjunto igual ao atual = nada é enviado e o seletor
fecha. Sucesso: a foto devolvida vai para o índice (sem mexer no cursor), sync, "Marcações salvas". Perder `manage`
fecha o seletor; a foto sair do visualizador também.

### 10.4 Pessoas em várias fotos (`manage`)

Na seleção do álbum, "Pessoas" → "Adicionar pessoas" (o mesmo seletor, ninguém marcado; manda `add_member_ids`) ou
"Remover pessoas" (lista só quem está marcado em alguma foto selecionada, calculada do índice, sem rede; desabilitado
quando ninguém está; manda `remove_member_ids`). Confirmar exige ao menos uma pessoa. Mais de 200 fotos: pedidos de até
200, em sequência, **todos tentados** (um `403` interrompe); as fotos devolvidas vão para o índice a cada pedido e um
sync roda no fim. Resultado: "Marcações atualizadas em {n} fotos" ("1 foto") e a seleção termina; com falha,
"Marcações atualizadas em {ok} de {total} fotos: {motivo}" (só o motivo quando nada passou), e a seleção fica.

### 10.5 Erros nas marcações

| Resposta | Efeito |
|----------|--------|
| `404` (`missing_photo_ids` / `missing_member_ids`) | nada mudou no servidor; sync, o seletor recarrega, "Algumas fotos ou pessoas não existem mais. Confira e tente de novo." — as listas extras não são lidas |
| `400` e outros | mensagem do servidor (ou o texto genérico); uma foto: o seletor fica aberto; várias: fecha e a seleção fica |
| sem rede | "Sem conexão" |
| `403` | tratamento global (seção 8.1); o seletor fecha e os controles somem |

### 10.6 Filtro por pessoas (todo membro, offline)

"Pessoas" na raiz abre `GalleryPeople`. A lista vem do índice (`GalleryPeople.taggedPeople`): toda pessoa marcada em ao
menos uma foto alcançável, com "{n} fotos" ("1 foto"), por nome (sem acentos), com busca local. Ninguém marcado:
"Ninguém foi marcado nas fotos ainda.". Seleção múltipla; as escolhidas ficam como chips no topo. Com alguém escolhido
e a busca vazia, a tela mostra o resultado **E**: as fotos em que todas as pessoas escolhidas estão marcadas
(`photosWithAll`), numa grid de 3 colunas na ordem da árvore (pré-ordem; dentro do álbum por `position`, depois `id`),
com "Fotos com todas as pessoas selecionadas" a partir de duas pessoas e "Nenhuma foto com todas essas pessoas." quando
vazio. Escolher na lista limpa a busca. Seleção e índice atualizam o resultado ao vivo; uma pessoa que deixou de estar
marcada em qualquer foto sai da lista e da seleção. A seleção dura a visita (volta do visualizador intacta).

Tocar numa foto abre o visualizador pela rota `PeoplePhoto/{ids separados por vírgula}/{photoId}`, que pagina pelo
resultado, não pelo álbum. Detalhes, menu de gestão e tratamento de remoção funcionam como no álbum; as ações do menu
usam o álbum da própria foto. Uma foto que sai do resultado (apagada, ou desmarcada) conta como removida (seção 1.4);
quando foi o próprio usuário que desmarcou, o aviso é "Marcações salvas".

### 10.7 Você no filtro

Não há entrada nem tela própria de "Minhas fotos": com `member_id` no perfil, a própria pessoa vem **primeiro** na
lista do filtro (também entre os resultados de uma busca), como "{nome} (você)", e escolhê-la é como escolher qualquer
outra — sozinha, o resultado são as fotos dela. Sem vínculo, ou sem marcação em nenhuma foto, ela não tem destaque (e,
sem marcação, não aparece, como qualquer pessoa). O destaque segue o perfil ao vivo; a busca casa só com o nome.
