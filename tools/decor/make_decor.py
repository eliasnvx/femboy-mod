#!/usr/bin/env python3
"""Ores, gamer corner furniture and posters: block models, blockstates, item definitions and textures.
PLACEHOLDER-quality procedural pixel art, no AI.

    python3 tools/decor/make_decor.py

Ores draw only the crystals on a transparent overlay; the stone underneath is the vanilla texture referenced
by the model (not copied). Furniture reuses the plush atlas packer (tools/plush). Animated textures (monitor
code, rainbow LED) scroll or blend slowly: no flashing (SPEC §1.1).
"""
import colorsys
import json
import os
import random
import sys

from PIL import Image

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "plush"))
from make_plushies import Box, hexc, model_json, pack, put, rect, shade  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod")


def path(*parts):
    p = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def write_json(obj, *parts):
    with open(path(*parts), "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def save(img, *parts):
    img.save(path(*parts))


def mcmeta(frametime, interpolate, *parts):
    write_json({"animation": {"frametime": frametime, "interpolate": interpolate}}, *parts)


def item_def(name, model):
    write_json({"model": {"type": "minecraft:model", "model": model}}, "items", name + ".json")


def horizontal_states(name, model_for_state=None, extra=""):
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        states = [("", f"femboymod:block/{name}")] if model_for_state is None else model_for_state
        for suffix, model in states:
            key = f"facing={facing}" + suffix
            entry = {"model": model}
            if y:
                entry["y"] = y
            variants[key] = entry
    write_json({"variants": variants}, "blockstates", name + ".json")


# ------------------------------------------------------------------------------------------------ ores

ROSE = [hexc(c) for c in ("#F9D3E0", "#F4AFC8", "#E58AAE", "#C9678F")]
GLITTER = [hexc(c) for c in ("#FFFFFF", "#FFD1EC", "#E9C6FF", "#C9E6FF", "#FFE7B8")]


def crystal_overlay(seed, clusters, palette, size=16):
    """Small angular crystal clusters: light top-left, dark bottom-right, like vanilla ore gems."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for cx, cy in clusters:
        shape = [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (0, -1)]
        shape += rng.sample([(2, 1), (1, 2), (-1, 1), (1, -1), (2, 0), (0, 2)], 2)
        for dx, dy in shape:
            x, y = cx + dx, cy + dy
            if 0 <= x < size and 0 <= y < size:
                tone = 0 if dx + dy < 0 else 1 if dx + dy == 0 else 2 if dx + dy <= 2 else 3
                px[x, y] = palette[tone]
        px[cx + 1, cy + 1] = palette[3]
        px[cx, cy] = palette[0]
    return img


def sparkle_overlay(seed, count, palette, size=16):
    """Scattered single-pixel and plus-shaped glitter specks in pastel colors."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    px = img.load()
    for i in range(count):
        x, y = rng.randrange(1, size - 1), rng.randrange(1, size - 1)
        color = palette[1 + i % (len(palette) - 1)]
        px[x, y] = palette[0]
        if i % 3 == 0:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                px[x + dx, y + dy] = color
        else:
            px[x + 1, y] = color
    return img


def overlay_model(base, overlay):
    faces = {f: {"uv": [0, 0, 16, 16], "texture": t, "cullface": f} for f, t in
             ((f, None) for f in ("down", "up", "north", "south", "west", "east"))}
    base_faces = {f: dict(v, texture="#base") for f, v in faces.items()}
    over_faces = {f: dict(v, texture="#overlay") for f, v in faces.items()}
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": base, "base": base, "overlay": overlay},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": base_faces},
            {"from": [0, 0, 0], "to": [16, 16, 16], "faces": over_faces},
        ],
    }


def simple_block(name):
    write_json({"variants": {"": {"model": f"femboymod:block/{name}"}}}, "blockstates", name + ".json")
    item_def(name, f"femboymod:block/{name}")


def ores():
    clusters = [(3, 3), (10, 2), (6, 8), (12, 10), (2, 12)]
    save(crystal_overlay(1, clusters, ROSE), "textures", "block", "rose_quartz_ore_overlay.png")
    save(crystal_overlay(2, [(4, 2), (11, 5), (3, 10), (9, 11)], ROSE), "textures", "block", "deepslate_rose_quartz_ore_overlay.png")
    save(sparkle_overlay(3, 14, GLITTER), "textures", "block", "glitter_ore_overlay.png")
    for name, base in (("rose_quartz_ore", "minecraft:block/stone"),
                       ("deepslate_rose_quartz_ore", "minecraft:block/deepslate"),
                       ("glitter_ore", "minecraft:block/stone")):
        write_json(overlay_model(base, f"femboymod:block/{name}_overlay"), "models", "block", name + ".json")
        simple_block(name)

    # Rose quartz block: cloudy pink with faceted light streaks
    rng = random.Random(7)
    img = Image.new("RGBA", (16, 16), ROSE[1])
    px = img.load()
    for y in range(16):
        for x in range(16):
            n = rng.choice((0.96, 1.0, 1.0, 1.03))
            px[x, y] = shade(ROSE[1], n)
    for x0, y0, length in ((2, 12, 7), (8, 9, 6), (1, 5, 5), (9, 3, 5)):
        for i in range(length):
            put(img, [(x0 + i, y0 - i)], ROSE[0])
            put(img, [(x0 + i + 1, y0 - i)], ROSE[2])
    put(img, [(x, 0) for x in range(16)] + [(0, y) for y in range(16)], shade(ROSE[0], 1.0))
    put(img, [(x, 15) for x in range(16)] + [(15, y) for y in range(16)], ROSE[3])
    save(img, "textures", "block", "rose_quartz_block.png")
    write_json({"parent": "minecraft:block/cube_all", "textures": {"all": "femboymod:block/rose_quartz_block"}},
               "models", "block", "rose_quartz_block.json")
    simple_block("rose_quartz_block")


# ------------------------------------------------------------------------------------------------ item icons (32x32)

def outline(img, color):
    src = img.copy().load()
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            if src[x, y][3] == 0 and any(
                    0 <= x + dx < img.width and 0 <= y + dy < img.height and src[x + dx, y + dy][3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                px[x, y] = color


def gem(img, cx, cy, r):
    """Hexagonal rose quartz crystal with facets."""
    px = img.load()
    for y in range(-r * 2, r * 2 + 1):
        for x in range(-r, r + 1):
            if abs(x) + abs(y) / 2 <= r:
                tone = 0 if x < 0 and y < 0 else 1 if x < 0 else 2 if y < 0 else 3
                if x == 0:
                    tone = max(0, tone - 1)
                px[cx + x, cy + y] = ROSE[tone]
    px[cx - r // 2, cy - r] = hexc("#FFFFFF")


def icons():
    ink = hexc("#5A2E45")
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    gem(img, 16, 16, 7)
    gem(img, 9, 22, 3)
    outline(img, ink)
    save(img, "textures", "item", "rose_quartz.png")

    gold, gold_dark = hexc("#F2C94C"), hexc("#B8892A")
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for cx in (10, 22):
        put(img, rect(cx - 1, 4, 3, 3), gold)
        put(img, [(cx, 7), (cx, 8), (cx, 9)], gold_dark)
        gem(img, cx, 16, 4)
    outline(img, ink)
    save(img, "textures", "item", "rose_quartz_earrings.png")

    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = img.load()
    for y in range(32):
        for x in range(32):
            d = ((x - 16) ** 2) / 121 + ((y - 17) ** 2) / 64
            if 0.62 <= d <= 1.0:
                px[x, y] = ROSE[1] if y < 17 else ROSE[2]
            elif 0.5 <= d < 0.62:
                px[x, y] = ROSE[3]
    for bx, by in ((5, 17), (27, 17), (16, 9), (16, 25), (9, 11), (23, 11), (9, 23), (23, 23)):
        put(img, [(bx, by)], ROSE[0])
    put(img, rect(14, 22, 5, 5), gold)
    gem(img, 16, 24, 2)
    outline(img, ink)
    save(img, "textures", "item", "rose_quartz_bracelet.png")
    # Style Coupon: a pink ticket with a notched edge and a flower stamp
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    put(img, rect(3, 9, 26, 14), hexc("#FFC2DF"))
    put(img, rect(3, 9, 26, 2), hexc("#FFE7F2"))
    for y in range(11, 23, 3):
        put(img, [(3, y), (28, y)], (0, 0, 0, 0))
    put(img, [(20, y) for y in range(10, 23, 2)], hexc("#E0789E"))
    flower = hexc("#F291BE")
    put(img, [(11, 13), (10, 14), (12, 14), (11, 15), (9, 16), (13, 16), (11, 17), (10, 18), (12, 18), (11, 19)], flower)
    put(img, [(11, 16)], hexc("#FFE7A0"))
    put(img, rect(23, 13, 3, 1) + rect(23, 16, 3, 1) + rect(23, 19, 2, 1), hexc("#B58CFF"))
    outline(img, ink)
    save(img, "textures", "item", "style_coupon.png")
    for name in ("rose_quartz", "rose_quartz_earrings", "rose_quartz_bracelet", "style_coupon"):
        write_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"femboymod:item/{name}"}},
                   "models", "item", name + ".json")
        item_def(name, f"femboymod:item/{name}")


