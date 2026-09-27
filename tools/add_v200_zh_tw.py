#!/usr/bin/env python3
"""一次性脚本：把 v2.0.0 收尾批次的 31 个键写成 zh_tw 译文。

其余 19 个非英中语言由并行翻译任务处理，本脚本只管 zh_tw。

**行尾**：zh_tw.json 是纯 LF，写入必须保持 LF（`check_lang_format.py` 会报混用）。
因此这里做文本层替换，而不是 `json.dumps` 整体重写。

术语依 `ritual.echoesofoblivion.*` 既有译法：
    echo → 回聲     corruption → 侵蝕     沉默/靜默 区分于 寂靜

用法：python tools/add_v200_zh_tw.py
"""

from __future__ import annotations

import json
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except (AttributeError, ValueError):
    pass

PATH = Path("src/main/resources/assets/echoesofoblivion/lang/zh_tw.json")

VALUES = {
    # ---- 12 段殘片的物品名稱（沿用同檔既有標題，保證同名一致）----
    "item.echoesofoblivion.fragment_founding": "論建橋之前",
    "item.echoesofoblivion.fragment_first_bridge": "第一次成功跨越",
    "item.echoesofoblivion.fragment_choir_record": "合唱，紀錄",
    "item.echoesofoblivion.fragment_cost_ledger": "代價帳簿",
    "item.echoesofoblivion.fragment_letter_home": "一封沒有寄出的信",
    "item.echoesofoblivion.fragment_her_first_note": "字條，無日期",
    "item.echoesofoblivion.fragment_her_last_note": "最後一句完整的話",
    "item.echoesofoblivion.fragment_apparatus_spec": "儀器：用途",
    "item.echoesofoblivion.fragment_the_quiet_order": "寂靜教令",
    "item.echoesofoblivion.fragment_after": "之後",
    "item.echoesofoblivion.fragment_prayer_first": "祈禱，第一次",
    "item.echoesofoblivion.fragment_prayer_last": "祈禱，最後一次",

    # ---- 發掘鏟 ----
    "item.echoesofoblivion.excavation_shovel.desc": "右鍵點擊記憶水晶取樣。水晶仍可閱讀。",
    "item.echoesofoblivion.excavation_shovel.keep": "它不摧毀所觸碰之物。",
    "item.echoesofoblivion.excavation_shovel.sealed": "有東西從封泥裡鬆脫了。",
    "item.echoesofoblivion.excavation_shovel.opened": "封泥破裂。一段殘片浮現。",

    # ---- 穩定劑 ----
    "item.echoesofoblivion.stabilizer.desc": "%s 秒內，世界維持原樣。",
    "item.echoesofoblivion.stabilizer.applied": "保持靜止 %s 秒。",

    # ---- 記憶之瓶 ----
    "item.echoesofoblivion.memory_vial.desc": "減輕 %s 點侵蝕。",
    "item.echoesofoblivion.memory_vial.cost": "代價是一段記憶。由不得你挑。",
    "item.echoesofoblivion.memory_vial.used": "%s 消失了。你輕了一些，也少了一些。",
    "item.echoesofoblivion.memory_vial.used_empty": "已無物可付。它照收不誤。",
    "item.echoesofoblivion.memory_vial.clear": "你身上已沒有可減輕的東西。",

    # ---- 記憶之刃 ----
    "item.echoesofoblivion.memory_blade.desc": "對回聲比對活物更重。",
    "item.echoesofoblivion.memory_blade.useless": "寂靜聚合體無法被砍。",
    "item.echoesofoblivion.memory_blade.no_effect": "刀刃穿了過去。那裡沒有可砍的東西。",

    # ---- 碎晶法杖 ----
    "item.echoesofoblivion.shatter_staff.desc": "右鍵打散你能看見的回聲。",
    "item.echoesofoblivion.shatter_staff.useless": "它對聚合體毫無作用。",
    "item.echoesofoblivion.shatter_staff.hit": "%s 個回聲被打散。",

    # ---- 研究者提燈 ----
    "item.echoesofoblivion.researcher_lantern.desc": "持有時，侵蝕累積速度減半。",
    "item.echoesofoblivion.researcher_lantern.slow": "她帶著它走到了最後。它沒能救她。",
}


def main() -> int:
    raw_bytes = PATH.read_bytes()
    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        print("zh_tw.json 含 BOM，中止")
        return 1

    raw = raw_bytes.decode("utf-8")
    use_crlf = raw.count("\r\n") > 0
    text = raw.replace("\r\n", "\n").rstrip("\n")

    data = json.loads(text)
    missing = [k for k in VALUES if k not in data]
    if missing:
        print(f"以下键不存在于 zh_tw.json：{missing}")
        return 1

    changed = 0
    for key, value in VALUES.items():
        old = json.dumps(data[key], ensure_ascii=False)
        new = json.dumps(value, ensure_ascii=False)
        # 最后一条属性没有尾逗号，其余都有——两种都要能匹配
        for suffix in (",", ""):
            line = f'  "{key}": {old}{suffix}'
            if line in text:
                text = text.replace(line, f'  "{key}": {new}{suffix}', 1)
                changed += 1
                break
        else:
            print(f"找不到原始行：{key}")
            return 1

    out = text + "\n"
    if use_crlf:
        out = out.replace("\n", "\r\n")
    PATH.write_bytes(out.encode("utf-8"))

    # 写后自证
    check = json.loads(PATH.read_text(encoding="utf-8-sig"))
    assert len(check) == 198, f"键数变成 {len(check)}"
    for key, value in VALUES.items():
        assert check[key] == value, f"{key} 未写入"
    print(f"zh_tw.json：{changed} 个键写入，共 {len(check)} 键，"
          f"行尾 {'CRLF' if use_crlf else 'LF'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
