#!/usr/bin/env python3
"""Generates a STARTING Blockbench/GeckoLib model for femboymod:cat_ears from the code placeholder
geometry (same masks, spacing and tilt as CosmeticModels.catEars()). Open the .geo.json in Blockbench
(GeckoLib Animated Model / Bedrock), then polish the model and paint the texture by hand (SPEC §11).

Coordinates: Java model space (y down, head pivot at neck) -> Bedrock (y up, neck at y=24).
"""
import json, math, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod")
EAR = ["...MM...", "...MM...", "..MMMM..", "..MAAM..", ".MMAAMM.", ".MAAAAM.",
       "MMAAAAMM", "MAAAAAAM", "MAFAAFAM", "MFFAAFFM"]
PIXEL, SPACING, TILT_DEG = 0.45, 2.9, math.degrees(0.3)
UV = {"main": [0, 0], "accent": [0, 8], "detail": [0, 16], "band": [0, 24]}

def bb_cube(x0, y0, z0, w, h, d, px, py, pz, uv):
    """Java box (relative to a part at pivot p) -> Bedrock cube in absolute model coordinates."""
    return {"origin": [round(x0 + px, 4), round(24 - (y0 + py + h), 4), round(z0 + pz, 4)],
            "size": [round(w, 4), round(h, 4), round(d, 4)], "uv": uv}

def extrude(rows, legend, x0, y0, z0, depth, pivot):
    cubes = []
    for r, line in enumerate(rows):
        c = 0
        while c < len(line):
            ch = line[c]; e = c + 1
            while e < len(line) and line[e] == ch:
                e += 1
            if ch in legend:
                cubes.append(bb_cube(x0 + c * PIXEL, y0 + r * PIXEL, z0, (e - c) * PIXEL, PIXEL, depth, *pivot, UV[legend[ch]]))
            c = e
    return cubes

bones = [{"name": "armorHead", "pivot": [0, 24, 0]}]
band_pivot = (0, 0, 0)
bones.append({"name": "headband", "parent": "armorHead", "pivot": [0, 24, 0], "cubes": [
    bb_cube(-4.5, -8.6, -1.2, 9, 0.8, 1.6, *band_pivot, UV["band"]),
    bb_cube(-4.6, -8.2, -1.1, 0.6, 3.2, 1.4, *band_pivot, UV["band"]),
    bb_cube(4.0, -8.2, -1.1, 0.6, 3.2, 1.4, *band_pivot, UV["band"])]})
w, h = len(EAR[0]) * PIXEL, len(EAR) * PIXEL
for side, name in ((-1, "left_ear"), (1, "right_ear")):
    pivot = (side * SPACING, -8.3, -0.2)
    cubes = extrude(EAR, {"M": "main", "A": "main", "F": "main"}, -w / 2, -h, -0.4, 0.9, pivot)
    cubes += extrude(EAR, {"A": "accent", "F": "accent"}, -w / 2, -h, -0.6, 0.2, pivot)
    cubes += extrude(EAR, {"F": "detail"}, -w / 2, -h, -0.75, 0.2, pivot)
    bones.append({"name": name, "parent": "armorHead", "pivot": [pivot[0], 24 - pivot[1], pivot[2]],
                  "rotation": [0, 0, round(side * TILT_DEG, 2)], "cubes": cubes})

geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.femboymod.cat_ears", "texture_width": 32, "texture_height": 32,
                    "visible_bounds_width": 2, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.5, 0]},
    "bones": bones}]}
os.makedirs(f"{ROOT}/geckolib/models/cosmetic", exist_ok=True)
json.dump(geo, open(f"{ROOT}/geckolib/models/cosmetic/cat_ears.geo.json", "w"), indent=1)

anim = {"format_version": "1.8.0", "animations": {"idle": {"loop": True, "animation_length": 4.0, "bones": {
    "left_ear": {"rotation": {"0.0": [0, 0, 0], "2.6": [0, 0, 0], "2.7": [-8, 0, -12], "2.85": [0, 0, 0]}},
    "right_ear": {"rotation": {"0.0": [0, 0, 0], "1.2": [0, 0, 0], "1.3": [-8, 0, 12], "1.45": [0, 0, 0]}}}}}}
os.makedirs(f"{ROOT}/geckolib/animations/cosmetic", exist_ok=True)
json.dump(anim, open(f"{ROOT}/geckolib/animations/cosmetic/cat_ears.animation.json", "w"), indent=1)

# Starting texture: fur noise in the four UV zones. Colored = shown undyed, gray = multiplied by colorway.
random.seed(5)
zones = {"main": ((242, 145, 190), 0), "accent": ((255, 205, 225), 8), "detail": ((255, 246, 248), 16), "band": ((222, 120, 170), 24)}
colored, gray = Image.new("RGBA", (32, 32)), Image.new("RGBA", (32, 32))
for (r, g, b), v0 in zones.values():
    lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255
    for y in range(v0, v0 + 8):
        for x in range(32):
            n = random.randint(-14, 10) + (6 if (x * 7 + y * 3) % 5 == 0 else 0)
            colored.putpixel((x, y), (max(0, min(255, r + n)), max(0, min(255, g + n)), max(0, min(255, b + n)), 255))
            v = max(0, min(255, int(255 * min(1.0, lum + 0.25)) + n))
            gray.putpixel((x, y), (v, v, v, 255))
os.makedirs(f"{ROOT}/textures/cosmetic", exist_ok=True)
colored.save(f"{ROOT}/textures/cosmetic/cat_ears.png")
gray.save(f"{ROOT}/textures/cosmetic/cat_ears_dyeable.png")
print("cat_ears geo/animation/textures written")
