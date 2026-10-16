#!/usr/bin/env python3
"""The mod's own sounds, synthesized from scratch (numpy), no samples, no AI:
- the Bug mob: a glitchy little beetle that chitters like an insect and blips like a crashing program;
- headpats: two soft pats and a small sparkle.

    .venv/bin/python tools/sounds/make_sounds.py            # WAV previews in build/sounds + mono OGG in assets
    .venv/bin/python tools/sounds/make_sounds.py --wav-only # previews only

OGG needs `soundfile` (libsndfile with Vorbis) in the venv: `.venv/bin/pip install -r tools/sounds/requirements.txt`.
ffmpeg's built-in Vorbis encoder only writes stereo, and Minecraft plays stereo sounds without distance falloff.
Soft, short and quiet on purpose: band-limited square waves, gentle bit crushing, no harsh noise bursts.
"""
import os
import sys
import wave

import numpy as np

RATE = 44100
ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
SOUNDS_DIR = os.path.join(ROOT, "common", "src", "main", "resources", "assets", "femboymod", "sounds")
PREVIEW = os.path.join(ROOT, "build", "sounds")


def t(seconds):
    return np.arange(int(seconds * RATE)) / RATE


def env(n, attack=0.005, release=0.05):
    """Linear attack, exponential release, in seconds."""
    a = max(1, int(attack * RATE))
    e = np.ones(n)
    e[:a] = np.linspace(0, 1, a)
    e *= np.exp(-np.arange(n) / max(1, release * RATE))
    return e


def square(freq, seconds, harmonics=7):
    """Band-limited square (odd harmonics), softer than a raw square."""
    x = t(seconds)
    out = np.zeros_like(x)
    for k in range(1, harmonics + 1, 2):
        out += np.sin(2 * np.pi * freq * k * x) / k
    return out * (4 / np.pi) * 0.5


def crush(x, bits=5, hold=3):
    """Bit crush and sample-and-hold: the 'digital' grain."""
    steps = 2 ** bits
    y = np.round(x * steps / 2) / (steps / 2)
    if hold > 1:
        y = np.repeat(y[::hold], hold)[:len(x)]
    return y


def lowpass(x, cutoff):
    alpha = 1 - np.exp(-2 * np.pi * cutoff / RATE)
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def click(rng, freq=None, seconds=0.008):
    """One insect click: a tiny decaying ping with a breath of noise."""
    freq = freq or rng.uniform(4200, 6400)
    x = t(seconds)
    ping = np.sin(2 * np.pi * freq * x) * np.exp(-x / (seconds / 4))
    noise = rng.normal(0, 0.3, len(x)) * np.exp(-x / 0.0008)
    return ping + noise


def place(buffer, sound, at, gain=1.0):
    start = int(at * RATE)
    end = min(len(buffer), start + len(sound))
    buffer[start:end] += sound[:end - start] * gain


def chitter(rng, buffer, start, count, gain):
    at = start
    for i in range(count):
        swell = np.sin(np.pi * (i + 0.5) / count)
        place(buffer, click(rng), at, gain * (0.4 + 0.6 * swell))
        at += rng.uniform(0.022, 0.038)
    return at


def blip(freqs, note=0.055, bits=5, hold=3):
    parts = []
    for f in freqs:
        tone = square(f, note) * env(int(note * RATE), 0.003, note / 2.5)
        parts.append(crush(tone, bits, hold))
    return np.concatenate(parts)


def normalize(x, peak):
    x = lowpass(x, 9000)
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def ambient(seed):
    rng = np.random.default_rng(seed)
    out = np.zeros(int(0.75 * RATE))
    end = chitter(rng, out, 0.0, int(rng.integers(7, 12)), 0.6)
    notes = rng.choice([880, 990, 1175, 1320, 1568, 1760], size=int(rng.integers(2, 4)), replace=False)
    place(out, blip(notes), end + 0.02, 0.5)
    return normalize(out, 0.32)


