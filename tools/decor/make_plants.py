#!/usr/bin/env python3
"""Plants (0.3.0): Debug Flytrap (3 stages, open and digesting), catnip, Potted Grass and the catnip effect icon.
Textures, block models, blockstates, item definitions. PLACEHOLDER-quality procedural pixel art, no AI.

    python3 tools/decor/make_plants.py

The flytrap's traps are stamped from small pixel maps onto a cross texture; potted plants use vanilla's
flower_pot_cross template. Potted Grass gets its own pre-colored tuft (no biome tint, it lives indoors).
"""
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "plush"))
sys.path.insert(0, os.path.dirname(__file__))
from make_plushies import hexc  # noqa: E402
from make_decor import item_def, save, write_json  # noqa: E402

PALETTE = {
    "g": hexc("#4E9A3F"),  # leaf
    "G": hexc("#7CC45E"),  # leaf highlight
    "d": hexc("#2F6B2C"),  # stem, shadow
    "p": hexc("#F07AA6"),  # trap inside (the mod's pink)
    "P": hexc("#C94C7C"),  # deep inside
    "t": hexc("#E8F5C8"),  # cilia "teeth"
    "m": hexc("#8CBF7A"),  # catnip leaf
    "M": hexc("#B5DCA3"),  # catnip leaf highlight
    "n": hexc("#5C8A4E"),  # catnip stem and veins
    "w": hexc("#EDE4FA"),  # catnip flowers
    "l": hexc("#B79AE0"),  # catnip flower spots
    "a": hexc("#5FA83A"),  # grass
    "A": hexc("#86C957"),  # grass highlight
    "s": hexc("#3A7424"),  # grass shadow
}

TRAP_SMALL_OPEN = [
    "t......t",
    "gt....tg",
    "gpt..tpg",
    ".gpPPpg.",
    "..gppg..",
]
TRAP_SMALL_CLOSED = [
    "..t..t..",
    ".tgttgt.",
    ".gGggPg.",
    ".gGgPgg.",
    "..gPgg..",
]
TRAP_BIG_OPEN = [
    "t........t",
    "Gt......tG",
    "gGt....tGg",
    ".gpt..tpg.",
    ".gppPPppg.",
    "..ggppgg..",
]
TRAP_BIG_CLOSED = [
    "..t.tt.t..",
    ".tgttttgt.",
    ".gGgggggg.",
    ".gGgggPgg.",
    ".ggGgPPgg.",
    "..gggggg..",
]
ROSETTE = [
    "gG..........Gg",
    ".ggG......Ggg.",
    "..gggG..Gggg..",
    "...gggddggg...",
]

# per stage: the traps (stamp size, left x, top y); stems run down to the rosette
STAGES = [
    [("small", 4, 5)],
    [("small", 0, 6), ("big", 5, 1)],
    [("big", 3, 0), ("small", 0, 5), ("small", 8, 4)],
]

CATNIP = [
    "................",
    ".......w........",
    "......wlw.......",
    ".......w........",
    "......lwl.......",
    "...MM..n..MM....",
    "..MmmMnnnMmmM...",
    "...mm..n..mm....",
    ".......n........",
    ".MM....n....MM..",
    "MmmMnnnnnnnMmmM.",
    ".mm....n....mm..",
    "...MM..n..MM....",
    "..MmmMnnnMmmM...",
    "...mm..n..mm....",
    ".......n........",
]

GRASS_TUFT = [
    "................",
    "................",
    "................",
    "......A.........",
    "..A...a....A....",
    "..a..Aa...Aa..A.",
    "..as.as..Aas..a.",
    ".Aas.asA.aas.Aa.",
    ".aas.aaa.as.Aas.",
    ".sas.saa.as.aas.",
    "..sa.sas.ssAas..",
    "..ssasssassaas..",
    "...ssssssssss...",
    "....ssssssss....",
    "................",
    "................",
]

