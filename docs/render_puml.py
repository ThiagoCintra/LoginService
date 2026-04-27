#!/usr/bin/env python3
# Renders PlantUML .puml files to PNG using plantuml public server.
# Usage: python3 render_puml.py file.puml

import sys
import zlib
import base64
import urllib.request

ENCODE_TABLE = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-_"

def encode6bit(b):
    if b < 0 or b >= 64:
        return '?'
    return ENCODE_TABLE[b]

def append3bytes(b1, b2, b3):
    c1 = b1 >> 2
    c2 = ((b1 & 0x3) << 4) | (b2 >> 4)
    c3 = ((b2 & 0xF) << 2) | (b3 >> 6)
    c4 = b3 & 0x3F
    r = ''.join(encode6bit(c) for c in (c1, c2, c3, c4))
    return r

def plantuml_encode(data: bytes) -> str:
    compressed = zlib.compress(data)
    # The PlantUML server expects the "raw" DEFLATE (no zlib header)
    # Python's zlib.compress includes header; remove the first 2 bytes and last 4 bytes
    # Alternative: use compressobj with -15 window bits to get raw deflate
    compressor = zlib.compressobj(level=9, wbits=-15)
    raw = compressor.compress(data) + compressor.flush()
    res = ''
    for i in range(0, len(raw), 3):
        b1 = raw[i]
        b2 = raw[i+1] if i+1 < len(raw) else 0
        b3 = raw[i+2] if i+2 < len(raw) else 0
        res += append3bytes(b1, b2, b3)
    return res


def render(puml_path):
    with open(puml_path, 'rb') as f:
        text = f.read()
    encoded = plantuml_encode(text)
    url = f"https://www.plantuml.com/plantuml/png/{encoded}"
    print('Requesting', url)
    try:
        with urllib.request.urlopen(url) as resp:
            data = resp.read()
        out = puml_path.rsplit('.', 1)[0] + '.png'
        with open(out, 'wb') as f:
            f.write(data)
        print('Saved', out)
    except Exception as e:
        print('Failed to render', puml_path, 'error', e)

if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: render_puml.py file1.puml [file2.puml ...]')
        sys.exit(1)
    for p in sys.argv[1:]:
        render(p)
