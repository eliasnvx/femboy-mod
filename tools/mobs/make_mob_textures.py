#!/usr/bin/env python3
"""PLACEHOLDER entity textures for the hostile meme mobs, painted on vanilla box-UV layouts. No AI.

    python3 tools/mobs/make_mob_textures.py

Box UV of addBox(w, h, d) at texOffs(u, v): up (u+d, v, w x d), down (u+d+w, v, w x d),
west (u, v+d, d x h), north (u+d, v+d, w x h), east (u+d+w, v+d, d x h), south (u+2d+w, v+d, w x h).
"""
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod", "textures", "entity")


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def faces(u, v, w, h, d):
    return {
        "up": (u + d, v, w, d), "down": (u + d + w, v, w, d),
        "west": (u, v + d, d, h), "north": (u + d, v + d, w, h),
        "east": (u + d + w, v + d, d, h), "south": (u + 2 * d + w, v + d, w, h),
    }


def fill(img, rect, color, rng=None, noise=(1.0,)):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), shade(color, rng.choice(noise)) if rng else color)


def box(img, u, v, w, h, d, color, rng, noise=(0.92, 1.0, 1.0, 1.06), light=True):
    for name, rect in faces(u, v, w, h, d).items():
        f = {"up": 1.08, "down": 0.8}.get(name, 1.0) if light else 1.0
        fill(img, rect, shade(color, f), rng, noise)


def bug():
    rng = random.Random(3)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    shell, dark, glitch, pink = hexc("#3B2A57"), hexc("#241936"), hexc("#5CFF8A"), hexc("#FF5CC8")
    box(img, 0, 0, 6, 4, 8, shell, rng)
    up = faces(0, 0, 6, 4, 8)["up"]
    x0, y0, w, h = up
    for y in range(y0, y0 + h):              # shell seam down the middle + glitchy "code" pixels
        img.putpixel((x0 + 2, y), dark)
        img.putpixel((x0 + 3, y), dark)
    for _ in range(9):
        x, y = rng.randrange(w), rng.randrange(h)
        if x not in (2, 3):
            img.putpixel((x0 + x, y0 + y), rng.choice((glitch, glitch, pink)))
    for name in ("west", "east", "south"):
        fx, fy, fw, fh = faces(0, 0, 6, 4, 8)[name]
        for x in range(fx, fx + fw):
            img.putpixel((x, fy + fh - 1), dark)   # darker belly edge
        for _ in range(3):
            img.putpixel((fx + rng.randrange(fw), fy + rng.randrange(fh - 1)), glitch)
    box(img, 0, 12, 4, 3, 3, dark, rng)
    nx, ny, _, _ = faces(0, 12, 4, 3, 3)["north"]
    for ex in (nx, nx + 3):                   # glowing eyes
        img.putpixel((ex, ny + 1), glitch)
    img.putpixel((nx + 1, ny + 2), pink)      # little mandibles
    img.putpixel((nx + 2, ny + 2), pink)
    box(img, 14, 12, 1, 4, 1, dark, rng, light=False)
    tip = faces(14, 12, 1, 4, 1)
    for name in ("west", "north", "east", "south"):
        x, y, _, _ = tip[name]
        img.putpixel((x, y), glitch)          # antenna tips
    box(img, 20, 12, 3, 1, 1, dark, rng, light=False)
    return img


TEXTURES = {"bug.png": bug}


def main():
    os.makedirs(ROOT, exist_ok=True)
    for name, make in TEXTURES.items():
        make().save(os.path.join(ROOT, name))
        print("wrote", name)


if __name__ == "__main__":
    main()
