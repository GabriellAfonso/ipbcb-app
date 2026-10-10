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
| Aparência    | Modo escuro       | Switch; "Ativado"/"Desativado". Troca o tema recriando a Activity.          |
| Notificações | Aniversários      | Switch; "Ativado"/"Desativado".                                             |
| Dados        | Resetar galeria   | Pede confirmação; apaga as fotos em cache e mostra "Cache apagado".         |
| Dados        | Resetar Bíblia    | Pede confirmação; apaga a Bíblia local e mostra "Baixando novamente…".      |
| Diagnóstico  | Logs do app       | Abre a tela de logs.                                                        |
| Sobre        | Versão            | `versionName` do build, só leitura.                                         |

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
