# IPB Castelo Branco

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen?style=flat)
<a href="https://play.google.com/store/apps/details?id=com.ipb.castelobranco" target="_blank"><img src="https://img.shields.io/badge/Play%20Store-Disponível-34A853?style=flat&logo=googleplay&logoColor=white" alt="Play Store"></a>

> **Em produção** — <a href="https://play.google.com/store/apps/details?id=com.ipb.castelobranco" target="_blank">disponível publicamente na Google Play Store</a>.

App oficial da Igreja Presbiteriana de Castelo Branco. Centraliza a escala mensal de responsabilidades, hinário, ferramentas de louvor e área administrativa em um único lugar — substituindo o uso disperso de planilhas, PDFs e grupos de mensagens.

---

## Funcionalidades

### Público geral
- **Escala mensal** — tabela de responsáveis pelo estudo e liturgia de cada culto, com exportação direta para o WhatsApp
- **Hinário Novo Cântico** — hinário oficial da Igreja Presbiteriana, com busca por número ou título e visualização completa da letra
- **Hub para o ministério de Louvor** — letras, cifras e histórico dos louvores
- **Tabela de músicas** — histórico de últimos cultos, repertório, músicas e tons mais usados
- **Bíblia** — leitor offline com seleção de livro/capítulo, posição salva e download automático em background (WorkManager)
- **Galeria** — álbuns de fotos organizados por evento, com sincronização em background via WorkManager (prioriza Wi-Fi)

### Conta de usuário
- **Autenticação** — login com e-mail/senha ou Google (Credentials API / OAuth)
- **Perfil** — edição de dados pessoais e foto com recorte integrado (UCrop)
- **Configurações** — alternância de tema claro/escuro/sistema

### Em desenvolvimento
- **Estudos** — seção de estudos bíblicos (em breve)

### Área administrativa
- **Painel admin** — dashboard de acesso rápido às ferramentas de gestão
- **Editor de escala** — criação e edição da escala mensal diretamente no app
- **Cadastro de música** — registro de novas músicas com metadados (tom, data, etc.)

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Kotlin 2.3.10 |
| UI | Jetpack Compose BOM 2026.02 + Material3 |
| Arquitetura | MVVM + Clean Architecture (feature-based) |
| DI | Hilt 2.59.1 (KSP) |
| Rede | Retrofit 3.0.0 + OkHttp 5.3.2 |
| Serialização | Kotlinx Serialization JSON 1.10.0 |
| Persistência local | DataStore Preferences 1.2.0 |
| Imagens | Coil 2.6.0 + UCrop 2.2.8 |
| Background | WorkManager 2.10.0 |
| Navegação | Navigation Compose 2.9.7 (grafos por feature) |
| Testes | JUnit4 + MockK + Turbine + Coroutines Test |
| Build | AGP 9.0.1, JVM 17 |

---

## Screenshots

| Home | Tabelas | Bíblia | Hinário |
|------|---------|--------|---------|
| <img src="screenshots/home.jpeg" width="180"> | <img src="screenshots/tabelas.jpeg" width="180"> | <img src="screenshots/biblia.jpeg" width="180"> | <img src="screenshots/hinario.jpeg" width="180"> |

---

## Como rodar localmente

### Pré-requisitos

- Android Studio Iguana ou superior
- JDK 17
- Android SDK com API 36

### Configuração

1. Clone o repositório:
   ```bash
   git clone https://github.com/GabriellAfonso/ipbcb-app.git
   cd ipbcb-app
   ```

2. Crie o arquivo `local.properties` na raiz com o caminho do SDK e o Client ID do Google:
   ```properties
   sdk.dir=/caminho/para/seu/android-sdk
   GOOGLE_CLIENT_ID=seu_client_id_aqui
   ```

3. Abra o projeto no Android Studio e aguarde a sincronização do Gradle.

4. Execute em um emulador ou dispositivo com Android 7.0+ (API 24):
   ```
   Run > Run 'app'
   ```

> **API:** O app consome `https://gabrielafonso.com.br/ipbcb/`. Funcionalidades de rede exigem acesso à internet.

---

## Estrutura de pastas

```
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── data/           — DataStore, snapshot cache local
│   ├── di/             — Módulos Hilt (Retrofit, OkHttp, qualifiers)
│   ├── domain/         — Interfaces de repositório, AuthEventBus
│   ├── network/        — AuthInterceptor, TokenAuthenticator (refresh automático)
│   └── presentation/
│       ├── CoreActivity.kt     — única Activity do app
│       ├── navigation/         — AppNavHost, AppRoutes, LocalAppNavigator
│       └── base/BaseScreen.kt  — Scaffold padrão
└── features/
    ├── auth/           — login, registro, Google OAuth
    ├── schedule/       — agenda mensal com exportação WhatsApp
    ├── hymnal/         — hinário com busca
    ├── worshiphub/     — letras, cifras, tabelas de músicas
    ├── gallery/        — álbuns e galeria de fotos
    ├── bible/          — leitor offline com WorkManager para download
    ├── studies/        — em desenvolvimento
    ├── profile/        — perfil e foto do usuário
    ├── settings/       — tema do app
    └── admin/          — painel, editor de escala, cadastro de músicas
```

Cada feature segue o padrão `data/` → `domain/` → `presentation/` com módulo Hilt próprio.

---

## Decisões técnicas

**Single Activity + Compose Navigation**
Toda a navegação é gerenciada por um único `NavHost` com grafos por feature (`worshipHubGraph`, `adminGraph`, etc.). Isso elimina a necessidade de múltiplas Activities e simplifica o gerenciamento de back stack.

**Shared ViewModels por grafo**
Features com múltiplas telas (ex: worshiphub, hymnal, gallery) compartilham um único ViewModel escopado ao grafo via `hiltViewModel(graphEntry)`, evitando recriação de estado ao navegar entre telas do mesmo módulo.

**Dois clientes HTTP**
APIs públicas (auth) usam um `OkHttpClient` sem interceptor de token (`@AuthLessClient`). APIs protegidas usam outro com `AuthInterceptor` + `TokenAuthenticator` para refresh automático (`@Client`). Evita vazamento de token em endpoints públicos e elimina logout manual por expiração.

**Snapshot cache local**
Dados de leitura frequente (hinário, músicas) são armazenados como JSON local via `JsonSnapshotStorage`. O app exibe dados cacheados enquanto busca atualizações em background — funcional mesmo com conexão instável.

**WorkManager para downloads em background**
Galeria e Bíblia usam `WorkManager` para sincronização/download offline. Galeria prioriza Wi-Fi para evitar consumo de dados móveis; Bíblia baixa o conteúdo na primeira abertura via `BibleDownloadWorker`.
