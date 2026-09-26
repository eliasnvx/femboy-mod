#!/usr/bin/env python3
"""Mod icon options: 32x32 pixel art drawn in code, upscaled with nearest neighbour. No AI images.

    python3 tools/docs/make_mod_icon.py                 # writes art/icon/options/*.png + a contact sheet
    python3 tools/docs/make_mod_icon.py --pick kitty    # also installs that option as the mod icon (128px) and art/icon/icon_512.png
"""
import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "art" / "icon"
MOD_ICON = ROOT / "common" / "src" / "main" / "resources" / "assets" / "femboymod" / "icon.png"
N = 32

OUTLINE = (91, 42, 79, 255)
PINK = (255, 143, 200, 255)
PINK_DARK = (226, 90, 160, 255)
PINK_LIGHT = (255, 199, 228, 255)
WHITE = (255, 246, 251, 255)
LAVENDER = (190, 160, 255, 255)
LAVENDER_DARK = (140, 110, 220, 255)
BLUSH = (255, 120, 170, 255)
SPARKLE = (255, 255, 255, 255)

BACKGROUNDS = {
    "pink": ((255, 214, 236), (222, 196, 255)),
    "lavender": ((226, 208, 255), (255, 196, 228)),
    "night": ((74, 48, 110), (150, 70, 140)),
}


def background(name):
    top, bottom = BACKGROUNDS[name]
    img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    radius = 6
    for y in range(N):
        t = y / (N - 1)
        c = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,)
        for x in range(N):
            dx = max(radius - x, 0, x - (N - 1 - radius))
            dy = max(radius - y, 0, y - (N - 1 - radius))
            if dx * dx + dy * dy <= radius * radius:
                img.putpixel((x, y), c)
    edge = tuple(max(0, v - 60) for v in bottom) + (255,)
    outline_layer(img, img.copy(), edge, inside=True)
    return img


def mask():
    return Image.new("L", (N, N), 0)


