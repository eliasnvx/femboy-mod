#!/usr/bin/env python3
"""Lookbook entries (data/femboymod/femboymod/lookbook/<item>.json) for every cosmetic, from the mod's own data.

    python3 tools/lookbook/make_lookbook.py           # writes the entries
    python3 tools/lookbook/make_lookbook.py --check   # fails if an entry is missing or out of date

Sources: base crafting/smithing recipes (not the colorway ones), Thrifter and Wandering Cosplayer trades,
mob drops and the Stray Cat's morning gifts. Hand-written extra sources go in EXTRA. Rerun after changing recipes,
trades or loot; the Lookbook GameTest checks that every cosmetic has an entry.

1.20.1 branch: reads this branch's data formats (recipes/, loot_tables/, tags/items/, {"item"}/{"tag"} ingredients,
"Count"/"tag" in trades) and writes the same lookbook JSON as 26.3.
"""
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
DATA = os.path.join(ROOT, "common", "src", "main", "resources", "data", "femboymod")
OUT = os.path.join(DATA, "femboymod", "lookbook")
ITEMS_JAVA = os.path.join(ROOT, "common", "src", "main", "java", "dev", "eliasnvx", "femboymod", "registry",
                          "FemboyItems.java")

# Vanilla tags have no data in this repo: show one typical item for them
TAG_ITEMS = {
    "#minecraft:wool": "minecraft:white_wool",
    "#minecraft:planks": "minecraft:oak_planks",
    "#minecraft:netherite_tool_materials": "minecraft:netherite_ingot",
    "#minecraft:wooden_slabs": "minecraft:oak_slab",
    "#minecraft:logs": "minecraft:oak_log",
}
# Recipe types that only recolor an item the player already has (1.20.1 dyes with the vanilla armor dye recipe)
RECOLOR_TYPES = {"minecraft:crafting_dye", "femboymod:colorway", "femboymod:dye"}
# 1.20.1 has no vanilla transmute recipe: the mod's own serializer stands in for it
TRANSMUTE_TYPES = {"minecraft:crafting_transmute", "femboymod:crafting_transmute"}
RECOLOR_SUFFIXES = ("_dyed", "_glitter", "_neon", "_pearl")
THRIFTER = "femboymod:thrifter"
COSPLAYER = "femboymod:cosplayer"
GIFT_LOOT = {"gameplay/stray_cat_gift.json": "femboymod:stray_cat"}
# Extra hand-written sources: item id -> list of sources
EXTRA = {}


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def cosmetic_ids():
    """Cosmetic item ids, parsed from the cosmetic(...) / backpack(...) / hair clip registrations."""
    src = open(ITEMS_JAVA, encoding="utf-8").read()
    ids = [i for i in re.findall(r'(?:cosmetic|backpack)\("([a-z0-9_]+)"', src) if not i.endswith("_")]
    shapes = re.search(r"HAIR_CLIP_SHAPES = List\.of\(([^)]*)\)", src, re.S)
    ids += ["hair_clip_" + s for s in re.findall(r'"([a-z0-9_]+)"', shapes.group(1))]
    return sorted({"femboymod:" + i for i in ids})


def tag_values(tag):
    ns, path = tag[1:].split(":")
    if ns == "femboymod":
        return read(os.path.join(DATA, "tags", "items", path + ".json"))["values"]
    if tag in TAG_ITEMS:
        return [TAG_ITEMS[tag]]
    raise SystemExit("unknown tag in a recipe, add it to TAG_ITEMS: " + tag)


def ingredient(value):
    if isinstance(value, list):
        value = value[0]
    if isinstance(value, dict):
        value = value.get("item") or "#" + value["tag"]
    return tag_values(value)[0] if value.startswith("#") else value


def stack(entry):
    """An item and count; trades write "Count" (1.20.1 item stack NBT), recipe results "count"."""
    item = entry.get("id") or entry["item"]
    count = entry.get("Count", entry.get("count", 1))
    return item if count == 1 else {"id": item, "count": count}


