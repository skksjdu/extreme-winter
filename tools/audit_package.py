"""Verify the distributable's metadata, bytecode target and separation from test tooling."""
import hashlib
import json
import pathlib
import struct
import zipfile

root = pathlib.Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text().splitlines()
                  if line and not line.startswith('#'))
version = properties['mod_version']
path = root / f'build/libs/extreme-winter-{version}.jar'
with zipfile.ZipFile(path) as jar:
    names = jar.namelist()
    meta = json.loads(jar.read('fabric.mod.json'))
    assert meta['version'] == version
    assert meta['depends']['minecraft'] == properties['minecraft_version']
    assert meta['depends']['fabricloader'] == '>=' + properties['loader_version']
    assert meta['depends']['fabric-api'] == '>=' + properties['fabric_version']
    assert meta['depends']['java'] == '>=25'
    assert set(meta['depends']) == {'fabricloader', 'minecraft', 'java', 'fabric-api'}
    assert meta['mixins'] == ['extreme_winter.mixins.json']
    mixins = json.loads(jar.read('extreme_winter.mixins.json'))
    assert mixins['mixins'] == ['FurnaceWeatheringMixin', 'HeatItemBarMixin', 'HeatPlacementMixin', 'HeatDropMixin', 'RepairInsulationMixin', 'AnvilInsulationMixin', 'HeatSourceIndexMixin', 'WinterCropGrowthMixin', 'WinterHarvestMixin', 'BerryHarvestMixin', 'WinterRoofProgressMixin', 'WinterCraftingProgressMixin'] and 'client' not in mixins
    assert not any('gametest' in p.lower() or '/test/' in p or p.startswith('META-INF/jars/') for p in names)
    classes = [p for p in names if p.endswith('.class')]
    assert classes
    for name in classes:
        assert struct.unpack('>H', jar.read(name)[6:8])[0] == 69, name
    for name in names:
        if name.endswith('.json'): json.loads(jar.read(name))
    assert not any('starter_shelter' in name or '/shelter/' in name for name in names)
    assert 'assets/extreme_winter/lang/zh_cn.json' in names
    chinese = json.loads(jar.read('assets/extreme_winter/lang/zh_cn.json'))
    english = json.loads(jar.read('assets/extreme_winter/lang/en_us.json'))
    assert set(chinese) == set(english), 'Translations must have matching keys'
    for key in ('durability', 'campfire_pickup', 'furnace_pickup', 'torch_cooldown', 'lava_cooling', 'shelter'):
        assert 'tooltip.extreme_winter.' + key in chinese
    assert 'assets/extreme_winter/blockstates/snow_drift.json' in names
    assert 'data/extreme_winter/loot_table/blocks/snow_drift.json' in names
    for icon in ('empty', 'half', 'full'):
        png = jar.read('assets/extreme_winter/textures/gui/sprites/hud/warmth_' + icon + '.png')
        assert struct.unpack('>II', png[16:24]) == (9, 9)
    pixel_assets = []
    if tuple(map(int, version.split('.'))) >= (26, 0, 7):
        for folder, assets in {
            'item': ('thermal_lining', 'hot_water_bottle', 'warming_stew', 'survival_manual', 'warmth_meter'),
            'block': ('stove_front', 'stove_front_on', 'stove_side', 'stove_top', 'weather_copper')
        }.items():
            for asset in assets:
                name = f'assets/extreme_winter/textures/{folder}/{asset}.png'
                png = jar.read(name)
                assert png[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', png[16:24]) == (16, 16), name
                pixel_assets.append(name)
        for name in names:
            if name.startswith('assets/extreme_winter/models/') and name.endswith('.json'):
                for reference in json.loads(jar.read(name)).get('textures', {}).values():
                    if reference.startswith('extreme_winter:'):
                        assert 'assets/extreme_winter/textures/' + reference.split(':', 1)[1] + '.png' in names, reference
    visuals = [name for name in names if name.startswith('assets/minecraft/')
               or name.startswith('resourcepacks/')]
    assert not visuals, 'Vanilla visuals must not be overridden'
print(json.dumps({'file': str(path), 'bytes': path.stat().st_size,
                  'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'classes': len(classes), 'bytecode_java': 25,
                  'minecraft': meta['depends']['minecraft'], 'test_classes_in_release': False,
                  'mixins': mixins['mixins'], 'shelter_generation': False, 'optional_mod_dependencies': False,
                  'heat_tooltip_languages': ['zh_cn', 'en_us'],
                  'original_pixel_assets': len(pixel_assets),
                  'visual_overrides': len(visuals), 'snow_overrides': False, 'wood_overrides': False}, indent=2))
