#!/usr/bin/env python3
"""临时探针 2：术语对照。用完即删。"""
from __future__ import annotations
import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

BASE = Path("src/main/resources/assets/echoesofoblivion/lang")
LOCALES = ["de_de", "nl_nl", "sv_se", "da_dk", "nb_no", "fr_fr", "it_it", "es_es",
           "es_mx", "pt_br", "pt_pt", "pl_pl", "cs_cz", "tr_tr", "ja_jp", "ko_kr",
           "ru_ru", "uk_ua"]

KEYS = [
    "fragment.echoesofoblivion.choir_errata.title",
    "item.echoesofoblivion.fragment_choir_errata",
    "fragment.echoesofoblivion.choir_record.title",
    "fragment.echoesofoblivion.kind.log",
    "screen.echoesofoblivion.codex.empty_title",
    "message.echoesofoblivion.boss.not_collected",
    "message.echoesofoblivion.burn.not_witnessed",
    "message.echoesofoblivion.misaligned_echo.hint",
    "message.echoesofoblivion.inscription.observed.2",
    "message.echoesofoblivion.milestone.complete",
]


def main() -> int:
    for loc in LOCALES:
        d = json.loads((BASE / (loc + ".json")).read_text(encoding="utf-8"))
        print(f"--- {loc}")
        for k in KEYS:
            print(f"    {k.split('.')[-1]:<16} {d[k]!r}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
