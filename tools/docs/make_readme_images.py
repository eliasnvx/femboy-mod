#!/usr/bin/env python3
"""README / mod page images: banner, section headers and screenshot galleries. No AI images (AGENTS.md):
backgrounds, the pixel font and the frames are drawn here, the pictures are real in-game screenshots.

    FEMBOYMOD_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest   # takes the docs_* screenshots
    python3 tools/docs/make_readme_images.py                       # writes docs/images/*.png
    python3 tools/docs/make_readme_images.py --page-base URL       # also writes docs/pages/curseforge.md with absolute image URLs
"""
import argparse
import glob
import math
import os
import re

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
SHOTS = os.path.join(ROOT, "fabric", "build", "run", "clientGameTest", "screenshots")
OUT = os.path.join(ROOT, "docs", "images")
ITEM_TEX = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "femboymod", "textures", "item")

PINK_TOP = (255, 214, 234)
LAVENDER_BOTTOM = (217, 198, 255)
PLUM = (90, 46, 69)
INK = (58, 42, 56)
WHITE = (255, 255, 255)
ACCENT = (242, 145, 190)

# ------------------------------------------------------------------------------------------------ pixel font (5x7)

GLYPHS = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01111"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["111", "010", "010", "010", "010", "010", "111"],
    "J": ["00111", "00010", "00010", "00010", "00010", "10010", "01100"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "0": ["01110", "10011", "10101", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00110", "01000", "10000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    "6": ["00110", "01000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00010", "01100"],
    " ": ["000", "000", "000", "000", "000", "000", "000"],
    ".": ["0", "0", "0", "0", "0", "0", "1"],
    ",": ["00", "00", "00", "00", "00", "01", "10"],
    ":": ["0", "1", "0", "0", "0", "1", "0"],
    "!": ["1", "1", "1", "1", "1", "0", "1"],
    "'": ["1", "1", "0", "0", "0", "0", "0"],
    "-": ["000", "000", "000", "111", "000", "000", "000"],
    "+": ["000", "010", "010", "111", "010", "010", "000"],
    "/": ["00001", "00010", "00010", "00100", "01000", "01000", "10000"],
    "&": ["01100", "10010", "10100", "01000", "10101", "10010", "01101"],
    "%": ["11001", "11010", "00010", "00100", "01000", "01011", "10011"],
    "(": ["01", "10", "10", "10", "10", "10", "01"],
    ")": ["10", "01", "01", "01", "01", "01", "10"],
    "*": ["01010", "11011", "11111", "11111", "01110", "00100", "00000"],  # heart
}


def text_width(text, scale):
    return sum((len(GLYPHS.get(ch, GLYPHS[" "])[0]) + 1) * scale for ch in text.upper()) - scale


def draw_text(img, text, x, y, scale, fill, shadow=PLUM, outline=None):
    """Pixel text; `*` draws a heart. Shadow one font pixel down-right, optional 1px outline."""
    def blit(ox, oy, color):
        cx = ox
        for ch in text.upper():
            glyph = GLYPHS.get(ch, GLYPHS[" "])
            for gy, row in enumerate(glyph):
                for gx, bit in enumerate(row):
                    if bit == "1":
                        ImageDraw.Draw(img).rectangle([cx + gx * scale, oy + gy * scale,
                                                       cx + (gx + 1) * scale - 1, oy + (gy + 1) * scale - 1], fill=color)
            cx += (len(glyph[0]) + 1) * scale
    if outline:
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            blit(x + dx * max(1, scale // 3), y + dy * max(1, scale // 3), outline)
    if shadow:
        blit(x + scale, y + scale, shadow)
    blit(x, y, fill)


# ------------------------------------------------------------------------------------------------ backgrounds

HEART = ["0110110", "1111111", "1111111", "0111110", "0011100", "0001000"]


def gradient(w, h, top=PINK_TOP, bottom=LAVENDER_BOTTOM):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        c = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        for x in range(w):
            px[x, y] = c
    return img


def heart_pattern(img, cell=48, scale=3, alpha=0.18, color=WHITE):
    """Tiled pixel hearts, every other row offset, blended over the background."""
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    a = int(255 * alpha)
    for row, y in enumerate(range(-cell, img.height + cell, cell)):
        for x in range(-cell, img.width + cell, cell):
            ox = x + (cell // 2 if row % 2 else 0)
            for gy, line in enumerate(HEART):
                for gx, bit in enumerate(line):
                    if bit == "1":
                        d.rectangle([ox + gx * scale, y + gy * scale, ox + (gx + 1) * scale - 1, y + (gy + 1) * scale - 1],
                                    fill=color + (a,))
    base = img.convert("RGBA")
    base.alpha_composite(layer)
    return base


def background(w, h):
    return heart_pattern(gradient(w, h))


# ------------------------------------------------------------------------------------------------ frames and chips

def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def framed(picture, border=WHITE, width=8, radius=26, shadow=True):
    """Picture with rounded corners, a colored border and a soft drop shadow (RGBA, larger than the picture)."""
    w, h = picture.size
    pad = 24
    out = Image.new("RGBA", (w + 2 * width + 2 * pad, h + 2 * width + 2 * pad), (0, 0, 0, 0))
    if shadow:
        sh = Image.new("RGBA", out.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle([pad + 6, pad + 10, pad + w + 2 * width + 6, pad + h + 2 * width + 10],
                                             radius=radius + width, fill=(60, 20, 50, 110))
        out.alpha_composite(sh.filter(ImageFilter.GaussianBlur(10)))
    frame = Image.new("RGBA", (w + 2 * width, h + 2 * width), border + (255,))
    out.paste(frame, (pad, pad), rounded_mask(frame.size, radius + width))
    out.paste(picture.convert("RGBA"), (pad + width, pad + width), rounded_mask((w, h), radius))
    return out


def chip(text, scale=3, fill=(255, 239, 247), ink=PLUM, border=ACCENT):
    tw = text_width(text, scale)
    w, h = tw + 12 * scale, 7 * scale + 8 * scale
    img = Image.new("RGBA", (w + 4, h + 4), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([2, 4, w + 1, h + 3], radius=h // 2, fill=(90, 46, 69, 90))
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=h // 2, fill=fill, outline=border, width=max(2, scale // 2 + 1))
    draw_text(img, text, 6 * scale, 4 * scale, scale, ink, shadow=None)
    return img


TRANS = [(91, 206, 250), (245, 169, 184), (255, 255, 255), (245, 169, 184), (91, 206, 250)]


def icon(name, scale=4, tint=None):
    """An item icon from the mod's textures (grey layers tinted like in game), scaled without smoothing."""
    if name == "pride_badge":  # five stripe layers tinted like a trans flag, then the pin
        base = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        for k, color in enumerate(TRANS):
            stripe = Image.open(os.path.join(ITEM_TEX, f"pride_badge_stripe{k}.png")).convert("RGBA")
            px = stripe.load()
            for y in range(32):
                for x in range(32):
                    pr, pg, pb, pa = px[x, y]
                    if pa:
                        px[x, y] = (pr * color[0] // 255, pg * color[1] // 255, pb * color[2] // 255, pa)
            base.alpha_composite(stripe)
        base.alpha_composite(Image.open(os.path.join(ITEM_TEX, "pride_badge_pin.png")).convert("RGBA"))
        return base.resize((32 * scale, 32 * scale), Image.NEAREST)
    base = Image.open(os.path.join(ITEM_TEX, name + ".png")).convert("RGBA")
    if tint:
        r, g, b = tint
        px = base.load()
        for y in range(base.height):
            for x in range(base.width):
                pr, pg, pb, pa = px[x, y]
                if pa and not (pr == pg == pb and pr < 70):
                    px[x, y] = (pr * r // 255, pg * g // 255, pb * b // 255, pa)
    detail = os.path.join(ITEM_TEX, name + "_detail.png")
    if os.path.exists(detail):
        base.alpha_composite(Image.open(detail).convert("RGBA"))
    return base.resize((base.width * scale, base.height * scale), Image.NEAREST)


# ------------------------------------------------------------------------------------------------ screenshots

def shot(name):
    files = sorted(glob.glob(os.path.join(SHOTS, f"*{name}*.png")))
    if not files:
        raise SystemExit(f"missing screenshot {name}: run FEMBOYMOD_DOCS_SHOTS=1 ./gradlew :fabric:runClientGameTest")
    return Image.open(files[-1]).convert("RGB")


def crop_center(img, w, h, cx=0.5, cy=0.5):
    x0 = int(img.width * cx - w / 2)
    y0 = int(img.height * cy - h / 2)
    x0 = max(0, min(img.width - w, x0))
    y0 = max(0, min(img.height - h, y0))
    return img.crop((x0, y0, x0 + w, y0 + h))


def fit(img, w, h):
    """Scale to cover w x h, then crop the middle."""
    s = max(w / img.width, h / img.height)
    img = img.resize((int(img.width * s + 0.5), int(img.height * s + 0.5)), Image.LANCZOS)
    return crop_center(img, w, h)


def paste(canvas, overlay, x, y):
    canvas.alpha_composite(overlay, (int(x), int(y)))


# ------------------------------------------------------------------------------------------------ images

def banner():
    w, h = 1920, 640
    img = background(w, h)
    draw_text(img, "FEMBOY MOD", 70, 90, 17, WHITE, shadow=PLUM, outline=PLUM)
    draw_text(img, "SOCKS, EARS & BACKPACKS", 76, 250, 6, PLUM, shadow=(255, 255, 255))
    x, y = 76, 350
    for label in ("37 COSMETICS", "19 COLORWAYS", "GAMER ROOM", "FRIENDS", "NEW ORES", "PUBLIC API"):
        c = chip(label, 3)
        if x + c.width > 1080:
            x, y = 76, y + c.height + 14
        paste(img, c, x, y)
        x += c.width + 14
    x, y = 76, y + 80
    for label, fill in (("MC 26.3", (230, 247, 236)), ("FABRIC", (241, 236, 255)), ("NEOFORGE", (255, 240, 225))):
        c = chip(label, 3, fill=fill, border=PLUM)
        paste(img, c, x, y)
        x += c.width + 14
    hero = crop_center(shot("docs_hero"), 840, 760, cx=0.5, cy=0.52)
    frame = framed(hero.resize((520, 470), Image.LANCZOS), border=WHITE, width=9)
    paste(img, frame, w - frame.width - 70, (h - frame.height) // 2)
    frame_x = w - frame.width - 70
    for i, (name, tint) in enumerate((("cat_ear_headphones", (247, 168, 204)), ("tail", (242, 145, 190)),
                                       ("pride_badge", None), ("bubble_tea", None))):
        paste(img, icon(name, 3, tint), frame_x - 110, 110 + i * 110)
    return img.convert("RGB")


def header(title, icon_name, tint=None):
    w, h = 1600, 132
    img = background(w, h)
    d = ImageDraw.Draw(img)
    d.rectangle([0, h - 8, w, h], fill=ACCENT)
    ic = icon(icon_name, 3, tint)
    paste(img, ic, 40, (h - 8 - ic.height) // 2)
    draw_text(img, title, 40 + ic.width + 30, (h - 8 - 7 * 8) // 2, 8, WHITE, shadow=PLUM, outline=PLUM)
    return img.convert("RGB")


def gallery(title, cards, cols, card_w, card_h, width=1920):
    """cards: [(image, caption, subcaption, border color)]. Title on top, framed cards with captions below."""
    rows = math.ceil(len(cards) / cols)
    cell_w = card_w + 90
    cell_h = card_h + 190
    h = 170 + rows * cell_h + 20
    img = background(width, h)
    draw_text(img, title, (width - text_width(title, 9)) // 2, 50, 9, WHITE, shadow=PLUM, outline=PLUM)
    left = (width - cols * cell_w) // 2
    for i, (pic, caption, sub, color) in enumerate(cards):
        col, row = i % cols, i // cols
        x = left + col * cell_w
        y = 170 + row * cell_h
        frame = framed(fit(pic, card_w, card_h), border=color, width=8)
        paste(img, frame, x + (cell_w - frame.width) // 2, y)
        cy = y + frame.height + 4
        draw_text(img, caption, x + (cell_w - text_width(caption, 5)) // 2, cy, 5, color, shadow=PLUM, outline=PLUM)
        if sub:
            draw_text(img, sub, x + (cell_w - text_width(sub, 3)) // 2, cy + 50, 3, PLUM, shadow=None)
    return img.convert("RGB")


def upscale_ui(img, box, factor=2):
    return img.crop(box).resize(((box[2] - box[0]) * factor, (box[3] - box[1]) * factor), Image.NEAREST)


def selfie():
    files = sorted(glob.glob(os.path.join(SHOTS, "femboymod_selfie_*.png")))
    return Image.open(files[-1]).convert("RGB") if files else None


# Only icons drawn by our own tools (no AI drafts) go on the pages; see ASSET_PROVENANCE.md.
HEADERS = {
    "header_cosmetics": ("COSMETICS & OUTFITS", "cat_ear_headphones", (247, 168, 204)),
    "header_drip": ("DRIP, SETS & STYLE", "crop_sweater", (245, 169, 184)),
    "header_backpacks": ("BACKPACKS & CHARMS", "pride_badge", None),
    "header_food": ("BYTE ENERGY & SNACKS", "bubble_tea", None),
    "header_gamer_room": ("GAMER ROOM", "led_strip", None),
    "header_friends": ("FRIENDS", "stray_cat_spawn_egg", None),
    "header_ores": ("ORES & GEMS", "rose_quartz", None),
    "header_mobs": ("MEME MOBS", "dark_shades", (42, 42, 51)),
    "header_server": ("SERVERS & ADDONS", "style_coupon", None),
    "header_install": ("INSTALLATION", "moonstone", None),
}


def build():
    os.makedirs(OUT, exist_ok=True)
    banner().save(os.path.join(OUT, "banner.png"))
    for name, (title, ic, tint) in HEADERS.items():
        header(title, ic, tint).save(os.path.join(OUT, name + ".png"))

    def person(name):
        return crop_center(shot(name), 620, 760, cx=0.5, cy=0.5)

    gallery("MIX & MATCH", [
        (person("docs_outfit_edgy"), "EDGY", "SHADES, CHAINS, FISHNETS", (58, 42, 56)),
        (person("docs_outfit_soft"), "SOFT", "SAKURA HOODIE, PEARL", (247, 184, 207)),
        (person("docs_outfit_cyber"), "CYBER", "NEON VISOR, FOX TAIL", (124, 247, 255)),
        (person("docs_outfit_pride"), "PRIDE", "RAINBOW, GLITTER, QUARTZ", (181, 140, 255)),
    ], 4, 380, 460).save(os.path.join(OUT, "outfits.png"))

    outfit_screen = shot("docs_outfit_screen")
    ui = upscale_ui(outfit_screen, (0, 0, 435, 480), 2)
    cards = [(ui, "OUTFIT SCREEN", "10 SLOTS, PRESETS, HUD", ACCENT)]
    pic = selfie()
    if pic:
        cards.append((pic, "PHOTO MODE", "INSTANT-PRINT SELFIES", WHITE))
    cards.append((crop_center(shot("docs_back"), 900, 760), "BACK VIEW", "BACKPACK CHARMS, FOX TAIL", (244, 166, 200)))
    gallery("DRESS UP", cards, 3, 470, 520).save(os.path.join(OUT, "ui.png"))

    gallery("GAMER ROOM", [
        (crop_center(shot("docs_gamer_room_wide"), 1700, 956, cy=0.5), "BATTLESTATION", "CHAIR, PC, TERMINAL, LED, POSTERS, RUBBER DUCK",
         (181, 140, 255)),
    ], 1, 1300, 731).save(os.path.join(OUT, "gamer_room.png"))

    gallery("THE WORLD", [
        (crop_center(shot("docs_ores"), 1500, 900, cy=0.45), "ORES & GEODES", "ROSE QUARTZ, GLITTER, MOONSTONE, NEON", (124, 247, 255)),
        (crop_center(shot("docs_mobs"), 1400, 800, cy=0.62), "MEME MOBS", "FASHION CRITIC, BUGS, HISSY CAT", (255, 124, 124)),
    ], 2, 800, 450).save(os.path.join(OUT, "world.png"))

    gallery("FRIENDS & EMOTES", [
        (crop_center(shot("docs_friends"), 1500, 900, cy=0.58), "FRIENDS", "STRAY CATS, COSPLAYER, VIBE CHECK", (255, 182, 218)),
        (person("docs_emote"), "HEART HANDS", "PRESS G FOR EMOTES", ACCENT),
    ], 2, 700, 420).save(os.path.join(OUT, "friends.png"))
    print("wrote", sorted(os.listdir(OUT)))


def curseforge_page(base):
    """README with absolute image URLs, for pasting into CurseForge / Modrinth (they can't resolve relative paths)."""
    with open(os.path.join(ROOT, "README.md"), encoding="utf-8") as f:
        text = f.read()
    text = re.sub(r"\]\((docs/images/[^)]+)\)", lambda m: "](" + base.rstrip("/") + "/" + m.group(1) + ")", text)
    text = re.sub(r'src="(docs/images/[^"]+)"', lambda m: 'src="' + base.rstrip("/") + "/" + m.group(1) + '"', text)
    out = os.path.join(ROOT, "docs", "pages", "curseforge.md")
    with open(out, "w", encoding="utf-8") as f:
        f.write(text)
    print("wrote", out)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--page-base", help="raw URL of the repo root, e.g. https://raw.githubusercontent.com/<user>/<repo>/<branch>")
    args = parser.parse_args()
    build()
    if args.page_base:
        curseforge_page(args.page_base)
