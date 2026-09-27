# 资源清单与接入说明

本文档记录 Echoes of Oblivion 的美术与音频资源**当前状态**与替换步骤。

---

## 0. 现状总览

| 类别 | 状态 | 说明 |
| --- | --- | --- |
| 音频（11 个事件） | ✅ **已全部就位** | 8 个程序化合成 + 3 个 CC0 第三方，全部 Ogg Vorbis，共 214 KB |
| 贴图（9 张） | ✅ **已重绘为像素画** | 由 `tools/make_textures.py` 生成（原创），不再是单色块 |
| 贴图（2 张实体） | ⚠️ 未使用 | 自绘几何渲染不采样它们，保留以备升级为正式模型 |
| 实体渲染 | ✅ **已可见** | 自绘几何，不依赖模型文件与贴图 |
| 方块/物品模型 | ✅ **由 datagen 生成** | 单一来源，见第 3 节 |

---

## 1. 音频（已完成）

`sounds.json` 声明的 11 个事件全部有对应文件，客户端日志中不再出现
`File ... does not exist` 警告。

### 程序化合成的 8 个（本项目原创，见 `tools/SoundSynth.java`）

| 文件 | 时长 | 合成方式 |
| --- | --- | --- |
| `memory_collect.ogg` | 1.60s | 五分量失谐钟声 + 上滑高频闪光 |
| `memory_vision.ogg` | 3.00s | 三个低频失谐正弦（拍频）+ 一阶低通噪声层 |
| `heartbeat.ogg` | 1.40s | 两次低频「砰」，频率随时间下潜 |
| `corruption_pulse.ogg` | 2.60s | 两轮呼吸包络的失真低频 |
| `boss_summon.ogg` | 4.50s | 30→90Hz 上行轰鸣 + 加速颤音 + 渐进失真 |
| `boss_hit.ogg` | 1.10s | 快速下潜音 + 被吸走的噪声尾 |
| `portal_open.ogg` | 2.60s | 扫频带通噪声（撕裂）+ 40→70Hz 共振 |
| `portal_travel.ogg` | 1.50s | 900→90Hz 下坠音 + 被拽走的噪声 |

### CC0 第三方素材的 3 个（Undead Moans / AntumDeluge）

| 文件 | 用途 |
| --- | --- |
| `whisper_ambient.ogg` | 高侵蚀时的环境低语 |
| `boss_death.ogg` | Boss 终结 |
| `boss_death_alt.ogg` | Boss 终结（变体，`sounds.json` 中随机选用） |

完整授权信息见 `CREDITS.md`。

### 重新生成流程

音频必须是 **Ogg Vorbis**——Minecraft 的 `SoundManager` 把事件名硬编码为 `.ogg` 路径，
**wav 文件不会被识别**（已实测：留下 8 条 `does not exist` 警告）。

```
# 1. 合成 wav（JDK 自带 javax.sound，无外部依赖）
javac -d run/downloads/synth-classes tools/SoundSynth.java
java -cp run/downloads/synth-classes SoundSynth <输出目录>

# 2. wav -> ogg
ffmpeg -y -i in.wav -c:a libvorbis -q:a 4 -ar 44100 -ac 1 out.ogg

# 3. 放入 src/main/resources/assets/echoesofoblivion/sounds/
```

转换后体积从 1.6 MB 降到 142 KB。

### 替换为其他素材时

- 必须是 **Ogg Vorbis**；源素材若是 wav/mp3，用上面的 ffmpeg 命令转换。
- 文件名必须与 `sounds.json` 中的事件名一致，否则需同步修改 `sounds.json`。
- 建议单声道、44.1kHz、单文件 200 KB 以内。

---

## 2. 贴图（已完成：9 张原创像素画）

原先 11 张 PNG 都是 **16×16 单色块**（约 83 字节，1 种颜色）。现已重绘为
按坐标逐像素设计的像素画，由 `tools/make_textures.py` 生成——纯 Python 标准库
手写 PNG，不依赖 Pillow 等任何图像库，因此版权完全属于本项目。

| 文件 | 尺寸 | 设计 |
| --- | --- | --- |
| `textures/block/memory_crystal.png` | 16×16 | 暗色岩基嵌多枚发光小晶体（方块六面） |
| `textures/item/memory_crystal.png` | 16×16 | 竖立双尖锥晶体，内含亮心 |
| `textures/item/corridor_key.png` | 16×16 | 八边环形匙柄 + 竖直匙杆 + 双齿，紫色符文 |
| `textures/item/memory_scroll.png` | 16×16 | 米色卷纸 + 上下木轴 + 紫色符文 |
| `textures/item/echo_whisper.png` | 16×16 | 青蓝声波涡旋 |
| `textures/item/shard_of_truth.png` | 16×16 | 五边形晶体碎片，白热内芯 |
| `textures/item/void_embers.png` | 16×16 | 三块独立炭团，透出橙红余火 |
| `textures/item/corruption_essence.png` | 16×16 | 不规则暗红团块，亮色脉络 |
| `textures/item/eye_of_silence.png` | 16×16 | 苍白眼球 + 竖瞳 + 紫环 |

