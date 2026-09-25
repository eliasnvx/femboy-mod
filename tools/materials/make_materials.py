#!/usr/bin/env python3
"""Procedural PLACEHOLDER material textures for the code cosmetic models (fur, knit, fabric, fishnet).

They are grayscale and tinted per color group in game. The models declare a 64x64 UV space, so a
128x128 file doubles the texel density without touching any model code (--density 2).

    python3 tools/materials/make_materials.py --density 1 --out <dir>
    python3 tools/materials/make_materials.py --density 2 --out <dir>

The same design is drawn at both densities, so screenshots compare resolution, not style.
Hand-painted textures replace these before release (SPEC §11, ASSET_PROVENANCE.md).
"""
import argparse
import os
import random

from PIL import Image

UV_SIZE = 64


def gray(v, a=255):
    v = max(0, min(255, int(v)))
    return (v, v, v, a)


def fur(d, rng):
    """Strands running down, each with a lit left edge and a shaded right edge."""
    s = UV_SIZE * d
    img = Image.new("RGBA", (s, s), gray(214))
    px = img.load()
    for _ in range(int(s * s / (3 * d))):
        x, y = rng.randrange(s), rng.randrange(s)
        length = rng.randint(3, 6) * d
        tone = rng.choice((190, 200, 228, 238, 248))
        drift = rng.choice((-1, 0, 0, 1))
        for i in range(length):
            xx = (x + (i * drift) // (3 * d)) % s
            yy = (y + i) % s
            px[xx, yy] = gray(tone - i * 18 / d * 0.5)
            if d > 1:  # a 2nd texel column: highlight + shadow give each strand volume
                px[(xx + 1) % s, yy] = gray(tone - 26 - i * 4)
    return img


def knit(d, rng):
    """Stockinette: columns of V-shaped stitches. At 1x a stitch is 2 px wide and only reads as ribs."""
    s = UV_SIZE * d
    img = Image.new("RGBA", (s, s))
    px = img.load()
    half = 2 * d  # one leg of the V
    for y in range(s):
        for x in range(s):
            cx, cy = x % (2 * half), y % half
            t = cx if cx < half else 2 * half - 1 - cx  # 0 at the column edge, half-1 at the V's center
            # each leg is a slanted loop: brightest along its diagonal, darker away from it
            v = 244 - (abs(cy - t) * 40) // half
            if t == 0:
                v -= 30  # groove between stitch columns
            px[x, y] = gray(v + rng.randint(-4, 4))
    return img


def fabric(d, rng):
    """Twill: diagonal ribs; at 2x each rib gets a highlight and a thread texture."""
    s = UV_SIZE * d
    img = Image.new("RGBA", (s, s))
    px = img.load()
    period = 4 * d
    for y in range(s):
        for x in range(s):
            p = (x + y) % period
            v = 228 if p < period // 2 else 206
            if d > 1 and p == 0:
                v = 246
            if d > 1 and (x * 3 + y) % 7 == 0:
                v -= 8  # weave
            px[x, y] = gray(v + rng.randint(-5, 5))
    return img


def fishnet(d, rng):
    """Diamond net with transparent holes; at 2x the strings are half as thick relative to the leg."""
    s = UV_SIZE * d
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    px = img.load()
    period = 4 * d
    for y in range(s):
        for x in range(s):
            if (x + y) % period == 0 or (x - y) % period == 0:
                knot = (x + y) % period == 0 and (x - y) % period == 0
                px[x, y] = gray(255 if knot else 232)
    return img


MATERIALS = {"fur": fur, "knit": knit, "fabric": fabric, "fishnet": fishnet}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--density", type=int, choices=(1, 2, 4), default=1)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    for name, make in MATERIALS.items():
        make(args.density, random.Random(name)).save(os.path.join(args.out, name + ".png"))
        print(f"{name}.png {UV_SIZE * args.density}x{UV_SIZE * args.density}")


if __name__ == "__main__":
    main()
