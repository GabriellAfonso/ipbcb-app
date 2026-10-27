# Configurações — Spec

Preferências do aparelho e ferramentas de diagnóstico. Nada aqui fala com o servidor: tudo vive no
`@SettingsPrefs` DataStore, nos caches locais ou no buffer de logs em memória.

As telas formam o `settingsGraph` (`SettingsNavGraph.kt`). A galeria é alcançada só pela porta de core
`GalleryStoragePort` (`core/domain/gallery`), implementada pela galeria: uma feature não importa outra.

---

## 1. Telas

| Tela          | Origem                                  |
|---------------|-----------------------------------------|
| Configurações | Tela inicial (`CoreScreen`)             |
| Galeria       | Configurações → "Galeria" (seção 4)      |
| Logs do app   | Configurações → Diagnóstico → "Logs do app" (seção escondida, 2.6) |

---

## 2. Configurações

Lista em seções, separadas por divisor:

| Seção        | Item              | Comportamento                                                               |
|--------------|-------------------|-----------------------------------------------------------------------------|
| Aparência    | Tema              | Três opções: Sistema / Claro / Escuro (2.2).                                |
| Leitura      | Manter tela acesa | Switch; ligado por padrão. A tela não apaga nas telas de leitura (2.3).     |
| Notificações | Aniversários      | Switch; "Ativado"/"Desativado". Ao ligar, garante o acesso a notificações (2.1). |
| Galeria      | Galeria ›         | "Fotos {x} · Vídeos {y}"; abre Configurações › Galeria (seção 4). |
| Dados        | Apagar Bíblia     | Mostra o espaço ocupado (2.4). Pede confirmação; apaga a Bíblia do aparelho e mostra "Bíblia apagada" (2.5). |
| Diagnóstico  | Logs do app       | Abre a tela de logs. Seção escondida até destravar (2.6).                   |
| Sobre        | Versão            | `versionName` do build. Tocável: 5 toques destravam o Diagnóstico (2.6).    |

### 2.1 Acesso a notificações ao ligar Aniversários

A permissão pedida na primeira abertura costuma ser negada sem querer. Ligar o switch de Aniversários (de
desligado para ligado) é a forma de pedir de novo. A preferência é salva de qualquer jeito; o acesso só decide se
a notificação aparece.

| Situação ao ligar                                                         | Ação                                      |
|---------------------------------------------------------------------------|-------------------------------------------|
| Notificações do app já permitidas                                         | Nada                                      |
| Android 13+, permissão `POST_NOTIFICATIONS` não concedida                 | Pede a permissão pelo diálogo do sistema  |
| O sistema não mostra mais o diálogo (negada duas vezes)                   | Abre as configurações de notificação do app |
| Permissão concedida (ou Android < 13), mas notificações desligadas no sistema | Abre as configurações de notificação do app |

"Não mostra mais o diálogo" = a permissão é negada sem diálogo: o `shouldShowRequestPermissionRationale` é falso
antes e depois do pedido. Se o usuário acabou de negar no diálogo, nada mais acontece — sem abrir configurações
logo em seguida. Desligar o switch nunca pede nada.

### 2.2 Tema

Escolha única entre três botões lado a lado, com o atual marcado:

| Opção   | Efeito                                                          |
|---------|-----------------------------------------------------------------|
| Sistema | Segue o modo claro/escuro do aparelho (padrão de instalação).   |
| Claro   | Sempre claro.                                                   |
| Escuro  | Sempre escuro.                                                  |

Tocar na opção já marcada não faz nada. Outra opção salva a escolha e recria a Activity para aplicar o tema. Na
abertura do app, o tema salvo é aplicado antes da primeira tela.

### 2.3 Manter tela acesa

Enquanto uma tela de leitura está aberta e a preferência está ligada, o aparelho não apaga a tela por inatividade.
Ao sair da tela (ou desligar a preferência), volta ao comportamento normal do sistema.

Telas de leitura: hino (Hinário), capítulo (Bíblia), letra e cifra (Louvor). Listas, formulários e demais telas
seguem o tempo de tela do sistema.

### 2.4 Espaço ocupado

Cada item de Dados mostra quanto ocupa no aparelho o que vai ser apagado (a galeria mostra o seu na linha "Galeria":
vídeos = `gallery/videos/`, fotos = o resto da pasta da galeria):

