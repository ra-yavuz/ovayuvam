# ovayuvam

> A private, local-first Android map that slowly reveals the places you have been.

ovayuvam is a small open source companion project for the ovayuva family. The first
version is "my own world" only: no backend, no accounts, no social graph, no ads,
and no runtime `INTERNET` permission.

The app stores visited map cells on the device. A visible location tracking session
can keep adding cells while the app is not on screen, using a foreground service and
a persistent notification. Hidden tracking is not a goal.

## Status

This is an early seed repo. It contains:

- A standalone Android project under `app/`.
- A local SQLite store for visited fog-grid cells.
- A simple Compose screen that draws revealed cells without remote map tiles.
- A visible location tracking service.
- A draft public page for `https://ovayuva.tr/yuvam/` under `web/yuvam/`.
- Product, privacy, architecture, and roadmap notes under `docs/`.

## What is deliberately out of scope for version 1

- No ovayuva backend.
- No Firebase.
- No server-side account.
- No friend groups.
- No shared world upload.
- No remote map tile provider.
- No automatic Google Drive upload.

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

ovayuvam records location-derived cells. Even if raw coordinates are not exported,
visited cells can still reveal sensitive routines. Keep exports encrypted. Do not
use the app for safety, emergency, navigation, legal proof, or important records.

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
