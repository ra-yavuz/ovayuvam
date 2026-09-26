# ovayuvam 0.10.0

Package: `tr.ovayuva.ovayuvam`. Version code: 31.
This release is for testing tracks, not a production launch.

## Map Controls

Tap the logo to flip between the brand and explored area. The area face shows
all-time and today's estimated square metres. The chosen face is saved on the
phone. In logo mode, the totals appear for eight seconds after three minutes of
active map viewing, then return to the logo. Settings, painting and replay pause
these previews. Manually pinned totals do not flip back automatically.

Today's area is the full reveal union minus the union of non-today discoveries.
Overlaps, revisits, unknown old timestamps and painted plans do not inflate it.
The counters use the phone's calendar day and cached geographic tiles, not the
camera zoom. Existing worlds and encrypted backups retain their format.

In painting mode, one finger paints. Two fingers pan and zoom, even when zoomed
too far out to paint. Adding a second finger cancels the unfinished stroke; that
gesture cannot save a stray line when either finger is lifted. Ordinary map
gestures outside painting mode are unchanged. The pen has transparent artwork
and no background tile.

## Sharing And Settings

Share my world captures the current map view, including fog and visible map
credits, and opens Android's share sheet with the ovayuvam website link. It does
not export the database or upload anything to an app server. Only the chosen
image is granted to the receiving app through a private FileProvider. Old share
images are removed from the app cache after seven days when sharing again.

Circular information buttons explain settings and replay. Replay has a more
prominent action. All new controls, help and share text support English, German,
Turkish, Russian, Spanish and French. Phone language remains the default.
Running tracking notifications refresh when the language changes, including
their channel name and pause action, without sound or vibration.

Map credits are shown on launch for eight seconds, then collapse into a map
button. Tapping it shows the credits again. The expanded credits link to the
providers and OpenStreetMap's licence information. Shared images retain them.
The timeout respects Android accessibility recommendations.

Reference: [OpenStreetMap attribution guidelines](https://osmfoundation.org/wiki/Licence/Attribution_Guidelines#Interactive_maps).

## Verification

All 84 unit tests passed. Release lint reported no errors and 50 warnings.
Native emulator checks passed for discovery area, daily overlap handling,
backup storage, fog tiles, exploration suggestions, controls, sharing and quiet
notifications. The running tracking service updated its title, message, pause
action and channel name in all six languages. The three-minute automatic badge
preview and return were checked at the real interval. Screens were inspected
at ordinary and narrow widths, including larger text.

A signed upgrade from 0.9.1 preserved every row in the test fixture: 1,681
visited cells, 10,201 reveal cells, one visit-count row, one stay zone and one
painted route. This is emulator evidence, not a claim that every physical phone
has been tested. No database or backup format migration is introduced.

## Artifact Identity

- APK SHA-256: `8924c072e5022cdc99e93cd56439048bbba100158d5665fd97aa86276ef588b4`
- App Bundle SHA-256: `54c4d36ef237e8e164e135c33429b595e0a01bf61fffb1faf7661069280c8ecb`

The GitHub prerelease contains source only. Google Play internal testing lists
this build as available to testers. The closed Alpha update has been submitted
for review. Neither state is a production launch or production approval.
