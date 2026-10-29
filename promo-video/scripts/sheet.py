"""Contact sheets for the review loop: one labelled frame per timestamp, tiled 4x2.

usage: python scripts/sheet.py <video.mp4> <out_prefix> [t1 t2 ...]
Default timestamps = 0.3 s after each scene beat (0, 3, 7, 11, 15 s) plus mid-scene holds.
"""
import os
import subprocess
import sys

from PIL import Image, ImageDraw

video, prefix = sys.argv[1], sys.argv[2]
times = [float(t) for t in sys.argv[3:]] or [0.0, 1.0, 2.3, 3.3, 4.6, 5.8, 6.6, 7.3, 8.6, 9.6, 10.6, 11.3, 12.4, 13.6, 14.6, 15.3, 16.2, 17.3]
tmp = prefix + "_tmp.png"
tiles = []
for t in times:
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-ss", f"{t:.3f}", "-i", video, "-frames:v", "1", tmp], check=True)
    im = Image.open(tmp).convert("RGB")
    im.thumbnail((640, 640))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 78, 26], fill=(0, 0, 0))
    d.text((6, 6), f"{t:.2f}s", fill=(255, 255, 0))
    tiles.append(im)
os.remove(tmp)
w, h = tiles[0].size
for n in range(0, len(tiles), 8):
    chunk = tiles[n : n + 8]
    sheet = Image.new("RGB", (w * 4 + 15, h * 2 + 5), (255, 255, 255))
    for i, im in enumerate(chunk):
        sheet.paste(im, ((i % 4) * (w + 5), (i // 4) * (h + 5)))
    path = f"{prefix}-{n // 8 + 1}.png"
    sheet.save(path)
    print(path)
