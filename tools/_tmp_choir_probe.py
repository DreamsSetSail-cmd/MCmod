#!/usr/bin/env python3
"""临时探针：检查 choir 三键现状与术语环境。用完即删。"""
from __future__ import annotations
import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

BASE = Path("src/main/resources/assets/echoesofoblivion/lang")
CRLF = ["de_de", "nl_nl", "sv_se", "fr_fr", "it_it", "es_es", "es_mx", "pt_br", "pt_pt"]
LF = ["da_dk", "nb_no", "pl_pl", "cs_cz", "tr_tr", "ja_jp", "ko_kr", "ru_ru", "uk_ua"]
LOCALES = CRLF + LF

KEYS = [
    "message.echoesofoblivion.choir.bleed",
    "message.echoesofoblivion.choir.bleed.hint",
    "fragment.echoesofoblivion.choir_errata.text",
]

REPORT_KEYS = [
    "memory.echoesofoblivion.memory_2.content",
    "memory.echoesofoblivion.memory_2.clue",
    "fragment.echoesofoblivion.choir_record.text",
    "item.echoesofoblivion.memory_scroll.desc",
    "item.echoesofoblivion.memory_vial.desc",
]


def main() -> int:
    for loc in LOCALES:
        p = BASE / (loc + ".json")
        b = p.read_bytes()
        raw = b.decode("utf-8")
        c = raw.count("\r\n")
        lo = raw.count("\n") - c
        d = json.loads(raw)
        want = "CRLF" if loc in CRLF else "LF"
        has = all(k in d for k in KEYS)
        print(f"{loc:6} keys={len(d):3} crlf={c:3} lf={lo:3} "
              f"bom={b[:3] == b'\xef\xbb\xbf'} tail_nl={raw.endswith(chr(10))} "
              f"want={want} has3={has}")
        for k in KEYS:
            v = d.get(k)
            print(f"    {k.split('.')[-2:]} ns={v.count('%s')} "
                  f"bsn={v.count(chr(92) + 'n')} :: {v!r}")
    print("\n=== 术语参照 ===")
    for loc in LOCALES:
        d = json.loads((BASE / (loc + ".json")).read_text(encoding="utf-8"))
        print(f"--- {loc}")
        for k in REPORT_KEYS:
            print(f"    {k} = {d[k]!r}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
