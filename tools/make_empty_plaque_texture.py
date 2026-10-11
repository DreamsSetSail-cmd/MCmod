#!/usr/bin/env python3
"""为空碑生成方块贴图（v2.5.0）。

空碑是 v2.x 系列的**最后一个视觉元素**，也是「缺席优于出现」的最终形态：
击败聚合体、走完清点之后，世界里多出一块碑——而它是空的。

设计要点：**它必须比名字碑更空。**

- 名字碑（v2.3.0）有一块被打磨过的、明显空白的铭文区——那是「本来会有字」。
- 空碑连打磨都没有。它就是一块**还没被处理过的石头**，
  只是形状是碑。玩家会立刻看出差别：这一块**不是等待，而是没有开始**。

产出：
    textures/block/empty_plaque.png
    textures/item/empty_plaque.png     （同一张图，与其它方块物品一致）

用法：python tools/make_empty_plaque_texture.py
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

# 比名字碑更暗、更粗的一档石材：未经打磨
STONE_D = hexc("24242C")
STONE = hexc("3A3A46")
STONE_L = hexc("4A4A58")
# 只留一条极淡的、像凿子试过一下的痕——不构成任何区域
MARK = hexc("565664")


def empty_plaque() -> Canvas:
    c = Canvas()

    # 1. 整面粗糙石材，没有砖缝（名字碑有砖缝，它没有）
    c.rect(0, 0, 15, 15, STONE)

    # 2. 几条极浅的横向錾痕，表示「石头被取出来了，但没被加工」
    for y in (4, 9, 13):
        for x in range(2, 14):
            if (x + y) % 3:
                c.set(x, y, STONE_D)

    # 3. 唯一的「人迹」：一道短促的凿痕，像是有人试了一下就停了。
    #    它**不构成任何区域**——这是与名字碑最关键的区别。
    c.set(7, 6, MARK)
    c.set(8, 7, MARK)

    # 4. 左上高光 / 右下阴影：给方块体积感
    for i in range(16):
        c.set(i, 0, STONE_L if i % 4 else STONE)
        c.set(0, i, STONE_L if i % 4 else STONE)
        c.set(i, 15, STONE_D)
        c.set(15, i, STONE_D)

    return c


def main() -> int:
    root = Path("src/main/resources/assets/echoesofoblivion/textures")
    canvas = empty_plaque()

    block_png = root / "block" / "empty_plaque.png"
    write_png(block_png, canvas.px)
    item_png = root / "item" / "empty_plaque.png"
    write_png(item_png, canvas.px)

    opaque = sum(1 for row in canvas.px for p in row if p[3] > 0)
    print(f"  {block_png}  ({opaque}/256 不透明像素)")
    print(f"  {item_png}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())