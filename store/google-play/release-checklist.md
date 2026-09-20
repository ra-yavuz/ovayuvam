# Google Play release checklist

## Already prepared

- Signed Android App Bundle: `release/ovayuvam-0.5.6.aab`
- AAB SHA-256: `61da7b7b381f92a7e276ca324f14883ccc29350ea670f166c2122fa2064dbbc6`
- Signed APK mirror: `release/ovayuvam-0.5.6.apk`
- APK SHA-256: `69090f199bb28016e5e5f5857031da441dc17302c3ac8e2efcb873e467356618`
- Package: `tr.ovayuva.ovayuvam`
- Version: `0.5.6`
- Version code: `15`
- Privacy policy URL: `https://ovayuva.tr/yuvam/privacy/`
- Store icon draft: `assets/play-icon-512.png`
- Feature graphic draft: `assets/feature-graphic-1024x500.png`
- Store listing copy: `listing-en.md`
- Data safety draft: `data-safety.md`
- App content draft: `app-content.md`
- Permission rationale: `permissions.md`

## Still needed in Play Console

1. Create or access the Google Play developer account.
2. Complete legal identity verification, payments and tax setup as required by Google.
3. Create a new app with package name `tr.ovayuva.ovayuvam`.
4. Choose app or game: `App`.
5. Choose free or paid: likely `Free`.
6. Upload `release/ovayuvam-0.5.6.aab` to internal testing first.
7. Fill the main store listing using `listing-en.md`.
8. Upload store icon and feature graphic from `assets/`.
9. Capture and upload final phone screenshots.
10. Add privacy policy URL: `https://ovayuva.tr/yuvam/privacy/`.
11. Complete App content forms using `data-safety.md`, `app-content.md` and `permissions.md`.
12. Run internal testing on at least one physical Android phone.
13. Promote from internal testing only after install, permission flow, map tile loading, goal pin, direction arrow and foreground notification are checked.

## Owner-only data still needed

- Public developer name.
- Developer legal address.
- Developer support phone number, if Google requires it for the account.
- Payment profile and tax details.
- Whether the release should be private testing first or production.
