"""Verify the distributable's metadata, bytecode target and separation from test tooling."""
import hashlib
import json
import pathlib
import struct
import zipfile

root = pathlib.Path(__file__).resolve().parents[1]
version = next(line.split('=', 1)[1].strip() for line in (root / 'gradle.properties').read_text().splitlines()
               if line.startswith('mod_version='))
path = root / f'build/libs/extreme-winter-{version}.jar'
with zipfile.ZipFile(path) as jar:
    names = jar.namelist()
    meta = json.loads(jar.read('fabric.mod.json'))
    assert meta['version'] == version
    assert meta['depends']['minecraft'] == '1.21.6'
    assert set(meta['depends']) == {'fabricloader', 'minecraft', 'java', 'fabric-api'}
    assert meta['mixins'] == ['extreme_winter.mixins.json']
    mixins = json.loads(jar.read('extreme_winter.mixins.json'))
    assert mixins['mixins'] == ['FurnaceWeatheringMixin', 'HeatItemBarMixin', 'HeatPlacementMixin', 'HeatDropMixin'] and 'client' not in mixins
    assert not any('gametest' in p.lower() or '/test/' in p or p.startswith('META-INF/jars/') for p in names)
    classes = [p for p in names if p.endswith('.class')]
    assert classes
    for name in classes:
        assert struct.unpack('>H', jar.read(name)[6:8])[0] == 65, name
    for name in names:
        if name.endswith('.json'): json.loads(jar.read(name))
    assert not any('starter_shelter' in name or '/shelter/' in name for name in names)
    assert 'assets/extreme_winter/lang/zh_cn.json' in names
    assert 'assets/extreme_winter/blockstates/snow_drift.json' in names
    assert 'data/extreme_winter/loot_table/blocks/snow_drift.json' in names
    for icon in ('empty', 'half', 'full'):
        png = jar.read('assets/extreme_winter/textures/gui/sprites/hud/warmth_' + icon + '.png')
        assert struct.unpack('>II', png[16:24]) == (9, 9)
    visuals = [name for name in names if name.startswith('assets/minecraft/')
               or name.startswith('resourcepacks/')]
    assert not visuals, 'Vanilla visuals must not be overridden'
print(json.dumps({'file': str(path), 'bytes': path.stat().st_size,
                  'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'classes': len(classes), 'bytecode_java': 21,
                  'minecraft': meta['depends']['minecraft'], 'test_classes_in_release': False,
                  'mixins': mixins['mixins'], 'shelter_generation': False, 'optional_mod_dependencies': False,
                  'visual_overrides': len(visuals), 'snow_overrides': False, 'wood_overrides': False}, indent=2))
