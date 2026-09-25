# Galeria — Spec

A Galeria é o acervo de fotos dos eventos da igreja. É **conteúdo restrito a membros**: o servidor
protege os dois endpoints com `IsMemberUser`. No app ela funciona **offline-first** — nada é
exibido a partir da rede, só a partir do disco. Toda imagem vem de arquivo local; nenhuma tela carrega URL de
mídia direto. A mídia (`/ipbcb/media/gallery/`) exige JWT e só é baixada pelo `@AuthedRetrofit`. O que a tela mostra é sempre o resultado de um
download já concluído.

---

## 1. Telas

| Tela      | Rota                        | Origem                        |
|-----------|-----------------------------|-------------------------------|
| Galeria   | `GalleryMain`               | `CoreScreen → botão Galeria`  |
| Álbum     | `Album/{albumId}`           | Clique num álbum da grid      |
| Foto      | `Photo/{albumId}/{index}`   | Clique numa foto do álbum     |

As três vivem em `galleryGraph` (`GalleryNavGraph.kt`), com `GalleryViewModel` escopado ao grafo
via `hiltViewModel(graphEntry)`.

`isLoggedIn` é **parâmetro de `galleryGraph`** (`StateFlow<Boolean>`), vindo do `CoreViewModel` que
o `AppNavHost` resolve. Não pode ser buscado por `getBackStackEntry(AppRoutes.CORE)`: o
`CoreViewModel` é resolvido **fora** do `NavHost`, no escopo da Activity, então aquele lookup
devolve uma segunda instância — sem `initialize()`, com `_isLoggedIn` travado em `false`. Era o
que fazia a galeria pedir login a quem já estava logado.

### 1.1 Galeria

Grid de 2 colunas de álbuns (`AlbumItem`: thumbnail + nome). Acima da grid, um banner
não-bloqueante reflete o estado do download em andamento.

### 1.2 Álbum

Grid de 3 colunas com as fotos do álbum, lidas do disco (`getLocalPhotos`). Vazio exibe
"Nenhuma foto neste álbum."

### 1.3 Foto

Visualizador em pager horizontal, com o nome da foto na top bar.

---

## 2. Dados

### 2.1 `GET api/photos/` — `IsMemberUser`

```json
[{
  "id": 12,
  "name": "img00.jpg",
  "description": "",
  "album_id": 3,
  "album_name": "Acampamento 2025",
  "image_url": "https://.../media/gallery/img00.jpg",
  "date_taken": null,
  "uploaded_at": "2026-03-11T14:02:00Z"
}]
```

A extensão do arquivo salvo é derivada de `image_url` (`png` / `webp` / `jpg`, com `jpg` como
padrão).

Galeria vazia é `200` com `[]` — **não** é erro.

| Código | Quando                          | `detail`                                              |
|--------|---------------------------------|-------------------------------------------------------|
| `401`  | Sem token / token inválido      | "As credenciais de autenticação não foram fornecidas." |
| `403`  | Autenticado, mas não é membro   | "Disponível apenas para membros."                      |

### 2.2 `GET api/albums/{id}/photos/` — `IsMemberUser`

Mesmo payload, filtrado por álbum. Usado só pelo download por álbum.

### 2.3 Disco

`filesDir/gallery/{albumId}/{photoId}.{ext}` para a imagem e `{photoId}.json` para o
`GalleryPhotoDto` correspondente. Os álbuns são **derivados do disco**: cada subdiretório é um
álbum, e o nome vem do `album_name` da primeira foto. Thumbnail é a foto chamada `img00.jpg`, ou
a primeira do álbum.

A gravação é atômica: a imagem é escrita primeiro num arquivo temporário em `cacheDir` e só é movida
para `{photoId}.{ext}` quando o corpo chegou inteiro. `exists()` considera baixada qualquer foto com
nome `{photoId}.*`, então um arquivo truncado com o nome final viraria um buraco permanente. O temporário
fica fora de `gallery/` porque ali todo subdiretório é lido como álbum.

`GalleryRepositoryImpl` é `@Singleton` e expõe `albumsFlow`, `thumbnailsFlow` e `photosFlow`,
repopulados por `preload()` (registrado como `Preloadable`).

---

## 3. Download

Roda em `GalleryDownloadWorker` (WorkManager, trabalho único `gallery_auto_download`), nunca no
`viewModelScope` — o download é longo e precisa sobreviver à saída da tela.

| Gatilho                          | Política   | Rede       |
|----------------------------------|------------|------------|
| Boot do app, galeria vazia       | `KEEP`     | `UNMETERED`|
| Login bem-sucedido               | `REPLACE`  | `UNMETERED`|
| Botão "Baixar Galeria Completa"  | `KEEP`     | `UNMETERED`|
| Botão "Tentar novamente"         | `REPLACE`  | `UNMETERED`|
| Botão "Usar dados móveis"        | `REPLACE`  | `CONNECTED`|
| Logout                           | cancela    | —          |

O worker chama `repository.preload()` a cada álbum concluído — e antes de terminar, em qualquer
desfecho —, então a grid ganha álbuns durante o download.

### 3.1 Uma foto só conta quando está no disco

O laço por foto vive em `GalleryPhotoDownloader` (`data/download/`), usado pelo worker e por
`GalleryRepositoryImpl.downloadAlbum`/`downloadAllPhotos`. `downloaded` é sempre o número de fotos da
lista que estão no disco ao fim da rodada; o progresso só avança numa foto salva ou já presente.

A mídia (`/ipbcb/media/`) é protegida por JWT (backend `009-protected-media-access`). Cada foto é
classificada assim:

