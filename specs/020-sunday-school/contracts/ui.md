# UI contract: Sunday School

## Routes (`studiesGraph`)

| Route | Screen | Args |
|---|---|---|
| `StudiesRoutes.SundaySchool` = `SundaySchool` | year tabs + lesson list | — |
| `StudiesRoutes.SundaySchoolLesson` = `SundaySchoolLesson/{lessonId}` | reader | `lessonId: Int` |
| `AppRoutes.SUNDAY_SCHOOL_MANAGE` = `SundaySchoolManage` | panel list | — |
| `StudiesRoutes.SundaySchoolLessonForm` = `SundaySchoolLessonForm?lessonId={lessonId}&year={year}` | form | `lessonId` (-1 = new), `year` |
| `StudiesRoutes.SundaySchoolYears` = `SundaySchoolYears` | "Anos da EBD" | — |

The panel card `PanelCard.SUNDAY_SCHOOL` (`AtLeast(Scope.SUNDAY_SCHOOL, VIEW)`, label "EBD", description
"Aulas da Escola Bíblica Dominical") calls `AdminNav.sundaySchool` → `navigate(AppRoutes.SUNDAY_SCHOOL_MANAGE)`.

## Copy (pt-BR)

- Hub card: "Escola Bíblica Dominical" / "Aulas por ano".
- Empty: "Nenhuma aula publicada ainda". Gone: "Esta aula não está mais disponível".
- Lesson row: title; second line "16/08/2026 · Prof. Fulano" with each part only when known.
- Panel: "Rascunho" badge, FAB "Nova aula", menu "Anos da EBD", delete dialog "Apagar a aula “X”?" / "Apagar" /
  "Cancelar". Form: "Título", "Ano", "Data", "Professor", "Status" (Rascunho/Publicado), tabs "Editar" /
  "Pré-visualizar", "Salvar", "Adicionar seção", "Adicionar parágrafo / lista / versículo-chave", "Adicionar
  referência", "Descartar alterações?". Non-editable block: caption "Este bloco só pode ser movido ou apagado".
- Years: "Anos da EBD", "N aulas · M publicadas", dialog "Tema de 2026" with "Salvar" / "Remover tema".
