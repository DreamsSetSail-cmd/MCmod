#!/usr/bin/env python3
"""为残片龛生成方块贴图与模型资源。

残片龛是 v2.0.0 新增的方块，也是 **12 段残片在世界里的唯一来源**
（残片刻意没有合成配方，理由见 `LoreFragmentBlock` 的类注释）。

它是**满方块**而不是像记忆水晶那样的小方块，所以贴图必须是 16x16 的完整面，
模型走 `minecraft:block/cube_all`。这里复用 `make_v2_textures.py` 的 `Canvas`
与 `write_png`，避免再写一份 PNG 编码器。

产出：
    textures/block/fragment_niche.png    16x16 方块面
    textures/item/fragment_niche.png     物品栏图标（同一张图，见下）

物品栏图标为什么就是方块贴图：方块物品的图标本来就该是它自己的一个面，
Minecraft 的原版方块物品（如石头）也是这么做的。这里不另画。

用法：python tools/make_niche_texture.py
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))

from make_v2_textures import Canvas, hexc, write_png  # noqa: E402

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

# 深板岩色系——龛是石头做的，与废墟的墙基同材质
STONE_D = hexc("2A2A34")
STONE = hexc("4A4A58")
STONE_L = hexc("6A6A7C")
VOID = hexc("0E0A14")
PURPLE_D = hexc("4A2A6A")
PURPLE = hexc("8B5CF6")


def fragment_niche() -> Canvas:
    """残片龛：一块深板岩砖面，中央是一个空的凹槽，槽底透出一点紫光。

    设计要点：凹槽必须**看起来是空的**。它不是箱子、不是书架，
    而是一个明显「本来是放东西的地方」——这正是叙事要传达的：
    东西已经被拿走了，很久以前。
    """
    c = Canvas()

    # 1. 砖面底：满铺深板岩色
    c.rect(0, 0, 15, 15, STONE)

    # 2. 砖缝：把面分成 4 块，暗示这是砌起来的石作
    for i in range(16):
        c.set(i, 7, STONE_D)
        c.set(i, 8, STONE_D)
        c.set(7, i, STONE_D)
    c.set(7, 7, STONE_L)
    c.set(8, 8, STONE_L)

    # 3. 凹槽：居中的 8x8 方孔，边缘用最深的颜色做「厚度」
    c.rect(4, 4, 11, 11, VOID)
    c.rect(5, 5, 10, 10, hexc("1A1420"))

    # 4. 槽底的一点紫光：说明这里曾经放过有共鸣的东西，而且余温还在
    c.set(7, 7, PURPLE_D)
    c.set(8, 7, PURPLE)
    c.set(7, 8, PURPLE)
    c.set(8, 8, PURPLE_D)

    # 5. 左上高光、右下阴影：给方块一点体积感
    for i in range(16):
        c.set(i, 0, STONE_L if i % 3 else STONE)
        c.set(0, i, STONE_L if i % 3 else STONE)
        c.set(i, 15, STONE_D)
        c.set(15, i, STONE_D)

    return c


def main() -> int:
    root = Path("src/main/resources/assets/echoesofoblivion/textures")
    canvas = fragment_niche()

    block_png = root / "block" / "fragment_niche.png"
    write_png(block_png, canvas.px)

    # 物品图标：方块物品的图标就是它的一个面，与 memory_crystal 的处理一致
    item_png = root / "item" / "fragment_niche.png"
    write_png(item_png, canvas.px)

    opaque = sum(1 for row in canvas.px for p in row if p[3] > 0)
    print(f"  {block_png}  ({opaque}/256 不透明像素)")
    print(f"  {item_png}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
