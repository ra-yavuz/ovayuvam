# Release Readiness

## Current release

- Version: `0.3.1`
- Package: `tr.ovayuva.ovayuvam`
- APK: `ovayuvam-0.3.1.apk`
- Size: `1,657,262` bytes
- SHA-256: `a9a20543a51e712fbf6e6a3ca76afee954e25c3042f164ae4fd1f69de7d0989f`
- Signing certificate SHA-256: `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`

## Verified

- Clean release build passed with `./gradlew clean testDebugUnitTest assembleRelease`.
- Android release lint passed.
- APK signature verification passed.
- APK declares no `android.permission.INTERNET`.
- APK version is `versionCode=4` and `versionName=0.3.1`.
- APK asks only for location, foreground-service, notification, and AndroidX internal receiver permissions.
- Android automatic backup is disabled in the manifest and backup rule files.
- The reveal view uses a circular paintbrush-style radius, not square visual chunks.
- Map zoom supports a wide inspection range while keeping the reveal radius tied to world cells.

## V1 boundary

This is production-ready as a local-first sideloaded v1. It is not yet a Play
Store-reviewed release.

The app opens directly to a full-screen local fog map. It does not fetch remote map
tiles and does not show a full street or satellite basemap. On launch, it requests
the needed permissions and starts visible location tracking as soon as permission
and Android Location are available.

There is no export or import UI in version 0.3.1. Later backup work must be an
explicit product decision.

## Not included yet

- Google Drive backup.
- Encrypted backups.
- Friend groups.
- Shared worlds.
- Offline basemap packs.
- Play Store listing review.
- Device battery matrix testing.

## Policy and platform notes

Android requires a location foreground service to declare the `location` service
type and the matching foreground-service permission. It also requires coarse or
fine location permission before the service starts. See Android foreground service
types: https://developer.android.com/develop/background-work/services/fgs/service-types

Android 10 and newer use `ACCESS_BACKGROUND_LOCATION` for background location
access. This app does not declare that permission in version 0.3.1. See Android
location permissions: https://developer.android.com/develop/sensors-and-location/location/permissions

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
