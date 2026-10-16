#!/usr/bin/env python3
"""Cozy interior furniture (0.2.0): bean bag, fluffy rug, fairy lights, lava lamp, pet bed, laundry basket, mini fridge,
vanity table. Block models, blockstates, item definitions and textures. PLACEHOLDER-quality procedural pixel art, no AI.

    python3 tools/decor/make_interior.py

Dyeable pieces (bean bag, rug, pet bed, lava lamp) get one texture per dye color and a blockstate variant per
color; their items pick the model by the `color` block state (the loot table copies it). The lava lamp's wax moves
in a slow interpolated animation; fairy lights are steady. Nothing blinks (SPEC §1.1).

1.20.1 port: item models are `models/item/<name>.json`; the dyeable ones pick the color's model through overrides on
the `femboymod:color` item property (dye id / 16 of the item's `BlockStateTag` color, registered in FemboyModClient). The
26.3 item definitions (`items/*.json`) are written too, as for every item of the port. Glowing model elements (26.3
`light_emission`) use Forge's `forge_data.block_light`; vanilla 1.20.1 (Fabric) has no per-element light and draws them
lit by the block's own light.
"""
import colorsys
import json
import math
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "plush"))
sys.path.insert(0, os.path.dirname(__file__))
from make_plushies import Box, hexc, model_json, pack, put, rect, shade  # noqa: E402
from make_decor import horizontal_states, item_def, mcmeta, outline, save, write_json  # noqa: E402

# Vanilla dye colors, a little softer for fabric (blended toward white).
DYES = [
    ("white", "#F9FFFE"), ("orange", "#F9801D"), ("magenta", "#C74EBD"), ("light_blue", "#3AB3DA"),
    ("yellow", "#FED83D"), ("lime", "#80C71F"), ("pink", "#F38BAA"), ("gray", "#474F52"),
    ("light_gray", "#9D9D97"), ("cyan", "#169C9C"), ("purple", "#8932B8"), ("blue", "#3C44AA"),
    ("brown", "#835432"), ("green", "#5E7C16"), ("red", "#B02E26"), ("black", "#1D1D21"),
]
DEFAULT = "pink"
COLOR_PROPERTY = "femboymod:color"
CREAM = hexc("#F6EBDD")
WHITE = hexc("#FFFFFF")


def soft(color_hex, amount=0.18):
    c = hexc(color_hex)
    return tuple(int(v + (255 - v) * amount) for v in c[:3]) + (255,)


def facing_and_color_states(name, extra_props=(), model_for=None):
    """Blockstate with facing x color (x extra boolean props); model_for(color, extras) -> model id."""
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for color, _ in DYES:
            for extras in combos(extra_props):
                key = ",".join([f"color={color}", f"facing={facing}"] + [f"{p}={v}" for p, v in extras])
                entry = {"model": model_for(color, dict(extras))}
                if y:
                    entry["y"] = y
                variants[key] = entry
    write_json({"variants": variants}, "blockstates", name + ".json")


def combos(props):
    if not props:
        return [()]
    out = []
    for rest in combos(props[1:]):
        for value in ("false", "true"):
            out.append(((props[0], value),) + rest)
    return out


def color_item(name, model_for_color):
    """Item definition choosing the model by the item's `color` block state (default pink)."""
    cases = [{"when": color, "model": {"type": "minecraft:model", "model": model_for_color(color)}} for color, _ in DYES]
    write_json({"model": {"type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "color",
                          "cases": cases,
                          "fallback": {"type": "minecraft:model", "model": model_for_color(DEFAULT)}}},
               "items", name + ".json")
    # 1.20.1: overrides on the femboymod:color item property (DYES is in dye id order; the last match wins)
    overrides = [{"predicate": {COLOR_PROPERTY: index / len(DYES)}, "model": model_for_color(color)}
                 for index, (color, _) in enumerate(DYES)]
    write_json({"parent": model_for_color(DEFAULT), "overrides": overrides}, "models", "item", name + ".json")


def block_item(name, model):
    """Item definition (26.3) and item model (1.20.1) of a block item drawn with a block model."""
    item_def(name, model)
    write_json({"parent": model}, "models", "item", name + ".json")


