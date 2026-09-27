#!/usr/bin/env python3
"""一次性脚本：写入 v2.0.0 的物品名称与界面文案键。

`en_us` / `zh_cn` 写真实文本；其余语言先写英文回退，随后由翻译任务补齐。

命名规律：`item.echoesofoblivion.<registry_name>`，
与 `ModItems` 里的注册名一一对应。

用法：python tools/add_v200_keys.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")

# (键后缀, 英文, 中文)
ITEMS = [
    # 考古
    ("ruins_compass", "Ruins Compass", "遗迹罗盘"),
    ("excavation_shovel", "Excavation Shovel", "探查铲"),
    # 仪式
    ("resonance_fork", "Resonance Fork", "共鸣音叉"),
    ("ritual_alloy", "Ritual Alloy", "仪式合金"),
    ("silence_shard", "Silence Shard", "寂静碎片"),
    # 消耗
    ("purification_agent", "Purification Agent", "净化剂"),
    ("stabilizer", "Stabilizer", "稳定剂"),
    ("memory_vial", "Memory Vial", "记忆之瓶"),
    # 材料
    ("crystal_dust", "Crystal Dust", "水晶粉末"),
    ("resonant_alloy", "Resonant Alloy", "共振合金"),
    ("corrupted_fragment", "Corrupted Fragment", "腐蚀残块"),
    ("mirror_shard", "Mirror Shard", "镜面碎片"),
    ("membrane", "Membrane", "薄膜"),
    ("archivist_ink", "Archivist's Ink", "档案墨"),
    ("ossuary_ash", "Ossuary Ash", "骸骨灰"),
    ("thread_of_choir", "Thread of the Choir", "合唱之线"),
    ("blank_plaque", "Blank Plaque", "空白铭牌"),
    ("sealed_fragment", "Sealed Fragment", "封存的残片"),
    # 工具
    ("memory_blade", "Memory Blade", "记忆之刃"),
    ("shatter_staff", "Shatter Staff", "碎晶法杖"),
    ("researcher_lantern", "Researcher's Lantern", "研究者提灯"),
]

# (完整键, 英文, 中文)
MISC = [
    # 残片类别标签
    ("fragment.echoesofoblivion.kind.chronicle", "Chronicle", "编年"),
    ("fragment.echoesofoblivion.kind.log", "Record", "记录"),
    ("fragment.echoesofoblivion.kind.letter", "Letter", "信件"),
    ("fragment.echoesofoblivion.kind.prayer", "Prayer", "祈祷"),
    # 残片通用提示
    ("item.echoesofoblivion.lore_fragment.hint", "Right-click to read", "右键阅读"),
    ("screen.echoesofoblivion.fragment.hint", "ESC to close", "ESC 关闭"),
    # 罗盘
    ("item.echoesofoblivion.ruins_compass.desc",
     "Points toward the nearest memory crystal", "指向最近的记忆水晶"),
    ("item.echoesofoblivion.ruins_compass.none",
     "Nothing answers. There is no crystal within reach.", "没有回应。附近没有水晶。"),
    ("item.echoesofoblivion.ruins_compass.found",
     "The needle turns %s, distance %s.", "指针转向%s，距离%s。"),
    ("item.echoesofoblivion.compass.direction.front", "ahead", "正前方"),
    ("item.echoesofoblivion.compass.direction.front_right", "ahead-right", "右前方"),
    ("item.echoesofoblivion.compass.direction.right", "right", "正右方"),
    ("item.echoesofoblivion.compass.direction.back_right", "behind-right", "右后方"),
    ("item.echoesofoblivion.compass.direction.back", "behind you", "正后方"),
    ("item.echoesofoblivion.compass.direction.back_left", "behind-left", "左后方"),
    ("item.echoesofoblivion.compass.direction.left", "left", "正左方"),
    ("item.echoesofoblivion.compass.direction.front_left", "ahead-left", "左前方"),
    ("item.echoesofoblivion.compass.distance.near", "close", "很近"),
    ("item.echoesofoblivion.compass.distance.mid", "moderate", "中等"),
    ("item.echoesofoblivion.compass.distance.far", "distant", "很远"),
    # 净化剂
    ("item.echoesofoblivion.purification_agent.desc",
     "Clears the infection in a small area around you",
     "清除你周围一小片区域的感染"),
    ("item.echoesofoblivion.purification_agent.warning",
     "It does not cleanse what you have already learned.",
     "它清不掉你已经知道的东西。"),
    ("item.echoesofoblivion.purification_agent.cleansed",
     "%s chunks returned to normal.", "%s 个区块恢复正常。"),
    ("item.echoesofoblivion.purification_agent.clean",
     "Nothing here is infected.", "这里没有被感染。"),
]


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    for path in files:
        locale = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        added = 0

        for suffix, en, zh in ITEMS:
            key = f"item.echoesofoblivion.{suffix}"
            if key not in data:
                data[key] = zh if locale == "zh_cn" else en
                added += 1

        for key, en, zh in MISC:
            if key not in data:
                data[key] = zh if locale == "zh_cn" else en
                added += 1

        if added:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
        print(f"  {locale:<7} +{added:2d} 键   共 {len(data)} 键")

    total_keys = len(ITEMS) + len(MISC)
    print(f"\n完成：{len(files)} 个文件，每份新增 {total_keys} 个键")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
