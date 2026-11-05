#!/usr/bin/env python3
"""Check ported JSON, local textures, translations and Java platform boundaries."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
for file in RESOURCES.rglob("*.json"):
    data = json.loads(file.read_text())
    if "/models/" not in str(file):
        continue
    for texture in data.get("textures", {}).values():
        if texture.startswith("statpatterns:"):
            image = RESOURCES / "assets/statpatterns/textures" / (texture.split(":", 1)[1] + ".png")
            assert image.is_file(), f"Missing texture: {file}: {texture}"
for language in (RESOURCES / "assets/statpatterns/lang").glob("*.lang"):
    keys = set()
    for line in language.read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        key = line.split("=", 1)[0]
        assert key not in keys, f"Duplicate translation: {language}: {key}"
        keys.add(key)
    for key in ("statpatterns.probability", "statpatterns.confidence", "statpatterns.primary_only"):
        assert key in keys, f"Missing control translation: {language}: {key}"
    for item in ("probability_pattern", "probability_pattern_terminal", "wireless_probability_pattern_terminal"):
        base = "item.statpatterns." + item
        assert base + ".name" in keys, f"Missing 1.12.2 item name: {language}: {base}"
        assert base not in keys, f"Unsuffixed key causes a translated .name suffix in 1.12.2: {language}: {base}"
for source in (ROOT / "src/main/java").rglob("*.java"):
    text = source.read_text()
    assert "net.neoforged" not in text and "net.minecraft.world.item" not in text, source
    if "net.createmod" in text:
        assert "/compat/ponder/" in str(source), f"Optional Ponder API escaped isolation: {source}"
assert json.loads((RESOURCES / "pack.mcmeta").read_text())["pack"]["pack_format"] == 3
print("PASS: JSON, textures, translations and optional API isolation")