def outline_layer(canvas, layer, color, inside=False):
    """Paint a 1px outline around (or just inside) the opaque pixels of layer."""
    a = layer.getchannel("A").load()
    px = canvas.load()
    for y in range(N):
        for x in range(N):
            filled = a[x, y] > 0
            if filled != inside:
                continue
            near = [(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if inside:
                if any(not (0 <= nx < N and 0 <= ny < N) or a[nx, ny] == 0 for nx, ny in near):
                    px[x, y] = color
            elif any(0 <= nx < N and 0 <= ny < N and a[nx, ny] > 0 for nx, ny in near):
                px[x, y] = color


def stamp(canvas, layer, outline=OUTLINE):
    outline_layer(canvas, layer, outline)
    canvas.alpha_composite(layer)


def fill(layer_draw_fn, color):
    m = mask()
    layer_draw_fn(ImageDraw.Draw(m))
    layer = Image.new("RGBA", (N, N), color)
    layer.putalpha(m)
    return layer


def disc(cx, cy, r):
    return lambda d: [d.point((x, y), 255) for y in range(N) for x in range(N)
                      if (x - cx) ** 2 + (y - cy) ** 2 <= r * r]


def heart_points(cx, cy, s):
    pts = []
    for y in range(N):
        for x in range(N):
            u, v = (x - cx) / s, -(y - cy) / s
            if (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0:
                pts.append((x, y))
    return pts


def heart(cx, cy, s):
    return lambda d: [d.point(p, 255) for p in heart_points(cx, cy, s)]


def sparkle(img, x, y, color=SPARKLE, big=False):
    px = img.load()
    for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)) + (((2, 0), (-2, 0), (0, 2), (0, -2)) if big else ()):
        if 0 <= x + dx < N and 0 <= y + dy < N:
            px[x + dx, y + dy] = color


def shade(img, layer, color, test):
    a = layer.getchannel("A").load()
    px = img.load()
    for y in range(N):
        for x in range(N):
            if a[x, y] and test(x, y):
                px[x, y] = color


# ---------------------------------------------------------------- options

def kitty():
    """Round cat face with blush and ^ ^ eyes."""
    img = background("pink")
    head = Image.alpha_composite(
        fill(disc(16, 19, 10), WHITE),
        Image.alpha_composite(fill(lambda d: d.polygon([(6, 15), (7, 4), (14, 11)], 255), WHITE),
                              fill(lambda d: d.polygon([(26, 15), (25, 4), (18, 11)], 255), WHITE)))
    stamp(img, head)
    inner = Image.alpha_composite(fill(lambda d: d.polygon([(8, 12), (8, 7), (12, 11)], 255), PINK),
                                  fill(lambda d: d.polygon([(24, 12), (24, 7), (20, 11)], 255), PINK))
    img.alpha_composite(inner)
    shade(img, head, PINK_LIGHT, lambda x, y: (x - 16) ** 2 + (y - 19) ** 2 <= 100 and y >= 26)
    d = ImageDraw.Draw(img)
    for ex in (11, 21):  # ^ ^ eyes
        d.point([(ex - 2, 19), (ex - 1, 18), (ex, 17), (ex + 1, 18), (ex + 2, 19)], OUTLINE)
    d.rectangle([7, 21, 9, 22], BLUSH)
    d.rectangle([23, 21, 25, 22], BLUSH)
    d.point([(14, 23), (15, 24), (16, 23), (17, 24), (18, 23)], OUTLINE)  # w mouth
    sparkle(img, 27, 27, big=True)
    return img


def sock():
    """Striped thigh-high programming sock."""
    img = background("lavender")
    shape = lambda d: (d.rectangle([11, 3, 19, 23], 255), d.rounded_rectangle([11, 18, 27, 28], 4, 255))
    layer = fill(shape, WHITE)
    stamp(img, layer)
    shade(img, layer, PINK, lambda x, y: y >= 6 and (y // 3) % 2 == 0 and y < 18)
    shade(img, layer, PINK, lambda x, y: y >= 18 and x < 22 and (y // 3) % 2 == 0)
    shade(img, layer, PINK_LIGHT, lambda x, y: y <= 4)  # cuff
    shade(img, layer, PINK_DARK, lambda x, y: x >= 23 and y >= 19)  # toe
    shade(img, layer, PINK_DARK, lambda x, y: y >= 26 and x <= 15)  # heel
    sparkle(img, 5, 8, big=True)
    sparkle(img, 25, 9)
    return img


def headphones():
    """Cat-ear gamer headphones with a heart."""
    img = background("night")
    ring = lambda d: [d.point((x, y), 255) for y in range(N) for x in range(N)
                      if 8.5 <= math.hypot(x - 16, y - 17) <= 11.5 and y <= 18]
    band = fill(ring, PINK)
    ears = Image.alpha_composite(fill(lambda d: d.polygon([(6, 10), (7, 2), (13, 7)], 255), PINK),
                                 fill(lambda d: d.polygon([(26, 10), (25, 2), (19, 7)], 255), PINK))
    cups = fill(lambda d: (d.rounded_rectangle([2, 15, 8, 26], 2, 255), d.rounded_rectangle([24, 15, 30, 26], 2, 255)), PINK)
    body = Image.alpha_composite(Image.alpha_composite(band, ears), cups)
    stamp(img, body, (40, 20, 50, 255))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 8), (8, 5), (11, 7)], WHITE)
    d.polygon([(24, 8), (24, 5), (21, 7)], WHITE)
    d.rectangle([4, 17, 6, 24], PINK_LIGHT)
    d.rectangle([26, 17, 28, 24], PINK_LIGHT)
    h = fill(heart(16, 21, 5.2), PINK_LIGHT)
    stamp(img, h, (40, 20, 50, 255))
    shade(img, h, WHITE, lambda x, y: y <= 18 and x <= 14)
    sparkle(img, 27, 4)
    sparkle(img, 4, 29)
    return img


def heart_cat():
    """A heart with cat ears."""
    img = background("pink")
    body = Image.alpha_composite(
        fill(heart(16, 17, 11.5), PINK),
        Image.alpha_composite(fill(lambda d: d.polygon([(5, 10), (6, 2), (12, 6)], 255), PINK),
                              fill(lambda d: d.polygon([(27, 10), (26, 2), (20, 6)], 255), PINK)))
    stamp(img, body)
    d = ImageDraw.Draw(img)
    d.polygon([(7, 7), (7, 4), (10, 6)], PINK_LIGHT)
    d.polygon([(25, 7), (25, 4), (22, 6)], PINK_LIGHT)
    shade(img, body, PINK_DARK, lambda x, y: y >= 22 or x >= 26)
    d.rectangle([10, 13, 11, 15], OUTLINE)
    d.rectangle([20, 13, 21, 15], OUTLINE)
    d.point([(10, 13), (20, 13)], WHITE)
    d.rectangle([7, 17, 9, 18], BLUSH)
    d.rectangle([22, 17, 24, 18], BLUSH)
    d.point([(14, 18), (15, 19), (16, 18), (17, 19), (18, 18)], OUTLINE)
    sparkle(img, 8, 10, WHITE)
    sparkle(img, 28, 26, big=True)
    return img


def hoodie_kitty():
    """Kitty face peeking out of a cat-ear hoodie."""
    img = background("lavender")
    hood = Image.alpha_composite(
        fill(lambda d: d.rounded_rectangle([3, 7, 28, 31], 10, 255), PINK),
        Image.alpha_composite(fill(lambda d: d.polygon([(4, 14), (5, 1), (13, 8)], 255), PINK),
                              fill(lambda d: d.polygon([(27, 14), (26, 1), (18, 8)], 255), PINK)))
    stamp(img, hood)
    d = ImageDraw.Draw(img)
    d.polygon([(6, 9), (6, 4), (10, 8)], PINK_LIGHT)
    d.polygon([(25, 9), (25, 4), (21, 8)], PINK_LIGHT)
    face = fill(disc(16, 19, 8), WHITE)
    stamp(img, face, PINK_DARK)
    for ex in (12, 20):
        d.rectangle([ex, 18, ex + 1, 20], OUTLINE)
        d.point((ex, 18), WHITE)
    d.rectangle([9, 22, 10, 23], BLUSH)
    d.rectangle([22, 22, 23, 23], BLUSH)
    d.point([(14, 23), (15, 24), (16, 23), (17, 24), (18, 23)], OUTLINE)
    d.line([(12, 28), (12, 31)], WHITE)  # hoodie strings
    d.line([(20, 28), (20, 31)], WHITE)
    sparkle(img, 28, 4, big=True)
    return img


OPTIONS = {
    "kitty": ("Kitty", kitty),
    "heart_cat": ("Heart cat", heart_cat),
    "hoodie": ("Hoodie kitty", hoodie_kitty),
    "headphones": ("Headphones", headphones),
    "sock": ("Sock", sock),
}


def upscale(img, size):
    return img.resize((size, size), Image.NEAREST)


def contact_sheet(images):
    cell, pad, label = 256, 24, 28
    sheet = Image.new("RGBA", (pad + len(images) * (cell + pad), cell + 2 * pad + label), (250, 244, 250, 255))
    d = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("/System/Library/Fonts/Supplemental/Arial Bold.ttf", 20)
    except OSError:
        font = ImageFont.load_default()
    for i, (key, (name, img)) in enumerate(images.items()):
        x = pad + i * (cell + pad)
        sheet.alpha_composite(upscale(img, cell), (x, pad))
        text = f"{i + 1}. {name}"
        w = d.textlength(text, font=font)
        d.text((x + (cell - w) / 2, pad + cell + 6), text, fill=OUTLINE, font=font)
        # small preview at real mod-list size
        sheet.alpha_composite(upscale(img, 32), (x + cell - 36, pad + 4))
    return sheet


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pick", choices=OPTIONS, help="install this option as the mod icon")
    args = parser.parse_args()
    (OUT / "options").mkdir(parents=True, exist_ok=True)
    rendered = {k: (name, fn()) for k, (name, fn) in OPTIONS.items()}
    for key, (_, img) in rendered.items():
        upscale(img, 512).save(OUT / "options" / f"{key}_512.png")
    contact_sheet(rendered).save(OUT / "options" / "contact_sheet.png")
    if args.pick:
        img = rendered[args.pick][1]
        upscale(img, 512).save(OUT / "icon_512.png")
        img.save(OUT / "icon_32.png")
        MOD_ICON.parent.mkdir(parents=True, exist_ok=True)
        upscale(img, 128).save(MOD_ICON)
        print("installed", args.pick, "->", MOD_ICON.relative_to(ROOT))
    print("wrote", OUT / "options")


if __name__ == "__main__":
    main()
