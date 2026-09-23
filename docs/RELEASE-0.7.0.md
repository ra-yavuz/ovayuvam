# ovayuvam 0.7.0

Android package: `tr.ovayuva.ovayuvam`. Version code: `24`.

## Changes

Open the info sheet and choose **Watch your world grow** to replay first
discoveries on the real map. Choose Week, Month, Year, or All; pause, restart,
or move the date slider. Earlier discoveries remain visible at the start of
a shorter period. Close the replay or press Back to return to the live map.
The controls adapt to landscape and larger text.

Replay reads a snapshot of saved discoveries. It does not erase history or
stop background tracking. It is not an exact route recording or a reconstruction
of historical visit colors. Data without a known discovery date remains a
baseline; future-dated entries are excluded from the snapshot.

The ongoing tracking notification now has 83 short messages. Its text rotates
about every 90 minutes while tracking runs, without sound, vibration, or a new
notification. Evening progress summaries remain. No wake-up alarms or separate
reminders are added; Android may delay rotation while the phone sleeps.

Reveal geometry, visit counting, permissions, backup format, and storage schema
are unchanged. Install over the existing app without uninstalling or clearing
its data to keep your world, visit counts, goal, and settings.

## Verification

- All 42 release unit tests passed. Release lint completed with zero errors and
  13 existing warnings.
- The signed APK upgraded from 0.6.1 on Android 13. All 10,201 reveal rows,
  1,681 visited rows, the saved goal, and a seeded 100-visit record with its stay
  state were preserved.
- Isolated device checks passed for stay movement, restart, departure and return,
  backup merging, large history, geographic queries, and silent notification
  replacement using the same notification ID.
- Replay image checks show increasing revealed area over a loaded real map
  between the first, middle, and final frames.
- Signed-app checks passed for play, pause, restart, seeking, all four periods,
  landscape, 150% text size, Close and Back. All saved map and visit rows remained
  unchanged after replay. The separate empty-world check also passed.
- The signed foreground service ran with synthetic GPS, kept notification ID
  1001 and silent flags, and removed its notification when stopped.
- Candidate website checks passed in English, German, Turkish, and French at
  phone and desktop sizes, including download consent and the new APK link.
- APK and bundle signing checks and bundle validation passed. The existing app
  signing certificate was retained.
- All 7 Play upload-helper tests passed, and the workflow configuration parsed.
  No Google Play upload has been verified.

These checks use an emulator and synthetic history. They do not prove physical
phone battery use, field GPS accuracy, manufacturer-specific background behavior,
Google Play policy compliance, or Play approval.
A thermally paused emulator run hit a graphics-renderer timeout. The signed
replay checks passed on rerun, but phone performance still needs field testing.

## Distribution

This is a tester release outside Google Play. Download links use the existing
testing flow at [the product website](https://ovayuvam.ovayuva.tr/).
The GitHub release contains source only, not APK or bundle assets.

An optional [internal-release workflow](PLAY-AUTOMATION.md) is prepared but
disabled until dedicated credentials and permissions are configured. It targets
internal testing only; production publishing remains manual.

## Artifact Identity

- APK: `ovayuvam-0.7.0.apk`, 44,855,447 bytes.
- APK SHA-256: `6e24abfe7d93e562fecbc1f5f7e4c32de8ffff67ac2bac6d6bd353a6a8924b42`.
- App bundle: `ovayuvam-0.7.0.aab`, 18,816,744 bytes.
- Bundle SHA-256: `92792a87c3d76ebe075d318d0e61ba6a35cfe7df77398cbed025fc61f1917c5f`.
- Signing certificate SHA-256:
  `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`.
