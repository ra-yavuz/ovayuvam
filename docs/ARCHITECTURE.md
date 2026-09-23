# Architecture

ovayuvam starts as a local Android app. The first release has no backend and no
ovayuva account. It uses internet access only for the real basemap.

## Runtime pieces

- `MainActivity`: full-screen MapLibre map with a Compose fog overlay for the private world.
- `LocationTrailService`: visible foreground service that records GPS fixes while
  the app is revealing the world.
- `TrackingState`: small shared-preferences flag used by the UI to reflect whether
  tracking is active and where the latest current cell is.
- `DailyProgressStore`: small shared-preferences store for the current day's
  accepted walking distance and latest progress time, used only for notification text.
- `GoalState`: small shared-preferences store for the optional private goal pin.
- `WorldCell`: converts latitude and longitude into stable Web Mercator grid cells.
- `VisitRepository`: stores visited cells in SQLite on the device.

## Storage

Version 0.6.0 adds `visit_zones` and `visit_counts` through database migration 3.
Existing visited/reveal rows are unchanged. A stable geographic stay anchor and
a configurable 150-1,000 meter radius group local movement. Accurate observations
must establish ten minutes beyond an additional 100 meter exit buffer before
three return fixes over ten seconds can start another visit. Counts increase at
most once per observed cell in a stay epoch. Missing GPS does not prove absence.
Monotonic observation time and Android's boot count protect pending timers.

`VisitDetector` contains the pure decision logic. `VisitRepository` persists the
state and increments in one transaction. Location callbacks run on a service
worker thread. The map reads viewport-bounded history on an IO dispatcher without
the former 8,000-cell cutoff. Far, already departed zones are excluded from normal
presence updates until approached again.

The existing one-second location request also delivers stationary fixes. Those
fixes provide evidence for stay confirmation and absence, while movement under
one meter does not repaint the saved trail. The visit repository deduplicates
fixes received within two seconds of its last accepted observation.

Visit tint is drawn in a separate layer with an opacity ceiling of 36%, using
replacement blending so overlapping marks do not accumulate opacity. Color is
restricted to the fully cleared centers of core reveal brushes. It fades between
zoom 12 and 14.5 and is absent at the default zoom of 15.6 and a small zoom-out.
Hue and opacity interpolate on a logarithmic visit scale. Each decade from 1 to
1,000 receives equal space, followed by an asymptotic tail rather than a hard
visit ceiling. At zoom 12 and below, 1, 10, 100, and 1,000 visits have opacities
of 12%, 18%, 24%, and 30%. The fog pass is independent and
retains the same geometry. Old unmeasured cells and road-only glow receive no tint.

Backup payload v2 includes visit counts and accepts v1 payloads. Existing counts
merge by maximum, making repeated imports idempotent. Independent device counts
are not additive. Detection state is reset conservatively on import.

The local database stores grid cells, first seen time, last seen time, and sample
count. It does not store raw route uploads. This still counts as sensitive
location history because repeated cells can reveal routines.

Android cloud backup and device transfer are disabled for version 1. Version 0.5.10
includes manual encrypted export/import. The file contains visited cells, reveal
cells and the optional goal pin, encrypted with a user passphrase using
PBKDF2WithHmacSHA256 and AES-256-GCM. Later Google Drive backup should be
explicit, encrypted, user-started, and restorable without an ovayuva server.

## Map approach

Version 0.5.10 uses MapLibre with OpenFreeMap vector tiles styled in the Ovayuva
paper-and-ink direction. A heavy fog overlay sits above the real map and clears a
soft circular radius as the user walks. The visible trail is meant to feel like
brushing fog from paper, not like square tile chunks.

Strict fog-of-war reveal is used at every zoom level. The app does not aggregate
visited cells into large zoomed-out regions because that can make too much of the
map feel revealed.

The foreground location service does not accept every raw Android fix. It ignores
stale seed locations, drops low-accuracy fixes, rejects large implausible jumps, and
fills the saved trail between accepted fixes. That keeps GPS glitches from clearing
random fog marks and makes movement look less dotted without increasing the reveal
radius.

The foreground notification uses only local stats. It can show a default
fog-clearing line, an evening square-meter summary, an evening walking-distance
fallback, or a gentle return line after multiple days without progress. The
square-meter summary counts only unique core reveal cells first seen today and
ignores road/path glow cells. It does not use hectares and does not schedule
separate background marketing notifications.

Road and path reveal is not generated from saved cells when the map opens. Opening
or resuming the map seeds the road tracker with the current position. Nearby
rendered streets and paths are added only after fresh movement on the open map.

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
