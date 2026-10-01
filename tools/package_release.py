"""Package an already tested local candidate without overwriting prior releases."""
from pathlib import Path
import argparse, hashlib, json, re, shutil, subprocess, xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--build-log', required=True)
parser.add_argument('--game-log', required=True, action='append')
parser.add_argument('--stage', required=True)
parser.add_argument('--limitation', action='append')
args = parser.parse_args()
logs = [root / args.build_log, *[root / p for p in args.game_log]]
texts = [p.read_text(encoding='utf-8', errors='replace') for p in logs]
for path, text in zip(logs, texts):
    assert 'BUILD SUCCESSFUL' in text and 'BUILD FAILED' not in text, f'Unsuccessful validation: {path}'
audit = json.loads(subprocess.check_output(['python', str(root / 'tools/audit_package.py')], cwd=root, text=True))
version = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines() if '=' in line)['mod_version']
for path, text in zip(logs[1:], texts[1:]):
    assert f'extreme_winter {version}' in text, f'Game log is for another mod version: {path}'
source = Path(audit['file'])
target = root / 'outputs' / source.name
if target.exists():
    assert hashlib.sha256(target.read_bytes()).hexdigest() == audit['sha256'], 'An existing different release must not be overwritten'
else:
    shutil.copyfile(source, target)
assert hashlib.sha256(target.read_bytes()).hexdigest() == audit['sha256']
unit = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
for path in (root / 'build/test-results/test').glob('TEST-*.xml'):
    suite = ET.parse(path).getroot()
    for key in unit:
        unit[key] += int(suite.attrib[key])
assert unit['tests'] > 0 and unit['failures'] == unit['errors'] == unit['skipped'] == 0
limitations = args.limitation or ['User subjective playtest pending']
if args.stage != 'E':
    limitations.append('Final compatibility and performance matrix reserved for stage E')
evidence = dict(audit, version=version, stage=args.stage, file=str(target), unit=unit,
                logs=[str(p.relative_to(root)).replace('\\', '/') for p in logs],
                log_sha256={str(p.relative_to(root)).replace('\\', '/'): hashlib.sha256(p.read_bytes()).hexdigest() for p in logs},
                game_observations=[line for text in texts[1:] for line in text.splitlines() if re.search(r'TEST .*PASSED', line)],
                installed_in_user_instance=False, pushed=False, published=False,
                limitations=limitations)
(root / f'outputs/package-{version}.json').write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
(root / f'outputs/extreme-winter-{version}.sha256').write_text(audit['sha256'] + '  ' + target.name + '\n', encoding='ascii')
print(json.dumps({'version': version, 'sha256': audit['sha256'], 'unit': unit, 'game_observations': len(evidence['game_observations'])}, indent=2))
