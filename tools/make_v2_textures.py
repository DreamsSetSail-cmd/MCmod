#!/usr/bin/env python3
"""Echoes of Oblivion —— 新物品贴图生成器（v2.0.0）。

`make_textures.py` 覆盖 v1.x 的 9 张贴图（手绘字符网格）。
本脚本覆盖 v2.0.0 新增的 29 件物品，**全部用几何图元绘制**——
这是 v1.x 得出的经验：16x16 下手绘网格精度不够，对称图形必须用图元生成。

用法::

    python tools/make_v2_textures.py

输出到 ``src/main/resources/assets/echoesofoblivion/textures/item/``。

设计基调与 v1.x 一致：暗色、冷色、自发光、近黑描边。
每件物品都有一条明确的主色，方便玩家在物品栏里快速识别。
"""

from __future__ import annotations

import struct
import sys
import zlib
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

TRANSPARENT = (0, 0, 0, 0)


def hexc(value: str, alpha: int = 255) -> tuple[int, int, int, int]:
    v = value.lstrip("#")
    return (int(v[0:2], 16), int(v[2:4], 16), int(v[4:6], 16), alpha)


def write_png(path: Path, pixels: list[list[tuple[int, int, int, int]]]) -> None:
    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag: bytes, data: bytes) -> bytes:
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
           + chunk(b"IEND", b""))
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


