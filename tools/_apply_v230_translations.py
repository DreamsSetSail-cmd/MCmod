#!/usr/bin/env python3
"""一次性脚本：为 v2.3.0「名字碑」的 8 个语言键补齐 5 个语言的译文。

目标语言：ru_ru / uk_ua / ja_jp / ko_kr / zh_tw（均为 LF 行尾）。

**只改这 8 个键的值**，其余 206 个键的行在字节层原样保留：

- 文本层逐行替换，不走 `json.dump` / `json.dumps` 整体重写。
- 每行按 `\n` 切开（这 5 个文件是纯 LF），只替换整行等于
  `  "<key>": <任意>"` 形态的行，并写回**该行原有的行尾符**。
- 因此除 8 行外，所有字节（含缩进、行尾、末尾换行、BOM 状态）都不变。

用法：python tools/_apply_v230_translations.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

LANG_DIR = Path("src/main/resources/assets/echoesofoblivion/lang")

K_STELE = "block.echoesofoblivion.name_stele"
K_CUT = "block.echoesofoblivion.name_stele.inscribed"
K_TAKEN = "block.echoesofoblivion.name_stele.taken"
K1 = "message.echoesofoblivion.inscription.observed.1"
K2 = "message.echoesofoblivion.inscription.observed.2"
K3 = "message.echoesofoblivion.inscription.observed.3"
K4 = "message.echoesofoblivion.inscription.observed.4"
K5 = "message.echoesofoblivion.inscription.observed.5"

TRANSLATIONS: dict[str, dict[str, str]] = {
    # ---------------------------------------------------------------- ru_ru
    #
    # «Субъект» 大写，是档案里的一个类目，不是指代。
    # For the second entry, "This was not requested" uses the impersonal
    # «Этого не требовалось» — 无人称、官僚、冷。
    # «вырезано» 是完成体被动短尾：刻的动作已经结束且不可撤销。
    # «Никто не прочтёт» 是完成体将来时——确定的、永久的不否定，
    # 不含「以后也许会有人读到」的余地。
    # «Очередь» 取「队列 / 轮到的位置」义：有人已经在这里了，
    # 而且这个状态仍在延续。
    "ru_ru": {
        K_STELE: "Каменная стела имён",
        K_CUT: "Имя вырезано. Никто не прочтёт его.",
        K_TAKEN: "Кто-то уже здесь.",
        K1: "Субъект продолжает двигаться по Коридору с целью.",
        K2: "Субъект начал вести собственные записи. Этого не требовалось.",
        K3: "Субъект внёс имя в реестр. Имя было принято.",
        K4: "Субъект больше не спрашивает, для чего нужен Коридор.",
        K5: "Почерк субъекта совпадает с более ранними записями.",
    },
    # ---------------------------------------------------------------- uk_ua
    #
    # «Цього не вимагалося» — 无人称被动，与 ru 的 «Этого не требовалось» 对位。
    # «Ніхто не прочитає» 同样是完成体将来时，保住「将来也不会」。
    # «Черга» 同 ru 的 «очередь»：座位 / 岗位式的「已经在这里」。
    "uk_ua": {
        K_STELE: "Кам'яна стела імен",
        K_CUT: "Ім'я вирізано. Ніхто не прочитає його.",
        K_TAKEN: "Хтось уже тут.",
        K1: "Суб'єкт продовжує рухатися Коридором із метою.",
        K2: "Суб'єкт почав вести власні записи. Цього не вимагалося.",
        K3: "Суб'єкт вніс ім'я до реєстру. Ім'я було прийнято.",
        K4: "Суб'єкт більше не питає, для чого потрібен Коридор.",
        K5: "Почерк суб'єкта збігається з ранішими записами.",
    },
    # ---------------------------------------------------------------- ja_jp
    #
    # 「記録」= records / entries；「名簿」= register；
    # 「筆跡」保留，不弱化为「書き方」；「以前の記載」保住「更早的条目」。
    # 「切られた」用被动，不写谁切的；「読まれることはない」是确定的永久否定。
    "ja_jp": {
        K_STELE: "名の碑",
        K_CUT: "名は切られた。誰かに読まれることはない。",
        K_TAKEN: "もう誰かがここにいる。",
        K1: "対象は目的を持って回廊を進み続けている。",
        K2: "対象は自身の記録を取り始めた。これは求められていない。",
        K3: "対象は名簿に名を一つ加えた。名は受理された。",
        K4: "対象は回廊が何のためのものかを、もはや問わない。",
        K5: "対象の筆跡は、以前の記載と一致する。",
    },
    # ---------------------------------------------------------------- ko_kr
    #
    # 「명부」= 名册 / 登记簿；「받아들여졌다」用被动，不说被谁接受。
    # 「필체」保留，「이전의 기재 사항」保住「更早的条目」。
    #　「더 이상 묻지 않는다」是行为观察（不再问），不是心理描写。
    "ko_kr": {
        K_STELE: "이름 비석",
        K_CUT: "이름이 새겨졌습니다. 아무도 그것을 읽지 않을 것입니다.",
        K_TAKEN: "누군가 이미 여기 있습니다.",
        K1: "대상은 목적을 지닌 채 회랑을 계속 지나간다.",
        K2: "대상은 스스로의 기록을 남기기 시작했다. 이는 요구되지 않았다.",
        K3: "대상이 명부에 이름 하나를 더했다. 그 이름은 받아들여졌다.",
        K4: "대상은 회랑이 무엇을 위한 것인지 더 이상 묻지 않는다.",
        K5: "대상의 필체는 이전의 기재 사항과 일치한다.",
    },
    # ---------------------------------------------------------------- zh_tw
    #
    # 台湾用语：碑 / 對象 / 紀錄 / 侵蝕（该档已有「腐化」，此处不涉）。
    # 该档既有写法为「迴廊」（非「走廊」），沿用。
    # 「記下來」= keeping records；「名冊」= register；
    # 「筆跡」与「更早的紀錄」都保留，不弱化。
    "zh_tw": {
        K_STELE: "名字碑",
        K_CUT: "名字刻好了。沒有人會讀到它。",
        K_TAKEN: "已經有人在這裡了。",
        K1: "對象仍在迴廊中帶著目的移動。",
        K2: "對象已開始為自己記下紀錄。此舉未被要求。",
        K3: "對象在名冊上添了一個名字。名字已被接受。",
        K4: "對象已不再詢問迴廊是用來做什麼的。",
        K5: "對象的筆跡與更早的紀錄一致。",
    },
}


def split_key_line(line: str) -> tuple[str, str, bool, str] | None:
    """拆一行 `  "key": "value",` → (key, value, 是否有尾逗号, 行尾符)。

    值一律是简单 JSON 字符串（无 `%`、无 `\\n`），因此从末尾倒着找第一个 `"`
    就是值的收尾引号，`\"` 不会出现在这 8 个键的译文里。
    返回 None 表示这行不是键行。
    """
    stripped = line.rstrip("\r")
    eol = line[len(stripped):]
    if not stripped.startswith('  "') or not stripped.rstrip().endswith('"'):
        return None
    try:
        key = stripped[3:stripped.index('":', 3)]
    except ValueError:
        return None

    body = stripped.rstrip()
    has_comma = body.endswith(",")
    if has_comma:
        body = body[:-1].rstrip()
    if not body.endswith('"'):
        return None
    close = body.rindex('"')
    open_ = body.index('"', body.index(":"))  # 值的起始引号
    if close <= open_:
        return None
    return key, body[open_ + 1:close], has_comma, eol


def replace_lines(path: Path, mapping: dict[str, str]) -> tuple[int, int, int]:
    """文本层逐行替换；返回 (替换行数, 键数, 未找到的键数)。"""
    raw_bytes = path.read_bytes()
    raw = raw_bytes.decode("utf-8-sig")

    lines = raw.split("\n")
    replaced = 0
    for i, line in enumerate(lines):
        parts = split_key_line(line)
        if parts is None:
            continue
        key, _old, has_comma, eol = parts
        if key not in mapping:
            continue
        # 尾逗号按**该行原有**状态保留：最后一个键没有逗号，其余有。
        lines[i] = ('  ' + json.dumps(key, ensure_ascii=False) + ': '
                    + json.dumps(mapping[key], ensure_ascii=False)
                    + (',' if has_comma else '') + eol)
        replaced += 1

    out = "\n".join(lines)
    # 与原字节比较：只有被替换的行不同，其余字节必须完全一致。
    encoded = out.encode("utf-8")
    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        encoded = b"\xef\xbb\xbf" + encoded
    path.write_bytes(encoded)

    data = json.loads(out)
    total = len(data)
    missing = sum(1 for k in mapping if k not in data)
    return replaced, total, missing


def main() -> int:
    rc = 0
    print(f"共 {len(TRANSLATIONS)} 个语言 × {len(TRANSLATIONS['ru_ru'])} 个键\n")
    for locale, mapping in TRANSLATIONS.items():
        path = LANG_DIR / f"{locale}.json"
        before_keys = len(json.loads(path.read_text(encoding="utf-8")))
        replaced, total, missing = replace_lines(path, mapping)
        note = "OK" if (replaced == 8 and total == 214 and missing == 0) else "异常"
        if note != "OK":
            rc = 1
        print(f"  {locale:<7} 替换 {replaced} 行   {before_keys} -> {total} 键   {note}")
    print()
    print("完成。" if rc == 0 else "存在问题。")
    return rc


if __name__ == "__main__":
    raise SystemExit(main())