def recipes(wanted):
    out = {}
    folder = os.path.join(DATA, "recipes")
    for name in sorted(os.listdir(folder)):
        r = read(os.path.join(folder, name))
        if r["type"] in RECOLOR_TYPES or name[:-5].endswith(RECOLOR_SUFFIXES) or "result" not in r:
            continue
        result = (r["result"].get("id") or r["result"]["item"]) if isinstance(r["result"], dict) else r["result"]
        if result not in wanted:
            continue
        if r["type"] == "minecraft:crafting_shaped":
            counts = {}
            for row in r["pattern"]:
                for ch in row:
                    if ch != " ":
                        counts[ch] = counts.get(ch, 0) + 1
            items = [stack({"id": ingredient(r["key"][ch]), "count": n}) for ch, n in counts.items()]
            source = {"kind": "crafting", "items": items}
        elif r["type"] == "minecraft:crafting_shapeless":
            counts = {}
            for i in r["ingredients"]:
                item = ingredient(i)
                counts[item] = counts.get(item, 0) + 1
            source = {"kind": "crafting", "items": [stack({"id": i, "count": n}) for i, n in counts.items()]}
        elif r["type"] in TRANSMUTE_TYPES:
            source = {"kind": "crafting", "items": [ingredient(r["input"]), ingredient(r["material"])]}
        elif r["type"] == "minecraft:smithing_transform":
            source = {"kind": "smithing",
                      "items": [ingredient(r["template"]), ingredient(r["base"]), ingredient(r["addition"])]}
        else:
            continue
        out.setdefault(result, []).append(source)
    return out


def trades():
    out = {}
    root = os.path.join(DATA, "villager_trade")
    for dirpath, _, files in os.walk(root):
        rel = os.path.relpath(dirpath, root).split(os.sep)
        for name in sorted(files):
            t = read(os.path.join(dirpath, name))
            gives = t["gives"]
            source = {"kind": "trade", "items": [stack(t["wants"])]}
            if rel[0] == "thrifter":
                source["from"] = THRIFTER
                source["level"] = int(rel[1])
            else:
                source["from"] = COSPLAYER
            if "femboymod:colorway" in gives.get("tag", {}):
                source["note"] = "lookbook.femboymod.note.colorway"
            out.setdefault(gives["id"], []).append(source)
    for item, sources in out.items():
        # one line per trader: the earliest plain offer, plus one colorway line
        kept = {}
        for s in sorted(sources, key=lambda s: (s.get("level", 0), "note" in s)):
            kept.setdefault((s["from"], "note" in s), s)
        out[item] = sorted(kept.values(), key=lambda s: ("note" in s, s["from"] != THRIFTER, s.get("level", 0)))
    return out


def loot():
    out = {}
    folder = os.path.join(DATA, "loot_tables", "entities")
    for name in sorted(os.listdir(folder)):
        mob = "femboymod:" + name[:-5]
        for pool in read(os.path.join(folder, name)).get("pools", []):
            for e in pool.get("entries", []):
                if e.get("type") == "minecraft:item":
                    out.setdefault(e["name"], []).append({"kind": "drop", "from": mob})
    for rel, giver in GIFT_LOOT.items():
        for pool in read(os.path.join(DATA, "loot_tables", rel)).get("pools", []):
            for e in pool.get("entries", []):
                if e.get("type") == "minecraft:item":
                    ids = [e["name"]]
                elif e.get("type") == "minecraft:tag":
                    ids = tag_values("#" + e["name"])
                else:
                    continue
                for i in ids:
                    out.setdefault(i, []).append({"kind": "gift", "from": giver})
    return out


def build():
    items = cosmetic_ids()
    by_recipe, by_trade, by_loot = recipes(set(items)), trades(), loot()
    entries = {}
    for item in items:
        sources = by_recipe.get(item, []) + by_trade.get(item, []) + by_loot.get(item, []) + EXTRA.get(item, [])
        entries[item] = {"sources": sources}
    return entries


def main():
    check = "--check" in sys.argv
    entries = build()
    os.makedirs(OUT, exist_ok=True)
    stale = []
    for item, entry in entries.items():
        path = os.path.join(OUT, item.split(":")[1] + ".json")
        text = json.dumps(entry, indent=2) + "\n"
        if check:
            if not os.path.exists(path) or open(path, encoding="utf-8").read() != text:
                stale.append(item)
        else:
            with open(path, "w", encoding="utf-8") as f:
                f.write(text)
    empty = [i for i, e in entries.items() if not e["sources"]]
    if empty:
        print("no source found (creative only?):", ", ".join(empty))
    if check and stale:
        raise SystemExit("lookbook entries out of date, rerun tools/lookbook/make_lookbook.py: " + ", ".join(stale))
    print(("checked " if check else "wrote ") + str(len(entries)) + " entries")


if __name__ == "__main__":
    main()