def glowing(level):
    """Per-element light on Forge (26.3: the element's `light_emission`)."""
    return {"forge_data": {"block_light": level}}


def atlas_model(name, boxes, seed, scale=0.625, translation=(0, 0, 0), extra_textures=None, extra_elements=()):
    atlas, placed = pack(boxes, random.Random(seed))
    save(atlas, "textures", "block", name + ".png")
    model = model_json(name, boxes, placed, atlas.size[0], scale, list(translation))
    model["elements"].extend(extra_elements)
    if extra_textures:
        model["textures"].update(extra_textures)
    write_json(model, "models", "block", name + ".json")
    return model


# ------------------------------------------------------------------------------------------------ bean bag

def bean_bag():
    for color, hx in DYES:
        c = soft(hx, 0.1)

        def dent(img, w, h, c=c):  # a soft sunken seat in the middle of the top
            for y in range(h):
                for x in range(w):
                    dx, dy = (x - w / 2 + 0.5) / (w / 2), (y - h / 2 + 1.5) / (h / 2)
                    if dx * dx + dy * dy < 0.45:
                        put(img, [(x, y)], shade(c, 0.88))

        def piping(img, w, h, c=c):
            put(img, rect(0, 0, w, 1), shade(c, 1.1))

        def heart(img, w, h):  # a small cream heart patch on the front
            cx = w // 2
            put(img, [(cx - 2, 2), (cx - 1, 2), (cx + 1, 2), (cx + 2, 2)] + rect(cx - 2, 3, 5, 1) + rect(cx - 1, 4, 3, 1)
                + [(cx, 5)], CREAM)

        boxes = [  # stacked and tapering, so it reads as a squashy bag rather than a chair
            Box("bottom", (1.5, 0, 1.5), (14.5, 2, 14.5), shade(c, 0.94)),
            Box("body", (1, 2, 1), (15, 5, 15), c, {"north": heart}),
            Box("seat", (2, 5, 2), (14, 6, 11), c, {"up": dent}),
            Box("back", (1.5, 5, 10), (14.5, 10, 15), shade(c, 0.98)),
            Box("back_top", (2.5, 10, 11), (13.5, 12, 14.5), shade(c, 1.02), {"up": piping}),
            Box("puff_left", (1, 5, 2.5), (3, 7, 10), shade(c, 1.02)),
            Box("puff_right", (13, 5, 2.5), (15, 7, 10), shade(c, 1.02)),
        ]
        atlas_model(f"bean_bag_{color}", boxes, 11)
    facing_and_color_states("bean_bag", model_for=lambda color, _: f"femboymod:block/bean_bag_{color}")
    color_item("bean_bag", lambda color: f"femboymod:block/bean_bag_{color}")


# ------------------------------------------------------------------------------------------------ fluffy rug

RUG_HEIGHT = 1.5


def shag_texture(c, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), c)
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = shade(c, rng.choice((0.9, 0.95, 1.0, 1.0, 1.04, 1.08)))
    for _ in range(18):  # light tuft tips
        x, y = rng.randrange(16), rng.randrange(16)
        px[x, y] = shade(c, 1.14)
    return img


def heart_rows():
    """x ranges per row (z) of a 16x16 heart, two lobes to the north, the point to the south."""
    return [[(2, 6), (10, 14)], [(1, 7), (9, 15)], [(0, 16)], [(0, 16)], [(0, 16)], [(0, 16)], [(1, 15)], [(1, 15)],
            [(2, 14)], [(3, 13)], [(4, 12)], [(5, 11)], [(6, 10)], [(7, 9)], [(7, 9)], []]


def round_rows():
    rows = []
    for z in range(16):
        dz = z - 7.5
        half = math.sqrt(max(0.0, 7.9 ** 2 - dz * dz))
        x0, x1 = round(8 - half), round(8 + half)
        rows.append([(x0, x1)] if x1 > x0 else [])
    return rows


