#!/usr/bin/env python3
"""一次性脚本：为 v2.3.0 的 8 个「名字碑」键写入 de_de / nl_nl / sv_se / da_dk / nb_no 译文。

做法是**文本层逐行替换**（绝不 json.dump 整体重写）：
只把匹配行的「值」部分换掉，行首缩进、键名、冒号、逗号、行尾都原样保留，
因此其余 206 个键逐字节不变。

用法::
    python tools/_apply_v230_5lang.py
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")

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

TRANSLATIONS: dict[str, dict[str, str]] = {
    # ---------------- 德语 ----------------
    "de_de": {
        "block.echoesofoblivion.name_stele": "Namensstele",
        "block.echoesofoblivion.name_stele.inscribed":
            "Der Name ist eingraviert. Niemand wird ihn lesen.",
        "block.echoesofoblivion.name_stele.taken": "Jemand ist schon hier.",
        "message.echoesofoblivion.inscription.observed.1":
            "Das Subjekt bewegt sich weiterhin zielgerichtet durch den Korridor.",
        "message.echoesofoblivion.inscription.observed.2":
            "Das Subjekt hat begonnen, eigene Aufzeichnungen zu führen. "
            "Dies wurde nicht verlangt.",
        "message.echoesofoblivion.inscription.observed.3":
            "Das Subjekt hat dem Register einen Namen hinzugefügt. "
            "Der Name wurde angenommen.",
        "message.echoesofoblivion.inscription.observed.4":
            "Das Subjekt fragt nicht mehr, wozu der Korridor dient.",
        "message.echoesofoblivion.inscription.observed.5":
            "Die Handschrift des Subjekts stimmt mit den früheren Einträgen überein.",
    },
    # ---------------- 荷兰语 ----------------
    "nl_nl": {
        "block.echoesofoblivion.name_stele": "Naamsstele",
        "block.echoesofoblivion.name_stele.inscribed":
            "De naam is ingesneden. Niemand zal hem lezen.",
        "block.echoesofoblivion.name_stele.taken": "Er is hier al iemand.",
        "message.echoesofoblivion.inscription.observed.1":
            "Het subject beweegt zich nog steeds doelgericht door de Gang.",
        "message.echoesofoblivion.inscription.observed.2":
            "Het subject is eigen optekeningen gaan bijhouden. "
            "Dit werd niet verzocht.",
        "message.echoesofoblivion.inscription.observed.3":
            "Het subject heeft een naam aan het register toegevoegd. "
            "De naam werd aanvaard.",
        "message.echoesofoblivion.inscription.observed.4":
            "Het subject vraagt niet langer waarvoor de Gang dient.",
        "message.echoesofoblivion.inscription.observed.5":
            "Het handschrift van het subject komt overeen met de eerdere aantekeningen.",
    },
    # ---------------- 瑞典语 ----------------
    "sv_se": {
        "block.echoesofoblivion.name_stele": "Namnstele",
        "block.echoesofoblivion.name_stele.inscribed":
            "Namnet är inhugget. Ingen kommer att läsa det.",
        "block.echoesofoblivion.name_stele.taken": "Någon är redan här.",
        "message.echoesofoblivion.inscription.observed.1":
            "Subjektet fortsätter att röra sig målmedvetet genom korridoren.",
        "message.echoesofoblivion.inscription.observed.2":
            "Subjektet har börjat föra egna anteckningar. Detta begärdes inte.",
        "message.echoesofoblivion.inscription.observed.3":
            "Subjektet lade till ett namn i registret. Namnet godtogs.",
        "message.echoesofoblivion.inscription.observed.4":
            "Subjektet frågar inte längre vad korridoren är till för.",
        "message.echoesofoblivion.inscription.observed.5":
            "Subjektets handstil stämmer överens med de tidigare posterna.",
    },
    # ---------------- 丹麦语 ----------------
    "da_dk": {
        "block.echoesofoblivion.name_stele": "Navnestele",
        "block.echoesofoblivion.name_stele.inscribed":
            "Navnet er indhugget. Ingen vil læse det.",
        "block.echoesofoblivion.name_stele.taken": "Der er allerede nogen her.",
        "message.echoesofoblivion.inscription.observed.1":
            "Subjektet fortsætter med at bevæge sig målrettet gennem korridoren.",
        "message.echoesofoblivion.inscription.observed.2":
            "Subjektet er begyndt at føre egne optegnelser. "
            "Dette blev ikke anmodet om.",
        "message.echoesofoblivion.inscription.observed.3":
            "Subjektet tilføjede et navn til registret. "
            "Navnet blev accepteret.",
        "message.echoesofoblivion.inscription.observed.4":
            "Subjektet spørger ikke længere, hvad korridoren er til.",
        "message.echoesofoblivion.inscription.observed.5":
            "Subjektets håndskrift stemmer overens med de tidligere poster.",
    },
    # ---------------- 挪威语（Bokmål） ----------------
    "nb_no": {
        "block.echoesofoblivion.name_stele": "Navnestele",
        "block.echoesofoblivion.name_stele.inscribed":
            "Navnet er hugget inn. Ingen vil lese det.",
        "block.echoesofoblivion.name_stele.taken": "Det er allerede noen her.",
        "message.echoesofoblivion.inscription.observed.1":
            "Subjektet fortsetter å bevege seg målrettet gjennom korridoren.",
        "message.echoesofoblivion.inscription.observed.2":
            "Subjektet har begynt å føre egne nedtegnelser. "
            "Dette ble ikke anmodet om.",
        "message.echoesofoblivion.inscription.observed.3":
            "Subjektet la til et navn i registeret. Navnet ble godtatt.",
        "message.echoesofoblivion.inscription.observed.4":
            "Subjektet spør ikke lenger hva korridoren er til.",
        "message.echoesofoblivion.inscription.observed.5":
            "Subjektets håndskrift stemmer overens med de tidligere oppføringene.",
    },
}


def line_pattern(key: str) -> re.Pattern[str]:
    """匹配一行 ``  "key": "value",``：只把值捕获为第 1 组。"""
    return re.compile(
        r'^(\s*"' + re.escape(key) + r'"\s*:\s*")([^"\\]*)("(?:,)?)$'
    )


def detect_eol(raw: str) -> str:
    crlf = raw.count("\r\n")
    lf_only = raw.count("\n") - crlf
    if crlf and lf_only:
        raise SystemExit("行尾混用，拒绝继续")
    return "\r\n" if crlf else "\n"


def apply_locale(locale: str) -> tuple[int, str, bytes, bytes]:
    path = LANG_DIR / f"{locale}.json"
    raw_bytes = path.read_bytes()
    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        raise SystemExit(f"{locale}: 含 BOM，拒绝继续")
    raw = raw_bytes.decode("utf-8")
    eol = detect_eol(raw)

    if not raw.endswith(eol):
        raise SystemExit(f"{locale}: 缺少末尾换行，拒绝继续")

    lines = raw.split(eol)
    assert lines[-1] == "", f"{locale}: split 尾部异常"

    table = TRANSLATIONS[locale]
    assert list(table) == KEYS, f"{locale}: 键表与 KEYS 不一致"

    replaced = 0
    for idx, line in enumerate(lines):
        for key in KEYS:
            pat = line_pattern(key)
            m = pat.match(line)
            if m is None:
                continue
            value = table[key]
            assert "%" not in value, f"{locale}/{key}: 译文含 %"
            assert "\\" not in value, f"{locale}/{key}: 译文含反斜杠"
            assert '"' not in value, f"{locale}/{key}: 译文含双引号"
            assert "\r" not in value and "\n" not in value
            lines[idx] = m.group(1) + value + m.group(3)
            replaced += 1

    if replaced != len(KEYS):
        raise SystemExit(f"{locale}: 只替换了 {replaced}/{len(KEYS)} 个键")

    for key in KEYS:
        hits = sum(1 for line in lines if line_pattern(key).match(line))
        if hits != 1:
            raise SystemExit(f"{locale}/{key}: 替换后命中 {hits} 行")

    new_raw = eol.join(lines)
    new_bytes = new_raw.encode("utf-8")
    path.write_bytes(new_bytes)
    return replaced, eol, raw_bytes, new_bytes


def main() -> int:
    for locale in TRANSLATIONS:
        n, eol, old, new = apply_locale(locale)
        name = "CRLF" if eol == "\r\n" else "LF"
        print(f"  {locale:<7} 已替换 {n} 个键  行尾={name}  "
              f"{len(old)} -> {len(new)} 字节")
    print("\n全部 5 个语言文件写入完成。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
