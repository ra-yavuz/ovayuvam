"""Validate shipped language resources and Android format placeholders."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

resources = Path(__file__).resolve().parents[1] / 'app/src/main/res'
base = {item.attrib['name']: ''.join(item.itertext()) for item in ET.parse(resources/'values/strings.xml').getroot()}
placeholder = re.compile(r'%(?:\d+\$)?[dsf]')
for language in ['de', 'tr', 'ru', 'es', 'fr']:
    translated = {item.attrib['name']: ''.join(item.itertext()) for item in ET.parse(resources/f'values-{language}/strings.xml').getroot()}
    assert translated.keys() == base.keys(), (language, base.keys() ^ translated.keys())
    for key, text in translated.items():
        assert text.strip(), (language, key)
        assert placeholder.findall(text) == placeholder.findall(base[key]), (language, key)
    arrays = ET.parse(resources/f'values-{language}/notifications.xml').getroot()
    assert {a.attrib['name'] for a in arrays} == {'notification_everyday', 'notification_progress', 'notification_quiet'}
    assert all(len(a) >= 10 and all(''.join(i.itertext()).strip() for i in a) for a in arrays)
print(f'PASS: {len(base)} strings in six languages; notification collections and placeholders complete')
