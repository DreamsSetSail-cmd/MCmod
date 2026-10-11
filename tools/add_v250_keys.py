#!/usr/bin/env python3
"""一次性脚本：写入 v2.5.0 的 8 个语言键。

v2.5.0 是 v2.x 系列的终章：**「之后」**。

Boss 战原来的结局是一个**奖励**（一句「你赢了」+ 一件真理碎片），
这一版把它换成一次**结算**：走完清点，真理碎片才真正交到手上，
然后世界里多出一块**空碑**。

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾按文件保留**（CRLF 与 LF 两种都存在，见 `check_lang_format.py`），
因此这里做文本层插入而不是 `json.dumps` 整体重写。

用法：python tools/add_v250_keys.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

LANG_DIR = Path("src/main/resources/assets/eoblivion/lang")
if not LANG_DIR.exists():
    LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")

# 清点文案。
#
# 语气是全篇最克制的一组：**它不谴责、不安慰、不总结**。
# 它只是一条一条把账念出来，像一份清单。
#
# 这也是全篇唯一一次明确的「清点你的损失」——前几版都让你一点点交出东西
# 而从不强调，这一版把账摊开放在你面前。
KEYS = [
    # 清点开始。一句话，不安慰。
    ("message.echoesofoblivion.reckoning.opening",
     "The corridor is quiet. It is time to count what is missing.",
     "走廊安静了。该清点少了什么了。"),

    # 逐条列出烧掉的记忆。%s 是那段记忆的标题。
    ("message.echoesofoblivion.reckoning.burned",
     "Burned: %s",
     "已焚烧：%s"),

    # 一段都没烧过时的替代行。刻意不是「你什么都没烧」这种表扬口吻，
    # 而是**中性的记录**——「无」本身就是一条账目。
    ("message.echoesofoblivion.reckoning.burned_none",
     "Burned: none.",
     "已焚烧：无。"),

    # 逐条列出刻下的名字。**只有数量，没有名字**——
    # 因为那些名字永远不会被显示（见 NameSteleBlock 的类注释）。
    ("message.echoesofoblivion.reckoning.inscribed",
     "A name was added to the register. It is still there.",
     "名册上多了一个名字。它还在那里。"),

    # 一块碑都没刻过时的替代行。
    ("message.echoesofoblivion.reckoning.inscribed_none",
     "Added to the register: nothing.",
     "名册上新增：无。"),

    # 收尾：你**再也没有**的东西。
    # 这一句是整段清点的落点，也是最冷的一句——它不说你失去了什么，
    # 只说「没有回来」。
    ("message.echoesofoblivion.reckoning.closing",
     "Nothing that was spent comes back. Not the memories, not the names.",
     "花掉的东西不会回来。记忆不会，名字也不会。"),

    # 结算完成。这一句之后真理碎片才到手。
    ("message.echoesofoblivion.reckoning.settled",
     "The account is closed.",
     "账目结清了。"),

    # 空碑：清点之后出现的那一块。它不接受铭刻。
    ("block.echoesofoblivion.empty_plaque", "Empty Plaque", "空碑"),
    ("block.echoesofoblivion.empty_plaque.refused",
     "The stone does not take it.",
     "这块石头不接受。"),
    ("block.echoesofoblivion.empty_plaque.look",
     "There is nothing written here. There is room for something.",
     "这里什么都没有写。而位置是够的。"),
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
