# Architecture

ovayuvam starts as a local Android app. The first release has no backend and no
ovayuva account. It uses internet access only for the real basemap.

## Runtime pieces

- `MainActivity`: full-screen MapLibre map with a Compose fog overlay for the private world.
- `LocationTrailService`: visible foreground service that records GPS fixes while
  the app is revealing the world.
- `TrackingState`: small shared-preferences flag used by the UI to reflect whether
  tracking is active and where the latest current cell is.
- `GoalState`: small shared-preferences store for the optional private goal pin.
- `WorldCell`: converts latitude and longitude into stable Web Mercator grid cells.
- `VisitRepository`: stores visited cells in SQLite on the device.

## Storage

The local database stores grid cells, first seen time, last seen time, and sample
count. It does not store raw route uploads. This still counts as sensitive
location history because repeated cells can reveal routines.

Android cloud backup and device transfer are disabled for version 1. Version 0.5.1
does not include export or import UI. Later Google Drive backup should be explicit,
encrypted, user-started, and restorable without an ovayuva server.

## Map approach

Version 0.5.1 uses MapLibre with OpenFreeMap vector tiles styled in the Ovayuva
paper-and-ink direction. A heavy fog overlay sits above the real map and clears a
soft circular radius as the user walks. The visible trail is meant to feel like
brushing fog from paper, not like square tile chunks.

The foreground location service does not accept every raw Android fix. It ignores
stale seed locations, drops low-accuracy fixes, rejects large implausible jumps, and
fills the saved trail between accepted fixes. That keeps GPS glitches from clearing
random fog islands and makes movement look less dotted without increasing the reveal
radius.

The optional goal pin is a local latitude and longitude stored in shared
preferences. The pin is drawn above the fog. If it is off screen at normal
exploration zoom, an edge arrow points toward it.

The map tile provider can see tile requests for the viewed area. The app does not
send revealed cells, routes, accounts, or friend data to an ovayuva backend.
Offline map packs remain future work.

## Sharing approach

Friend groups are a later product layer. They should not change the default private
mode. The likely shape is client-encrypted world snapshots or selected regions, with
clear group membership and revocation limits.
