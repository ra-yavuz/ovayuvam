import unittest
import base64
import os
from pathlib import Path
import tempfile
from unittest.mock import patch
from play_release import PACKAGE, prepare, property_value, release_identity


class PlayReleaseTest(unittest.TestCase):
    def metadata(self, **changes):
        return {'applicationId': PACKAGE, 'elements': [dict(versionName='0.7.0', versionCode=24, **changes)]}

    def test_matching_tag(self):
        self.assertEqual(24, release_identity(self.metadata(), 'v0.7.0'))

    def test_wrong_package(self):
        metadata = self.metadata()
        metadata['applicationId'] = 'tr.ovayuva.app'
        with self.assertRaises(ValueError): release_identity(metadata, 'v0.7.0')

    def test_wrong_tag_or_branch(self):
        for tag in ['main', 'v0.7.1', 'v0.7.0\n', 'v0.7.0;echo unsafe']:
            with self.assertRaises(ValueError): release_identity(self.metadata(), tag)

    def test_invalid_code_or_multiple_outputs(self):
        for code in [0, -1, True, '24']:
            metadata = self.metadata()
            metadata['elements'][0]['versionCode'] = code
            with self.assertRaises(ValueError): release_identity(metadata, 'v0.7.0')
        metadata = self.metadata()
        metadata['elements'] *= 2
        with self.assertRaises(ValueError): release_identity(metadata, 'v0.7.0')

    def test_property_values_cannot_inject_another_property(self):
        value = ' secret\\key\nother=bad:!#\t\u00e9'
        escaped = property_value(value)
        self.assertNotIn('\n', escaped)
        self.assertNotIn('=', escaped)
        self.assertEqual(value, bytes.fromhex(escaped.replace('\\u', '')).decode('utf-16-be'))

    def test_missing_signing_secrets_fail_closed(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(ValueError): prepare()

    def test_signing_files_are_private_and_existing_files_are_not_overwritten(self):
        previous = Path.cwd()
        with tempfile.TemporaryDirectory() as directory:
            try:
                os.chdir(directory)
                with patch.dict(os.environ, {
                    'RUNNER_TEMP': directory,
                    'PLAY_UPLOAD_KEYSTORE_BASE64': base64.b64encode(b'test-only-key').decode(),
                    'PLAY_UPLOAD_STORE_PASSWORD': 'test-only-password',
                    'PLAY_UPLOAD_KEY_ALIAS': 'test-only-alias',
                    'PLAY_UPLOAD_KEY_PASSWORD': 'test-only-password',
                }):
                    prepare()
                    key = Path(directory) / 'play-signing/upload.jks'
                    self.assertEqual(b'test-only-key', key.read_bytes())
                    self.assertEqual(0o600, key.stat().st_mode & 0o777)
                    self.assertEqual(0o600, Path('keystore.properties').stat().st_mode & 0o777)
                    with self.assertRaises(FileExistsError): prepare()
            finally:
                os.chdir(previous)
