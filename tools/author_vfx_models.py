"""Original articulated energy models. No third-party geometry or texture is copied."""
import json,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/castigoclasses/models/vfx'
def part(p,to,size,rot=(0,0,0),turn=(0,0,0),delay=0,scale=1,shade=0):
    return dict(position=p,destination=to,size=size,rotation=rot,turn=turn,delay=delay,scale=scale,shade=shade)
models={}
# A crown of luminous feathers unfolds and rises around the healed character.
models['HEALING_BEAM']=[part([math.cos(a)*.5,.05,math.sin(a)*.5],[math.cos(a)*.9,.55,math.sin(a)*.9],[.16,.28,.10],[0,-a*180/math.pi,25],[15,35,-45],i*.008,shade=55) for i in range(12) for a in [i*math.tau/12]]
models['MESH_RING']=[part([math.cos(a)*.82,.03,math.sin(a)*.82],[math.cos(a)*.82,.13,math.sin(a)*.82],[.07,.11,.07],turn=[0,90,0],shade=35) for a in [i*math.tau/8 for i in range(8)]]
# Impact splinters form a radial spearburst, then open into a broken crown.
models['MESH_BURST']=[part([0,.35,0],[math.cos(a)*1.25,.18+(i%3)*.13,math.sin(a)*1.25],[.12,.45,.09],[0,-a*180/math.pi,0],[0,30,65],(i%3)*.025,shade=55 if i%2 else -15) for i in range(12) for a in [i*math.tau/12]]
models['MESH_SHIELD']=[part([0,.5,.45],[(i-2)*.28,.5+(.1 if i in (0,4) else 0),.75],[.28,.75,.13],[0,(i-2)*-12,(i-2)*-9],[0,0,0],i*.015,shade=25) for i in range(5)]
models['MESH_SIGIL']=[part([math.cos(a)*.45,.7,math.sin(a)*.45],[math.cos(a)*.9,.65+(i%2)*.22,math.sin(a)*.9],[.2,.3,.12],[0,0,20],[30,80,20],i*.02,shade=-10) for i in range(6) for a in [i*math.tau/6]]
models['MESH_VORTEX']=[part([math.cos(a)*.9,.05,math.sin(a)*.9],[math.cos(a+2)*.2,.65,math.sin(a+2)*.2],[.18,.32,.1],[0,-a*180/math.pi,45],[25,150,80],i*.012,shade=25) for i in range(12) for a in [i*math.tau/12]]
models['MESH_WAVE']=[part([math.cos(a)*.15,.02,math.sin(a)*.15],[math.cos(a)*.96,.08,math.sin(a)*.96],[.12,.3,.1],[0,-a*180/math.pi,40],[0,0,65],0,shade=35) for a in [i*math.tau/12 for i in range(12)]]
ROOT.mkdir(parents=True,exist_ok=True)
(ROOT/'choreography.json').write_text(json.dumps(models,indent=2)+'\n',encoding='utf8')
print('Created',len(models),'original articulated VFX models')
