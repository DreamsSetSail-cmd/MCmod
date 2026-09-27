#!/usr/bin/env python3
"""一次性脚本：写入 v2.0.0 的仪式与记忆焚烧文案。

`en_us` / `zh_cn` 写真实文本；其余语言先写英文回退，随后由翻译任务补齐。

用法：python tools/add_ritual_keys.py
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

# (完整键, 英文, 中文, 是否含占位符说明)
KEYS = [
    # ---- 音叉本体 ----
    ("item.echoesofoblivion.resonance_fork.selected", "Selected rite: %s", "当前仪式：%s"),
    ("item.echoesofoblivion.resonance_fork.hint",
     "Right-click to perform. Sneak-right-click to change rite.", "右键执行；潜行右键切换仪式。"),
    ("item.echoesofoblivion.resonance_fork.costs",
     "Each rite consumes materials.", "每种仪式都会消耗材料。"),
    ("item.echoesofoblivion.resonance_fork.missing",
     "Materials for %s are missing.", "缺少%s所需的材料。"),
    ("item.echoesofoblivion.resonance_fork.failed", "Nothing answered.", "没有回应。"),

    # ---- 三种仪式 ----
    ("ritual.echoesofoblivion.requiem", "Requiem", "安魂曲"),
    ("ritual.echoesofoblivion.requiem.desc",
     "Scatters every echo within 32 blocks. Costs 1 Ossuary Ash.",
     "驱散 32 格内所有回响。消耗骸骨灰 ×1。"),
    ("ritual.echoesofoblivion.requiem.done",
     "%s echoes were returned to silence.", "%s 个回响归入寂静。"),

    ("ritual.echoesofoblivion.descent", "Descent", "寂静降临"),
    ("ritual.echoesofoblivion.descent.desc",
     "Silences the world within 48 blocks for 30 seconds. Costs 1 Silence Shard and 2 Void Embers.",
     "让 48 格内的世界静音 30 秒。消耗寂静碎片 ×1 与虚空余烬 ×2。"),
    ("ritual.echoesofoblivion.descent.done",
     "You have done what they did. Listen to nothing.", "你做了他们做过的事。听，什么都没有。"),
    ("ritual.echoesofoblivion.descent.ended",
     "Sound returns, reluctantly.", "声音不情愿地回来了。"),

    ("ritual.echoesofoblivion.void_passage", "Void Passage", "虚空通道"),
    ("ritual.echoesofoblivion.void_passage.desc",
     "Cross between worlds without a portal. Costs 1 Mirror Shard and 10 corruption.",
     "不需传送门跨越两界。消耗镜面碎片 ×1 与 10 点侵蚀值。"),
    ("ritual.echoesofoblivion.void_passage.done",
     "The crossing costs you %s corruption.", "这次跨越让你付出 %s 点侵蚀。"),

    # ---- 记忆焚烧 ----
    ("message.echoesofoblivion.burn.not_witnessed",
     "You never witnessed this memory. There is nothing to burn.",
     "你从未见证过这段记忆。没有东西可以焚烧。"),
    ("message.echoesofoblivion.burn.done",
     "%s burns away. The memory is gone; %s",
     "%s 焚烧殆尽。这段记忆消失了；%s"),
    ("message.echoesofoblivion.burn.effect.purge",
     "every echo nearby is scattered.", "附近的回响尽数消散。"),
    ("message.echoesofoblivion.burn.effect.purge_none",
     "nothing nearby was listening.", "附近没有东西在听。"),
    ("message.echoesofoblivion.burn.effect.silence",
     "the echoes nearby stop, but do not leave.", "附近的回响停下了，但没有离开。"),
    ("message.echoesofoblivion.burn.effect.silence_none",
     "nothing nearby was moving.", "附近没有东西在动。"),
    ("message.echoesofoblivion.burn.effect.warning",
     "the infection around you recedes.", "你周围的感染退去了。"),
    ("message.echoesofoblivion.burn.effect.warning_none",
     "there was no infection here to push back.",
     "这里没有可以被推回去的感染。"),
    ("message.echoesofoblivion.burn.hint",
     "Sneak and press the memory attack key to burn it.",
     "潜行 + 按下记忆攻击键即可焚烧。"),
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
        for key, en, zh in KEYS:
            if key not in data:
                data[key] = zh if locale == "zh_cn" else en
                added += 1
        if added:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
        print(f"  {locale:<7} +{added:2d} 键   共 {len(data)} 键")

    print(f"\n完成：{len(files)} 个文件，每份新增 {len(KEYS)} 个键")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
