#!/usr/bin/env python3
"""Item tags for wearing our cosmetics in other mods' slots (SPEC §4.3), from the registrations in FemboyItems.

    python3 tools/compat/make_slot_tags.py

Writes data/femboymod/tags/item/slot/<our slot>.json (every built-in cosmetic of that slot; addons can add theirs)
and, for Curios, Trinkets and Accessories, their item tags pointing at ours plus the slot assignments for players.
These files only matter when the slot mod is installed. Backpacks (back) stay in our slot on purpose.
"""
import json
import os
import re

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
DATA = os.path.join(ROOT, "common", "src", "main", "resources", "data")
# 1.20.1: item tags live in tags/items
TAGS = "items"
ITEMS_JAVA = os.path.join(ROOT, "common", "src", "main", "java", "dev", "eliasnvx", "femboymod", "registry",
                          "FemboyItems.java")

# our slot -> (curios slot, trinkets group/slot, accessories slot); None = no matching slot there
MAPPING = {
    "head_accessory": ("head", "head/hat", "hat"),
    "face": ("head", "head/face", "face"),
    "neck": ("necklace", "chest/necklace", "necklace"),
    "outfit_top": ("body", None, None),
    "hands": ("hands", "hand/glove", "hand"),
    "waist": ("belt", "legs/belt", "belt"),
    "feet": (None, "feet/shoes", "shoes"),
}
CONSTANTS = {"HEAD_ACCESSORY": "head_accessory", "FACE": "face", "NECK": "neck", "BACK": "back", "TAIL": "tail",
             "LEGS_OVERLAY": "legs_overlay", "OUTFIT_TOP": "outfit_top", "OUTFIT_BOTTOM": "outfit_bottom",
             "HANDS": "hands", "WAIST": "waist", "FEET": "feet"}


def write(obj, *parts):
    path = os.path.join(DATA, *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(obj, indent=2) + "\n")


def cosmetics_by_slot():
    src = open(ITEMS_JAVA, encoding="utf-8").read()
    slots = {}
    for name, const in re.findall(r'cosmetic\("([a-z0-9_]+)",\s*FemboySlots\.([A-Z_]+)\)', src):
        slots.setdefault(CONSTANTS[const], []).append("femboymod:" + name)
    shapes = re.search(r"HAIR_CLIP_SHAPES = List\.of\(([^)]*)\)", src, re.S)
    for shape in re.findall(r'"([a-z0-9_]+)"', shapes.group(1)):
        slots.setdefault("head_accessory", []).append("femboymod:hair_clip_" + shape)
    return {slot: sorted(items) for slot, items in slots.items()}


def main():
    by_slot = cosmetics_by_slot()
    for slot, items in by_slot.items():
        write({"values": items}, "femboymod", "tags", TAGS, "slot", slot + ".json")
    curios, trinkets, accessories = {}, {}, {}
    for slot, (c, t, a) in MAPPING.items():
        ref = {"id": "#femboymod:slot/" + slot, "required": False}
        for table, key in ((curios, c), (trinkets, t), (accessories, a)):
            if key:
                table.setdefault(key, []).append(ref)
    for key, values in curios.items():
        write({"values": values}, "curios", "tags", TAGS, key + ".json")
    write({"entities": ["minecraft:player"], "slots": sorted(curios)}, "femboymod", "curios", "entities", "femboymod.json")
    for key, values in trinkets.items():
        group, name = key.split("/")
        write({"values": values}, "trinkets", "tags", TAGS, group, name + ".json")
    # Trinkets reads entity slot assignments from data/<ns>/entities/ (its own slots: head/hat, chest/necklace, ...)
    write({"entities": ["player"], "slots": sorted(trinkets)}, "trinkets", "entities", "femboymod.json")
    for key, values in accessories.items():
        write({"values": values}, "accessories", "tags", TAGS, key + ".json")
    print("slot tags:", {s: len(i) for s, i in by_slot.items()})


if __name__ == "__main__":
    main()
