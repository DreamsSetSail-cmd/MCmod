# 素材来源与授权

本模组使用的全部音频素材均为**可商用、无需署名**的授权（CC0 1.0 Universal 或自行生成）。
以下逐条记录来源，便于日后核查。

---

## 1. 程序化合成的音效（原创，无第三方权利）

由本仓库内的 `tools/SoundSynth.java` 生成（JDK 自带的 `javax.sound.sampled`，
无任何外部依赖）。这些音频是为本模组专门设计的，**版权归本项目所有**，
可按项目许可证自由分发。

| 文件 | 时长 | 合成方式 |
| --- | --- | --- |
| `memory_collect.wav` | 1.60s | 五分量失谐钟声 + 上滑高频闪光，指数衰减 |
| `memory_vision.wav` | 3.00s | 三个低频失谐正弦形成拍频 + 一阶低通噪声层 |
| `heartbeat.wav` | 1.40s | 两次低频「砰」，频率随时间下潜 |
| `corruption_pulse.wav` | 2.60s | 两轮呼吸包络的失真低频 |
| `boss_summon.wav` | 4.50s | 30→90Hz 上行轰鸣 + 加速颤音 + 渐进失真 |
| `boss_hit.wav` | 1.10s | 快速下潜音 + 被吸走的噪声尾 |
| `portal_open.wav` | 2.60s | 扫频带通噪声（撕裂）+ 40→70Hz 共振 |
| `portal_travel.wav` | 1.50s | 900→90Hz 下坠音 + 被拽走的噪声 |

重新生成方式：

```bash
javac -d run/downloads/synth-classes tools/SoundSynth.java
java -cp run/downloads/synth-classes SoundSynth <输出目录>
```

生成结果是 16-bit 单声道 WAV。**Minecraft 只识别 `.ogg`**（`SoundManager` 把事件名
硬编码为 `.ogg` 路径），因此还需要一步转码：

```bash
ffmpeg -y -i memory_collect.wav -c:a libvorbis -q:a 4 -ar 44100 -ac 1 memory_collect.ogg
```

转码后体积从 1.6 MB 降到 142 KB。

---

## 2. CC0 第三方素材

### Undead Moans

- **作者**：AntumDeluge
- **来源**：<https://opengameart.org/content/undead-moans>
- **永久链接**：<https://opengameart.org/node/81246>
- **授权**：CC0 1.0 Universal（页面标注 + 压缩包内附完整 LICENSE.txt）
- **原始规格**：Ogg Vorbis、96kbps 可变、44100Hz、单声道
- **下载包**：`undead-moans.zip`（96.2 KB）

本模组使用其中 3 个文件，按语义映射如下：

| 原始文件 | 模组内文件名 | 用途 |
| --- | --- | --- |
| `undead-1.ogg` | `whisper_ambient.ogg` | 高侵蚀时的环境低语 |
| `undead-3.ogg` | `boss_death.ogg` | Boss 终结 |
| `undead-2.ogg` | `boss_death_alt.ogg` | Boss 终结（变体，`sounds.json` 中随机选用） |

> CC0 允许无署名使用；此处记录仅为可追溯性与对作者的致意。

---

## 3. 贴图

### 程序化生成的 9 张（本项目原创）

由本仓库内的 `tools/make_textures.py` 生成（纯 Python 标准库手写 PNG，
不依赖 Pillow 等任何图像库）。设计坐标逐像素定义，因此**版权完全属于本项目**，
可按项目许可证自由分发。

| 文件 | 尺寸 | 设计 |
| --- | --- | --- |
| `block/memory_crystal.png` | 16×16 | 暗色岩基嵌多枚发光小晶体（方块六面） |
| `item/memory_crystal.png` | 16×16 | 竖立双尖锥晶体，内含亮心 |
| `item/corridor_key.png` | 16×16 | 八边环形匙柄 + 竖直匙杆 + 双齿，紫色符文点缀 |
| `item/memory_scroll.png` | 16×16 | 米色卷纸 + 上下木轴 + 紫色符文 |
| `item/echo_whisper.png` | 16×16 | 青蓝声波涡旋，中心亮、边缘散 |
| `item/shard_of_truth.png` | 16×16 | 五边形晶体碎片，白热内芯 |
| `item/void_embers.png` | 16×16 | 三块独立炭团，裂缝透出橙红余火 |
| `item/corruption_essence.png` | 16×16 | 不规则暗红团块，表面亮色脉络 |
| `item/eye_of_silence.png` | 16×16 | 苍白眼球 + 竖瞳 + 紫环 |

重新生成：

```bash
python tools/make_textures.py
```

### CC0 第三方贴图（已下载验证，未接入）

**Treasure Chests (32x32 and 16x16)** —— 授权 **CC0 1.0 Universal**，
来源 <https://opengameart.org/content/treasure-chests-32x32-and-16x16>。

文件与完整来源信息保存在 `tools/cc0-assets/chests/`（含 `SOURCE.txt`）。

**未接入的原因**：该图是一张宝箱，与它唯一能替换的位置（记忆水晶方块贴图）
语义不符。保留为已验证的 CC0 备用素材。

> **调研结论**：OpenGameArt 上 CC0 授权的 16×16 物品图标包极为稀少——
> 逐页核查过的候选（`roguelikerpg-items`、`700-rpg-icons`、`gui-items`、
> `rpg-gui-construction-kit-v10`、`arkana-fts-gui`、`objects-for-16x16-tilesets`、
> `lpc-interface-items-...`、`scrapped-projects`、`ryzom-selected-textures`）
> 授权均为 CC-BY、CC-BY-SA 或 GPL。CC-BY-SA 具有传染性，不宜用于本模组；
> CC-BY 虽可用但引入署名义务。因此物品贴图改为本项目原创绘制。

---

## 4. 授权结论

- 全部分发的**音频**为 CC0 或本项目原创。
- 全部分发的**贴图**为本项目原创（CC0 备用素材保留在 `tools/` 下，未进入构建产物）。
- 无 CC-BY 类「必须署名」素材，因此不构成署名义务。
- 全部素材均可随模组自由分发，**包括商业用途**。
- 如需替换为其他素材，请确保来源标注 CC0 或等价的公共领域授权；
  **避免 CC-BY-SA**（要求衍生作品以相同方式共享）。

---

## 5. 尚未使用的候选素材（保留在 `run/` 下）

`run/` 目录不参与打包，以下文件不会进入发布产物：

| 素材 | 状态 | 未采用原因 |
| --- | --- | --- |
| `zombies.zip`（24 个僵尸人声，CC0） | 已下载 | 24-bit/44.1kHz WAV，OpenAL 对 24-bit 支持不确定，风险高于收益 |
| `portal_1.ogg`（CC0） | 已下载 | 1.84 MB，对 1~2 秒的音效而言体积失控 |
| `80-CC0-creature-SFX_0.zip`（CC0） | 下载损坏 | OpenGameArt 限速导致截断，多次重试未成功 |
| `chests_16x16.png` 等 | 已下载 | 见第 3 节；副本已存入 `tools/cc0-assets/` |
