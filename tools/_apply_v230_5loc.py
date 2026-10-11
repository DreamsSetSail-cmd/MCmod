#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""v2.3.0 名字碑：5 个语言的 8 个键，文本层逐行替换（一次性脚本，用后删除）。

不做 json.dump 整体重写；只替换命中键的那一行，其余字节原样保留。
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

LANG_DIR = Path(__file__).resolve().parent.parent / "src/main/resources/assets/echoesofoblivion/lang"

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

T = {
    "pt_br": {
        KEYS[0]: "Estela do nome",
        KEYS[1]: "O nome est\u00e1 gravado. Ningu\u00e9m vai ler isso.",
        KEYS[2]: "Algu\u00e9m j\u00e1 est\u00e1 aqui.",
        KEYS[3]: "O indiv\u00edduo continua se movendo pelo corredor com prop\u00f3sito.",
        KEYS[4]: "O indiv\u00edduo come\u00e7ou a manter registros pr\u00f3prios. Isso n\u00e3o foi solicitado.",
        KEYS[5]: "O indiv\u00edduo acrescentou um nome ao registro. O nome foi aceito.",
        KEYS[6]: "O indiv\u00edduo n\u00e3o pergunta mais para que serve o corredor.",
        KEYS[7]: "A caligrafia do indiv\u00edduo corresponde aos registros anteriores.",
    },
    "pt_pt": {
        KEYS[0]: "Estela do nome",
        KEYS[1]: "O nome est\u00e1 gravado. Ningu\u00e9m o ler\u00e1.",
        KEYS[2]: "J\u00e1 se encontra algu\u00e9m aqui.",
        KEYS[3]: "O sujeito continua a mover-se pelo corredor com prop\u00f3sito.",
        KEYS[4]: "O sujeito come\u00e7ou a manter registos pr\u00f3prios. Isto n\u00e3o foi solicitado.",
        KEYS[5]: "O sujeito acrescentou um nome ao registo. O nome foi aceite.",
        KEYS[6]: "O sujeito j\u00e1 n\u00e3o pergunta para que serve o corredor.",
        KEYS[7]: "A caligrafia do sujeito corresponde aos registos anteriores.",
    },
    "pl_pl": {
        KEYS[0]: "Stela Imienia",
        KEYS[1]: "Imi\u0119 zosta\u0142o wyryte. Nikt go nie odczyta.",
        KEYS[2]: "Kto\u015b ju\u017c tu jest.",
        KEYS[3]: "Obiekt nadal porusza si\u0119 po korytarzu w spos\u00f3b celowy.",
        KEYS[4]: "Obiekt zacz\u0105\u0142 prowadzi\u0107 w\u0142asne zapisy. Nie zosta\u0142o to zlecone.",
        KEYS[5]: "Obiekt dopisa\u0142 imi\u0119 do rejestru. Imi\u0119 zosta\u0142o przyj\u0119te.",
        KEYS[6]: "Obiekt nie pyta ju\u017c, do czego s\u0142u\u017cy korytarz.",
        KEYS[7]: "Pismo obiektu odpowiada wcze\u015bniejszym wpisom.",
    },
    "cs_cz": {
        KEYS[0]: "Jmenn\u00e1 st\u00e9la",
        KEYS[1]: "Jm\u00e9no je vyryto. Nikdo ho nep\u0159e\u010dte.",
        KEYS[2]: "N\u011bkdo u\u017e je tady.",
        KEYS[3]: "Subjekt se nad\u00e1le pohybuje chodbou \u00fa\u010deln\u011b.",
        KEYS[4]: "Subjekt si za\u010dal v\u00e9st vlastn\u00ed z\u00e1znamy. Nebylo to vy\u017e\u00e1d\u00e1no.",
        KEYS[5]: "Subjekt p\u0159idal do rejst\u0159\u00edku jm\u00e9no. Jm\u00e9no bylo p\u0159ijato.",
        KEYS[6]: "Subjekt se ji\u017e nept\u00e1, k \u010demu chodba slou\u017e\u00ed.",
        KEYS[7]: "Rukopis subjektu odpov\u00edd\u00e1 d\u0159\u00edv\u011bj\u0161\u00edm z\u00e1pis\u016fm.",
    },
    "tr_tr": {
        KEYS[0]: "Ad Steli",
        KEYS[1]: "Ad kaz\u0131nd\u0131. Kimse onu okumayacak.",
        KEYS[2]: "Burada zaten biri var.",
        KEYS[3]: "Denek koridorda ama\u00e7l\u0131 bir \u015fekilde ilerlemeyi s\u00fcrd\u00fcr\u00fcyor.",
        KEYS[4]: "Denek kendi kay\u0131tlar\u0131n\u0131 tutmaya ba\u015flad\u0131. Bu talep edilmedi.",
        KEYS[5]: "Denek kayda bir ad ekledi. Ad kabul edildi.",
        KEYS[6]: "Denek art\u0131k koridorun ne i\u015fe yarad\u0131\u011f\u0131n\u0131 sormuyor.",
        KEYS[7]: "Dene\u011fin el yaz\u0131s\u0131 \u00f6nceki kay\u0131tlarla uyu\u015fuyor.",
    },
}

