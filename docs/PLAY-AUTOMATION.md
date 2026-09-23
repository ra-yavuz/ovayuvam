# Automatic Internal Releases

Audience: the ovayuvam maintainer. Purpose: remove repeated manual bundle uploads
while keeping production publishing manual.

The `play-internal.yml` workflow is prepared but disabled until access is set up.
It has not yet made a verified Play upload. Once enabled, pushing a `vX.Y.Z` tag
on a commit in `main` builds and signs the app in Docker, runs release tests and
lint, checks its signing certificate and version, then submits the bundle to
the `internal` testing track with status `completed`. It does not upload APKs or
bundles to public GitHub release assets, edit store listings, or target production.

Google still controls review and availability. A successful API commit is not
proof that every tester can already download the update. Policy failures stop
the workflow; they are not silently converted into draft releases.

## One-Time Google Setup

1. Reuse the existing ovayuvam Google Cloud project. Enable the Google Play Android Developer
   API: <https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com>.
2. Use a dedicated service account for ovayuvam releases. It does not need project Owner,
   billing access, or broad administrator roles.
3. In Play Console, open **Users and permissions**, invite the service account's
   email, and give it access to **ovayuvam only**. Grant view app information and
   release to testing tracks. If the app is still a draft, grant the draft-app
   permission only as needed. Do not grant production or financial permissions.
4. Create a JSON key for this account and keep it outside all repositories.
   Never paste its contents into chat, source files, or release notes.
5. Confirm the app already exists in Play Console, its initial bundle has been
   uploaded, and the internal tester list is configured. Existing Console and
   policy requirements must still be completed.

Google's official setup guide:
<https://developers.google.com/android-publisher/getting_started>.
App-specific permissions:
<https://developers.google.com/android-publisher/api-ref/rest/v3/grants>.

## One-Time GitHub Setup

Create the `google-play` environment in this repository. Limit its deployment
tags to `v*`, restrict who can push release tags, and protect `main`. Do not add
a required reviewer if internal releases should be fully automatic.

Store these as environment secrets, not repository files:

- `PLAY_SERVICE_ACCOUNT_JSON`: the dedicated service account's JSON key.
- `PLAY_UPLOAD_KEYSTORE_BASE64`: base64 of the existing ovayuvam upload keystore.
- `PLAY_UPLOAD_STORE_PASSWORD`: that keystore's password.
- `PLAY_UPLOAD_KEY_ALIAS`: its existing upload-key alias.
- `PLAY_UPLOAD_KEY_PASSWORD`: the upload key's password.

Use the existing ovayuvam signing key, not ovayuva's private app key. The workflow
checks certificate SHA-256
`41a682a94ae3098fb09bf3e984be9c591f3093329618d057a59b3f922719873e`.
Confirm that this is the upload certificate accepted by Play for this app.
The owner can revoke or rotate the dedicated Google credential independently.

Only after the secrets and permissions are ready, set the **repository variable**
`PLAY_INTERNAL_RELEASES_ENABLED` to `true`. It must be a repository variable, not
an environment variable, because the job checks it before opening the environment.

The workflow uses pinned action commits. The third-party uploader receives the
dedicated app-scoped Google credential. Its implementation and settings are at
<https://github.com/r0adkll/upload-google-play>. Keep tag and workflow write access
limited to trusted maintainers. Keyless Google authentication can replace the
JSON key later without changing the internal-only release policy.

## Each Release

Increase Android `versionCode` and `versionName`. Update
`store/google-play/whatsnew/whatsnew-en-US`, complete the local signed-device release
checks, commit to `main`, then push the matching version tag. Do not reuse a
version code already uploaded manually or by another workflow.

The workflow builds its own signed bundle from the tagged source. Its SHA-256
appears in the job summary; it is not assumed to match a separately built local
bundle byte for byte. No source edits are made during the workflow.

For the first upload of an already tagged release, use **Actions > Release to
Play internal testing > Run workflow** and select that tag. Do not run it on
`main`. Re-running an already accepted version can fail because Play version
codes cannot be reused. Inspect the Console before retrying an uncertain result.

Matrix release updates from the coding session can include the workflow result.
This workflow does not store the session's encrypted Matrix identity or send
Matrix messages itself.

## Limits

This automation does not complete the separate consent, tracking controls, physical
device, native compatibility, or policy checks needed for Play distribution. It does not
assert Play approval or production readiness. Disable automatic uploads by
setting `PLAY_INTERNAL_RELEASES_ENABLED` to `false`.
