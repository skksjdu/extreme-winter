"""Carry the approved, local target configuration forward with newly introduced defaults."""
from pathlib import Path
import argparse, json, re

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--source',required=True,help='Previously reviewed target JSON; never an installed game configuration')
args=parser.parse_args()
source=root/args.source
assert source.resolve().is_relative_to((root/'outputs').resolve()), 'Use an existing delivery JSON in outputs'
previous=json.loads(source.read_text(encoding='utf-8'))
assert previous['configVersion']==3 and previous['maxSnowLayers']==16
text=(root/'src/main/java/dev/extremewinter/config/WinterConfig.java').read_text(encoding='utf-8')
defaults={}
for typ,key,value in re.findall(r'public (int|double|float|boolean|String) (\w+) = ([^;]+);',text):
    value=value.strip()
    defaults[key]=json.loads(value) if typ in ('String','boolean') else float(value.rstrip('f')) if typ in ('double','float') else int(value)
defaults.update(previous)
properties=dict(line.split('=',1) for line in (root/'gradle.properties').read_text().splitlines() if '=' in line)
release_dir=root/'outputs'/'releases'/properties['mod_version']
release_dir.mkdir(parents=True,exist_ok=True)
target=release_dir/f"extreme-winter-{properties['mod_version']}-target-config.json"
result=json.dumps(defaults,ensure_ascii=False,indent=2)+'\n'
if target.exists(): assert target.read_text(encoding='utf-8')==result,'A different existing target configuration must be reviewed explicitly'
else: target.write_text(result,encoding='utf-8')
print(target)
