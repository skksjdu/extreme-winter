"""Generate a vanilla-model resource pack: rounded, clustered leaves.

No renderer extensions, PBR assumptions, custom shaders, or changes to snow/wood assets.
Python standard library only. Texture coordinates stay within the vanilla block tile.
"""
import json
import pathlib
import random
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
PACK = ROOT / 'src/client/resources/resourcepacks/powder_and_foliage'


def write_json(relative, value):
    path = PACK / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')


def write_png(relative, pixels):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
    path = PACK / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    raw = b''.join(b'\0' + bytes(sum(row, ())) for row in pixels)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 32, 32, 8, 6, 0, 0, 0))
    path.write_bytes(png + chunk(b'IDAT', zlib.compress(raw)) + chunk(b'IEND', b''))


def element(low, high, tint=False, cull=False):
    faces = {}
    for side in ('down', 'up', 'north', 'south', 'west', 'east'):
        face = {'texture': '#all'}
        if tint: face['tintindex'] = 0
        if cull: face['cullface'] = side
        faces[side] = face
    if low[1] > 0: faces.pop('down') # Avoid coplanar internal faces between canopy/cap bands.
    return {'from': low, 'to': high, 'faces': faces}


def main():
    write_json('pack.mcmeta', {'pack': {'pack_format': 63, 'description': 'Extreme Winter · 圆润树叶'}})
    randomizer = random.Random(111)
    # Consume the original generator's snow RNG sequence, keeping existing leaf textures byte-identical.
    for _ in range(32 * 32): randomizer.choice((-1, 0, 0, 1))

    # Eight bands approximate a rounded canopy; ordinary cuboids work in both vanilla and Sodium/Iris.
    elements = []
    for i, inset in enumerate((3.5, 1.75, 0.7, 0.15, 0.15, 0.7, 1.75, 3.5)):
        elements.append(element([inset, i * 2, inset], [16 - inset, i * 2 + 2, 16 - inset], tint=True))
    leaves = ('oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'mangrove', 'cherry',
              'pale_oak', 'azalea', 'flowering_azalea')
    for kind in leaves:
        # Neutral foliage gets the vanilla biome/species tint. Untinted ornamental leaves retain their hue.
        base = (155, 181, 147) if kind == 'pale_oak' else (248, 159, 182) if kind == 'cherry' else (
            (82, 139, 50) if 'azalea' in kind else (170, 170, 170))
        clusters = [(randomizer.randrange(32), randomizer.randrange(32), randomizer.uniform(2.8, 5.3),
                     randomizer.randrange(-28, 20)) for _ in range(55)]
        texture = []
        for y in range(32):
            row = []
            for x in range(32):
                color = (0, 0, 0, 0)
                for cx, cy, radius, shade in clusters:
                    dx = min(abs(x - cx), 32 - abs(x - cx)) / radius
                    dy = min(abs(y - cy), 32 - abs(y - cy)) / (radius * 0.75)
                    if dx * dx + dy * dy < 1:
                        light = shade + int((1 - dy) * 10) - (9 if abs(dx) < 0.12 else 0)
                        color = tuple(max(0, min(255, c + light)) for c in base) + (255,)
                row.append(color)
            texture.append(row)
        if kind in ('cherry', 'pale_oak', 'azalea', 'flowering_azalea'):
            model_elements = json.loads(json.dumps(elements))
            for part in model_elements:
                for face in part['faces'].values(): face.pop('tintindex', None)
        else: model_elements = elements
        if kind == 'flowering_azalea':
            for cx, cy in ((5, 7), (22, 19), (13, 28)):
                for dy in range(-2, 3):
                    for dx in range(-2, 3):
                        if dx * dx + dy * dy <= 5: texture[(cy + dy) % 32][(cx + dx) % 32] = (196, 132, 208, 255)
        name = kind + '_leaves'
        write_png(f'assets/minecraft/textures/block/{name}.png', texture)
        write_json(f'assets/minecraft/models/block/{name}.json', {
            'parent': 'minecraft:block/block', 'ambientocclusion': True,
            'textures': {'all': f'minecraft:block/{name}', 'particle': f'minecraft:block/{name}'},
            'elements': model_elements})
    print('Generated default-enabled resource pack: 11 leaf species; snow and wood untouched')


if __name__ == '__main__': main()
