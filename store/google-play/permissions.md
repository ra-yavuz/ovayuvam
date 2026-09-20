# Permissions and sensitive access

## Declared Android permissions in 0.5.10

- `android.permission.INTERNET`
- `android.permission.ACCESS_COARSE_LOCATION`
- `android.permission.ACCESS_FINE_LOCATION`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_LOCATION`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.ACCESS_NETWORK_STATE`
- `tr.ovayuva.ovayuvam.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`

## Permission rationale

Internet:

- Needed to load OpenFreeMap/OpenStreetMap-based map tiles.
- Not used for an ovayuva backend, account, ads or analytics.

Approximate and precise location:

- Needed to reveal the fog map where the user walks.
- The core app experience does not work without location permission.
- The app stores location-derived grid cells locally on the device.

Foreground service and foreground service location:

- Needed to keep the user-visible location tracking service active while the app reveals walked places.
- Android shows a notification while tracking is active.

Post notifications:

- Needed on Android 13+ so the foreground-service notification can be shown.
- The notification may show local progress text, such as walking distance for today.

Access network state:

- Added by map/network dependencies for map loading state.

Dynamic receiver permission:

- AndroidX internal receiver protection permission.

## Background location

The app does not declare `android.permission.ACCESS_BACKGROUND_LOCATION`.

If Play Console asks about background location, use this answer:

`ovayuvam does not request Android background location permission. It uses a foreground location service with a visible notification after the user grants location permission. Location is used to reveal the private fog-of-war map where the user walks.`

## Store disclosure wording

Short disclosure:

`ovayuvam uses location to reveal the map where you walk. Tracking is visible through an Android notification. Your revealed world and progress stats are stored locally on your phone.`
