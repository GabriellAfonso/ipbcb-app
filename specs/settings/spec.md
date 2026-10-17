# Configurações — Spec

Preferências do aparelho e ferramentas de diagnóstico. Nada aqui fala com o servidor: tudo vive no
`@SettingsPrefs` DataStore, nos caches locais ou no buffer de logs em memória.

---

## 1. Telas

| Tela          | Origem                                  |
|---------------|-----------------------------------------|
| Configurações | Tela inicial (`CoreScreen`)             |
| Logs do app   | Configurações → Diagnóstico → "Logs do app" |

---

## 2. Configurações

Lista em seções, separadas por divisor:

| Seção        | Item              | Comportamento                                                               |
|--------------|-------------------|-----------------------------------------------------------------------------|
| Aparência    | Tema              | Três opções: Sistema / Claro / Escuro (2.2).                                |
| Leitura      | Manter tela acesa | Switch; ligado por padrão. A tela não apaga nas telas de leitura (2.3).     |
| Notificações | Aniversários      | Switch; "Ativado"/"Desativado". Ao ligar, garante o acesso a notificações (2.1). |
| Dados        | Resetar galeria   | Pede confirmação; apaga as fotos em cache e mostra "Cache apagado".         |
| Dados        | Resetar Bíblia    | Pede confirmação; apaga a Bíblia local e mostra "Baixando novamente…".      |
| Diagnóstico  | Logs do app       | Abre a tela de logs.                                                        |
| Sobre        | Versão            | `versionName` do build, só leitura.                                         |

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
