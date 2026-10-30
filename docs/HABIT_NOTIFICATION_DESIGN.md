# Local money check-ins

## Decision

Mizani uses an opt-in, on-device check-in engine rather than a server-side engagement or
profiling service. The encrypted Room snapshot is read by the existing WorkManager reminder
pipeline; no transaction, balance, investment, debt or habit signal is sent over the network.

The master switch is off by default. Each sensitive category is also off by default and must be
selected separately in **Settings → Reminders**. This makes the feature useful without turning a
single engagement choice into consent for debt, income or investment prompts.

## Available check-ins

The engine emits at most one category, and the notification surface intentionally uses the same
neutral copy for every category:

> Mizani money check-in — Open Mizani for a private money review.

The category is not included in the title, body, `PendingIntent`, logs or persisted preference
metadata. After opening the app, the user can review the relevant screen while the normal app lock
continues to protect financial details.

Current descriptive signals are deliberately conservative:

| Category | Local condition | Guardrail |
| --- | --- | --- |
| Spending | At least three positive expense entries in the inclusive seven-day window, checked on Monday | No amount, category or merchant is exposed |
| Expected income | A user-recorded recurring income rule is due from one day ago through two days ahead | Expected income is never treated as received income |
| Investments | An active manual holding has a valuation date at least 30 days old | This is a valuation refresh prompt, not market or trading advice |
| Debt | An active debt with a positive balance and payment amount is overdue or due within three days | No creditor, balance, rate or amount is exposed |
| Savings goals | An unfinished active goal has a target date within 14 days or is overdue | No goal name, target or progress is exposed |

A generic six-day cooldown limits habit check-ins to at most one per local week. Existing daily
transaction and user-configured commitment reminders remain separate, and the shared daily worker
will not post both a habit check-in and a daily check-in in the same run.

## Delivery and lifecycle

- WorkManager schedules one unique, wall-clock-aligned one-time check at the existing reminder time.
  Android may delay it under Doze, battery restrictions or OEM policy; Mizani does not request
  exact-alarm access for this non-critical feature.
- WorkManager input contains only the schedule generation, local date and reminder minute.
- The notification channel is user-controlled, uses private lock-screen visibility, and requires
  the Android notification permission on supported versions.
- The worker checks the latest settings after reading the encrypted snapshot. It posts only when
  the master switch and selected category are still enabled.
- The notification uses one stable ID, so repeated worker execution updates one notification
  instead of accumulating a list of financial alerts.
- Deleting local data or replacing it from a backup cancels scheduled daily, habit and commitment
  work and clears every Mizani notification before schedules are rebuilt from the current settings
  and restored ledger.
- DataStore stores only category choices, the master switch and a generic last-check-in date. It
  does not store which financial category triggered the last notification.

## Safety boundaries

Check-ins are descriptive reminders, not financial advice, credit decisions, debt collection,
investment recommendations or predictions. There is no market feed, account connection, cloud
sync, advertising SDK or behavioral analytics pipeline. Notification copy is intentionally less
useful while the device is locked so another person cannot infer that the user owns debt,
investments or expected income.

## Research basis

- Android recommends waiting for users to understand the value of notifications before requesting
  `POST_NOTIFICATIONS`, and gives users channel-level control: [notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
  and [notification channels](https://developer.android.com/develop/ui/views/notifications/channels).
- Android documents that WorkManager is persistent but best-effort: timing depends on constraints
  and system optimizations, and unique work prevents accidental duplicate scheduling: [define work
  requests](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
  and [manage work](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/manage-work).
- A large randomized savings-reminder study found that messages tailored to recent behavior
  outperformed a generic message in that setting, while also reinforcing that reminder content and
  cadence need to be evaluated rather than assumed: [two-million-person megastudy](https://pmc.ncbi.nlm.nih.gov/articles/PMC12477529/).

These sources support user control, conservative cadence and behavior-aware descriptive prompts;
they do not establish that any particular Mizani threshold will improve retention or financial
outcomes. Those outcomes must be measured only with a future, separately approved privacy design.

## Verification matrix

- JVM tests cover each signal, priority ordering, archived/disabled records and the weekly
  cooldown.
- Disposable API 36 emulator tests cover Settings placement and explicit category opt-in, real
  WorkManager worker execution, private notification visibility, neutral copy, absence of a debt
  name/amount in notification extras, and repeated-worker suppression.
- Release follow-up should still include API 26/33/current devices, denied or revoked notification
  permission, blocked channel, lock-screen collapsed/expanded views, reboot/time-zone changes,
  battery restrictions, process death around notification posting, delete/restore with pending
  work, TalkBack, and large font sizes.
