"""Import user-supplied PNGs only; keep proprietary pack content outside public sources."""
import sys,json,re,zipfile,struct,unicodedata
from pathlib import Path
root=Path(__file__).resolve().parents[1]
plugin=root.parent/'CastigoClasses_plugin'
def slug(text):
    return re.sub('[^a-z0-9]+','_',unicodedata.normalize('NFKD',text).encode('ascii','ignore').decode().lower()).strip('_')
def main(archive):
    mapping={}
    with zipfile.ZipFile(archive) as z:
        pngs={Path(n).stem:n for n in z.namelist() if '/assets/classi_regno/textures/item/' in n and n.endswith('.png')}
        assert len(pngs)==40,'Expected 40 skill icons'
        for file in sorted((plugin/'src/main/resources/classes').glob('*.yml')):
            if file.stem=='mago':continue
            parts=re.split(r'^  ([a-z0-9_]+):\s*$',file.read_text(encoding='utf-8-sig').split('skills:',1)[1],flags=re.M)
            for i in range(1,len(parts),2):
                sid,body=parts[i:i+2];name=re.search(r'^    name:\s*(.+)$',body,re.M).group(1).strip().strip("'\"")
                prefix='arciere_regno' if file.stem=='arciere' else file.stem
                key='rg_'+prefix+'_'+slug(name)
                assert key in pngs,(sid,key)
                raw=z.read(pngs[key]);assert raw[:8]==b'\x89PNG\r\n\x1a\n'
                w,h=struct.unpack('!II',raw[16:24]);assert 0<w<=1024 and 0<h<=1024 and len(raw)<2000000
                target=root/'local-icons/assets/classi_regno/textures/item'/f'{key}.png';target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
                mapping[sid]='texture:classi_regno:textures/item/'+key+'.png'
        assert len(mapping)==40
    (plugin/'src/main/resources/skill-icons.json').write_text(json.dumps(mapping,indent=2)+'\n',encoding='utf-8')
    (root/'src/test/resources/skill-icons.json').write_text(json.dumps(mapping,indent=2)+'\n',encoding='utf-8')
    print('Imported 40 PNGs and matched 40 skill IDs. No coal mappings imported.')
if __name__=='__main__':main(sys.argv[1])
