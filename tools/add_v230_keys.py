#!/usr/bin/env python3
"""一次性脚本：写入 v2.3.0 的 8 个语言键。

v2.3.0 的主题是「名字碑」：backlog 说「名字碑必须空白」，这一版不推翻它——
它让玩家**自己决定要不要往上写**，然后承担结果。

- 碑本身仍是空白的（世界生成放下的就是空碑）。
- 玩家刻上的是**自己的名字**，而且**看不到**。
- 代价是真实的：侵蚀增长加快，并开始出现关于你的记录式短句。

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾按文件保留**（CRLF 与 LF 两种都存在，见 `check_lang_format.py`），
因此这里做文本层插入而不是 `json.dumps` 整体重写。

用法：python tools/add_v230_keys.py
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

KEYS = [
    # 物品名与两条即时反馈
    ("block.echoesofoblivion.name_stele", "Name Stele", "名字碑"),
    ("block.echoesofoblivion.name_stele.inscribed",
     "The name is cut. No one will read it.",
     "名字刻好了。没有人会读到它。"),
    ("block.echoesofoblivion.name_stele.taken",
     "Somebody is already here.",
     "已经有人在这里了。"),

    # 刻名后偶尔出现的「关于你的记述」（5 句，随机取一）。
    #
    # 语气要求：**第三人称、档案语气、说一件你做过的事**。
    # 它不是在对你说，它是在**记述你**——就像记述他们一样。
    # 因此：不用「你」，不解释、不安抚、不加感叹号。
    ("message.echoesofoblivion.inscription.observed.1",
     "The subject continues to move through the corridor with purpose.",
     "对象仍在走廊中带着目的移动。"),
    ("message.echoesofoblivion.inscription.observed.2",
     "The subject has begun keeping records of its own. This was not requested.",
     "对象已开始为自己留存记录。此举未被要求。"),
    ("message.echoesofoblivion.inscription.observed.3",
     "The subject added a name to the register. The name was accepted.",
     "对象在名册上添了一个名字。名字已被接受。"),
    ("message.echoesofoblivion.inscription.observed.4",
     "The subject no longer asks what the corridor is for.",
     "对象已不再询问走廊是用来做什么的。"),
    ("message.echoesofoblivion.inscription.observed.5",
     "The subject's handwriting matches the earlier entries.",
     "对象的笔迹与更早的条目一致。"),
]


def insert_keys(path: Path, locale: str) -> tuple[int, int]:
    raw_bytes = path.read_bytes()
    had_bom = raw_bytes.startswith(b"\xef\xbb\xbf")
    raw = raw_bytes.decode("utf-8-sig")

    use_crlf = raw.count("\r\n") > 0
    text = raw.replace("\r\n", "\n").rstrip("\n")

    data = json.loads(text)
    missing = [(k, en, zh) for k, en, zh in KEYS if k not in data]
    if not missing:
        return 0, len(data)

    lines = text.split("\n")
    close_index = max(i for i, line in enumerate(lines) if line.strip() == "}")
    body = lines[:close_index]

    last = body[-1]
    if not last.rstrip().endswith(",") and not last.rstrip().endswith("{"):
        body[-1] = last.rstrip() + ","

    for key, en, zh in missing:
        value = zh if locale == "zh_cn" else en
        body.append(f"  {json.dumps(key, ensure_ascii=False)}: "
                    f"{json.dumps(value, ensure_ascii=False)},")

    body[-1] = body[-1][:-1] if body[-1].endswith(",") else body[-1]

    lines = body + lines[close_index:]
    out = "\n".join(lines) + "\n"
    if use_crlf:
        out = out.replace("\n", "\r\n")

    encoded = out.encode("utf-8")
    if had_bom:
        encoded = b"\xef\xbb\xbf" + encoded
    path.write_bytes(encoded)

    return len(missing), len(data) + len(missing)


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    print(f"本批共 {len(KEYS)} 个键\n")
    for path in files:
        added, total = insert_keys(path, path.stem)
        print(f"  {path.stem:<7} +{added} 键   共 {total} 键")

    print(f"\n完成：{len(files)} 个文件")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())