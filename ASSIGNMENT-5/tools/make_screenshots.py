#!/usr/bin/env python3
"""Render captured program output as terminal-style PNG screenshots (dark window, traffic-light dots)."""
import sys, os, textwrap
from PIL import Image, ImageDraw, ImageFont

FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
BG, BAR, FG = (30, 30, 30), (45, 45, 45), (212, 212, 212)
RED, GREEN, YELLOW, BLUE = (255, 99, 99), (110, 220, 130), (240, 200, 90), (130, 180, 255)
SIZE, PAD, LH, COLS = 15, 18, 20, 118

def colour(line):
    l = line.lower()
    if any(k in l for k in ("<-- bypass", "mismatch", "failed", "partial load", "damaged  ", "duplicate numbers    : 1", "lost")) or ("unsafe -> accepted" in l and "-> rejected" in l):
        return RED
    if any(k in l for k in ("rolled back", "-> match", " match", "rejected", "valid   ", "[ok]")) and "unsafe -> accepted" not in l:
        return GREEN
    if line.startswith("===") or line.startswith("TEST") or line.startswith("[") or line.startswith("--"):
        return BLUE
    if "skipped" in l and "->" in l:
        return YELLOW
    return FG

def render(text_lines, title, out_png):
    font = ImageFont.truetype(FONT, SIZE)
    wrapped = []
    for ln in text_lines:
        ln = ln.expandtabs(4).rstrip("\n")
        if len(ln) <= COLS:
            wrapped.append((ln, colour(ln)))
        else:
            parts = textwrap.wrap(ln, COLS, subsequent_indent="      ", drop_whitespace=False, replace_whitespace=False) or [ln[:COLS]]
            c = colour(ln)
            wrapped += [(p, c) for p in parts]
    cw = font.getlength("M")
    width = int(PAD * 2 + cw * COLS)
    top = 34
    height = int(top + PAD + LH * len(wrapped) + PAD)
    img = Image.new("RGB", (width, height), BG)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, width, top], fill=BAR)
    for i, col in enumerate(((255, 95, 86), (255, 189, 46), (39, 201, 63))):
        d.ellipse([14 + i * 20, 11, 26 + i * 20, 23], fill=col)
    tf = ImageFont.truetype(FONT, 12)
    d.text(((width - tf.getlength(title)) / 2, 10), title, font=tf, fill=(170, 170, 170))
    y = top + PAD
    for text, col in wrapped:
        d.text((PAD, y), text, font=font, fill=col)
        y += LH
    img.save(out_png)

def chunks(lines, n):
    # split on blank lines near the limit so that blocks stay together
    out, cur = [], []
    for ln in lines:
        cur.append(ln)
        if len(cur) >= n - 6 and ln.strip() == "":
            out.append(cur); cur = []
        elif len(cur) >= n:
            out.append(cur); cur = []
    if cur and any(x.strip() for x in cur):
        out.append(cur)
    return out

if __name__ == "__main__":
    src, title, prefix = sys.argv[1], sys.argv[2], sys.argv[3]
    maxl = int(sys.argv[4]) if len(sys.argv) > 4 else 42
    lines = [l for l in open(src, encoding="utf-8").read().split("\n")]
    while lines and lines[-1] == "": lines.pop()
    parts = chunks(lines, maxl)
    for i, part in enumerate(parts, 1):
        out = f"{prefix}.png" if len(parts) == 1 else f"{prefix}_part{i}.png"
        render(part, title, out)
        print(out)