| Resposta da foto                       | Efeito                                             |
|----------------------------------------|----------------------------------------------------|
| já no disco                            | conta, sem requisição                              |
| `2xx` com corpo, salvo                 | conta                                              |
| `2xx` sem corpo, `404`, outro erro     | não conta, segue; tentada de novo na próxima rodada |
| `IOException` (inclusive corpo cortado) | não conta, segue; conta como falha de rede         |
| `401` (após o refresh falhar)          | **para a rodada** — sessão expirada                |
| `403`                                  | **para a rodada** — "sem acesso"                   |
| `429`                                  | **para a rodada**                                  |

`401`, `403` e `429` descrevem quem pede, não a foto: toda requisição seguinte teria a mesma resposta.
Por isso param a rodada em vez de marcar a foto como falha.

### 3.2 Como a rodada termina

| Resultado da rodada                              | `Result`                                         |
|--------------------------------------------------|--------------------------------------------------|
| lista de fotos falhou (`401`/`403`)              | `failure` com `errorCode`                        |
| lista de fotos falhou (outros)                   | `retry` até 3 tentativas, depois `failure`       |
| completa, sem falha de rede                      | `success` (falhas `404` ficam para o próximo gatilho) |
| completa, com falha de rede                      | `retry` até 3 tentativas, depois `success`       |
| parada por `429`                                 | `retry`, **sem limite de tentativas**            |
| parada por `403`                                 | `failure(403)`, sem nova tentativa               |
| parada por `401`                                 | `failure(401)`, sem nova tentativa               |

Em `403` as fotos já baixadas **ficam** no aparelho e continuam visíveis — igual ao `403` da lista de
fotos, e uma recusa equivocada do servidor nunca apaga a galeria local. Elas só saem no logout. A
mensagem é o `userMessage` do `AppError` (o `detail` estruturado) ou, sem ele, "Disponível apenas para
membros.".

`429` nunca vira erro na tela: cada rodada limitada salva ao menos as fotos anteriores ao limite, então
a sequência sempre termina. As duas requisições (`enqueueWifiOnly`, `enqueueAnyNetwork`) usam backoff
`EXPONENTIAL` a partir de 60 s. `Retry-After` não é honrado — `CoroutineWorker` não define atraso por
tentativa.

Um `401` só chega ao laço depois de o `TokenAuthenticator` tentar renovar o token. Se o refresh foi
recusado (`401`/`400`), ele apaga os tokens, `isLoggedInFlow` vira `false` e a tela cai no ramo
"Faça login para acessar a galeria.". Não há logout completo nesse caminho: o trabalho não é cancelado e
as fotos locais não são apagadas — isso só acontece no logout explícito. Se o refresh falhou por rede,
os tokens ficam e a tela mostra o placeholder de `401` com o botão de login.

### 3.3 O download só é disparado com sessão ativa

`triggerIfNeeded()` no boot **exige login**. Sem essa guarda, um usuário deslogado enfileira um
trabalho que só pode terminar em `401`, e o `WorkInfo` `FAILED` fica retido no banco do
WorkManager por dias. Como `downloadState` é derivado de `getWorkInfosForUniqueWorkFlow`, esse
erro antigo continua sendo emitido **depois** de o usuário logar — a tela pedia login a quem já
estava logado. Por isso o login bem-sucedido também re-enfileira com `REPLACE`: substituir o
trabalho é o que apaga o `WorkInfo` falho.

Logout cancela o trabalho e apaga as fotos do disco — o acervo é restrito a membros e não deve
sobreviver ao fim da sessão.

---

## 4. Estados da tela

`GalleryDownloadState` (`isDownloading`, `isPending`, `isResuming`, `downloaded`, `total`, `error`,
`errorCode`, `isResolved`) é mapeado do `WorkInfo`:

| `WorkInfo.State`                       | Estado                                       |
|----------------------------------------|----------------------------------------------|
| `RUNNING`                              | `isDownloading` + progresso                  |
| `ENQUEUED`, `runAttemptCount == 0`     | `isPending`                                  |
| `ENQUEUED`, `runAttemptCount > 0`      | `isResuming` (backoff após `429` ou rede)    |
| `FAILED`             | `error` + `errorCode`                        |
| demais / ausente     | resolvido, sem erro                          |

`isResolved` distingue "ainda não sei" (valor inicial do `stateIn`) de "sei que não há nada" —
sem ele a tela piscaria o estado vazio antes do primeiro `WorkInfo` chegar.

Ordem de decisão da tela:

1. **Deslogado** → "Faça login para acessar a galeria." + botão de login.
2. **Banner** (independe da grid): baixando / pausado ("Download pausado. Continua automaticamente em
   instantes.", sem botão, não é erro) / aguardando WiFi (com atalho para dados móveis) / **`errorCode ==
   403` com álbuns no aparelho** → aviso "Disponível apenas para membros." acima da grid, sem botão.
3. **Com álbuns** → grid.
4. **Sem álbuns e sem download**:
   - não resolvido → `CircularProgressIndicator`
   - `errorCode == 401` → mensagem + "Conectar à sua conta"
   - `errorCode == 403` → mensagem, **sem** botão de login (logar de novo não torna ninguém membro)
   - outro erro → mensagem + "Tentar novamente"
   - sem erro → "Nenhum álbum disponível localmente." + "Baixar Galeria Completa"

O botão de login aparece **somente** em `401`. Falha de rede não é problema de sessão, e oferecer
login ali manda o usuário a uma tela que não resolve nada.
