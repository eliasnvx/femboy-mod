#!/usr/bin/env python3
"""Software preview of a JSON block model (axis-aligned elements, no element rotation): renders the model
like the inventory icon (GUI rotation) and from the front, big, so shapes and UVs can be checked without
starting the game.  python3 tools/plush/preview.py cat_plush out.png"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

ASSETS = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod")
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}


def corners(frm, to, face):
    (x0, y0, z0), (x1, y1, z1) = frm, to
    # order: top-left, top-right, bottom-right, bottom-left as seen from outside (matches MC's default UV)
    return {
        "north": [(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        "south": [(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        "west": [(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        "east": [(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        "up": [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)],
        "down": [(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)],
    }[face]


def render(model, tex, yaw, pitch, size=512):
    t = np.asarray(tex.convert("RGBA")).astype(np.float32)
    th, tw = t.shape[:2]
    cy, sy, cp, sp = math.cos(math.radians(yaw)), math.sin(math.radians(yaw)), math.cos(math.radians(pitch)), math.sin(math.radians(pitch))

    def proj(p):
        x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
        x, z = x * cy - z * sy, x * sy + z * cy          # yaw around y
        y, z = y * cp - z * sp, y * sp + z * cp          # pitch around x
        return np.array([x, -y]), z

    img = np.zeros((size, size, 4), np.float32)
    depth = np.full((size, size), np.inf)
    scale = size / 22.0
    for el in model["elements"]:
        for face, spec in el["faces"].items():
            quad = corners(el["from"], el["to"], face)
            pts, zs = zip(*(proj(p) for p in quad))
            pts = np.array(pts) * scale + size / 2
            u0, v0, u1, v1 = [c / 16 for c in spec["uv"]]
            uvs = np.array([(u0, v0), (u1, v0), (u1, v1), (u0, v1)])
            for tri in ((0, 1, 2), (0, 2, 3)):
                a, b, c = pts[list(tri)]
                za, zb, zc = (zs[i] for i in tri)
                ua, ub, uc = uvs[list(tri)]
                xs, ys = [a[0], b[0], c[0]], [a[1], b[1], c[1]]
                x_min, x_max = int(max(0, min(xs))), int(min(size - 1, max(xs)) + 1)
                y_min, y_max = int(max(0, min(ys))), int(min(size - 1, max(ys)) + 1)
                den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
                if abs(den) < 1e-9:
                    continue
                gx, gy = np.meshgrid(np.arange(x_min, x_max) + 0.5, np.arange(y_min, y_max) + 0.5)
                w1 = ((b[1] - c[1]) * (gx - c[0]) + (c[0] - b[0]) * (gy - c[1])) / den
                w2 = ((c[1] - a[1]) * (gx - c[0]) + (a[0] - c[0]) * (gy - c[1])) / den
                w3 = 1 - w1 - w2
                inside = (w1 >= 0) & (w2 >= 0) & (w3 >= 0)
                z = w1 * za + w2 * zb + w3 * zc
                u = w1 * ua[0] + w2 * ub[0] + w3 * uc[0]
                v = w1 * ua[1] + w2 * ub[1] + w3 * uc[1]
                tx = np.clip((u * tw).astype(int), 0, tw - 1)
                ty = np.clip((v * th).astype(int), 0, th - 1)
                col = t[ty, tx]
                sub_depth = depth[y_min:y_max, x_min:x_max]
                ok = inside & (z < sub_depth) & (col[..., 3] > 0)
                sub_depth[ok] = z[ok]
                region = img[y_min:y_max, x_min:x_max]
                region[ok] = np.concatenate([col[..., :3][ok] * SHADE[face], col[..., 3:][ok]], axis=-1)
    return Image.fromarray(img.clip(0, 255).astype(np.uint8))


def main():
    name, out = sys.argv[1], sys.argv[2]
    model = json.load(open(os.path.join(ASSETS, "models", "block", name + ".json")))
    tex = Image.open(os.path.join(ASSETS, "textures", "block", name + ".png"))
    views = [render(model, tex, 45, -30), render(model, tex, 0, -10), render(model, tex, 90, -20)]
    sheet = Image.new("RGBA", (len(views) * 512, 512), (70, 70, 80, 255))
    for i, v in enumerate(views):
        sheet.alpha_composite(v, (i * 512, 0))
    sheet.save(out)


if __name__ == "__main__":
    main()
