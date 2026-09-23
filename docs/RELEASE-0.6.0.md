# ovayuvam 0.6.0

Android package: `tr.ovayuva.ovayuvam`. Version code: `22`.

## Changes

- Explored places gain a light visit-frequency tint when zoomed out. Maximum
  opacity is 24%; the tint fades away between zoom levels 10 and 12. It stays
  inside fully revealed core areas and does not enlarge the fog-clearing radius.
- Local stay areas group movement around a home or garden. The default radius
  is 150 meters, adjustable up to 1,000 meters. A confirmed departure and return
  are required before counting another visit.
- Visit detection filters uncertain GPS readings, duplicate fixes, observation
  gaps, short trips, and restarts. Stationary fixes can confirm a stay without
  repeatedly repainting the saved trail.
- The map loads history by visible area, including old places beyond the former
  8,000-cell display limit. Query padding respects ground distances at high latitudes.
- Encrypted backups include visit counts. Older backups remain readable;
  importing the same backup repeatedly does not add counts.
- The existing black folded-map launcher update is included.

## Existing Worlds

Install over the existing app. Do not uninstall or clear app data first. Database
migration 3 only adds visit tables; existing visited cells, reveal cells, goal,
and settings remain in place. Historical GPS sample counts are not converted
into claimed visits. Visit measurement starts after updating.

## Verification

- 28 release unit tests passed, covering visit decisions, backup compatibility,
  heat opacity, map coordinates, notification text, and launcher resources.
- Release lint completed with zero errors and 13 warnings. The warnings concern
  dependency versions, existing resource/API cleanup, and preference API style.
- The signed APK upgraded from 0.5.11 on Android 13 while retaining all 10,201
  synthetic reveal rows, all 1,681 visited rows, and the saved goal unchanged.
- Device repository checks use the separate `.verification` package. They cover
  house movement, departure/return, persisted state, repeated imports, history
  over 8,000 cells, date-line queries, and high-latitude brush edges.
- The signed release APK recorded one visit from simulated stationary fixes
  with small position changes and kept that count after restarting the app.
- Emulator image comparisons found tint changes only inside the explored area
  at zoom 11, and no pixel differences with heat enabled or disabled at zoom
  15.6. Streets and labels remained readable in the inspected images.
- Website browser checks cover English, German, Turkish, and French at phone
  and desktop sizes, including the existing download consent step.

GPS thresholds are conservative estimates. Physical-phone walks, battery
behavior across manufacturers, and Google Play approval are not established by
these tests. This is a tester release outside Google Play. Downloads are shared
through the existing testing flow; the product site is
[ovayuvam.ovayuva.tr](https://ovayuvam.ovayuva.tr/).

## Artifact Identity

- APK: `ovayuvam-0.6.0.apk`, 44,805,871 bytes.
- APK SHA-256: `5f54738e73fa9773c663ac3900bda63b3b21232054f1016c1faaae50a7053473`.
- App bundle: `ovayuvam-0.6.0.aab`, 18,678,056 bytes.
- Bundle SHA-256: `5a0965940b79be1b05a0c1fcf225b0b0ed61cfce0b5afa66c06b2cab2a10b5cb`.
- APK signing certificate SHA-256:
  `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`.

There are no new permissions, accounts, backend services, or history uploads.
Visit records remain on the device or in a user-created encrypted backup.
