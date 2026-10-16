#!/usr/bin/env python3
"""Icons for the 0.2.0 Halloween costume, pet outfits, candy and the lost sock. Drawn from shapes, no AI (the spawn eggs are not made here: they started as AI drafts, picked and kept by hand).

Dyeable items get two 32x32 layers like the other wear icons (make_wear_icons.py): <name>.png is grey and tinted with
the colorway, <name>_detail.png is drawn as is. Fixed-color items get one layer. Also writes the item model files.

    python3 tools/items/make_v020_icons.py            # textures + item models
    python3 tools/items/make_v020_icons.py --preview  # also build/v020_icons_preview.png
"""
import json
import os
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(__file__))
from make_wear_icons import G_BASE, G_LIGHT, G_SHADE, OUTLINE, SIZE, layers, outline, shade_rows, tinted  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "femboymod")
OUT = os.path.join(ASSETS, "textures", "item")

DARK = (0x2A, 0x23, 0x28, 255)
GOLD = (0xF2, 0xC9, 0x4C, 255)
GOLD_SHADE = (0xC9, 0x9A, 0x2E, 255)
WHITE = (0xFA, 0xF7, 0xF4, 255)
GREEN = (0x5E, 0x9E, 0x2E, 255)
ORANGE = (0xF0, 0x8A, 0x24, 255)
ORANGE_LIGHT = (0xF8, 0xAE, 0x5C, 255)
ORANGE_SHADE = (0xC4, 0x66, 0x14, 255)
PINK = (0xFF, 0x7E, 0xB6, 255)

# default colors (same as the renderers), for the item model tint and the preview
DYEABLE = {"witch_hat": 0x5B3A8C, "pumpkin_hoodie": 0xF08A24, "ghost_sheet": 0xF2F2F6,
           "pet_bow": 0xFF8FB8, "mini_witch_hat": 0x5B3A8C, "bell_collar": 0xF291BE, "pet_bandana": 0x7EC8F0}


def witch_hat(scale=1.0, band_light=True):
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)

    def p(x, y):
        return 16 + (x - 16) * scale, 16 + (y - 16) * scale

    b.ellipse([*p(2, 21), *p(30, 28)], fill=G_BASE)                      # brim
    b.polygon([p(9, 24), p(23, 24), p(20, 12), p(23, 6), p(26, 4), p(17, 7), p(13, 13)], fill=G_BASE)  # cone, tip bent
    shade_rows(base, 3, light_rows=1, shade_rows=2)
    b.rectangle([*p(10, 19), *p(22, 21)], fill=G_LIGHT if band_light else G_SHADE)  # band
    d.rectangle([*p(15, 19), *p(17, 21)], fill=GOLD)                      # buckle
    outline(base, detail)
    return base, detail


def pumpkin_hoodie():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    b.rectangle([9, 9, 23, 28], fill=G_BASE)                              # body
    b.polygon([(9, 10), (3, 14), (3, 25), (7, 25), (9, 17)], fill=G_BASE)   # sleeves
    b.polygon([(23, 10), (29, 14), (29, 25), (25, 25), (23, 17)], fill=G_BASE)
    b.ellipse([10, 3, 22, 12], fill=G_BASE)                               # hood
    shade_rows(base, 3, light_rows=1, shade_rows=2)
    b.rectangle([9, 26, 23, 28], fill=G_SHADE)                            # hem
    for cx in (13, 19):                                                   # carved eyes
        d.polygon([(cx - 2, 18), (cx + 2, 18), (cx, 15)], fill=DARK)
    d.rectangle([12, 21, 20, 22], fill=DARK)                              # grin
    detail.putpixel((14, 22), (0, 0, 0, 0))
    detail.putpixel((18, 22), (0, 0, 0, 0))
    detail.putpixel((11, 20), DARK)
    detail.putpixel((21, 20), DARK)
    d.rectangle([15, 1, 16, 3], fill=GREEN)                               # stem on the hood
    outline(base, detail)
    return base, detail


def ghost_sheet():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    b.ellipse([6, 2, 26, 22], fill=G_BASE)
    b.rectangle([6, 12, 26, 26], fill=G_BASE)
    for x in (6, 13, 20):                                                 # wavy hem
        b.polygon([(x, 26), (x + 3, 30), (x + 6, 26)], fill=G_BASE)
    shade_rows(base, 3, light_rows=2, shade_rows=2)
    d.ellipse([10, 9, 13, 14], fill=DARK)                                 # eye holes
    d.ellipse([18, 9, 21, 14], fill=DARK)
    outline(base, detail)
    return base, detail


def candy_pail():
    img = Image.new("RGBA", (SIZE, SIZE))
    d = ImageDraw.Draw(img)
    d.arc([7, 1, 25, 21], 180, 360, fill=DARK, width=2)                   # handle
    d.ellipse([5, 9, 27, 29], fill=ORANGE)
    d.ellipse([7, 10, 20, 18], fill=ORANGE_LIGHT)
    d.ellipse([5, 9, 27, 29], outline=ORANGE_SHADE)
    d.line([(16, 11), (16, 28)], fill=ORANGE_SHADE)
    d.polygon([(10, 19), (14, 19), (12, 16)], fill=DARK)                  # face
    d.polygon([(18, 19), (22, 19), (20, 16)], fill=DARK)
    d.rectangle([11, 22, 21, 23], fill=DARK)
    d.rectangle([9, 8, 12, 11], fill=PINK)                                # candy on top
    d.rectangle([19, 7, 22, 10], fill=GOLD)
    detail = Image.new("RGBA", (SIZE, SIZE))
    outline(img, detail)
    img.alpha_composite(detail)
    return img