重新生成：`python tools/make_textures.py`

### 仍为占位的 2 张

| 文件 | 尺寸 | 说明 |
| --- | --- | --- |
| `textures/entity/phantom.png` | 32×64 | 亡魂 —— 当前自绘几何渲染**不采样**它 |
| `textures/entity/silent_aggregate.png` | 64×64 | Boss —— 同上 |

这两张要等实体升级为 `MobRenderer` + 正式模型时才会用到。

> 传送门方块刻意使用 `RenderShape.INVISIBLE`，只靠粒子表现，不需要贴图。

### 关于 CC0 素材的调研结论

已逐页核查 OpenGameArt 上的候选贴图包，**CC0 授权的 16×16 物品图标包极为稀少**：
`roguelikerpg-items`、`700-rpg-icons`、`gui-items`、`rpg-gui-construction-kit-v10`、
`arkana-fts-gui`、`objects-for-16x16-tilesets`、`lpc-interface-items-...`、
`scrapped-projects`、`ryzom-selected-textures` 的授权均为 CC-BY、CC-BY-SA 或 GPL。

其中 CC-BY-SA **具有传染性**（要求衍生作品以相同方式共享），不宜用于本模组；
CC-BY 可用但引入署名义务。唯一验证通过的 CC0 贴图包是
[Treasure Chests](https://opengameart.org/content/treasure-chests-32x32-and-16x16)
（**CC0 1.0**），但它是宝箱、与它能替换的位置语义不符，故保留在
`tools/cc0-assets/chests/`（含 `SOURCE.txt`）作为备用。

因此物品贴图选择本项目原创绘制——零授权风险，风格统一，且可版本控制。

**若将来要引入 CC0 贴图**，可用来源：

1. [OpenGameArt](https://opengameart.org/) — 注意搜索页需在浏览器里操作
   （列表由 JS 渲染，命令行抓不到结果），授权选 `CC0`
2. [Kenney.nl](https://kenney.nl/assets) — 全部 CC0，但下载按钮由 JS 触发，
   需在浏览器中操作
3. [itch.io](https://itch.io/game-assets/free) — 逐个核对授权说明，
   筛选时注意区分 CC0 / CC-BY / CC-BY-SA

---

## 3. datagen（已完成）

方块状态与物品模型改由代码生成，`src/main/resources` 下的手写副本已删除，
避免两处来源漂移。

```bash
gradlew runData
```

输出到 `src/generated/resources/`，并由 `build.gradle` 中的
`sourceSets.main.resources { srcDir 'src/generated/resources' }` 纳入构建。

| Provider | 生成内容 |
| --- | --- |
| `ModBlockStateProvider` | `blockstates/memory_crystal.json`、`blockstates/unstable_portal.json`、`models/block/memory_crystal.json`、`models/item/memory_crystal.json` |
| `ModItemModelProvider` | 其余 7 个 `models/item/*.json` |

`ExistingFileHelper` 通过 `--existing src/main/resources` 初始化，因此
**引用了不存在的贴图时 datagen 会直接报错**——这是加新方块时最有用的一层保护。

---

## 4. 渲染实现说明

Forge 1.20.6 **移除了整个 GUI 覆层体系**（`IGuiOverlay`、`ForgeGui`、
`RegisterGuiOverlaysEvent`、`RenderGuiEvent` 在 Forge 50.2.10 的 jar 中逐个核实均不存在）。
因此：

- 全屏过渡：`RenderLevelStageEvent` + `RenderSystem` 直绘
- 侵蚀压迫感：`ViewportEvent` 雾效
- 实体可见性：`ShapeRenderer` 自绘彩色几何（`RenderType.lightning()`，加法混合、无贴图采样）

**升级为正式模型的方式**：新建 `ModelLayerLocation`，在
`EntityRenderersEvent.RegisterLayerDefinitions` 中注册 `LayerDefinition`，
再把 `PhantomRenderer` / `SilentAggregateRenderer` 改为继承
`MobRenderer<T, M>`。届时才需要 `textures/entity/*.png` 按标准 UV 布局绘制。
