# ovayuvam 0.10.1

Package: `tr.ovayuva.ovayuvam`. Version code: 32.
Scope: testing tracks, not a production launch.

The logo and explored-area badge uses less space on the map. The fixed
236 by 80 dp frame is replaced by a content-sized frame with a 48 dp minimum
tap target. Inner padding is reduced from 12 to 8 dp horizontally and from
8 to 4 dp vertically. The icon and gap beside the title are also smaller.

Both faces are measured together so their dimensions stay the same during a
flip. Larger text and long area values can wrap and increase the height.
Only the visible face is read by accessibility services.

Area calculations, saved maps, backups, notifications and the remembered
badge choice are unchanged. No storage migration is introduced.

## Verification

All 84 unit tests passed. Release lint reported zero errors and 50 warnings.
Native map-control checks passed, including compact dimensions and stable
dimensions across flips. Screens were checked in all six supported languages.
Narrow-screen checks at 130% and 200% text size passed, with room for both
totals. The selected face remained saved after force-stop and relaunch.

A signed update from 0.10.0 preserved all rows in the test world: 1,681 visited
cells, 10,201 reveal cells, one visit count, one stay zone and one planned route.
These are emulator checks, not a claim that every physical phone was tested.
The APK retains the existing signing certificate.

## Artifact Identity

- APK SHA-256: `a6e435dcbbee01602af4de6924af3b485a2235ce45c6673b0c178fb3c14f6807`
- App Bundle SHA-256: `71744464f21f006ffbc734d4fd454247c476f65623c2fff90eaa1d27cbe4f814`

The public GitHub prerelease contains source only, without APK or App Bundle
attachments. Production publishing is outside this release's scope.
