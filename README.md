# ovayuvam

> A private, local-first Android map that slowly reveals the places you have been.

ovayuvam is a small open source companion project for the ovayuva family. The first
version is "my own world" only: no ovayuva backend, no accounts, no social graph,
and no ads. The real map uses OpenFreeMap/OpenStreetMap tiles over the internet.

The app opens directly to a full-screen fog map. On launch it asks for the needed
location permission and starts revealing walked places as soon as permission and
Android Location are available. Tracking is visible through a foreground service
notification. Hidden tracking is not a goal.

## Status

This is a local-first Android app available to invited Google Play internal testers. It
contains:

- A standalone Android project under `app/`.
- A local SQLite store for visited fog-grid cells.
- A full-screen MapLibre map with Ovayuva-style paper-and-ink OpenFreeMap tiles.
- A heavier fog overlay that hides unexplored map areas.
- Precise 20 m reveal trails over the real map.
- Small road/path reveal extensions after fresh movement on the open map, like fog being brushed along nearby streets.
- Smoother trail recording that filters poor fixes and fills gaps between good GPS points.
- Strict fog-of-war reveal at every zoom level, without country-scale aggregation.
- Cached fog rendering that keeps large explored worlds responsive while zooming.
- Translucent visit colors when zoomed out, fading away at street level.
- Local return detection that groups movement within a stay and filters GPS drift.
- An adjustable stay radius for larger homes or properties.
- Optional weekly nearby exploration suggestions with an off-screen direction arrow.
- Automatic visible location tracking after permission is granted.
- A silent foreground notification with rotating messages and local progress.
- A read-only replay of first discoveries, by week, month, year, or all time.
- Manual encrypted export/import from the info sheet.
- A small info sheet with privacy, contact, and legal details.
- Ovayuva-style hand-drawn colors, fonts, and map controls.
- A draft public page for `https://ovayuva.tr/yuvam/` under `web/yuvam/`.
- Product, privacy, architecture, and roadmap notes under `docs/`.

## Download

Version `0.8.0`, Android package `tr.ovayuva.ovayuvam`.

Visit [the product website](https://ovayuvam.ovayuva.tr/) to request a tester invitation.
Downloads are through Google Play internal testing. This is not a public production release.

Install the update over the existing app to retain your revealed world, goal,
and settings. Do not uninstall first. Visit counts have been recorded since
version 0.6.0 and are retained by this update. Earlier GPS sample counts cannot
tell us how many independent visits took place.

Enable **Weekly exploration** in settings for an occasional nearby suggestion.
After an hour in one area, a suitable unexplored street or path within 500 metres
can be marked **Explore here**. There is at most one suggestion per seven days;
it expires after three days. Map access information may be incomplete, so follow
local signs. No suggestion is invented when suitable map data is unavailable.
Long-press goal placement has been removed. Settings also show the installed
version and build number.

Open the info sheet and choose **Watch your world grow** to replay your saved
discoveries. Pause, seek to a date, or choose a week, month, year, or all-time
view. The camera starts close to the first discoveries and widens as the revealed
world grows. Earlier discoveries remain visible at the start of a shorter period.
Replay does not change saved history or stop tracking. It reconstructs first
discoveries, not an exact route or past visit colors.

The existing tracking notification rotates through 83 short messages about every
90 minutes while the service runs. Updates are silent and replace the same
notification. No separate reminders or wake-up alarms are scheduled. Android
may delay a change while the phone sleeps. Evening progress summaries remain.

Visit tint grows gradually with the number of visits, with room to distinguish
dozens, hundreds, and thousands of returns. It stays below 36% opacity, fades in
between zoom 14.5 and 12, and is absent at the default close-up view. A default
150-meter stay radius groups movement around a home or garden; the settings
sheet allows a larger area. Another visit requires reliable evidence of leaving
and returning. GPS gaps and uncertain fixes may cause visits to be undercounted.

Encrypted backups include revealed cells, visit counts, and the optional goal
pin. Older backups remain readable. Keep your passphrase; ovayuvam cannot recover
it. Select a file first, then enter its passphrase. Import and export show progress
and a clear result. Imports merge with your existing world. This feature does not
add accounts, automatic cloud sync, or friend sharing.

Version 0.7.2 uses reusable geographic fog tiles and smaller overview images when
you zoom out. New exploration updates nearby tiles. The brush radius stays tied
to real distance. Rendering pauses when the map is off screen; location tracking
continues as before. See the [release notes](docs/RELEASE-0.8.0.md) and
[rendering design](docs/FOG-RENDERING.md).

## What is deliberately out of scope for version 1

- No ovayuva backend.
- No Firebase.
- No server-side account.
- No friend groups.
- No shared world upload.
- No ovayuva map backend. The basemap is loaded from OpenFreeMap/OpenStreetMap.
- No automatic Google Drive upload.
- No Google Play production release or review approval yet.

Future versions can add optional encrypted backup to the user's Google Drive and
optional friend groups. Those features must be separate opt-ins, not default data
flows.

## Build

Use a normal Android toolchain with JDK 21, Android SDK 36, and Gradle through the
checked-in wrapper:

```sh
./gradlew testDebugUnitTest assembleDebug
```

The repo also includes a minimal container recipe in `docker/` for local Android
builds without installing the toolchain on your machine.

The optional [Play internal-release workflow](docs/PLAY-AUTOMATION.md) can build
and upload signed bundles from release tags. It stays disabled until dedicated
credentials are configured. It never targets production.

## Safety and privacy

ovayuvam records location-derived cells. Visited cells can reveal sensitive
routines. Do not use the app for safety, emergency, navigation, legal proof, or
important records.

No warranty is provided. You use this software at your own risk. You are responsible
for checking local laws, platform rules, map-data rules, and safety conditions before
using or modifying it.

## Relationship to ovayuva

This repo is designed to be open source and independent. It does not include private
ovayuva app source, private artwork, server code, production keys, deployment paths,
or proprietary product internals.

## License

MIT. The license covers this repository's code and documentation. It does not grant
rights to third-party brands, app-store names, icons, map data, or the ovayuva brand.