def rug_elements(rows):
    elements = []
    for z, row in enumerate(rows):
        for x0, x1 in row:
            side = {"texture": "#rug", "uv": [x0, z, x1, z + RUG_HEIGHT]}
            elements.append({"from": [x0, 0, z], "to": [x1, RUG_HEIGHT, z + 1], "faces": {
                "up": {"texture": "#rug", "uv": [x0, z, x1, z + 1]},
                "down": {"texture": "#rug", "uv": [x0, z, x1, z + 1], "cullface": "down"},
                "north": side, "south": side,
                "west": {"texture": "#rug", "uv": [z, 0, z + 1, RUG_HEIGHT]},
                "east": {"texture": "#rug", "uv": [z, 0, z + 1, RUG_HEIGHT]}}})
    return elements


def fluffy_rug():
    shapes = {"square": [[(0, 16)]] * 16, "round": round_rows(), "heart": heart_rows()}
    for i, (color, hx) in enumerate(DYES):
        save(shag_texture(soft(hx, 0.12), 100 + i), "textures", "block", f"fluffy_rug_{color}.png")
        for shape, rows in shapes.items():
            write_json({
                "parent": "minecraft:block/block",
                "ambientocclusion": False,
                "textures": {"rug": f"femboymod:block/fluffy_rug_{color}", "particle": f"femboymod:block/fluffy_rug_{color}"},
                "elements": rug_elements(rows),
                "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 3, 0], "scale": [0.625] * 3},
                            "fixed": {"rotation": [90, 0, 0], "scale": [0.8] * 3}},
            }, "models", "block", f"fluffy_rug_{shape}_{color}.json")
    variants = {}
    for color, _ in DYES:
        for shape in shapes:
            variants[f"color={color},shape={shape}"] = {"model": f"femboymod:block/fluffy_rug_{shape}_{color}"}
    write_json({"variants": variants}, "blockstates", "fluffy_rug.json")
    color_item("fluffy_rug", lambda color: f"femboymod:block/fluffy_rug_round_{color}")


# ------------------------------------------------------------------------------------------------ pet bed

def pet_bed():
    for color, hx in DYES:
        c = soft(hx, 0.1)

        def paw(img, w, h):  # a paw print on the cushion
            cx, cy = w // 2, h // 2 + 1
            dark = shade(CREAM, 0.86)
            put(img, rect(cx - 2, cy, 4, 3), dark)
            put(img, [(cx - 3, cy - 2), (cx - 1, cy - 3), (cx, cy - 3), (cx + 2, cy - 2)], dark)

        def rim_top(img, w, h, c=c):
            put(img, rect(0, 0, w, h), shade(c, 1.08))

        boxes = [  # corners left open so the round rim reads as round; low front for climbing in
            Box("base_x", (2, 0, 1), (14, 3, 15), shade(c, 0.9)),
            Box("base_z", (1, 0, 2), (15, 3, 14), shade(c, 0.9)),
            Box("cushion", (3, 3, 3), (13, 9, 13), CREAM, {"up": paw}),
            Box("rim_n", (3, 3, 1), (13, 10, 3), c, {"up": rim_top}),
            Box("rim_s", (3, 3, 13), (13, 12, 15), c, {"up": rim_top}),
            Box("rim_w", (1, 3, 3), (3, 11, 13), c, {"up": rim_top}),
            Box("rim_e", (13, 3, 3), (15, 11, 13), c, {"up": rim_top}),
            Box("corner_nw", (2, 3, 2), (3, 10, 3), c, seam=False),
            Box("corner_ne", (13, 3, 2), (14, 10, 3), c, seam=False),
            Box("corner_sw", (2, 3, 13), (3, 11, 14), c, seam=False),
            Box("corner_se", (13, 3, 13), (14, 11, 14), c, seam=False),
        ]
        atlas_model(f"pet_bed_{color}", boxes, 21)
    facing_and_color_states("pet_bed", model_for=lambda color, _: f"femboymod:block/pet_bed_{color}")
    color_item("pet_bed", lambda color: f"femboymod:block/pet_bed_{color}")


# ------------------------------------------------------------------------------------------------ fairy lights

FAIRY_MODES = [  # bulb colors per mode: warm white, pink, pastel colors
    ["#FFE7A8"] * 5,
    ["#FF9FD0"] * 5,
    ["#FF9FD0", "#9FE3FF", "#FFE7A0", "#B8F2C8", "#D7B3FF"],
]
WIRE = hexc("#3E4A3A")
BULB_X = [1.5, 4.5, 7.5, 10.5, 13.5]


