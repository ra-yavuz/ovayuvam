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
- Small road/path reveal extensions near the current location, like fog being brushed along nearby streets.
- Smoother trail recording that filters poor fixes and fills gaps between good GPS points.
- Strict fog-of-war reveal at every zoom level, without country-scale aggregation.
- A private goal pin with an off-screen direction arrow.
- Automatic visible location tracking after permission is granted.
- A small info sheet with privacy, contact, and legal details.
- Ovayuva-style hand-drawn colors, fonts, and map controls.
- A draft public page for `https://ovayuva.tr/yuvam/` under `web/yuvam/`.
- Product, privacy, architecture, and roadmap notes under `docs/`.

## Download

Current APK release: `0.5.4`.

- Download: [ovayuvam-0.5.4.apk](https://github.com/ra-yavuz/ovayuvam/releases/download/v0.5.4/ovayuvam-0.5.4.apk)
- Mirror: [ovayuva.tr/yuvam/ovayuvam-0.5.4.apk](https://ovayuva.tr/yuvam/ovayuvam-0.5.4.apk)
- Package: `tr.ovayuva.ovayuvam`
- Size: `44,527,323` bytes
- SHA-256: `25c0287a3390163687ef42c9a3f81289601a4a814a07afca2c11aacd2c5022a9`

Google Play Console uploads should use the signed Android App Bundle:

- Bundle: `release/ovayuvam-0.5.4.aab`
- Bundle SHA-256: `638a068f39cd514a7e7acb41138f21dd16e7ddd8624eaa01865a08de011f516a`

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
- No export or import UI in this simple first map release.
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
