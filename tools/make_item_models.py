#!/usr/bin/env python3
"""为所有物品贴图补齐 item 模型 JSON（v2.0.0）。

datagen 的 `ModItemModelProvider` 只覆盖显式列出的物品；本脚本负责把
**所有存在于 textures/item 下的贴图**都补上一个 `item/generated` 模型，
避免出现「贴图有了但模型没加」导致物品显示为紫黑格。

用法：python tools/make_item_models.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

ASSETS = Path("src/main/resources/assets/echoesofoblivion")
TEX_DIR = ASSETS / "textures/item"
MODEL_DIR = ASSETS / "models/item"

# 这些物品的模型由 datagen 负责（BlockItem 的模型由方块 provider 生成）
DATAGEN_OWNED = {"memory_crystal"}


def main() -> int:
    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    created, existing = [], []
    for png in sorted(TEX_DIR.glob("*.png")):
        name = png.stem
        if name in DATAGEN_OWNED:
            continue
        target = MODEL_DIR / f"{name}.json"
        if target.exists():
            existing.append(name)
            continue
        model = {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"echoesofoblivion:item/{name}"},
        }
        target.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        created.append(name)

    print(f"新建模型 {len(created)} 个")
    for n in created:
        print(f"  + {n}.json")
    print(f"已存在 {len(existing)} 个（跳过）")
    print(f"models/item 下共 {len(list(MODEL_DIR.glob('*.json')))} 个模型")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
