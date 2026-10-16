#!/usr/bin/env python3
"""Hand-drawn 16x16 item icons as pixel maps (no AI). Writes textures/item/<name>.png, plus the item model and the
item definition when they are missing.

    python3 tools/items/make_item_icons.py            # writes everything
    python3 tools/items/make_item_icons.py --preview  # also build/item_icons_preview.png at 8x
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
ASSETS = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "femboymod")

# One palette for all icons; '.' is transparent
PALETTE = {
    "o": (0x4A, 0x20, 0x38),  # outline (dark plum)
    "c": (0xC2, 0x50, 0x8E),  # pink cover
    "h": (0xE0, 0x7A, 0xB0),  # cover highlight
    "b": (0x8E, 0x34, 0x66),  # spine / cover shade
    "p": (0xFB, 0xF4, 0xEA),  # paper
    "s": (0xE0, 0xCF, 0xC4),  # paper shade
    "w": (0xFF, 0xE4, 0xF1),  # heart
    "y": (0xF6, 0xC9, 0x5A),  # gold ribbon
    "g": (0x7F, 0xC6, 0x6E),  # photo: grass
    "k": (0x9C, 0xC8, 0xF0),  # photo: sky
    "m": (0xF2, 0x8F, 0xB8),  # photo: pink figure
}

ICONS = {
    # an instant print: picture with sky, grass and a little pink figure, wide margin at the bottom
    "photo": [
        "................",
        "..oooooooooooooo",
        "..oppppppppppppo",
        "..opkkkkkkkkkkpo",
        "..opkkkkkmkkkkpo",
        "..opkkkkmmmkkkpo",
        "..opkkkkkmkkkkpo",
        "..opggggmgmgggpo",
        "..opggggggggggpo",
        "..opggggggggggpo",
        "..oppppppppppppo",
        "..oppppppppcpppo",
        "..oppppppppppppo",
        "..oooooooooooooo",
        "................",
        "................",
    ],
    "lookbook": [
        "................",
        "...ooooooooooo..",
        "..obhhhhhhhhhco.",
        "..obchhhhhhhcco.",
        "..obcwwccwwccco.",
        "..obwwwwwwwwcco.",
        "..obwwwwwwwwcco.",
        "..obcwwwwwwccco.",
        "..obccwwwwcccco.",
        "..obcccwwccccco.",
        "..obcccccccccco.",
        "..obcccccccycco.",
        "..oooooooooyooo.",
        "..opppppppsyspo.",
        "...oooooooooyo..",
        "................",
    ],
}


def icon(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), PALETTE[ch] + (255,))
    return img


def write_if_missing(path, obj):
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            f.write(json.dumps(obj, indent=2) + "\n")


def photo_textures():
    """Card and 'developing' textures for the photo item model (PhotoSpecialRenderer)."""
    card = Image.new("RGBA", (32, 32), (0xFA, 0xF6, 0xEE, 255))
    for i in range(32):
        card.putpixel((i, 0), (0xFF, 0xFD, 0xF8, 255))
        card.putpixel((i, 31), (0xD9, 0xCF, 0xC2, 255))
        card.putpixel((0, i), (0xFF, 0xFD, 0xF8, 255))
        card.putpixel((31, i), (0xD9, 0xCF, 0xC2, 255))
    card.save(os.path.join(ASSETS, "textures", "item", "photo_card.png"))
    developing = Image.new("RGBA", (32, 32))
    for y in range(32):
        for x in range(32):
            t = (x + y) / 62
            developing.putpixel((x, y), (int(0x5C + 0x30 * t), int(0x55 + 0x28 * t), int(0x5E + 0x22 * t), 255))
    developing.save(os.path.join(ASSETS, "textures", "item", "photo_developing.png"))


def main():
    photo_textures()
    images = {}
    for name, rows in ICONS.items():
        img = icon(rows)
        images[name] = img
        img.save(os.path.join(ASSETS, "textures", "item", name + ".png"))
        write_if_missing(os.path.join(ASSETS, "models", "item", name + ".json"),
                         {"parent": "minecraft:item/generated", "textures": {"layer0": "femboymod:item/" + name}})
        write_if_missing(os.path.join(ASSETS, "items", name + ".json"),
                         {"model": {"type": "minecraft:model", "model": "femboymod:item/" + name}})
    if "--preview" in sys.argv:
        sheet = Image.new("RGBA", (len(images) * 136, 136), (0x8B, 0x8B, 0x8B, 255))
        for i, img in enumerate(images.values()):
            sheet.alpha_composite(img.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4))
        os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
        sheet.save(os.path.join(ROOT, "build", "item_icons_preview.png"))
    print("wrote", ", ".join(images))


if __name__ == "__main__":
    main()
