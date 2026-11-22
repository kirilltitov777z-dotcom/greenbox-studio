#!/usr/bin/env python3
"""Generate the GreenBox logo and all Android launcher icon densities.
Run: python3 tools/make_logo.py
Requires Pillow only.
"""
import math
import os
from PIL import Image, ImageDraw

DARK = (46, 125, 50, 255)      # #2E7D32
MINT = (200, 230, 160, 255)    # #C8E6A0
SIZE = 1024


def build_logo(size=SIZE):
    s = size / SIZE
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # rounded-square app icon background
    d.rounded_rectangle(
        [int(60 * s), int(60 * s), int(964 * s), int(964 * s)],
        radius=int(220 * s), fill=DARK)

    # wavy scalloped badge
    wave = []
    for i in range(720):
        a = math.radians(i / 2.0)
        r = 300 + 26 * math.cos(8 * a)
        wave.append((512 * s + math.cos(a) * r * s,
                     512 * s + math.sin(a) * r * s))
    d.polygon(wave, fill=MINT)

    # new symbol: camera-aperture hexagon + play triangle
    hexa = []
    for i in range(6):
        a = math.radians(-90 + 60 * i)
        hexa.append((512 * s + math.cos(a) * 168 * s,
                     512 * s + math.sin(a) * 168 * s))
    hexa.append(hexa[0])
    d.line(hexa, fill=DARK, width=int(58 * s), joint="curve")
    for p in hexa[:-1]:
        x, y = p
        rr = 29 * s
        d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=DARK)

    d.polygon([(512 * s - 50 * s, 512 * s - 64 * s),
               (512 * s - 50 * s, 512 * s + 64 * s),
               (512 * s + 88 * s, 512 * s)], fill=DARK)
    return img


def round_icon(img, px):
    small = img.resize((px, px), Image.LANCZOS)
    mask = Image.new("L", (px, px), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, px, px], fill=255)
    out = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    out.paste(small, (0, 0), mask)
    return out


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    logo = build_logo()
    logo.save(os.path.join(root, "logo.png"))
    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, px in densities.items():
        folder = os.path.join(root, "app", "src", "main", "res", "mipmap-" + name)
        os.makedirs(folder, exist_ok=True)
        logo.resize((px, px), Image.LANCZOS).save(os.path.join(folder, "ic_launcher.png"))
        round_icon(logo, px).save(os.path.join(folder, "ic_launcher_round.png"))
    print("logo.png and launcher icons generated")


if __name__ == "__main__":
    main()
