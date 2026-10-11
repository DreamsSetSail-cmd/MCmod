#!/usr/bin/env python3
"""v2.4.0「合唱」批次：为 18 个语言文件翻译 3 个键。

只做**文本层逐行替换**：读取 → 只改这 3 行 → 写回。
不整体重写 JSON，保证另外 216 个键逐字节不变、行尾风格不变。

用法::

    python tools/_apply_v240_choir.py            # 写入
    python tools/_apply_v240_choir.py --dry-run  # 只报告
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

K_BLEED = "message.echoesofoblivion.choir.bleed"
K_HINT = "message.echoesofoblivion.choir.bleed.hint"
K_TEXT = "fragment.echoesofoblivion.choir_errata.text"

CRLF_LOCALES = ["de_de", "nl_nl", "sv_se", "fr_fr", "it_it", "es_es", "es_mx",
                "pt_br", "pt_pt"]
LF_LOCALES = ["da_dk", "nb_no", "pl_pl", "cs_cz", "tr_tr", "ja_jp", "ko_kr",
              "ru_ru", "uk_ua"]

# errata 的 7 行；写回时用字面量 \n（反斜杠 + n，两字符）连接。
DATA: dict[str, dict[str, object]] = {
    "de_de": {
        "bleed": "Du hast dies nicht gesehen, doch du kennst jetzt seinen Namen: %s",
        "hint": "Jemand in der Nähe weiß etwas, das du nicht weißt.",
        "errata": [
            "Sitzung 41. Anwesend: neun.",
            "Die Brücke wird gebaut werden. (Geprüft.)",
            "Die Brücke wird gebaut werden. (Geprüft.)",
            "Die Brücke ist gebaut. (Geprüft.)",
            "Die Brücke war gebaut. (Geprüft.)",
            "Alle vier Einträge sind derselbe Eintrag. Ich habe sie selbst verglichen.",
            "Ich weiß nicht, warum das Archiv vier behält.",
        ],
    },
    "nl_nl": {
        "bleed": "Je hebt dit niet aanschouwd, maar je kent nu de naam ervan: %s",
        "hint": "Iemand in de buurt weet iets wat jij niet weet.",
        "errata": [
            "Sessie 41. Aanwezig: negen.",
            "De brug zal gebouwd worden. (Nagekeken.)",
            "De brug zal gebouwd worden. (Nagekeken.)",
            "De brug is gebouwd. (Nagekeken.)",
            "De brug was gebouwd. (Nagekeken.)",
            "Alle vier aantekeningen zijn dezelfde aantekening. Ik heb ze zelf vergeleken.",
            "Ik weet niet waarom het archief er vier bewaart.",
        ],
    },
    "sv_se": {
        "bleed": "Du har inte bevittnat detta, men du känner nu dess namn: %s",
        "hint": "Någon i närheten vet något som du inte vet.",
        "errata": [
            "Session 41. Närvarande: nio.",
            "Bron kommer att byggas. (Kontrollerat.)",
            "Bron kommer att byggas. (Kontrollerat.)",
            "Bron är byggd. (Kontrollerat.)",
            "Bron var byggd. (Kontrollerat.)",
            "Alla fyra poster är samma post. Jag har jämfört dem själv.",
            "Jag vet inte varför arkivet behåller fyra.",
        ],
    },
    "da_dk": {
        "bleed": "Du har ikke været vidne til dette, men du kender nu dets navn: %s",
        "hint": "Nogen i nærheden ved noget, du ikke ved.",
        "errata": [
            "Session 41. Til stede: ni.",
            "Broen vil blive bygget. (Kontrolleret.)",
            "Broen vil blive bygget. (Kontrolleret.)",
            "Broen er bygget. (Kontrolleret.)",
            "Broen var bygget. (Kontrolleret.)",
            "Alle fire poster er den samme post. Jeg har selv sammenlignet dem.",
            "Jeg ved ikke, hvorfor arkivet beholder fire.",
        ],
    },
    "nb_no": {
        "bleed": "Du har ikke vært vitne til dette, men du kjenner nå navnet på det: %s",
        "hint": "Noen i nærheten vet noe du ikke vet.",
        "errata": [
            "Sesjon 41. Til stede: ni.",
            "Broen vil bli bygget. (Kontrollert.)",
            "Broen vil bli bygget. (Kontrollert.)",
            "Broen er bygget. (Kontrollert.)",
            "Broen var bygget. (Kontrollert.)",
            "Alle fire oppføringer er den samme oppføringen. Jeg har sammenlignet dem selv.",
            "Jeg vet ikke hvorfor arkivet beholder fire.",
        ],
    },
    "fr_fr": {
        "bleed": "Vous n'avez pas vu ceci, mais vous en connaissez maintenant le nom : %s",
        "hint": "Quelqu'un tout près sait quelque chose que vous ignorez.",
        "errata": [
            "Séance 41. Présents : neuf.",
            "Le pont sera construit. (Vérifié.)",
            "Le pont sera construit. (Vérifié.)",
            "Le pont est construit. (Vérifié.)",
            "Le pont a été construit. (Vérifié.)",
            "Les quatre entrées sont la même entrée. Je les ai comparées moi-même.",
            "Je ne sais pas pourquoi l'archive en garde quatre.",
        ],
    },
    "it_it": {
        "bleed": "Non hai assistito a questo, ma ora ne conosci il nome: %s",
        "hint": "Qualcuno qui vicino sa qualcosa che tu non sai.",
        "errata": [
            "Sessione 41. Presenti: nove.",
            "Il ponte sarà costruito. (Verificato.)",
            "Il ponte sarà costruito. (Verificato.)",
            "Il ponte è costruito. (Verificato.)",
            "Il ponte è stato costruito. (Verificato.)",
            "Le quattro registrazioni sono la stessa registrazione. Le ho confrontate io stesso.",
            "Non so perché l'archivio ne conservi quattro.",
        ],
    },
    "es_es": {
        "bleed": "No has presenciado esto, pero ahora conoces su nombre: %s",
        "hint": "Alguien cerca de ti sabe algo que tú no sabes.",
        "errata": [
            "Sesión 41. Presentes: nueve.",
            "El puente será construido. (Verificado.)",
            "El puente será construido. (Verificado.)",
            "El puente está construido. (Verificado.)",
            "El puente estaba construido. (Verificado.)",
            "Las cuatro entradas son la misma entrada. Las he comparado yo mismo.",
            "No sé por qué el archivo conserva cuatro.",
        ],
    },
    "es_mx": {
        "bleed": "No has presenciado esto, pero ahora conoces su nombre: %s",
        "hint": "Alguien cerca de ti sabe algo que tú no sabes.",
        "errata": [
            "Sesión 41. Presentes: nueve.",
            "El puente será construido. (Verificado.)",
            "El puente será construido. (Verificado.)",
            "El puente está construido. (Verificado.)",
            "El puente estaba construido. (Verificado.)",
            "Las cuatro entradas son la misma entrada. Las he comparado yo mismo.",
            "No sé por qué el archivo conserva cuatro.",
        ],
    },
    "pt_br": {
        "bleed": "Você não testemunhou isto, mas agora conhece o seu nome: %s",
        "hint": "Alguém perto de você sabe algo que você não sabe.",
        "errata": [
            "Sessão 41. Presentes: nove.",
            "A ponte será construída. (Verificado.)",
            "A ponte será construída. (Verificado.)",
            "A ponte está construída. (Verificado.)",
            "A ponte estava construída. (Verificado.)",
            "As quatro entradas são a mesma entrada. Eu mesmo as comparei.",
            "Não sei por que o arquivo mantém quatro.",
        ],
    },
    "pt_pt": {
        "bleed": "Não testemunhaste isto, mas agora conheces o seu nome: %s",
        "hint": "Alguém perto de ti sabe algo que tu não sabes.",
        "errata": [
            "Sessão 41. Presentes: nove.",
            "A ponte será construída. (Verificado.)",
            "A ponte será construída. (Verificado.)",
            "A ponte está construída. (Verificado.)",
            "A ponte estava construída. (Verificado.)",
            "As quatro entradas são a mesma entrada. Comparei-as eu mesmo.",
            "Não sei porque o arquivo mantém quatro.",
        ],
    },
    "pl_pl": {
        "bleed": "Nie byłeś świadkiem tego, ale teraz znasz jego nazwę: %s",
        "hint": "Ktoś w pobliżu wie coś, czego ty nie wiesz.",
        "errata": [
            "Sesja 41. Obecnych: dziewięciu.",
            "Most zostanie zbudowany. (Sprawdzone.)",
            "Most zostanie zbudowany. (Sprawdzone.)",
            "Most jest zbudowany. (Sprawdzone.)",
            "Most był zbudowany. (Sprawdzone.)",
            "Wszystkie cztery wpisy to ten sam wpis. Porównałem je sam.",
            "Nie wiem, dlaczego archiwum trzyma cztery.",
        ],
    },
    "cs_cz": {
        "bleed": "Tohle jste neviděli, ale teď znáte jeho název: %s",
        "hint": "Někdo poblíž ví něco, co vy nevíte.",
        "errata": [
            "Zasedání 41. Přítomno: devět.",
            "Most bude postaven. (Zkontrolováno.)",
            "Most bude postaven. (Zkontrolováno.)",
            "Most je postaven. (Zkontrolováno.)",
            "Most byl postaven. (Zkontrolováno.)",
            "Všechny čtyři záznamy jsou tentýž záznam. Porovnal jsem je sám.",
            "Nevím, proč archiv drží čtyři.",
        ],
    },
    "tr_tr": {
        "bleed": "Buna tanık olmadın, ama artık adını biliyorsun: %s",
        "hint": "Yakınlarda biri, senin bilmediğin bir şeyi biliyor.",
        "errata": [
            "Oturum 41. Mevcut: dokuz.",
            "Köprü inşa edilecektir. (Kontrol edildi.)",
            "Köprü inşa edilecektir. (Kontrol edildi.)",
            "Köprü inşa edilmiştir. (Kontrol edildi.)",
            "Köprü inşa edilmişti. (Kontrol edildi.)",
            "Dört kayıt da aynı kayıttır. Onları kendim karşılaştırdım.",
            "Arşivin neden dört kayıt tuttuğunu bilmiyorum.",
        ],
    },
    "ja_jp": {
        "bleed": "あなたはこれを目撃していません。しかし、今はその名を知っています：%s",
        "hint": "近くにいる誰かが、あなたの知らないことを知っています。",
        "errata": [
            "第 41 回会議。出席：九名。",
            "橋は建設される。（確認済み。）",
            "橋は建設される。（確認済み。）",
            "橋は建設された。（確認済み。）",
            "橋は建設されていた。（確認済み。）",
            "四つの項目は同じ一つの項目である。私自身で比べた。",
            "記録保管庫がなぜ四つ残しているのかは分からない。",
        ],
    },
    "ko_kr": {
        "bleed": "당신은 이것을 목격하지 않았습니다. 그러나 이제 그 이름을 알고 있습니다: %s",
        "hint": "근처에 있는 누군가가 당신이 모르는 것을 알고 있습니다.",
        "errata": [
            "제41차 회의. 출석: 아홉.",
            "다리는 세워질 것이다. (확인됨.)",
            "다리는 세워질 것이다. (확인됨.)",
            "다리는 세워졌다. (확인됨.)",
            "다리는 세워져 있었다. (확인됨.)",
            "네 개의 기록은 같은 하나의 기록이다. 내가 직접 대조했다.",
            "기록 보관소가 왜 네 개를 남겨 두는지는 나도 모른다.",
        ],
    },
    "ru_ru": {
        "bleed": "Ты не был свидетелем этого, но теперь знаешь его название: %s",
        "hint": "Кто-то поблизости знает то, чего ты не знаешь.",
        "errata": [
            "Заседание 41. Присутствуют: девять.",
            "Мост будет построен. (Проверено.)",
            "Мост будет построен. (Проверено.)",
            "Мост построен. (Проверено.)",
            "Мост был построен. (Проверено.)",
            "Все четыре записи — одна и та же запись. Я сравнил их сам.",
            "Я не знаю, почему архив хранит четыре.",
        ],
    },
    "uk_ua": {
        "bleed": "Ти не був свідком цього, але тепер знаєш його назву: %s",
        "hint": "Хтось поблизу знає те, чого ти не знаєш.",
        "errata": [
            "Засідання 41. Присутні: дев'ять.",
            "Міст буде збудований. (Перевірено.)",
            "Міст буде збудований. (Перевірено.)",
            "Міст збудований. (Перевірено.)",
            "Міст був збудований. (Перевірено.)",
            "Усі чотири записи — це той самий запис. Я порівняв їх сам.",
            "Я не знаю, чому архів зберігає чотири.",
        ],
    },
}


def literal(value: str) -> str:
    """把 JSON 字符串值转成文件里的字面文本（只有 \\n 需要转义）。"""
    assert '"' not in value and "\\" not in value, value
    return value


def expected_errata(loc: str) -> str:
    rows = DATA[loc]["errata"]
    assert isinstance(rows, list) and len(rows) == 7, loc
    return "\\n".join(rows)  # 两字符：反斜杠 + n


def apply_locale(loc: str, dry_run: bool) -> list[str]:
    path = LANG_DIR / f"{loc}.json"
    raw = path.read_bytes()
    assert not raw.startswith(b"\xef\xbb\xbf"), f"{loc}: BOM"
    text = raw.decode("utf-8")
    nl = "\r\n" if "\r\n" in text else "\n"
    if nl == "\r\n":
        assert text.count("\n") == text.count("\r\n"), f"{loc}: 行尾混用"
    else:
        assert "\r" not in text, f"{loc}: 出现裸 CR"
    assert text.endswith(nl), f"{loc}: 缺少末尾换行"

    orig_lines = text.split(nl)
    assert orig_lines[-1] == "", f"{loc}: 末尾换行异常"
    lines = list(orig_lines)

    # 文件里的字面文本（choir_errata 用两字符 \n）
    wanted_raw = {
        K_BLEED: literal(str(DATA[loc]["bleed"])),
        K_HINT: literal(str(DATA[loc]["hint"])),
        K_TEXT: expected_errata(loc),
    }
    # json.loads 之后的值（choir_errata 是真换行符）
    wanted_parsed = dict(wanted_raw)
    wanted_parsed[K_TEXT] = wanted_raw[K_TEXT].replace("\\n", "\n")

    changed: dict[int, str] = {}
    for key, value in wanted_raw.items():
        needle = f'  "{key}": "'
        hits = [i for i, ln in enumerate(lines) if ln.startswith(needle)]
        assert len(hits) == 1, f"{loc}: {key} 命中 {len(hits)} 行"
        i = hits[0]
        assert lines[i].endswith('",') or lines[i].endswith('"'), (
            f"{loc}: {key} 行尾异常: {lines[i][-5:]!r}")
        suffix = "," if lines[i].endswith('",') else ""
        new_line = f'  "{key}": "{value}"{suffix}'
        changed[i] = lines[i]
        lines[i] = new_line

    new_text = nl.join(lines)
    new_bytes = new_text.encode("utf-8")

    # --- 校验 ---
    before = json.loads(text)
    after = json.loads(new_text)
    assert list(before.keys()) == list(after.keys()), f"{loc}: 键序变化"
    assert len(after) == 219, f"{loc}: 键数 {len(after)}"
    for k in before:
        if k in wanted_raw:
            continue
        assert before[k] == after[k], f"{loc}: 非目标键被改动 {k}"
    for k, v in wanted_parsed.items():
        assert after[k] == v, f"{loc}: {k} 值不符"

    # 逐行比对：除被替换的 3 行外，其余行必须逐字节相同
    assert len(lines) == len(orig_lines)
    diff = [i for i in range(len(lines)) if lines[i] != orig_lines[i]]
    assert sorted(diff) == sorted(changed), f"{loc}: 意外改动的行 {diff}"

    # 行尾风格/末尾换行/BOM
    assert ("\r\n" in new_text) == (nl == "\r\n"), f"{loc}: 行尾风格变化"
    assert new_text.endswith(nl) and not new_text.endswith(nl * 2), f"{loc}: 末尾换行"
    assert not new_bytes.startswith(b"\xef\xbb\xbf"), f"{loc}: 引入 BOM"

    # 占位符与字面 \n
    assert after[K_BLEED].count("%s") == 1, loc
    assert after[K_HINT].count("%s") == 0, loc
    assert after[K_TEXT].count("%s") == 0, loc
    assert after[K_BLEED].count("%") == 1 and after[K_HINT].count("%") == 0, loc
    text_line = lines[[i for i, ln in enumerate(lines)
                       if ln.startswith(f'  "{K_TEXT}": "')][0]]
    assert text_line.count("\\n") == 6, f"{loc}: 文件里字面 \\n 数量"
    assert after[K_TEXT].count("\n") == 6, f"{loc}: 解析后换行数"
    assert "\r" not in text_line, f"{loc}: 文本行里出现 CR"
    assert after[K_BLEED].count("\\n") == 0 and after[K_HINT].count("\\n") == 0, loc

    rows = after[K_TEXT].split("\n")
    assert len(rows) == 7, f"{loc}: 行数 {len(rows)}"
    assert rows[1] == rows[2], f"{loc}: 第 2/3 行（重复的两条）不相同"
    assert len(set(rows[1:5])) == 3, f"{loc}: 四行版本数异常"

    if not dry_run:
        path.write_bytes(new_bytes)
    return [
        f"{loc}",
        f"  行尾={nl!r} 行数={len(lines) - 1} 改动行={sorted(changed)}",
        f"  bleed %s={after[K_BLEED].count('%s')} hint %s={after[K_HINT].count('%s')} "
        f"errata 字面\\n={text_line.count(chr(92) + 'n')}",
        f"  rows[1]==rows[2]: {rows[1] == rows[2]}",
    ]


def main() -> int:
    dry_run = "--dry-run" in sys.argv
    want = CRLF_LOCALES + LF_LOCALES
    assert sorted(want) == sorted(DATA), "语言列表与数据不一致"
    report: list[str] = []
    for loc in want:
        report.extend(apply_locale(loc, dry_run))
    print("\n".join(report))
    print()
    print(f"{'（试运行）' if dry_run else ''}完成 {len(want)} 个语言文件，"
          f"每个 3 键，其余 216 键逐行未动。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