def pet_bow():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    b.polygon([(16, 15), (4, 7), (3, 23)], fill=G_BASE)
    b.polygon([(16, 15), (28, 7), (29, 23)], fill=G_BASE)
    b.polygon([(14, 17), (10, 28), (14, 27), (16, 19)], fill=G_BASE)      # tails
    b.polygon([(18, 17), (22, 28), (18, 27), (16, 19)], fill=G_BASE)
    shade_rows(base, 3, light_rows=2, shade_rows=2)
    b.rectangle([13, 12, 19, 18], fill=G_LIGHT)                           # knot
    outline(base, detail)
    return base, detail


def bell_collar():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    b.ellipse([3, 5, 29, 23], fill=G_BASE)
    b.ellipse([7, 8, 25, 19], fill=(0, 0, 0, 0))
    shade_rows(base, 3, light_rows=1, shade_rows=1)
    d.ellipse([12, 18, 20, 27], fill=GOLD)                                # bell
    d.line([(13, 25), (19, 25)], fill=GOLD_SHADE)
    d.line([(16, 24), (16, 26)], fill=DARK)
    outline(base, detail)
    return base, detail


def pet_bandana():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    b.rectangle([3, 6, 29, 10], fill=G_BASE)                              # band
    b.polygon([(6, 10), (26, 10), (16, 28)], fill=G_BASE)                 # triangle
    shade_rows(base, 3, light_rows=1, shade_rows=2)
    for x, y in ((11, 13), (16, 15), (21, 13), (14, 19), (18, 19), (16, 23)):  # white dots
        d.rectangle([x, y, x + 1, y + 1], fill=WHITE)
    outline(base, detail)
    return base, detail


SMALL_PALETTE = {
    "o": (0x4A, 0x20, 0x38), "p": (0xFF, 0x7E, 0xB6), "P": (0xFF, 0xB3, 0xD4), "w": (0xFF, 0xF4, 0xF8),
    "y": (0xFF, 0xD8, 0x4A), "Y": (0xE8, 0xB3, 0x24), "r": (0xFF, 0x8C, 0x1A), "k": (0x2A, 0x23, 0x28),
    "b": (0x7E, 0xC8, 0xF0), "g": (0x8F, 0xD6, 0xA8), "G": (0x5E, 0xA8, 0x7A), "l": (0xC8, 0xA2, 0xE8),
}
SMALL = {
    "candy": [
        "................",
        "................",
        "................",
        "................",
        ".oo..........oo.",
        ".oPo..oooo..oPo.",
        "..oPoopppPoooPo.",
        "..oPopwppppopPo.",
        "..oPopppppppPo..",
        ".oPoopppppPoopo.",
        ".oPo..oooo..oPo.",
        ".oo..........oo.",
        "................",
        "................",
        "................",
        "................",
    ],
    "lost_sock": [
        "................",
        "......oooooo....",
        "......owwwwo....",
        "......opppPo....",
        "......owwwwo....",
        "......opppPo....",
        "......owwwwo....",
        "......opppPo....",
        "......owwwwo....",
        ".....oopppPo....",
        "...oowwwwwwo....",
        "..oPpppppppo....",
        "..owwwwwwwwo....",
        "...oooooooo.....",
        "................",
        "................",
    ],
}


def small(rows):
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), SMALL_PALETTE[ch] + (255,))
    return img


def write_json(obj, *parts):
    path = os.path.join(*parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(json.dumps(obj, indent=2) + "\n")


def item_files(name, two_layers, default=None):
    textures = {"layer0": "femboymod:item/" + name}
    if two_layers:
        textures["layer1"] = "femboymod:item/" + name + "_detail"
    write_json({"parent": "minecraft:item/generated", "textures": textures}, ASSETS, "models", "item", name + ".json")
    model = {"type": "minecraft:model", "model": "femboymod:item/" + name}
    if two_layers:
        model["tints"] = [{"type": "femboymod:colorway", "default": default, "stripe": 0},
                          {"type": "minecraft:constant", "value": -1}]
    write_json({"model": model}, ASSETS, "items", name + ".json")


TWO_LAYER = {
    "witch_hat": witch_hat,
    "pumpkin_hoodie": pumpkin_hoodie,
    "ghost_sheet": ghost_sheet,
    "pet_bow": pet_bow,
    "mini_witch_hat": lambda: witch_hat(scale=0.75),
    "bell_collar": bell_collar,
    "pet_bandana": pet_bandana,
}


def main():
    preview = []
    for name, draw in TWO_LAYER.items():
        base, detail = draw()
        base.save(os.path.join(OUT, name + ".png"))
        detail.save(os.path.join(OUT, name + "_detail.png"))
        item_files(name, True, DYEABLE[name])
        icon = tinted(base, DYEABLE[name])
        icon.alpha_composite(detail)
        preview.append(icon)
    pail = candy_pail()
    pail.save(os.path.join(OUT, "candy_pail.png"))
    item_files("candy_pail", False)
    preview.append(pail)
    for name, rows in SMALL.items():
        img = small(rows)
        img.save(os.path.join(OUT, name + ".png"))
        item_files(name, False)
        preview.append(img)
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(preview) * 136, 136), (0x8B, 0x8B, 0x8B, 255))
        for i, img in enumerate(preview):
            sheet.alpha_composite(img.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4))
        os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
        sheet.save(os.path.join(ROOT, "build", "v020_icons_preview.png"))
    print("wrote", len(preview), "icons")


if __name__ == "__main__":
    main()
