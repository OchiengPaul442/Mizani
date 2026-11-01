# Daily Momentum design

Daily Momentum is a small, local recognition feature for building the habit of recording money.
It is intentionally not a budget score, a spending target, a financial reward, or a behavioral
analytics system. The screen leads with tracked days and streaks because those are the user
behavior being recognized; points are a secondary, playful progress signal.

## Rules

- Each unique calendar day with a non-deleted income, expense, transfer, or refund earns 10 points.
- Adjustments do not earn points because they correct balances rather than represent a daily check-in.
- Future-dated entries are ignored until their local date arrives.
- The current streak is the consecutive run ending today; a day with no entry resets it to zero.
- Milestones are deterministic recognition labels at 3, 7, 30 and 100 tracked days (30, 70, 300
  and 1,000 points). The UI calls these milestones, not “unlocks”, because the current release
  does not promise a reward that does not exist. They do not unlock money, credit, advice, paid
  features, or notifications.

The calculation runs from the encrypted Room snapshot already consumed by Home. No new table,
network call, account, identifier, or notification payload is introduced. Deleting or restoring a
ledger therefore naturally recalculates the visible progress from the resulting source of truth.

## Product guardrails

- Never reward larger amounts, more spending, debt, or repeated duplicate entries.
- Keep the feature private and optional in tone: it should encourage recording, not shame a missed day.
- Make the next action obvious: when today is not tracked, offer a direct path to record a money
  activity; when it is tracked, celebrate completion without asking for another entry.
- Do not add a Daily Momentum notification; the existing generic daily reminder remains separately
  controlled in Settings and remains free to disable.
- Keep milestone copy descriptive and avoid implying financial health or financial advice.

## QA focus

- Verify duplicate entries on one date earn one day of points.
- Verify the UI explains points as a secondary signal and reports milestones in tracked days.
- Verify deleted, adjustment, and future-dated entries do not count.
- Verify streak reset, best streak, milestone thresholds, backup restore recalculation, and delete-all
  data behavior.
- Exercise Home at compact phone width, large font scale, medium tablet width, unfolded foldable
  width, rotation, and split-screen resizing. The card must reflow without horizontal scrolling.
