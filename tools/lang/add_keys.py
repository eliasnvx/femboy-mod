#!/usr/bin/env python3
"""Adds or updates translation keys in every lang file (sorted keys, 2-space indent, like the files are kept).

    python3 tools/lang/add_keys.py keys.json

keys.json: {"some.key": {"en_us": "Text", "ru_ru": "Текст", ...}, ...}. Every key needs every language that has a
file in assets/femboymod/lang (LangFilesTest checks the key sets match).
"""
import json
import os
import sys

LANG = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "femboymod",
                    "lang")


def main():
    keys = json.load(open(sys.argv[1], encoding="utf-8"))
    langs = sorted(f[:-5] for f in os.listdir(LANG) if f.endswith(".json"))
    missing = [(k, l) for k, texts in keys.items() for l in langs if l not in texts]
    if missing:
        raise SystemExit("missing translations: " + ", ".join(f"{k} [{l}]" for k, l in missing))
    for lang in langs:
        path = os.path.join(LANG, lang + ".json")
        data = json.load(open(path, encoding="utf-8"))
        for key, texts in keys.items():
            data[key] = texts[lang]
        with open(path, "w", encoding="utf-8") as f:
            f.write(json.dumps(dict(sorted(data.items())), indent=2, ensure_ascii=False) + "\n")
    print(f"{len(keys)} keys in {len(langs)} languages")


if __name__ == "__main__":
    main()
