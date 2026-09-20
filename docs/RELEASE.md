# Release Readiness

## Current release

- Version: `0.5.4`
- Package: `tr.ovayuva.ovayuvam`
- APK: `ovayuvam-0.5.4.apk`
- Size: `44,527,323` bytes
- SHA-256: `25c0287a3390163687ef42c9a3f81289601a4a814a07afca2c11aacd2c5022a9`
- Play App Bundle: `ovayuvam-0.5.4.aab`
- Bundle size: `17,792,070` bytes
- Bundle SHA-256: `638a068f39cd514a7e7acb41138f21dd16e7ddd8624eaa01865a08de011f516a`
- Signing certificate SHA-256: `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`

## Verified

- Clean release build passed with `./gradlew clean testDebugUnitTest assembleRelease bundleRelease`.
- Android release lint passed.
- APK signature verification passed.
- APK version is `versionCode=13` and `versionName=0.5.4`.
- APK asks for internet and network-state permissions for OpenFreeMap/OpenStreetMap map tiles.
- APK asks for location, foreground-service, notification, and AndroidX internal receiver permissions.
- APK does not declare `android.permission.ACCESS_WIFI_STATE`.
- Android automatic backup is disabled in the manifest and backup rule files.
- The map view uses MapLibre with OpenFreeMap vector tiles.
- The Android launcher foreground, themed launcher mask, and `/yuvam/` logo use the same folded map artwork as the map-view info button.
- The reveal view uses a smaller circular paintbrush-style radius over a much stronger fog layer.
- The location service filters stale or low-accuracy fixes, rejects implausible jumps, and interpolates between accepted fixes for a smoother walking trail.
- Version 0.5.4 keeps strict fog-of-war reveal at every zoom level.
- The service records a more precise 20 m reveal trail and interpolates points every 10 m.
- While the map is open, nearby rendered streets and paths add small road/path reveal cells so explored places can spread like a street web instead of isolated dots.
- Long-pressing the map stores a private local goal pin.
- When the goal pin is off screen at normal exploration zoom, the app draws an edge arrow toward it.
- Map zoom supports a wide inspection range while keeping the reveal radius tied to world cells.

## V1 boundary

This is production-ready as a local-first sideloaded v1. It is not yet a Play
Store-reviewed release.

The app opens directly to a full-screen real street map with a heavy fog overlay. The
basemap loads from OpenFreeMap/OpenStreetMap over the internet. Revealed places
are stored locally as grid cells on the phone. A goal pin, if set, is stored
locally on the phone. On launch, it requests the needed permissions and starts
visible location tracking as soon as permission and Android Location are available.

There is no export or import UI in version 0.5.4. Later backup work must be an
explicit product decision.

## Not included yet

- Google Drive backup.
- Encrypted backups.
- Friend groups.
- Shared worlds.
- Offline basemap packs.
- Google Play developer-account submission and review approval.
- Device battery matrix testing.

## Policy and platform notes

Android requires a location foreground service to declare the `location` service
type and the matching foreground-service permission. It also requires coarse or
fine location permission before the service starts. See Android foreground service
types: https://developer.android.com/develop/background-work/services/fgs/service-types

Android 10 and newer use `ACCESS_BACKGROUND_LOCATION` for background location
access. This app does not declare that permission in version 0.5.4. See Android
location permissions: https://developer.android.com/develop/sensors-and-location/location/permissions

Real map tiles require network access. The app uses OpenFreeMap/OpenMapTiles/
OpenStreetMap data for the visible basemap. Map tile requests may reveal the
rough area being viewed to the tile provider, separate from the local fog data.

Google Play has extra review requirements for background location. Any Play Store
submission must treat this as separate policy work. See Google Play background
location guidance: https://support.google.com/googleplay/android-developer/answer/9799150

Android Auto Backup can include SQLite databases by default. This app opts out
because visited cells are sensitive. See Android Auto Backup: https://developer.android.com/identity/data/autobackup

Future Google Drive backup should use an explicit opt-in and app-owned storage,
with encryption before upload. See Drive `appDataFolder`: https://developers.google.com/workspace/drive/api/guides/appdata

## No warranty

No warranty is provided. You use this software at your own risk. Do not use it for
emergencies, safety, navigation, legal proof, or important records.
