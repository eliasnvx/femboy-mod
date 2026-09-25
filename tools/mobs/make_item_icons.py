#!/usr/bin/env python3
"""PLACEHOLDER 32x32 item icons for the hostile mob drops and spawn eggs (drawn in code, no AI)."""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod", "textures", "item")
SIZE = 32


def hexc(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def outline(img, color):
    px = img.load()
    src = img.copy().load()
    for y in range(SIZE):
        for x in range(SIZE):
            if src[x, y][3] == 0 and any(0 <= x + dx < SIZE and 0 <= y + dy < SIZE and src[x + dx, y + dy][3]
                                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = color


def spawn_egg(base, spots, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cx, cy = 15.5, 17.0
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = (x - cx) / 10.0, (y - cy) / 13.0
            if dy < 0:
                dx *= 1.15  # narrower top
            if dx * dx + dy * dy <= 1.0:
                light = 1.12 - 0.35 * (dx + dy + 1) / 2
                img.putpixel((x, y), shade(base, light))
    for _ in range(9):
        sx, sy, r = rng.randint(9, 22), rng.randint(8, 26), rng.choice((1, 1, 2))
        for y in range(sy - r, sy + r + 1):
            for x in range(sx - r, sx + r + 1):
                if (x - sx) ** 2 + (y - sy) ** 2 <= r * r + 0.5 and img.getpixel((x, y))[3]:
                    img.putpixel((x, y), spots)
    img.putpixel((12, 9), shade(base, 1.5))
    img.putpixel((12, 10), shade(base, 1.4))
    outline(img, shade(base, 0.45))
    return img


def glitch_shard():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    core, light, magenta = hexc("#3CE67A"), hexc("#B8FFD0"), hexc("#FF4FC3")
    poly = [(16, 3), (24, 12), (21, 28), (11, 29), (8, 14)]  # jagged crystal
    def inside(x, y, pts):
        c = False
        for i in range(len(pts)):
            (x1, y1), (x2, y2) = pts[i], pts[(i + 1) % len(pts)]
            if (y1 > y) != (y2 > y) and x < (x2 - x1) * (y - y1) / (y2 - y1) + x1:
                c = not c
        return c
    for y in range(SIZE):
        for x in range(SIZE):
            if inside(x + 0.5, y + 0.5, [(px - 2, py) for px, py in poly]) and not inside(x + 0.5, y + 0.5, poly):
                img.putpixel((x, y), magenta)            # chromatic offset on the left
            if inside(x + 0.5, y + 0.5, poly):
                facet = light if x < 16 - (y - 3) * 0.2 else core
                img.putpixel((x, y), shade(facet, 1.0 - (y / SIZE) * 0.25))
    snapshot = img.copy()
    for y in range(8, 27, 6):                            # scanline glitch rows, shifted one pixel right
        for x in range(SIZE - 1):
            p = snapshot.getpixel((x, y))
            if p[3]:
                img.putpixel((x + 1, y), p)
    outline(img, hexc("#1B3A2A"))
    return img


# Same masks as CosmeticModels.EarShape (M outer, A inner, F fur, D dark tip); greys are dyed in game.
EARS = {
    "fox": ["...DD...", "..DDDD..", "..DMMD..", "..MAAM..", ".MMAAMM.", ".MAAAAM.", "MMAFFAMM", "MAFFFFAM",
            "MAFFFFAM", "MFFFFFFM", "MFFFFFFM"],
    "bunny": ["..MM..", ".MMMM.", ".MFFM.", "MMFFMM", "MFFFFM", "MFFFFM", "MFFFFM", "MFFFFM", "MFFFFM", "MFFFFM",
              "MFFFFM", "MMFFMM", ".MFFM.", ".MMMM."],
    "bear": [".MMMM.", "MMAAMM", "MAFFAM", "MAFFAM", "MMMMMM"],
    "wolf": ["...MM...", "..MMMM..", "..MAAM..", ".MMAAMM.", ".MAAAAM.", "MMAFFAMM", "MAFFFFAM", "MAFFFFAM",
             "MFFFFFFM"],
}
EAR_GREYS = {"M": hexc("#D6D6D6"), "A": hexc("#ABABAB"), "F": hexc("#FFFFFF"), "D": hexc("#3A3A3A")}


def ears_icon(kind):
    mask = EARS[kind]
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    w, h = len(mask[0]), len(mask)
    base_y = 23
    for cx in (9, 22):                                   # two ears standing on the headband
        x0, y0 = cx - w // 2, base_y - h
        for r, row in enumerate(mask):
            for c, ch in enumerate(row):
                if ch in EAR_GREYS:
                    img.putpixel((x0 + c, y0 + r), EAR_GREYS[ch])
    for x in range(4, 28):                               # headband arc
        y = base_y + int(round(((x - 15.5) / 12) ** 2 * 4))
        for t in range(2):
            img.putpixel((x, min(SIZE - 1, y + t)), EAR_GREYS["M"] if t == 0 else EAR_GREYS["A"])
    outline(img, hexc("#6E6E6E"))
    return img


ICONS = {
    "fox_ears.png": lambda: ears_icon("fox"),
    "bunny_ears.png": lambda: ears_icon("bunny"),
    "bear_ears.png": lambda: ears_icon("bear"),
    "wolf_ears.png": lambda: ears_icon("wolf"),
    "glitch_shard.png": glitch_shard,
    "bug_spawn_egg.png": lambda: spawn_egg(hexc("#3B2A57"), hexc("#5CFF8A"), 1),
}


def main():
    for name, make in ICONS.items():
        make().save(os.path.join(ROOT, name))
        print("wrote", name)


if __name__ == "__main__":
    main()
