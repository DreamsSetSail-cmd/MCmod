#!/usr/bin/env python3
"""一次性脚本：写入 v2.4.0 的 5 个语言键。

v2.4.0 的主题是「合唱」：backlog 把多人差异化标为「待评估」，
这一版的评估结论是——**差异化，但不是让每个玩家特别，
而是让所有玩家共同不可靠。**

本批包含：
1. 第 13 段残片（`choir_errata`）的名称、标题与正文。
   它是一份**抄写错误率异常的会议记录**，也是唯一一段内容本身就在讲
   「同一句话被抄了多次」的残片。
2. 合唱渗出的提示（多人时，别人的进度漏到你这里）。

`en_us` / `zh_cn` 写真实文本；其余语言写英文回退，随后由翻译任务补齐。

**行尾按文件保留**（CRLF 与 LF 两种都存在，见 `check_lang_format.py`），
因此这里做文本层插入而不是 `json.dumps` 整体重写。

用法：python tools/add_v240_keys.py
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

# 第 13 段残片的正文。
#
# 它是一份会议记录，但**同一句话被抄了四次，每次都不一样**。
# 记录者不承认自己抄错了——他只是在每一版后面标注了「已核」。
#
# 这一段刻意不解释合唱机制（底线二：可解释即失效）。
# 它只让读者产生一个怀疑：**那我读到的是哪一版？**
CHOIR_ERRATA_TEXT = (
    "Session 41. Present: nine.\\n"
    "The bridge will be built. (Checked.)\\n"
    "The bridge will be built. (Checked.)\\n"
    "The bridge is built. (Checked.)\\n"
    "The bridge was built. (Checked.)\\n"
    "All four entries are the same entry. I have compared them myself.\\n"
    "I do not know why the archive keeps four.")
CHOIR_ERRATA_TEXT_ZH = (
    "第 41 次會談。出席：九人。\\n"
    "橋將會建成。（已核。）\\n"
    "橋將會建成。（已核。）\\n"
    "橋已建成。（已核。）\\n"
    "橋早已建成。（已核。）\\n"
    "四條是同一條。我自己比對過。\\n"
    "我不知道檔案為什麼留著四條。")

KEYS = [
    # 残片物品名、标题、正文
    ("item.echoesofoblivion.fragment_choir_errata", "Errata, Session 41", "勘誤，第 41 次會談"),
    ("fragment.echoesofoblivion.choir_errata.title", "Errata, Session 41", "勘誤，第 41 次會談"),
    ("fragment.echoesofoblivion.choir_errata.text", CHOIR_ERRATA_TEXT, CHOIR_ERRATA_TEXT_ZH),

    # 合唱渗出提示（多人）。
    #
    # %s 是**别人已经见证、而你还不知道的**那段记忆的标题。
    # 语气：不是提示，是**渗出**——像是从隔壁漏过来的，而没有人打算告诉你。
    # 因此不说「某玩家解锁了」，也不说「你听到了」。
    ("message.echoesofoblivion.choir.bleed",
     "You have not witnessed this, but you know its name now: %s",
     "你从未见证过这个，但你现在知道它的名字了：%s"),

    # 合唱渗出的补充说明（可在配置/帮助里展示）。
    # 一句话讲清这件事的性质：**差异本身存在**。不解释机制。
    ("message.echoesofoblivion.choir.bleed.hint",
     "Someone nearby knows something you do not.",
     "附近的某个人知道一些你不知道的事。"),
]


def insert_keys(path: Path, locale: str) -> tuple[int, int]:
    raw_bytes = path.read_bytes()
    had_bom = raw_bytes.startswith(b"\xef\xbb\xbf")
    raw = raw_bytes.decode("utf-8-sig")

    use_crlf = raw.count("\r\n") > 0
    text = raw.replace("\r\n", "\n").rstrip("\n")

    data = json.loads(text)
    missing = [(k, en, zh) for k, en, zh in KEYS if k not in data]
    if not missing:
        return 0, len(data)

    lines = text.split("\n")
    close_index = max(i for i, line in enumerate(lines) if line.strip() == "}")
    body = lines[:close_index]

    last = body[-1]
    if not last.rstrip().endswith(",") and not last.rstrip().endswith("{"):
        body[-1] = last.rstrip() + ","

    for key, en, zh in missing:
        value = zh if locale == "zh_cn" else en
        body.append(f"  {json.dumps(key, ensure_ascii=False)}: "
                    f"{json.dumps(value, ensure_ascii=False)},")

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

    print(f"本批共 {len(KEYS)} 个键\n")
    for path in files:
        added, total = insert_keys(path, path.stem)
        print(f"  {path.stem:<7} +{added} 键   共 {total} 键")

    print(f"\n完成：{len(files)} 个文件")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
