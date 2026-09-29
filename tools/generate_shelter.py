"""Compile the editable layer blueprint into a vanilla gzip NBT structure. Python stdlib only."""
import gzip
import json
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]

def string(value):
    value = value.encode('utf-8')
    return struct.pack('>H', len(value)) + value

def payload(kind, value):
    if kind == 1: return struct.pack('>b', value)
    if kind == 3: return struct.pack('>i', value)
    if kind == 8: return string(value)
    if kind == 9:
        child, values = value
        return bytes([child]) + struct.pack('>i', len(values)) + b''.join(payload(child, x) for x in values)
    if kind == 10:
        return b''.join(bytes([k]) + string(name) + payload(k, v) for name, (k, v) in value.items()) + b'\0'
    raise ValueError(kind)

def main():
    spec = json.loads((ROOT / 'structures/starter_shelter.json').read_text(encoding='utf-8'))
    width, height, depth = spec['size']
    assert len(spec['layers']) == height
    states = []
    indices = {}
    for key, raw in spec['palette'].items():
        name, _, props = raw.partition('[')
        state = {'Name': (8, name)}
        if props:
            state['Properties'] = (10, {k: (8, v) for k, v in (p.split('=') for p in props.rstrip(']').split(','))})
        indices[key] = len(states)
        states.append(state)
    blocks = []
    for y, layer in enumerate(spec['layers']):
        assert len(layer) == depth
        for z, row in enumerate(layer):
            assert len(row) == width, (y, z, row, len(row))
            for x, key in enumerate(row):
                # Omit air outside the front wall so the entrance does not carve terrain.
                if z == 0 and key == '.': continue
                block = {'pos': (9, (3, [x, y, z])), 'state': (3, indices[key])}
                if [x, y, z] == spec['chest']['position']:
                    items = [{'Slot': (1, i), 'id': (8, name), 'count': (3, count)}
                             for i, (name, count) in enumerate(spec['chest']['items'])]
                    block['nbt'] = (10, {'id': (8, 'minecraft:chest'), 'Items': (9, (10, items))})
                blocks.append(block)
    # Data version is read from the verified 1.21.6 game version metadata.
    data = {'DataVersion': (3, 4435), 'size': (9, (3, spec['size'])),
            'palette': (9, (10, states)), 'blocks': (9, (10, blocks)), 'entities': (9, (10, []))}
    target = ROOT / 'src/main/resources/data/extreme_winter/structure/starter_shelter.nbt'
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(gzip.compress(bytes([10]) + string('') + payload(10, data), mtime=0))
    print(f'{target}: {len(blocks)} blocks, {len(states)} states, {target.stat().st_size} bytes')

if __name__ == '__main__': main()
