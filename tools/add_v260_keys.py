#!/usr/bin/env python3
"""一次性脚本：写入 v2.6.0 的 5 个语言键。

v2.6.0 的主题是「仪器」——玩家可以重建那台仪器，
在主世界造出一块永久、会扩散、不可撤回的**静默区**。

这是全篇最重的一次玩家行为：那个文明为了让思想在人与人之间架桥，
付出的代价是不断交出「安静」；他们为自救造的仪器，其成功恰恰需要清空一切声音。
现在玩家做了同一件事，而它**真的有用**。

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾按文件保留**（CRLF 与 LF 两种都存在，见 `check_lang_format.py`），
因此这里做文本层插入而不是 `json.dumps` 整体重写。

用法：python tools/add_v260_keys.py
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
    # 方块名（同时用作物品名）
    ("block.echoesofoblivion.apparatus_core", "Apparatus Core", "仪器核心"),

    # 激活成功。%s = 变安静的区块数。
    #
    # 语气：只说「安静了」，不说「你成功了」。它是一次**既成事实**，
    # 不是一个成就。数字必须出现——玩家会拿它去估算自己做了多大的事。
    ("block.echoesofoblivion.apparatus_core.activated",
     "%s chunks went quiet.",
     "%s 个区块安静了。"),

    # 第二句。这是全篇最需要克制的一句。
    #
    # 它**不解释机制**（底线二），也不评价玩家（不说「你做得对」/「你不该」）。
    # 它只指出一个事实：这件事以前发生过，而且当时也有人是**为了救命**才做的。
    # 玩家会自己接上后半句。
    ("block.echoesofoblivion.apparatus_core.done",
     "That is what they built it for.",
     "他们造它，就是为了这个。"),

    # 这片地面本来就静着（玩家在旧静默区里又放了一台）。
    # 不能说「0 个区块安静了」——那读起来像失败，而实际上什么都没发生。
    ("block.echoesofoblivion.apparatus_core.already",
     "This ground was already quiet.",
     "这片地面本来就安静着。"),

    # 静默区里的残片龛死掉时的反馈（动作栏）。
    #
    # 这是「造仪器」最直接的代价：你可能正在把还没找到的那几段历史，
    # 连同它们所在的地方一起清掉。
    # 语气：陈述，不控诉。它不说「你毁了它」。
    ("block.echoesofoblivion.fragment_niche.silenced",
     "The niche is empty. Nothing is kept here any more.",
     "龛里是空的。这里不再留存任何东西。"),
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
