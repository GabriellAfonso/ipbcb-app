# Contract (app side): Events API usage

The backend contract is the source of truth: `backend/specs/020-church-events/contracts/events-api.md`. This file
records only how the app calls it.

## Retrofit interfaces

| Interface | Qualifier | Area |
|-----------|-----------|------|
| `EventsApi` | `@AuthedRetrofit` | `features/events/data/api` |
| `EventsAdminApi` | `@AuthedRetrofit` | `features/admin/events/data/api` |

`@AuthedRetrofit` is used for the public list too, so a member's token travels when present (members-only events).
`AuthInterceptor` leaves the request without a token when there is no session.

```kotlin
// EventsApi
@GET("api/events/")
suspend fun getEvents(@Header("If-None-Match") ifNoneMatch: String? = null): Response<List<EventPublicDto>>

// EventsAdminApi
@GET("api/admin/events/")
suspend fun getEvents(@Query("status") status: String): Response<EventListDto>

@GET("api/admin/events/{id}/")
suspend fun getEvent(@Path("id") id: Int): Response<EventRecordDto>

@Multipart @POST("api/admin/events/")
suspend fun createEvent(
    @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    @Part image: MultipartBody.Part,
): Response<EventRecordDto>

@Multipart @PATCH("api/admin/events/{id}/")
suspend fun updateEvent(
    @Path("id") id: Int,
    @PartMap fields: Map<String, @JvmSuppressWildcards RequestBody>,
    @Part image: MultipartBody.Part?,
): Response<EventRecordDto>

@DELETE("api/admin/events/{id}/")
suspend fun deleteEvent(@Path("id") id: Int): Response<Unit>
```

Paths are constants in an `...Endpoints` object, per the project's no-magic-strings rule.

## Status → app reaction

| Answer | Public list | Management read | Management write |
|--------|-------------|-----------------|------------------|
| 200/201/204 | update the snapshot | show | `Saved` / `Deleted` |
| 304 | keep the snapshot | — | — |
| 400 `VALIDATION_ERROR` | — | (`status` cannot be invalid) | field errors under the fields |
| 401 | the authenticator renews; on failure the session ends (R4) | same | same |
| 403 `PERMISSION_DENIED` | — | leave the area with a message | message, stay on the form |
| 404 | — | back to the list, "Este evento não existe mais." | same |
| network / 5xx | keep the saved list; nothing shown in the carousel | error state with retry | message, keep the form |

## Image upload

- Part name: `image`. Content type: `image/jpeg`; the uCrop output is JPEG.
- File name: `event.jpg`.

## UI entry points

| Route | Where | Screen |
|-------|-------|--------|
| `AppRoutes.EVENT_DETAIL` = `"events/{eventId}"` | inline `composable` in `AppNavHost` | `EventDetailScreen` |
| `EventsAdminRoutes.GRAPH` = `"graph/admin/events"` | nested in `adminGraph` | list → form |
| `EventsAdminRoutes.LIST` | | `EventsAdminListScreen` |
| `EventsAdminRoutes.FORM` = `"EventsAdminForm?eventId={eventId}"` | | `EventFormScreen` (new / edit / read-only) |
