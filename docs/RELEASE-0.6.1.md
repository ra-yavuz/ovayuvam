# ovayuvam 0.6.1

Android package: `tr.ovayuva.ovayuvam`. Version code: `23`.

## Changes

Visit color and opacity now increase gradually with accumulated visit counts.
There is no eight-visit cutoff. Equal space on the visual scale is given to
1, 10, 100, and 1,000 visits, followed by a gradual tail for higher counts.
The scale is fixed: panning to a different area does not change its meaning.
These are accumulated visits since counting began, not a rolling yearly total.

The default close-up view at zoom 15.6 and small zoom-outs remain free of tint.
Color starts below zoom 14.5, grows as the map zooms out, and reaches full zoom
strength at 12. At full strength, 1, 10, 100, and 1,000 visits use 12%, 18%, 24%,
and 30% opacity. Opacity stays below 36% even for very large counts. At zoom 13,
those values are reduced to 60%, keeping the map and street labels readable.

Only rendering and the app version changed. Stay detection, reveal geometry,
permissions, backup format, and storage schema are unchanged from 0.6.0.
Existing measured visits use the new appearance immediately. Previously revealed
places without measured visits gain tint when visited after counting began.
Install over the existing app without uninstalling or clearing its data.

## Verification

- 31 release unit tests passed, including continuous visit intensity, large
  counts, fixed color scale, zoom fading, and no tint at default or nearby zoom.
- Release lint completed with zero errors and 13 existing warnings.
- The signed APK upgraded from 0.6.0 on Android 13 and preserved all 10,201
  reveal rows, 1,681 visited rows, the saved goal, and a seeded 100-visit record
  with its stay state unchanged.
- Separate verification-package checks passed for stay movement, restarts,
  departure/return, backup merging, large history, and geographic queries.
- Heat-on/off emulator images differ at zoom 12, 13, and 14. At zoom 14.6 and
  the default 15.6, there are zero changed pixels in the checked map region.
  Inspected neighborhood images retain readable map details and labels.
- Candidate website checks passed in English, German, Turkish, and French at
  phone and desktop sizes, including download consent and the new APK link.

These checks use synthetic data and an emulator. Physical-phone readability,
field GPS accuracy, and battery use across manufacturers still need field tests.

## Distribution

This is a tester release outside Google Play. Download links are shared through
the existing testing flow. The product site is
[ovayuvam.ovayuva.tr](https://ovayuvam.ovayuva.tr/).

The update does not add accounts, backend services, or location-history uploads.

## Artifact Identity

- APK: `ovayuvam-0.6.1.apk`, 44,805,871 bytes.
- APK SHA-256: `808986c0e638e3164d24d0f31d62831afdbcea70ad08a37b703a9e45fdad53b3`.
- App bundle: `ovayuvam-0.6.1.aab`, 18,679,053 bytes.
- Bundle SHA-256: `cd5efa221ad04a79f1d30d8d4957d83cd6454953c1fb31b2c75900429777ffa1`.
- APK signing certificate SHA-256:
  `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`.
