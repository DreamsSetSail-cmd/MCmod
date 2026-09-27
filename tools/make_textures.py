#!/usr/bin/env python3
"""Echoes of Oblivion —— 贴图生成器。

用**纯 Python 标准库**（``zlib`` + ``struct``）手写 PNG，不依赖 Pillow 或任何图像库。
每个贴图都是按坐标逐像素设计的，因此风格统一、可版本控制，且**版权完全属于本项目**。

用法::

    python tools/make_textures.py

输出到 ``src/main/resources/assets/echoesofoblivion/textures/``：

- ``item/*.png``   —— 8 个物品图标（16x16，RGBA）
- ``block/memory_crystal.png`` —— 方块六面贴图（16x16，RGBA）

设计基调：暗色、冷色、带自发光；轮廓用近黑，避免在任意背景上糊掉。
"""

from __future__ import annotations

import struct
import zlib
from pathlib import Path

# --------------------------------------------------------------------------- PNG 写入


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    """把 RGBA 像素矩阵写成 PNG（colortype 6，8-bit）。

    ``pixels[y][x]`` —— 行优先，左上角为原点。
    """
    height = len(pixels)
    width = len(pixels[0])
    assert all(len(row) == width for row in pixels), "每行宽度必须一致"

    raw = bytearray()
    for row in pixels:
        raw.append(0)  # filter type 0 (None)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


# --------------------------------------------------------------------------- 调色板

# 每个调色板：名字 -> (r, g, b)
TRANSPARENT = (0, 0, 0, 0)


def hexc(value: str, alpha: int = 255) -> tuple[int, int, int, int]:
    value = value.lstrip("#")
    return (int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha)


# --------------------------------------------------------------------------- 绘制工具


