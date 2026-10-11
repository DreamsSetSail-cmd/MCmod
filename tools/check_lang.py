#!/usr/bin/env python3
"""语言文件校验器。

以 ``en_us.json`` 为基准，检查每个语言文件：

1. JSON 是否合法（UTF-8）
2. 键集合是否与基准**完全一致**（不缺、不多）
3. 占位符 ``%s`` 与 ``%%`` 的出现次数是否与基准一致
4. 记忆正文里的字面量换行转义 ``\\n`` 数量是否一致
   （少了它幻境屏幕就不会分句）
5. 共鸣标记 ``◈`` 是否存在
6. **译文是否仍与英文逐字相同**（即漏译 / 未翻译的占位残留）

第 6 条是 v2.5.0 补上的。起因很实际：前面几个版本里有多次
「某个语言的某个键一直是英文回退」，而前五条**全都检查不出来**——
英文回退的键在键集合、占位符、换行、标记上都是合法的。
每次都靠事后人肉抽查才发现，代价太高。

判定方式：值与基准**逐字相同**即视为未翻译。允许白名单
（专名与同形词，见 ``ALLOWED_IDENTICAL``），因为有些键在
某些语言里本来就与英文同形（如 ``Requiem`` 在法语里就是 Requiem）。

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

# 未翻译检查的**逐字相同白名单**。
#
# 只放两类：
#   1. 专名（按设计保留英文，见 docs/translation-spec.md）。
#   2. 在多种语言里本来就与英文同形的词（如仪式名 Requiem）。
#
# 加白名单前请先确认那真的是同形，而不是漏译。
ALLOWED_IDENTICAL = {
    # 模组名：不翻译
    "itemGroup.echoesofoblivion",
    "key.categories.echoesofoblivion",
    # 专名：设计上保留英文
    "entity.echoesofoblivion.silent_aggregate",
    # 通用怪物名词：多数语言沿用英文（与各文件既有做法一致）
    "entity.echoesofoblivion.phantom",
    # 「它叫你的名字」的格式串：只由括号与标点构成，多种语言同形
    "message.echoesofoblivion.name_call",
    # 仪式名：多种语言里与英文同形
    "ritual.echoesofoblivion.requiem",
}


def count(haystack: str, needle: str) -> int:
    return haystack.count(needle)


def untranslated_keys(ref_keys: list[str], baseline: dict, data: dict) -> list[str]:
    """找出「值与英文逐字相同」的键（白名单除外）。"""
    result: list[str] = []
    for key in ref_keys:
        if key in ALLOWED_IDENTICAL:
            continue
        ref_val = baseline[key]
        if not isinstance(ref_val, str) or not ref_val.strip():
            continue
        # 只对「有实质内容」的字符串做判定：纯符号 / 纯占位符不参与
        if len(ref_val.strip()) < 2:
            continue
        if data.get(key) == ref_val:
            result.append(key)
    return result


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
            continue

        # 未翻译检查：基准文件自己跳过（它当然与自己相同）
        if name != BASELINE:
            stale = untranslated_keys(ref_keys, baseline, data)
            if stale:
                preview = ", ".join(k.split(".")[-1] for k in stale[:4])
                more = "" if len(stale) <= 4 else f" …共 {len(stale)} 个"
                problems.append(
                    (name, f"{len(stale)} 个键仍是英文（疑似漏译）: {preview}{more}"))
                continue

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
