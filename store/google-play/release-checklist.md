# Google Play release checklist

## Already prepared

- Signed Android App Bundle: `release/ovayuvam-0.5.2.aab`
- AAB SHA-256: `e2b02154911d6ecd06a7e20d059ae3bde9ee5c8fd383f6ed1c26b774be91d9fd`
- Signed APK mirror: `release/ovayuvam-0.5.2.apk`
- APK SHA-256: `a015dd6f288beedfeed1d6929ddea88fc862bef92510428358531f14ed3e4515`
- Package: `tr.ovayuva.ovayuvam`
- Version: `0.5.2`
- Version code: `11`
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
6. Upload `release/ovayuvam-0.5.2.aab` to internal testing first.
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