fail = []

for loc, table in T.items():
    path = LANG_DIR / f"{loc}.json"
    raw_bytes = path.read_bytes()
    raw = raw_bytes.decode("utf-8")

    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        fail.append(f"{loc}: 有 BOM")
        continue

    crlf_total = raw.count("\r\n")
    lf_total = raw.count("\n") - crlf_total
    if crlf_total and lf_total:
        fail.append(f"{loc}: 行尾混用")
        continue
    term = "\r\n" if crlf_total else "\n"
    lines = raw.split(term)
    if lines[-1] != "":
        fail.append(f"{loc}: 缺末尾换行")
        continue
    body = lines[:-1]

    before = json.loads(raw)
    before_count = len(before)

    replaced_at = {}
    for i, line in enumerate(body):
        stripped = line.lstrip()
        if not stripped.startswith('"'):
            continue
        key = stripped[1:].split('"', 1)[0]
        if key in table:
            if key in replaced_at:
                fail.append(f"{loc}: 键出现多次 {key}")
            replaced_at[key] = i

    missing = [k for k in table if k not in replaced_at]
    if missing:
        fail.append(f"{loc}: 未找到键 {missing}")
        continue
    if len(replaced_at) != len(table):
        fail.append(f"{loc}: 命中行数 {len(replaced_at)} != {len(table)}")
        continue

    for key, idx in replaced_at.items():
        old = body[idx]
        comma = "," if old.rstrip().endswith(",") else ""
        body[idx] = f'  "{key}": {json.dumps(table[key], ensure_ascii=False)}{comma}'

    new_raw = term.join(body) + term
    path.write_bytes(new_raw.encode("utf-8"))

    # ---- 校验 ----
    after = json.loads(new_raw)
    if len(after) != before_count:
        fail.append(f"{loc}: 键数 {len(after)} != {before_count}")
        continue
    if list(after.keys()) != list(before.keys()):
        fail.append(f"{loc}: 键顺序变化")
        continue
    changed = sorted(k for k in before if before[k] != after[k])
    if changed != sorted(table):
        fail.append(f"{loc}: 变化的键 {changed}")
        continue
    for k, v in table.items():
        if any(c in v for c in ("%", "\\", "\n", "\r", '"')):
            fail.append(f"{loc}/{k}: 非法字符")
    b2 = path.read_bytes()
    if b2.startswith(b"\xef\xbb\xbf") or not b2.endswith(b"\n"):
        fail.append(f"{loc}: BOM/结尾异常")
    crlf2 = b2.count(b"\r\n")
    lf2 = b2.count(b"\n") - crlf2
    if term == "\r\n" and (lf2 != 0 or crlf2 != len(body)):
        fail.append(f"{loc}: CRLF 行数异常 crlf={crlf2} lf={lf2}")
    if term == "\n" and (crlf2 != 0 or lf2 != len(body)):
        fail.append(f"{loc}: LF 行数异常 crlf={crlf2} lf={lf2}")
    for line in body:
        if line.startswith('"'):
            fail.append(f"{loc}: 顶格行 {line[:40]}")
            break
        if line != "}" and not line.startswith("  "):
            fail.append(f"{loc}: 缩进异常 {line[:40]}")
            break
    old_body = raw.split(term)[:-1]
    diff_idx = [i for i, (a, b) in enumerate(zip(old_body, body)) if a != b]
    if diff_idx != sorted(replaced_at.values()):
        fail.append(f"{loc}: 变动行 {diff_idx} 与预期不符")
    ws = "CRLF" if term == "\r\n" else "LF"
    print(f"  OK  {loc:<6} 键 {len(after)}  变动 {len(diff_idx)} 行 @ {[i + 1 for i in diff_idx]}  行尾 {ws}")

print()
if fail:
    print("失败：")
    for f in fail:
        print("  " + f)
    sys.exit(1)
print("全部 5 个文件替换并自校验通过。")
