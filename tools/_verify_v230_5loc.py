#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""独立校验：改动只落在 8 个键的行上，其余字节逐字节未变。"""
from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = Path(__file__).resolve().parent.parent
LANG = ROOT / "src/main/resources/assets/echoesofoblivion/lang"
BAK = ROOT / "tools/_bak_v230_5loc"

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
EXPECT_TERM = {"pt_br": "\r\n", "pt_pt": "\r\n", "pl_pl": "\n", "cs_cz": "\n", "tr_tr": "\n"}

bad = []
for loc, term in EXPECT_TERM.items():
    old_raw = (BAK / f"{loc}.json").read_bytes().decode("utf-8")
    new_b = (LANG / f"{loc}.json").read_bytes()
    new_raw = new_b.decode("utf-8")

    old_body = old_raw.split(term)[:-1]
    new_body = new_raw.split(term)[:-1]
    if len(old_body) != len(new_body):
        bad.append(f"{loc}: 行数 {len(new_body)} != {len(old_body)}")

    key_of = {}
    for i, line in enumerate(new_body):
        s = line.lstrip()
        if s.startswith('"'):
            key_of[i] = s[1:].split('"', 1)[0]
    diff = [i for i in range(len(old_body)) if old_body[i] != new_body[i]]
    diff_keys = [key_of.get(i) for i in diff]
    if sorted(diff_keys) != sorted(KEYS):
        bad.append(f"{loc}: 变动行的键 {diff_keys}")

    # 其余行逐字节相同
    same_others = all(old_body[i] == new_body[i] for i in range(len(old_body)) if i not in diff)

    b = (LANG / f"{loc}.json").read_bytes()
    bom = b.startswith(b"\xef\xbb\xbf")
    crlf = b.count(b"\r\n")
    lf = b.count(b"\n") - crlf
    trailing = b.endswith(b"\n") and not b.endswith(b"\n\n")
    multi = [i + 1 for i, line in enumerate(new_body) if line.count('": "') > 1]

    data = json.loads(new_raw)
    old_data = json.loads(old_raw)
    n = len(data)
    order_same = list(data.keys()) == list(old_data.keys())
    other_unchanged = all(data[k] == old_data[k] for k in old_data if k not in KEYS)

    bad_chars = {k: data[k] for k in KEYS if any(c in data[k] for c in ("%", "\\", "\n", "\r", '"'))}
    indent_ok = all(line in ("{", "}") or line.startswith("  ") and not line.startswith("   ") for line in new_body)

    print(f"{loc}: 键={n} 变动行数={len(diff)} 其余行逐字节一致={same_others} "
          f"其余键值一致={other_unchanged} 键序一致={order_same} BOM={bom} CRLF={crlf} LF={lf} "
          f"末尾单换行={trailing} 多键行={multi} 缩进={indent_ok} 非法字符={bad_chars}")
    if not (n == 214 and len(diff) == 8 and same_others and other_unchanged and order_same
            and not bom and trailing and not multi and indent_ok and not bad_chars
            and (crlf, lf) == ((len(new_body), 0) if term == "\r\n" else (0, len(new_body)))):
        bad.append(f"{loc}: 综合校验未通过")

    for k in KEYS:
        print(f"    {k} = {data[k]}")

print()
if bad:
    print("失败：")
    for x in bad:
        print("  " + x)
    sys.exit(1)
print("独立校验通过：5 个文件均 214 键，仅 8 行变动，其余逐字节未变。")
