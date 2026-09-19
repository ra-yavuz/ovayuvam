# Architecture

ovayuvam starts as a local Android app. The first release has no backend and no
runtime internet permission.

## Runtime pieces

- `MainActivity`: full-screen Compose fog map for the private world.
- `LocationTrailService`: visible foreground service that records GPS fixes while
  the app is revealing the world.
- `TrackingState`: small shared-preferences flag used by the UI to reflect whether
  tracking is active and where the latest current cell is.
- `WorldCell`: converts latitude and longitude into stable Web Mercator grid cells.
- `VisitRepository`: stores visited cells in SQLite on the device.

## Storage

The local database stores grid cells, first seen time, last seen time, and sample
count. It does not store raw route uploads. This still counts as sensitive
location history because repeated cells can reveal routines.

Android cloud backup and device transfer are disabled for version 1. Version 0.3.0
does not include export or import UI. Later Google Drive backup should be explicit,
encrypted, user-started, and restorable without an ovayuva server.

## Map approach

Version 1 draws a local full-screen fog map. It starts with a parchment-like map
that is darkened by fog, then clears local cells as the user walks. It does not
request remote map tiles. A later map layer can be added only after map licensing,
provider privacy, offline behavior, and attribution are handled.

The app has no runtime `INTERNET` permission. The map works without app network
access because the first version is not a real basemap. It is a local drawing of
visited grid cells.

## Sharing approach

Friend groups are a later product layer. They should not change the default private
mode. The likely shape is client-encrypted world snapshots or selected regions, with
clear group membership and revocation limits.
