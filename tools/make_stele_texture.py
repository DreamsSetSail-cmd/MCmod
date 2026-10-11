#!/usr/bin/env python3
"""为名字碑生成方块贴图（v2.3.0）。

名字碑是一个**满方块**，外观与纪念碑里其它深板岩石材基本一致——
这是刻意的：**你不该一眼看出「这块能写字」**。

但也不能完全一样，否则玩家永远发现不了它。所以这里做的是一面
**被打磨过、且显然是空白的碑面**：比周围的砖更平整、更亮，
中间有一块略微内凹的矩形——它什么字都没有，而那个空白本身在说
「这里本来会有字」。

产出：
    textures/block/name_stele.png    16x16 方块面
    textures/item/name_stele.png     物品栏图标（同一张图，与其它方块物品一致）

用法：python tools/make_stele_texture.py
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

# 深板岩色系（与墙体石材同族，但打磨面更亮一档）
STONE_D = hexc("2E2E38")
STONE = hexc("464654")
STONE_L = hexc("585868")
# 打磨面：比周围砖亮，暗示「这块被处理过」
FACE = hexc("6A6A7E")
FACE_D = hexc("4E4E60")
# 内凹的空白矩形：比打磨面更暗，读成「凹槽」而不是「文字」
INSET = hexc("3A3A48")
INSET_D = hexc("25252E")
# 顶部一道极细的高光，给方块体积感
HILIGHT = hexc("7E7E94")


def name_stele() -> Canvas:
    c = Canvas()

    # 1. 底：深板岩砖面
    c.rect(0, 0, 15, 15, STONE)

    # 2. 砖缝：横竖各一条，与其它石作同一套砌法
    for i in range(16):
        c.set(i, 7, STONE_D)
        c.set(i, 8, STONE_D)
        c.set(7, i, STONE_D)

    # 3. 打磨过的碑面：居中 12x12，比周围亮
    c.rect(2, 2, 13, 13, FACE)
    # 左侧与底部加深，做出「面被切出来」的厚度
    for i in range(2, 14):
        c.set(2, i, FACE_D)
        c.set(i, 13, FACE_D)

    # 4. 空白的铭文区：内凹的矩形。
    #    这里**什么字都没有**——而那个空白本身就是内容。
    c.rect(4, 4, 11, 11, INSET)
    c.rect(5, 5, 10, 10, hexc("2A2A34"))
    # 只留一道极淡的、像是刻刀试过一下的痕迹，暗示「本来要写点什么」
    c.set(6, 8, INSET_D)
    c.set(7, 8, INSET_D)
    c.set(8, 8, INSET_D)

    # 5. 顶部高光
    for i in range(2, 14):
        c.set(i, 2, HILIGHT if i % 4 else FACE)

    return c


def main() -> int:
    root = Path("src/main/resources/assets/echoesofoblivion/textures")
    canvas = name_stele()

    block_png = root / "block" / "name_stele.png"
    write_png(block_png, canvas.px)

    item_png = root / "item" / "name_stele.png"
    write_png(item_png, canvas.px)

    opaque = sum(1 for row in canvas.px for p in row if p[3] > 0)
    print(f"  {block_png}  ({opaque}/256 不透明像素)")
    print(f"  {item_png}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