class Canvas:
    """16x16 画布，几何图元绘制。"""

    def __init__(self, size: int = 16):
        self.size = size
        self.px = [[TRANSPARENT for _ in range(size)] for _ in range(size)]

    def set(self, x: int, y: int, color) -> None:
        if 0 <= x < self.size and 0 <= y < self.size:
            self.px[y][x] = color

    def get(self, x: int, y: int):
        if 0 <= x < self.size and 0 <= y < self.size:
            return self.px[y][x]
        return TRANSPARENT

    def rect(self, x0, y0, x1, y1, color) -> None:
        for y in range(max(0, int(y0)), min(self.size - 1, int(y1)) + 1):
            for x in range(max(0, int(x0)), min(self.size - 1, int(x1)) + 1):
                self.set(x, y, color)

    def polygon(self, points, color) -> None:
        if len(points) < 3:
            return
        ys = [p[1] for p in points]
        for y in range(max(0, int(min(ys))), min(self.size - 1, int(max(ys)) + 1) + 1):
            scan = y + 0.5
            xs = []
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
                for x in range(max(0, int(xs[i] + 0.5)), min(self.size - 1, int(xs[i + 1] - 0.5)) + 1):
                    self.set(x, y, color)

    def circle(self, cx, cy, r, color) -> None:
        for y in range(self.size):
            for x in range(self.size):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                    self.set(x, y, color)

    def ring(self, cx, cy, outer, inner, color) -> None:
        for y in range(self.size):
            for x in range(self.size):
                d2 = (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2
                if inner * inner <= d2 <= outer * outer:
                    self.set(x, y, color)

    def line(self, x0, y0, x1, y1, color, thick: int = 1) -> None:
        """整数 Bresenham 直线，thick 为额外加粗的宽度。"""
        dx, dy = abs(x1 - x0), abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx - dy
        x, y = x0, y0
        while True:
            for ox in range(thick):
                for oy in range(thick):
                    self.set(x + ox, y + oy, color)
            if x == x1 and y == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x += sx
            if e2 < dx:
                err += dx
                y += sy

    def outline(self, color) -> None:
        """给不透明区域补一圈描边。"""
        adds = []
        for y in range(self.size):
            for x in range(self.size):
                if self.px[y][x][3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    if self.get(x + dx, y + dy)[3] > 128:
                        adds.append((x, y))
                        break
        for x, y in adds:
            self.set(x, y, color)

    def glow(self, color_hex: str, radius: int = 1, strength: int = 90) -> None:
        base = hexc(color_hex)
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
                a = round(strength * max(0.0, 1.0 - (best - 1.0) / max(1.0, radius)))
                if a > 0:
                    self.set(x, y, (base[0], base[1], base[2], a))

    def shade_vertical(self, top_delta: int = 18, bottom_delta: int = -20) -> None:
        for y in range(self.size):
            t = y / (self.size - 1)
            delta = round(top_delta + (bottom_delta - top_delta) * t)
            for x in range(self.size):
                r, g, b, a = self.px[y][x]
                if a == 0:
                    continue
                self.px[y][x] = (max(0, min(255, r + delta)),
                                 max(0, min(255, g + delta)),
                                 max(0, min(255, b + delta)), a)


# --------------------------------------------------------------------------- 调色

OUTLINE = hexc("0E0A14")
STEEL_D = hexc("4A4A5E")
STEEL = hexc("8A8AB0")
STEEL_L = hexc("D0D0E8")
WOOD_D = hexc("4A3420")
WOOD = hexc("8A6A3C")
PARCH_D = hexc("9A8A60")
PARCH = hexc("E0D0A8")
PURPLE_D = hexc("4A2A6A")
PURPLE = hexc("8B5CF6")
PURPLE_L = hexc("C8A8FF")
CYAN_D = hexc("1E5A6E")
CYAN = hexc("4FC3E8")
CYAN_L = hexc("A8ECFF")
RED_D = hexc("5A1620")
RED = hexc("A8303E")
RED_L = hexc("E06A78")
GOLD_D = hexc("6A4A10")
GOLD = hexc("C89A30")
GOLD_L = hexc("F0D070")
BONE_D = hexc("8A8470")
BONE = hexc("D8D0B8")
BONE_L = hexc("F0EAD8")
GREY_D = hexc("3A3A48")
GREY = hexc("6A6A80")
GREY_L = hexc("A0A0B8")


# --------------------------------------------------------------------------- 物品

def ruins_compass() -> Canvas:
    """遗迹罗盘：黄铜圆盘 + 指针，指针指向一个不在正北的方向。"""
    c = Canvas()
    c.ring(8, 8, 7.2, 5.6, OUTLINE)
    c.ring(8, 8, 6.6, 5.8, GOLD_D)
    c.circle(8, 8, 5.6, GOLD)
    c.ring(8, 8, 5.6, 4.6, GOLD_L)
    # 指针：从中心斜向西北，暗示「北不是答案」
    c.line(8, 8, 5, 4, RED, 1)
    c.line(8, 8, 11, 12, STEEL_L, 1)
    c.circle(8, 8, 1.2, OUTLINE)
    c.set(8, 8, RED_L)
    c.shade_vertical(12, -14)
    return c


def excavation_shovel() -> Canvas:
    """探查铲：木柄 + 窄刃，刃口偏亮表示经常使用。"""
    c = Canvas()
    # 柄
    c.line(5, 12, 10, 4, WOOD_D, 2)
    c.line(5, 12, 10, 5, WOOD, 1)
    # 刃
    c.polygon([(9, 5), (12, 2), (14, 4), (11, 7)], OUTLINE)
    c.polygon([(10, 5), (12, 3), (13, 4), (11, 6)], STEEL)
    c.line(11, 4, 12, 3, STEEL_L, 1)
    # 握把
    c.rect(3, 12, 6, 14, OUTLINE)
    c.rect(4, 12, 6, 13, WOOD)
    c.shade_vertical(14, -16)
    return c


def resonance_fork() -> Canvas:
    """共鸣音叉：U 形双叉 + 柄，叉尖有青色共振。"""
    c = Canvas()
    # 双叉
    c.line(5, 2, 5, 7, STEEL, 1)
    c.line(10, 2, 10, 7, STEEL, 1)
    c.line(4, 2, 4, 7, STEEL_D, 1)
    c.line(11, 2, 11, 7, STEEL_D, 1)
    # 底部连接
    c.rect(4, 7, 11, 8, OUTLINE)
    c.rect(5, 7, 10, 8, STEEL)
    # 柄
    c.rect(7, 9, 8, 14, OUTLINE)
    c.rect(7, 9, 8, 14, WOOD)
    c.set(7, 9, STEEL_L)
    # 叉尖共振
    c.set(5, 1, CYAN_L)
    c.set(10, 1, CYAN_L)
    c.set(4, 1, CYAN)
    c.set(11, 1, CYAN)
    c.glow("4FC3E8", 1, 80)
    c.shade_vertical(10, -12)
    return c


def ritual_alloy() -> Canvas:
    """仪式合金：锭状，表面有刻痕。"""
    c = Canvas()
    c.polygon([(3, 10), (6, 6), (13, 6), (10, 10)], OUTLINE)
    c.polygon([(4, 9), (6, 7), (12, 7), (10, 9)], PURPLE_D)
    c.polygon([(5, 8), (6, 7), (12, 7), (11, 8)], PURPLE)
    c.rect(7, 11, 12, 12, OUTLINE)
    c.rect(7, 11, 11, 12, PURPLE_D)
    # 刻痕
    c.set(8, 7, PURPLE_L)
    c.set(10, 7, PURPLE_L)
    c.glow("8B5CF6", 1, 70)
    c.shade_vertical(12, -14)
    return c


def silence_shard() -> Canvas:
    """寂静碎片：一块**中空**的晶体——它的特征是什么都没有。"""
    c = Canvas()
    c.polygon([(8, 1), (13, 7), (11, 14), (5, 14), (3, 7)], OUTLINE)
    c.polygon([(8, 3), (12, 7), (10, 12), (6, 12), (4, 7)], hexc("2A2438"))
    # 中心空腔：全黑，但边缘有极细的亮线，让「空」是被看见的
    c.polygon([(8, 6), (10, 8), (9, 11), (7, 11), (6, 8)], hexc("050308"))
    c.ring(8, 8.5, 3.4, 2.6, hexc("6A5A8A"))
    c.line(6, 5, 8, 3, STEEL_L, 1)
    c.shade_vertical(10, -12)
    return c


def purification_agent() -> Canvas:
    """净化剂：小瓶 + 淡蓝液体 + 软木塞。"""
    c = Canvas()
    # 瓶身
    c.rect(5, 5, 10, 14, OUTLINE)
    c.rect(5, 5, 10, 14, hexc("D8E4F0", 90))
    # 液体
    c.rect(6, 8, 9, 13, hexc("9ADCFF"))
    c.rect(6, 12, 9, 13, hexc("5AB8E0"))
    # 瓶颈与塞
    c.rect(7, 3, 8, 5, OUTLINE)
    c.rect(7, 3, 8, 4, WOOD)
    # 高光
    c.rect(6, 6, 6, 11, hexc("FFFFFF", 120))
    c.glow("9ADCFF", 1, 70)
    return c


def stabilizer() -> Canvas:
    """稳定剂：深色注射器，内容物为浑浊灰色。"""
    c = Canvas()
    # 筒身
    c.rect(6, 2, 9, 11, OUTLINE)
    c.rect(6, 2, 9, 11, hexc("C8CCD8", 100))
    # 药液
    c.rect(7, 4, 8, 10, GREY)
    # 推杆
    c.rect(7, 0, 8, 2, STEEL_D)
    c.rect(6, 0, 9, 1, STEEL)
    # 针
    c.rect(7, 12, 8, 15, STEEL_L)
    c.glow("A0A0B8", 1, 60)
    return c


def memory_vial() -> Canvas:
    """记忆之瓶：圆底瓶，装着紫色发光的记忆。"""
    c = Canvas()
    c.circle(8, 9, 5.4, OUTLINE)
    c.circle(8, 9, 4.6, hexc("D8CCE8", 100))
    c.circle(8, 10, 3.6, PURPLE_D)
    c.circle(8, 10, 2.6, PURPLE)
    c.circle(8, 9, 1.2, PURPLE_L)
    c.rect(7, 2, 8, 5, OUTLINE)
    c.rect(7, 2, 8, 3, WOOD)
    c.glow("8B5CF6", 1, 100)
    return c


def crystal_dust() -> Canvas:
    """水晶粉末：一小堆紫色砂粒。"""
    c = Canvas()
    c.polygon([(3, 13), (8, 9), (13, 13)], OUTLINE)
    c.polygon([(4, 12), (8, 10), (12, 12)], PURPLE_D)
    for x, y in ((6, 11), (9, 11), (7, 12), (10, 12), (5, 12)):
        c.set(x, y, PURPLE)
    c.set(8, 11, PURPLE_L)
    c.set(11, 11, PURPLE_L)
    c.glow("8B5CF6", 1, 60)
    return c


def resonant_alloy() -> Canvas:
    """共振合金：比仪式合金更亮的锭，带青色共振纹。"""
    c = Canvas()
    c.polygon([(3, 10), (6, 6), (13, 6), (10, 10)], OUTLINE)
    c.polygon([(4, 9), (6, 7), (12, 7), (10, 9)], hexc("2A5A6A"))
    c.polygon([(5, 8), (6, 7), (12, 7), (11, 8)], CYAN_D)
    c.rect(7, 11, 12, 12, OUTLINE)
    c.rect(7, 11, 11, 12, CYAN_D)
    # 共振纹
    c.line(6, 8, 11, 8, CYAN, 1)
    c.set(8, 7, CYAN_L)
    c.set(10, 7, CYAN_L)
    c.glow("4FC3E8", 1, 80)
    c.shade_vertical(12, -14)
    return c


def corrupted_fragment() -> Canvas:
    """腐蚀残块：不规则的暗红块，边缘在碎裂。"""
    c = Canvas()
    c.polygon([(4, 5), (11, 4), (13, 10), (8, 13), (3, 10)], OUTLINE)
    c.polygon([(5, 6), (10, 5), (12, 10), (8, 12), (4, 10)], RED_D)
    c.polygon([(6, 7), (9, 6), (11, 10), (8, 11), (5, 9)], RED)
    c.set(8, 7, RED_L)
    c.set(7, 8, RED_L)
    # 碎裂的边缘
    c.set(2, 9, RED_D)
    c.set(13, 6, RED_D)
    c.glow("A8303E", 1, 70)
    return c


def mirror_shard() -> Canvas:
    """镜面碎片：一块反光的薄片，上面映着一个不属于这里的东西。"""
    c = Canvas()
    c.polygon([(8, 1), (14, 8), (8, 15), (2, 8)], OUTLINE)
    c.polygon([(8, 3), (12, 8), (8, 13), (4, 8)], hexc("2A3448"))
    c.polygon([(8, 4), (11, 8), (8, 12), (5, 8)], hexc("5A6A88"))
    # 高光斜线
    c.line(6, 6, 8, 4, hexc("E8F0FF"), 1)
    c.line(7, 11, 10, 8, hexc("A8B8D8"), 1)
    # 倒影：一个极小的、深色的人形
    c.rect(8, 8, 8, 10, hexc("0A0E18"))
    c.set(8, 7, hexc("0A0E18"))
    c.glow("8A9AC8", 1, 60)
    return c


def membrane() -> Canvas:
    """薄膜：半透明的组织片，用于仪式包裹。"""
    c = Canvas()
    c.polygon([(2, 6), (10, 2), (14, 7), (9, 13), (3, 12)], hexc("6A7A6A", 140))
    c.polygon([(4, 7), (10, 4), (12, 8), (8, 11), (5, 10)], hexc("9AAAA0", 110))
    c.polygon([(6, 8), (9, 6), (10, 8), (8, 10)], hexc("C8D8CC", 90))
    # 脉络
    c.line(3, 9, 9, 5, hexc("5A6A5A", 170), 1)
    c.line(6, 11, 12, 7, hexc("5A6A5A", 170), 1)
    return c


def archivist_ink() -> Canvas:
    """档案墨：深色墨水瓶，瓶口有干涸的墨迹。"""
    c = Canvas()
    # 瓶身（方形）
    c.rect(4, 6, 11, 14, OUTLINE)
    c.rect(5, 7, 10, 13, hexc("1A1428"))
    c.rect(5, 7, 10, 9, hexc("2A2040"))
    # 瓶颈
    c.rect(6, 3, 9, 6, OUTLINE)
    c.rect(7, 3, 8, 5, hexc("1A1428"))
    # 干涸墨迹
    c.set(6, 6, hexc("4A3A6A"))
    c.set(9, 6, hexc("4A3A6A"))
    # 标签
    c.rect(6, 10, 9, 12, PARCH_D)
    c.set(7, 11, OUTLINE)
    c.set(8, 11, OUTLINE)
    return c


def ossuary_ash() -> Canvas:
    """骸骨灰：一小堆灰白粉末，混着碎骨。"""
    c = Canvas()
    c.polygon([(2, 13), (7, 9), (14, 13)], OUTLINE)
    c.polygon([(3, 12), (7, 10), (13, 12)], BONE_D)
    c.polygon([(5, 11), (8, 10), (11, 11)], BONE)
    # 碎骨
    c.rect(6, 9, 7, 10, BONE_L)
    c.rect(10, 10, 11, 11, BONE_L)
    c.set(9, 9, BONE_L)
    c.glow("D8D0B8", 1, 50)
    return c


def thread_of_choir() -> Canvas:
    """合唱之线：一卷极细的丝线，颜色在灰与青之间游移。"""
    c = Canvas()
    # 线轴
    c.rect(4, 4, 11, 12, OUTLINE)
    c.rect(5, 5, 10, 11, hexc("3A4A5A"))
    # 缠绕的线
    for y in range(5, 12, 2):
        c.line(5, y, 10, y, hexc("8AAAB8"), 1)
    for y in range(6, 12, 2):
        c.line(5, y, 10, y, hexc("C8D8E0"), 1)
    # 抽出的线头
    c.line(10, 6, 14, 2, hexc("C8D8E0"), 1)
    c.set(14, 1, CYAN_L)
    c.glow("8AAAB8", 1, 60)
    return c


def blank_plaque() -> Canvas:
    """空白铭牌：一块什么都没有写的碑片。"""
    c = Canvas()
    c.rect(3, 2, 12, 14, OUTLINE)
    c.rect(4, 3, 11, 13, hexc("6A6A78"))
    c.rect(4, 3, 11, 6, hexc("8A8A98"))
    # 刻意留白：中间什么都没有。
    # 只在左下角有一小道像是刻刀滑过的痕迹，暗示「本来要写点什么」。
    c.set(5, 11, hexc("4A4A56"))
    c.set(6, 11, hexc("4A4A56"))
    c.set(7, 12, hexc("4A4A56"))
    c.shade_vertical(10, -12)
    return c


def sealed_fragment() -> Canvas:
    """被封住的残片：纸卷外裹着封印蜡。"""
    c = Canvas()
    # 卷起的纸
    c.rect(3, 5, 12, 12, OUTLINE)
    c.rect(4, 6, 11, 11, PARCH_D)
    c.rect(4, 6, 11, 8, PARCH)
    # 束带
    c.rect(7, 5, 8, 12, hexc("3A2A1A"))
    # 蜡封
    c.circle(8, 9, 2.4, OUTLINE)
    c.circle(8, 9, 1.6, RED)
    c.set(8, 9, RED_L)
    # 封印上的符号：一个被划掉的圆
    c.set(8, 8, OUTLINE)
    return c


def memory_blade() -> Canvas:
    """记忆之刃：短剑，刃身有紫色晶纹，柄为暗色。"""
    c = Canvas()
    # 刃
    c.polygon([(12, 1), (14, 1), (7, 9), (5, 8)], OUTLINE)
    c.polygon([(12, 2), (13, 2), (7, 8), (6, 8)], STEEL_L)
    c.polygon([(11, 3), (12, 3), (7, 8), (6, 8)], STEEL)
    # 护手
    c.rect(4, 8, 9, 9, OUTLINE)
    c.rect(5, 8, 8, 9, GOLD_D)
    # 柄
    c.polygon([(4, 9), (6, 11), (4, 14), (2, 12)], OUTLINE)
    c.polygon([(4, 10), (5, 11), (4, 13), (3, 12)], PURPLE_D)
    # 晶纹
    c.set(9, 5, PURPLE)
    c.set(8, 6, PURPLE)
    c.set(7, 7, PURPLE_L)
    c.glow("8B5CF6", 1, 70)
    return c


def shatter_staff() -> Canvas:
    """碎晶法杖：长杖，顶端嵌一块会碎的晶体。"""
    c = Canvas()
    # 杖身
    c.line(4, 14, 10, 6, WOOD_D, 2)
    c.line(4, 14, 10, 7, WOOD, 1)
    # 顶端晶体
    c.polygon([(10, 1), (13, 5), (10, 8), (7, 5)], OUTLINE)
    c.polygon([(10, 2), (12, 5), (10, 7), (8, 5)], CYAN_D)
    c.polygon([(10, 3), (11, 5), (10, 6), (9, 5)], CYAN_L)
    # 裂纹
    c.set(10, 4, OUTLINE)
    c.set(9, 5, OUTLINE)
    c.glow("4FC3E8", 1, 90)
    return c


def researcher_lantern() -> Canvas:
    """研究者提灯：提灯，光偏冷，照亮的是「事实」而不是路。"""
    c = Canvas()
    # 提梁
    c.ring(8, 3, 3.0, 2.2, STEEL_D)
    # 灯体
    c.rect(4, 5, 11, 13, OUTLINE)
    c.rect(5, 6, 10, 12, hexc("2A3448"))
    # 光
    c.rect(6, 7, 9, 11, hexc("A8D8F0"))
    c.rect(7, 8, 8, 10, hexc("FFFFFF"))
    # 底座
    c.rect(3, 13, 12, 14, OUTLINE)
    c.rect(4, 13, 11, 14, STEEL_D)
    c.glow("A8D8F0", 2, 80)
    return c


# --------------------------------------------------------------------------- 残片

def _scroll_body(c: Canvas, accent) -> None:
    """残片的统一外形：卷起的纸 + 类别色标签。"""
    c.rect(4, 2, 11, 14, OUTLINE)
    c.rect(5, 3, 10, 13, PARCH_D)
    c.rect(5, 3, 10, 5, PARCH)
    # 卷边
    c.rect(4, 2, 11, 2, hexc("B8A87A"))
    c.rect(4, 14, 11, 14, hexc("B8A87A"))
    # 文字线
    for y in range(6, 12, 2):
        c.line(6, y, 9, y, hexc("8A7A58"), 1)
    # 类别标签
    c.rect(11, 6, 12, 10, OUTLINE)
    c.rect(11, 7, 12, 9, accent)


def fragment_founding() -> Canvas:
    c = Canvas(); _scroll_body(c, GREY); return c


def fragment_first_bridge() -> Canvas:
    c = Canvas(); _scroll_body(c, GREY)
    # 编年类：加一道桥形符号
    c.line(6, 11, 9, 11, GREY_L, 1)
    c.set(7, 10, GREY_L); c.set(8, 10, GREY_L)
    return c


def fragment_choir_record() -> Canvas:
    c = Canvas(); _scroll_body(c, CYAN); return c


def fragment_cost_ledger() -> Canvas:
    c = Canvas(); _scroll_body(c, CYAN)
    # 记录类：加两行数字似的痕迹
    c.line(6, 8, 9, 8, CYAN_L, 1)
    c.line(6, 11, 8, 11, CYAN_L, 1)
    return c


def fragment_letter_home() -> Canvas:
    c = Canvas(); _scroll_body(c, GOLD)
    # 信件类：折角
    c.polygon([(5, 3), (7, 3), (5, 5)], hexc("C8B888"))
    return c


def fragment_her_first_note() -> Canvas:
    c = Canvas(); _scroll_body(c, GOLD)
    c.set(8, 7, GOLD_L); c.set(7, 8, GOLD_L); c.set(8, 9, GOLD_L)
    return c


def fragment_her_last_note() -> Canvas:
    c = Canvas(); _scroll_body(c, GOLD)
    # 最后一封：字迹只写了一半
    c.line(6, 8, 7, 8, GOLD_L, 1)
    c.set(8, 8, hexc("6A5A3A"))
    return c


def fragment_apparatus_spec() -> Canvas:
    c = Canvas(); _scroll_body(c, CYAN)
    # 规格图：一个带空心的圆
    c.ring(8, 9, 2.6, 1.6, CYAN_L)
    return c


def fragment_the_quiet_order() -> Canvas:
    c = Canvas(); _scroll_body(c, GREY)
    # 教令：封印式记号
    c.rect(7, 8, 9, 10, OUTLINE)
    c.set(8, 9, GREY_L)
    return c


def fragment_after() -> Canvas:
    c = Canvas(); _scroll_body(c, GREY)
    # 「之后」：整页几乎空白，只有最下面一行
    for y in range(6, 11, 2):
        c.line(6, y, 9, y, hexc("A89878"), 1)
    c.line(6, 12, 9, 12, hexc("6A5A3A"), 1)
    return c


def fragment_prayer_first() -> Canvas:
    c = Canvas(); _scroll_body(c, PURPLE_L)
    c.ring(8, 9, 2.2, 1.4, PURPLE_L)
    return c


def fragment_prayer_last() -> Canvas:
    c = Canvas(); _scroll_body(c, PURPLE_L)
    # 最后的祈祷：符号散开了
    c.set(7, 8, PURPLE_L)
    c.set(9, 9, PURPLE_L)
    c.set(8, 10, hexc("6A5A8A"))
    return c


# --------------------------------------------------------------------------- 入口

TEXTURES = {
    "ruins_compass": ruins_compass,
    "excavation_shovel": excavation_shovel,
    "resonance_fork": resonance_fork,
    "ritual_alloy": ritual_alloy,
    "silence_shard": silence_shard,
    "purification_agent": purification_agent,
    "stabilizer": stabilizer,
    "memory_vial": memory_vial,
    "crystal_dust": crystal_dust,
    "resonant_alloy": resonant_alloy,
    "corrupted_fragment": corrupted_fragment,
    "mirror_shard": mirror_shard,
    "membrane": membrane,
    "archivist_ink": archivist_ink,
    "ossuary_ash": ossuary_ash,
    "thread_of_choir": thread_of_choir,
    "blank_plaque": blank_plaque,
    "sealed_fragment": sealed_fragment,
    "memory_blade": memory_blade,
    "shatter_staff": shatter_staff,
    "researcher_lantern": researcher_lantern,
    "fragment_founding": fragment_founding,
    "fragment_first_bridge": fragment_first_bridge,
    "fragment_choir_record": fragment_choir_record,
    "fragment_cost_ledger": fragment_cost_ledger,
    "fragment_letter_home": fragment_letter_home,
    "fragment_her_first_note": fragment_her_first_note,
    "fragment_her_last_note": fragment_her_last_note,
    "fragment_apparatus_spec": fragment_apparatus_spec,
    "fragment_the_quiet_order": fragment_the_quiet_order,
    "fragment_after": fragment_after,
    "fragment_prayer_first": fragment_prayer_first,
    "fragment_prayer_last": fragment_prayer_last,
}


def main() -> int:
    root = Path("src/main/resources/assets/echoesofoblivion/textures/item")
    for name, factory in TEXTURES.items():
        canvas = factory()
        target = root / f"{name}.png"
        write_png(target, canvas.px)
        opaque = sum(1 for row in canvas.px for p in row if p[3] > 0)
        colors = len({p for row in canvas.px for p in row if p[3] > 0})
        print(f"  {name + '.png':<32} {target.stat().st_size:5d} B   不透明 {opaque:3d}   颜色 {colors:3d}")
    print(f"\n共生成 {len(TEXTURES)} 张物品贴图")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