def sag(x):
    """Wire height at x: hangs from y 15 at the block edges down to y 11 in the middle (joins neighbours)."""
    t = (x - 8) / 8
    return 11 + 4 * t * t


def fairy_lights():
    wire = Image.new("RGBA", (16, 16), WIRE)
    save(wire, "textures", "block", "fairy_lights_wire.png")
    bulbs = {}
    for hx in {h for mode in FAIRY_MODES for h in mode}:
        img = Image.new("RGBA", (16, 16), hexc(hx))
        put(img, rect(4, 2, 4, 4), WHITE)  # glint
        name = "fairy_lights_bulb_" + hx.lstrip("#").lower()
        save(img, "textures", "block", name + ".png")
        bulbs[hx] = name
    for index, mode in enumerate(FAIRY_MODES):
        elements, textures = [], {"wire": "femboymod:block/fairy_lights_wire", "particle": "femboymod:block/fairy_lights_wire"}
        for x in range(16):  # the wire in 1 px steps along the wall
            y = sag(x + 0.5)
            elements.append({"from": [x, y - 0.5, 15], "to": [x + 1, y, 15.5], "faces": {
                f: {"texture": "#wire", "uv": [0, 0, 1, 0.5]} for f in ("north", "south", "up", "down")}})
        for i, bx in enumerate(BULB_X):
            key = f"bulb{i}"
            textures[key] = "femboymod:block/" + bulbs[mode[i]]
            y = sag(bx) - 0.5
            elements.append({"from": [bx - 0.75, y - 2.5, 14.25], "to": [bx + 0.75, y, 15.5], **glowing(15), "faces": {
                f: {"texture": "#" + key, "uv": [4, 2, 8, 10]} for f in ("north", "south", "east", "west", "down")}})
        write_json({"parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures, "elements": elements},
                   "models", "block", f"fairy_lights_{index}.json")
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for index in range(len(FAIRY_MODES)):
            entry = {"model": f"femboymod:block/fairy_lights_{index}"}
            if y:
                entry["y"] = y
            variants[f"facing={facing},mode={index}"] = entry
    write_json({"variants": variants}, "blockstates", "fairy_lights.json")
    # flat icon: a sagging string with pastel bulbs
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for x in range(2, 30):
        t = (x - 16) / 14
        y = int(9 + 8 * (1 - t * t))
        put(icon, [(x, y)], WIRE)
    for i, x in enumerate((5, 11, 17, 23, 28)):
        t = (x - 16) / 14
        y = int(9 + 8 * (1 - t * t)) + 1
        c = hexc(FAIRY_MODES[2][i])
        put(icon, rect(x - 1, y, 3, 4), c)
        put(icon, [(x - 1, y)], WHITE)
        put(icon, [(x, y - 1)], WIRE)
    outline(icon, hexc("#2B2530"))
    save(icon, "textures", "item", "fairy_lights.png")
    write_json({"parent": "minecraft:item/generated", "textures": {"layer0": "femboymod:item/fairy_lights"}},
               "models", "item", "fairy_lights.json")
    item_def("fairy_lights", "femboymod:item/fairy_lights")


# ------------------------------------------------------------------------------------------------ lava lamp

LAMP_FRAMES = 24
LAMP_FRAMETIME = 6
METAL = hexc("#C9C3D6")


def lamp_frames(hx, lit):
    """Glass with two wax blobs rising and sinking on slow sine paths; frames blend (interpolate)."""
    wax = hexc(hx)
    liquid = shade(soft(hx, 0.55), 1.0 if lit else 0.7)
    frames = LAMP_FRAMES if lit else 1
    img = Image.new("RGBA", (16, 16 * frames), liquid)
    px = img.load()
    for f in range(frames):
        phase = 2 * math.pi * f / LAMP_FRAMES
        blobs = [(6.0, 8 + 6 * math.sin(phase), 2.6), (10.0, 8 - 6 * math.sin(phase + 1.3), 2.0)]
        for y in range(16):
            for x in range(16):
                d = min(((x - bx) ** 2 + (y - by) ** 2) / (r * r) for bx, by, r in blobs)
                if d < 1.0:
                    px[x, 16 * f + y] = shade(wax, (1.12 if d < 0.35 else 1.0) * (1.0 if lit else 0.7))
        put(img, [(13, 16 * f + y) for y in range(2, 14)], shade(liquid, 1.15))  # glass highlight
    return img


def lava_lamp():
    save(Image.new("RGBA", (16, 16), METAL), "textures", "block", "lava_lamp_metal.png")
    for color, hx in DYES:
        for lit in (True, False):
            suffix = "" if lit else "_off"
            save(lamp_frames(hx, lit), "textures", "block", f"lava_lamp_{color}{suffix}.png")
            if lit:
                mcmeta(LAMP_FRAMETIME, True, "textures", "block", f"lava_lamp_{color}.png.mcmeta")
            glass_faces = {f: {"texture": "#glass", "uv": [3, 0, 13, 16]} for f in ("north", "south", "east", "west")}
            metal = lambda: {f: {"texture": "#metal", "uv": [0, 0, 6, 6]} for f in ("north", "south", "east", "west", "up", "down")}
            glass = {"from": [5.5, 3, 5.5], "to": [10.5, 12, 10.5], "faces": glass_faces}
            if lit:
                glass.update(glowing(12))
            write_json({
                "parent": "minecraft:block/block",
                "textures": {"glass": f"femboymod:block/lava_lamp_{color}{suffix}", "metal": "femboymod:block/lava_lamp_metal",
                             "particle": "femboymod:block/lava_lamp_metal"},
                "elements": [
                    {"from": [5, 0, 5], "to": [11, 3, 11], "faces": metal()},
                    glass,
                    {"from": [6.5, 12, 6.5], "to": [9.5, 14, 9.5], "faces": metal()},
                ],
                "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 1, 0], "scale": [0.9] * 3},
                            "fixed": {"rotation": [0, 180, 0], "scale": [0.8] * 3}},
            }, "models", "block", f"lava_lamp_{color}{suffix}.json")
    variants = {}
    for color, _ in DYES:
        variants[f"color={color},lit=true"] = {"model": f"femboymod:block/lava_lamp_{color}"}
        variants[f"color={color},lit=false"] = {"model": f"femboymod:block/lava_lamp_{color}_off"}
    write_json({"variants": variants}, "blockstates", "lava_lamp.json")
    color_item("lava_lamp", lambda color: f"femboymod:block/lava_lamp_{color}")


