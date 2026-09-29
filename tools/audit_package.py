"""Verify the distributable's metadata, bytecode target and separation from test tooling."""
import hashlib
import json
import pathlib
import struct
import zipfile

root = pathlib.Path(__file__).resolve().parents[1]
path = root / 'build/libs/extreme-winter-1.1.1.jar'
with zipfile.ZipFile(path) as jar:
    names = jar.namelist()
    meta = json.loads(jar.read('fabric.mod.json'))
    assert meta['version'] == '1.1.1'
    assert meta['depends']['minecraft'] == '1.21.6'
    assert set(meta['depends']) == {'fabricloader', 'minecraft', 'java', 'fabric-api'}
    assert meta['mixins'] == ['extreme_winter.mixins.json']
    mixins = json.loads(jar.read('extreme_winter.mixins.json'))
    assert mixins['mixins'] == ['FurnaceWeatheringMixin'] and 'client' not in mixins
    assert not any('gametest' in p.lower() or '/test/' in p or p.startswith('META-INF/jars/') for p in names)
    classes = [p for p in names if p.endswith('.class')]
    assert classes
    for name in classes:
        assert struct.unpack('>H', jar.read(name)[6:8])[0] == 65, name
    for name in names:
        if name.endswith('.json'): json.loads(jar.read(name))
    assert 'data/extreme_winter/structure/starter_shelter.nbt' in names
    assert 'assets/extreme_winter/lang/zh_cn.json' in names
    assert 'assets/extreme_winter/blockstates/snow_drift.json' in names
    assert 'data/extreme_winter/loot_table/blocks/snow_drift.json' in names
    for icon in ('empty', 'half', 'full'):
        png = jar.read('assets/extreme_winter/textures/gui/sprites/hud/warmth_' + icon + '.png')
        assert struct.unpack('>II', png[16:24]) == (9, 9)
    pack = 'resourcepacks/powder_and_foliage/'
    assert pack + 'pack.mcmeta' in names
    visuals = [name.removeprefix(pack + 'assets/minecraft/') for name in names
               if name.startswith(pack + 'assets/minecraft/') and not name.endswith('/')]
    assert len(visuals) == 22, visuals
    assert all('leaves' in name for name in visuals), 'Unexpected snow/wood/other overrides'
print(json.dumps({'file': str(path), 'bytes': path.stat().st_size,
                  'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'classes': len(classes), 'bytecode_java': 21,
                  'minecraft': meta['depends']['minecraft'], 'test_classes_in_release': False,
                  'server_furnace_mixin_only': True, 'optional_mod_dependencies': False,
                  'visual_overrides': len(visuals), 'snow_overrides': False, 'wood_overrides': False}, indent=2))
