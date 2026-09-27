# Echoes of Oblivion（遗忘的回响）

> 你不是英雄。你是污染源。

一款基于 **Minecraft 1.20.6 + Forge** 的心理恐怖 / 探索模组。

*[English README](README.md)*

---

## 它是什么

穿过一道不稳定的传送门，进入 **Silent Corridor（寂静走廊）**——一个永无止境、毫无生机的维度，
遍布失落文明的废墟。那里没有怪物、没有动物、没有天气。只有死一般的寂静，以及
**Memory Crystal（记忆水晶）** 的微弱光芒。

触摸水晶，你会目睹那个文明最后时刻的一段碎片。把它们拼起来，你就会知道他们对自己做了什么。

而那些知识不会留在走廊里。你把它带回了家。

## 核心机制

**记忆考古** —— 五段固定记忆，各带一条线索。线索收集齐备后，相关联的记忆会开始**共鸣**，
揭示出任何单条记忆都无法给出的信息。进度按玩家独立记录并持久化在存档中。

**心理恐怖，没有跳杀** —— 你每多知道一点，侵蚀值就涨一分。它让你周围的雾变浓、
在视野余光里投下亡魂的幻影、在环境音里叠上低语与心跳。侵蚀越高，你越看不清自己在往哪走。

**现实坍缩** —— 一旦你带着真相回到主世界，**Mirror Chunks（镜像区块）** 就会从你脚下开始扩散。
区块内重力会闪烁、粒子反向运动，亡魂在其中成形并猎杀生者。

**概念 Boss** —— **Silent Aggregate（寂静聚合体）** 免疫游戏中的一切武器。它无法被杀死，
只能被*动摇*：你亲眼见证过的每一段记忆都可以使用一次，用来扣减它的稳定性；
共鸣的记忆造成的伤害翻倍。见证全部五段记忆、身处感染区块内、手持寂静之眼——这是召唤它的三个条件。

## 安装

1. 安装 **Minecraft Forge 50.2.10 或更高版本（对应 1.20.6）**。
2. 把 jar 放进 `mods/` 目录。

需要 **Java 21**。客户端与专用服务端均可使用。

## 操作

| 操作 | 方式 |
| --- | --- |
| 开启传送门 | 用黑曜石 / 哭泣的黑曜石 / 紫水晶搭一个 2×3 门框，手持**走廊之钥**右键门洞内部 |
| 进入走廊 | 走进传送门 |
| 目睹记忆 | 右键**记忆水晶** |
| 脱离幻境 | `ESC` 或点击 |
| 记忆攻击（Boss 战） | `R` —— 当前选中的快捷栏槽位决定你使用哪段记忆 |
| 召唤 Boss | 手持**寂静之眼**，站在感染区块内，且已见证全部 5 段记忆 |

## 配置

首次运行会生成两个配置文件：`echoesofoblivion-common.toml`（服务端）与
`echoesofoblivion-client.toml`（客户端）。主要选项：

| 键 | 默认值 | 含义 |
| --- | --- | --- |
| `corruptionGainRate` | 1 | 每秒自然增长的侵蚀值 |
| `mirrorExpandInterval` | 300 | 镜像区块扩散间隔（秒） |
| `enableMirrorChunks` | true | 是否启用现实坍缩机制 |
| `overlayIntensity` | 1.0 | 腐蚀雾的强度 |
| `enableShadowEffects` | true | 是否启用亡魂幻影 |

## 语言

模组内置 **21 种语言**（每种 59 条文本）：

`en_us` `zh_cn` `zh_tw` `ja_jp` `ko_kr` `ru_ru` `uk_ua` `de_de` `fr_fr` `es_es` `es_mx`
`it_it` `pt_br` `pt_pt` `nl_nl` `pl_pl` `cs_cz` `sv_se` `da_dk` `nb_no` `tr_tr`

其中 `es_es`/`es_mx` 与 `pt_br`/`pt_pt` 是真正的地区变体，不是复制品。

修改任一语言文件后请运行校验器：

```bash
python tools/check_lang.py
```

它会检查每个语言的 59 个键是否与 `en_us` 完全一致，以及
`%s` / `%%` 占位符、字面量 `\n` 换行和 `◈` 标记是否都被保留下来
——占位符缺失会导致客户端在运行时崩溃。

新增语言：复制 `en_us.json`，并参阅 [docs/translation-spec.md](docs/translation-spec.md)
里的术语表与格式规则。

## 从源码构建

```bash
./gradlew build               # 编译并打包 -> build/libs/
./gradlew runClient           # 启动开发客户端
./gradlew runServer           # 启动开发服务端
./gradlew runData             # 重新生成方块状态与物品模型
./gradlew runGameTestServer   # 运行服务端自动化测试
```

产物：`build/libs/echoes-of-oblivion-1.0.0-mc1.20.6-forge.jar`

### 重新生成美术与音频

音效与贴图**都由代码生成**，可复现且没有第三方授权风险：

```bash
# 音效：合成 8 个 wav，再转成 ogg（Minecraft 只识别 .ogg）
javac -d run/downloads/synth-classes tools/SoundSynth.java
java -cp run/downloads/synth-classes SoundSynth <输出目录>
ffmpeg -y -i in.wav -c:a libvorbis -q:a 4 -ar 44100 -ac 1 out.ogg

# 贴图：9 张像素画，纯 Python 标准库，不需要 Pillow
python tools/make_textures.py
```

## 项目文档

| 文件 | 内容 |
| --- | --- |
| [ASSETS.md](ASSETS.md) | 素材清单、重生成步骤、每张贴图与音效的当前状态 |
| [CREDITS.md](CREDITS.md) | 所有第三方素材的逐文件来源与授权 |
| [achieve.md](achieve.md) | 本模组实现时对照的 8 阶段路线图 |
| [tech-outline.md](tech-outline.md) | 技术架构：包结构、网络、持久化、渲染 |
| [tools/README.md](tools/README.md) | 开发工具与验证命令 |

## 值得了解的技术细节

**Forge 1.20.6 移除了整个 GUI 覆层体系。** `IGuiOverlay`、`ForgeGui`、
`RegisterGuiOverlaysEvent` 与 `RenderGuiEvent` 在 Forge 50.2.10 的 jar 里都不存在
（已逐个类核实）。因此本模组用 `RenderLevelStageEvent` + `RenderSystem` 实现全屏过渡，
用 `ViewportEvent` 雾效实现侵蚀压迫感——不依赖自定义着色器，在老显卡上也能跑。

**实体是几何体而非模型。** `client/ShapeRenderer.java` 直接通过 `VertexConsumer`
输出彩色立方体，因此亡魂与寂静聚合体无需任何模型文件或贴图就能显示。
等美术资源到位后，把它们升级为标准的 `MobRenderer` + `LayerDefinition` 模型是一处独立的改动。

**GameTest 有一个环境限制。** 在 Forge 50.2.10 的测试服务器里，
*任何会移动的 `LivingEntity` 在 tick 时都会抛异常*——因为 `ForgeHooks.isLivingOnLadder`
读取了 `ForgeConfig.SERVER`，而测试服务器不加载服务端配置。这与模组本身无关，
自动化测试因此避开了 `entity.tick()`。详见 `tools/README.md`。

## 授权

**MIT** —— 见 [LICENSE](LICENSE)。

随模组分发的第三方素材为 **CC0 1.0 Universal**（公共领域，不保留任何权利），
逐文件记录在 [CREDITS.md](CREDITS.md)。

Minecraft 是 Mojang Studios 的商标。本项目为未官方授权的第三方模组。
