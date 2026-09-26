# ovayuvam 0.9.1

Package: `tr.ovayuva.ovayuvam`. Version code: 30. Date: 26 September 2026.
This is a testing release, not a public production launch.

New discoveries have a translucent green tint for the phone's current calendar
day. Revisiting old places does not make them new again. The tint respects local
midnight, time-zone changes and daylight-saving transitions. Replay does not
apply today's tint to historical frames.

A small badge below the logo estimates the all-time revealed area in square
metres. It measures the union of the saved brush footprints, corrects for latitude
and counts overlaps once. Planned routes and repeated visits are excluded.
The estimate uses fixed geographic raster tiles on a worker, not per-frame
calculations while zooming. It includes saved discoveries from older versions.

Also included from 0.9.0: six languages, first-launch tracking disclosure,
persistent pause controls, clearer settings, adjustable fog and a path-planning
pen. Weekly exploration defaults to on; existing explicit off choices are kept.
The stay radius is fixed rather than user-configurable.

## Verification

- 83 unit tests passed, including local-date and daylight-saving boundaries.
- Release lint completed with zero errors; existing warnings remain.
- Native checks passed for freshness pixels, midnight reset, overlapping shapes,
  geographic area, tile reuse, memory limits, backup restore and storage.
- Actual map screenshots verified the green tint, unchanged older regions,
  unchanged totals when a discovery ages, all six badge translations and a narrow
  screen with enlarged text.
- A signed 0.9.0-to-0.9.1 upgrade preserved all fixture rows: 1,681 visited cells,
  10,201 reveal cells, one visit-count row, one stay zone and one planned-path row.

These are emulator and automated checks. They do not establish physical-device
battery use, every device's behaviour, or production approval.

## Integrity

- APK SHA-256: `f1af7464fdcd3c941b9dd32f3451f1ab7988819ab979d5e2b40ac1aed949495b`
- Bundle SHA-256: `811747b7cd63326c245a7310530f340f18765ba994fd8b6dff1cde8ecc531a43`

Install the update over the existing app. Do not uninstall to update. The database
format and signing identity are unchanged from 0.9.0.
