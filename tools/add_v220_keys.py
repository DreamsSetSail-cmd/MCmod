#!/usr/bin/env python3
"""一次性脚本：写入 v2.2.0 的 4 个语言键。

v2.2.0 的主题是「顺序的代价」：非线性在前几版是**结构**，这一版把它变成**后果**。

- 矛盾碎片（3 个键）：按错误顺序见证记忆时，幻境里多出互相矛盾的碎片。
- 「第一段没说完的话」不需要语言键——它在渲染层砍掉末句，
  因此在全部 21 个语言里同时成立（理由见 MemoryVisionScreen 的注释）。

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾按文件保留**（CRLF 与 LF 两种都存在，见 `check_lang_format.py`），
因此这里做文本层插入而不是 `json.dumps` 整体重写。

用法：python tools/add_v220_keys.py
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

# 矛盾碎片：语气必须与记忆正文一致（冷静、记录式），但内容与已知事实冲突。
# 关键设计：它们**不能**解释成「后来情况变了」——必须是真正的矛盾，
# 让玩家无法把它们和已有的记忆拼成一条自洽的时间线。
KEYS = [
    # 先见证 memory_4（警告）→ 再看 memory_3（最后的实验）。
    # memory_4 的核心是「说出来了，但没有人听」；这一句直接否定「说出来」这个动作。
    ("message.echoesofoblivion.contradiction.warning_before_apparatus",
     "The warning was never given.\\nNo one was told, and so no one failed to listen.",
     "那句警告从未被说出口。\\n没有人被告知，因此也没有人没有听。"),
    # 先见证 memory_4（警告）→ 再看 memory_0（陨落）。
    # 把警告放到陨落之后——时间线反了，而且反得理所当然。
    ("message.echoesofoblivion.contradiction.warning_before_fall",
     "You are reading these out of order.\\nThe warning came after. It always came after.",
     "你在倒着读。\\n警告是之后才有的。它一直都是之后才有的。"),
    # 先见证 memory_3（最后的实验）→ 再看 memory_1（最初的寂静）。
    # memory_1 是「寂静先于一切」；这一句把仪器放到寂静之前，于是因果倒置。
    ("message.echoesofoblivion.contradiction.apparatus_before_silence",
     "There was an instrument before there was anything for it to work on.\\nIt was built first. The silence came to fill it.",
     "在还没有东西可供它作用之前，仪器就已经存在了。\\n它先被造出来。寂静是后来才来填满它的。"),
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
