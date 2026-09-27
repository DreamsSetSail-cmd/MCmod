#!/usr/bin/env python3
"""一次性脚本：写入 v2.0.0 收尾批次的 30 个语言键。

分两部分：
1. 12 个残片物品的**名称键**（`item.echoesofoblivion.fragment_*`）。
   这是修复一个真实缺陷——`ModItems.fragment()` 以 `fragment_<id>` 注册，
   而语言文件里只有 `fragment.echoesofoblivion.<id>.title/text`，
   于是 12 个残片在物品栏里显示为原始键名。
   名称直接取自英文标题（物品名与残片标题本就该一致）。
2. 6 件新实现物品的提示与反馈（探查铲 / 稳定剂 / 记忆之瓶 / 记忆之刃 /
   碎晶法杖 / 研究者提灯）。

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾必须按文件保留**：本仓库的语言文件一部分是 CRLF、一部分是 LF
（两者都合规，但同一文件内不能混用，见 `check_lang_format.py`）。
因此这里做**文本层插入**而不是 `json.dumps` 整体重写——
后者会把 CRLF 文件全部改成 LF。

用法：python tools/add_v200_final_keys.py
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

# 12 段残片：物品名与英文标题一致
FRAGMENT_TITLES = {
    "founding": "On the Founding",
    "first_bridge": "First Successful Crossing",
    "choir_record": "The Choir, Recorded",
    "cost_ledger": "Ledger of Cost",
    "letter_home": "Letter, Unsent",
    "her_first_note": "Note, Undated",
    "her_last_note": "Last Complete Sentence",
    "apparatus_spec": "Apparatus: Purpose",
    "the_quiet_order": "The Quiet Order",
    "after": "After",
    "prayer_first": "Prayer, First",
    "prayer_last": "Prayer, Last",
}

FRAGMENT_TITLES_ZH = {
    "founding": "论建城",
    "first_bridge": "第一次成功的跨越",
    "choir_record": "唱诗班录音",
    "cost_ledger": "代价账簿",
    "letter_home": "未寄出的信",
    "her_first_note": "无日期的字条",
    "her_last_note": "最后一句完整的话",
    "apparatus_spec": "仪器：用途",
    "the_quiet_order": "寂静教团",
    "after": "之后",
    "prayer_first": "祷词·首",
    "prayer_last": "祷词·末",
}

# (完整键, 英文, 中文)
OTHER_KEYS = [
    # ---- 探查铲 ----
    ("item.echoesofoblivion.excavation_shovel.desc",
     "Right-click a memory crystal to take a sample. The crystal stays readable.",
     "右键记忆水晶取样。水晶仍可阅读。"),
    ("item.echoesofoblivion.excavation_shovel.keep",
     "It does not destroy what it touches.",
     "它不摧毁所触碰的东西。"),
    ("item.echoesofoblivion.excavation_shovel.sealed",
     "Something sealed came loose.",
     "有东西从封泥里松脱了。"),
    ("item.echoesofoblivion.excavation_shovel.opened",
     "The seal breaks. A fragment surfaces.",
     "封泥破裂。一段残片浮出。"),

    # ---- 稳定剂 ----
    ("item.echoesofoblivion.stabilizer.desc",
     "For %s seconds, the world holds its shape.",
     "%s 秒内，世界保持原样。"),
    ("item.echoesofoblivion.stabilizer.applied",
     "Held still for %s seconds.",
     "保持静止 %s 秒。"),

    # ---- 记忆之瓶 ----
    ("item.echoesofoblivion.memory_vial.desc",
     "Relieves %s corruption.",
     "减轻 %s 点侵蚀。"),
    ("item.echoesofoblivion.memory_vial.cost",
     "It is paid for with a memory. You do not choose which.",
     "代价是一段记忆。由不得你挑。"),
    ("item.echoesofoblivion.memory_vial.used",
     "%s is gone. You feel lighter, and less.",
     "%s 消失了。你轻了一些，也少了一些。"),
    ("item.echoesofoblivion.memory_vial.used_empty",
     "Nothing left to give. It takes anyway.",
     "已无物可付。它照收不误。"),
    ("item.echoesofoblivion.memory_vial.clear",
     "There is nothing in you left to relieve.",
     "你身上已没有可减轻的东西。"),

    # ---- 记忆之刃 ----
    ("item.echoesofoblivion.memory_blade.desc",
     "Heavier against echoes than against the living.",
     "对回响比对活物更重。"),
    ("item.echoesofoblivion.memory_blade.useless",
     "The Silent Aggregate cannot be cut.",
     "寂静聚合体无法被砍。"),
    ("item.echoesofoblivion.memory_blade.no_effect",
     "The blade passes through. There is nothing there to cut.",
     "刀刃穿了过去。那里没有可砍的东西。"),

    # ---- 碎晶法杖 ----
    ("item.echoesofoblivion.shatter_staff.desc",
     "Right-click to break the echoes you can see.",
     "右键打散你能看见的回响。"),
    ("item.echoesofoblivion.shatter_staff.useless",
     "It does nothing to the Aggregate.",
     "它对聚合体毫无作用。"),
    ("item.echoesofoblivion.shatter_staff.hit",
     "%s echoes scattered.",
     "%s 个回响被打散。"),

    # ---- 研究者提灯 ----
    ("item.echoesofoblivion.researcher_lantern.desc",
     "While carried, corruption builds at half speed.",
     "持有时，侵蚀积累速度减半。"),
    ("item.echoesofoblivion.researcher_lantern.slow",
     "She carried it to the end. It did not save her.",
     "她带着它走到了最后。它没能救她。"),
]


def build_keys() -> list[tuple[str, str, str]]:
    keys: list[tuple[str, str, str]] = []
    for frag_id, en in FRAGMENT_TITLES.items():
        keys.append((f"item.echoesofoblivion.fragment_{frag_id}",
                     en, FRAGMENT_TITLES_ZH[frag_id]))
    keys.extend(OTHER_KEYS)
    return keys


def insert_keys(path: Path, keys: list[tuple[str, str, str]], locale: str) -> tuple[int, int]:
    """按文件原有行尾，把缺失的键插到最后一个 '}' 之前。"""
    raw_bytes = path.read_bytes()
    had_bom = raw_bytes.startswith(b"\xef\xbb\xbf")
    raw = raw_bytes.decode("utf-8-sig")

    use_crlf = raw.count("\r\n") > 0
    text = raw.replace("\r\n", "\n").rstrip("\n")

    data = json.loads(text)
    missing = [(k, en, zh) for k, en, zh in keys if k not in data]
    if not missing:
        return 0, len(data)

    # 最后一个 '}' 所在行
    lines = text.split("\n")
    close_index = max(i for i, line in enumerate(lines) if line.strip() == "}")
    body = lines[:close_index]

    # 上一行需要补逗号
    last = body[-1]
    if not last.rstrip().endswith(",") and not last.rstrip().endswith("{"):
        body[-1] = last.rstrip() + ","

    for key, en, zh in missing:
        value = zh if locale == "zh_cn" else en
        body.append(f"  {json.dumps(key, ensure_ascii=False)}: "
                    f"{json.dumps(value, ensure_ascii=False)},")

    # 原本的最后一行条目现在不再是最后一条，去掉它多出来的逗号
    body[-1] = body[-1][:-1] if body[-1].endswith(",") else body[-1]

    lines = body + lines[close_index:]
    out = "\n".join(lines) + "\n"
    if use_crlf:
        out = out.replace("\n", "\r\n")

    encoded = out.encode("utf-8")
    if had_bom:
        encoded = b"\xef\xbb\xbf" + encoded
    path.write_bytes(encoded)

    return len(missing), len(data) + len(missing)


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    keys = build_keys()
    print(f"本批共 {len(keys)} 个键 "
          f"（12 个残片名称 + {len(OTHER_KEYS)} 个提示与反馈）\n")

    for path in files:
        locale = path.stem
        added, total = insert_keys(path, keys, locale)
        print(f"  {locale:<7} +{added:2d} 键   共 {total} 键")

    print(f"\n完成：{len(files)} 个文件")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
