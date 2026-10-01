# API Contract: Monthly Birthdays

## GET /ipbcb/members/birthdays/

Fetches birthdays for a month or a month range. The app always asks for the whole year (`month=1-12`).

### Request

```
GET /ipbcb/members/birthdays/?month=1-12
Authorization: Bearer {jwt_token}
```

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| month     | String | Yes    | `M` (single month) or `M-M` (inclusive range), 1 = January, 12 = December |

### Response: 200 OK (with birthdays)

```json
{
  "birthdays": [
    { "name": "Alice", "gender": "F", "birth_month": 1, "birth_day": 5 },
    { "name": "Bob", "gender": "M", "birth_month": 1, "birth_day": 12 },
    { "name": "Carlos", "gender": null, "birth_month": 7, "birth_day": 25 }
  ]
}
```

### Response: 200 OK (no birthdays in the range)

```json
{
  "birthdays": []
}
```

### Response: 401 Unauthorized

User not authenticated or token expired. Handled by `TokenAuthenticator` (auto-refresh) and `BaseSnapshotRepository` (emits `SnapshotState.Error`).

### Notes

- Results are ordered by `birth_month`, then `birth_day`, ascending
- No pagination (a year of birthdays is a few hundred rows at most)
- `400` when `month` is malformed or out of range
- No ETag support
