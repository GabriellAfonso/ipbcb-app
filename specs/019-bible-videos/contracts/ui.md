# UI contract

## Reader toolbar
`[←] [Livro] [Cap] [Tradução] ……… [▶] [Aa]` — ▶ (`Icons.Filled.SmartDisplay`, white) when the chapter/book has
videos, or always for managers.

## Videos sheet (`ModalBottomSheet`)
- Section header: "Gênesis 1" / "Gênesis" + "Reordenar" (managers, ≥ 2 videos).
- Card: thumbnail 16:9 (rounded 8 dp; placeholder with play icon), "Playlist" badge over the thumbnail, "Visto"
  badge and 50 % alpha when watched; title `titleSmall`, 2 lines; watched check (`CheckCircle` / outline) for
  signed-in; ⋮ for managers: "Editar locais", "Remover deste capítulo|livro", "Excluir vídeo".
- Footer for managers: "+ Adicionar link". Empty for managers: "Nenhum vídeo aqui ainda." + button.
- Add dialog: "Adicionar vídeo", field "Link do YouTube", "Cancelar" / "Adicionar" (progress while sending),
  error text under the field.
- Reorder mode: only that section, drag handles, "Concluir".
- Confirm dialogs: "Excluir vídeo?" — "O vídeo sai de todos os lugares onde aparece." / "Excluir".

## "Locais do vídeo" screen (`BaseScreen`)
- Header: thumbnail + title. List of places "Gênesis 1", "Êxodo (livro inteiro)" with ✕. "+ Local". Top bar
  action "Salvar" (enabled when changed). Leaving with changes discards them.
- Picker (full screen inside the route): book list with search → chapter grid (multi-select, 5 columns) + chip
  "Livro inteiro" → "Adicionar (n)".
- Save with empty list: "Excluir vídeo?" confirmation.

## Reader end of chapter
After the last verse: `OutlinedButton` "Marcar como lido" / `FilledTonalButton` "Lido ✓" (signed-in only).

## Index
- Book row: right side "12/50" (signed-in) instead of "50 cap."; complete → "50/50" + check icon in primary.
- Long-press row (signed-in): dialog "Marcar os 50 capítulos de Gênesis como lidos?" / "Desmarcar os 50 capítulos
  de Gênesis?" with "Marcar" / "Desmarcar" and "Cancelar".
- Chapter grid: read cells with `primaryContainer` background and a small check.
