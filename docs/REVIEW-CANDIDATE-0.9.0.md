# ovayuvam 0.9.0 Review Candidate

Status: local review candidate, version code 29. This document does not announce
a published Google Play or website release.

## Changes

- First-launch location disclosure before runtime permission requests. Choosing
  "Not now" leaves tracking off and still allows viewing the map.
- Persistent pause controls in settings and the ongoing notification. Relaunching
  does not undo a pause. Tracking resumes only after the user enables it.
- English, German, Turkish, Russian, Spanish and French. The default follows the
  phone. The language picker works offline; all six languages ship in the bundle.
- Fog opacity defaults to 82%, adjustable between 55% and 96%.
- Planned paths use the hand-drawn pen. Undo removes the last stroke; clearing
  plans requires confirmation. Drawing is available at street-level zoom.
- Plans lighten at most 40% of the remaining fog. Overlapping strokes do not
  accumulate extra lightening. Real discoveries still clear the fog normally.
- Weekly exploration defaults to on. A previously saved off setting stays off.
- Stay detection uses a fixed 150-metre radius, without a user-facing slider.

## Data and Rendering

Database schema 4 adds a separate planned-path table. Existing discoveries,
visit counts and stay zones are retained. Plans are not visits, discoveries,
heatmap input or replay history. Encrypted backups include plans in an optional
field; older v1 and v2 backups remain readable. Older app versions ignore the
new optional field when opening a new backup.

Saved plans use a separate 8 MiB geographic tile cache. Live strokes do not
rebuild that cache. Their mask is combined with saved plans before applying the
lightening limit, so painting over an existing plan cannot fully reveal it.
Both the brush width and the saved tiles are tied to map coordinates, not zoom.

The pen bitmap is reused from the ovayuva artwork with the project owner's
express permission. No proprietary ovayuva implementation code is included.

## Checks

Run `python3 tools/check_translations.py` to validate language keys, format
placeholders and notification collections. Run the verification unit tests,
Android instrumentation checks and release lint before using a signed candidate.

The `reviewCandidate` instrumentation flag checks consent and pause preferences,
the weekly default, path persistence, encrypted restore, discovery separation,
fog pixels, tile reuse and the six resource sets. Actual permission dialogs,
service stopping, language selection and layout require the emulator UI checks.
These checks do not establish physical-device battery life or Play approval.
