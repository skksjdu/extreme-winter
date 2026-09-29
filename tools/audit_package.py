"""Verify the distributable's metadata, bytecode target and separation from test tooling."""
import hashlib
import json
import pathlib
import struct
import zipfile

root = pathlib.Path(__file__).resolve().parents[1]
path = root / 'build/libs/extreme-winter-1.0.0.jar'
with zipfile.ZipFile(path) as jar:
    names = jar.namelist()
    meta = json.loads(jar.read('fabric.mod.json'))
    assert meta['version'] == '1.0.0'
    assert meta['depends']['minecraft'] == '1.21.6'
    assert set(meta['depends']) == {'fabricloader', 'minecraft', 'java', 'fabric-api'}
    assert 'mixins' not in meta
    assert not any('gametest' in p.lower() or '/test/' in p or p.startswith('META-INF/jars/') for p in names)
    classes = [p for p in names if p.endswith('.class')]
    assert classes
    for name in classes:
        assert struct.unpack('>H', jar.read(name)[6:8])[0] == 65, name
    for name in names:
        if name.endswith('.json'): json.loads(jar.read(name))
    assert 'data/extreme_winter/structure/starter_shelter.nbt' in names
    assert 'assets/extreme_winter/lang/zh_cn.json' in names
print(json.dumps({'file': str(path), 'bytes': path.stat().st_size,
                  'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                  'classes': len(classes), 'bytecode_java': 21,
                  'minecraft': meta['depends']['minecraft'], 'test_classes_in_release': False,
                  'mixins': False, 'optional_mod_dependencies': False}, indent=2))
