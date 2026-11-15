#!/usr/bin/env python3
"""为仪器核心生成方块贴图（v2.6.0）。

仪器核心是**那台仪器的心脏**：玩家把它放进世界，右键启动，
一片区域从此永久没有声音。

视觉要传达两件事：

1. **它是一件被造出来的东西**，不是自然物——所以它有对称的环、有中心孔。
2. **它现在停着**。中心孔是暗的，环是冷的。它不该发光，因为它的工作是
   让东西**停止**，而不是启动。玩家启动它之后看到的也不是光，
   而是一声闷响之后世界安静下来。

产出：
    textures/block/apparatus_core.png   16x16 方块面
    textures/item/apparatus_core.png    物品栏图标（同一张图，与其它方块物品一致）

用法：python tools/make_apparatus_core_texture.py
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

# 冷色石材：比名字碑更冷、更暗，像是被抽掉温度
STONE_D = hexc("1E1E26")
STONE = hexc("33333E")
STONE_L = hexc("4A4A5A")
# 环：暗铜色，不是发光体
RING_D = hexc("3A3020")
RING = hexc("6A5A38")
RING_L = hexc("8A7A52")
# 中心孔：全暗。它的工作是让东西停止。
VOID = hexc("0A0A0E")


def apparatus_core() -> Canvas:
    c = Canvas()

    # 1. 底：冷色石面
    c.rect(0, 0, 15, 15, STONE)

    # 2. 外框：一圈更暗的边，把它从周围的石头里切出来
    for i in range(16):
        c.set(i, 0, STONE_D)
        c.set(i, 15, STONE_D)
        c.set(0, i, STONE_D)
        c.set(15, i, STONE_D)

    # 3. 外环：对称的暗铜圆环（半径 6.2，内 4.8）
    c.ring(8, 8, 6.2, 4.8, RING)
    # 环上四处加亮点，暗示它是被装配出来的、有卡扣
    for (x, y) in ((8, 2), (8, 14), (2, 8), (14, 8)):
        c.set(x, y, RING_L)

    # 4. 内圈凹槽：比石面更暗，形成"东西嵌在里面"的深度
    c.circle(8, 8, 4.4, STONE_D)

    # 5. 中心孔：全暗，且**没有高光**——它不发光，它在吸收
    c.circle(8, 8, 2.6, VOID)
    # 只在孔口留一点极淡的边缘，证明它是个"孔"而不是一块黑
    c.ring(8, 8, 2.9, 2.6, STONE_L)

    # 6. 左上高光 / 右下阴影
    for i in range(2, 14):
        if (i + 2) % 4 == 0:
            c.set(i, 1, STONE_L)

    return c


def main() -> int:
    root = Path("src/main/resources/assets/echoesofoblivion/textures")
    canvas = apparatus_core()

    block_png = root / "block" / "apparatus_core.png"
    write_png(block_png, canvas.px)
    item_png = root / "item" / "apparatus_core.png"
    write_png(item_png, canvas.px)

    opaque = sum(1 for row in canvas.px for p in row if p[3] > 0)
    print(f"  {block_png}  ({opaque}/256 不透明像素)")
    print(f"  {item_png}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())