def hurt(seed):
    rng = np.random.default_rng(seed)
    high = rng.choice([784, 880, 988])
    error = blip([high, high / 2], note=0.07, bits=4, hold=4)
    stutter = np.concatenate([error[:int(0.02 * RATE)]] * 2 + [error])  # the "e-e-error" stammer
    out = np.zeros(int(0.32 * RATE))
    place(out, stutter, 0.0, 0.8)
    chitter(rng, out, 0.0, 4, 0.5)
    return normalize(out, 0.5)


def death(seed):
    rng = np.random.default_rng(seed)
    notes = [1760, 1397, 1175, 988, 784, 659, 523, 392, 262]
    out = np.zeros(int(1.0 * RATE))
    at = 0.0
    for i, f in enumerate(notes):
        bits = max(3, 6 - i // 2)  # it falls apart as it goes
        note = blip([f], note=0.045, bits=bits, hold=3 + i // 3)
        repeats = 2 if rng.random() < 0.35 else 1
        for _ in range(repeats):
            place(out, note, at, 0.7)
            at += 0.045
    pop_len = int(0.12 * RATE)
    pop = rng.normal(0, 1, pop_len) * np.exp(-np.arange(pop_len) / (0.02 * RATE))
    place(out, lowpass(pop, 2500), at, 0.6)
    return normalize(out, 0.5)


def step(seed):
    rng = np.random.default_rng(seed)
    out = np.zeros(int(0.05 * RATE))
    place(out, click(rng, rng.uniform(2800, 3800), 0.012), 0.0, 1.0)
    return normalize(out, 0.22)


def pat(seed):
    """Two soft pats (low, muffled thumps) and a small high sparkle on top."""
    rng = np.random.default_rng(seed)
    out = np.zeros(int(0.45 * RATE))
    for i, at in enumerate((0.0, rng.uniform(0.13, 0.17))):
        n = int(0.06 * RATE)
        thump = rng.normal(0, 1, n) * np.exp(-np.arange(n) / (0.012 * RATE))
        thump = lowpass(thump, 700) + np.sin(2 * np.pi * rng.uniform(150, 190) * t(0.06)) * np.exp(-t(0.06) / 0.02) * 0.6
        place(out, thump, at, 1.0 - 0.15 * i)
    for k, f in enumerate(rng.choice([1568, 1760, 2093, 2349], size=2, replace=False)):
        ping = np.sin(2 * np.pi * f * t(0.25)) * env(int(0.25 * RATE), 0.004, 0.06)
        place(out, ping, 0.2 + 0.05 * k, 0.12)
    return normalize(out, 0.45)


SOUNDS = {
    "mob/bug/ambient1": lambda: ambient(1), "mob/bug/ambient2": lambda: ambient(2), "mob/bug/ambient3": lambda: ambient(3),
    "mob/bug/hurt1": lambda: hurt(4), "mob/bug/hurt2": lambda: hurt(5),
    "mob/bug/death": lambda: death(6),
    "mob/bug/step1": lambda: step(7), "mob/bug/step2": lambda: step(8), "mob/bug/step3": lambda: step(9),
    "social/headpat1": lambda: pat(10), "social/headpat2": lambda: pat(11), "social/headpat3": lambda: pat(12),
}


def write_wav(path, x):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = (np.clip(x, -1, 1) * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(data.tobytes())


def main():
    wav_only = "--wav-only" in sys.argv
    sf = None
    if not wav_only:
        import soundfile as sf  # noqa: F811 (only needed for OGG)
    for name, make in SOUNDS.items():
        x = make()
        write_wav(os.path.join(PREVIEW, name + ".wav"), x)
        if sf is not None:
            path = os.path.join(SOUNDS_DIR, name + ".ogg")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            sf.write(path, x.astype(np.float32), RATE, format="OGG", subtype="VORBIS")
    print("sounds written" + (" (WAV previews only)" if wav_only else ""))


if __name__ == "__main__":
    main()
