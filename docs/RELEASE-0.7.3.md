# ovayuvam 0.7.3

Android package: `tr.ovayuva.ovayuvam`, version code `27`.
This is an internal-test release, not a Google Play production release.

## Changes

- **Watch your world grow** starts with the first visible discoveries. The camera
  widens as more places appear and returns to a close view when rewound.
- Backup import and export select the file before asking for its passphrase.
  Returning from the Android file picker after the app restarts no longer loses
  a passphrase entered before file selection.
- Backup work runs off the main thread. Progress, success, and errors appear in
  the backup dialog, including wrong-passphrase and inaccessible-file errors.
- Imports merge revealed cells and visit counts in one database transaction.
  A failed merge rolls back instead of leaving a partially imported world.
- Password entry disables autocorrection. Export asks for confirmation to help
  catch a mistyped passphrase.

Install over your existing app. Do not uninstall to update. The update does not
reset your world, goal, or settings and does not change the backup format.
Keep the original backup and its passphrase until you have checked a restore.

## Verification

- 66 release unit tests passed. Android lint reported no errors.
- Signed APK and app bundle signatures verified; bundle validation passed.
- Android 13 emulator checks covered progressive replay framing and rewind,
  unchanged replay history, database merge rollback, export after activity
  destruction, and import after reinstall and process termination in the picker.
- A wrong passphrase produced a visible, retryable error. Retrying with the
  correct passphrase restored the exported fixture's 27 reveal records exactly.
- A signed 0.7.2 to 0.7.3 upgrade was checked against saved world and goal fixtures.

Older undated history remains a visible baseline. The app cannot reconstruct a
discovery order that was never recorded. Emulator and unit checks do not prove
that every document provider or damaged backup can be restored.

## Access

Existing testers can update through Google Play internal testing. Request a
tester invitation through [the product website](https://ovayuvam.ovayuva.tr/).
This source release does not publish APK download assets.

Provided without warranty. Stay aware of your surroundings and use at your own risk.
