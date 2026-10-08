"""Rebuild original, tintable RGBA VFX textures; Python standard library only."""
import math
import struct
import zlib
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/castigoclasses/textures/vfx'

def png(path, size, alpha, preview=False):
    def chunk(kind, data):
        return struct.pack('!I', len(data)) + kind + data + struct.pack('!I', zlib.crc32(kind + data))
    pixels = bytearray()
    for y in range(size):
        pixels.append(0)
        for x in range(size):
            a = round(255 * max(0, min(1, alpha((x+.5)/size, (y+.5)/size))))
            pixels.extend(tuple(round(18+(c-18)*a/255) for c in (85,255,102))+(255,) if preview else (255,255,255,a))
    path.write_bytes(b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', size, size, 8, 6, 0, 0, 0))
                     + chunk(b'IDAT', zlib.compress(pixels, 9)) + chunk(b'IEND', b''))
    if not preview:
        path.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":true}}\n', encoding='utf-8')

def ring(u, v):
    x, y = (u-.5)*2, (v-.5)*2
    r, a = math.hypot(x, y), math.atan2(y, x)
    # Two narrow circles with soft halos; segmented geometric glyphs in between.
    lines = sum(.75*math.exp(-((r-t)/.008)**2) + .2*math.exp(-((r-t)/.025)**2) for t in (.73, .88))
    spoke = math.exp(-(math.sin(a*12)/.13)**2) * math.exp(-((r-.8)/.045)**8)
    arc = math.exp(-((r-.8)/.007)**2) * max(0, math.cos(a*24))**8
    return lines + .8*spoke + .5*arc

def column(u, v):
    a, t = 2*math.pi*u, 2*math.pi*v
    # Periodic in both axes, avoiding a visible seam during UV scrolling.
    threads = (.5+.5*math.sin(9*a+.35*math.sin(t)))**12
    ribbons = (.5+.5*math.sin(4*a+.6*math.sin(t)+.2*math.cos(2*t)))**4
    return .13 + .42*ribbons + .43*threads

def seal(u,v,kind):
    x,y=(u-.5)*2,(v-.5)*2;r=math.hypot(x,y);a=math.atan2(y,x)
    edge=.8*math.exp(-((r-.83)/.014)**2)+.25*math.exp(-((r-.83)/.04)**2)
    spokes=math.exp(-(math.sin(a*(3 if kind=='dark' else 4 if kind=='holy' else 6))/.1)**2)*math.exp(-((r-.53)/.24)**8)
    inner=.6*math.exp(-((r-(.34+.07*math.cos(a*(3 if kind=='dark' else 6))))/.012)**2)
    return edge+.75*spokes+inner

def ribbon(u,v):
    # U along the stroke, V across it. Transparent borders hide the quad edges.
    return math.sin(math.pi*u)**.35*math.exp(-((v-.5)/.16)**2)

def slash(u,v):
    return math.sin(math.pi*u)**.2*math.exp(-((v-.5)/.23)**2)*(.65+.35*math.sin(u*math.pi*3)**2)

def flare(u,v):
    x,y=(u-.5)*2,(v-.5)*2;r=math.hypot(x,y)
    return .9*math.exp(-(r/.24)**2)+.5*math.exp(-(x/.025)**2-(y/.7)**2)+.5*math.exp(-(y/.025)**2-(x/.7)**2)

def shield(u,v):
    # Transparent panel rim and a central diamond.
    edge=min(u,v,1-u,1-v)
    diamond=abs(abs(u-.5)+abs(v-.5)-.32)
    return .045+.55*math.exp(-(edge/.018)**2)+.35*math.exp(-(diamond/.013)**2)

def wave(u,v):
    r=math.hypot((u-.5)*2,(v-.5)*2)
    return .85*math.exp(-((r-.83)/.025)**2)+.22*math.exp(-((r-.77)/.08)**2)

def lightning(u,v):
    center=.5+.15*math.sin(u*math.pi*14)*math.sin(math.pi*u)
    return math.sin(math.pi*u)**.3*(math.exp(-((v-center)/.025)**2)+.35*math.exp(-((v-center)/.1)**2))

if __name__ == '__main__':
    ROOT.mkdir(parents=True, exist_ok=True)
    png(ROOT/'rune_ring.png', 256, ring)
    png(ROOT/'healing_column.png', 128, column)
    spells={'holy_seal':lambda u,v:seal(u,v,'holy'),'dark_seal':lambda u,v:seal(u,v,'dark'),
            'nature_seal':lambda u,v:seal(u,v,'nature'),'ribbon':ribbon,'slash':slash,'flare':flare,'shield_grid':shield,'wave':wave,'lightning':lightning}
    for name,fn in spells.items():png(ROOT/(name+'.png'),256,fn)
    print('Created rune_ring.png and healing_column.png')
    if len(sys.argv)>1:
        preview=Path(sys.argv[1]);preview.mkdir(parents=True,exist_ok=True)
        png(preview/'ring-preview.png',256,ring,True)
        png(preview/'column-preview.png',128,column,True)
        for name,fn in spells.items():png(preview/(name+'-preview.png'),256,fn,True)
