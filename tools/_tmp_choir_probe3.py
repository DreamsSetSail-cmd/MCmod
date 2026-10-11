#!/usr/bin/env python3
"""临时探针 3：残片正文语域对照。用完即删。"""
from __future__ import annotations
import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

BASE = Path("src/main/resources/assets/echoesofoblivion/lang")
LOCALES = ["de_de", "fr_fr", "ja_jp", "ko_kr", "ru_ru", "uk_ua", "pl_pl", "cs_cz",
           "tr_tr", "es_es", "pt_br"]


def main() -> int:
    for loc in LOCALES:
        d = json.loads((BASE / (loc + ".json")).read_text(encoding="utf-8"))
        print(f"--- {loc}")
        for k, v in d.items():
            if k.startswith("fragment.") and k.endswith(".text"):
                print(f"    {k.split('.')[-2]:<22} {v!r}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
