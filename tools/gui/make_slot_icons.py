#!/usr/bin/env python3
"""Empty cosmetic slot icons (16x16 GUI sprites) in vanilla's empty-armor-slot style. Drawn in code, no AI.

Like vanilla: one dark grey, 1 px lines, no fill, no anti-aliasing, so the pixels match the GUI's at every scale.
Shapes follow the author's drafts. '#' = ink. sym() mirrors an 8-column left half.
Output: textures/gui/sprites/container/slot/cosmetic/<slot>.png (hand edits to the PNGs are fine; rerunning overwrites them)
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod",
                    "textures", "gui", "sprites", "container", "slot", "cosmetic")
# vanilla's empty armor slots use this grey
INK = (0x55, 0x55, 0x55, 255)


def sym(left):
    """Mirror an 8-column left half into 16 columns."""
    return [row + row[::-1] for row in left]


def pad(rows):
    rows = list(rows)
    while len(rows) < 16:
        rows.append("." * 16)
    for r in rows:
        assert len(r) == 16, (r, len(r))
    return rows


ICONS = {
    "head_accessory": pad(sym([
        "........",
        "........",
        "........",
        ".#......",
        ".##.....",
        ".#.#....",
        ".##.#...",
        ".###.#..",
        ".#....##",
        "#.......",
        "#.......",
        "#.......",
    ])),
    "face": pad(sym([
        "........",
        "........",
        "........",
        "........",
        "........",
        ".##.##..",
        "#..#..##",
        "#.....#.",
        ".#...#..",
        "..#.#...",
        "...#....",
    ])),
    "neck": pad(sym([
        "........",
        "........",
        "........",
        "....####",
        "..##....",
        ".#......",
        ".#......",
        "..##....",
        "....####",
        ".......#",
        "......#.",
        "......#.",
        ".......#",
    ])),
    "outfit_top": pad(sym([
        "........",
        ".....###",
        "....#...",
        "..##.#..",
        ".#...#..",
        "#.....##",
        "#..#....",
        "#..#....",
        "#..#....",
        "####....",
        "...#.###",
        "...#.#..",
        "...#.###",
        "...#....",
        "...#####",
    ])),
    "outfit_bottom": pad(sym([
        "........",
        "........",
        "........",
        "....####",
        "....#...",
        "....####",
        "...#..#.",
        "...#..#.",
        "..#..#..",
        "..#..#..",
        ".#..#...",
        ".#..#...",
        ".##.##.#",
        "...#..#.",
    ])),
    "waist": pad(sym([
        "........",
        "........",
        "........",
        "........",
        "......##",
        "#######.",
        "#.....#.",
        "#######.",
        "..#...##",
        "...#....",
        "....#...",
        ".....##.",
        ".......#",
    ])),
    "hands": pad([
        "................",
        "........#.......",
        "......##.##.....",
        ".....#.#.#.#....",
        ".....#.#.#.##...",
        ".....#.#.#.#.#..",
        ".....#.#.#.#.#..",
        "...#.#.#.#.#.#..",
        "..#.#........#..",
        "..#..#.......#..",
        "...#.........#..",
        "....#........#..",
        ".....#......#...",
        "......######....",
    ]),
    "legs_overlay": pad([
        "................",
        "........######..",
        "........#....#..",
        "........######..",
        "........#....#..",
        "........######..",
        "........#....#..",
        "........#....#..",
        "........#....#..",
        ".......#.....#..",
        "......#......#..",
        "....##.......#..",
        "...#........#...",
        "...#.......#....",
        "....#######.....",
    ]),
    "tail": pad([
        "................",
        "..........###...",
        ".........#...#..",
        "........#...#...",
        "........#..#....",
        "........#..#....",
        "........#...#...",
        "........#....#..",
        "..#.....#....#..",
        "..##...#.....#..",
        "..#.###......#..",
        "..#.........#...",
        "...#.......#....",
        "....#######.....",
    ]),
    "back": pad(sym([
        "........",
        "......##",
        ".....#..",
        "...#####",
        "..#.....",
        "..#.....",
        "..######",
        "..#....#",
        "..#.....",
        "..#.####",
        "..#.#...",
        "..#.#...",
        "..#.####",
        "...#....",
        "....####",
    ])),
}


def main():
    os.makedirs(ROOT, exist_ok=True)
    for name, rows in ICONS.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == "#":
                    img.putpixel((x, y), INK)
        img.save(os.path.join(ROOT, name + ".png"))
        print("wrote", name)


if __name__ == "__main__":
    main()
