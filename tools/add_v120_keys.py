#!/usr/bin/env python3
"""一次性脚本：为全部语言文件补充 v1.2.0 新增键。

新增 1 个键：
- 「它叫你的名字」的聊天栏文本（message.echoesofoblivion.name_call）

该键含一个 `%s`，会被替换为**玩家自己的名字**。
翻译时只负责括号与标点，不要翻译占位符本身。

初始化策略：
- `en_us` 写半角 `(%s).`
- `zh_cn` 写全角 `（%s）。`
- 其余语言一律先写**半角英文形式** `(%s).` 作为回退——
  这在 16 个使用半角标点的语言里**恰好就是正确结果**，
  只有 ja_jp / ko_kr / zh_tw 需要事后改为全角 `（%s）。`
  （这一点最初被漏掉，是翻译阶段发现的）。

已存在该键的文件会被跳过，因此重复运行是安全的。

用法：python tools/add_v120_keys.py
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

KEY = "message.echoesofoblivion.name_call"

# 英文：括号 + 句号，没有任何其它内容
VALUE_EN = "(%s)."
# 中文：用全角括号与句号
VALUE_ZH = "（%s）。"


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    for path in files:
        locale = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        if KEY in data:
            print(f"  {locale:<7} 已存在，跳过")
            continue
        data[KEY] = VALUE_ZH if locale == "zh_cn" else VALUE_EN
        path.write_text(
            json.dumps(data, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"  {locale:<7} +1 键   共 {len(data)} 键")

    print(f"\n完成：{len(files)} 个文件")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
