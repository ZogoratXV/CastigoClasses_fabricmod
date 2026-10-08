"""Import user-owned sounds for a local build. Source archives and audio are not published to GitHub."""
import argparse,json,re,zipfile
from pathlib import Path
parser=argparse.ArgumentParser();parser.add_argument('archives',type=Path);args=parser.parse_args()
root=Path(__file__).resolve().parents[1]/'local-audio/assets/castigoclasses_audio';root.mkdir(parents=True,exist_ok=True)
chosen={'cleric':['holy_summon','holy_impact','holy_explosion','orbs','bash'],
 'mage':['fire_ball','fire_explode','ice_break','meteor_explosion','thunder_strike','thunder_teleport'],
 'necromancer':['shoot','summon','slash'],
 'warrior':['warrior_slash1','warrior_pierce','warrior_stomp','warrior_airdash','warrior_charge'],
 'archer':['awakened_archer_arrow_shoot','awakened_archer_first_hit','awakened_archer_strong_arrow'],
 'assassin':['aa_cut','aa_dash']}
events={}
for file in args.archives.glob('samus2002_AWAKENED*.zip'):
    found=re.search(r'AWAKENED_([A-Z]+)',file.name)
    if not found or found[1].lower() not in chosen:continue
    kind=found[1].lower()
    with zipfile.ZipFile(file) as z:
        for name in chosen[kind]:
            matches=[p for p in z.namelist() if 'ItemsAdder/' in p and p.endswith('/'+name+'.ogg')]
            if not matches:raise ValueError(f'Missing sound {kind}/{name}')
            path=root/'sounds'/kind/(name+'.ogg');path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(z.read(matches[0]))
            events[kind+'.'+name]={'sounds':[{'name':'castigoclasses_audio:'+kind+'/'+name,'stream':False}]}
expected=sum(map(len,chosen.values()))
if len(events)!=expected:raise ValueError(f'Expected {expected} sounds, found {len(events)}')
(root/'sounds.json').write_text(json.dumps(events,indent=2)+'\n',encoding='utf8')
(root/'CREDITS.txt').write_text('Audio from user-provided AWAKENED packages by SamusDev / samus2002.\nhttps://samusdev.com/\nhttps://www.youtube.com/@SamusDev\nKeep the original package terms with your licensed server assets.\n',encoding='utf8')
print('Imported',len(events),'local sound events')
