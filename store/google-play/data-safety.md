# Data safety draft

Google Play defines "collect" as transmitting user data off the user's device. It says local-only processing does not need to be disclosed as collected. The app still stores sensitive location-derived data locally, so keep the privacy policy explicit.

Version 0.5.10 also stores an optional local goal pin if the user long-presses the
map. That pin is local app data and is not sent to an ovayuva backend.

Version 0.5.10 stores today's accepted walking distance and latest progress time
locally so the foreground notification can show gentle progress text.

Version 0.5.10 lets the user manually export an encrypted backup file through the
Android file picker. The user chooses where that file goes. If they choose Drive,
Files, or another provider, that transfer is user-started and handled by Android
and the selected provider, not by an ovayuva backend.

## High-level answers

- Does the app collect or share user data? Conservative answer: `Yes`.
- Is all collected user data encrypted in transit? `Yes`, for HTTPS map tile requests.
- Does the app provide a way for users to request deletion? `No server account exists. Local data can be removed by uninstalling the app or clearing app data in Android settings. User-created export files must be deleted by the user wherever they saved them.`

## Data types

Use a conservative declaration because the real map loads third-party map tiles and map tile requests can reveal the viewed map area.

### Location

Approximate location:

- Collected: `Yes`
- Shared: `Yes`
- Purpose: `App functionality`
- Required or optional: `Required for the core app experience after the user grants location permission`
- Processed ephemerally: `Yes for map tile requests; local revealed-world cells are stored only on device`
- Notes: `Users can manually export an encrypted local backup. That export is user-initiated and may be saved to a provider they choose.`

Precise location:

- Collected: `Yes`, conservative answer because the app requests `ACCESS_FINE_LOCATION` and centers the map on the device location.
- Shared: `Yes`, conservative answer because map tile requests to OpenFreeMap/OpenStreetMap-based infrastructure may reveal the viewed map area.
- Purpose: `App functionality`
- Required or optional: `Required for the core app experience after the user grants location permission`
- Processed ephemerally: `Yes for off-device tile requests; local revealed-world cells are stored only on device`
- Notes: `Users can manually export an encrypted local backup. That export is user-initiated and may be saved to a provider they choose.`

## Data not collected by ovayuvam

- Name
- Email address
- Phone number
- Account ID
- Contacts
- Photos or videos
- Audio
- Files and documents
- Calendar
- Messages
- Payment information
- Advertising ID
- Crash analytics
- App analytics

## Sharing

The app has no ovayuva backend for the revealed world. It does make network requests to:

- OpenFreeMap / OpenStreetMap-based tile services for visible map tiles.

Those requests are part of app functionality. They are not used for ads, analytics, account management or marketing.

## Security practices

- Data encrypted in transit: `Yes` for HTTPS map tile requests.
- Users can request data deletion: `No server data exists for ovayuvam. Users can delete local data by uninstalling the app or clearing app data. User-created export files must be deleted from the place where the user saved them.`
- Independent security review: `No`.
- Committed to Play Families Policy: `No`, unless the target audience is later changed to include children.
