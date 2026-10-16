#!/usr/bin/env python3
"""32x32 icons for the feet slot shoes and the cat paw gloves, drawn from shapes (no AI).

Each item gets two layers like the other dyeable icons: <name>.png is light grey and tinted with the colorway,
<name>_detail.png is drawn as is (outline, soles, laces, pads).

    python3 tools/items/make_wear_icons.py            # writes the textures
    python3 tools/items/make_wear_icons.py --preview  # also build/wear_icons_preview.png, tinted with the defaults
"""
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
OUT = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "femboymod", "textures", "item")
SIZE = 32

OUTLINE = (0x3A, 0x26, 0x32, 255)
WHITE = (0xFA, 0xF7, 0xF4, 255)
SOLE_LINE = (0xC9, 0xBE, 0xC4, 255)
PINK = (0xF2, 0x8F, 0xB8, 255)
DARK = (0x2A, 0x23, 0x28, 255)
# tinted layer greys: shade, base, highlight
G_SHADE = (0xB4, 0xB4, 0xB4, 255)
G_BASE = (0xDC, 0xDC, 0xDC, 255)
G_LIGHT = (0xF4, 0xF4, 0xF4, 255)

# default colors (same as the renderers), used for the preview only
DEFAULTS = {"sneakers": 0xF7B8D2, "canvas_high_tops": 0xC8A2E8, "platform_sneakers": 0xF291BE,
            "bunny_slippers": 0xFFF6F8, "cat_paw_gloves": 0xFFF6F8}


def layers():
    return Image.new("RGBA", (SIZE, SIZE)), Image.new("RGBA", (SIZE, SIZE))


def outline(base, detail):
    """Dark 1 px outline around everything drawn on either layer, into the detail layer."""
    filled = {(x, y) for x in range(SIZE) for y in range(SIZE)
              if base.getpixel((x, y))[3] or detail.getpixel((x, y))[3]}
    for x, y in filled:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < SIZE and 0 <= ny < SIZE and (nx, ny) not in filled:
                detail.putpixel((nx, ny), OUTLINE)


def shade_rows(base, top, light_rows=2, shade_rows=3):
    """Highlight the first rows of the tinted shape and darken its last rows."""
    for x in range(SIZE):
        ys = [y for y in range(SIZE) if base.getpixel((x, y))[3]]
        for i, y in enumerate(ys):
            if i < light_rows:
                base.putpixel((x, y), G_LIGHT)
            elif i >= len(ys) - shade_rows:
                base.putpixel((x, y), G_SHADE)


def shoe(collar_top, sole_h, toe_cap=False, high_laces=False, ankle_patch=False):
    """Side view, toe to the left. collar_top: y of the highest point of the upper; sole_h: sole height."""
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    sole_top = 26 - sole_h
    # upper: rounded toe on the left, heel on the right
    toe = min(sole_top - 5, collar_top + 6)
    b.polygon([(3, sole_top), (3, toe + 2), (7, toe), (13, toe - 2), (18, collar_top + 1),
               (19, collar_top), (27, collar_top), (29, collar_top + 2), (29, sole_top)], fill=G_BASE)
    b.ellipse([2, toe, 10, sole_top + 1], fill=G_BASE)
    shade_rows(base, collar_top)
    # collar opening
    d.line([(19, collar_top), (27, collar_top)], fill=DARK)
    # sole
    d.rectangle([2, sole_top, 29, 25], fill=WHITE)
    for y in range(sole_top + 2, 26, 3):
        d.line([(3, y), (28, y)], fill=SOLE_LINE)
    if toe_cap:
        d.chord([1, toe + 1, 9, sole_top + 2], 90, 270, fill=WHITE)
    # laces: short white bars along the instep
    steps = 4 if high_laces else 3
    for i in range(steps):
        x = 11 + i * 2
        y = toe - 1 - i * (2 if high_laces else 1)
        d.line([(x, y), (x + 2, y + 1)], fill=WHITE)
    if ankle_patch:
        d.ellipse([22, collar_top + 4, 26, collar_top + 8], fill=WHITE)
        detail.putpixel((24, collar_top + 6), PINK)
    else:
        # simple side stripe, no brand shape
        d.line([(14, sole_top - 2), (25, sole_top - 5)], fill=WHITE)
    outline(base, detail)
    return base, detail


