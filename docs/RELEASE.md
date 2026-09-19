# Release Readiness

## Current release

- Version: `0.2.0`
- Package: `tr.ovayuva.ovayuvam`
- APK: `ovayuvam-0.2.0.apk`
- Size: `1,347,297` bytes
- SHA-256: `23533dadc5a9e7f5405cfff5d64032797ea290779f023f618872d187db71b2d5`
- Signing certificate SHA-256: `41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`

## Verified

- Clean release build passed with `./gradlew clean testDebugUnitTest assembleRelease`.
- Android release lint passed.
- APK signature verification passed.
- APK declares no `android.permission.INTERNET`.
- APK version is `versionCode=2` and `versionName=0.2.0`.
- APK asks only for location, foreground-service, notification, and AndroidX internal receiver permissions.
- Android automatic backup is disabled in the manifest and backup rule files.

## V1 boundary

This is production-ready as a local-first sideloaded v1. It is not yet a Play
Store-reviewed release.

The app draws a local visited-cell canvas. It does not fetch remote map tiles and
does not show a full street or satellite basemap. Location tracking must be started
by the user, uses a foreground service, and shows a persistent notification.

Manual export and import use JSON files selected by the user. Those files are not
encrypted by the app yet. Treat them as private location history.

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
access. This app does not declare that permission in version 0.2.0. See Android
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
