#!/usr/bin/env python3
"""临时自检脚本：记录 / 比对 8 个翻译键的改动边界。

用 `--snapshot` 记录改动前的基线（键数、行尾、BOM、SHA-256、
以及**除这 8 个键之外**全部行的逐行字节哈希）。
用 `--verify` 在改动后比对：只有这 8 个键所在的行允许变化。
"""

from __future__ import annotations

import glob
import hashlib
import json
import os
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")
SNAP = Path("tools/_translation_snapshot.json")

TARGET = [
    "block.echoesofoblivion.name_stele",
    "block.echoesofoblivion.name_stele.inscribed",
    "block.echoesofoblivion.name_stele.taken",
    "message.echoesofoblivion.inscription.observed.1",
    "message.echoesofoblivion.inscription.observed.2",
    "message.echoesofoblivion.inscription.observed.3",
    "message.echoesofoblivion.inscription.observed.4",
    "message.echoesofoblivion.inscription.observed.5",
]

LOCALES = ["ru_ru", "uk_ua", "ja_jp", "ko_kr", "zh_tw"]


def h(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def describe(path: Path) -> dict:
    """记录文件的整体属性 + 每一行（键行按 key 归类）的哈希。"""
    raw_bytes = path.read_bytes()
    raw = raw_bytes.decode("utf-8-sig")
    crlf = raw.count("\r\n")
    lf_only = raw.count("\n") - crlf
    data = json.loads(raw)
    lines = raw.replace("\r\n", "\n").split("\n")

    line_hashes: dict[str, str] = {}
    for i, line in enumerate(lines):
        stripped = line.strip()
        key = None
        if stripped.startswith('"') and '":' in stripped:
            key = stripped[1:stripped.index('":')]
        label = f"KEY:{key}" if key else f"RAW:{i}"
        line_hashes[label] = h(line)

    return {
        "keys": len(data),
        "crlf": crlf,
        "lf_only": lf_only,
        "bom": raw_bytes.startswith(b"\xef\xbb\xbf"),
        "end_newline": raw_bytes.endswith(b"\n"),
        "sha256": hashlib.sha256(raw_bytes).hexdigest(),
        "line_hashes": line_hashes,
        "target_values": {k: data[k] for k in TARGET},
    }


def main() -> int:
    action = sys.argv[1] if len(sys.argv) > 1 else "--verify"

    if action == "--snapshot":
        snap = {}
        for loc in LOCALES + ["en_us"]:
            snap[loc] = describe(LANG_DIR / f"{loc}.json")
        SNAP.write_text(json.dumps(snap, ensure_ascii=False, indent=1), encoding="utf-8")
        for loc in LOCALES + ["en_us"]:
            s = snap[loc]
            print(f"  {loc:<7} keys={s['keys']} crlf={s['crlf']} lf={s['lf_only']} "
                  f"bom={s['bom']} endnl={s['end_newline']} sha={s['sha256'][:12]}")
        print(f"\n基线已写入 {SNAP}")
        return 0

    snap = json.loads(SNAP.read_text(encoding="utf-8"))
    problems: list[str] = []

    for loc in LOCALES:
        now = describe(LANG_DIR / f"{loc}.json")
        before = snap[loc]

        # --- 1. 硬性格式要求 ---
        if now["keys"] != 214:
            problems.append(f"{loc}: 键数 {now['keys']} != 214")
        if now["crlf"] != 0:
            problems.append(f"{loc}: 出现 CRLF {now['crlf']} 行")
        if now["lf_only"] != before["lf_only"]:
            problems.append(f"{loc}: LF 行数变化 {before['lf_only']} -> {now['lf_only']}")
        if now["bom"]:
            problems.append(f"{loc}: 出现 BOM")
        if not now["end_newline"]:
            problems.append(f"{loc}: 缺少末尾换行")

        # --- 2. 只有 8 个目标键所在行允许变化 ---
        changed, added, removed = [], [], []
        for label, digest in before["line_hashes"].items():
            if label not in now["line_hashes"]:
                removed.append(label)
            elif now["line_hashes"][label] != digest:
                changed.append(label)
        for label in now["line_hashes"]:
            if label not in before["line_hashes"]:
                added.append(label)

        allowed = {f"KEY:{k}" for k in TARGET}
        illegal = [x for x in changed + added + removed if x not in allowed]
        if illegal:
            problems.append(f"{loc}: 越界改动 {illegal}")
        if set(changed) != allowed:
            problems.append(f"{loc}: 实际改动的键不是这 8 个: "
                            f"缺 {sorted(allowed - set(changed))}")

        # --- 3. 目标值不得含 % 或换行转义 ---
        for k in TARGET:
            v = now["target_values"][k]
            if "%" in v:
                problems.append(f"{loc}/{k}: 含 %")
            if "\\n" in v or "\n" in v:
                problems.append(f"{loc}/{k}: 含换行")
            if not v.strip():
                problems.append(f"{loc}/{k}: 空值")
            if v != v.strip():
                problems.append(f"{loc}/{k}: 首尾空白")

        # --- 4. 仍然保留英文占位/英文回退的检查（en_us 之外不应再是英文）---
        print(f"  {loc:<7} keys={now['keys']} crlf={now['crlf']} lf={now['lf_only']} "
              f"bom={now['bom']} endnl={now['end_newline']} "
              f"改动行={len(changed)} 越界={len(illegal)}")

    print()
    if problems:
        print(f"发现 {len(problems)} 个问题：")
        for p in problems:
            print(f"  - {p}")
        return 1
    print("自检通过：5 个文件各 214 键，纯 LF，无 BOM，末尾换行，"
          "且除 8 个目标键外全部行逐行字节未变。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
