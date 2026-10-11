#!/usr/bin/env python3
"""一次性脚本：写入 v2.1.0 的 2 个语言键。

v2.1.0 的主题是「介质在腐坏」：
- 错位的回响（走廊里重放你听过的声音，但音高错、方向反、有延迟）
- 废墟的布局语法（能认出曾经的秩序）

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾按文件保留**（CRLF 与 LF 两种都存在，见 `check_lang_format.py`），
因此这里做文本层插入而不是 `json.dumps` 整体重写。

用法：python tools/add_v210_keys.py
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
    # 首次听到错位回响时的一次性提示。
    # 刻意只说「它回来了，但不对」——不解释、不安抚、不加感叹号。
    # 玩家自己会得出「介质坏了」这个结论，那比被告知有效得多。
    ("message.echoesofoblivion.misaligned_echo",
     "Something you heard before comes back wrong.",
     "你听过的某个东西回来了，但不对。"),
    # 配置界面里的说明。玩家菜单里的条目，语气可以直白一些。
    ("message.echoesofoblivion.misaligned_echo.hint",
     "The corridor does not keep sound well.",
     "走廊留不住声音。"),
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
