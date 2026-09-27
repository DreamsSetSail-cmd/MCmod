#!/usr/bin/env python3
"""一次性脚本：为全部语言文件补充 v1.1.0 新增键。

新增 7 个键：
- 记忆图鉴界面（screen.echoesofoblivion.codex.*）
- 记忆卷轴的物品提示（item.echoesofoblivion.memory_scroll.desc）

`en_us` 写入英文原文；`zh_cn` 写入中文；其余语言先写入英文原文作为回退
（Minecraft 找不到翻译时显示键名，回退英文要好得多），随后由翻译任务补齐。

用法：python tools/add_codex_keys.py
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

KEYS_EN = [
    ("screen.echoesofoblivion.codex", "Memory Codex"),
    ("screen.echoesofoblivion.codex.hint", "ESC to close"),
    ("screen.echoesofoblivion.codex.no_resonance",
     "Nothing has resonated yet. Match a clue to a memory."),
    ("screen.echoesofoblivion.codex.empty_title", "the record is blank"),
    ("screen.echoesofoblivion.codex.empty_body",
     "Someone started to write it down. It did not finish."),
    ("item.echoesofoblivion.memory_scroll.desc", "A record of what you have witnessed"),
]

KEYS_ZH = [
    ("screen.echoesofoblivion.codex", "记忆图鉴"),
    ("screen.echoesofoblivion.codex.hint", "ESC 关闭"),
    ("screen.echoesofoblivion.codex.no_resonance",
     "还没有任何共鸣。把一条线索对上它所指向的记忆。"),
    ("screen.echoesofoblivion.codex.empty_title", "记录是空白的"),
    ("screen.echoesofoblivion.codex.empty_body",
     "有人开始把它写下来。没有写完。"),
    ("item.echoesofoblivion.memory_scroll.desc", "你已目睹之物的记录"),
]


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    for path in files:
        locale = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        additions = KEYS_ZH if locale == "zh_cn" else KEYS_EN

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
