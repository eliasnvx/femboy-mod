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


def hissy_cat_eyes():
    """Emissive eyes for the vanilla adult cat UV (head front face: x 5..9, y 5..8)."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    glow, pupil = hexc("#E8FF4A"), hexc("#FF7A2E")
    for x, y in ((5, 6), (9, 6)):
        img.putpixel((x, y), glow)
    for x, y in ((6, 6), (8, 6)):     # inner half of each eye: slit pupil
        img.putpixel((x, y), pupil)
    return img


def fashion_critic():
    """Player skin layout (64x64): head 0,0; hat 32,0; body 16,16; right arm 40,16; left arm 32,48;
    right leg 0,16; left leg 16,48. Overlay layers (jacket, sleeves, pants) stay transparent."""
    rng = random.Random(11)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin, hair, beret = hexc("#EBC8AE"), hexc("#2B2230"), hexc("#7A1F45")
    black, collar, gold = hexc("#1E1B22"), hexc("#2C2833"), hexc("#E8C24A")
    trousers, shoes, lips, lens = hexc("#4A4A58"), hexc("#141216"), hexc("#B8506A"), hexc("#101014")
    soft = (0.97, 1.0, 1.0, 1.03)

    box(img, 0, 0, 8, 8, 8, skin, rng, soft)
    f = faces(0, 0, 8, 8, 8)
    fill(img, f["up"], hair, rng, soft)
    fill(img, f["south"], hair, rng, soft)
    for name in ("west", "east"):
        x, y, w, h = f[name]
        fill(img, (x, y, w, 4), hair, rng, soft)             # hair down to the ears
    x, y, w, h = f["north"]                                   # face: fringe, sunglasses, smirk
    fill(img, (x, y, w, 2), hair, rng, soft)
    fill(img, (x + 1, y + 3, 3, 2), lens)
    fill(img, (x + 4, y + 3, 3, 2), lens)
    img.putpixel((x + 1, y + 3), hexc("#6A6A80"))            # lens shine
    img.putpixel((x + 4, y + 3), hexc("#6A6A80"))
    fill(img, (x + 3, y + 6, 3, 1), lips)
    img.putpixel((x + 6, y + 5), lips)                        # smirk

    hat = faces(32, 0, 8, 8, 8)                               # beret on the hat layer, tilted to the right
    fill(img, hat["up"], beret, rng, soft)
    for name in ("north", "south", "west", "east"):
        x, y, w, h = hat[name]
        fill(img, (x, y, w, 2), beret, rng, soft)
    x, y, w, h = hat["east"]
    fill(img, (x, y + 2, w, 1), beret, rng, soft)             # droops on one side
    hx, hy, _, _ = hat["up"]
    img.putpixel((hx + 4, hy + 3), shade(beret, 0.7))         # stem

    box(img, 16, 16, 8, 12, 4, black, rng, soft)              # turtleneck with a gold chain
    x, y, w, h = faces(16, 16, 8, 12, 4)["north"]
    fill(img, (x, y, w, 2), collar, rng, soft)
    for i, (cx, cy) in enumerate(((1, 2), (2, 3), (3, 4), (4, 4), (5, 3), (6, 2))):
        img.putpixel((x + cx, y + cy), gold)
    img.putpixel((x + 3, y + 5), gold)                         # pendant
    for (u, v) in ((40, 16), (32, 48)):                       # arms: sleeves + hands
        box(img, u, v, 4, 12, 4, black, rng, soft)
        for name, (fx, fy, fw, fh) in faces(u, v, 4, 12, 4).items():
            if name in ("north", "south", "west", "east"):
                fill(img, (fx, fy + 9, fw, 3), skin, rng, soft)
        fill(img, faces(u, v, 4, 12, 4)["down"], skin, rng, soft)
    for (u, v) in ((0, 16), (16, 48)):                        # trousers + shoes
        box(img, u, v, 4, 12, 4, trousers, rng, soft)
        for name, (fx, fy, fw, fh) in faces(u, v, 4, 12, 4).items():
            if name in ("north", "south", "west", "east"):
                fill(img, (fx, fy + 10, fw, 2), shoes, rng, soft)
        fill(img, faces(u, v, 4, 12, 4)["down"], shoes, rng, soft)
    return img


def caffeinated_zombie():
    """Vanilla zombie model UV (64x64; left arm/leg mirror the right ones): lime skin, pink hair,
    huge sleepless eyes with bags, pink sweatband, black tank top with a lightning bolt, wristbands,
    denim shorts, neon sneakers. Original design, nothing taken from the vanilla texture."""
    rng = random.Random(21)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin, spot = hexc("#9CCB5E"), hexc("#7FAE48")
    hair, band = hexc("#FF4FA8"), hexc("#FF77BE")
    white, pupil, bag = hexc("#F4F4EE"), hexc("#1FB8D8"), hexc("#5E7F3A")
    top, bolt = hexc("#232028"), hexc("#FFE14A")
    shorts, sneaker, sole = hexc("#3F5FA8"), hexc("#45E0D0"), hexc("#F4F4EE")
    soft = (0.94, 1.0, 1.0, 1.05)

    def mottle(rect, color, n):
        x0, y0, w, h = rect
        for _ in range(n):
            img.putpixel((x0 + rng.randrange(w), y0 + rng.randrange(h)), color)

    head = faces(0, 0, 8, 8, 8)
    box(img, 0, 0, 8, 8, 8, skin, rng, soft)
    for name in ("north", "south", "west", "east"):
        mottle(head[name], spot, 4)
    fill(img, head["up"], hair, rng, soft)
    for name in ("south", "west", "east"):
        x, y, w, h = head[name]
        fill(img, (x, y, w, 3 if name != "south" else 5), hair, rng, soft)   # messy dyed hair
    x, y, w, h = head["north"]
    fill(img, (x, y, w, 1), hair, rng, soft)
    img.putpixel((x + 2, y + 1), hair)
    img.putpixel((x + 5, y + 1), hair)
    for ex in (x + 1, x + 5):                                                 # wide-open eyes, pupils tiny
        fill(img, (ex, y + 3, 2, 2), white)
        img.putpixel((ex + (1 if ex == x + 1 else 0), y + 4), pupil)
        fill(img, (ex, y + 5, 2, 1), bag)                                     # eye bags
    fill(img, (x + 2, y + 6, 4, 1), hexc("#3A2A30"))                          # jittery grin
    img.putpixel((x + 3, y + 6), white)
    img.putpixel((x + 4, y + 6), white)

    hat = faces(32, 0, 8, 8, 8)                                               # sweatband on the hat layer
    for name in ("north", "south", "west", "east"):
        hx, hy, hw, hh = hat[name]
        fill(img, (hx, hy + 2, hw, 1), band, rng, soft)
    hx, hy, _, _ = hat["north"]
    img.putpixel((hx + 3, hy + 2), white)

    box(img, 16, 16, 8, 12, 4, top, rng, soft)                                # tank top with a bolt
    body = faces(16, 16, 8, 12, 4)
    x, y, w, h = body["north"]
    for bx, by in ((5, 1), (4, 2), (3, 3), (4, 3), (5, 3), (4, 4), (3, 5), (2, 6)):
        img.putpixel((x + bx, y + by + 1), bolt)
    fill(img, (x, y + 9, w, 3), shorts, rng, soft)                           # shorts waistband
    for name in ("south", "west", "east"):
        bx, by, bw, bh = body[name]
        fill(img, (bx, by + 9, bw, 3), shorts, rng, soft)
    fill(img, (x, y, w, 1), skin, rng, soft)                                  # neck above the collar

    box(img, 40, 16, 4, 12, 4, skin, rng, soft)                               # bare arms + wristband
    for name, (fx, fy, fw, fh) in faces(40, 16, 4, 12, 4).items():
        if name in ("north", "south", "west", "east"):
            fill(img, (fx, fy, fw, 3), top, rng, soft)
            fill(img, (fx, fy + 8, fw, 2), band, rng, soft)
            mottle((fx, fy + 3, fw, 5), spot, 2)

    box(img, 0, 16, 4, 12, 4, skin, rng, soft)                                # shorts, bare shins, neon sneakers
    for name, (fx, fy, fw, fh) in faces(0, 16, 4, 12, 4).items():
        if name in ("north", "south", "west", "east"):
            fill(img, (fx, fy, fw, 5), shorts, rng, soft)
            mottle((fx, fy + 5, fw, 4), spot, 2)
            fill(img, (fx, fy + 9, fw, 2), sneaker, rng, soft)
            fill(img, (fx, fy + 11, fw, 1), sole)
    fill(img, faces(0, 16, 4, 12, 4)["down"], sole)
    return img


def caffeinated_zombie_eyes():
    """Emissive pupils for the zombie head front face (x 8..15, y 8..15)."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for x, y in ((10, 12), (13, 12)):
        img.putpixel((x, y), hexc("#4FF0FF"))
    return img


TEXTURES = {"bug.png": bug, "caffeinated_zombie.png": caffeinated_zombie, "caffeinated_zombie_eyes.png": caffeinated_zombie_eyes, "hissy_cat_eyes.png": hissy_cat_eyes, "fashion_critic.png": fashion_critic}


def main():
    os.makedirs(ROOT, exist_ok=True)
    for name, make in TEXTURES.items():
        make().save(os.path.join(ROOT, name))
        print("wrote", name)


if __name__ == "__main__":
    main()
