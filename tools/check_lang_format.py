#!/usr/bin/env python3
"""检查语言文件的排版问题。

`check_lang.py` 只校验键集合与占位符；本脚本额外检查**格式层**的问题，
这些问题不影响功能但会让 diff 混乱：

1. 一行里出现多个键（JSON 合法，但后续任何改动都会产生巨大 diff）
2. 行尾混用（同文件里既有 CRLF 又有 LF）
3. 缺少末尾换行
4. UTF-8 BOM

用法：python tools/check_lang_format.py
"""

from __future__ import annotations

import glob
import json
import os
import re
import sys
from pathlib import Path

# Windows 控制台默认是 GBK，直接 print ✓ / ✗ 会抛 UnicodeEncodeError。
# 这里强制把标准输出切到 UTF-8；旧版 Python 没有 reconfigure 时降级为忽略错误。
try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")

# 一行里出现两个 "key": 模式，说明多个键被挤在同一行
KEY_PATTERN = re.compile(r'"[^"]+"\s*:\s*"')


def main() -> int:
    problems = 0
    files = sorted(glob.glob(str(LANG_DIR / "*.json")))

    for path in files:
        name = os.path.basename(path)
        raw_bytes = Path(path).read_bytes()
        raw = raw_bytes.decode("utf-8")

        issues: list[str] = []

        # 1. BOM
        if raw_bytes.startswith(b"\xef\xbb\xbf"):
            issues.append("含 UTF-8 BOM")

        # 2. 末尾换行
        if not raw.endswith("\n"):
            issues.append("缺少末尾换行")

        # 3. 行尾混用
        crlf = raw.count("\r\n")
        lf_only = raw.count("\n") - crlf
        if crlf and lf_only:
            issues.append(f"行尾混用（CRLF {crlf} 行 / LF {lf_only} 行）")

        # 4. 一行多个键
        lines = raw.replace("\r\n", "\n").split("\n")
        multi = [i + 1 for i, line in enumerate(lines) if len(KEY_PATTERN.findall(line)) > 1]
        if multi:
            issues.append(f"第 {multi} 行有多个键")

        # 5. JSON 合法性（复用已有校验器的前提）
        try:
            json.loads(raw)
        except Exception as exc:  # noqa: BLE001
            issues.append(f"JSON 非法: {exc}")

        if issues:
            problems += 1
            print(f"  ✗ {name:<12} " + "; ".join(issues))
        else:
            print(f"  ✓ {name:<12} 格式正常（{len(lines) - 1} 行）")

    print()
    if problems:
        print(f"{problems} 个文件存在格式问题")
        return 1
    print(f"全部 {len(files)} 个语言文件格式正常。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
