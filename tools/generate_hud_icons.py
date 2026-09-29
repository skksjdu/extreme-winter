"""Deterministic 9 x 9 Minecraft-style warmth sprites. Python standard library only."""
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
FLAME = ["....o....", "...oo....", "...oyo...", ".ooyyo...", ".oyyyo.o.",
         "oyyyyyoYo", "oyYyyyYyo", ".oyYYYyo.", "..ooooo.."]
COLORS = {".": (0, 0, 0, 0), "o": (113, 49, 25, 255), "y": (248, 128, 31, 255), "Y": (255, 225, 117, 255)}

def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))

def main():
    target = ROOT / 'src/client/resources/assets/extreme_winter/textures/gui/sprites/hud'
    target.mkdir(parents=True, exist_ok=True)
    for name in ('empty', 'half', 'full'):
        pixels = bytearray()
        for row in FLAME:
            pixels.append(0)
            for x, char in enumerate(row):
                color = COLORS[char]
                if char != '.' and (name == 'empty' or (name == 'half' and x > 4)):
                    color = (54, 46, 41, 255) if char != 'o' else (93, 76, 59, 255)
                pixels.extend(color)
        png = bytes([137, 80, 78, 71, 13, 10, 26, 10])
        png += chunk(b'IHDR', struct.pack('>IIBBBBB', 9, 9, 8, 6, 0, 0, 0))
        png += chunk(b'IDAT', zlib.compress(pixels)) + chunk(b'IEND', b'')
        (target / ('warmth_' + name + '.png')).write_bytes(png)
    print('Generated three 9 x 9 warmth sprites')

if __name__ == '__main__': main()
