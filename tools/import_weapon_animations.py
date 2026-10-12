"""Import animation JSON only from the user's Better Combat JAR for a private build.
The upstream mod is All Rights Reserved. Never commit the output or its JAR.
Usage: python tools/import_weapon_animations.py path/to/bettercombat.jar
"""
import sys, zipfile, json
from pathlib import Path
root = Path(__file__).resolve().parents[1] / "local-animations/assets/castigoclasses_bc/player_animations"
with zipfile.ZipFile(sys.argv[1]) as archive:
    meta = json.loads(archive.read("fabric.mod.json"))
    if meta.get("id") != "bettercombat":
        raise SystemExit("Expected a Better Combat JAR")
    root.mkdir(parents=True, exist_ok=True)
    count = 0
    for name in archive.namelist():
        if name.startswith("assets/bettercombat/player_animations/") and name.endswith(".json"):
            data = archive.read(name)
            json.loads(data)
            (root / Path(name).name).write_bytes(data)
            count += 1
    print(f"Imported {count} unmodified animation files for the local build.")
