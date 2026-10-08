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
        path.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false}}\n', encoding='utf-8')

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
    # Original angular glyphs, three concentric bands and a distinct central emblem.
    x,y=(u-.5)*2,(v-.5)*2;r=math.hypot(x,y);a=math.atan2(y,x)
    edge=sum(.8*math.exp(-((r-rad)/.009)**2) for rad in (.93,.88,.66,.62))
    sector=(a/(2*math.pi)*20)%1; index=int((a+math.pi)/(2*math.pi)*20)
    gx=(sector-.5)*2;gy=(r-.77)/.085
    stem=abs(gx)<.1 and abs(gy)<.8
    branch=abs(gy-(.45 if index%2 else -.4)*gx)<.13 and abs(gx)<.62 and abs(gy)<.7
    cap=abs(gy-.6)<.1 and abs(gx)<(.55 if index%3 else .25)
    glyph=.95 if .68<r<.85 and (stem or branch or cap) else 0
    count=3 if kind=='dark' else 4 if kind=='holy' else 6
    # Polygon/star rather than fine hairline spokes.
    petal=.36+.13*math.cos(a*count)
    emblem=.9*math.exp(-((r-petal)/.016)**2)
    ticks=.7*math.exp(-(math.sin(a*40)/.22)**2)*math.exp(-((r-.905)/.015)**8)
    return edge+glyph+emblem+ticks+.12*math.exp(-((r-.4)/.22)**2)

def cloud(u,v):
    # Pixelated billow with nested value bands; white texture is colored per mesh lobe.
    u,v=(int(u*48)+.5)/48,(int(v*48)+.5)/48
    x,y=(u-.5)*2,(v-.5)*2;r=math.hypot(x,y);a=math.atan2(y,x)
    boundary=.66+.09*math.sin(a*5)+.07*math.cos(a*9+1.3)
    density=max(0,min(1,(boundary-r)*5))
    noise=.8+.2*math.sin(x*19+math.sin(y*13))*math.cos(y*17)
    return round(density*noise*5)/5

def shard(u,v):
    return 1

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
    png(ROOT/'rune_ring.png', 256, lambda u,v:seal(u,v,'arcane'))
    png(ROOT/'healing_column.png', 128, column)
    spells={'holy_seal':lambda u,v:seal(u,v,'holy'),'dark_seal':lambda u,v:seal(u,v,'dark'),
            'nature_seal':lambda u,v:seal(u,v,'nature'),'ribbon':ribbon,'slash':slash,'flare':flare,'shield_grid':shield,'wave':wave,'lightning':lightning,'cloud':cloud,'shard':shard}
    for name,fn in spells.items():png(ROOT/(name+'.png'),256,fn)
    print('Created rune_ring.png and healing_column.png')
    if len(sys.argv)>1:
        preview=Path(sys.argv[1]);preview.mkdir(parents=True,exist_ok=True)
        png(preview/'ring-preview.png',256,ring,True)
        png(preview/'column-preview.png',128,column,True)
        for name,fn in spells.items():png(preview/(name+'-preview.png'),256,fn,True)
