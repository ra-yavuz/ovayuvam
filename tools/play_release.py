"""Prepare private CI signing files and check a tagged release before Play upload."""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re

PACKAGE = 'tr.ovayuva.ovayuvam'
BUNDLE = Path('app/build/outputs/bundle/release/app-release.aab')
METADATA = Path('app/build/outputs/apk/release/output-metadata.json')


def property_value(value):
    encoded = value.encode('utf-16-be')
    return ''.join('\\u' + encoded[i:i+2].hex() for i in range(0, len(encoded), 2))


def release_identity(metadata, tag):
    if not re.fullmatch(r'v\d+\.\d+\.\d+', tag):
        raise ValueError('Select a version tag such as v0.7.0, not a branch.')
    elements = metadata.get('elements', [])
    if metadata.get('applicationId') != PACKAGE or len(elements) != 1:
        raise ValueError('Unexpected Android package or release outputs.')
    release = elements[0]
    if release.get('versionName') != tag[1:]:
        raise ValueError('The tag does not match the packaged version.')
    code = release.get('versionCode')
    if type(code) is not int or code <= 0:
        raise ValueError('Invalid Android version code.')
    return code


def prepare():
    names = ['PLAY_UPLOAD_KEYSTORE_BASE64', 'PLAY_UPLOAD_STORE_PASSWORD',
             'PLAY_UPLOAD_KEY_ALIAS', 'PLAY_UPLOAD_KEY_PASSWORD']
    values = {name: os.environ.get(name, '') for name in names}
    if not all(values.values()):
        raise ValueError('Configure all Play upload signing secrets before enabling releases.')
    key = base64.b64decode(values[names[0]], validate=True)
    if not key:
        raise ValueError('The upload keystore is empty.')
    directory = Path(os.environ['RUNNER_TEMP']) / 'play-signing'
    directory.mkdir(mode=0o700, exist_ok=False)
    key_path = directory / 'upload.jks'
    key_path.touch(mode=0o600)
    key_path.write_bytes(key)
    properties = {'storeFile': '/run/secrets/upload.jks',
                  'storePassword': values[names[1]], 'keyAlias': values[names[2]],
                  'keyPassword': values[names[3]]}
    path = Path('keystore.properties')
    with path.open('x') as stream:
        os.chmod(path, 0o600)
        stream.write(''.join(name + '=' + property_value(value) + '\n' for name, value in properties.items()))


def verify():
    tag = os.environ['GITHUB_REF_NAME']
    code = release_identity(json.loads(METADATA.read_text()), tag)
    if not BUNDLE.is_file() or BUNDLE.stat().st_size == 0:
        raise ValueError('The release bundle is missing or empty.')
    digest = hashlib.sha256()
    with BUNDLE.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    digest = digest.hexdigest()
    (Path(os.environ['RUNNER_TEMP']) / 'play-bundle.sha256').write_text(f'{digest}  {BUNDLE}\n')
    with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a') as stream:
        stream.write(f'Package: `{PACKAGE}`\n\nVersion: `{tag}` / `{code}`\n\n'
                     f'AAB SHA-256: `{digest}`\n\nTarget: internal testing only.\n\n')
    print(f'Verified {PACKAGE} {tag} (code {code}); bundle SHA-256 {digest}')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['prepare', 'verify'])
    args = parser.parse_args()
    try:
        {'prepare': prepare, 'verify': verify}[args.command]()
    except (ValueError, OSError, KeyError) as error:
        raise SystemExit(str(error)) from None
