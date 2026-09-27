#!/usr/bin/env python3
"""一次性脚本：为 v2.0.0 的新物品补齐合成配方。

背景：42 个注册物品里原本只有 8 个有配方，其余（含 6 件刚实现功能的工具）
在生存模式下**没有任何获取途径**。本脚本补 22 个配方。

**12 段残片刻意不给配方**——它们只能通过世界生成与探查铲获得。
给残片配方会把「考古」变成「合成」，而这段历史的价值恰恰在于它得被找到。

设计原则（与模组主题一致）：
- 不给免费的东西：所有中间材料都要么来自危险区域，要么消耗已有资源。
- 仪式合金是仪式类物品的共同基础，形成清晰的进阶阶梯。
- 记忆之瓶同时是消耗品与净化剂的原料——同一件东西既是止痛药又是解药。

用法：python tools/add_v200_recipes.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

OUT = Path("src/main/resources/data/echoesofoblivion/recipes")

M = "echoesofoblivion:"


def item(name: str) -> dict:
    return {"item": name if ":" in name else M + name}


def shaped(pattern: list[str], key: dict[str, dict], result: str, count: int = 1) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": key,
        "result": {"id": result if ":" in result else M + result, "count": count},
    }


def shapeless(ingredients: list[dict], result: str, count: int = 1) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "ingredients": ingredients,
        "result": {"id": result if ":" in result else M + result, "count": count},
    }


def smelting(ingredient: dict, result: str, xp: float) -> dict:
    return {
        "type": "minecraft:smelting",
        "ingredient": ingredient,
        "result": {"id": result if ":" in result else M + result},
        "experience": xp,
        "cookingtime": 200,
    }


RECIPES: dict[str, dict] = {
    # ============================================================ 材料层
    # 水晶粉末：把一颗记忆水晶磨成粉——不可逆地销毁一段历史，换取合成材料。
    # 这是刻意的道德成本：想造工具，就得先毁掉能读的东西。
    "crystal_dust": shapeless(
        [item("memory_crystal"), item("void_embers")], "crystal_dust", 4),

    # 骸骨灰：烧腐化精华为灰。
    "ossuary_ash": smelting(item("corruption_essence"), "ossuary_ash", 0.35),

    # 薄膜：腐化精华为核，外面裹虚空余烬。
    "membrane": shapeless([item("corruption_essence"), item("void_embers"), item("void_embers")],
                          "membrane", 2),

    # 镜面碎片：玻璃 + 回响低语 + 虚空余烬。合成的不是镜子，是「会看回来的东西」。
    "mirror_shard": shapeless([item("minecraft:glass"), item("echo_whisper"), item("void_embers")],
                              "mirror_shard"),

    # 腐化残片：真理碎片被污染后的样子。
    "corrupted_fragment": shapeless([item("shard_of_truth"), item("corruption_essence")],
                                    "corrupted_fragment"),

    # 仪式合金：一切仪式类物品的基础。用铁锭而不是模组材料，让进阶从原版资源起步。
    "ritual_alloy": shaped(
        ["SSS", "III", "SSS"],
        {"S": item("shard_of_truth"), "I": item("minecraft:iron_ingot")},
        "ritual_alloy", 2),

    # 共鸣合金：合金 + 水晶粉末，用于需要精确共鸣的部件。
    "resonant_alloy": shapeless([item("ritual_alloy"), item("ritual_alloy"), item("crystal_dust")],
                                "resonant_alloy", 2),

    # 唱诗线：薄膜 + 回响低语 + 骸骨灰。声音被纺成线。
    "thread_of_choir": shapeless([item("membrane"), item("echo_whisper"), item("ossuary_ash")],
                                 "thread_of_choir"),

    # 寂静碎片：最贵的材料。腐化精华 + 两颗水晶粉末 + 镜面碎片。
    "silence_shard": shapeless(
        [item("corruption_essence"), item("crystal_dust"), item("crystal_dust"), item("mirror_shard")],
        "silence_shard"),

    # 档案员之墨：写东西要用的墨。骸骨灰 + 虚空余烬。
    "archivist_ink": shapeless([item("ossuary_ash"), item("void_embers"), item("minecraft:glass_bottle")],
                               "archivist_ink"),

    # 空白铭牌：刻了名字也还是空白的。石头 + 墨。
    "blank_plaque": shaped(
        ["SSS", "SIS", "SSS"],
        {"S": item("minecraft:stone_bricks"), "I": item("archivist_ink")},
        "blank_plaque"),

    # 被封住的残片：腐化残片 + 薄膜，把内容封起来。
    "sealed_fragment": shapeless([item("corrupted_fragment"), item("membrane"), item("ossuary_ash")],
                                 "sealed_fragment"),

    # ============================================================ 工具与器物
    "excavation_shovel": shaped(
        [" A ", " S ", " S "],
        {"A": item("ritual_alloy"), "S": item("minecraft:stick")},
        "excavation_shovel"),

    "memory_blade": shaped(
        [" C ", " C ", " T "],
        {"C": item("corrupted_fragment"), "T": item("shard_of_truth")},
        "memory_blade"),

    "shatter_staff": shaped(
        ["  D", " A ", "T  "],
        {"D": item("crystal_dust"), "A": item("ritual_alloy"), "T": item("thread_of_choir")},
        "shatter_staff"),

    # 提灯：薄膜作芯，四周是虚空余烬。玻璃罩在中间。
    "researcher_lantern": shaped(
        [" E ", "EME", " E "],
        {"M": item("membrane"), "E": item("void_embers")},
        "researcher_lantern"),

    "ruins_compass": shaped(
        [" R ", "RCR", " R "],
        {"R": item("resonant_alloy"), "C": item("minecraft:compass")},
        "ruins_compass"),

    "resonance_fork": shaped(
        [" R ", " R ", " T "],
        {"R": item("resonant_alloy"), "T": item("shard_of_truth")},
        "resonance_fork"),

    # ============================================================ 消耗品
    # 净化剂：以记忆之瓶为核心——解药本身也是记忆做的。
    "purification_agent": shaped(
        [" M ", "VAV", " C "],
        {"M": item("membrane"), "V": item("memory_vial"),
         "A": item("ossuary_ash"), "C": item("crystal_dust")},
        "purification_agent"),

    "stabilizer": shaped(
        [" E ", "EBE", " E "],
        {"E": item("void_embers"), "B": item("minecraft:glass_bottle")},
        "stabilizer", 2),

    # 记忆之瓶：仪式合金 + 玻璃 + 真理碎片。它装的不是液体。
    "memory_vial": shaped(
        [" A ", "G G", " T "],
        {"A": item("ritual_alloy"), "G": item("minecraft:glass"),
         "T": item("shard_of_truth")},
        "memory_vial"),
}


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    written = 0
    for name, recipe in sorted(RECIPES.items()):
        path = OUT / f"{name}.json"
        path.write_text(json.dumps(recipe, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8")
        written += 1
        print(f"  {name}")

    print(f"\n写入 {written} 个配方 → {OUT}")

    # 自证：全部配方可解析，且结果物品存在
    for path in sorted(OUT.glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        assert "type" in data and "result" in data, f"{path.name} 结构异常"
    print(f"仓库现有配方总数：{len(list(OUT.glob('*.json')))}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
