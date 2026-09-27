#!/usr/bin/env python3
"""语言文件校验器。

以 ``en_us.json`` 为基准，检查每个语言文件：

1. JSON 是否合法（UTF-8）
2. 键集合是否与基准**完全一致**（不缺、不多）
3. 占位符 ``%s`` 与 ``%%`` 的出现次数是否与基准一致
4. 记忆正文里的字面量换行转义 ``\\n`` 数量是否一致
   （少了它幻境屏幕就不会分句）
5. 共鸣标记 ``◈`` 是否存在

用法::

    python tools/check_lang.py

退出码 0 表示全部通过，1 表示存在问题。
"""

from __future__ import annotations

import glob
import json
import os
import sys
from pathlib import Path

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")
BASELINE = "en_us.json"
PLACEHOLDERS = ("%s", "%%")
NEWLINE_ESCAPE = "\\n"  # 字面两字符：反斜杠 + n


def count(haystack: str, needle: str) -> int:
    return haystack.count(needle)


def main() -> int:
    baseline_path = LANG_DIR / BASELINE
    if not baseline_path.exists():
        print(f"找不到基准文件: {baseline_path}")
        return 1

    baseline = json.loads(baseline_path.read_text(encoding="utf-8"))
    ref_keys = list(baseline.keys())
    print(f"基准 {BASELINE}: {len(ref_keys)} 个键\n")

    problems: list[tuple[str, str]] = []
    files = sorted(glob.glob(str(LANG_DIR / "*.json")))

    for path in files:
        name = os.path.basename(path)
        try:
            data = json.loads(Path(path).read_text(encoding="utf-8"))
        except Exception as exc:  # noqa: BLE001 - 报告任意解析失败
            problems.append((name, f"JSON 非法: {exc}"))
            continue

        missing = [k for k in ref_keys if k not in data]
        extra = [k for k in data if k not in ref_keys]
        if missing or extra:
            detail = []
            if missing:
                detail.append(f"缺 {len(missing)} 个: {missing[:3]}")
            if extra:
                detail.append(f"多 {len(extra)} 个: {extra[:3]}")
            problems.append((name, "; ".join(detail)))
            continue

        mismatch = None
        for key in ref_keys:
            ref_val = str(baseline[key])
            val = str(data[key])

            for token in PLACEHOLDERS:
                if count(ref_val, token) != count(val, token):
                    mismatch = (f"{key}: {token} 数量不符 "
                                f"({count(ref_val, token)} vs {count(val, token)})")
                    break
            if mismatch:
                break

            if key.endswith(".content"):
                if count(ref_val, NEWLINE_ESCAPE) != count(val, NEWLINE_ESCAPE):
                    mismatch = (f"{key}: 换行转义 {NEWLINE_ESCAPE} 数量不符 "
                                f"({count(ref_val, NEWLINE_ESCAPE)} vs "
                                f"{count(val, NEWLINE_ESCAPE)})")
                    break

            if "◈" in ref_val and "◈" not in val:
                mismatch = f"{key}: 缺少共鸣标记 ◈"
                break

        if mismatch:
            problems.append((name, mismatch))
        else:
            print(f"  OK   {name:<14} {len(data)} 键")

    print()
    if problems:
        print(f"发现 {len(problems)} 个问题：")
        for name, msg in problems:
            print(f"  {name}: {msg}")
        return 1

    print(f"全部 {len(files)} 个语言文件通过校验。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
