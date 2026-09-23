# Revisit heat

Status: implemented in the 0.6.0 tester release, 2026-09-23. Release checks
are recorded separately from this design.

Audience: ovayuvam product and Android contributors.
Purpose: define how repeated visits can add meaning to the explored world while
preserving the full-screen map, private local storage, and strict fog boundaries.
Scope: visit detection, storage, backup, map rendering, and acceptance checks.

## Experience

At street level, the usual map and fog remain visible. When the user zooms out
to see a town or wider area, explored paths gradually gain color according to
how often the user has returned. Unexplored ground keeps exactly the same fog.
The color reflects return frequency, not time spent standing somewhere.

Use a restrained teal, gold, and coral progression with different lightness as
well as hue. Suggested visit bands are 1, 2-3, 4-7, and 8+. Keep the scale stable
when panning; entering a different viewport must not change a place's meaning.
An optional small legend reads "Visits". A settings switch controls the overlay;
there is no new page or tracking button.

The implemented tint is capped at 24% opacity so the map remains readable.
Replacement blending prevents repeated overlapping brush marks from making it
opaque. Color is limited to the fully cleared centers of core reveal brushes;
the independent fog pass retains the original geometry.

Initial visual tuning: fully visible at map zoom 10 and below, smoothly fading
out between 10 and 12, absent at 12 and above. These are prototype values, to be
checked on real phone screens. At country or world scale, narrow trails may
become subpixel and fade from view. Do not enlarge them to keep them visible.

## Baseline code evidence

These observations describe the code before the 0.6.0 implementation.

- `storage/VisitRepository.kt` increments `samples` on repeated writes. This
  measures recording activity and cannot establish independent visits.
- `location/LocationTrailService.kt` filters stale and inaccurate fixes and
  large jumps, then fills trails between accepted positions. It has no visit
  departure/return state.
- `domain/WorldCell.kt` uses 75-unit visited cells and 20-unit reveal cells in
  Web Mercator coordinates. These are projected units; real ground dimensions
  vary with latitude. Detection distances must use geographic distance.
- `MainActivity.kt` draws circular reveal brushes, including separate road
  marks. It currently reads at most 8,000 recent reveal cells for the map.
- `backup/WorldBackupCodec.kt` stores sample counts and first/last seen times,
  but no independent visit history.

Paths above are relative to `app/src/main/java/tr/ovayuva/ovayuvam/`.
This evidence supports the design's integration points, not measured detection
accuracy or a claim that the feature is already available.

## What counts as a visit

Separate the area used to recognize a stay from the cells used to draw heat.
Moving between rooms, floors, a garden, or nearby cell boundaries belongs to
one stay. A storage cell boundary must never independently end that stay.

Proposed default: a stay covers a 150-ground-meter radius around a stable local
anchor. This is a tolerance for local movement, not a claim about a building's
boundary. Establish the anchor from reliable arrival observations and keep it
fixed for the stay. It must not follow the user down a street or grow with every
GPS excursion. Poor accuracy pauses decisions instead of enlarging the area.
For a property larger than this area, offer an optional larger stay radius in
settings. Apply a radius change conservatively without creating a new visit.

Existing coarse visited cells remain useful for storing heat statistics. Cells
actually observed during the same stay share a stay identifier and receive at
most one visit each from it. First entering a different room can add newly
observed coverage, but going back to an earlier room cannot add a revisit.
Do not assign a visit to every cell inside the stay radius. Only observed
coverage receives counts or heat; the fog reveal radius remains independent.

A walking passage still qualifies without establishing a long stationary stay.
For cells along a route, apply the same departure buffer and absence requirement
locally. When cells belong to an ongoing stay, its shared departure state takes
precedence over individual cell departure. This prevents one end of a large
house from rearming while the user spends time at the other end. Nearby stay
areas may overlap; associate a cell with the active stay without merging all
overlapping areas into an ever-growing region.

Each stay, or route area outside a stay, has the following persistent state:

1. Candidate: reliable observations indicate presence. Confirm an initial visit
   with at least three distinct fixes spanning ten seconds. A walking passage
   qualifies; stopping is not required. Very brief or uncertain passages can be
   missed deliberately.
2. Present: further observations leave the count unchanged. Standing overnight,
   crossing midnight, and reopening the app do not create another visit.
3. Away candidate: require reliable fixes at least 100 ground meters beyond the
   stay boundary for at least 60 seconds. With the default radius, this means
   at least 250 meters from its anchor. For route areas, measure from the cell
   boundary. Returning earlier cancels departure.
4. Away: require at least ten minutes of observed absence before a return can
   qualify. On return, apply the same presence confirmation and increment once.
   An earlier return rejoins the existing visit without increasing the count.

These thresholds are starting values, not validated accuracy claims. A normal
walk away and back can add a visit; pacing nearby and repeated short loops do
not. Close neighboring destinations can intentionally remain one stay. GPS
alone cannot identify property boundaries or reliably distinguish rooms and
floors, so this feature measures approximate return frequency, not venue visits.
The first arrival is one visit; revisits equal visits minus one.

