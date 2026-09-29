# Galeria — Spec

A Galeria é o acervo de fotos dos eventos da igreja, organizado numa **árvore de álbuns** com capas e ordem
definidas pela igreja. É **conteúdo restrito a membros** (`IsMemberUser` no servidor) e **somente leitura** no app
para todos os membros. O app mantém uma **cópia local** que segue o servidor: novos, alterados e apagados — álbuns e
fotos — chegam ao aparelho pelo feed de mudanças.

Fonte da verdade no backend: `backend/specs/gallery/spec.md`, `013-gallery-write-api/contracts/gallery-api.md`,
`014-gallery-trash-sync/contracts/gallery-trash-api.md`, `015-gallery-member-tags/contracts/gallery-tags-api.md`.

Ainda não existem no app (features seguintes): gestão (criar, renomear, mover, ordenar, capas, upload, apagar),
lixeira, e marcação de membros ("Minhas fotos", filtros). O campo `members` das fotos já é guardado, mas não é
exibido. O card "Galeria" do painel de gestão continua desabilitado.

---

## 1. Telas

| Tela    | Rota                          | Origem                      |
|---------|-------------------------------|-----------------------------|
| Galeria | `GalleryMain`                 | `CoreScreen → botão Galeria` |
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

`api/photos/`, `api/albums/` e `api/albums/{id}/photos/` não são mais usados pelo app.

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
cacheDir/gallery-*.part                       temporários da escrita atômica
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

- **Um sync por vez**: pedido enquanto outro roda é pulado (`Skipped`) — o que está rodando atende.
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

Cancela o download e o sync periódico, cancela um sync em andamento e apaga índice, cursor, originais, capas e o cache
de previews (`GalleryAutoDownloadUseCase.clearOnLogout()`). O acervo é restrito a membros e não sobrevive ao fim da
sessão. "Resetar galeria" nas configurações apaga índice e arquivos; o próximo sync reconstrói tudo.
