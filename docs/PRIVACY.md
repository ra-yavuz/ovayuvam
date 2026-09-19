# Privacy

ovayuvam is designed around a simple rule: version 1 is your own world on your own
device.

## Version 1 data

The app records:

- Visited grid cell coordinates.
- First seen time.
- Last seen time.
- Sample count per cell.

The app does not record:

- Server account IDs.
- Friend lists.
- Public posts.
- Remote map tile requests.
- Backend sync records.

## Sensitive data warning

Visited cells are location history. They can reveal home, work, habits, medical
visits, religious visits, and travel patterns. Treat exports as private even when
they contain cells instead of raw GPS points.

## Backup

Android automatic backup is disabled in version 1. A later backup feature can use
the user's Google Drive only after a clear opt-in. The backup should be encrypted on
the device before upload. The app should not hold a server-side recovery key.

## Friend sharing

Sharing is not part of version 1. A later friend group feature should be opt-in per
group and should explain what friends can see, what they cannot see, and what remains
visible after someone leaves a group.

## No warranty

No warranty is provided. You use this software at your own risk. Do not use it for
emergencies, safety, navigation, legal proof, or important records.

