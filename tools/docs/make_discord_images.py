#!/usr/bin/env python3
"""Discord server art: invite background (1920x1080), server banner (960x540) and 128x128 emojis.

    python3 tools/docs/make_discord_images.py      # writes build/discord/

Uses the README image helpers (pixel font, gradient, item icons) and in-game screenshots from build/gallery
(the docs screenshot run). No image AI.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
import make_readme_images as art  # noqa: E402

ROOT = art.ROOT
OUT = os.path.join(ROOT, "build", "discord")
GALLERY = os.path.join(ROOT, "build", "gallery")
MOD_ICON = os.path.join(ROOT, "art", "icon", "icon_1024.png")
# Largest side of an emoji's pixel art after scaling, inside the 128 px canvas
EMOJI_FILL = 124

# Emoji name -> (item texture, tint for grey dyeable icons)
EMOJIS = {
    "socks": ("programming_socks", (255, 182, 217)),
    "catears": ("cat_ears", (255, 255, 255)),
    "tail": ("tail", (242, 145, 190)),
    "headphones": ("cat_ear_headphones", (247, 168, 204)),
    "choker": ("uwu_choker", (255, 182, 217)),
    "heartglasses": ("heart_glasses", (242, 145, 190)),
    "bubbletea": ("bubble_tea", None),
    "byteenergy": ("byte_energy_pink", None),
    "strawberrymilk": ("strawberry_milk", None),
    "sharkplush": ("shark_plush_charm", None),
    "pawcharm": ("cat_paw_charm", None),
    "heartpin": ("heart_pin", None),
    "rosequartz": ("rose_quartz", None),
    "moonstone": ("moonstone", None),
    "pride": ("pride_badge", None),
    "coupon": ("style_coupon", None),
}


def left_fade(img, width_frac=0.62, strength=225):
    """Darkens-to-pastel the left part so text stays readable over a screenshot."""
    w, h = img.size
    overlay = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    edge = int(w * width_frac)
    for x in range(edge):
        a = int(strength * (1 - x / edge) ** 1.4)
        draw.line([(x, 0), (x, h)], fill=(*art.PLUM, a))
    out = img.convert("RGBA")
    out.alpha_composite(overlay)
    return out


def invite_background():
    shot = Image.open(os.path.join(GALLERY, "01_hero.png")).convert("RGB").resize((1920, 1080), Image.LANCZOS)
    # A light blur keeps the scene recognisable behind Discord's invite card
    img = left_fade(shot.filter(ImageFilter.GaussianBlur(2)))
    icon = Image.open(MOD_ICON).convert("RGBA").resize((220, 220), Image.LANCZOS)
    art.paste(img, art.framed(icon, border=art.WHITE, width=8, radius=40), 110, 150)
    art.draw_text(img, "FEMBOY MOD", 110, 440, 11, art.WHITE, shadow=art.PLUM, outline=art.PLUM)
    art.draw_text(img, "SOCKS, EARS & BACKPACKS", 116, 560, 4, art.WHITE, shadow=art.PLUM)
    x, y = 116, 650
    for label in ("37 COSMETICS", "GAMER ROOM", "FRIENDS", "PUBLIC API"):
        chip = art.chip(label, 3)
        art.paste(img, chip, x, y)
        x += chip.width + 14
    x, y = 116, 740
    for label, fill in (("26.3", (230, 247, 236)), ("1.21.1", (241, 236, 255)), ("1.20.1", (255, 240, 225))):
        chip = art.chip("MC " + label, 3, fill=fill, border=art.PLUM)
        art.paste(img, chip, x, y)
        x += chip.width + 14
    return img.convert("RGB")


def server_banner():
    w, h = 960, 540
    img = art.background(w, h)
    icon = Image.open(MOD_ICON).convert("RGBA").resize((200, 200), Image.LANCZOS)
    art.paste(img, art.framed(icon, border=art.WHITE, width=7, radius=36), (w - 214) // 2, 60)
    title = "FEMBOY MOD"
    scale = 9
    art.draw_text(img, title, (w - art.text_width(title, scale)) // 2, 320, scale, art.WHITE, shadow=art.PLUM, outline=art.PLUM)
    sub = "COZY FASHION FOR MINECRAFT"
    art.draw_text(img, sub, (w - art.text_width(sub, 3)) // 2, 420, 3, art.PLUM, shadow=art.WHITE)
    for i, (name, tint) in enumerate((("programming_socks", (255, 182, 217)), ("cat_ears", None),
                                       ("tail", (242, 145, 190)), ("bubble_tea", None))):
        x = 70 if i < 2 else w - 70 - 96
        y = 110 + (i % 2) * 150
        art.paste(img, art.icon(name, 3, tint), x, y)
    return img.convert("RGB")


def emoji(name, tint):
    """128x128, transparent, nearest-neighbour: pixel art stays crisp at Discord's small emoji size."""
    # Crop to the drawn pixels and scale by the largest whole factor: small icons fill the emoji, pixels stay sharp
    base = art.icon(name, 1, tint)
    base = base.crop(base.getbbox())
    factor = max(1, EMOJI_FILL // max(base.size))
    icon = base.resize((base.width * factor, base.height * factor), Image.NEAREST)
    canvas = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    canvas.alpha_composite(icon, ((128 - icon.width) // 2, (128 - icon.height) // 2))
    return canvas


def main():
    os.makedirs(os.path.join(OUT, "emojis"), exist_ok=True)
    invite_background().save(os.path.join(OUT, "invite_background_1920x1080.png"))
    server_banner().save(os.path.join(OUT, "server_banner_960x540.png"))
    for key, (tex, tint) in EMOJIS.items():
        emoji(tex, tint).save(os.path.join(OUT, "emojis", key + ".png"))
    Image.open(MOD_ICON).convert("RGBA").resize((128, 128), Image.LANCZOS).save(os.path.join(OUT, "emojis", "femboymod.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    main()
