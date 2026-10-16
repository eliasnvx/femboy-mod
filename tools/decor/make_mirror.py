#!/usr/bin/env python3
"""Mirror and Gilded Mirror: textures, block models, blockstates, item models, loot tables. Drawn in code, no AI.

    python3 tools/decor/make_mirror.py

The mirror is a 4 px deep board at the back of a two block tall space; the glass is drawn by MirrorRenderer
(translucent pane over live reflections), so the block models leave the front open: frame and back panel.
"""
import json
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
RES = os.path.join(ROOT, "common", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "femboymod")
DATA = os.path.join(RES, "data", "femboymod")

FRAMES = {
    # name: (base, light, shadow, accent)
    "mirror": ((0xF4, 0xEE, 0xEA), (0xFF, 0xFB, 0xF8), (0xD8, 0xCC, 0xCC), (0xF2, 0xA6, 0xC8)),
    "gilded_mirror": ((0xE8, 0xB8, 0x48), (0xFA, 0xE0, 0x8A), (0xB0, 0x80, 0x28), (0xFF, 0xF1, 0xB8)),
}
SILVER = [(0xC9, 0xD3, 0xDC), (0xD7, 0xDF, 0xE6), (0xE4, 0xEA, 0xEF)]


def path(*parts):
    p = os.path.join(*parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def write_json(obj, *parts):
    with open(path(*parts), "w", encoding="utf-8") as f:
        f.write(json.dumps(obj, indent=2) + "\n")


def frame_texture(name):
    base, light, shadow, accent = FRAMES[name]
    rng = random.Random(name)
    img = Image.new("RGBA", (16, 16), base + (255,))
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.12:  # faint wood grain / gold grain
                img.putpixel((x, y), (light if rng.random() < 0.5 else shadow) + (255,))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 0], fill=light + (255,))
    d.rectangle([0, 15, 15, 15], fill=shadow + (255,))
    d.rectangle([0, 0, 0, 15], fill=light + (255,))
    d.rectangle([15, 0, 15, 15], fill=shadow + (255,))
    # little hearts at the corners of the frame
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        img.putpixel((cx, cy), accent + (255,))
    return img


def back_texture():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            img.putpixel((x, y), SILVER[min(2, (x + y) // 11)] + (255,))
    d = ImageDraw.Draw(img)
    d.line([(3, 15), (15, 3)], fill=(0xF2, 0xF6, 0xF8, 255))
    return img


def glass_texture():
    """Front face of the 12x28 pane is u 0..12, v 0..28: a faint tint and one soft diagonal sheen near the top."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for v in range(28):
        for u in range(24):
            x = u % 12
            band = x + v * 0.5  # a line rising to the right
            alpha = 22
            if 9 <= band < 12:
                alpha = 60
            elif 12 <= band < 13:
                alpha = 38
            img.putpixel((u, v), (0xEE, 0xF4, 0xFF, alpha))
    return img


def floor_texture():
    """Neutral light texture for the reflected floor; MirrorRenderer tints it with the floor's map color."""
    rng = random.Random("floor")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 236 + rng.randint(-10, 10)
            img.putpixel((x, y), (v, v, v, 255))
    return img


def item_icon(name):
    base, light, shadow, accent = FRAMES[name]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    outline = (0x4A, 0x20, 0x38, 255)
    d.rectangle([3, 0, 12, 15], fill=outline)
    d.rectangle([4, 1, 11, 14], fill=base + (255,))
    d.rectangle([5, 2, 10, 13], fill=SILVER[1] + (255,))
    d.line([(6, 9), (9, 4)], fill=(0xF8, 0xFB, 0xFF, 255))
    d.line([(7, 11), (9, 8)], fill=(0xF8, 0xFB, 0xFF, 255))
    img.putpixel((4, 1), light + (255,))
    img.putpixel((7, 0), accent + (255,))
    img.putpixel((8, 0), accent + (255,))
    return img


def face(texture, uv=None):
    f = {"texture": texture}
    if uv:
        f["uv"] = uv
    return f


def element(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def half_model(upper):
    """Model for a mirror facing north: a 4 px deep board at the back of the block, glass side (north) open."""
    fr, bk = "#frame", "#back"
    elements = [
        element([0, 0, 15], [16, 16, 16], {"north": face(bk), "south": face(fr), "east": face(fr), "west": face(fr),
                                            "up": face(fr), "down": face(fr)}),
        element([0, 0, 12], [2, 16, 15], {"north": face(fr), "east": face(bk), "west": face(fr),
                                           "up": face(fr), "down": face(fr)}),
        element([14, 0, 12], [16, 16, 15], {"north": face(fr), "west": face(bk), "east": face(fr),
                                             "up": face(fr), "down": face(fr)}),
    ]
    if upper:
        elements.append(element([2, 14, 12], [14, 16, 15], {"north": face(fr), "down": face(bk), "up": face(fr)}))
    else:
        elements.append(element([2, 0, 12], [14, 2, 15], {"north": face(fr), "up": face(bk), "down": face(fr)}))
    return {"parent": "minecraft:block/block", "textures": {"particle": "#frame"}, "elements": elements}


def main():
    frame_texture("mirror").save(path(ASSETS, "textures", "block", "mirror_frame.png"))
    frame_texture("gilded_mirror").save(path(ASSETS, "textures", "block", "gilded_mirror_frame.png"))
    back_texture().save(path(ASSETS, "textures", "block", "mirror_back.png"))
    glass_texture().save(path(ASSETS, "textures", "entity", "mirror_glass.png"))
    floor_texture().save(path(ASSETS, "textures", "entity", "mirror_floor.png"))
    for half in ("lower", "upper"):
        write_json(half_model(half == "upper"), ASSETS, "models", "block", "mirror_template_" + half + ".json")
    for name in FRAMES:
        item_icon(name).save(path(ASSETS, "textures", "item", name + ".png"))
        for half in ("lower", "upper"):
            write_json({"parent": "femboymod:block/mirror_template_" + half,
                        "textures": {"frame": "femboymod:block/" + name + "_frame", "back": "femboymod:block/mirror_back"}},
                       ASSETS, "models", "block", name + "_" + half + ".json")
        variants = {}
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            for half in ("lower", "upper"):
                v = {"model": "femboymod:block/" + name + "_" + half}
                if y:
                    v["y"] = y
                variants["facing=" + facing + ",half=" + half] = v
        write_json({"variants": variants}, ASSETS, "blockstates", name + ".json")
        write_json({"parent": "minecraft:item/generated", "textures": {"layer0": "femboymod:item/" + name}},
                   ASSETS, "models", "item", name + ".json")
        # item definition: on 1.20.1 only read for tints (client/ItemTints); kept like the other items
        write_json({"model": {"type": "minecraft:model", "model": "femboymod:item/" + name}}, ASSETS, "items", name + ".json")
        # like vanilla doors: only the lower half drops the item (1.20.1 loot format: condition lists)
        write_json({"type": "minecraft:block", "pools": [{
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item",
                         "conditions": [{"block": "femboymod:" + name, "condition": "minecraft:block_state_property",
                                         "properties": {"half": "lower"}}],
                         "name": "femboymod:" + name}],
            "rolls": 1.0}], "random_sequence": "femboymod:blocks/" + name}, DATA, "loot_tables", "blocks", name + ".json")
    print("wrote mirror assets")


if __name__ == "__main__":
    main()