| Item            | O que é somado                                                              | Texto                              |
|-----------------|-----------------------------------------------------------------------------|------------------------------------|
| Apagar Bíblia   | Arquivos das traduções baixadas                                               | `Apaga a Bíblia do aparelho · 8,4 MB` |

- Calculado fora da thread principal ao abrir a tela e de novo depois de cada "Apagar".
- Enquanto calcula, o texto aparece sem o tamanho.
- Formato: B, KB, MB ou GB (base 1024), uma casa decimal abaixo de 10 e vírgula decimal (`8,4 MB`, `312 MB`).
- Tamanho zero: "nada no aparelho" no lugar do tamanho.
- Depois de apagar vale o texto de conclusão ("Galeria apagada" / "Bíblia apagada").

### 2.5 Apagar não baixa de novo

O download automático no WiFi (galeria depois do login, Bíblia no boot) vale só até a pessoa apagar. Depois de
"Apagar", nada volta sozinho: a galeria e a Bíblia ficam vazias até a pessoa tocar no botão de baixar dentro da
própria feature (galeria: `specs/gallery/spec.md` 3.3; Bíblia: `specs/bible/spec.md` 5). A escolha fica gravada no
aparelho e sobrevive a fechar o app e ao logout.

| Item           | Diálogo                                                                                              |
|----------------|------------------------------------------------------------------------------------------------------|
| Apagar Bíblia  | "Apaga a Bíblia do aparelho. Ela só volta a ser baixada quando você tocar em baixar na Bíblia. Deseja continuar?" |

Botões: "Apagar" e "Cancelar".

### 2.6 Diagnóstico escondido

A seção Diagnóstico (com "Logs do app") não aparece por padrão. Tocar 5 vezes em "Versão" faz a seção aparecer;
toques além do quinto não fazem nada. Sem aviso nem contagem visível.

O destravamento vive só no `SettingsViewModel`: vale enquanto Configurações estiver na pilha (ir aos logs e voltar
mantém a seção) e some ao sair de Configurações ou fechar o app.

---

## 3. Logs do app

Mostra o buffer em memória do `LogBufferTree` (Timber): no máximo 500 entradas, mensagens cortadas em 2000
caracteres, perdido ao fechar o app. Cada linha: `HH:mm:ss.SSS N/tag: mensagem`, em fonte monospace, colorida
pelo nível (W laranja, E/A cor de erro, I verde, demais cor do texto).

- **Filtro de nível:** chips D / I / W / E na ordem; mostra as entradas com nível maior ou igual ao escolhido
  (padrão D).
- **Rolagem:** a lista vai para a última entrada quando o número de entradas filtradas muda.
- **Compartilhar** (TopBar): manda todas as entradas, sem filtro, como texto pelo seletor do sistema.
- **Limpar** (TopBar): esvazia o buffer e a lista.
- **Copiar:** cada linha é selecionável (long press seleciona e abre o menu de copiar do sistema). A seleção
  vale por linha; para levar várias, use "Compartilhar".
- Sem entradas: "Nenhum log".

---

## 4. Configurações › Galeria

`GallerySettingsScreen` / `GallerySettingsViewModel`, rota `settings/gallery`. Espaço para futuras opções da galeria.

| Seção         | Item                           | Comportamento |
|---------------|--------------------------------|---------------|
| Vídeos        | Baixar vídeos automaticamente  | Switch, desligado por padrão (`specs/gallery/spec.md` 11.3). Descrição: desligado "Somente no WiFi · {n} vídeos ({tamanho})"; ligado e faltando "{k} de {n} baixados"; completo "Somente no WiFi · {n} de {n} baixados". |
| Armazenamento | Baixar galeria                 | Diálogo Fotos / Vídeos (galeria 3.5). |
| Armazenamento | Apagar galeria                 | "Fotos {x} · Vídeos {y}". Diálogo "Escolha o que apagar deste aparelho." com Fotos e Vídeos marcados, cada um com o tamanho; "Apagar" desabilitado sem nenhum; só Vídeos avisa "Apagar os vídeos desliga o download automático de vídeos."; os dois avisam que a galeria só volta por "Baixar galeria". Efeito: galeria 3.3 / 3.4. Depois: "Galeria apagada" / "Fotos apagadas" / "Vídeos apagados". |

Os tamanhos são medidos ao abrir e depois de cada ação; a tela principal mede de novo a cada volta (`ON_RESUME`).

