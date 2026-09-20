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

This is a production-ready local-first v1 release for sideloaded Android use. It
contains:

- A standalone Android project under `app/`.
- A local SQLite store for visited fog-grid cells.
- A full-screen MapLibre map with Ovayuva-style paper-and-ink OpenFreeMap tiles.
- A heavier fog overlay that hides unexplored map areas.
- Precise 20 m reveal trails over the real map.
- Small road/path reveal extensions after fresh movement on the open map, like fog being brushed along nearby streets.
- Smoother trail recording that filters poor fixes and fills gaps between good GPS points.
- Strict fog-of-war reveal at every zoom level, without country-scale aggregation.
- A private goal pin with an off-screen direction arrow.
- Automatic visible location tracking after permission is granted.
- A foreground notification that can reflect local progress.
- Manual encrypted export/import from the info sheet.
- A small info sheet with privacy, contact, and legal details.
- Ovayuva-style hand-drawn colors, fonts, and map controls.
- A draft public page for `https://ovayuva.tr/yuvam/` under `web/yuvam/`.
- Product, privacy, architecture, and roadmap notes under `docs/`.

## Download

Current APK release: `0.5.8`.

- Download: [ovayuvam-0.5.8.apk](https://github.com/ra-yavuz/ovayuvam/releases/download/v0.5.8/ovayuvam-0.5.8.apk)
- Mirror: [ovayuva.tr/yuvam/ovayuvam-0.5.8.apk](https://ovayuva.tr/yuvam/ovayuvam-0.5.8.apk)
- Package: `tr.ovayuva.ovayuvam`
- Size: `44,773,083` bytes
- SHA-256: `4eedfebdc677ec6a6d6d5dd2fbe554c758a696ebd545ed61ef3ae32f3df20a42`

Google Play Console uploads should use the signed Android App Bundle:

- Bundle: `release/ovayuvam-0.5.8.aab`
- Bundle SHA-256: `a48a5ff21069105bfa768c2e964dc845a156f129b78dabb911e24c190ae33373`

Version 0.5.8 adds local progress-aware notification text. While tracking is
active, the permanent notification can show a normal fog-clearing line, an evening
summary with today's approximate cleared area and walked distance, or a gentle
return line after multiple days without progress.

Version 0.5.8 does not paint road/path reveal just because the map opens. The
first visible position after opening only seeds the road tracker. Nearby road and
path reveal cells are added after fresh movement while the map is open.

Version 0.5.8 includes manual encrypted export/import. The backup file includes
revealed cells and the optional goal pin. It is encrypted with a passphrase chosen
by the user. ovayuvam cannot recover that passphrase.

This is a sideloaded APK outside Google Play. Android may ask you to allow
installation from your browser or file manager. If a browser reaches 100 percent
and appears stuck, open the Downloads or Files app and install the completed APK
from there.

## What is deliberately out of scope for version 1

- No ovayuva backend.
- No Firebase.
- No server-side account.
- No friend groups.
- No shared world upload.
- No ovayuva map backend. The basemap is loaded from OpenFreeMap/OpenStreetMap.
- No automatic Google Drive upload.
- No Google Play developer-account submission or review approval yet.

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
