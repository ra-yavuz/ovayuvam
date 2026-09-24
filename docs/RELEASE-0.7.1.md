# ovayuvam 0.7.1

Android package: `tr.ovayuva.ovayuvam`. Version code: `25`.

## Changes

Zooming across a large explored world no longer reprojects and redraws every
saved reveal point on each frame. A worker renders the fog and visit colors into
bounded, map-aligned images. Camera gestures reuse those images while a new view
is prepared. Image dimensions are capped at 1536 pixels per side.

Circular brush detail and translucent visit colors remain. Reveal radii stay in
ground meters, without a minimum screen-pixel radius that enlarges small areas
at distant zoom levels. The live location and goal markers update separately.
Map-data refresh and cached rendering pause while the map is off screen.
Background location tracking is unchanged.

There is no database migration, history deletion, new permission or backup-format
change. Update the existing installation without uninstalling or clearing data.

## Verification

- All 48 release unit tests passed. Release lint reported zero errors.
- Signed APK upgrade and replay retained all 10,201 reveal rows, 1,681 visited
  rows, the visit-count and stay records, and the goal in the test world.
- Android pixel tests passed for clear visited centers, covered unvisited areas,
  translucent heat, legacy history, date-line wrapping and cancellation.
- Heat appeared at zoom 12, 13 and 14, with real map details still visible.
  Heat-on and heat-off images were identical at zoom 14.6 and 15.6.
- Signed-app replay, seeking, closing, rotation and recentering passed. Replay
  images showed increasing revealed area without changing saved rows.
- Existing stay detection, backup merging, spatial query and silent notification
  checks passed on Android 13.
- APK and bundle signatures and bundle validation passed. The signing certificate
  and application ID are unchanged.

In an Android 13 emulator with the same synthetic 10,201-point history, wide zoom
animations requested at 2.5 seconds took 7.5 to 11.3 seconds on version 0.7.0 and
2.6 to 2.9 seconds on version 0.7.1. The new renderer also completed the camera
sequence with 100,000 reveal points, with animations taking 2.5 to 3.1 seconds.
These measurements show a large reduction in the tested UI stalls, not a promised
frame rate on every phone. Physical-device performance, field GPS accuracy,
battery use and Google Play policy approval are not established by these tests.

## Distribution

Available to the existing Google Play internal tester list. Production is unchanged.
Request a tester invitation through [the product website](https://ovayuvam.ovayuva.tr/).
The GitHub release is source-only and does not include public APK or bundle downloads.

## Artifact Identity

- APK: `ovayuvam-0.7.1.apk`, 44,855,447 bytes.
- APK SHA-256: `c987bcb0f6f6409df9ec19fc74c52e21785b8cf61a8d76312a28047bd53cf7b6`.
- App bundle: `ovayuvam-0.7.1.aab`, 18,824,029 bytes.
- Bundle SHA-256: `244b29dde0951433c2362bf9667fe7963610ba7629c6238d52abc94380521c08`.
- Signing certificate SHA-256:
  `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`.

No warranty is provided. Use the app at your own risk and stay aware of your surroundings.
