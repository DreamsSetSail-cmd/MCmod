#!/usr/bin/env python3
"""一次性脚本：为全部语言文件补充叙事新增键。

新增 8 个键：
- 5 个共鸣因果碎片（memory.echoesofoblivion.resonance.*）
- 3 句 Boss 台词（message.echoesofoblivion.boss.{phase1,phase2,dying}）

`en_us` 与 `zh_cn` 写入真实译文；其余语言先写入英文原文作为回退
（Minecraft 找不到翻译时会显示键名，回退到英文比显示键名好得多），
之后再由翻译任务补齐。

用法：python tools/add_narrative_keys.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")

NEW_KEYS_EN = [
    ("memory.echoesofoblivion.resonance.sky_apparatus",
     "The crack was not a wound. It was a door we opened on purpose."),
    ("memory.echoesofoblivion.resonance.silence_bridge",
     "The bridges ran both ways. We paid for every crossing with quiet."),
    ("memory.echoesofoblivion.resonance.bridge_silence",
     "We called it progress. We never asked what the medium was made of."),
    ("memory.echoesofoblivion.resonance.apparatus_warning",
     "She warned us about the Apparatus. The Apparatus edited the warning."),
    ("memory.echoesofoblivion.resonance.warning_silence",
     "She never spoke again after that. We assumed she had said enough."),
    ("message.echoesofoblivion.boss.phase1", "...we can hear you now."),
    ("message.echoesofoblivion.boss.phase2", "...please keep talking."),
    ("message.echoesofoblivion.boss.dying", "...thank you for bringing us back."),
]

NEW_KEYS_ZH = [
    ("memory.echoesofoblivion.resonance.sky_apparatus",
     "那道裂痕不是伤口。是我们有意打开的门。"),
    ("memory.echoesofoblivion.resonance.silence_bridge",
     "桥是双向的。每一次跨越，我们都用安静付账。"),
    ("memory.echoesofoblivion.resonance.bridge_silence",
     "我们称之为进步。我们从没问过，那个介质是用什么做的。"),
    ("memory.echoesofoblivion.resonance.apparatus_warning",
     "她警告我们提防那台仪器。而仪器改写了那句警告。"),
    ("memory.echoesofoblivion.resonance.warning_silence",
     "那之后她再没出过声。我们以为她已经说完了。"),
    ("message.echoesofoblivion.boss.phase1", "……我们现在能听见你了。"),
    ("message.echoesofoblivion.boss.phase2", "……请继续说。"),
    ("message.echoesofoblivion.boss.dying", "……谢谢你把我们带回来。"),
]


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    for path in files:
        locale = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))

        if locale == "zh_cn":
            additions = NEW_KEYS_ZH
        else:
            additions = NEW_KEYS_EN

        added = 0
        for key, value in additions:
            if key not in data:
                data[key] = value
                added += 1

        if added:
            path.write_text(
                json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
        print(f"  {locale:<7} +{added} 键   共 {len(data)} 键")

    print(f"\n完成：{len(files)} 个文件")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
