"""Generate legacy raster launcher icons (API 24/25) for Recipely — design 5A.

Crossed fork & spoon in cream on a terracotta radial gradient.
Squircle -> ic_launcher.webp, circle -> ic_launcher_round.webp, per density.
"""
import os
from PIL import Image, ImageDraw

RES = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
CREAM = (251, 247, 239, 255)          # #FBF7EF
G_IN = (224, 138, 99)                  # #E08A63
G_OUT = (199, 93, 60)                  # #C75D3C

M = 512                                 # master render size
S = M / 108.0                           # 108-viewport -> px scale

DENSITIES = {                           # mipmap dir -> px size
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def radial_gradient():
    """Terracotta radial gradient matching the 5A vector background."""
    g = 256
    grad = Image.new("RGB", (g, g))
    px = grad.load()
    cx, cy, r = 0.40 * g, 0.32 * g, 0.85 * g
    for y in range(g):
        for x in range(g):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            t = min(d / r, 1.0)
            px[x, y] = (
                round(G_IN[0] + (G_OUT[0] - G_IN[0]) * t),
                round(G_IN[1] + (G_OUT[1] - G_IN[1]) * t),
                round(G_IN[2] + (G_OUT[2] - G_IN[2]) * t),
            )
    return grad.resize((M, M), Image.LANCZOS).convert("RGBA")


def rrect(d, x, y, w, h, r):
    d.rounded_rectangle([x * S, y * S, (x + w) * S, (y + h) * S], radius=r * S, fill=CREAM)


def spoon_layer():
    layer = Image.new("RGBA", (M, M), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    rrect(d, 52.3, 40, 3.4, 42, 1.7)                       # handle
    d.ellipse([46.5 * S, 25.5 * S, 61.5 * S, 46.5 * S], fill=CREAM)  # bowl
    return layer.rotate(-20, resample=Image.BICUBIC, center=(M / 2, M / 2))


def fork_layer():
    layer = Image.new("RGBA", (M, M), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    rrect(d, 52.3, 40, 3.4, 42, 1.7)                       # handle
    rrect(d, 48.5, 32, 11, 11, 2)                          # head
    for tx in (49, 52.7, 56.4):                            # tines
        rrect(d, tx, 24, 2.6, 11, 1.3)
    return layer.rotate(20, resample=Image.BICUBIC, center=(M / 2, M / 2))


def base_icon():
    img = radial_gradient()
    img.alpha_composite(spoon_layer())
    img.alpha_composite(fork_layer())
    return img


def masked(img, circle):
    mask = Image.new("L", (M, M), 0)
    d = ImageDraw.Draw(mask)
    if circle:
        d.ellipse([0, 0, M - 1, M - 1], fill=255)
    else:
        d.rounded_rectangle([0, 0, M - 1, M - 1], radius=int(0.235 * M), fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out


def main():
    base = base_icon()
    squircle = masked(base, circle=False)
    circle = masked(base, circle=True)
    for d, size in DENSITIES.items():
        folder = os.path.join(RES, d)
        os.makedirs(folder, exist_ok=True)
        squircle.resize((size, size), Image.LANCZOS).save(
            os.path.join(folder, "ic_launcher.webp"), "WEBP", lossless=True, method=6)
        circle.resize((size, size), Image.LANCZOS).save(
            os.path.join(folder, "ic_launcher_round.webp"), "WEBP", lossless=True, method=6)
        print(f"  {d}: {size}px  ic_launcher.webp + ic_launcher_round.webp")
    # 432px preview for review + 256px README asset (squircle, transparent corners)
    squircle.resize((432, 432), Image.LANCZOS).save(
        os.path.join(os.path.dirname(__file__), "..", "docs", "mockups", "icon-5A-preview.png"))
    squircle.resize((256, 256), Image.LANCZOS).save(
        os.path.join(os.path.dirname(__file__), "..", "docs", "icon.png"))
    print("Done. README asset: docs/icon.png")


if __name__ == "__main__":
    main()
