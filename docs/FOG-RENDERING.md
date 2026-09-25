# Fog rendering

The Android map uses local, world-anchored image tiles for revealed history and
visit heat. Saved exploration remains in the existing SQLite database. Rendering
does not migrate, delete, or rewrite that history.

## Tiles and zoom

`FogTiles` divides Web Mercator into 256-pixel tiles. Canonical tile keys wrap at
the date line, so repeated world copies share images. The current camera chooses
a discrete detail level, with at most 48 visible tile placements. A high-density
screen selects an appropriate level instead of rejecting a capped screen image.

`FogTileIndex` puts each circular brush into every level-14 bucket it touches.
Queries at closer zooms inspect only those geographic buckets. Brushes retain
their ground-distance radius, Mercator latitude correction, and irregular edges.
Tile edges do not define reveal edges.

`FogTileCache` paints detailed tiles on a coroutine worker. Levels below 14 are
an image pyramid: each overview downsamples its four children. A sparse uncached
overview with at most 64 brushes can be painted directly, avoiding a deep chain
of mostly empty detail images. Existing child images take precedence. Previously built
overviews do not scan the saved points again. The first view of an uncached area
still requires work. The cache is in memory, so restarting the app rebuilds it
from saved history.

## Updates and memory

The cache compares rendering data, ignoring timestamps and sample increases
beyond the brush's existing strength limit. A changed brush or visit count
invalidates intersecting cached tiles and their affected overview areas. Large
replacements, such as replay changes, clear the cache. Removals are supported so
replay rewinds cannot permanently retain later discoveries.

The image LRU has a 32 MiB budget and a 512-entry limit, including empty entries.
Published frames and in-progress parent images may temporarily retain additional
images. This is a cache limit, not a total process-memory limit. Published images
are immutable; eviction drops references rather than recycling an image that
the drawing thread might still use.

Map frames transform cached images and draw the live position and goal. Camera
state is read during drawing, not by the enclosing composition. A coarse image
is withheld during a large zoom-in until appropriate detail is ready, preventing
an overview pixel from appearing as a large newly explored area.

The UI reads a complete history snapshot on the I/O dispatcher, rather than
replacing tile contents with a viewport subset whenever the map moves. Unchanged
snapshot comparisons also run there. Reads and rendering pause below the
STARTED lifecycle state; the location service is unaffected.

## Tests

`FogTilesTest` covers phone-sized views, stable geographic keys, contiguous tile
extents, wrapped worlds, brush overlap, and spatial updates. `FogTileChecks`
checks actual Android bitmap pixels, cached-object reuse, local invalidation,
overview reuse, tile boundaries, legacy history, replay removal, eviction, and
cancellation. `FogPerformanceChecks` exercises actual MapLibre camera animations
with recorded frame intervals. Emulator results are not a guarantee of frame
rates or battery use on physical phones.
