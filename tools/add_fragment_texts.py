#!/usr/bin/env python3
"""一次性脚本：写入 12 段残片的标题与正文（v2.0.0）。

残片是「故事太少」的核心解法：把文明的编年史拆成 12 段，散落在走廊遗迹里。
每段都有独立语气，让玩家能分辨「谁在说话」：

- CHRONICLE 编年 —— 冷静、事后的官方记述
- LOG       记录 —— 短句、术语、编号
- LETTER    信件 —— 只有这里出现「我」
- PRAYER    祈祷 —— 后期才有，且没有对象

`en_us` / `zh_cn` 写入真实文本；其余语言先写英文回退，随后由翻译任务补齐。

用法：python tools/add_fragment_texts.py
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

# (id, 英文标题, 英文正文, 中文标题, 中文正文)
FRAGMENTS = [
    (
        "founding",
        "On the Founding",
        "Before the bridges we had only our own heads, and each of us believed that was enough.\\n"
        "It was the first thing we were wrong about.",
        "论建桥之前",
        "在桥之前，我们只有各自的脑袋，而每个人都以为这就够了。\\n"
        "这是我们弄错的第一件事。",
    ),
    (
        "first_bridge",
        "First Successful Crossing",
        "He thought of a door.\\nI thought of the same door, at the same moment, without being told.\\n"
        "We wrote it down because we did not know what else to do with it.",
        "第一次成功跨越",
        "他想到了门。\\n我在同一瞬间想到了同一扇门，没有人告诉我。\\n"
        "我们把它记了下来，因为除此之外不知道还能做什么。",
    ),
    (
        "choir_record",
        "The Choir, Recorded",
        "Entry 1: two minds, overlap 4 seconds.\\n"
        "Entry 412: nine minds, overlap 40 seconds. No one could say afterwards which thought was whose.\\n"
        "Entry 413: overlap has no measured end. We have stopped counting.",
        "合唱，记录",
        "第 1 条：两个意识，重合 4 秒。\\n"
        "第 412 条：九个意识，重合 40 秒。事后没有人能说清哪个念头是谁的。\\n"
        "第 413 条：重合没有测出的终点。我们不再计数了。",
    ),
    (
        "cost_ledger",
        "Ledger of Cost",
        "Item: one measure of quiet, per crossing, per mind.\\n"
        "Projected total at current expansion: unrecoverable.\\n"
        "Recommendation: continue. The expense is ours, and it is only quiet.",
        "代价账簿",
        "项：每一次跨越、每一个意识，支出一份安静。\\n"
        "按当前扩张速度推算总额：不可回收。\\n"
        "建议：继续。这笔开销是我们自己的，而且不过是安静而已。",
    ),
    (
        "letter_home",
        "Letter, Unsent",
        "I am writing this instead of sleeping.\\n"
        "The Choir is wider than it was, and everyone is pleased. I am pleased.\\n"
        "I only wanted to put down that last night I woke and could not find the edge of my own thought.\\n"
        "There was no edge. I am sure it is nothing.",
        "一封没有寄出的信",
        "我写这个，是因为睡不着。\\n"
        "合唱比过去更宽了，所有人都很高兴。我也高兴。\\n"
        "我只是想记下来：昨夜我醒来，找不到自己念头的边界。\\n"
        "那里没有边界。我相信这不算什么。",
    ),
    (
        "her_first_note",
        "Note, Undated",
        "I have listened to the hum for eleven nights.\\n"
        "It is not a sound. A sound has a source.\\n"
        "It is a place, and we have been widening the road to it.",
        "字条，无日期",
        "那道嗡鸣我听了十一个夜晚。\\n"
        "它不是声音。声音有来源。\\n"
        "它是个地方，而我们一直在把通往那里的路拓宽。",
    ),
    (
        "her_last_note",
        "Last Complete Sentence",
        "Stop building.",
        "最后一句完整的话",
        "停止建造。",
    ),
    (
        "apparatus_spec",
        "Apparatus: Purpose",
        "Function: to establish a chamber of quiet that cannot be entered or replaced.\\n"
        "Cost: the chamber must first be emptied of all sound.\\n"
        "Note: this is not a flaw. This is the mechanism.",
        "仪器：用途",
        "功能：建立一个无法被进入、也无法被替换的静默腔室。\\n"
        "代价：该腔室必须先被清空一切声音。\\n"
        "备注：这不是缺陷。这就是它的原理。",
    ),
    (
        "the_quiet_order",
        "The Quiet Order",
        "By unanimous silence of the Choir:\\n"
        "1. No speech.\\n"
        "2. No writing of speech.\\n"
        "3. No names.\\n"
        "Signed: (no signature)",
        "寂静教令",
        "经合唱一致沉默通过：\\n"
        "一、不得言语。\\n"
        "二、不得记录言语。\\n"
        "三、不得留名。\\n"
        "签署：（无署名）",
    ),
    (
        "after",
        "After",
        "Seventy days without incident.\\n"
        "We have kept the Order. We do not know what we were keeping it from.\\n"
        "Nothing has been written here for some time. This entry is only to mark that we are still here.",
        "之后",
        "七十天无事发生。\\n"
        "我们遵守了教令。我们不知道自己在防什么。\\n"
        "这里已经很久没有新记录。这一条只是为了标记我们还在这里。",
    ),
    (
        "prayer_first",
        "Prayer, First",
        "To the place where sound used to be:\\n"
        "we did not mean to build here.\\n"
        "Please give back what we paid, and keep the rest.",
        "祈祷，第一次",
        "致那个曾经有声音的位置：\\n"
        "我们并不是有意要建在这里。\\n"
        "请把我们已经付出的还回来，剩下的你留着。",
    ),
    (
        "prayer_last",
        "Prayer, Last",
        "We are not being punished.\\n"
        "There is no one left to punish us.",
        "祈祷，最后一次",
        "我们不是在受罚。\\n"
        "已经没有谁可以惩罚我们了。",
    ),
]


def main() -> int:
    files = sorted(LANG_DIR.glob("*.json"))
    if not files:
        print(f"在 {LANG_DIR} 下找不到语言文件")
        return 1

    for path in files:
        locale = path.stem
        data = json.loads(path.read_text(encoding="utf-8"))
        added = 0
        for fid, en_title, en_text, zh_title, zh_text in FRAGMENTS:
            title_key = f"fragment.echoesofoblivion.{fid}.title"
            text_key = f"fragment.echoesofoblivion.{fid}.text"
            if locale == "zh_cn":
                pairs = ((title_key, zh_title), (text_key, zh_text))
            else:
                pairs = ((title_key, en_title), (text_key, en_text))
            for key, value in pairs:
                if key not in data:
                    data[key] = value
                    added += 1
        if added:
            path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                            encoding="utf-8")
        print(f"  {locale:<7} +{added:2d} 键   共 {len(data)} 键")

    print(f"\n完成：{len(files)} 个文件，每份新增 {len(FRAGMENTS) * 2} 个键")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