# ------------------------------------------------------------------------------------------------ laundry basket

WICKER = hexc("#D9B27C")
CLOTHES = [hexc(c) for c in ("#F38BAA", "#9FD3F4", "#FFFFFF", "#C9A3F0")]


def weave(img, w, h):
    for y in range(h):
        for x in range(w):
            if (x // 2 + y // 2) % 2 == 0:
                put(img, [(x, y)], shade(WICKER, 0.86))
    put(img, rect(0, 0, w, 1), shade(WICKER, 1.08))


def clothes(img, w, h):
    rng = random.Random(5)
    for y in range(h):
        for x in range(w):
            put(img, [(x, y)], rng.choice(CLOTHES))
    put(img, [(x, h // 2) for x in range(w)], hexc("#FFFFFF"))  # a striped sock across


def laundry_basket():
    def handle(img, w, h):
        put(img, rect(0, 0, w, h), shade(WICKER, 0.75))

    common = [
        Box("basket", (2, 0, 2), (14, 11, 14), WICKER, {f: weave for f in ("north", "south", "east", "west")}),
        Box("clothes", (3, 11, 3), (13, 12, 13), CLOTHES[0], {"up": clothes}),
        Box("handle_w", (1, 8, 6), (2, 10, 10), WICKER, {f: handle for f in ("north", "south", "east", "west", "up", "down")}),
        Box("handle_e", (14, 8, 6), (15, 10, 10), WICKER, {f: handle for f in ("north", "south", "east", "west", "up", "down")}),
    ]
    closed = common + [Box("lid", (1.5, 12, 1.5), (14.5, 13, 14.5), shade(WICKER, 1.04), {"up": weave})]
    opened = common + [Box("lid", (1.5, 12, 13.5), (14.5, 25, 14.5), shade(WICKER, 1.04), {"north": weave, "south": weave})]
    atlas_model("laundry_basket", closed, 31)
    atlas_model("laundry_basket_open", opened, 31)
    horizontal_states("laundry_basket", [(",open=false", "femboymod:block/laundry_basket"),
                                         (",open=true", "femboymod:block/laundry_basket_open")])
    block_item("laundry_basket", "femboymod:block/laundry_basket")


# ------------------------------------------------------------------------------------------------ mini fridge

FRIDGE = hexc("#F7E3EC")
FRIDGE_EDGE = hexc("#E3C3D2")


def mini_fridge():
    def door(img, w, h):  # a handle on the right, a heart sticker and a bolt sticker
        put(img, rect(w - 4, 8, 1, 12), hexc("#B8A6B2"))
        put(img, [(6, 6), (7, 6), (9, 6), (10, 6)] + rect(5, 7, 7, 2) + rect(6, 9, 5, 1) + rect(7, 10, 3, 1) + [(8, 11)],
            hexc("#F38BAA"))
        put(img, [(7, 18), (8, 18), (8, 19), (7, 20), (8, 20), (9, 20), (8, 21), (8, 22)], hexc("#7FD6F0"))

    def inside(img, w, h):  # dark interior, two shelves, cans
        put(img, rect(1, 1, w - 2, h - 2), hexc("#4A4552"))
        for shelf in (h // 3, 2 * h // 3):
            put(img, rect(1, shelf, w - 2, 1), hexc("#CFC8D6"))
        for i, c in enumerate(("#FF7EC0", "#6FC8FF", "#B77CFF", "#FFFFFF")):
            x = 3 + i * 6
            put(img, rect(x, h // 3 - 5, 3, 5), hexc(c))
            put(img, rect(x, 2 * h // 3 - 5, 3, 5), hexc(("#6FC8FF", "#FF7EC0", "#FFFFFF", "#B77CFF")[i]))

    def edge(img, w, h):
        put(img, rect(0, 0, w, 1) + rect(0, h - 1, w, 1), FRIDGE_EDGE)

    body_closed = Box("body", (1, 0, 2), (15, 15, 15), FRIDGE, {"up": edge})
    body_open = Box("body", (1, 0, 2), (15, 15, 15), FRIDGE, {"up": edge, "north": inside})
    feet = [Box(f"foot{i}", (x, 0, z), (x + 1.5, 0.5, z + 1.5), hexc("#8E8396"), seam=False)
            for i, (x, z) in enumerate(((1.5, 2.5), (13, 2.5), (1.5, 13), (13, 13)))]
    closed = [Box("door", (1, 0.5, 1), (15, 15, 2), FRIDGE, {"north": door}), body_closed] + feet
    opened = [Box("door", (0, 0.5, -12), (1, 15, 2), FRIDGE, {"west": door}), body_open] + feet
    for box in (body_closed, body_open):
        box.frm = (1, 0.5, 2)
    atlas_model("mini_fridge", closed, 41)
    atlas_model("mini_fridge_open", opened, 41)
    horizontal_states("mini_fridge", [(",open=false", "femboymod:block/mini_fridge"),
                                      (",open=true", "femboymod:block/mini_fridge_open")])
    block_item("mini_fridge", "femboymod:block/mini_fridge")


# ------------------------------------------------------------------------------------------------ vanity table

VANITY = hexc("#F6E6EE")
VANITY_TRIM = hexc("#E9B8CF")


def vanity_glass():
    """Glass for the vanity table's item model (in game the block entity draws the real, reflecting glass)."""
    img = Image.new("RGBA", (16, 16), hexc("#DCEBF2"))
    for y in range(16):
        for x in range(16):
            if 3 <= (x + y) % 16 <= 5:
                put(img, [(x, y)], hexc("#F6FBFD"))
    return img


def vanity_table():
    save(vanity_glass(), "textures", "block", "vanity_table_glass.png")

    def drawers(img, w, h):  # two drawer fronts with gold knobs
        half = w // 2
        for x0 in (1, half + 1):
            put(img, rect(x0, 1, half - 2, h - 2), shade(VANITY, 1.04))
            put(img, rect(x0, 1, half - 2, 1), VANITY_TRIM)
            put(img, [(x0 + half // 2 - 1, h // 2)], hexc("#E8C24A"))

    def top(img, w, h):
        put(img, rect(0, 0, w, 1), VANITY_TRIM)

    legs = [Box(f"leg{i}", (x, 0, z), (x + 1.5, 7, z + 1.5), VANITY, seam=False)
            for i, (x, z) in enumerate(((1, 3.5), (13.5, 3.5), (1, 14), (13.5, 14)))]
    lower = [
        Box("top", (0, 11, 3), (16, 12, 16), VANITY, {"up": top, "north": top}),
        Box("drawers", (1, 7, 3.5), (15, 11, 15.5), VANITY, {"north": drawers}),
        Box("mirror_stand", (1, 12, 12), (15, 16, 16), VANITY, {"up": top}),
    ] + legs
    atlas_model("vanity_table_lower", lower, 51)
    # upper half: frame around the glass (x 3..13, y 2..14, glass drawn by the block entity at z 12.5) and the back
    upper = [
        Box("frame_l", (1, 0, 12), (3, 15, 15), VANITY),
        Box("frame_r", (13, 0, 12), (15, 15, 15), VANITY),
        Box("frame_b", (3, 0, 12), (13, 2, 15), VANITY),
        Box("frame_t", (3, 14, 12), (13, 15, 15), VANITY),
        Box("crest", (6, 15, 12.5), (10, 16, 14.5), VANITY_TRIM),
    ]
    model = atlas_model("vanity_table_upper", upper, 52, extra_textures={"back": "femboymod:block/mirror_back"},
                        extra_elements=[{"from": [1, 0, 15], "to": [15, 15, 16], "faces": {
                            f: {"texture": "#back", "uv": [1, 1, 15, 16]} for f in ("north", "south", "east", "west", "up", "down")}}])
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for half in ("lower", "upper"):
            entry = {"model": f"femboymod:block/vanity_table_{half}"}
            if y:
                entry["y"] = y
            variants[f"facing={facing},half={half}"] = entry
    write_json({"variants": variants}, "blockstates", "vanity_table.json")
    # item: both halves in one model, the upper one lifted a block
    lower_model = json.load(open(os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources",
                                              "assets", "femboymod", "models", "block", "vanity_table_lower.json")))
    elements = []
    for el in lower_model["elements"]:
        el = json.loads(json.dumps(el))
        for face in el["faces"].values():
            face["texture"] = "#lower"
        elements.append(el)
    for el in model["elements"]:
        el = json.loads(json.dumps(el))
        el["from"][1] += 16
        el["to"][1] += 16
        for face in el["faces"].values():
            if face["texture"] == "#0":
                face["texture"] = "#upper"
        elements.append(el)
    glass = {"from": [3, 18, 12.5], "to": [13, 30, 12.5], "faces": {"north": {"texture": "#glass", "uv": [0, 0, 10, 12]}}}
    elements.append(glass)
    write_json({
        "parent": "minecraft:block/block",
        "texture_size": lower_model["texture_size"],
        "textures": {"lower": "femboymod:block/vanity_table_lower", "upper": "femboymod:block/vanity_table_upper",
                     "back": "femboymod:block/mirror_back", "glass": "femboymod:block/vanity_table_glass",
                     "particle": "femboymod:block/vanity_table_lower"},
        "elements": elements,
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -3, 0], "scale": [0.36] * 3},
                    "fixed": {"rotation": [0, 180, 0], "translation": [0, -4, 0], "scale": [0.45] * 3},
                    "ground": {"translation": [0, 2, 0], "scale": [0.3] * 3},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1.5, 0], "scale": [0.3] * 3},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.3] * 3}},
    }, "models", "item", "vanity_table.json")
    item_def("vanity_table", "femboymod:item/vanity_table")


def main():
    bean_bag()
    fluffy_rug()
    pet_bed()
    fairy_lights()
    lava_lamp()
    laundry_basket()
    mini_fridge()
    vanity_table()
    print("interior assets written")


if __name__ == "__main__":
    main()