def bunny_slippers():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    # fluffy body, toe to the left
    b.ellipse([2, 14, 30, 27], fill=G_BASE)
    b.rectangle([12, 18, 30, 26], fill=G_BASE)
    # ears standing up at the front
    b.polygon([(6, 16), (5, 6), (7, 3), (9, 6), (10, 15)], fill=G_BASE)
    b.polygon([(10, 15), (11, 5), (14, 3), (15, 7), (14, 15)], fill=G_BASE)
    shade_rows(base, 3, light_rows=2, shade_rows=3)
    d.line([(7, 6), (8, 13)], fill=PINK)
    d.line([(12, 6), (12, 13)], fill=PINK)
    # face
    detail.putpixel((6, 19), DARK)
    detail.putpixel((10, 19), DARK)
    detail.putpixel((8, 21), PINK)
    # sole
    d.line([(4, 26), (28, 26)], fill=SOLE_LINE)
    outline(base, detail)
    return base, detail


def cat_paw_gloves():
    base, detail = layers()
    b, d = ImageDraw.Draw(base), ImageDraw.Draw(detail)
    # rounded paw, cuff at the bottom
    b.ellipse([5, 3, 27, 25], fill=G_BASE)
    b.rectangle([8, 18, 24, 28], fill=G_BASE)
    shade_rows(base, 3, light_rows=2, shade_rows=2)
    # fluffy cuff
    d.rectangle([7, 25, 25, 29], fill=WHITE)
    for x in range(8, 25, 3):
        detail.putpixel((x, 25), SOLE_LINE)
    # toe beans and the big pad
    for cx, cy in ((10, 9), (14, 6), (18, 6), (22, 9)):
        d.ellipse([cx - 1, cy - 1, cx + 2, cy + 2], fill=PINK)
    d.ellipse([11, 13, 21, 21], fill=PINK)
    outline(base, detail)
    return base, detail


ICONS = {
    "sneakers": lambda: shoe(collar_top=12, sole_h=3),
    "canvas_high_tops": lambda: shoe(collar_top=6, sole_h=3, toe_cap=True, high_laces=True, ankle_patch=True),
    "platform_sneakers": lambda: shoe(collar_top=9, sole_h=7),
    "bunny_slippers": bunny_slippers,
    "cat_paw_gloves": cat_paw_gloves,
}


def tinted(base, rgb):
    r, g, b = rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255
    out = Image.new("RGBA", base.size)
    for x in range(base.width):
        for y in range(base.height):
            p = base.getpixel((x, y))
            if p[3]:
                out.putpixel((x, y), (p[0] * r // 255, p[1] * g // 255, p[2] * b // 255, p[3]))
    return out


def main():
    made = {}
    for name, draw in ICONS.items():
        base, detail = draw()
        base.save(os.path.join(OUT, name + ".png"))
        detail.save(os.path.join(OUT, name + "_detail.png"))
        made[name] = (base, detail)
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(made) * 136, 272), (0x8B, 0x8B, 0x8B, 255))
        for i, (name, (base, detail)) in enumerate(made.items()):
            icon = tinted(base, DEFAULTS[name])
            icon.alpha_composite(detail)
            sheet.alpha_composite(icon.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4))
            sheet.alpha_composite(icon.resize((32, 32), Image.NEAREST), (i * 136 + 4, 140))
            sheet.alpha_composite(icon.resize((16, 16), Image.BOX), (i * 136 + 44, 140))
        os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
        sheet.save(os.path.join(ROOT, "build", "wear_icons_preview.png"))
    print("wrote", ", ".join(made))


if __name__ == "__main__":
    main()