EFFECT_ICON = [
    "..................",
    "........ww........",
    ".......wllw.......",
    "........ww........",
    ".......wllw.......",
    "........nn........",
    "...MMM..nn..MMM...",
    "..MmmmM.nn.MmmmM..",
    "..mmmmmnnnnmmmmm..",
    "...mmm..nn..mmm...",
    "........nn........",
    ".MMM....nn....MMM.",
    "MmmmM...nn...MmmmM",
    "mmmmmnnnnnnnnmmmmm",
    ".mmm....nn....mmm.",
    "........nn........",
    "........nn........",
    "..................",
]


def stamp(img, rows, x0, y0):
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and 0 <= x0 + x < img.width and 0 <= y0 + y < img.height:
                px[x0 + x, y0 + y] = PALETTE[ch]


def from_rows(rows):
    img = Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    stamp(img, rows, 0, 0)
    return img


def stem(img, x_top, y_top, x_bottom, y_bottom):
    """A 1px stem bending from the trap down to the rosette's center."""
    px = img.load()
    for y in range(y_top, y_bottom + 1):
        t = (y - y_top) / max(1, y_bottom - y_top)
        x = round(x_top + (x_bottom - x_top) * t * t)
        px[x, y] = PALETTE["d"]


def flytrap(stage, closed):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    stamp(img, ROSETTE, 1, 12)
    for size, x0, y0 in STAGES[stage]:
        rows = {("small", False): TRAP_SMALL_OPEN, ("small", True): TRAP_SMALL_CLOSED,
                ("big", False): TRAP_BIG_OPEN, ("big", True): TRAP_BIG_CLOSED}[(size, closed)]
        stem(img, x0 + len(rows[0]) // 2, y0 + len(rows), 8, 14)
        stamp(img, rows, x0, y0)
    return img


def plant_models(name, texture):
    write_json({"parent": "minecraft:block/cross", "textures": {"cross": texture}}, "models", "block", name + ".json")


def potted_model(name, texture):
    write_json({"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": texture}}, "models", "block", name + ".json")


def generated_item(name, texture):
    write_json({"parent": "minecraft:item/generated", "textures": {"layer0": texture}}, "models", "item", name + ".json")
    item_def(name, f"femboymod:item/{name}")


def main():
    variants = {}
    for stage in range(3):
        for closed in (False, True):
            name = f"debug_flytrap_stage{stage}" + ("_closed" if closed else "")
            save(flytrap(stage, closed), "textures", "block", name + ".png")
            plant_models(name, f"femboymod:block/{name}")
            variants[f"digesting={'true' if closed else 'false'},stage={stage}"] = {"model": f"femboymod:block/{name}"}
    write_json({"variants": variants}, "blockstates", "debug_flytrap.json")
    generated_item("debug_flytrap", "femboymod:block/debug_flytrap_stage1")
    potted = {}
    for closed in (False, True):
        name = "potted_debug_flytrap" + ("_closed" if closed else "")
        potted_model(name, "femboymod:block/debug_flytrap_stage0" + ("_closed" if closed else ""))
        potted[f"digesting={'true' if closed else 'false'}"] = {"model": f"femboymod:block/{name}"}
    write_json({"variants": potted}, "blockstates", "potted_debug_flytrap.json")

    save(from_rows(CATNIP), "textures", "block", "catnip.png")
    plant_models("catnip", "femboymod:block/catnip")
    write_json({"variants": {"": {"model": "femboymod:block/catnip"}}}, "blockstates", "catnip.json")
    generated_item("catnip", "femboymod:block/catnip")
    potted_model("potted_catnip", "femboymod:block/catnip")
    write_json({"variants": {"": {"model": "femboymod:block/potted_catnip"}}}, "blockstates", "potted_catnip.json")

    save(from_rows(GRASS_TUFT), "textures", "block", "potted_grass_plant.png")
    potted_model("potted_grass", "femboymod:block/potted_grass_plant")
    write_json({"variants": {"": {"model": "femboymod:block/potted_grass"}}}, "blockstates", "potted_grass.json")

    save(from_rows(EFFECT_ICON), "textures", "mob_effect", "catnip.png")


if __name__ == "__main__":
    main()