class Canvas:
    """16x16 画布。所有设计坐标以左上角为 (0,0)。"""

    def __init__(self, size: int = 16):
        self.size = size
        self.px = [[TRANSPARENT for _ in range(size)] for _ in range(size)]

    def set(self, x: int, y: int, color: tuple[int, int, int, int]) -> None:
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[y][x] = color

    def get(self, x: int, y: int) -> tuple[int, int, int, int]:
        if 0 <= x < self.size and 0 <= y < self.size:
            return self.px[y][x]
        return TRANSPARENT

    def rows(self, mapping: dict[str, tuple[int, int, int, int]], rows: list[str]) -> None:
        """按字符画批量绘制。

        ``rows`` 里每个字符对应 ``mapping`` 中一个键；``.`` 表示不动（保持透明）。
        行数与列数不得超过画布尺寸。
        """
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                if ch not in mapping:
                    raise KeyError(f"字符 {ch!r} 不在调色板中")
                self.set(x, y, mapping[ch])

    def outline(self, color: tuple[int, int, int, int]) -> None:
        """给所有不透明像素的外缘补一圈描边（只填透明处）。"""
        additions = []
        for y in range(self.size):
            for x in range(self.size):
                if self.px[y][x][3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if self.get(x + dx, y + dy)[3] > 128:
                        additions.append((x, y))
                        break
        for x, y in additions:
            self.set(x, y, color)

    def glow(self, color: str, radius: int = 2, strength: int = 110) -> None:
        """在不透明像素周围加一圈柔光，alpha 随**欧氏**距离平滑衰减。

        <p>注意用欧氏距离而非切比雪夫距离：后者会让最外圈所有像素拿到同一个
        alpha，形成一圈生硬的「平环」，把贴图颜色数压到个位数。
        """
        base = hexc(color)
        for y in range(self.size):
            for x in range(self.size):
                if self.px[y][x][3] != 0:
                    continue
                best = None
                for dy in range(-radius, radius + 1):
                    for dx in range(-radius, radius + 1):
                        if dx == 0 and dy == 0:
                            continue
                        if self.get(x + dx, y + dy)[3] > 128:
                            d = (dx * dx + dy * dy) ** 0.5
                            if best is None or d < best:
                                best = d
                if best is None:
                    continue
                # 距离 1 -> strength；距离越大衰减越快
                a = round(strength * max(0.0, 1.0 - (best - 1.0) / max(1.0, radius)))
                if a > 0:
                    self.set(x, y, (base[0], base[1], base[2], a))

    def shade_vertical(self, top_delta: int = 26, bottom_delta: int = -26) -> None:
        """按 y 给不透明像素加垂直明暗，制造体积感。"""
        for y in range(self.size):
            t = y / (self.size - 1)
            delta = round(top_delta + (bottom_delta - top_delta) * t)
            for x in range(self.size):
                r, g, b, a = self.px[y][x]
                if a == 0:
                    continue
                self.px[y][x] = (
                    max(0, min(255, r + delta)),
                    max(0, min(255, g + delta)),
                    max(0, min(255, b + delta)),
                    a,
                )

    def pixels(self) -> list[list[tuple[int, int, int, int]]]:
        return self.px

    def polygon(self, points: list[tuple[float, float]],
                color: tuple[int, int, int, int]) -> None:
        """填充多边形（扫描线 + 奇偶规则）。

        传入顶点列表（可浮点），超出画布的部分自动裁剪。
        相比手写字符网格，几何图元能画出干净对称的轮廓。
        """
        if len(points) < 3:
            return
        ys = [p[1] for p in points]
        y_min = max(0, int(min(ys)))
        y_max = min(self.size - 1, int(max(ys)) + 1)
        for y in range(y_min, y_max + 1):
            scan = y + 0.5
            xs: list[float] = []
            for i in range(len(points)):
                x1, y1 = points[i]
                x2, y2 = points[(i + 1) % len(points)]
                if y1 == y2:
                    continue
                lo, hi = (y1, y2) if y1 < y2 else (y2, y1)
                if lo <= scan < hi:
                    t = (scan - y1) / (y2 - y1)
                    xs.append(x1 + t * (x2 - x1))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                x_start = max(0, int(xs[i] + 0.5))
                x_end = min(self.size - 1, int(xs[i + 1] - 0.5))
                for x in range(x_start, x_end + 1):
                    self.set(x, y, color)

    def circle(self, cx: float, cy: float, radius: float,
               color: tuple[int, int, int, int]) -> None:
        """填充圆（按像素中心距离判定）。"""
        r2 = radius * radius
        for y in range(self.size):
            for x in range(self.size):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                if dx * dx + dy * dy <= r2:
                    self.set(x, y, color)

    def ring(self, cx: float, cy: float, outer: float, inner: float,
             color: tuple[int, int, int, int]) -> None:
        """填充圆环（外半径与内半径之间）。"""
        for y in range(self.size):
            for x in range(self.size):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                d2 = dx * dx + dy * dy
                if inner * inner <= d2 <= outer * outer:
                    self.set(x, y, color)

    def rect(self, x0: int, y0: int, x1: int, y1: int,
             color: tuple[int, int, int, int]) -> None:
        """填充矩形（含边界）。"""
        for y in range(max(0, y0), min(self.size - 1, y1) + 1):
            for x in range(max(0, x0), min(self.size - 1, x1) + 1):
                self.set(x, y, color)


# --------------------------------------------------------------------------- 具体贴图


def memory_crystal() -> Canvas:
    """记忆水晶：竖向双尖锥晶体，内含亮心，外有冷紫柔光。"""
    c = Canvas()
    pal = {
        "o": hexc("1A0B26"),          # 描边
        "d": hexc("5B2E8F"),          # 暗面
        "m": hexc("8B5CF6"),          # 主色
        "l": hexc("B98CFF"),          # 亮面
        "h": hexc("E8D6FF"),          # 高光
        "c": hexc("FFF4FF"),          # 晶心
    }
    # 竖立的水晶：上尖、中宽、下尖
    c.rows(pal, [
        "................",
        ".......oo.......",
        "......omho......",
        "......omho......",
        ".....omllho.....",
        ".....omllho.....",
        "....omlcllho....",
        "....omlcllho....",
        "....omlcllho....",
        "....omlcllho....",
        ".....omllho.....",
        ".....omllho.....",
        "......omho......",
        "......omho......",
        ".......oo.......",
        "................",
    ])
    c.shade_vertical(top_delta=14, bottom_delta=-18)
    c.glow("8B5CF6", radius=2, strength=120)
    return c


def corridor_key() -> Canvas:
    """走廊之钥：八边环形匙柄 + 竖直匙杆 + 双齿，冷银带紫。

    用几何图元绘制（圆环 / 矩形 / 多边形），比字符网格更能保证对称与厚度——
    第一版用字符网格画出来匙杆只有 1 像素宽，在游戏里几乎看不见。
    """
    c = Canvas()
    outline = hexc("14121A")
    dark = hexc("6A6A82")
    mid = hexc("A0A0BC")
    light = hexc("DCDCF0")
    accent = hexc("A97BFF")

    # 匙柄：粗圆环，中心留孔
    c.ring(7.5, 4.0, 4.0, 2.0, outline)
    c.ring(7.5, 4.0, 3.2, 2.2, dark)
    # 环的左上受光、右下偏暗
    c.ring(7.5, 4.0, 3.2, 2.6, mid)
    c.rect(4, 2, 6, 3, light)

    # 匙杆：2 像素宽，从环底延伸到下方
    c.rect(6, 8, 7, 14, outline)
    c.rect(6, 8, 6, 14, mid)
    c.rect(7, 8, 7, 14, dark)

    # 双齿：向右伸出
    c.rect(8, 10, 10, 10, outline)
    c.rect(8, 10, 9, 10, mid)
    c.rect(8, 13, 10, 13, outline)
    c.rect(8, 13, 9, 13, mid)

    # 匙柄上的紫色符文点缀（呼应模组色调）
    c.rect(6, 3, 7, 3, accent)
    c.set(6, 4, accent)

    c.shade_vertical(top_delta=12, bottom_delta=-16)
    return c


def memory_scroll() -> Canvas:
    """记忆卷轴：米色卷纸 + 上下木轴 + 一抹紫色符文。"""
    c = Canvas()
    pal = {
        "o": hexc("241A12"),
        "w": hexc("D8C49A"),          # 纸暗
        "p": hexc("F2E4C0"),          # 纸亮
        "s": hexc("8A6A3C"),          # 轴木
        "t": hexc("5A4426"),          # 轴木暗
        "r": hexc("9A6BFF"),          # 符文
    }
    c.rows(pal, [
        "................",
        "...oooooooooo...",
        "...otssssssto...",
        "...otwwwwwwto...",
        "....opwwppwo....",
        "....opwrrpwo....",
        "....opwrrpwo....",
        "....opwwppwo....",
        "....opwwppwo....",
        "....opwwppwo....",
        "....opwwppwo....",
        "....opwwppwo....",
        "...otwwwwwwto...",
        "...otssssssto...",
        "...oooooooooo...",
        "................",
    ])
    c.shade_vertical(top_delta=10, bottom_delta=-14)
    return c


def echo_whisper() -> Canvas:
    """回声低语：青蓝色的声波涡旋，中心亮、边缘散。"""
    c = Canvas()
    pal = {
        "o": hexc("06283A"),
        "d": hexc("1E6E8C"),
        "m": hexc("4FC3E8"),
        "l": hexc("A8ECFF"),
        "h": hexc("EAFBFF"),
    }
    c.rows(pal, [
        "................",
        ".......oo.......",
        "......olho......",
        "....oolmmhoo....",
        "...odml..lmdo...",
        "..odm......mdo..",
        "..om...oo...mo..",
        ".odl..olho..ldo.",
        ".odl..ohho..ldo.",
        "..om...oo...mo..",
        "..odm......mdo..",
        "...odml..lmdo...",
        "....oolmmhoo....",
        "......olho......",
        ".......oo.......",
        "................",
    ])
    c.glow("4FC3E8", radius=2, strength=140)
    return c


def shard_of_truth() -> Canvas:
    """真理碎片：五边形尖锐碎片，白热核心 + 冷金边缘。

    第一版用 1 像素宽的斜条，在 16x16 下糊成一条线；改为有明确轮廓的多边形，
    内部再叠一层更亮的窄多边形作为「内芯」，读起来才像一个晶体碎片。
    """
    c = Canvas()
    outline = hexc("2A2A1A")
    edge = hexc("8A8A4E")
    body = hexc("D8D890")
    core = hexc("F8F6C0")
    hot = hexc("FFFFFF")

    # 外形：左上宽、右下收成尖
    outer = [(9.0, 1.5), (13.5, 6.5), (11.0, 14.5), (5.0, 11.5), (4.5, 5.0)]
    c.polygon(outer, outline)

    # 内缩一层作为锋面
    inner = [(9.0, 3.0), (12.0, 6.8), (10.2, 12.6), (6.2, 10.4), (5.8, 5.6)]
    c.polygon(inner, edge)

    # 受光面
    face = [(9.0, 4.0), (11.0, 7.0), (9.8, 11.4), (7.0, 9.8), (6.8, 6.2)]
    c.polygon(face, body)

    # 白热内芯
    core_shape = [(9.0, 5.2), (10.3, 7.2), (9.5, 10.0), (7.8, 8.8), (7.7, 6.8)]
    c.polygon(core_shape, core)
    c.set(9, 6, hot)
    c.set(9, 7, hot)

    c.glow("E8E8A0", radius=2, strength=110)
    return c


def void_embers() -> Canvas:
    """虚空余烬：三块相互分离的暗色炭块，各自透出橙红余火。

    第一版画到画布边缘被裁切；这一版把三块炭收进安全区，且每块独立成团，
    避免在物品栏里糊成一坨。
    """
    c = Canvas()
    outline = hexc("0A0A12")
    dark = hexc("2E2E44")
    mid = hexc("46465F")
    ember = hexc("B8411A")
    flame = hexc("FF8C3A")
    heart = hexc("FFD08A")

    def coal(cx: float, cy: float, r: float, glow_core: bool) -> None:
        c.circle(cx, cy, r, outline)
        c.circle(cx, cy, r - 1.0, dark)
        # 左上受光
        c.circle(cx - 0.7, cy - 0.7, r - 1.8, mid)
        # 火口
        c.circle(cx, cy, r - 2.2, ember)
        if glow_core:
            c.circle(cx, cy, r - 3.0, flame)
            c.set(int(cx), int(cy), heart)

    coal(5.5, 10.0, 4.0, True)    # 左下：最大，火最旺
    coal(11.0, 6.0, 3.0, False)   # 右上：中等
    coal(10.5, 12.0, 2.4, True)   # 右下：最小

    c.glow("FF8C3A", radius=1, strength=90)
    return c


def corruption_essence() -> Canvas:
    """腐蚀精粹：不规则暗红团块，表面有亮色脉络。"""
    c = Canvas()
    pal = {
        "o": hexc("1A0508"),
        "d": hexc("5A1620"),
        "m": hexc("8C2836"),
        "l": hexc("C24A5A"),
        "v": hexc("FF7A8C"),   # 脉络
    }
    c.rows(pal, [
        "................",
        "................",
        "......oooo......",
        "....oodmmmdoo...",
        "...odmmlmmlmdo..",
        "..odmmlvvmlmmdo.",
        "..odmmvmlmvmmdo.",
        ".odmmlmvvmlmmdo.",
        ".odmmvmlmvmmdo..",
        ".odmmlmvvmlmdo..",
        "..odmmvmlmmdo...",
        "..odmmlmmlmdo...",
        "...odmmmmdoo....",
        "....oodddoo.....",
        "......oooo......",
        "................",
    ])
    c.glow("C24A5A", radius=2, strength=110)
    return c


def eye_of_silence() -> Canvas:
    """寂静之眼：苍白眼球 + 竖瞳 + 紫环，直视感强。"""
    c = Canvas()
    pal = {
        "o": hexc("12081E"),
        "p": hexc("6A4AA8"),   # 紫环
        "w": hexc("C8C8E8"),   # 眼白暗
        "W": hexc("F0F0FF"),   # 眼白亮
        "i": hexc("2A1040"),   # 虹膜
        "s": hexc("08030E"),   # 竖瞳
        "S": hexc("14061E"),   # 竖瞳描边
    }
    c.rows(pal, [
        "................",
        "................",
        "....oooooooo....",
        "...oppppppppo...",
        "..opwwwwwwwwpo..",
        ".opwwWWWWWWwwpo.",
        ".opwWWiiiiWWwpo.",
        "opwWWiiSSiiWWwpo",
        "opwWWiiSSiiWWwpo",
        ".opwWWiiiiWWwpo.",
        ".opwwWWWWWWwwpo.",
        "..opwwwwwwwwpo..",
        "...oppppppppo...",
        "....oooooooo....",
        "................",
        "................",
    ])
    c.glow("6A4AA8", radius=2, strength=110)
    return c


def memory_crystal_block() -> Canvas:
    """记忆水晶方块贴图：深色岩基上嵌多枚小晶体，用于 cube_all 的六个面。"""
    c = Canvas()
    pal = {
        "o": hexc("0E0616"),
        "b": hexc("241830"),   # 岩基暗
        "B": hexc("38264A"),   # 岩基亮
        "d": hexc("5B2E8F"),
        "m": hexc("9A6BFF"),
        "l": hexc("D8B8FF"),
    }
    c.rows(pal, [
        "oooooooooooooooo",
        "obBBbbBBbbBBbbbo",
        "obBdddBBdddBBbbo",
        "obBdmlBdmlBdBbbo",
        "obBdllBdllBdBbbo",
        "obBdddBBdddBBbbo",
        "obBBbbBBbbBBbbbo",
        "obBBbbdmBBbbBbbo",
        "obBdddmlBdddbbbo",
        "obBdmlldBdmlBbbo",
        "obBdllldBdllBbbo",
        "obBddddd Bddbbbo".replace(" ", "B"),
        "obBBbbBBbbBBbbbo",
        "obBbbBBbbBBbbbbo",
        "obbBBbbBBbbBBbbo",
        "oooooooooooooooo",
    ])
    c.glow("9A6BFF", radius=2, strength=95)
    return c


# --------------------------------------------------------------------------- 入口

TEXTURES = {
    "item/memory_crystal.png": memory_crystal,
    "item/corridor_key.png": corridor_key,
    "item/memory_scroll.png": memory_scroll,
    "item/echo_whisper.png": echo_whisper,
    "item/shard_of_truth.png": shard_of_truth,
    "item/void_embers.png": void_embers,
    "item/corruption_essence.png": corruption_essence,
    "item/eye_of_silence.png": eye_of_silence,
    "block/memory_crystal.png": memory_crystal_block,
}


def main() -> int:
    root = Path("src/main/resources/assets/echoesofoblivion/textures")
    for rel, factory in TEXTURES.items():
        canvas = factory()
        target = root / rel
        write_png(target, canvas.pixels())
        opaque = sum(1 for row in canvas.pixels() for p in row if p[3] > 0)
        colors = len({p for row in canvas.pixels() for p in row if p[3] > 0})
        print(f"  {rel:34s} {target.stat().st_size:6d} B   不透明像素 {opaque:3d}   颜色 {colors:3d}")
    print(f"\n共生成 {len(TEXTURES)} 张贴图 -> {root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
