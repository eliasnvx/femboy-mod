#!/usr/bin/env python3
"""Chibi plushie block models + UV-packed texture atlases (SPEC §5.6). PLACEHOLDER art, no AI.

Each plush is a list of boxes in block units (0..16). Every face gets its own region in a texture atlas
(TEXELS_PER_UNIT texels per unit, so faces are not stretched), painted with a soft fabric fill, darker seams
and, on chosen faces, hand-placed features (eyes with highlights, blush, belly, inner ears).

    python3 tools/plush/make_plushies.py            # writes models/block/*_plush.json + textures/block/*_plush.png

The face points north; blockstates rotate it toward the player. Polish the atlases by hand before release.
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod")
TEXELS_PER_UNIT = 2
FACES = ("north", "south", "east", "west", "up", "down")


def hexc(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


class Box:
    def __init__(self, name, frm, to, color, paint=None, seam=True):
        self.name, self.frm, self.to, self.color = name, frm, to, color
        self.paint = paint or {}  # face -> fn(img, w, h)
        self.seam = seam

    def face_size(self, face):
        (x0, y0, z0), (x1, y1, z1) = self.frm, self.to
        w, h, d = x1 - x0, y1 - y0, z1 - z0
        return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[face]


def fabric_face(w, h, color, rng, seam):
    img = Image.new("RGBA", (w, h), color)
    px = img.load()
    for y in range(h):
        for x in range(w):
            n = rng.choice((0.97, 1.0, 1.0, 1.0, 1.03))
            if seam and (x == 0 or y == 0 or x == w - 1 or y == h - 1):
                n *= 0.9  # soft stitched seam
            px[x, y] = shade(color, n)
    return img


def pack(boxes, rng):
    """Shelf-pack every face region; returns atlas image and {(box, face): (u0, v0, u1, v1) in texels}."""
    regions = []
    for box in boxes:
        for face in FACES:
            fw, fh = box.face_size(face)
            w, h = max(1, round(fw * TEXELS_PER_UNIT)), max(1, round(fh * TEXELS_PER_UNIT))
            regions.append((box, face, w, h))
    regions.sort(key=lambda r: -r[3])
    size = 32
    while True:
        placed, x, y, shelf = {}, 0, 0, 0
        ok = True
        for box, face, w, h in regions:
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if y + h > size or w > size:
                ok = False
                break
            placed[(box.name, face)] = (x, y, x + w, y + h)
            x, shelf = x + w, max(shelf, h)
        if ok:
            break
        size *= 2
    atlas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for box, face, w, h in regions:
        u0, v0, u1, v1 = placed[(box.name, face)]
        light = {"up": 1.04, "down": 0.9}.get(face, 1.0)
        img = fabric_face(w, h, shade(box.color, light), rng, box.seam)
        if face in box.paint:
            box.paint[face](img, w, h)
        atlas.paste(img, (u0, v0))
    return atlas, placed


def model_json(name, boxes, placed, size, gui_scale, gui_translation):
    tex = f"femboymod:block/{name}"
    k = 16 / size
    elements = []
    for box in boxes:
        faces = {}
        for face in FACES:
            u0, v0, u1, v1 = placed[(box.name, face)]
            faces[face] = {"texture": "#0", "uv": [round(u0 * k, 4), round(v0 * k, 4), round(u1 * k, 4), round(v1 * k, 4)]}
        elements.append({"name": box.name, "from": list(box.frm), "to": list(box.to), "faces": faces})
    return {
        "parent": "minecraft:block/block",
        "texture_size": [size, size],
        "textures": {"0": tex, "particle": tex},
        "elements": elements,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": gui_translation, "scale": [gui_scale] * 3},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]},
            "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.55, 0.55, 0.55]},
        },
    }


# ---------------------------------------------------------------- face features

def put(img, pts, color):
    px = img.load()
    for x, y in pts:
        if 0 <= x < img.width and 0 <= y < img.height:
            px[x, y] = color


def rect(x0, y0, w, h):
    return [(x, y) for y in range(y0, y0 + h) for x in range(x0, x0 + w)]


EYE = hexc("#2A2233")
SHINE = hexc("#FFFFFF")
BLUSH = hexc("#F6A9C2")


def eyes(img, cx_left, cx_right, top, w=3, h=3):
    """Big dark eyes with a white highlight in the top-left corner."""
    for cx in (cx_left, cx_right):
        put(img, rect(cx, top, w, h), EYE)
        put(img, [(cx, top)], SHINE)
        if h > 3:
            put(img, [(cx + 1, top + 1)], SHINE)


def blush(img, x_left, x_right, y, w=2):
    put(img, rect(x_left, y, w, 1), BLUSH)
    put(img, rect(x_right, y, w, 1), BLUSH)


# ---------------------------------------------------------------- designs

def cat():
    white, pink = hexc("#F4EEF1"), hexc("#F4A7C0")

    def face(img, w, h):  # head north face: 18 x 14 texels
        eyes(img, 4, 11, 6, 3, 4)
        put(img, [(8, 10), (9, 10)], pink)                        # nose
        put(img, [(7, 11), (8, 12), (9, 12), (10, 11)], EYE)       # "w" mouth
        blush(img, 2, 14, 11)

    def inner_ear(img, w, h):
        put(img, rect(1, 1, max(1, w - 2), max(1, h - 1)), pink)

    def toes(img, w, h):
        put(img, [(1, h - 2), (w - 2, h - 2)], pink)

    return [
        Box("body", (5, 0, 6), (11, 4.5, 11), white),
        Box("head", (3.5, 4, 4.5), (12.5, 11, 11.5), white, {"north": face}),
        Box("head_cap", (4, 11, 5), (12, 11.5, 11), white),  # rounded top edge
        Box("ear_left", (4, 11.5, 6.5), (7.5, 12.5, 8.5), white, {"north": inner_ear}),
        Box("ear_left_mid", (4.5, 12.5, 7), (6.5, 13.5, 8.5), white, {"north": inner_ear}),
        Box("ear_left_tip", (4.5, 13.5, 7.5), (5.5, 14.5, 8.5), white),
        Box("ear_right", (8.5, 11.5, 6.5), (12, 12.5, 8.5), white, {"north": inner_ear}),
        Box("ear_right_mid", (9.5, 12.5, 7), (11.5, 13.5, 8.5), white, {"north": inner_ear}),
        Box("ear_right_tip", (10.5, 13.5, 7.5), (11.5, 14.5, 8.5), white),
        Box("paw_left", (5.5, 0, 4.5), (7.5, 1.5, 6.5), white, {"north": toes}),
        Box("paw_right", (8.5, 0, 4.5), (10.5, 1.5, 6.5), white, {"north": toes}),
        Box("tail", (10, 0.5, 9.5), (14, 2, 11), white),
        Box("tail_up", (12.5, 2, 9.5), (14, 6.5, 11), white),
        Box("tail_tip", (12.5, 6.5, 9.5), (14, 7.5, 11), pink),
    ]


def shark():
    blue, belly = hexc("#86AFD0"), hexc("#F3F3EF")
    mouth, tongue = hexc("#3A2A38"), hexc("#F28AA5")

    def face(img, w, h):  # head north face: 14 x 10 texels
        put(img, rect(0, h - 4, w, 4), belly)
        eyes(img, 2, 10, 2, 2, 3)
        put(img, rect(5, 7, 4, 1), mouth)
        put(img, rect(6, 8, 2, 1), tongue)
        blush(img, 0, 12, 6)

    def belly_side(img, w, h):
        put(img, rect(0, h - 3, w, 3), belly)

    def belly_bottom(img, w, h):
        put(img, rect(0, 0, w, h), belly)

    def gills(img, w, h):
        belly_side(img, w, h)
        gill = shade(blue, 0.78)
        for x in (w - 3, w - 5, w - 7):  # three gill slits behind the eyes, toward the front
            put(img, [(x, 2), (x, 3), (x, 4)], gill)

    def gills_west(img, w, h):
        belly_side(img, w, h)
        gill = shade(blue, 0.78)
        for x in (2, 4, 6):
            put(img, [(x, 2), (x, 3), (x, 4)], gill)

    return [
        Box("head", (4.5, 0, 2.5), (11.5, 5, 7), blue, {"north": face, "east": gills, "west": gills_west, "down": belly_bottom}),
        Box("head_cap", (5, 5, 3), (11, 5.5, 7), blue),
        Box("body", (5, 0.5, 7), (11, 4.5, 12), blue, {"east": belly_side, "west": belly_side, "down": belly_bottom}),
        Box("tail_stem", (6.5, 1.5, 12), (9.5, 3.5, 14), blue),
        Box("tail_fin", (7.25, 0.5, 14), (8.75, 6.5, 15.5), blue),
        Box("dorsal_fin", (7.25, 5, 6), (8.75, 7, 8.5), blue),
        Box("dorsal_tip", (7.25, 7, 7), (8.75, 8, 8.5), blue),
        Box("fin_left", (2.5, 0, 6), (4.5, 1, 8.5), blue),
        Box("fin_right", (11.5, 0, 6), (13.5, 1, 8.5), blue),
    ]


def creeper():
    green, dark, feet = hexc("#86D079"), hexc("#2E3D2B"), hexc("#6DB862")
    rng = random.Random(7)

    def mottle(img, w, h):
        px = img.load()
        for _ in range(w * h // 5):
            x, y = rng.randrange(w), rng.randrange(h)
            px[x, y] = shade(green, rng.choice((0.86, 1.1)))

    def face(img, w, h):  # head north face: 16 x 16 texels
        mottle(img, w, h)
        for cx in (3, 10):  # rounded eyes with a highlight
            put(img, rect(cx, 5, 3, 3), dark)
            put(img, [(cx, 5)], SHINE)
        put(img, rect(7, 8, 2, 2), dark)                   # small creeper mouth
        put(img, rect(6, 10, 4, 2), dark)
        put(img, [(6, 11), (9, 11)], shade(green, 0.9))
        blush(img, 1, 13, 9)

    return [
        Box("head", (4, 5, 4), (12, 13, 12), green, {"north": face, "east": mottle, "west": mottle, "south": mottle, "up": mottle}),
        Box("head_cap", (4.5, 13, 4.5), (11.5, 13.5, 11.5), green, {"up": mottle}),
        Box("body", (5.5, 2, 5.5), (10.5, 5, 10.5), green, {"north": mottle, "east": mottle, "west": mottle, "south": mottle}),
        Box("leg_fl", (5, 0, 4), (7.5, 2, 6.5), feet),
        Box("leg_fr", (8.5, 0, 4), (11, 2, 6.5), feet),
        Box("leg_bl", (5, 0, 9.5), (7.5, 2, 12), feet),
        Box("leg_br", (8.5, 0, 9.5), (11, 2, 12), feet),
    ]


PLUSHIES = {
    # name: (design, gui scale, gui translation)
    "cat_plush": (cat, 0.78, [0, 0, 0]),
    "shark_plush": (shark, 0.95, [0, 2, 0]),
    "creeper_plush": (creeper, 0.78, [0, 0, 0]),
}


def main():
    for name, (design, gui_scale, gui_translation) in PLUSHIES.items():
        boxes = design()
        atlas, placed = pack(boxes, random.Random(name))
        atlas.save(os.path.join(ROOT, "textures", "block", name + ".png"))
        with open(os.path.join(ROOT, "models", "block", name + ".json"), "w") as f:
            json.dump(model_json(name, boxes, placed, atlas.width, gui_scale, gui_translation), f, indent=2)
            f.write("\n")
        top = max(b.to[1] for b in boxes)
        print(f"{name}: {len(boxes)} boxes, atlas {atlas.width}x{atlas.height}, height {top}")


if __name__ == "__main__":
    main()
