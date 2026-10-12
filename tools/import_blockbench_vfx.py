"""Compile user-owned Generic Blockbench models; never execute archive contents.

Keeps hierarchical keyframes, face UVs and texture sheets. References are excluded.
Usage: python import_blockbench_vfx.py ARCHIVE [RESOURCE_DIRECTORY]
"""
import base64, collections, json, math, pathlib, re, sys, zipfile

def rotate(v, r):
    # THREE Euler XYZ = Rx Ry Rz (rightmost transform first).
    x,y,z=v
    for axis,angle in reversed(list(enumerate(r))):
        c,s=math.cos(math.radians(angle)),math.sin(math.radians(angle))
        if axis==0:y,z=c*y-s*z,s*y+c*z
        elif axis==1:x,z=c*x+s*z,-s*x+c*z
        else:x,y=c*x-s*y,s*x+c*y
    return [x,y,z]

def compile_model(d, ident, out):
    groups={g['uuid']:g for g in d['groups']}; elements={e['uuid']:e for e in d['elements']}
    nodes=[]; indices={}; textures=[]
    for i,t in enumerate(d['textures']):
        path=f'textures/blockbench/{ident}/{i}.png'
        dest=out/'assets/castigoclasses'/path;dest.parent.mkdir(parents=True,exist_ok=True)
        raw=base64.b64decode(t['source'].split(',',1)[1],validate=True)
        assert raw.startswith(b'\x89PNG\r\n\x1a\n')
        dest.write_bytes(raw)
        frames=max(1,round(t['height']/t['width'])) if t['uv_width']==t['uv_height'] else 1
        textures.append(dict(path='castigoclasses:'+path,frames=frames,ticks=max(1,t.get('frame_time',1))))
    def walk(entry,parent=-1,hidden=False):
        if isinstance(entry,str):
            e=elements[entry]
            if hidden or not e.get('export',True) or not e.get('visibility',True):return
            origin=e.get('origin',[0,0,0]); rot=e.get('rotation',[0,0,0]); quads=[]
            def add(points,uvs,tex):
                t=d['textures'][tex];pv=nodes[parent]['origin'] if parent>=0 else [0,0,0]
                vs=[]
                for point,uv in zip(points,uvs):
                    local=point if e['type']=='mesh' else [point[j]-origin[j] for j in range(3)]
                    pos=rotate(local,rot)
                    vs.extend([round((pos[j]+origin[j]-pv[j])/16,6) for j in range(3)]+[round(uv[0]/t['uv_width'],6),round(uv[1]/t['uv_height'],6)])
                if len(points)==3:vs.extend(vs[-5:])
                quads.append([tex,vs])
            if e['type']=='cube':
                x,y,z=e['from'];X,Y,Z=e['to'];inf=e.get('inflate',0);x-=inf;y-=inf;z-=inf;X+=inf;Y+=inf;Z+=inf
                faces={'north':[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],'south':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],'west':[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],'east':[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],'up':[(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],'down':[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]}
                for name,f in e['faces'].items():
                    if f.get('texture') is None:continue
                    u,v,U,V=f['uv'];uvs=[[u,V],[U,V],[U,v],[u,v]];shift=int(f.get('rotation',0)/90)%4;uvs=uvs[shift:]+uvs[:shift]
                    add(faces[name],uvs,int(f['texture']))
            else:
                assert e['type']=='mesh',e['type']
                for f in e['faces'].values():
                    if f.get('texture') is None or len(f['vertices'])<3:continue
                    vs=f['vertices']
                    polygons=[vs] if len(vs)<=4 else [[vs[0],vs[i],vs[i+1]] for i in range(1,len(vs)-1)]
                    for polygon in polygons:add([e['vertices'][v] for v in polygon],[f['uv'][v] for v in polygon],int(f['texture']))
            if parent<0:
                parent=len(nodes);nodes.append(dict(name='ungrouped',parent=-1,origin=[0,0,0],rotation=[0,0,0],quads=[]))
            nodes[parent]['quads'].extend(quads)
        else:
            g=groups.get(entry['uuid'],entry);name=g.get('name','')
            hidden=hidden or not g.get('export',True) or not g.get('visibility',True) or any(s in name.lower() for s in ['riferimento','manichino']) or name.lower()=='nemico'
            if hidden:return
            idx=len(nodes);indices[entry['uuid']]=idx
            nodes.append(dict(name=name,parent=parent,origin=g.get('origin',[0,0,0]),rotation=g.get('rotation',[0,0,0]),quads=[]))
            for child in entry.get('children',[]):walk(child,idx,hidden)
    for entry in d['outliner']:walk(entry)
    clips=[]
    for a in d['animations']:
        channels={}
        for uuid,b in a['animators'].items():
            if uuid not in indices:continue
            tracks=collections.defaultdict(list)
            for k in b.get('keyframes',[]):
                assert k['channel'] in ['position','rotation','scale']
                assert k['interpolation'] in ['linear','catmullrom','step']
                vals=[[float(pt[axis]) for axis in 'xyz'] for pt in k['data_points']]
                tracks[k['channel']].append([k['time'],k['interpolation'],vals[0],vals[-1]])
            channels[str(indices[uuid])]={ch:sorted(keys) for ch,keys in tracks.items()}
        clips.append(dict(name=a['name'],length=a['length'],loop=a['loop']=='loop',channels=channels))
    return dict(nodes=nodes,textures=textures,clips=clips)

def main():
    out=pathlib.Path(sys.argv[2]) if len(sys.argv)>2 else pathlib.Path(__file__).resolve().parents[1]/'src/main/resources'
    report=[]
    with zipfile.ZipFile(sys.argv[1]) as archive:
        models=sorted(n for n in archive.namelist() if n.endswith('.bbmodel'))
        assert len(models)==40
        for n in models:
            ident=pathlib.PurePosixPath(n).stem;assert re.fullmatch(r'[a-z0-9_]+',ident)
            d=json.loads(archive.read(n));result=compile_model(d,ident,out)
            dest=out/'assets/castigoclasses/models/blockbench'/f'{ident}.json';dest.parent.mkdir(parents=True,exist_ok=True)
            dest.write_text(json.dumps(result,separators=(',',':')),encoding='utf8')
            report.append(dict(id=ident,source=n,nodes=len(result['nodes']),quads=sum(len(g['quads']) for g in result['nodes']),clips=[c['name'] for c in result['clips']]))
    (out/'assets/castigoclasses/models/blockbench/index.json').write_text(json.dumps(report,indent=2),encoding='utf8')
    print(f'Compiled {len(report)} models, {sum(x["quads"] for x in report)} quads; no reference actors exported.')

if __name__=='__main__':main()
