# Project structure

## Module decision

V1 is a **single `:app` Gradle module** with enforced package boundaries. The PRD explicitly warns that
"avoid hundreds of Gradle modules in the first release" and that modularization should protect domains without
creating build-system bureaucracy. For a one-module, one-team V1, packages give the same dependency discipline
with faster builds and fewer configuration risks (AGP 9 + KSP). The boundary rules below are designed so that
`core`, `data`, `domain` and `feature` can be extracted into modules later without rewrites.

## Package layout

```
com.mizani.money
├── MizaniApplication.kt           application + SQLCipher native load + container
├── MainActivity.kt                FragmentActivity, theme, root routing, lock lifecycle
├── di/AppContainer.kt             explicit dependency graph (manual DI)
├── core/
│   ├── money/                     Money, Currency, parsing/formatting (pure JVM)
│   ├── model/                     domain enums, country packs, default categories
│   ├── time/                      DateRange, periods, recurring date math
│   └── security/                  PBKDF2-HMAC-SHA256, PIN policy
├── data/
│   ├── local/                     Room entities, DAOs, encrypted database, Keystore key
│   └── repo/                      FinanceRepository (write boundary), FinanceModels.kt
│                                  (LedgerSnapshot + screen/plan state), SettingsRepository,
│                                  BackupValidator.kt (restore validation)
├── domain/
│   ├── analytics/                 Analytics.kt + Insights.kt engines,
│   │                              AnalyticsModels.kt (metric/insight models)
│   └── export/                    CSV + encrypted backup/restore
├── ui/
│   ├── AppViewModel.kt            root state, period selection, lock state
│   ├── Navigation.kt              top-level destinations and bottom bar
│   ├── theme/                     colour, type, shapes, palette tokens
│   └── components/                reusable Compose primitives (cards, charts, pickers)
└── feature/
    ├── onboarding/ home/ transactions/ wallets/ more/
    ├── plan/ budget/ categories/ goals/ debts/
    ├── insights/ settings/ account/ lock/
```

## Dependency direction

```
feature / ui  ->  domain  ->  data  ->  core
                     \--------------------^
```

- `core.money` and `core.time` have **no Android or Compose dependency** and are unit-tested on the JVM.
- `domain.analytics` consumes plain entities/models and has **no Android UI dependency**; it is deterministic.
- `data.local` depends on `core.model` and Room/SQLCipher; it does not know about screens.
- `data.repo` is the only layer that opens write transactions. `FinanceRepository` is the single validated write
  boundary so balances, budgets, goals, bills and debts never partially apply.
- `ui`/`feature` read through `AppViewModel`/repositories and never touch DAOs or SQLite directly.
- `feature` packages are package-by-feature; each owns its screen, local state and dialogs.

## State and data flow

```
Room (SQLCipher) --Flow--> FinanceRepository --Flow--> AppViewModel --StateFlow--> Compose
        ^                                                     |
        └──────────── validated suspend mutations <───────────┘
```

The local database is the canonical source of truth. There is no network path in this build. `LedgerSnapshot`
combines the small set of Room flows once, so screens derive analytics from a consistent in-memory view and a
write is reflected immediately.

## Where things belong

| Change | Location |
|---|---|
| Money arithmetic/format | `core/money/Money.kt` + `MoneyTest` |
| New transaction type | `core/model/TransactionType`, repository validation, add-screen picker, analytics rules, tests |
| New analytic metric | `domain/analytics`, an explanation string, unit test, UI card |
| New database column/table | `data/local` entity + DAO + versioned migration test; never destructive migration |
| New screen | `feature/<name>` + route in `ui/Navigation.kt` |
| Shared visual element | `ui/components` once it has 2+ call sites |
| Preference | `SettingsRepository` (DataStore), never in the encrypted ledger |

## Testing layout

```
app/src/test/          pure JVM: money, analytics, insights, dates, PIN hashing
app/src/androidTest/   encrypted Room/SQLCipher CRUD, guest journey, app lock, backup/restore
app/schemas/           exported Room schema (checked in for migration tests)
```

## Evolution path

If the team grows or an iOS/KMP requirement appears, extract in this order:
1. `core:money`, `core:model`, `core:time` (pure Kotlin);
2. `core:database` (Room/SQLCipher/Keystore);
3. `domain:analytics`, `domain:export`;
4. feature modules (`feature:home`, `feature:transactions`, …).

Keep `FinanceRepository` as the write boundary across modules. Do not split graded modules merely for aesthetics.
