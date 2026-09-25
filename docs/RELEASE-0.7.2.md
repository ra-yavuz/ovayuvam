# ovayuvam 0.7.2

Android version code: 26. Package: `tr.ovayuva.ovayuvam`.

This update replaces the viewport-sized fog cache with reusable geographic
tiles and a zoom overview pyramid. Panning reuses existing images. New exploration
updates nearby cached tiles. Zoomed-out overviews reuse smaller images instead
of painting every saved point on each camera change.

The change removes the screen-resolution-dependent cache rejection affecting
larger phones. Camera movement also no longer triggers recomposition of the
map's enclosing UI, and history comparisons run off the main thread.

The circular reveal brush, radius in ground metres, heat colours, goal pin,
tracking service, database schema, and encrypted backup format are unchanged.
Existing exploration does not need to be reset or imported again.

See [Fog rendering](FOG-RENDERING.md) for implementation details and test scope.

## Verification

- 54 release unit tests passed; release lint reported no errors.
- Android bitmap checks passed for cache reuse, local invalidation, overview
  reuse, sparse history, tile boundaries, legacy data, date-line wrapping,
  replay removal, memory limits, and cancellation.
- Sixty small pans caused no extra point painting. A local reveal replaced two
  intersecting tile images while preserving 38 unrelated images in the fixture.
- MapLibre camera tests completed at 1080 x 2400 with 10,201 and 100,000 reveal
  points, and at 1440 x 3200 with 100,000 points. Frame timing remained uneven on
  the emulator; these checks do not establish smooth frame rates on every phone.
- A signed upgrade from 0.7.1 preserved the fixture's reveal history, visit
  counts, stay records, and goal. Signed replay, rotation, and recenter checks
  passed without changing saved history.
- The APK and app bundle passed signing and bundle validation with the existing
  release certificate. No storage migration is required.
