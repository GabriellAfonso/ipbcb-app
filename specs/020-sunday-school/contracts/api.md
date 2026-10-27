# API used by the app: Sunday School

Owner: backend, `backend/specs/026-sunday-school/contracts/api.md` (source of truth). Listed here is only what the
app calls and how.

## Public — `@AuthLessRetrofit`, `If-None-Match` → `304`

| Call | Snapshot key |
|---|---|
| `GET api/sunday-school/` | `sunday_school_index` |
| `GET api/sunday-school/?year=N` | `sunday_school_index_N` |
| `GET api/sunday-school/lessons/{id}/` | `sunday_school_lesson_{id}` |

404 `SUNDAY_SCHOOL_YEAR_NOT_FOUND` / `SUNDAY_SCHOOL_LESSON_NOT_FOUND` → snapshot cleared, error with app copy
(research R-03).

## Panel — `@AuthedRetrofit`, nothing stored

| Call | Use | Level |
|---|---|---|
| `GET api/sunday-school/manage/years/` | year selector, "Anos da EBD" | view |
| `GET api/sunday-school/manage/lessons/?year=N` | panel list | view |
| `GET api/sunday-school/manage/lessons/{id}/` | open the form | view |
| `POST api/sunday-school/manage/lessons/` | create | manage |
| `PATCH api/sunday-school/manage/lessons/{id}/` | save (all form fields sent) | manage |
| `DELETE api/sunday-school/manage/lessons/{id}/` | delete | owner |
| `PUT api/sunday-school/manage/lessons/order/` `{year, lesson_ids}` | reorder | manage |
| `PATCH api/sunday-school/manage/years/{year}/` `{theme}` | theme | manage |

Write bodies are built with `buildJsonObject` so `date: null` (clear) is sent despite `explicitNulls = false`.
`content` is encoded by `LessonContentCodec` (research R-02).

## Errors mapped by the app

| `error_code` | Screen behaviour |
|---|---|
| `SUNDAY_SCHOOL_CONTENT_INVALID` | `extras.errors[{path, message}]` → next to the field (R-10) |
| `SUNDAY_SCHOOL_ORDER_MISMATCH` | reload list + "A lista mudou; tente de novo" |
| `SUNDAY_SCHOOL_LESSON_NOT_FOUND` (panel) | "Esta aula foi apagada" + back to list |
| `VALIDATION_ERROR` | `field_errors` on `title` / `year` / `date` / `teacher` |
| 401 / 403 | generic auth texts (constitution) |
