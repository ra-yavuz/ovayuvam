# Release Readiness

## Current release

- Version: `0.5.10`
- Package: `tr.ovayuva.ovayuvam`
- APK: `ovayuvam-0.5.10.apk`
- Size: `44,773,087` bytes
- SHA-256: `cef2beb6e7ccde5780d22f5ce769e4f20533e28d60f18e89a48a514905d45fe6`
- Play App Bundle: `ovayuvam-0.5.10.aab`
- Bundle size: `18,593,792` bytes
- Bundle SHA-256: `29869ac9e8bc8bcd957040198d461c63e6f3260f9deadbf1dbb6da585589f4cf`
- Signing certificate SHA-256: `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`

## Verified

- Clean release build passed with `./gradlew clean testDebugUnitTest assembleRelease bundleRelease`.
- Android release lint passed.
- APK signature verification passed.
- APK version is `versionCode=19` and `versionName=0.5.10`.
- APK asks for internet and network-state permissions for OpenFreeMap/OpenStreetMap map tiles.
- APK asks for location, foreground-service, notification, and AndroidX internal receiver permissions.
- APK does not declare `android.permission.ACCESS_WIFI_STATE`.
- Android automatic backup is disabled in the manifest and backup rule files.
- The map view uses MapLibre with OpenFreeMap vector tiles.
- The Android launcher foreground, themed launcher mask, and `/yuvam/` logo use the same folded map artwork as the map-view info button.
- The reveal view uses a smaller circular paintbrush-style radius over a much stronger fog layer.
- The location service filters stale or low-accuracy fixes, rejects implausible jumps, and interpolates between accepted fixes for a smoother walking trail.
- Version 0.5.10 keeps road/path reveal from being generated just because the map opens.
- The first visible position after opening only seeds the road tracker.
- Nearby rendered streets and paths add small road/path reveal cells only after fresh movement while the map is open.
- Version 0.5.10 adds local progress-aware foreground notification text.
- The notification can rotate between fog-clearing lines, an evening square-meter summary, an evening walked-distance fallback, and a gentle return line after multiple inactive days.
- Version 0.5.10 restores cleared-area notification numbers as approximate square meters only.
- The square-meter estimate counts unique core reveal cells first seen today and ignores road/path glow cells.
- The notification does not use hectares.
- Today's distance is accumulated from accepted GPS movement.
- Version 0.5.10 adds manual encrypted export/import in the info sheet.
- Exported files include visited cells, reveal cells and the optional goal pin.
- Backup files are encrypted with PBKDF2WithHmacSHA256 and AES-256-GCM using a user passphrase.
- Wrong passphrase rejection and round-trip backup are covered by JVM unit tests.
- Strict fog-of-war reveal remains active at every zoom level.
- The bottom attribution pill still uses white text on a dark translucent background so the map credit remains readable over dark fog.
- The service records a more precise 20 m reveal trail and interpolates points every 10 m.
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
The foreground notification is user-visible and can change based on local progress.

Version 0.5.10 includes manual encrypted export/import. It is local and user-started,
not automatic cloud sync. Losing the passphrase makes the export unusable.

## Not included yet

- Google Drive backup.
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
access. This app does not declare that permission in version 0.5.10. See Android
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