# ------------------------------------------------------------------------------------------------ furniture

BLACK, GREY, PINK, PINK_LIGHT, WHITE = (hexc(c) for c in ("#2B2530", "#57505E", "#F291BE", "#FFC2DF", "#F4EEF1"))


def furniture_model(name, boxes, rng, extra_elements=(), extra_textures=None, scale=0.625, translation=(0, 0, 0)):
    atlas, placed = pack(boxes, rng)
    size = atlas.size[0]
    save(atlas, "textures", "block", name + ".png")
    model = model_json(name, boxes, placed, size, scale, list(translation))
    model["elements"].extend(extra_elements)
    if extra_textures:
        model["textures"].update(extra_textures)
    return model


def chair(rng):
    def heart(img, w, h):
        cx, cy = w // 2, h // 3
        put(img, [(cx - 2, cy), (cx - 1, cy), (cx + 1, cy), (cx + 2, cy)], PINK)
        put(img, rect(cx - 3, cy + 1, 7, 2), PINK)
        put(img, rect(cx - 2, cy + 3, 5, 1), PINK)
        put(img, rect(cx - 1, cy + 4, 3, 1), PINK)
        put(img, [(cx, cy + 5)], PINK)
        put(img, [(cx - 2, cy + 1)], PINK_LIGHT)

    def stripe_sides(img, w, h):
        put(img, rect(0, 0, 2, h) + rect(w - 2, 0, 2, h), PINK)

    def cushion(img, w, h):
        put(img, rect(0, 0, w, 2) + rect(0, h - 2, w, 2), PINK)

    boxes = [
        Box("foot_x", (2, 0, 7), (14, 1, 9), GREY, seam=False),
        Box("foot_z", (7, 0, 2), (9, 1, 14), GREY, seam=False),
        Box("pole", (7, 1, 7), (9, 6, 9), GREY, seam=False),
        Box("seat", (2, 6, 2), (14, 9, 14), BLACK, {"up": cushion}),
        Box("back", (2, 9, 12), (14, 22, 15), BLACK, {"north": heart, "south": stripe_sides}),
        Box("headrest", (4, 22, 12.5), (12, 24, 14.5), PINK),
        Box("arm_left", (1, 9, 4), (2.5, 12, 12), BLACK, {"up": cushion}),
        Box("arm_right", (13.5, 9, 4), (15, 12, 12), BLACK, {"up": cushion}),
    ]
    write_json(furniture_model("gamer_chair", boxes, rng, scale=0.5, translation=(0, -2, 0)),
               "models", "block", "gamer_chair.json")
    horizontal_states("gamer_chair")
    item_def("gamer_chair", "femboymod:block/gamer_chair")


CODE_BG = hexc("#1E1A2A")
CODE_COLORS = [hexc(c) for c in ("#FF9FD0", "#9FE3FF", "#F4EEF1", "#C9A3F0", "#FFE7A0")]
SCREEN_W, SCREEN_H = 13, 9


