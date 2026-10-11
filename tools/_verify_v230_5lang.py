#!/usr/bin/env python3
"""校验 v2.3.0 五语种改动：只动了这 8 个键，其余 206 个键逐字节不变。"""

from __future__ import annotations

import json
import sys
from pathlib import Path

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")
BACKUP = Path(sys.argv[1])

KEYS = [
    "block.echoesofoblivion.name_stele",
    "block.echoesofoblivion.name_stele.inscribed",
    "block.echoesofoblivion.name_stele.taken",
    "message.echoesofoblivion.inscription.observed.1",
    "message.echoesofoblivion.inscription.observed.2",
    "message.echoesofoblivion.inscription.observed.3",
    "message.echoesofoblivion.inscription.observed.4",
    "message.echoesofoblivion.inscription.observed.5",
]

EXPECT_EOL = {"de_de": b"\r\n", "nl_nl": b"\r\n", "sv_se": b"\r\n",
              "da_dk": b"\n", "nb_no": b"\n"}

ok = True


def check(cond: bool, msg: str) -> None:
    global ok
    print(("  OK   " if cond else "  FAIL ") + msg)
    if not cond:
        ok = False


for locale, want_eol in EXPECT_EOL.items():
    print(f"=== {locale} ===")
    old_b = (BACKUP / f"{locale}.json").read_bytes()
    new_b = (LANG_DIR / f"{locale}.json").read_bytes()

    # --- 1. BOM / 末尾换行 / 行尾纯净 ---
    check(not new_b.startswith(b"\xef\xbb\xbf"), "无 BOM")
    check(new_b.endswith(want_eol), f"末尾为单个换行 ({want_eol!r})")
    crlf = new_b.count(b"\r\n")
    lf_only = new_b.count(b"\n") - crlf
    if want_eol == b"\r\n":
        check(crlf > 0 and lf_only == 0, f"纯 CRLF (CRLF={crlf}, LF_only={lf_only})")
    else:
        check(lf_only > 0 and crlf == 0, f"纯 LF (CRLF={crlf}, LF_only={lf_only})")
    # 行尾风格与改前一致
    old_crlf = old_b.count(b"\r\n")
    old_lf = old_b.count(b"\n") - old_crlf
    check((old_crlf > 0) == (crlf > 0) and (old_lf > 0) == (lf_only > 0),
          "行尾风格与改前一致")

    old_txt = old_b.decode("utf-8")
    new_txt = new_b.decode("utf-8")

    # --- 2. 键数 214 ---
    old_d = json.loads(old_txt)
    new_d = json.loads(new_txt)
    check(len(new_d) == 214, f"键数 = {len(new_d)} (期望 214)")
    check(list(old_d.keys()) == list(new_d.keys()), "键顺序与集合完全一致")

    # --- 3. 非目标键的值逐字符（即逐字节，utf-8）不变 ---
    diff = [k for k in old_d if k not in KEYS and old_d[k] != new_d[k]]
    check(not diff, f"其余 {sum(1 for k in old_d if k not in KEYS)} 个键的值完全未变"
                    + (f" -> {diff[:3]}" if diff else ""))

    # --- 4. 目标键确实已从英文占位替换，且不外泄 % 或 \n ---
    still_en = [k for k in KEYS if new_d[k] == old_d[k]]
    check(not still_en, "8 个目标键均已改为译文" + (f" -> 未改: {still_en}" if still_en else ""))
    bad = [k for k in KEYS if "%" in new_d[k] or "\\" in new_d[k]
           or "\n" in new_d[k] or "\r" in new_d[k]]
    check(not bad, "8 个译文均不含 % / \\ / 换行" + (f" -> {bad}" if bad else ""))

    # --- 5. 逐行字节比对：只有 8 行不同，且每行都含目标键 ---
    old_lines = old_txt.replace("\r\n", "\n").rstrip("\n").split("\n")
    new_lines = new_txt.replace("\r\n", "\n").rstrip("\n").split("\n")
    # 216 行 = "{" + 214 个键 + "}"
    check(len(old_lines) == len(new_lines) == 216,
          f"行数一致且为 216 行 (old={len(old_lines)}, new={len(new_lines)})")
    if len(old_lines) == len(new_lines):
        changed = [i for i, (a, b) in enumerate(zip(old_lines, new_lines)) if a != b]
        check(len(changed) == 8, f"仅 {len(changed)} 行发生变化 (期望 8)")
        line_keys = [new_lines[i].strip().split('"')[1] for i in changed]
        check(sorted(line_keys) == sorted(KEYS), "变化的 8 行正是这 8 个键")
        # 每行仍是一行一键、2 空格缩进、键名原样
        shape_ok = all(new_lines[i].startswith('  "') for i in changed)
        check(shape_ok, "变化行仍为 2 空格缩进")
        onekey_ok = all(new_lines[i].count('": "') == 1 for i in changed)
        check(onekey_ok, "变化行仍是一行一键")
        # 未变化的行逐字节相同
        untouched_same = all(a == b for i, (a, b) in enumerate(zip(old_lines, new_lines))
                             if i not in changed)
        check(untouched_same, "未变化的 208 行逐字节完全一致（含末尾 } 与逗号布局）")
    print()

print("RESULT:", "ALL PASS" if ok else "FAILURES PRESENT")
sys.exit(0 if ok else 1)
