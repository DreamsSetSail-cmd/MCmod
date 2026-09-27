# 翻译参考规格（给翻译任务使用）

## 目标

为 Minecraft Forge 模组 **Echoes of Oblivion**（心理恐怖/探索）编写多语言文件。
每个语言文件是一个 JSON 对象：59 个键，键名**必须逐字照抄英文原文**，只翻译值。

## 输出位置与文件名

```
src/main/resources/assets/echoesofoblivion/lang/<locale>.json
```

必须使用 Minecraft 的语言代码作为文件名（下列即全部允许值）：
`es_es` `es_mx` `fr_fr` `de_de` `it_it` `pt_br` `pt_pt` `nl_nl` `pl_pl` `ru_ru`
`uk_ua` `tr_tr` `ja_jp` `ko_kr` `zh_tw` `sv_se` `cs_cz` `hu_hu` `fi_fi` `da_dk`
`nb_no` `el_gr` `ro_ro` `vi_vn` `th_th` `id_id` `ar_sa`

## 硬性规则

1. **文件名**必须是上表中的语言代码，写成 `<code>.json`。
2. **键名一字不改**。59 个键全部都要有，一个不能少、不能多。
3. **保留占位符**：`%s` 与 `%%` 必须原样保留，且出现次数一致。
   - `screen.echoesofoblivion.corruption_overlay` 的值形如 `腐蚀: %s%%`，
     翻译后仍须含 `%s%%`。
   - `message.echoesofoblivion.boss.hit` 含两个 `%s`，顺序不能换
     （第一个是伤害值，第二个是剩余稳定性）。
4. **保留换行转义**：`memory.*.content` 的值里有字面量 `\n`（反斜杠 + n，
   在 JSON 中写作 `\\n` 才对应一个反斜杠）。**必须原样保留 `\n` 分隔**，
   否则幻境屏幕不会分句。JSON 文件里请写成 `\\n`。
5. **保留特殊符号** `◈`（在 `screen.echoesofoblivion.resonance` 值开头）。
6. 输出为 **UTF-8 无 BOM** 的合法 JSON，缩进 2 空格，末尾留一个换行。
7. 专有名词**不音译**，按下表处理。

## 术语表（必须统一）

| 英文 | 处理方式 |
| --- | --- |
| Echoes of Oblivion | 模组名。**保留英文原样**，不要翻译。 |
| Silent Corridor | 专有地名。**保留英文**（或按各语言惯例音译，但同一文件内必须一致，且优先保留英文）。 |
| Silent Aggregate | Boss 专名。**保留英文**。 |
| Memory Crystal | 直译（各语言的自然说法），全文统一。 |
| Corridor Key | 直译。 |
| Memory Scroll | 直译。 |
| Shard of Truth | 直译。 |
| Void Embers | 直译。 |
| Corruption Essence | 直译。 |
| Eye of Silence | 直译。 |
| Phantom | 用该语言里 Minecraft 官方的亡灵/幽灵生物译名（如僵尸的亡灵系词根）。 |
| obsidian | 用 Minecraft 官方译名。 |
| crying obsidian | 用 Minecraft 官方译名。 |
| amethyst | 用 Minecraft 官方译名。 |
| ESC | 保留 `ESC`。 |

## 语气要求

这是一个**心理恐怖**模组：文案冷静、克制、不安。
不要用轻快或热情的语气；不要加感叹号；不要用「享受游戏吧」这类口吻。
`message.*` 是给玩家看的提示，要短、要冷。

## 59 个键与英文原文

