# Quickstart: Monthly Birthdays Highlight Card

## Prerequisites

- Android SDK installed (`sdk.dir` in `local.properties`)
- Valid JWT auth token (login via app)
- API server running at `https://gabrielafonso.com.br/ipbcb/`

## Build & Run

```bash
./gradlew :app:assembleDebug
# Install on device/emulator and launch
```

## Validation Scenarios

### 1. Birthdays appear on home screen

1. Launch app (logged in)
2. Home screen loads → highlight carousel shows birthday card
3. Card displays "Aniversariantes do Mês" title with list of names and days
4. Names sorted by day ascending

**Expected**: Birthday card shows current month's birthdays from API.

### 2. Empty month fallback

1. Launch app during a month with no registered birthdays
2. Birthday card shows placeholder: cake emoji + "Nenhum aniversariante esse mês"

**Expected**: Graceful empty state, no error shown.

### 3. Offline access

1. Launch app with internet → birthdays load
2. Kill app, enable airplane mode, relaunch
3. Birthday card shows cached data from previous load

**Expected**: Cached birthdays displayed without network.

### 4. Month transition

1. Load birthdays in June
2. Change device date to July (or wait for month change)
3. Relaunch app
4. New month's birthdays fetched

**Expected**: Fresh data for new month, old cache unused.

## Run Tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.domain.usecase.GetMonthlyBirthdaysUseCaseTest"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.data.repository.MembersRepositoryImplTest"
```

## Key Files

- Data model: [data-model.md](data-model.md)
- API contract: [contracts/birthdays-api.md](contracts/birthdays-api.md)
- Spec: [spec.md](spec.md)