Only actual, fresh location observations qualify. Interpolated trail points,
brush radius cells, and nearby road glow never independently count as visits.
Use reported accuracy when testing presence and departure. Uncertain fixes
pause confirmation. Reject stale, duplicate, out-of-order, or implausible fixes.
Deduplicate observations from multiple providers before counting evidence.

Use geographic distance and an uncertainty margin around zone boundaries.
Leaving requires more distance than entering. Nearby boundary jitter can keep
both neighboring zones occupied, but cannot repeatedly rearm either zone.
Persist state and commit increments atomically so service restarts cannot
duplicate a visit. Use monotonic time during a boot; reset pending timers after
a reboot or observation gap. Missing GPS is not proof of departure. A return
that happened entirely while tracking was unavailable may remain uncounted.

GPS alone cannot guarantee perfect classification. Prefer missing an uncertain
return over creating visits while the user is stationary.

## Drawing the heat

Keep visit intensity separate from fog opacity and reveal radius. Project the
existing circular reveal geometry once and reuse it as a clipping mask for the
color layer. Draw location, goal pin, and direction indicators above the result.
Changing zoom or the heat switch must not change the fog mask.

Assign cell intensity to already revealed core geometry. Blend colors within
that geometry to avoid visible square borders. Nearby road-only glow receives
no independent visit count or heat. Legacy areas without measured visits keep
their normal appearance.

At far zoom, aggregate intensity with a coverage-weighted mean, not a sum of
overlapping samples or cells. Preserve revealed coverage separately. A long
single walk must not look like frequent returns simply because it crosses many
cells. Never use a large heat brush to clear fog or a minimum on-screen radius
that expands the geographic reveal area.

Replace the recent-only map query with spatial queries and cached summaries
that include old explored areas. Otherwise frequently visited recent areas
could push old trails out of the overview. Keep database work and aggregation
off the UI thread; query on camera settling rather than every animation frame.

## Storage and privacy

Installing the new APK as an update must preserve the complete existing revealed
world. Keep the application ID and signing identity compatible with the released
app, increase the version code, and use an additive database migration. Create
visit tables alongside existing data; do not drop, rebuild, clear, or reinterpret
the existing visited and reveal tables. Preserve first/last seen values, sample
counts, goal pin, and settings. A migration failure must not fall back to deleting
the database. Installation must not require uninstalling or clearing app data.

Add separate visit statistics keyed by cell and persistent stay state, including
the anchor, radius, stay identifier, confirmed counts, last confirmed arrival,
departure evidence, and each touched cell's last counted stay identifier.
Keep the existing reveal data intact. Store summaries and bounded detection
state; this feature does not require retaining a raw GPS route archive.

Historical sample counts cannot be converted into visit counts. Start measuring
after the update. Existing explored areas stay visible but have unknown visit
frequency until new visits are observed. Settings should state when counting
began, so the heat is not mistaken for complete lifetime history.

Extend the encrypted backup payload with a versioned visit section and support
older backups. Reimporting the same backup must not increase counts. For v1,
merge per-zone counts using the maximum and document that independent device
histories are not additive. Reset live detection evidence on restore; a restore
does not prove departure. This is a single-device feature, not multi-device sync.

All counts stay on the device or inside the user's encrypted backup. No new
permissions, account, server, or location requests are needed. Any future world
sharing must exclude visit frequency by default.

## Acceptance checks

- Hours of standing, realistic GPS drift, boundary pacing, midnight, service
  restart, and reboot do not increase an already confirmed visit count.
- Movement through a large house and garden, including multiple cell borders
  and floors, stays one visit. First observing a new cell does not heat earlier
  cells again. A larger configured stay area also protects a large property.
- Walking steadily away does not drag the stay anchor along. Overlapping areas
  do not grow into one region, and the stay radius never expands fog clearing.
- A reliable departure, qualifying absence, and confirmed return add exactly
  one visit. A new walking passage qualifies without requiring a stop.
- Provider duplicates, poor accuracy, stale fixes, clock changes, observation
  gaps, and isolated jumps do not manufacture departures or returns.
- Old worlds and backups retain all reveal data. Repeated imports are
  idempotent, and historical samples never become claimed visit counts.
- Install the actual currently released APK, populate a representative explored
  world and goal, then install the candidate APK over it with app data retained.
  Compare every existing visited/reveal row before and after migration, check
  settings and goal, and inspect the map before new tracking writes occur.
  Include an older migrated database and a world exceeding 8,000 reveal cells.
  A fresh installation or backup round trip does not replace this upgrade test.
- Heat on/off and zoom transitions preserve identical fog geometry. Compare
  rendered masks, including latitude changes, map rotation, and the date line.
- A long one-time route stays cooler than a repeatedly walked route. Old trails
  remain visible when the stored world exceeds the current 8,000-cell limit.
- Real-device screen recordings confirm smooth transitions and readable pins.
  A stationary soak test and repeated outdoor walks validate the thresholds
  before release; simulated traces alone cannot establish real GPS behavior.

Implementation order: testable visit detector, durable storage and backup,
clipped map overlay and spatial loading, then field tuning and release checks.
