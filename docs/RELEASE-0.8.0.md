# ovayuvam 0.8.0

Internal tester release. Production review remains a separate task.

## Changes

- Optional Weekly exploration switch in settings, off by default.
- One Explore here suggestion per seven days, within 500 metres of both a
  recent resting area and the current location. Rest requires an hour within
  60 metres with accurate, fresh location fixes; gaps longer than five minutes
  reset the timer.
- Suggestions use local map road/path data and are just beyond the existing
  revealed brush. Motorways, railways, private/no-access ways, service roads,
  cycle-only ways, and tunnels are not eligible. Map tags are not a guarantee
  of actual access; follow local signs.
- Suggestions expire after three days, disappear when reached within 25 metres
  or when the user moves more than 500 metres away, and do not trigger extra
  sound or vibration. The existing ongoing notification carries the invitation.
- Long-press goal creation is removed. Legacy goal data remains in old backups
  for compatibility, but does not become a weekly suggestion.
- Settings show the installed version and build number beside backup controls.
- Existing replay, fog tile caching, encrypted backups, and saved exploration
  remain supported. No database migration or automatic deletion of history.

## Validation

- All 73 unit tests passed; release lint reported no errors.
- Android checks passed for opt-in, rest detection, stale fixes, arrival,
  expiry, the weekly limit, restart, and silent notification replacement.
- A real-map emulator check selected an unrevealed mapped street 43 metres
  away. Settings displayed Version 0.8.0 (28), and long-press created no goal.
- The signed update from 0.7.3 preserved all 10,201 reveal rows, 1,681 legacy
  visited rows, visit counts, and stay records. No database migration is used.
- APK and bundle signing certificates match the previous release. Google Play
  reports no loss of device support. Its native debug-symbol warning remains.
- These are automated and emulator results. Real-world path access and
  physical-phone background location behaviour still need tester feedback.