def code_frames():
    """Lines of 'code' (colored dashes with indentation) scrolling up one line per frame; loops seamlessly."""
    rng = random.Random(11)
    lines = []
    indent = 0
    for i in range(16):
        indent = max(0, min(4, indent + rng.choice((-2, 0, 0, 2))))
        tokens, x = [], 1 + indent
        while x < SCREEN_W - 2:
            length = rng.randint(1, 4)
            tokens.append((x, min(length, SCREEN_W - 1 - x), rng.choice(CODE_COLORS)))
            x += length + 1
            if rng.random() < 0.3:
                break
        lines.append(tokens)
    frames = Image.new("RGBA", (16, 16 * len(lines)), CODE_BG)
    for f in range(len(lines)):
        frame = Image.new("RGBA", (16, 16), CODE_BG)
        for row in range(SCREEN_H // 2 + 1):
            y = row * 2
            if y >= SCREEN_H:
                break
            for x, length, color in lines[(f + row) % len(lines)]:
                put(frame, rect(x, y, length, 1), color)
        frames.paste(frame, (0, 16 * f))
    return frames


def monitor(rng):
    def bezel_logo(img, w, h):
        put(img, [(w // 2, h - 1)], PINK)

    boxes = [
        Box("base", (5, 0, 7), (11, 1, 11), WHITE, seam=False),
        Box("neck", (7, 1, 8.5), (9, 4, 10), WHITE, seam=False),
        Box("screen", (1, 4, 7), (15, 14, 9), PINK, {"south": bezel_logo}),
    ]
    save(code_frames(), "textures", "block", "gamer_monitor_screen.png")
    mcmeta(6, False, "textures", "block", "gamer_monitor_screen.png.mcmeta")
    off = Image.new("RGBA", (16, 16), hexc("#141218"))
    put(off, [(2, 1), (3, 1), (2, 2)], hexc("#2A2533"))  # faint reflection
    save(off, "textures", "block", "gamer_monitor_screen_off.png")

    def screen(texture, emission):
        element = {"from": [1.5, 4.5, 6.9], "to": [14.5, 13.5, 7],
                   "faces": {"north": {"uv": [0, 0, SCREEN_W, SCREEN_H], "texture": "#screen"}}}
        if emission:
            element["light_emission"] = 15
        return element

    for suffix, texture, lit in (("", "gamer_monitor_screen", True), ("_off", "gamer_monitor_screen_off", False)):
        model = furniture_model("gamer_monitor", boxes, random.Random(5), [screen(texture, lit)],
                                {"screen": f"femboymod:block/{texture}"})
        write_json(model, "models", "block", f"gamer_monitor{suffix}.json")
    horizontal_states("gamer_monitor", [(",lit=true", "femboymod:block/gamer_monitor"),
                                        (",lit=false", "femboymod:block/gamer_monitor_off")])
    item_def("gamer_monitor", "femboymod:block/gamer_monitor")


def keyboard(rng):
    def keys(img, w, h):
        for y in range(1, h - 1, 2):
            for x in range(1, w - 1, 2):
                put(img, [(x, y)], WHITE)
        put(img, rect(5, h - 2, w - 10, 1), PINK_LIGHT)  # space bar

    def mouse_top(img, w, h):
        put(img, [(w // 2, 0), (w // 2, 1)], BLACK)

    boxes = [
        Box("board", (1, 0, 6), (13, 1.5, 11), PINK, {"up": keys}, seam=False),
        Box("mouse", (13.5, 0, 7), (15, 1, 9.5), WHITE, {"up": mouse_top}, seam=False),
    ]
    write_json(furniture_model("gamer_keyboard", boxes, rng, scale=0.8, translation=(0, 3, 0)),
               "models", "block", "gamer_keyboard.json")
    horizontal_states("gamer_keyboard")
    item_def("gamer_keyboard", "femboymod:block/gamer_keyboard")


def terminal(rng):
    cream, shade_c = hexc("#EDE3D6"), hexc("#CFC2B2")

    def screen(img, w, h):  # front: dark screen with a pink prompt, vents below
        put(img, rect(2, 2, w - 4, h - 7), hexc("#1E1A2A"))
        put(img, [(4, 4), (5, 5), (4, 6)], hexc("#FF9FD0"))
        put(img, rect(7, 6, 3, 1), hexc("#FF9FD0"))
        put(img, rect(4, 9, 8, 1), hexc("#9FE3FF"))
        put(img, rect(4, 11, 5, 1), hexc("#C9A3F0"))
        for x in range(4, w - 4, 2):
            put(img, [(x, h - 3)], shade_c)
        put(img, [(w - 4, h - 3)], hexc("#7CFF9E"))  # power LED (steady)

    def keys(img, w, h):
        for y in range(1, h - 1, 2):
            for x in range(1, w - 1, 2):
                put(img, [(x, y)], shade_c)

    boxes = [
        Box("case", (2, 0, 3), (14, 11, 13), cream, {"north": screen}),
        Box("keyboard", (1, 0, 0.5), (15, 1.5, 3), cream, {"up": keys}, seam=False),
    ]
    write_json(furniture_model("terminal", boxes, rng, scale=0.625, translation=(0, 0, 0)), "models", "block", "terminal.json")
    horizontal_states("terminal")
    item_def("terminal", "femboymod:block/terminal")


def vibe_scanner(rng):
    def pad_top(img, w, h):
        for x in range(0, w, 4):
            put(img, rect(x, 0, 2, h), PINK_LIGHT)

    def screen(img, w, h):  # little display on the right post: a heart meter
        put(img, rect(1, 2, w - 2, 6), hexc("#1E1A2A"))
        put(img, [(2, 6), (2, 5), (3, 4), (4, 5), (4, 6)], hexc("#FF9FD0"))

    boxes = [
        Box("pad", (0, 0, 5), (16, 1, 11), WHITE, {"up": pad_top}, seam=False),
        Box("post_left", (0, 1, 6), (2, 14, 10), WHITE),
        Box("post_right", (14, 1, 6), (16, 14, 10), WHITE, {"north": screen}),
        Box("bar", (0, 14, 6), (16, 16, 10), PINK),
    ]
    glow = {"from": [2, 13.6, 7], "to": [14, 14, 9], "light_emission": 15,
            "faces": {"down": {"uv": [0, 0, 16, 16], "texture": "#light"}, "north": {"uv": [0, 0, 16, 1], "texture": "#light"},
                      "south": {"uv": [0, 0, 16, 1], "texture": "#light"}}}
    write_json(furniture_model("vibe_scanner", boxes, rng, [glow], {"light": "femboymod:block/led_strip_pink"}, scale=0.5),
               "models", "block", "vibe_scanner.json")
    horizontal_states("vibe_scanner")
    item_def("vibe_scanner", "femboymod:block/vibe_scanner")


LED_COLORS = [("pink", "#FF7EC0"), ("purple", "#B77CFF"), ("blue", "#6FC8FF"), ("white", "#FFF6FA")]
RAINBOW_FRAMES = 12


def led_strip():
    base = Image.new("RGBA", (16, 16), hexc("#3A3440"))
    put(base, [(x, 0) for x in range(16)], hexc("#4A4452"))
    save(base, "textures", "block", "led_strip_base.png")
    for name, color in LED_COLORS:
        img = Image.new("RGBA", (16, 16), hexc(color))
        for x in range(1, 16, 3):
            put(img, [(x, 7), (x, 8)], shade(hexc(color), 1.0) if name == "white" else hexc("#FFFFFF"))
        save(img, "textures", "block", f"led_strip_{name}.png")
    # Rainbow: a soft hue gradient along the strip, shifting a little each frame and blended between frames
    frames = Image.new("RGBA", (16, 16 * RAINBOW_FRAMES))
    px = frames.load()
    for f in range(RAINBOW_FRAMES):
        for y in range(16):
            for x in range(16):
                r, g, b = colorsys.hsv_to_rgb((x / 32 + f / RAINBOW_FRAMES) % 1.0, 0.45, 1.0)
                px[x, 16 * f + y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    save(frames, "textures", "block", "led_strip_rainbow.png")
    mcmeta(10, True, "textures", "block", "led_strip_rainbow.png.mcmeta")

    for name in [n for n, _ in LED_COLORS] + ["rainbow"]:
        side = {"uv": [0, 0, 16, 1], "texture": "#base"}
        write_json({
            "parent": "minecraft:block/thin_block",
            "ambientocclusion": False,
            "textures": {"particle": f"femboymod:block/led_strip_{name}", "base": "femboymod:block/led_strip_base",
                         "light": f"femboymod:block/led_strip_{name}"},
            "elements": [
                {"from": [0, 0, 6], "to": [16, 0.5, 10], "faces": {
                    "down": {"uv": [0, 6, 16, 10], "texture": "#base", "cullface": "down"},
                    "north": side, "south": side,
                    "west": {"uv": [6, 0, 10, 1], "texture": "#base"}, "east": {"uv": [6, 0, 10, 1], "texture": "#base"}}},
                {"from": [0, 0.5, 6.5], "to": [16, 1, 9.5], "light_emission": 15, "faces": {
                    "up": {"uv": [0, 6.5, 16, 9.5], "texture": "#light"},
                    "north": {"uv": [0, 0, 16, 0.5], "texture": "#light"},
                    "south": {"uv": [0, 0, 16, 0.5], "texture": "#light"},
                    "west": {"uv": [6.5, 0, 9.5, 0.5], "texture": "#light"},
                    "east": {"uv": [6.5, 0, 9.5, 0.5], "texture": "#light"}}},
            ],
        }, "models", "block", f"led_strip_{name}.json")
    rotations = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180},
                 "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
    names = [n for n, _ in LED_COLORS] + ["rainbow"]
    variants = {}
    for facing, rot in rotations.items():
        for color, name in enumerate(names):
            variants[f"color={color},facing={facing}"] = dict({"model": f"femboymod:block/led_strip_{name}"}, **rot)
    write_json({"variants": variants}, "blockstates", "led_strip.json")
    write_json({"parent": "minecraft:item/generated", "textures": {"layer0": "femboymod:item/led_strip"}},
               "models", "item", "led_strip.json")
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    ipx = icon.load()
    for i in range(24):  # a coiled strip, rainbow along its length
        x, y = 4 + i, 22 - int(8 * abs(((i / 23) * 2) - 1))
        r, g, b = colorsys.hsv_to_rgb(i / 24, 0.5, 1.0)
        for dy in range(3):
            ipx[x, y + dy] = (int(r * 255), int(g * 255), int(b * 255), 255) if dy < 2 else hexc("#3A3440")
    outline(icon, hexc("#2B2530"))
    save(icon, "textures", "item", "led_strip.png")
    item_def("led_strip", "femboymod:item/led_strip")


# ------------------------------------------------------------------------------------------------ posters

FONT = {  # 5-row pixel font, 3 px wide except "w"
    "b": ["1..", "1..", "11.", "1.1", "11."], "t": [".1.", "111", ".1.", ".1.", ".11"],
    "w": [".....", "1...1", "1.1.1", "1.1.1", ".1.1."], ">": ["1..", ".1.", "..1", ".1.", "1.."],
    "<": ["..1", ".1.", "1..", ".1.", "..1"], "/": ["..1", "..1", ".1.", "1..", "1.."],
    "F": ["111", "1..", "11.", "1..", "1.."], "E": ["111", "1..", "11.", "1..", "111"],
    "M": ["1...1", "11.11", "1.1.1", "1...1", "1...1"], "B": ["11.", "1.1", "11.", "1.1", "11."],
    "O": [".1.", "1.1", "1.1", "1.1", ".1."], "Y": ["1.1", "1.1", ".1.", ".1.", ".1."],
    "D": ["11.", "1.1", "1.1", "1.1", "11."], " ": ["..", "..", "..", "..", ".."],
}


def text(img, s, x, y, color):
    for ch in s:
        for dy, row in enumerate(FONT[ch]):
            for dx, c in enumerate(row):
                if c == "1":
                    put(img, [(x + dx, y + dy)], color)
        x += len(FONT[ch][0]) + 1


def frame(img, color):
    w, h = img.size
    put(img, [(x, 0) for x in range(w)] + [(x, h - 1) for x in range(w)]
        + [(0, y) for y in range(h)] + [(w - 1, y) for y in range(h)], color)


def heart_at(img, cx, cy, color, big=False):
    pts = [(-1, 0), (1, 0), (-2, 1), (-1, 1), (0, 1), (1, 1), (2, 1), (-1, 2), (0, 2), (1, 2), (0, 3)]
    if big:
        pts = [(-2, 0), (-1, 0), (1, 0), (2, 0)] + [(x, 1) for x in range(-3, 4)] + [(x, 2) for x in range(-3, 4)] \
            + [(x, 3) for x in range(-2, 3)] + [(x, 4) for x in range(-1, 2)] + [(0, 5)]
    put(img, [(cx + x, cy + y) for x, y in pts], color)


def poster_btw():
    img = Image.new("RGBA", (16, 16), hexc("#1E1A2A"))
    put(img, rect(1, 1, 14, 2), hexc("#F291BE"))
    put(img, [(2, 1), (4, 1), (6, 1)], hexc("#FFE7F2"))
    text(img, ">", 2, 4, hexc("#9FE3FF"))
    put(img, rect(6, 6, 5, 1), hexc("#57505E"))
    text(img, "btw", 2, 9, hexc("#FF9FD0"))
    frame(img, hexc("#F291BE"))
    return img


def poster_sock():
    img = Image.new("RGBA", (16, 32), hexc("#E9D8FF"))
    for y in range(3, 26):
        pink = (y // 3) % 2 == 0
        put(img, rect(5, y, 6, 1), hexc("#F5A9B8") if pink else hexc("#FFFFFF"))
    put(img, rect(5, 26, 9, 3), hexc("#F5A9B8"))
    put(img, rect(12, 26, 2, 2), hexc("#FFFFFF"))
    put(img, rect(5, 2, 6, 1), hexc("#E0789E"))
    heart_at(img, 3, 7, hexc("#F291BE"))
    heart_at(img, 13, 16, hexc("#F291BE"))
    frame(img, hexc("#B58CFF"))
    return img


def poster_shark():
    img = Image.new("RGBA", (16, 16), hexc("#BFE6FF"))
    put(img, rect(0, 12, 16, 4), hexc("#86C5E8"))
    put(img, rect(3, 7, 10, 5), hexc("#86AFD0"))
    put(img, rect(4, 10, 8, 2), hexc("#F3F3EF"))
    put(img, [(7, 5), (7, 6), (8, 6)], hexc("#86AFD0"))
    put(img, [(13, 8), (14, 7), (14, 9)], hexc("#86AFD0"))
    put(img, [(5, 8), (9, 8)], hexc("#2A2233"))
    put(img, [(4, 9), (10, 9)], hexc("#F6A9C2"))
    put(img, [(7, 10)], hexc("#3A2A38"))
    frame(img, hexc("#5E8FB8"))
    return img


def poster_byte():
    img = Image.new("RGBA", (16, 32), hexc("#2B2530"))
    put(img, rect(5, 8, 7, 16), hexc("#F291BE"))
    put(img, rect(5, 7, 7, 1), hexc("#C9C3CC"))
    put(img, rect(5, 24, 7, 1), hexc("#C9C3CC"))
    put(img, [(9, 10), (8, 11), (8, 12), (7, 13), (8, 13), (9, 13), (8, 14), (7, 15), (7, 16)], hexc("#FFE7A0"))
    put(img, rect(6, 9, 1, 14), hexc("#FFC2DF"))
    for x, y in ((2, 3), (13, 5), (3, 27), (12, 28), (2, 18), (14, 20)):
        put(img, [(x, y)], hexc("#9FE3FF"))
    frame(img, hexc("#F291BE"))
    return img


def poster_code_love():
    img = Image.new("RGBA", (32, 32), hexc("#FFE7F2"))
    text(img, "<", 5, 5, hexc("#B58CFF"))
    text(img, "/", 9, 5, hexc("#B58CFF"))
    text(img, ">", 13, 5, hexc("#B58CFF"))
    heart_at(img, 16, 14, hexc("#F291BE"), big=True)
    heart_at(img, 15, 15, hexc("#FFC2DF"))
    for i, color in enumerate(("#F291BE", "#C9A3F0", "#9FE3FF")):
        put(img, rect(5, 24 + i * 2, 8 + i * 5, 1), hexc(color))
    frame(img, hexc("#F291BE"))
    return img


def poster_horizon():
    img = Image.new("RGBA", (32, 32))
    stripes = ["#5BCEFA", "#F5A9B8", "#FFFFFF", "#F5A9B8", "#5BCEFA"]
    for y in range(32):
        put(img, rect(0, y, 32, 1), hexc(stripes[min(4, y * 5 // 32)]))
    for y in range(-6, 7):
        for x in range(-6, 7):
            if x * x + y * y <= 36 and y <= 0:
                put(img, [(16 + x, 19 + y)], hexc("#FFE7A0"))
    put(img, rect(0, 20, 32, 1), hexc("#E0789E"))
    frame(img, hexc("#3A3440"))
    return img


def poster_cat_exe():
    img = Image.new("RGBA", (32, 32), hexc("#C9E6FF"))
    put(img, rect(4, 20, 24, 2), hexc("#57505E"))       # laptop base
    put(img, rect(8, 10, 16, 10), hexc("#2B2530"))      # laptop lid
    put(img, rect(9, 11, 14, 8), hexc("#1E1A2A"))
    put(img, rect(10, 12, 6, 1), hexc("#FF9FD0"))
    put(img, rect(12, 14, 8, 1), hexc("#9FE3FF"))
    put(img, rect(10, 16, 5, 1), hexc("#C9A3F0"))
    white = hexc("#F4EEF1")
    put(img, rect(6, 17, 12, 4), white)                 # sleeping cat on the keyboard
    put(img, [(6, 16), (8, 16), (7, 15)], white)
    put(img, rect(18, 19, 5, 2), white)
    put(img, [(8, 18), (10, 18)], hexc("#2A2233"))
    put(img, [(7, 19), (11, 19)], hexc("#F6A9C2"))
    for x, y in ((22, 6), (25, 4), (28, 2)):
        put(img, [(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)], hexc("#FFFFFF"))
    frame(img, hexc("#86AFD0"))
    return img


def poster_pink_creeper():
    img = Image.new("RGBA", (16, 16), hexc("#FFC2DF"))
    for y in range(1, 15):
        for x in range(1, 15):
            if (x * 7 + y * 3) % 5 == 0:
                put(img, [(x, y)], hexc("#F4AFC8"))
    face = hexc("#6B2F4A")
    put(img, rect(3, 4, 3, 3) + rect(10, 4, 3, 3), face)
    put(img, rect(6, 7, 4, 3) + rect(5, 9, 2, 3) + rect(9, 9, 2, 3), face)
    put(img, [(3, 4), (10, 4)], hexc("#FFFFFF"))
    heart_at(img, 13, 11, hexc("#F291BE"))
    frame(img, hexc("#E0789E"))
    return img


def poster_stay_hydrated():
    img = Image.new("RGBA", (16, 32), hexc("#C9E6FF"))
    for y in range(9, 27):
        inset = (y - 9) // 7
        put(img, rect(4 + inset, y, 8 - 2 * inset, 1), hexc("#E8C4B0"))
    for x, y in ((6, 24), (9, 25), (7, 21), (10, 22)):
        put(img, rect(x, y, 2, 2), hexc("#3A2433"))
    put(img, rect(3, 8, 10, 1), hexc("#FFE7F2"))
    put(img, [(9 + i // 3, 7 - i) for i in range(6)], hexc("#F291BE"))
    heart_at(img, 8, 29, hexc("#F291BE"))
    frame(img, hexc("#86AFD0"))
    return img


def poster_mochi_friends():
    img = Image.new("RGBA", (16, 16), hexc("#E9D8FF"))
    disc(img, 5, 10, 3, 3, hexc("#F7F3F5"))
    disc(img, 11, 10, 3, 3, hexc("#FFC2DF"))
    put(img, [(4, 10), (6, 10), (10, 10), (12, 10)], hexc("#2A2233"))
    put(img, [(3, 11), (13, 11)], hexc("#F291BE"))
    heart_at(img, 8, 3, hexc("#F291BE"))
    frame(img, hexc("#B58CFF"))
    return img


def poster_rainy_window():
    img = Image.new("RGBA", (32, 32), hexc("#8FA8C8"))
    rng = random.Random(21)
    for _ in range(40):
        x, y = rng.randrange(2, 30), rng.randrange(2, 22)
        put(img, [(x, y), (x, y + 1)], hexc("#C9E6FF"))
    put(img, rect(2, 15, 28, 1) + rect(15, 2, 1, 20), hexc("#EDE3D6"))
    put(img, rect(0, 22, 32, 10), hexc("#EDE3D6"))
    dark = hexc("#2B2530")
    put(img, rect(11, 16, 7, 6) + rect(12, 13, 5, 4), dark)                 # cat silhouette on the sill
    put(img, [(12, 12), (16, 12), (18, 20), (19, 19), (20, 18)], dark)
    put(img, [(13, 14), (15, 14)], hexc("#FFE7A0"))
    put(img, rect(20, 23, 6, 4), hexc("#E8C4B0"))                           # a warm cup
    frame(img, hexc("#57505E"))
    return img


def poster_pixel_heart():
    img = Image.new("RGBA", (16, 16), hexc("#FFE7F2"))
    heart_at(img, 8, 4, hexc("#F291BE"), big=True)
    put(img, [(6, 5), (5, 6)], hexc("#FFFFFF"))
    for x, y in ((2, 2), (13, 3), (3, 13), (13, 12)):
        put(img, [(x, y), (x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)], hexc("#FFE7A0"))
    frame(img, hexc("#F291BE"))
    return img


def poster_night_coding():
    img = Image.new("RGBA", (32, 32), hexc("#1E1A2A"))
    disc(img, 24, 7, 4, 4, hexc("#FFF3B0"))
    disc(img, 26, 6, 3, 3, hexc("#1E1A2A"))
    rng = random.Random(8)
    for _ in range(12):
        put(img, [(rng.randrange(2, 20), rng.randrange(2, 12))], hexc("#C9C3CC"))
    put(img, rect(6, 15, 20, 11), hexc("#F291BE"))
    put(img, rect(7, 16, 18, 9), hexc("#2B2530"))
    for i, color in enumerate(("#FF9FD0", "#9FE3FF", "#C9A3F0", "#FFE7A0")):
        put(img, rect(9 + (i % 2) * 2, 17 + i * 2, 6 + i * 2, 1), hexc(color))
    put(img, rect(14, 26, 4, 2) + rect(11, 28, 10, 1), hexc("#EDE3D6"))
    frame(img, hexc("#B58CFF"))
    return img


def poster_duck():
    img = Image.new("RGBA", (16, 16), hexc("#BFE6FF"))
    put(img, rect(0, 12, 16, 4), hexc("#86C5E8"))
    put(img, rect(4, 8, 8, 5), hexc("#FFD84A"))
    put(img, rect(6, 4, 5, 5), hexc("#FFD84A"))
    put(img, rect(11, 6, 2, 2), hexc("#FF9A3C"))
    put(img, [(9, 5)], hexc("#2A2233"))
    put(img, [(3, 8), (3, 9)], hexc("#FFD84A"))
    frame(img, hexc("#E0A800"))
    return img


def poster_snack_time():
    img = Image.new("RGBA", (16, 16), hexc("#FFE7C8"))
    for y in range(4, 13):
        half = min(5, (y - 3) * 5 // 7 + 1)
        put(img, rect(8 - half, y, 2 * half, 1), hexc("#F7F5EE"))
    put(img, rect(5, 9, 6, 4), hexc("#2B3A2E"))
    put(img, [(6, 6), (9, 6)], hexc("#2A2233"))
    put(img, [(5, 7), (10, 7)], hexc("#F6A9C2"))
    frame(img, hexc("#E0A860"))
    return img


def poster_cherry_blossom():
    img = Image.new("RGBA", (16, 32), hexc("#FFF3F8"))
    branch = hexc("#6B4A3A")
    for i in range(20):
        put(img, [(2 + i // 3, 6 + i)], branch)
    for i in range(6):
        put(img, [(5 + i, 12 + i // 2)], branch)
    rng = random.Random(4)
    for _ in range(9):
        x, y = rng.randrange(3, 13), rng.randrange(4, 28)
        put(img, [(x, y), (x + 1, y), (x, y + 1), (x - 1, y), (x, y - 1)], hexc("#F7B6CF"))
        put(img, [(x, y)], hexc("#FFE7A0"))
    frame(img, hexc("#E0789E"))
    return img


def poster_gamepad():
    img = Image.new("RGBA", (32, 32), hexc("#E9D8FF"))
    body = hexc("#F291BE")
    put(img, rect(7, 12, 18, 9), body)
    disc(img, 8, 18, 4, 5, body)
    disc(img, 23, 18, 4, 5, body)
    put(img, rect(9, 15, 5, 1) + rect(11, 13, 1, 5), hexc("#2B2530"))      # d-pad
    for x, y, c in ((21, 14, "#9FE3FF"), (23, 16, "#FFE7A0"), (19, 16, "#7CFF9E"), (21, 18, "#C9A3F0")):
        put(img, rect(x, y, 2, 2), hexc(c))
    put(img, rect(14, 14, 1, 1) + rect(17, 14, 1, 1), hexc("#FFE7F2"))
    heart_at(img, 16, 5, hexc("#B58CFF"))
    frame(img, hexc("#B58CFF"))
    return img


def poster_pastel_waves():
    img = Image.new("RGBA", (32, 32), hexc("#FFF3F8"))
    import math
    for k, color in enumerate(("#F7B6CF", "#E9C6FF", "#C9E6FF", "#C9F2DA", "#FFE7B8")):
        for x in range(32):
            y = 6 + k * 5 + round(2 * math.sin((x + k * 4) / 4.0))
            put(img, rect(x, y, 1, 3), hexc(color))
    frame(img, hexc("#C9A3F0"))
    return img


def poster_cozy_blanket():
    img = Image.new("RGBA", (16, 32), hexc("#FFE7C8"))
    put(img, rect(2, 12, 12, 16), hexc("#F7B6CF"))
    for y in range(13, 27, 3):
        put(img, rect(2, y, 12, 1), hexc("#FFE7F2"))
    white = hexc("#F4EEF1")
    put(img, rect(5, 7, 6, 5), white)
    put(img, [(5, 6), (10, 6)], white)
    put(img, [(6, 9), (9, 9)], hexc("#2A2233"))
    put(img, [(7, 10), (8, 10)], hexc("#F6A9C2"))
    for x, y in ((12, 4), (13, 2), (14, 1)):
        put(img, [(x, y)], hexc("#B58CFF"))
    frame(img, hexc("#E0A860"))
    return img


def poster_hello_code():
    img = Image.new("RGBA", (32, 32), hexc("#1E1A2A"))
    put(img, rect(2, 2, 28, 3), hexc("#F291BE"))
    put(img, [(4, 3), (6, 3), (8, 3)], hexc("#FFE7F2"))
    text(img, ">", 4, 9, hexc("#9FE3FF"))
    put(img, rect(9, 11, 12, 1), hexc("#C9A3F0"))
    text(img, "<", 4, 17, hexc("#FF9FD0"))
    text(img, "/", 8, 17, hexc("#FF9FD0"))
    text(img, ">", 12, 17, hexc("#FF9FD0"))
    heart_at(img, 24, 17, hexc("#FF9FD0"))
    put(img, rect(4, 25, 3, 2), hexc("#7CFF9E"))
    frame(img, hexc("#F291BE"))
    return img


POSTERS = {
    "btw": (poster_btw, "#FF9FD0"),
    "stay_warm": (poster_sock, "#C9A3F0"),
    "plush_buddy": (poster_shark, "#86AFD0"),
    "byte_break": (poster_byte, "#F291BE"),
    "code_with_love": (poster_code_love, "#F291BE"),
    "pastel_horizon": (poster_horizon, "#5BCEFA"),
    "cat_exe": (poster_cat_exe, "#86AFD0"),
    "pink_creeper": (poster_pink_creeper, "#E0789E"),
    "stay_hydrated": (poster_stay_hydrated, "#86AFD0"),
    "mochi_friends": (poster_mochi_friends, "#B58CFF"),
    "rainy_window": (poster_rainy_window, "#57505E"),
    "pixel_heart": (poster_pixel_heart, "#F291BE"),
    "night_coding": (poster_night_coding, "#B58CFF"),
    "debug_duck": (poster_duck, "#E0A800"),
    "snack_time": (poster_snack_time, "#E0A860"),
    "cherry_blossom": (poster_cherry_blossom, "#E0789E"),
    "pastel_gamepad": (poster_gamepad, "#B58CFF"),
    "pastel_waves": (poster_pastel_waves, "#C9A3F0"),
    "cozy_blanket": (poster_cozy_blanket, "#E0A860"),
    "hello_code": (poster_hello_code, "#F291BE"),
}


def posters():
    data = os.path.join(ROOT, "..", "..", "data", "femboymod", "painting_variant")
    os.makedirs(data, exist_ok=True)
    for name, (draw, _) in POSTERS.items():
        img = draw()
        save(img, "textures", "painting", name + ".png")
        with open(os.path.join(data, name + ".json"), "w") as f:
            json.dump({
                "asset_id": f"femboymod:{name}",
                "author": {"color": "gray", "translate": f"painting.femboymod.{name}.author"},
                "height": img.height // 16,
                "title": {"color": "light_purple", "translate": f"painting.femboymod.{name}.title"},
                "width": img.width // 16,
            }, f, indent=2)
            f.write("\n")


# ------------------------------------------------------------------------------------------------ food icons (32x32)

def disc(img, cx, cy, rx, ry, color):
    for y in range(cy - ry, cy + ry + 1):
        for x in range(cx - rx, cx + rx + 1):
            if ((x - cx) / max(rx, 0.5)) ** 2 + ((y - cy) / max(ry, 0.5)) ** 2 <= 1.0:
                put(img, [(x, y)], color)


def food_icons():
    ink = hexc("#3A2A38")
    # Bubble tea: clear cup, pink-tan tea, dark pearls at the bottom, domed lid and a straw
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(10, 28):
        inset = (y - 10) // 6
        put(img, rect(9 + inset, y, 14 - 2 * inset, 1), hexc("#E8C4B0"))
    for x, y in ((12, 24), (15, 25), (18, 24), (13, 21), (17, 22), (20, 21), (11, 18), (16, 19)):
        put(img, rect(x - 1, y - 1, 2, 2), hexc("#3A2433"))
        put(img, [(x - 1, y - 1)], hexc("#7A5A6A"))
    put(img, rect(10, 12, 1, 12), hexc("#FFF3EC"))
    disc(img, 16, 9, 8, 2, hexc("#FFE7F2"))
    put(img, rect(8, 9, 17, 1), hexc("#F7C9DC"))
    put(img, [(17 + i // 2, 7 - i) for i in range(7)] + [(18 + i // 2, 7 - i) for i in range(7)], hexc("#F291BE"))
    outline(img, ink)
    save(img, "textures", "item", "bubble_tea.png")
    # Strawberry milk: a little pink carton with a berry
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    put(img, rect(10, 11, 12, 17), hexc("#FFC2DF"))
    put(img, [(10 + i, 10 - i // 2) for i in range(6)] + [(21 - i, 10 - i // 2) for i in range(6)], hexc("#FFE7F2"))
    put(img, rect(13, 5, 6, 5), hexc("#FFE7F2"))
    put(img, rect(14, 3, 4, 2), hexc("#F7C9DC"))
    disc(img, 16, 19, 3, 3, hexc("#E0343F"))
    put(img, [(15, 18), (17, 20), (16, 21)], hexc("#FFE7A0"))
    put(img, [(15, 15), (16, 15), (17, 15), (16, 14)], hexc("#5DB85C"))
    put(img, rect(11, 12, 1, 15), hexc("#FFF3F8"))
    outline(img, ink)
    save(img, "textures", "item", "strawberry_milk.png")
    # Mochi: two soft balls, pink and white, with a tiny blush
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    disc(img, 12, 19, 7, 6, hexc("#F7F3F5"))
    disc(img, 20, 17, 7, 6, hexc("#FFC2DF"))
    put(img, [(17, 14), (18, 13), (19, 13)], hexc("#FFE7F2"))
    put(img, [(9, 17), (10, 16)], hexc("#FFFFFF"))
    put(img, [(18, 18), (19, 18)], hexc("#2A2233"))
    put(img, [(22, 18), (23, 18)], hexc("#2A2233"))
    put(img, [(17, 20), (24, 20)], hexc("#F291BE"))
    outline(img, ink)
    save(img, "textures", "item", "mochi.png")
    # Onigiri: white rice triangle with a nori band
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(6, 27):
        half = min(12, (y - 5) * 12 // 16 + 1)
        put(img, rect(16 - half, y, 2 * half, 1), hexc("#F7F5EE"))
    put(img, rect(11, 18, 10, 9), hexc("#2B3A2E"))
    put(img, rect(12, 19, 1, 7), hexc("#3E5242"))
    put(img, [(13, 10), (14, 9), (19, 12), (9, 16)], hexc("#E6E1D4"))
    outline(img, ink)
    save(img, "textures", "item", "onigiri.png")
    for name in ("bubble_tea", "strawberry_milk", "mochi", "onigiri"):
        write_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"femboymod:item/{name}"}},
                   "models", "item", name + ".json")
        item_def(name, f"femboymod:item/{name}")


# ------------------------------------------------------------------------------------------------ v1.1 clothing icons (32x32)
# layer0 is grey (tinted by the item's colorway), layer1 holds fixed-color details.

G1, G2, G3, G4 = (hexc(c) for c in ("#FFFFFF", "#D8D8D8", "#A8A8A8", "#787878"))


def tinted_item(name, default_rgb, base, detail):
    save(base, "textures", "item", name + ".png")
    save(detail, "textures", "item", name + "_detail.png")
    write_json({"parent": "minecraft:item/generated", "textures": {
        "layer0": f"femboymod:item/{name}", "layer1": f"femboymod:item/{name}_detail"}}, "models", "item", name + ".json")
    write_json({"model": {"type": "minecraft:model", "model": f"femboymod:item/{name}", "tints": [
        {"type": "femboymod:colorway", "default": default_rgb, "stripe": 0},
        {"type": "minecraft:constant", "value": -1}]}}, "items", name + ".json")


def blank():
    return Image.new("RGBA", (32, 32), (0, 0, 0, 0))


def clothing_icons():
    ink = hexc("#3A2A38")
    # Cat-ear headset: thick padded band, big ears with glowing inner parts, oval cups with a light ring, boom mic
    base, detail = blank(), blank()
    for i in range(22):
        x = 5 + i
        y = 13 - int(8 * (1 - ((i - 10.5) / 10.5) ** 2))
        put(base, rect(x, y, 1, 3), G2)
        put(base, [(x, y)], G1)
    for cx in (10, 21):                                     # ears on the band
        for row in range(5):
            put(base, rect(cx - 3 + row // 2, 1 + (4 - row), 7 - row, 1), G2)
        put(detail, [(cx, 3), (cx - 1, 4), (cx, 4), (cx + 1, 4), (cx - 1, 5), (cx, 5), (cx + 1, 5), (cx - 2, 5), (cx + 2, 5)], hexc("#FF9FD0"))
    for x0 in (1, 22):                                      # oval cups
        disc(base, x0 + 4, 20, 4, 6, G3)
        disc(base, x0 + 4, 20, 3, 5, G2)
        for dy in range(-4, 5):
            for dx in (-3, 3):
                put(detail, [(x0 + 4 + dx, 20 + dy)], hexc("#FFB6DA"))
        put(detail, rect(x0 + 3, 16, 3, 1) + rect(x0 + 3, 24, 3, 1), hexc("#FFB6DA"))
        put(base, rect(x0 + 2, 18, 5, 5), G4)
    put(base, [(6, 27), (7, 28), (8, 28), (9, 28), (10, 28)], G4)  # mic boom
    put(detail, [(11, 28), (11, 27)], hexc("#FF9FD0"))
    outline(base, ink)
    tinted_item("cat_ear_headphones", 0xF7A8CC, base, detail)
    # Heart glasses
    base, detail = blank(), blank()
    for cx in (9, 22):
        pts = [(-3, 0), (-2, 0), (2, 0), (3, 0)] + [(x, 1) for x in range(-4, 5)] + [(x, 2) for x in range(-4, 5)] \
            + [(x, 3) for x in range(-3, 4)] + [(x, 4) for x in range(-2, 3)] + [(x, 5) for x in range(-1, 2)] + [(0, 6)]
        put(base, [(cx + x, 11 + y) for x, y in pts], G2)
        put(detail, [(cx - 2, 13), (cx - 1, 12), (cx - 2, 12)], hexc("#FFFFFF"))
    put(base, rect(13, 12, 5, 1), G3)
    put(base, rect(1, 12, 3, 1) + rect(27, 12, 3, 1), G3)
    outline(base, ink)
    tinted_item("heart_glasses", 0xF291BE, base, detail)
    # Arm warmers: two striped tubes
    base, detail = blank(), blank()
    for x0 in (6, 17):
        put(base, rect(x0, 5, 9, 22), G2)
        put(base, rect(x0, 5, 9, 2) + rect(x0, 25, 9, 2), G3)
        for y in range(9, 24, 4):
            put(detail, rect(x0, y, 9, 2), hexc("#FFFFFF"))
    outline(base, ink)
    tinted_item("arm_warmers", 0xC8A2E8, base, detail)
    # Nail polish: bottle with a tinted polish and a fixed cap
    base, detail = blank(), blank()
    put(base, rect(9, 15, 14, 12), G2)
    put(base, rect(10, 16, 2, 10), G1)
    put(detail, rect(12, 6, 8, 9), hexc("#3A3440"))
    put(detail, rect(13, 7, 1, 7), hexc("#57505E"))
    put(detail, rect(11, 14, 10, 1), hexc("#C9C3CC"))
    outline(base, ink)
    tinted_item("nail_polish", 0xF291BE, base, detail)
    # Crop sweater
    base, detail = blank(), blank()
    put(base, rect(8, 7, 16, 14), G2)
    put(base, rect(2, 8, 6, 14) + rect(24, 8, 6, 14), G2)
    put(base, rect(8, 19, 16, 2) + rect(2, 20, 6, 2) + rect(24, 20, 6, 2), G3)
    put(base, rect(12, 5, 8, 3), G3)
    heart_at(detail, 16, 11, hexc("#FF8FB8"))
    outline(base, ink)
    tinted_item("crop_sweater", 0xF5A9B8, base, detail)
    # Belt chains
    base, detail = blank(), blank()
    put(base, rect(2, 10, 28, 5), G4)
    put(base, rect(2, 10, 28, 1), G3)
    heart_at(detail, 16, 10, hexc("#FF8FB8"), big=True)
    silver = hexc("#D8D8E0")
    for i in range(12):
        x = 5 + i
        put(detail, [(x, 16 + int(4 * (1 - ((i - 5.5) / 5.5) ** 2)))], silver)
    for i in range(10):
        x = 19 + i
        put(detail, [(x, 16 + int(6 * (1 - ((i - 4.5) / 4.5) ** 2)))], silver)
    outline(base, ink)
    tinted_item("belt_chains", 0x2A2A33, base, detail)


def phone_icon():
    ink = hexc("#3A2A38")
    img = blank()
    put(img, rect(9, 3, 14, 26), hexc("#F291BE"))
    put(img, rect(10, 5, 12, 20), hexc("#1E1A2A"))
    heart_at(img, 16, 12, hexc("#FF9FD0"))
    put(img, rect(14, 26, 4, 1), hexc("#FFE7F2"))
    put(img, rect(18, 3, 3, 1), hexc("#FFC2DF"))
    put(img, [(19, 7)], hexc("#9FE3FF"))
    outline(img, ink)
    save(img, "textures", "item", "phone.png")
    write_json({"parent": "minecraft:item/handheld", "textures": {"layer0": "femboymod:item/phone"}}, "models", "item", "phone.json")
    item_def("phone", "femboymod:item/phone")


def pride_badge_icon():
    """Five white stripe layers (each tinted by one colorway stripe) and a fixed gold pin on top."""
    ink = hexc("#3A2A38")
    layers = []
    for k in range(5):
        img = blank()
        put(img, rect(7, 9 + k * 3, 18, 3), G1)
        put(img, rect(7, 9 + k * 3, 1, 3), G2)
        layers.append(img)
    pin = blank()
    put(pin, rect(5, 6, 22, 1), hexc("#F2C94C"))
    put(pin, [(4, 5), (4, 6), (4, 7), (27, 6)], hexc("#B8892A"))
    frame = blank()
    put(frame, rect(7, 9, 18, 15), G1)
    outline(frame, ink)
    border = blank()
    fp, bp = frame.load(), border.load()
    for y in range(32):
        for x in range(32):
            if fp[x, y] == ink:
                bp[x, y] = ink
    for y in range(32):
        for x in range(32):
            if pin.getpixel((x, y))[3]:
                bp[x, y] = pin.getpixel((x, y))
    names = []
    for k, img in enumerate(layers):
        save(img, "textures", "item", f"pride_badge_stripe{k}.png")
        names.append(f"femboymod:item/pride_badge_stripe{k}")
    save(border, "textures", "item", "pride_badge_pin.png")
    names.append("femboymod:item/pride_badge_pin")
    write_json({"parent": "minecraft:item/generated", "textures": {f"layer{i}": n for i, n in enumerate(names)}},
               "models", "item", "pride_badge.json")
    tints = [{"type": "femboymod:colorway", "default": 0xF291BE, "stripe": k} for k in range(5)]
    tints.append({"type": "minecraft:constant", "value": -1})
    write_json({"model": {"type": "minecraft:model", "model": "femboymod:item/pride_badge", "tints": tints}}, "items", "pride_badge.json")


# ------------------------------------------------------------------------------------------------ mob effect icons (18x18)

def effect_icons():
    ink = hexc("#3A2A38")
    # Insight: a light bulb with a small spark
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    bulb, glow = hexc("#FFD84A"), hexc("#FFF3B0")
    for y in range(3, 11):
        for x in range(4, 14):
            if (x - 8.5) ** 2 + (y - 7) ** 2 <= 18:
                put(img, [(x, y)], bulb)
    put(img, [(6, 5), (7, 4), (6, 6)], glow)
    put(img, rect(7, 11, 4, 1), hexc("#C9C3CC"))
    put(img, rect(7, 12, 4, 1), hexc("#9A94A0"))
    put(img, rect(8, 13, 2, 1), hexc("#C9C3CC"))
    put(img, [(15, 2), (15, 4), (14, 3), (16, 3)], glow)
    outline(img, ink)
    save(img, "textures", "mob_effect", "insight.png")
    # Caffeinated: a pink can with a bolt
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    put(img, rect(6, 3, 6, 12), hexc("#F291BE"))
    put(img, rect(6, 2, 6, 1) + rect(6, 15, 6, 1), hexc("#C9C3CC"))
    put(img, [(9, 5), (8, 6), (8, 7), (7, 8), (8, 8), (9, 8), (9, 9), (8, 10), (8, 11)], hexc("#FFE7A0"))
    put(img, rect(7, 4, 1, 10), hexc("#FFC2DF"))
    outline(img, ink)
    save(img, "textures", "mob_effect", "caffeinated.png")
    # Jitter: a shaky zigzag
    img = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    pts = [(2, 9), (4, 5), (6, 12), (8, 4), (10, 13), (12, 5), (14, 11), (15, 8)]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        for i in range(8):
            put(img, [(round(x0 + (x1 - x0) * i / 8), round(y0 + (y1 - y0) * i / 8))], hexc("#B0E0FF"))
    outline(img, ink)
    save(img, "textures", "mob_effect", "jitter.png")


def photo_watermark():
    """Selfie watermark (Photo Mode): pink "FEMBOY MOD" with a heart and a white outline, scaled in game."""
    img = Image.new("RGBA", (56, 11), (0, 0, 0, 0))
    heart_at(img, 5, 2, hexc("#F291BE"))
    text(img, "FEMBOY MOD", 11, 3, hexc("#F291BE"))
    outline(img, hexc("#FFFFFF"))
    save(img, "textures", "gui", "photo_watermark.png")
    # Same logo as the NeoForge mod list banner (bannerFile), upscaled 8x nearest so the pixels stay crisp
    banner = img.resize((img.width * 8, img.height * 8), Image.NEAREST)
    banner.save(os.path.join(ROOT, "banner.png"))


# ------------------------------------------------------------------------------------------------ moonstone, neon quartz, geodes

MOON = [hexc(c) for c in ("#FFFFFF", "#DCE8FF", "#B9CCF2", "#8FA3D6")]
NEON = [hexc(c) for c in ("#FFFFFF", "#7CF7FF", "#FF5CE1", "#8A5CFF")]


def glowing_overlay_model(base, overlay, emission):
    model = overlay_model(base, overlay)
    model["elements"][1]["light_emission"] = emission
    return model


def crystal_sprite(size, seed):
    """Cross-model crystal sprite: 1..3 pointed shards growing from the bottom, taller for bigger stages."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shards = [(8, size)] + ([(5, size * 2 // 3), (11, size * 3 // 4)] if size >= 8 else [])
    for cx, height in shards:
        width = 2 if height < 8 else 3
        for y in range(16 - height, 16):
            t = (y - (16 - height)) / max(1, height)
            half = max(0, int(round(width * min(1.0, t * 2))))
            for x in range(cx - half, cx + half + 1):
                if 0 <= x < 16:
                    tone = 0 if x < cx else 1 if x == cx else 2
                    img.putpixel((x, y), ROSE[tone] if rng.random() > 0.08 else ROSE[3])
        img.putpixel((cx, 16 - height), ROSE[0])
    return img


def cluster_blockstate(name):
    rot = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90}, "west": {"x": 90, "y": 270}}
    write_json({"variants": {f"facing={f}": dict({"model": f"femboymod:block/{name}"}, **r) for f, r in rot.items()}}, "blockstates", name + ".json")


def new_ores():
    save(crystal_overlay(31, [(3, 4), (11, 3), (7, 9), (2, 12)], MOON), "textures", "block", "deepslate_moonstone_ore_overlay.png")
    write_json(glowing_overlay_model("minecraft:block/deepslate", "femboymod:block/deepslate_moonstone_ore_overlay", 8),
               "models", "block", "deepslate_moonstone_ore.json")
    simple_block("deepslate_moonstone_ore")
    save(crystal_overlay(32, [(4, 3), (11, 5), (6, 10), (12, 12), (2, 8)], NEON), "textures", "block", "neon_quartz_ore_overlay.png")
    write_json(glowing_overlay_model("minecraft:block/netherrack", "femboymod:block/neon_quartz_ore_overlay", 10),
               "models", "block", "neon_quartz_ore.json")
    simple_block("neon_quartz_ore")

    # Budding rose quartz: the block texture with bright budding spots
    img = Image.open(path("textures", "block", "rose_quartz_block.png")).convert("RGBA")
    for x, y in ((4, 4), (11, 6), (6, 11), (12, 12)):
        put(img, [(x, y), (x + 1, y), (x, y + 1), (x - 1, y), (x, y - 1)], ROSE[0])
        put(img, [(x, y)], hexc("#FFFFFF"))
    save(img, "textures", "block", "budding_rose_quartz.png")
    write_json({"parent": "minecraft:block/cube_all", "textures": {"all": "femboymod:block/budding_rose_quartz"}}, "models", "block", "budding_rose_quartz.json")
    simple_block("budding_rose_quartz")
    for name, size, seed in (("small_rose_quartz_bud", 4, 1), ("medium_rose_quartz_bud", 7, 2), ("large_rose_quartz_bud", 10, 3), ("rose_quartz_cluster", 14, 4)):
        save(crystal_sprite(size, seed), "textures", "block", name + ".png")
        write_json({"parent": "minecraft:block/cross", "textures": {"cross": f"femboymod:block/{name}"}}, "models", "block", name + ".json")
        cluster_blockstate(name)
        write_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"femboymod:block/{name}"}}, "models", "item", name + ".json")
        item_def(name, f"femboymod:item/{name}")

    # Item icons
    ink = hexc("#2A2A4A")
    img = blank()
    for y in range(-7, 8):                                   # crescent moonstone
        for x in range(-7, 8):
            if x * x + y * y <= 49 and (x - 3) ** 2 + (y + 1) ** 2 > 30:
                put(img, [(16 + x, 16 + y)], MOON[1] if x + y < 0 else MOON[2])
    put(img, [(11, 11), (12, 10), (10, 13)], MOON[0])
    outline(img, ink)
    save(img, "textures", "item", "moonstone.png")
    img = blank()
    gemcolors = NEON
    for yy in range(-8, 9):
        for xx in range(-5, 6):
            if abs(xx) + abs(yy) / 1.6 <= 5:
                put(img, [(16 + xx, 16 + yy)], gemcolors[1] if xx < 0 else gemcolors[2] if yy > 0 else gemcolors[3])
    put(img, [(14, 10), (13, 12)], NEON[0])
    outline(img, hexc("#1E1A2A"))
    save(img, "textures", "item", "neon_quartz.png")
    for name in ("moonstone", "neon_quartz"):
        write_json({"parent": "minecraft:item/generated", "textures": {"layer0": f"femboymod:item/{name}"}}, "models", "item", name + ".json")
        item_def(name, f"femboymod:item/{name}")

    # Pendant (grey chain tinted by colorway + fixed moonstone) and visor (grey frame + fixed neon lens)
    base, detail = blank(), blank()
    for i in range(20):
        x = 6 + i
        put(base, [(x, 6 + int(12 * (1 - ((i - 9.5) / 9.5) ** 2)))], G2)
    for y in range(-4, 5):
        for x in range(-4, 5):
            if x * x + y * y <= 16 and (x - 2) ** 2 + (y + 1) ** 2 > 9:
                put(detail, [(16 + x, 23 + y)], MOON[1])
    put(detail, [(14, 21)], MOON[0])
    outline(base, ink)
    tinted_item("moonstone_pendant", 0xF2C94C, base, detail)
    base, detail = blank(), blank()
    put(base, rect(3, 12, 26, 1) + rect(3, 19, 26, 1) + rect(2, 12, 1, 8) + rect(29, 12, 1, 8), G3)
    put(base, rect(1, 14, 2, 3) + rect(29, 14, 2, 3), G4)
    put(detail, rect(3, 13, 26, 6), NEON[1])
    put(detail, rect(4, 14, 8, 1), NEON[0])
    outline(base, hexc("#1E1A2A"))
    tinted_item("cyber_visor", 0x2A2A33, base, detail)


def dark_shades_icon():
    base, detail = blank(), blank()
    put(base, rect(3, 10, 26, 2), G3)                          # brow bar
    for x0 in (4, 17):
        put(base, rect(x0, 12, 11, 8), G3)
        put(detail, rect(x0 + 1, 13, 9, 6), hexc("#121218"))
        put(detail, rect(x0 + 2, 14, 3, 1) + rect(x0 + 2, 15, 1, 1), hexc("#8A8A9A"))
    put(base, rect(15, 13, 2, 2), G3)                          # bridge
    put(base, rect(1, 11, 2, 2) + rect(29, 11, 2, 2), G4)
    outline(base, hexc("#1E1A2A"))
    tinted_item("dark_shades", 0x2A2A33, base, detail)


def tail_icon():
    """Fluffy fox-style tail: slim base (bottom left), bushy middle, arcing up to a short pointed white tip,
    tufts of fur sticking out of the edge."""
    import math
    base, detail = blank(), blank()
    rng = random.Random(12)
    ink = hexc("#3A2A38")

    def centre(t):
        # an arc bulging to the lower right: base at the bottom left, tip at the top right
        a = math.radians(100 - 102 * t)
        return 6 + 19 * math.cos(a), 6 + 19 * math.sin(a)

    def radius(t):
        return max(0.6, 0.8 + 5.4 * math.sin(math.pi * t ** 1.3) ** 1.1)

    tip_from = 0.82
    samples = [i / 199 for i in range(200)]
    owner = {}                                                 # pixel -> t of the nearest centre sample
    for t in samples:
        x, y = centre(t)
        r = radius(t)
        for yy in range(int(y - r - 1), int(y + r + 2)):
            for xx in range(int(x - r - 1), int(x + r + 2)):
                d = math.hypot(xx + 0.5 - x, yy + 0.5 - y)
                if d <= r and 0 <= xx < 32 and 0 <= yy < 32:
                    owner[(xx, yy)] = max(owner.get((xx, yy), -1), t)
    # fur tufts: little spikes pushed outward along the edge
    for _ in range(14):
        t = rng.uniform(0.3, 0.9)
        x, y = centre(t)
        x2, y2 = centre(min(1.0, t + 0.01))
        nx, ny = -(y2 - y), x2 - x
        n = math.hypot(nx, ny) or 1
        side = rng.choice((-1, 1))
        r = radius(t)
        for k in range(1):
            px = int(x + side * nx / n * (r + k) + 0.5)
            py = int(y + side * ny / n * (r + k) + 0.5)
            if 0 <= px < 32 and 0 <= py < 32:
                owner.setdefault((px, py), t)
    for (xx, yy), t in owner.items():
        x, y = centre(t)
        light = (x - xx) + (y - yy)
        if t >= tip_from:
            detail.putpixel((xx, yy), hexc("#FFFFFF") if light > -1 else hexc("#E9E1E8"))
        else:
            base.putpixel((xx, yy), G1 if light > 2.5 else G2 if light > -1.5 else G3)
    # fur strokes inside the colored part
    for _ in range(12):
        t = rng.uniform(0.35, tip_from - 0.05)
        x, y = centre(t)
        px, py = int(x + rng.uniform(-2.5, 2.5)), int(y + rng.uniform(-2.5, 2.5))
        if base.getpixel((px, py))[3] and 0 <= px + 1 < 32 and base.getpixel((px + 1, py - 1))[3]:
            put(base, [(px, py), (px + 1, py - 1)], G1)
    merged = base.copy()
    merged.alpha_composite(detail)
    outline(merged, ink)
    mp, bp = merged.load(), base.load()
    for yy in range(32):
        for xx in range(32):
            if mp[xx, yy] == ink:
                bp[xx, yy] = ink
    tinted_item("tail", 0xF291BE, base, detail)


def moonstone_lamp(rng):
    stand = hexc("#EDE3D6")
    boxes = [Box("stand", (5, 0, 5), (11, 2, 11), stand, seam=False)]
    moon = Image.new("RGBA", (16, 16), MOON[1])
    for x, y, r in ((4, 5, 2), (10, 9, 2), (6, 11, 1), (11, 4, 1)):
        for yy in range(-r, r + 1):
            for xx in range(-r, r + 1):
                if xx * xx + yy * yy <= r * r:
                    put(moon, [(x + xx, y + yy)], MOON[2])
    save(moon, "textures", "block", "moonstone_lamp_moon.png")
    off = Image.new("RGBA", (16, 16), hexc("#8C96B0"))
    save(off, "textures", "block", "moonstone_lamp_moon_off.png")

    def ball(texture, emission):
        faces = {f: {"uv": [2, 2, 14, 14], "texture": "#moon"} for f in ("north", "south", "east", "west", "up", "down")}
        element = {"from": [4, 2, 4], "to": [12, 10, 12], "faces": faces}
        if emission:
            element["light_emission"] = 15
        return element

    for suffix, texture, lit in (("", "moonstone_lamp_moon", True), ("_off", "moonstone_lamp_moon_off", False)):
        model = furniture_model("moonstone_lamp", boxes, random.Random(3), [ball(texture, lit)], {"moon": f"femboymod:block/{texture}"},
                                scale=0.9, translation=(0, 2, 0))
        write_json(model, "models", "block", f"moonstone_lamp{suffix}.json")
    write_json({"variants": {"lit=true": {"model": "femboymod:block/moonstone_lamp"},
                             "lit=false": {"model": "femboymod:block/moonstone_lamp_off"}}}, "blockstates", "moonstone_lamp.json")
    item_def("moonstone_lamp", "femboymod:block/moonstone_lamp")


NEON_DESIGNS = ["heart", "cat", "btw"]


def neon_designs():
    pink, cyan = hexc("#FF5CE1"), hexc("#7CF7FF")
    heart = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pts = [(5, 4), (6, 3), (7, 4), (8, 4), (9, 3), (10, 4), (11, 5), (11, 6), (10, 7), (9, 8), (8, 9), (7, 9), (6, 8), (5, 7), (4, 6), (4, 5)]
    put(heart, pts, pink)
    cat = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    put(cat, [(4, 3), (5, 4), (6, 5), (7, 5), (8, 5), (9, 5), (10, 4), (11, 3), (11, 4), (11, 5), (11, 6), (11, 7), (10, 8), (9, 9),
              (8, 9), (7, 9), (6, 9), (5, 8), (4, 7), (4, 6), (4, 5), (4, 4)], cyan)
    put(cat, [(6, 6), (9, 6)], pink)
    btw = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    text(btw, "btw", 2, 4, pink)
    put(btw, rect(2, 11, 12, 1), cyan)
    return {"heart": heart, "cat": cat, "btw": btw}


def neon_sign():
    plate = Image.new("RGBA", (16, 16), hexc("#1E1A2A"))
    put(plate, [(x, 0) for x in range(16)] + [(x, 15) for x in range(16)], hexc("#3A3440"))
    save(plate, "textures", "block", "neon_sign_plate.png")
    variants = {}
    for index, (name, img) in enumerate(neon_designs().items()):
        save(img, "textures", "block", f"neon_sign_{name}.png")
        plate_faces = {f: {"uv": [0, 0, 16, 16], "texture": "#plate"} for f in ("north", "south", "east", "west", "up", "down")}
        write_json({
            "parent": "minecraft:block/block",
            "textures": {"particle": "femboymod:block/neon_sign_plate", "plate": "femboymod:block/neon_sign_plate",
                         "neon": f"femboymod:block/neon_sign_{name}"},
            "elements": [
                {"from": [1, 2, 14], "to": [15, 14, 16], "faces": plate_faces},
                {"from": [1, 2, 13.9], "to": [15, 14, 13.9], "light_emission": 15,
                 "faces": {"north": {"uv": [1, 2, 15, 14], "texture": "#neon"}}},
            ],
            "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.625, 0.625, 0.625]},
                        "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]}},
        }, "models", "block", f"neon_sign_{name}.json")
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            entry = {"model": f"femboymod:block/neon_sign_{name}"}
            if y:
                entry["y"] = y
            variants[f"design={index},facing={facing}"] = entry
    write_json({"variants": variants}, "blockstates", "neon_sign.json")
    item_def("neon_sign", "femboymod:block/neon_sign_heart")


def main():
    rng = random.Random(42)
    ores()
    icons()
    chair(rng)
    monitor(rng)
    keyboard(rng)
    terminal(rng)
    vibe_scanner(random.Random(9))
    led_strip()
    posters()
    effect_icons()
    food_icons()
    clothing_icons()
    pride_badge_icon()
    photo_watermark()
    phone_icon()
    new_ores()
    dark_shades_icon()
    tail_icon()
    moonstone_lamp(rng)
    neon_sign()
    print("decor assets written")


if __name__ == "__main__":
    main()
