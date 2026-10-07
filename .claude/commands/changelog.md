---
description: Gera um changelog em pt-BR agrupado por área, salva em changelogs/vX.Y.Z.md e exibe versão resumida para Play Store (≤500 chars). Use: /changelog 0.9.6 0.9.7
allowed-tools: Bash(bash .claude/scripts/changelog-range.sh:*), Bash(git log:*), Bash(git tag --list:*), Bash(git rev-parse:*), Bash(grep:*), Bash(sed:*), Bash(mkdir:*), Bash(tee:*), Bash(echo:*)
---

## Contexto

- Versão atual do app: !`grep 'versionName' app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/'`
- Tags de versão: !`T=$(git tag --list 'v*' --sort=-version:refname | head -10); echo "${T:-(nenhuma tag)}"`
- Range resolvido: !`bash .claude/scripts/changelog-range.sh $ARGUMENTS`
- Commits do range: !`R=$(bash .claude/scripts/changelog-range.sh $ARGUMENTS) && git log --oneline --no-merges "$R" || echo "$R"`
- Log completo com corpo: !`R=$(bash .claude/scripts/changelog-range.sh $ARGUMENTS) && git log --pretty=format:"%s|%b" --no-merges "$R" || echo "$R"`

## Sua tarefa

Gere um changelog completo e salve-o em arquivo. Siga os passos abaixo na ordem.

---

### Passo 1 — Identificar a versão alvo

O range é resolvido por **tag de versão** (`.claude/scripts/changelog-range.sh`). Cada release tem uma tag
`vX.Y.Z`, criada pelo `/release` no commit do changelog, logo depois do bump. Se a tag não existir, o resolvedor
cai no commit `chore(release): bump version to X.Y.Z`.

Dentro do `/release` a tag da versão alvo ainda não existe: o range termina no bump, que é o `HEAD`. Nesse caso,
escreva no header `v<anterior>..vX.Y.Z`, porque a tag vai para o commit deste changelog.

Formas aceitas em `$ARGUMENTS` (com ou sem `v`):

- `0.9.6 0.9.7` — da tag `v0.9.6` (exclusiva) até a `v0.9.7` (inclusiva), ou até o bump da `0.9.7` se a tag
  ainda não existe. **Versão alvo: `0.9.7`**. É a forma que o `/release` usa.
- `0.9.6` — da tag `v0.9.6` até `HEAD`. Versão alvo: o `versionName` atual
- `<ref>..<ref>` — range literal do git, usado como veio. Versão alvo: o `versionName` atual
- sem argumento — da tag mais recente até `HEAD`. Se a tag mais recente já está no `HEAD` (logo depois do
  `/release`), usa da penúltima até ela. Versão alvo: a versão da tag final, ou o `versionName` atual quando o
  range termina em `HEAD`

Confira o bloco **Range resolvido** antes de gerar. Se ele começar com `UNRESOLVED`, **pare e avise** o motivo —
não gere nada. Se o range vier vazio (nenhum commit), avise também.

Este comando nunca cria, move ou apaga tags nem faz commit — isso é feito pelo `/release`.

---

### Passo 2 — Mapeamento de escopos → seções

| Escopos dos commits                                  | Seção no changelog           |
|------------------------------------------------------|------------------------------|
| `hymnal`, `hinario`                                  | **Hinário**                  |
| `chord`, `cifra`, `lyrics`, `letra`                  | **Cifras e Letras**          |
| `worshiphub`, `repertorio`, `setlist`                | **Repertório**               |
| `gallery`, `galeria`                                 | **Galeria**                  |
| `bible`, `biblia`                                    | **Bíblia**                   |
| `schedule`, `agenda`                                 | **Agenda**                   |
| `studies`, `estudos`                                 | **Estudos**                  |
| `members`, `birthdays`                               | **Membros e aniversariantes** |
| `auth`, `profile`, `settings`, `home`, `theme`, `notifications`, `access` | **Geral**   |
| Sem escopo reconhecível                              | **Geral**                    |

> **Escopos transversais — classifique pelo body, não pelo escopo:** `core`, `admin`, `push` e `reports` mexem em
> código compartilhado ou no painel, mas quase sempre afetam uma feature específica. Leia o body e coloque na seção
> da feature. Ex.: `feat(push): distribute the sunday setlist` → **Repertório**; `feat(reports): make hymnal
> configuration owner-only` → **Hinário**; `feat(core): ...setlist...` → **Repertório**. Só use **Geral** quando o
> commit for sobre o app como um todo (login, navegação, tema, permissões) ou sobre o painel admin em si.

> **Sempre omitidos:** `build`, `deps`, `specs`, `speckit`, `claude`, `changelog`, `release`, `test` — exceto se o
> body descrever algo que o usuário final percebe.

---

### Passo 3 — Gerar o changelog completo (sem limite de caracteres)

Formato do arquivo `.md`:

~~~
# Changelog — v<versão alvo>

> Gerado em: <data atual, dd/MM/yyyy>
> Commits: `<range resolvido>` (`<sha curto inicial>..<sha curto final>`, <N> commits)

## <Seção com mais mudanças>
- <Descrição humana, pt-BR — pode ser detalhada>
- ...

## <Próxima seção>
- ...

## Geral
- ...

---

## 🏪 Play Store (≤ 500 caracteres)

<versão alvo>

  <Seção 1>
  - <item resumido>

  <Seção 2>
  - <item resumido>

  Geral
  - <item resumido>

`(XXX/500 caracteres)`
~~~

---

### Passo 4 — Salvar o arquivo

O arquivo é sempre `changelogs/v<versão alvo>.md` (com `v`, como os existentes):

```bash
mkdir -p changelogs && tee changelogs/v<versão-alvo>.md << 'EOF'
<conteúdo gerado>
EOF
```

Confirme a criação com: `echo "✅ Salvo em changelogs/v<versão-alvo>.md"`

---

### Regras gerais

1. **Leia o body dos commits** — o título sozinho não basta. Use o body (disponível no log completo acima) para
   entender *o que* a mudança faz e *onde* ela se aplica. Não assuma a seção só pelo escopo do título.
2. **Traduza e humanize** — não copie mensagens de commit cruas. `feat(hymnal): add vertical scroll mode` →
   `Novo modo de rolagem vertical`.
3. **Agrupe semanticamente** — múltiplos commits da mesma funcionalidade viram uma linha.
4. **Omita** chore, test, docs, bump de versão e refactors internos — só o que o usuário final percebe. Mudanças
   só do build de debug também ficam de fora.
5. **Ordem das seções:** mais mudanças primeiro; "Geral" sempre por último.
6. **Sem seções vazias** — omita seções sem mudanças visíveis ao usuário.
7. **Versão Play Store:** máximo **500 caracteres** (incluindo espaços). Se não couber tudo, priorize as mudanças
   mais impactantes. Informe a contagem ao final: `(XXX/500 caracteres)`.
8. O bloco Play Store deve ser a **última seção do arquivo**, separada por `---`, para fácil localização e cópia.