```
block.echoesofoblivion.memory_crystal = Memory Crystal
block.echoesofoblivion.unstable_portal = Unstable Portal
item.echoesofoblivion.memory_crystal = Memory Crystal
item.echoesofoblivion.corridor_key = Corridor Key
item.echoesofoblivion.corridor_key.invalid_frame = Invalid frame: needs a 2x3 obsidian / crying obsidian / amethyst frame
item.echoesofoblivion.memory_scroll = Memory Scroll
item.echoesofoblivion.echo_whisper = Echo Whisper
item.echoesofoblivion.shard_of_truth = Shard of Truth
item.echoesofoblivion.void_embers = Void Embers
item.echoesofoblivion.corruption_essence = Corruption Essence
item.echoesofoblivion.eye_of_silence = Eye of Silence
itemGroup.echoesofoblivion = Echoes of Oblivion
entity.echoesofoblivion.silent_aggregate = Silent Aggregate
entity.echoesofoblivion.phantom = Phantom
key.categories.echoesofoblivion = Echoes of Oblivion
key.echoesofoblivion.memory_attack = Memory Attack
sounds.echoesofoblivion.memory_collect = Memory Collected
sounds.echoesofoblivion.whisper_ambient = Whispers
sounds.echoesofoblivion.heartbeat = Heartbeat
sounds.echoesofoblivion.corruption_pulse = Corruption Pulse
sounds.echoesofoblivion.boss_summon = Silent Aggregate Awakens
sounds.echoesofoblivion.boss_hit = Memory Strikes
sounds.echoesofoblivion.boss_death = Silence Falls
sounds.echoesofoblivion.portal_open = Portal Opens
sounds.echoesofoblivion.portal_travel = Portal Travel
sounds.echoesofoblivion.memory_vision = Memory Vision
screen.echoesofoblivion.corruption_overlay = Corruption: %s%%
screen.echoesofoblivion.memory_vision = Memory Vision
screen.echoesofoblivion.resonance = ◈ Resonance: the fragments align
screen.echoesofoblivion.skip = Press ESC or click to break free
message.echoesofoblivion.milestone.whisper = You heard whispers that do not belong here...
message.echoesofoblivion.milestone.watched = It has begun to notice you.
message.echoesofoblivion.milestone.complete = The fragments are whole. Something waits at the end of the corridor.
message.echoesofoblivion.boss.not_collected = You never truly witnessed this memory.
message.echoesofoblivion.boss.no_target = There is nothing nearby to unsettle.
message.echoesofoblivion.boss.already_used = This memory is spent; it no longer cuts.
message.echoesofoblivion.boss.hit = The memory deals %s conceptual damage (%s stability left)
message.echoesofoblivion.boss.resonant_hit = Resonance! The memory deals %s conceptual damage (%s stability left)
message.echoesofoblivion.boss.defeated = The silence breaks. You brought back the truth - and its gaze.
message.echoesofoblivion.boss.need_memories = You have not witnessed every memory; it will not answer.
message.echoesofoblivion.boss.need_infected = This place is not yet corrupted; the rite cannot hold.
message.echoesofoblivion.boss.summoned = The air thickens. The Silent Aggregate descends.
death.attack.echoesofoblivion.corruption = %s was consumed by corruption
death.attack.echoesofoblivion.silence = %s was silenced by the Aggregate
memory.echoesofoblivion.memory_0.title = The Fall
memory.echoesofoblivion.memory_0.content = I still remember the day the sky cracked.\nWe thought we had achieved perfection.\nWe were wrong.
memory.echoesofoblivion.memory_0.clue = Clue: the sky that cracked
memory.echoesofoblivion.memory_1.title = The First Silence
memory.echoesofoblivion.memory_1.content = When the first voice faded, we called it a miracle.\nWhen the last voice faded, we understood.\nSilence is not peace. It is the absence of everything.
memory.echoesofoblivion.memory_1.clue = Clue: the absence of every sound
memory.echoesofoblivion.memory_2.title = The Architects
memory.echoesofoblivion.memory_2.content = We built more than cities.\nWe built bridges between thoughts, ladders between dreams.\nAnd when those bridges collapsed, we fell together.
memory.echoesofoblivion.memory_2.clue = Clue: bridges between thoughts
memory.echoesofoblivion.memory_3.title = The Last Experiment
memory.echoesofoblivion.memory_3.content = The Apparatus hummed with light never seen before.\nIt was the last thing we ever saw clearly.\nAfter that, only echoes.
memory.echoesofoblivion.memory_3.clue = Clue: the Apparatus
memory.echoesofoblivion.memory_4.title = The Warning
memory.echoesofoblivion.memory_4.content = She tried to warn us.\nHer words twisted mid-air, becoming something else.\nSomething that listened back.
memory.echoesofoblivion.memory_4.clue = Clue: the thing that listens back
```

> 上表中 `\n` 是**字面两字符**（反斜杠 + n）。写进 JSON 文件时要表示成 `\\n`，
> 这样 JSON 解析出来才是反斜杠 + n，游戏才能按它分行。

## 自检清单（写完每个文件后逐条核对）

- [ ] 文件名是正确的语言代码
- [ ] 键数量正好 59，与上表一一对应
- [ ] 所有 `%s` / `%%` / `\n` / `◈` 都已保留且数量正确
- [ ] 是合法 JSON（可用 `python -c "import json;json.load(open(...))"` 验证）
- [ ] 术语在一个文件内前后一致
- [ ] 语气冷淡、克制，符合心理恐怖基调
